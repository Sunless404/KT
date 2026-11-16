package com.sunless404.keeptrack.cloud

import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * Empaqueta la copia de seguridad (JSON) para Firestore: gzip + base64, troceado
 * en partes que caben holgadas en un documento (límite de 1 MiB).
 */
object SyncCodec {
    const val CHUNK_CHARS = 700_000

    fun encode(json: String): List<String> {
        val zipped = ByteArrayOutputStream().use { out ->
            GZIPOutputStream(out).use { it.write(json.toByteArray()) }
            out.toByteArray()
        }
        val b64 = Base64.getEncoder().encodeToString(zipped)
        return b64.chunked(CHUNK_CHARS).ifEmpty { listOf("") }
    }

    fun decode(chunks: List<String>): String {
        val bytes = Base64.getDecoder().decode(chunks.joinToString(""))
        return GZIPInputStream(bytes.inputStream()).use { it.readBytes().decodeToString() }
    }

    private val USERNAME = Regex("^[a-z0-9_.]{3,20}$")

    /** Nombre de usuario válido: 3 a 20 caracteres, letras minúsculas, números, punto y guion bajo. */
    fun normalizeUsername(raw: String): String? = raw.trim().lowercase().takeIf { USERNAME.matches(it) }
}
