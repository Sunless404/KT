package com.sunless404.keeptrack.ui.assistant

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sunless404.keeptrack.App
import com.sunless404.keeptrack.ai.AiException
import com.sunless404.keeptrack.ai.Gemini
import com.sunless404.keeptrack.domain.AiPrompt
import com.sunless404.keeptrack.domain.AssistantLogic
import com.sunless404.keeptrack.domain.Attachment
import com.sunless404.keeptrack.domain.ChatMessage
import com.sunless404.keeptrack.domain.RoutineJson
import com.sunless404.keeptrack.domain.dayName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.LocalDate

data class AssistantUi(
    val messages: List<ChatMessage> = emptyList(),
    val input: String = "",
    val pending: List<Attachment> = emptyList(),
    val busy: Boolean = false,
    val recordingSec: Int? = null,
    val notice: String? = null,
)

class AssistantViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as App
    private val repo = app.repo
    private val _ui = MutableStateFlow(AssistantUi())
    val ui = _ui.asStateFlow()

    val hasKey: Boolean get() = app.secrets.apiKey.isNotBlank()

    fun setInput(t: String) = _ui.update { it.copy(input = t) }
    fun removePending(a: Attachment) = _ui.update { it.copy(pending = it.pending - a) }
    fun clear() = _ui.update { AssistantUi() }
    fun dismissNotice() = _ui.update { it.copy(notice = null) }

    // ---------- Adjuntos ----------

    /** Carpeta de la caché donde la cámara deja las fotos (declarada en file_paths.xml). */
    fun cacheDir(): File = File(app.cacheDir, "adjuntos").apply { mkdirs() }

    /** Lee una imagen, la reduce a 1280 px como máximo y la añade como JPEG. */
    fun addImage(uri: Uri) {
        viewModelScope.launch {
            try {
                val bytes = withContext(Dispatchers.IO) { loadJpeg(uri) }
                _ui.update { it.copy(pending = it.pending + Attachment("image/jpeg", bytes)) }
            } catch (e: Exception) {
                _ui.update { it.copy(notice = "No se pudo leer la imagen: ${e.message}") }
            }
        }
    }

    private fun loadJpeg(uri: Uri): ByteArray {
        val max = 1280
        val bitmap: Bitmap = if (Build.VERSION.SDK_INT >= 28) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(app.contentResolver, uri)) { decoder, info, _ ->
                val s = info.size
                val scale = minOf(1f, max.toFloat() / maxOf(s.width, s.height))
                decoder.setTargetSize((s.width * scale).toInt().coerceAtLeast(1), (s.height * scale).toInt().coerceAtLeast(1))
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            app.contentResolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= max) sample *= 2
            app.contentResolver.openInputStream(uri)!!.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
            } ?: error("formato no soportado")
        }
        return ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            out.toByteArray()
        }
    }

    private var recorder: MediaRecorder? = null
    private var recordFile: File? = null
    private var recordTimer: Job? = null

    fun startRecording() {
        if (recorder != null) return
        val file = File(cacheDir(), "nota-${System.currentTimeMillis()}.aac")
        try {
            @Suppress("DEPRECATION")
            val r = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(app) else MediaRecorder()
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioSamplingRate(16_000)
            r.setAudioEncodingBitRate(32_000)
            r.setMaxDuration(MAX_AUDIO_SEC * 1000)
            r.setOutputFile(file.absolutePath)
            r.setOnInfoListener { _, what, _ ->
                if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) stopRecording()
            }
            r.prepare()
            r.start()
            recorder = r
            recordFile = file
            _ui.update { it.copy(recordingSec = 0) }
            recordTimer = viewModelScope.launch {
                while (true) {
                    delay(1000)
                    _ui.update { it.copy(recordingSec = (it.recordingSec ?: 0) + 1) }
                }
            }
        } catch (e: Exception) {
            _ui.update { it.copy(notice = "No se pudo grabar: ${e.message}") }
            recorder = null
        }
    }

    fun stopRecording(discard: Boolean = false) {
        val r = recorder ?: return
        val secs = _ui.value.recordingSec ?: 0
        recordTimer?.cancel()
        recorder = null
        val ok = try {
            r.stop()
            true
        } catch (_: Exception) {
            false // grabación demasiado corta
        } finally {
            r.release()
        }
        val file = recordFile
        _ui.update { it.copy(recordingSec = null) }
        if (discard || !ok || file == null) {
            file?.delete()
            return
        }
        val bytes = file.readBytes()
        file.delete()
        _ui.update { it.copy(pending = it.pending + Attachment("audio/aac", bytes, secs.coerceAtLeast(1))) }
    }

    // ---------- Conversación ----------

    fun send(text: String = _ui.value.input) {
        val s = _ui.value
        if (s.busy || (text.isBlank() && s.pending.isEmpty())) return
        if (s.recordingSec != null) stopRecording()
        val user = ChatMessage(fromUser = true, text = text.trim(), attachments = _ui.value.pending)
        _ui.update { it.copy(messages = it.messages + user, input = "", pending = emptyList(), busy = true) }
        viewModelScope.launch {
            val reply = try {
                val blocks = repo.allBlocks()
                val today = LocalDate.now()
                val tasks = repo.observeTasks(today, today.plusDays(30)).first()
                val system = AiPrompt.system(
                    todayIso = today.toString(),
                    weekday = dayName(today.dayOfWeek.value).lowercase(),
                    routineJson = blocks.takeIf { it.isNotEmpty() }?.let { RoutineJson.encode(it).toString() },
                    upcomingTasks = AssistantLogic.describeTasks(tasks),
                )
                val raw = Gemini.generate(app.secrets.apiKey, app.prefs.aiModel, system, AssistantLogic.contents(_ui.value.messages))
                val r = AssistantLogic.parseReply(raw, blocks)
                ChatMessage(fromUser = false, text = r.message, routine = r.routine, tasks = r.tasks)
            } catch (e: AiException) {
                ChatMessage(fromUser = false, text = e.message.orEmpty(), error = true)
            } catch (e: Exception) {
                ChatMessage(fromUser = false, text = "Error inesperado: ${e.message}", error = true)
            }
            _ui.update { it.copy(messages = it.messages + reply, busy = false) }
        }
    }

    fun applyRoutine(index: Int) {
        val m = _ui.value.messages.getOrNull(index) ?: return
        val r = m.routine ?: return
        viewModelScope.launch {
            repo.replaceTemplate(r.blocks)
            markApplied(index) { it.copy(routineApplied = true) }
        }
    }

    fun applyTasks(index: Int) {
        val m = _ui.value.messages.getOrNull(index) ?: return
        if (m.tasks.isEmpty()) return
        viewModelScope.launch {
            repo.addTasks(m.tasks)
            markApplied(index) { it.copy(tasksApplied = true) }
        }
    }

    private fun markApplied(index: Int, change: (ChatMessage) -> ChatMessage) = _ui.update { s ->
        s.copy(messages = s.messages.mapIndexed { i, m -> if (i == index) change(m) else m })
    }

    override fun onCleared() {
        stopRecording(discard = true)
    }

    companion object {
        const val MAX_AUDIO_SEC = 120
    }
}
