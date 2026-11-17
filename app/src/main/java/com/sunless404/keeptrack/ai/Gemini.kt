package com.sunless404.keeptrack.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Error de Gemini. [code] es el código HTTP, [TIMEOUT] si tardó demasiado o [OFFLINE] si no hubo conexión. */
open class AiException(message: String, val code: Int = 0) : Exception(message) {
    /** Fallos pasajeros: vale la pena reintentar o probar otro modelo. */
    val transient get() = code == TIMEOUT || code == 429 || code in 500..599
    /** El modelo no existe o no se puede usar con esta clave: se prueba otro. */
    val modelProblem get() = code == 404

    companion object {
        const val TIMEOUT = -1
        const val OFFLINE = -2
    }
}

data class AiModel(val id: String, val name: String)

/**
 * Cliente mínimo de la API de Google Gemini (plan gratuito de Google AI Studio).
 * Solo se llama cuando el usuario lo pide; no hay más tráfico de red.
 */
object Gemini {
    private const val BASE = "https://generativelanguage.googleapis.com/v1beta"
    const val DEFAULT_MODEL = "gemini-flash-latest"
    const val KEY_URL = "https://aistudio.google.com/apikey"

    private val json = Json { ignoreUnknownKeys = true }

    private fun request(path: String, apiKey: String, body: String?): JsonObject {
        val conn = URL("$BASE/$path").openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 10_000
            conn.readTimeout = 60_000
            conn.setRequestProperty("x-goog-api-key", apiKey)
            if (body != null) {
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conn.outputStream.use { it.write(body.toByteArray()) }
            }
            val code = conn.responseCode
            val text = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.use { it.readBytes().decodeToString() }.orEmpty()
            if (code !in 200..299) throw AiException(errorMessage(code, text), code)
            return json.parseToJsonElement(text).jsonObject
        } catch (e: java.net.SocketTimeoutException) {
            throw AiException("Gemini está tardando demasiado en responder. Inténtalo de nuevo en un rato.", AiException.TIMEOUT)
        } catch (e: java.net.UnknownHostException) {
            throw AiException("No hay conexión a Internet.", AiException.OFFLINE)
        } catch (e: IOException) {
            throw AiException("No se pudo conectar con Gemini. Comprueba la conexión a Internet. (${e.message})", AiException.OFFLINE)
        } finally {
            conn.disconnect()
        }
    }

    private fun errorMessage(code: Int, body: String): String {
        val detail = try {
            json.parseToJsonElement(body).jsonObject["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content
        } catch (_: Exception) {
            null
        }
        return when (code) {
            400 -> if (detail?.contains("API key", ignoreCase = true) == true)
                "La clave de API no es válida. Revísala en Ajustes." else "Petición rechazada: ${detail ?: code}"
            401, 403 -> "La clave de API no es válida o no tiene permiso. Revísala en Ajustes."
            404 -> "El modelo elegido ya no existe. Pulsa «Probar clave y elegir modelo» en Ajustes."
            429 -> "Has llegado al límite gratuito de Gemini por ahora. Espera un rato (o hasta mañana) y vuelve a intentarlo."
            503 -> "Los servidores de Gemini están saturados ahora mismo (503). Inténtalo en unos minutos."
            in 500..599 -> "Gemini no responde ahora mismo ($code). Inténtalo en unos minutos."
            else -> "Error $code: ${detail ?: "sin detalle"}"
        }
    }

    /** Modelos de la clave que generan texto, con los Flash primero (los Pro no suelen ser gratis). */
    suspend fun listModels(apiKey: String): List<AiModel> = withContext(Dispatchers.IO) {
        val res = request("models?pageSize=1000", apiKey, null)
        res["models"]?.jsonArray.orEmpty().mapNotNull { el ->
            val o = el.jsonObject
            val methods = o["supportedGenerationMethods"]?.jsonArray?.map { it.jsonPrimitive.content }.orEmpty()
            val id = o["name"]?.jsonPrimitive?.content?.removePrefix("models/") ?: return@mapNotNull null
            if ("generateContent" !in methods || !id.startsWith("gemini")) return@mapNotNull null
            if (listOf("tts", "image", "audio", "embedding", "live", "robotics", "computer").any { it in id }) return@mapNotNull null
            AiModel(id, o["displayName"]?.jsonPrimitive?.content ?: id)
        }.sortedWith(
            compareBy<AiModel>(
                { !it.id.endsWith("-latest") },
                { !it.id.contains("flash") },
                { it.id.contains("lite") },
                { it.id.contains("preview") || it.id.contains("exp") },
            ).thenByDescending { it.id }
        )
    }

    /** Modelos que han rechazado el ajuste de razonamiento en esta sesión. */
    private val noFast = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    /** Solo los Flash 2.5 aceptan seguro que se desactive el razonamiento. */
    private fun usesFastConfig(model: String) = model.contains("2.5-flash") && model !in noFast

    /** Modelos gratuitos de reserva, por si el elegido está saturado o no responde. */
    private val FALLBACKS = listOf("gemini-flash-latest", "gemini-flash-lite-latest", "gemini-2.5-flash", "gemini-2.5-flash-lite", "gemini-2.0-flash")

    /**
     * Envía una conversación ([contents] en el formato de la API: role + parts) y
     * devuelve el texto de la respuesta, pedida en JSON.
     *
     * Si Gemini está saturado (503), tarda demasiado o se agota el cupo de un modelo,
     * reintenta una vez y luego prueba otros modelos gratuitos antes de rendirse.
     */
    suspend fun generate(apiKey: String, model: String, system: String, contents: JsonArray): String {
        val models = (listOf(model) + FALLBACKS).distinct().take(4)
        var last: AiException? = null
        for ((i, m) in models.withIndex()) {
            val attempts = if (i == 0) 2 else 1
            for (a in 0 until attempts) {
                try {
                    return try {
                        generateOnce(apiKey, m, system, contents, fast = true)
                    } catch (e: AiException) {
                        // Algunos modelos no aceptan desactivar el razonamiento ("invalid argument"):
                        // se repite sin pedirlo y se recuerda para no volver a intentarlo con ese modelo.
                        if (e.code == 400 && usesFastConfig(m)) {
                            noFast += m
                            generateOnce(apiKey, m, system, contents, fast = false)
                        } else throw e
                    }
                } catch (e: AiException) {
                    last = e
                    if (!e.transient && !e.modelProblem) throw e
                    if (e.modelProblem) break
                    if (a + 1 < attempts) kotlinx.coroutines.delay(1500)
                }
            }
        }
        val e = last ?: AiException("Gemini no ha respondido.")
        throw if (e.transient) AiException(e.message + " (He probado con varios modelos.)", e.code) else e
    }

    /**
     * [fast]: en los modelos Flash se desactiva el "razonamiento" previo, que para esta tarea
     * apenas mejora la respuesta y la hace bastante más lenta (era la causa de muchos tiempos de espera).
     */
    private suspend fun generateOnce(apiKey: String, model: String, system: String, contents: JsonArray, fast: Boolean): String =
        withContext(Dispatchers.IO) {
            val body = buildJsonObject {
                put("systemInstruction", buildJsonObject {
                    put("parts", buildJsonArray { add(buildJsonObject { put("text", system) }) })
                })
                put("contents", contents)
                put("generationConfig", buildJsonObject {
                    put("responseMimeType", "application/json")
                    put("temperature", 0.3)
                    if (fast && usesFastConfig(model)) {
                        put("thinkingConfig", buildJsonObject { put("thinkingBudget", 0) })
                    }
                })
            }
            val res = request("models/$model:generateContent", apiKey, body.toString())
            val candidate = res["candidates"]?.jsonArray?.firstOrNull()?.jsonObject
            val text = candidate?.get("content")?.jsonObject?.get("parts")?.jsonArray
                ?.mapNotNull { (it as? JsonObject)?.get("text")?.jsonPrimitive?.content }
                ?.joinToString("")
            if (text.isNullOrBlank()) {
                val reason = candidate?.get("finishReason")?.jsonPrimitive?.content
                    ?: (res["promptFeedback"] as? JsonObject)?.get("blockReason")?.jsonPrimitive?.content
                throw AiException("Gemini no ha respondido" + (reason?.let { " ($it)" } ?: "") + ". Prueba a reformular el mensaje.")
            }
            text
        }
}
