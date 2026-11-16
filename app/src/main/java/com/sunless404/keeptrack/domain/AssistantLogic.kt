package com.sunless404.keeptrack.domain

import com.sunless404.keeptrack.data.Block
import com.sunless404.keeptrack.data.Task
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.LocalDate
import java.time.ZoneId
import java.util.Base64

/** Adjunto de un mensaje: imagen o audio. */
class Attachment(val mime: String, val bytes: ByteArray, val durationSec: Int = 0) {
    val isImage get() = mime.startsWith("image/")
}

/** Cambios de rutina propuestos por la IA, ya interpretados. */
data class RoutineProposal(
    val blocks: List<Block>,
    val errors: List<LineError>,
    val added: Int,
    val removed: Int,
    val analysis: Analysis,
)

data class AssistantReply(
    val message: String,
    val routine: RoutineProposal? = null,
    val tasks: List<Task> = emptyList(),
)

data class ChatMessage(
    val fromUser: Boolean,
    val text: String,
    val attachments: List<Attachment> = emptyList(),
    val routine: RoutineProposal? = null,
    val tasks: List<Task> = emptyList(),
    val routineApplied: Boolean = false,
    val tasksApplied: Boolean = false,
    val error: Boolean = false,
)

object AssistantLogic {
    private val json = Json { isLenient = true; ignoreUnknownKeys = true }

    private fun key(b: Block) = listOf(b.dayOfWeek, b.startMin, b.endMin, b.title.trim().lowercase(), b.category)

    /** Bloques añadidos y quitados respecto a la plantilla actual. */
    fun diff(current: List<Block>, proposed: List<Block>): Pair<Int, Int> {
        val a = current.map(::key).toSet()
        val b = proposed.map(::key).toSet()
        return (b - a).size to (a - b).size
    }

    /** Tareas de los próximos días en texto, para dar contexto a la IA. */
    fun describeTasks(tasks: List<Task>): String = tasks.joinToString("\n") { t ->
        "- ${LocalDate.ofEpochDay(t.epochDay)}" + (t.timeMin?.let { " ${fmtTime(it)}" } ?: "") +
            ": ${t.title}" + (if (t.done) " (hecha)" else "") + (if (t.description.isNotBlank()) " — ${t.description}" else "")
    }

    /**
     * Historial en el formato de la API. Solo el último mensaje lleva sus adjuntos
     * (en los anteriores se indica que los había) para no reenviarlos cada vez.
     */
    fun contents(history: List<ChatMessage>): JsonArray = buildJsonArray {
        val lastUser = history.indexOfLast { it.fromUser }
        history.forEachIndexed { i, m ->
            if (m.error) return@forEachIndexed
            add(buildJsonObject {
                put("role", if (m.fromUser) "user" else "model")
                put("parts", buildJsonArray {
                    if (m.fromUser && i == lastUser) {
                        m.attachments.forEach { a ->
                            add(buildJsonObject {
                                put("inlineData", buildJsonObject {
                                    put("mimeType", a.mime)
                                    put("data", Base64.getEncoder().encodeToString(a.bytes))
                                })
                            })
                        }
                    }
                    val note = if (m.fromUser && i != lastUser && m.attachments.isNotEmpty()) {
                        " [adjuntó ${m.attachments.count { it.isImage }} imagen(es) y ${m.attachments.count { !it.isImage }} audio(s)]"
                    } else ""
                    val text = when {
                        m.fromUser -> m.text.ifBlank { if (m.attachments.isNotEmpty()) "(sin texto)" else "" } + note
                        else -> buildJsonObject {
                            put("mensaje", m.text)
                            put("rutina", if (m.routine != null) JsonPrimitive("(propuesta omitida)") else JsonNull)
                            put("tareas", JsonArray(m.tasks.map { JsonPrimitive(it.title) }))
                        }.toString()
                    }
                    add(buildJsonObject { put("text", text) })
                })
            })
        }
    }

    private fun JsonObject.str(vararg keys: String): String? =
        keys.firstNotNullOfOrNull { (this[it] as? JsonPrimitive)?.takeIf { p -> p.isString }?.content }

    /** Interpreta la respuesta JSON de la IA. Si no es JSON válido, se muestra tal cual. */
    fun parseReply(raw: String, current: List<Block>, zone: ZoneId = ZoneId.systemDefault()): AssistantReply {
        val text = raw.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val root: JsonElement = try {
            json.parseToJsonElement(text)
        } catch (_: Exception) {
            return AssistantReply(message = raw.trim())
        }
        val obj = root as? JsonObject ?: return AssistantReply(message = raw.trim())
        val message = obj.str("mensaje", "message").orEmpty()

        val routine = obj["rutina"]?.takeIf { it !is JsonNull && it !is JsonPrimitive }?.let { r ->
            val parsed = RoutineParser.parse(r.toString())
            if (parsed.blocks.isEmpty() && parsed.errors.isEmpty()) null else {
                val (added, removed) = diff(current, parsed.blocks)
                RoutineProposal(parsed.blocks, parsed.errors, added, removed, RoutineAnalysis.analyze(parsed.blocks))
            }
        }

        val tasks = (obj["tareas"] as? JsonArray).orEmpty().mapNotNull { el ->
            val t = el as? JsonObject ?: return@mapNotNull null
            val title = t.str("titulo", "title")?.trim().orEmpty()
            val date = t.str("fecha", "date")?.let { runCatching { LocalDate.parse(it.trim()) }.getOrNull() }
            if (title.isEmpty() || date == null) return@mapNotNull null
            val time = t.str("hora", "time")?.let { RoutineParser.parseTime(it) }
            val remind = t.str("recordatorio", "reminder")?.let { RoutineParser.parseTime(it) }
            Task(
                epochDay = date.toEpochDay(),
                timeMin = time,
                title = title,
                description = t.str("descripcion", "description").orEmpty().trim(),
                notify = remind != null,
                remindAt = remind?.let { date.atStartOfDay(zone).plusMinutes(it.toLong()).toInstant().toEpochMilli() },
            )
        }

        return AssistantReply(
            message = message.ifBlank { if (routine != null || tasks.isNotEmpty()) "Te propongo estos cambios:" else "…" },
            routine = routine,
            tasks = tasks,
        )
    }
}
