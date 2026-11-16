package com.sunless404.keeptrack.domain

import com.sunless404.keeptrack.data.Block
import com.sunless404.keeptrack.data.Category
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Convierte la plantilla al JSON del formato B (el mismo que lee [RoutineParser]). */
object RoutineJson {
    private val dayKeys = listOf("lunes", "martes", "miercoles", "jueves", "viernes", "sabado", "domingo")

    /** Nombre de categoría en el JSON: sin tildes, como lo documenta el README. */
    fun categoryKey(c: Category): String = RoutineParser.normalize(c.label)

    /**
     * Agrupa en un solo bloque los que solo se diferencian en el día, para que el
     * JSON sea corto y fácil de editar (también para la IA).
     */
    fun encode(blocks: List<Block>): JsonObject {
        val groups = blocks.sortedWith(compareBy({ it.startMin }, { it.dayOfWeek }))
            .groupBy { listOf(it.title, it.category, it.startMin, it.endMin, it.notes, it.notify, it.leadMin) }
        val list: JsonArray = buildJsonArray {
            groups.values.forEach { same ->
                val b = same.first()
                add(buildJsonObject {
                    put("titulo", b.title)
                    put("categoria", categoryKey(b.category))
                    put("dias", JsonArray(same.map { it.dayOfWeek }.distinct().sorted().map { JsonPrimitive(dayKeys[it - 1]) }))
                    put("inicio", fmtTime(b.startMin))
                    put("fin", fmtTime(b.endMin))
                    if (b.notes.isNotBlank()) put("notas", b.notes)
                    put("aviso", b.notify)
                    b.leadMin?.let { put("antelacion", it) }
                })
            }
        }
        return buildJsonObject {
            put("version", 1)
            put("bloques", list)
        }
    }
}
