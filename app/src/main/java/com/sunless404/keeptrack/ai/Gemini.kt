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

class AiException(message: String) : Exception(message)

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
            conn.connectTimeout = 15_000
            conn.readTimeout = 120_000
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
            if (code !in 200..299) throw AiException(errorMessage(code, text))
            return json.parseToJsonElement(text).jsonObject
        } catch (e: IOException) {
            throw AiException("No se pudo conectar con Gemini. Comprueba la conexión a Internet. (${e.message})")
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

    /**
     * Envía una conversación ([contents] en el formato de la API: role + parts) y
     * devuelve el texto de la respuesta, pedida en JSON.
     */
    suspend fun generate(apiKey: String, model: String, system: String, contents: JsonArray): String =
        withContext(Dispatchers.IO) {
            val body = buildJsonObject {
                put("systemInstruction", buildJsonObject {
                    put("parts", buildJsonArray { add(buildJsonObject { put("text", system) }) })
                })
                put("contents", contents)
                put("generationConfig", buildJsonObject {
                    put("responseMimeType", "application/json")
                    put("temperature", 0.3)
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
