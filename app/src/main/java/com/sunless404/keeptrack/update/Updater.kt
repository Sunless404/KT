package com.sunless404.keeptrack.update

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import com.sunless404.keeptrack.App
import com.sunless404.keeptrack.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Versión publicada en GitHub Releases. */
data class Release(val versionCode: Int, val name: String, val notes: String, val apkUrl: String, val sizeBytes: Long)

sealed interface UpdateState {
    data object Idle : UpdateState
    data class Available(val release: Release) : UpdateState
    data class Downloading(val release: Release, val progress: Float) : UpdateState
    data class ReadyToInstall(val release: Release, val file: File) : UpdateState
    data class Failed(val release: Release, val message: String) : UpdateState
}

object ReleaseParser {
    private val json = Json { ignoreUnknownKeys = true }

    /** Lee la respuesta de /releases/latest. Las etiquetas son "build-N", con N = versionCode. */
    fun parse(body: String): Release? {
        val o = json.parseToJsonElement(body) as? JsonObject ?: return null
        val code = o["tag_name"]?.jsonPrimitive?.content?.removePrefix("build-")?.toIntOrNull() ?: return null
        val asset = o["assets"]?.jsonArray?.map { it.jsonObject }
            ?.firstOrNull { it["name"]?.jsonPrimitive?.content?.endsWith(".apk") == true } ?: return null
        return Release(
            versionCode = code,
            name = o["name"]?.jsonPrimitive?.content ?: "1.$code",
            notes = o["body"]?.jsonPrimitive?.content.orEmpty().trim(),
            apkUrl = asset["browser_download_url"]?.jsonPrimitive?.content ?: return null,
            sizeBytes = asset["size"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0,
        )
    }
}

/**
 * Busca versiones nuevas en las Releases de GitHub del proyecto, las descarga y
 * abre el instalador de Android (que siempre pide confirmación al usuario).
 */
class Updater(private val app: App) {
    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    private val dir get() = File(app.cacheDir, "updates").apply { mkdirs() }

    private var lastCheck = 0L
    /** Versión aplazada con "Más tarde": no se vuelve a ofrecer hasta que se reinicie la app. */
    private var postponedVersion = 0

    /**
     * Consulta la última versión publicada. Se llama cada vez que se abre o se vuelve a
     * la app (con un margen de 1 min para no repetir); [force] lo hace siempre.
     * Devuelve null si va bien, o el motivo si no se pudo comprobar.
     */
    suspend fun check(force: Boolean = false): String? {
        val now = System.currentTimeMillis()
        if (!force && now - lastCheck < 60_000L) return null
        val result = withContext(Dispatchers.IO) {
            runCatching {
                val c = URL(LATEST).openConnection() as HttpURLConnection
                c.connectTimeout = 10_000
                c.readTimeout = 15_000
                c.setRequestProperty("Accept", "application/vnd.github+json")
                // La API de GitHub rechaza peticiones sin User-Agent.
                c.setRequestProperty("User-Agent", "KeepTrack/${BuildConfig.VERSION_NAME}")
                try {
                    if (c.responseCode != 200) error("GitHub respondió ${c.responseCode}")
                    ReleaseParser.parse(c.inputStream.use { it.readBytes().decodeToString() }) ?: error("no hay ningún APK publicado")
                } finally {
                    c.disconnect()
                }
            }
        }
        val release = result.getOrElse { return "No se pudo comprobar: ${it.message}" }
        // Solo cuenta como comprobado si ha ido bien.
        lastCheck = now
        // Las versiones antiguas descargadas ya no sirven.
        dir.listFiles()?.forEach { if (!it.name.contains("-${release.versionCode}.")) it.delete() }
        val busy = _state.value is UpdateState.Downloading || _state.value is UpdateState.ReadyToInstall
        if (release.versionCode > BuildConfig.VERSION_CODE && !busy && (force || release.versionCode != postponedVersion)) {
            _state.value = UpdateState.Available(release)
        }
        return null
    }

    /** "Más tarde": no vuelve a preguntar por esta versión hasta la próxima vez que se abra la app. */
    fun postpone() {
        val s = _state.value
        if (s is UpdateState.Available) postponedVersion = s.release.versionCode
        _state.value = UpdateState.Idle
    }

    suspend fun download(release: Release) {
        val target = File(dir, "keeptrack-${release.versionCode}.apk")
        _state.value = UpdateState.Downloading(release, 0f)
        try {
            withContext(Dispatchers.IO) {
                val part = File(dir, target.name + ".part")
                val c = URL(release.apkUrl).openConnection() as HttpURLConnection
                c.connectTimeout = 15_000
                c.readTimeout = 30_000
                c.instanceFollowRedirects = true
                try {
                    if (c.responseCode != 200) error("respuesta ${c.responseCode}")
                    val total = c.contentLengthLong.takeIf { it > 0 } ?: release.sizeBytes
                    c.inputStream.use { input ->
                        part.outputStream().use { out ->
                            val buf = ByteArray(64 * 1024)
                            var done = 0L
                            var lastShown = 0f
                            while (true) {
                                val n = input.read(buf)
                                if (n < 0) break
                                out.write(buf, 0, n)
                                done += n
                                val p = if (total > 0) done.toFloat() / total else 0f
                                if (p - lastShown >= 0.01f) {
                                    lastShown = p
                                    _state.value = UpdateState.Downloading(release, p)
                                }
                            }
                        }
                    }
                } finally {
                    c.disconnect()
                }
                part.renameTo(target)
            }
            _state.value = UpdateState.ReadyToInstall(release, target)
        } catch (e: Exception) {
            _state.value = UpdateState.Failed(release, "No se pudo descargar: ${e.message}")
        }
    }

    /** Android exige permitir "instalar apps desconocidas" a KeepTrack la primera vez. */
    fun canInstall(): Boolean = app.packageManager.canRequestPackageInstalls()

    fun installPermissionIntent(): Intent =
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${app.packageName}"))

    fun installIntent(file: File): Intent {
        val uri = FileProvider.getUriForFile(app, "${app.packageName}.files", file)
        return Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    fun dismiss() {
        _state.value = UpdateState.Idle
    }

    companion object {
        const val LATEST = "https://api.github.com/repos/Sunless404/KT/releases/latest"
    }
}
