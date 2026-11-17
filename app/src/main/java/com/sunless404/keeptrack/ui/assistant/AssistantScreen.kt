package com.sunless404.keeptrack.ui.assistant

import android.Manifest
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.sunless404.keeptrack.ui.common.HelpTexts
import com.sunless404.keeptrack.ui.common.TopActions
import com.sunless404.keeptrack.ui.common.appear
import com.sunless404.keeptrack.ui.theme.Brand
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sunless404.keeptrack.domain.Attachment
import com.sunless404.keeptrack.domain.ChatMessage
import com.sunless404.keeptrack.domain.DAY_SHORT
import com.sunless404.keeptrack.domain.dayName
import com.sunless404.keeptrack.domain.fmtDuration
import com.sunless404.keeptrack.domain.fmtRange
import com.sunless404.keeptrack.domain.fmtShortDate
import com.sunless404.keeptrack.domain.fmtTime
import com.sunless404.keeptrack.ui.common.KtIcons
import com.sunless404.keeptrack.ui.common.composeColor
import java.io.File
import java.time.LocalDate

/** Texto de los mensajes: algo más grande y con más interlineado para que respire. */
private val ChatText = TextStyle(fontSize = 15.sp, lineHeight = 23.sp)

private val SUGGESTIONS = listOf(
    "Crea una rutina para estudiar 3 h al día entre semana",
    "Pasa el gimnasio a martes y jueves a las 19:00",
    "Apunta dentista el viernes a las 17:00 y avísame antes",
    "¿Cuántas horas de estudio tengo a la semana?",
)

@Composable
fun AssistantScreen(vm: AssistantViewModel, onOpenSettings: () -> Unit, onOpenProfile: () -> Unit) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val list = rememberLazyListState()
    var cameraFile by remember { mutableStateOf<File?>(null) }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        uri?.let(vm::addImage)
    }
    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val f = cameraFile
        if (ok && f != null) vm.addImage(Uri.fromFile(f))
    }
    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.startRecording()
    }

    LaunchedEffect(ui.messages.size, ui.busy) {
        val n = ui.messages.size + (if (ui.busy) 1 else 0)
        if (n > 0) list.animateScrollToItem(n - 1)
    }

    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars).imePadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainer).padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(KtIcons.Sparkle, null, Modifier.size(16.dp), tint = Brand.Accent)
                Text("  Gemini", style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.weight(1f))
            TopActions("Asistente", HelpTexts.ASSISTANT, onOpenProfile) {
                if (ui.messages.isNotEmpty()) {
                    IconButton(onClick = vm::clear) { Icon(Icons.Default.Refresh, "Nueva conversación") }
                }
            }
        }

        if (!vm.hasKey) {
            NoKey(onOpenSettings)
            return@Column
        }

        LazyColumn(
            state = list,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            if (ui.messages.isEmpty()) item { Welcome { vm.send(it) } }
            itemsIndexed(ui.messages) { i, m ->
                Box(Modifier.appear(0, 30f)) {
                    if (m.fromUser) UserBubble(m) else AssistantBubble(m, onApplyRoutine = { vm.applyRoutine(i) }, onApplyTasks = { vm.applyTasks(i) })
                }
            }
            if (ui.busy) item { Box(Modifier.appear(0, 20f)) { Typing() } }
        }

        ui.notice?.let {
            Text(
                it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            LaunchedEffect(it) {
                kotlinx.coroutines.delay(4000)
                vm.dismissNotice()
            }
        }
        InputBar(
            ui = ui,
            onInput = vm::setInput,
            onSend = { vm.send() },
            onRemove = vm::removePending,
            onGallery = { pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            onCamera = {
                val f = File(vm.cacheDir(), "foto-${System.currentTimeMillis()}.jpg")
                cameraFile = f
                takePhoto.launch(FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", f))
            },
            onMic = {
                when {
                    ui.recordingSec != null -> vm.stopRecording()
                    ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED -> vm.startRecording()
                    else -> micPermission.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
            onCancelRecording = { vm.stopRecording(discard = true) },
        )
    }
}

@Composable
private fun NoKey(onOpenSettings: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.padding(16.dp).fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(KtIcons.Assistant, null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.secondary)
            Text("Activa el asistente", style = MaterialTheme.typography.titleLarge)
            Text(
                "Usa Google Gemini, gratis y sin tarjeta. Solo necesitas una clave de Google AI Studio; se configura una vez en Ajustes.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = onOpenSettings) { Text("Configurar") }
        }
    }
}

private val SUGGESTION_ICONS = listOf(KtIcons.Routine, KtIcons.Dumbbell, KtIcons.Tasks, KtIcons.Stats)

@Composable
internal fun Welcome(onPick: (String) -> Unit) {
    val t = rememberInfiniteTransition(label = "orbe")
    val spin by t.animateFloat(0f, 360f, infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "giro")
    val breathe by t.animateFloat(0.92f, 1.06f, infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "respira")
    Column(Modifier.fillMaxWidth().padding(top = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(96.dp).appear(0), contentAlignment = Alignment.Center) {
            Box(
                Modifier.size(84.dp).scale(breathe).rotate(spin).clip(CircleShape)
                    .background(Brush.sweepGradient(listOf(Brand.Accent, Color(0xFFB46CFF), Brand.Fire, Brand.Accent))),
            )
            Box(Modifier.size(70.dp).clip(CircleShape).background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
                Icon(KtIcons.Sparkle, null, Modifier.size(32.dp), tint = Brand.Accent)
            }
        }
        Spacer(Modifier.height(20.dp))
        Text("¿En qué te ayudo hoy?", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.appear(1))
        Spacer(Modifier.height(6.dp))
        Text(
            "Escríbeme, mándame una foto de tu horario o una nota de voz.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.appear(2).padding(horizontal = 24.dp),
        )
        Spacer(Modifier.height(24.dp))
        SUGGESTIONS.chunked(2).forEachIndexed { r, pair ->
            Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEachIndexed { c, text ->
                    val i = r * 2 + c
                    Card(
                        onClick = { onPick(text) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.weight(1f).height(132.dp).appear(3 + i),
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Icon(SUGGESTION_ICONS[i], null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.height(10.dp))
                            Text(text, style = MaterialTheme.typography.bodyMedium, maxLines = 4, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
        Text(
            "Se envía a Google lo que escribes o adjuntas, tu rutina semanal y tus tareas del próximo mes.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.appear(8).padding(top = 6.dp, start = 12.dp, end = 12.dp),
        )
    }
}

@Composable
internal fun UserBubble(m: ChatMessage) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
        Column(
            Modifier.widthIn(max = 310.dp)
                .clip(RoundedCornerShape(24.dp, 24.dp, 8.dp, 24.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            m.attachments.forEach { a -> AttachmentView(a, onPrimary = true) }
            if (m.text.isNotBlank()) Text(m.text, style = ChatText, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
internal fun AssistantBubble(m: ChatMessage, onApplyRoutine: () -> Unit, onApplyTasks: () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                Modifier.size(32.dp).clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Brand.Accent.copy(alpha = 0.35f), Color(0xFFB46CFF).copy(alpha = 0.35f)))),
                contentAlignment = Alignment.Center,
            ) { Icon(KtIcons.Sparkle, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurface) }
            Spacer(Modifier.size(12.dp))
            Text(
                m.text,
                style = ChatText,
                color = if (m.error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f).padding(top = 4.dp)
                    .then(
                        if (m.error) Modifier.clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)).padding(12.dp)
                        else Modifier
                    ),
            )
        }
        m.routine?.let { r ->
            ProposalCard(
                title = "Cambios en tu rutina",
                subtitle = "+${r.added} bloques nuevos · −${r.removed} quitados" +
                    (if (r.analysis.overlaps.isNotEmpty()) " · ${r.analysis.overlaps.size} solapamientos" else ""),
                applied = m.routineApplied,
                applyLabel = "Aplicar a mi rutina",
                onApply = onApplyRoutine,
            ) {
                var open by remember { mutableStateOf(false) }
                if (r.errors.isNotEmpty()) {
                    Text("${r.errors.size} bloques no se entendieron y se ignorarán.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                r.analysis.sleepByNight.takeIf { it.isNotEmpty() }?.let { sleep ->
                    Text(
                        "Sueño: " + sleep.entries.joinToString { (d, mins) -> "${DAY_SHORT[d - 1]} ${fmtDuration(mins)}" },
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                OutlinedButton(onClick = { open = !open }) { Text(if (open) "Ocultar detalle" else "Ver la rutina propuesta") }
                AnimatedVisibility(open) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        (1..7).forEach { d ->
                            val day = r.blocks.filter { it.dayOfWeek == d }.sortedBy { it.startMin }
                            if (day.isEmpty()) return@forEach
                            Text(dayName(d), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
                            day.forEach { b ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(8.dp).clip(CircleShape).background(b.category.composeColor))
                                    Text("  ${fmtRange(b.startMin, b.endMin)}  ${b.title}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
        if (m.tasks.isNotEmpty()) {
            ProposalCard(
                title = if (m.tasks.size == 1) "1 tarea nueva" else "${m.tasks.size} tareas nuevas",
                subtitle = null,
                applied = m.tasksApplied,
                applyLabel = "Añadir a Tareas",
                onApply = onApplyTasks,
            ) {
                m.tasks.forEach { t ->
                    Column {
                        Text(t.title, style = MaterialTheme.typography.titleSmall)
                        Text(
                            fmtShortDate(LocalDate.ofEpochDay(t.epochDay)) + (t.timeMin?.let { " · ${fmtTime(it)}" } ?: "") +
                                (if (t.notify) " · con recordatorio" else ""),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (t.description.isNotBlank()) Text(t.description, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProposalCard(
    title: String,
    subtitle: String?,
    applied: Boolean,
    applyLabel: String,
    onApply: () -> Unit,
    content: @Composable () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.padding(start = 44.dp).fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            content()
            if (applied) {
                Text("✓ Aplicado", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
            } else {
                Button(onClick = onApply, modifier = Modifier.fillMaxWidth()) { Text(applyLabel) }
            }
        }
    }
}

@Composable
internal fun Typing() {
    val t = rememberInfiniteTransition(label = "escribiendo")
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh), contentAlignment = Alignment.Center) {
            Icon(KtIcons.Sparkle, null, Modifier.size(18.dp), tint = Brand.Accent)
        }
        Text("  Pensando", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        (0..2).forEach { i ->
            val a by t.animateFloat(0.2f, 1f, infiniteRepeatable(tween(500, delayMillis = i * 150), RepeatMode.Reverse), label = "punto$i")
            Box(Modifier.size(9.dp).alpha(a).clip(CircleShape).background(MaterialTheme.colorScheme.onSurfaceVariant))
        }
    }
}

@Composable
private fun AttachmentView(a: Attachment, onPrimary: Boolean, onRemove: (() -> Unit)? = null) {
    Box {
        if (a.isImage) {
            val bmp = remember(a) { BitmapFactory.decodeByteArray(a.bytes, 0, a.bytes.size)?.asImageBitmap() }
            bmp?.let {
                Image(it, "Imagen adjunta", Modifier.size(if (onRemove != null) 64.dp else 200.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
            }
        } else {
            Row(
                Modifier.clip(CircleShape)
                    .background(if (onPrimary) Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceContainerHighest)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(KtIcons.Mic, null, Modifier.size(16.dp), tint = if (onPrimary) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                Text(
                    " Nota de voz · ${a.durationSec / 60}:${"%02d".format(a.durationSec % 60)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (onPrimary) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        if (onRemove != null) {
            Box(
                Modifier.align(Alignment.TopEnd).size(20.dp).clip(CircleShape).background(MaterialTheme.colorScheme.inverseSurface),
                contentAlignment = Alignment.Center,
            ) {
                IconButton(onClick = onRemove, modifier = Modifier.size(20.dp)) {
                    Icon(Icons.Default.Close, "Quitar", Modifier.size(14.dp), tint = MaterialTheme.colorScheme.inverseOnSurface)
                }
            }
        }
    }
}

@Composable
private fun InputBar(
    ui: AssistantUi,
    onInput: (String) -> Unit,
    onSend: () -> Unit,
    onRemove: (Attachment) -> Unit,
    onGallery: () -> Unit,
    onCamera: () -> Unit,
    onMic: () -> Unit,
    onCancelRecording: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
            if (ui.pending.isNotEmpty()) {
                FlowRow(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ui.pending.forEach { a -> AttachmentView(a, onPrimary = false, onRemove = { onRemove(a) }) }
                }
            }
            if (ui.recordingSec != null) {
                val pulse = rememberInfiniteTransition(label = "rec")
                val a by pulse.animateFloat(0.3f, 1f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "recAlpha")
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(12.dp).alpha(a).clip(CircleShape).background(MaterialTheme.colorScheme.error))
                    Text(
                        "  Grabando ${ui.recordingSec / 60}:${"%02d".format(ui.recordingSec % 60)}",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedButton(onClick = onCancelRecording) { Text("Descartar") }
                    Spacer(Modifier.size(8.dp))
                    FilledIconButton(onClick = onMic) { Icon(KtIcons.Stop, "Terminar grabación") }
                }
                return@Column
            }
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest).padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onGallery, enabled = !ui.busy) { Icon(KtIcons.Image, "Adjuntar imagen") }
                IconButton(onClick = onCamera, enabled = !ui.busy) { Icon(KtIcons.Camera, "Hacer foto") }
                TextField(
                    value = ui.input,
                    onValueChange = onInput,
                    placeholder = { Text("Escribe un mensaje…") },
                    maxLines = 5,
                    shape = RoundedCornerShape(24.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                    ),
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.size(4.dp))
                if (ui.input.isBlank() && ui.pending.isEmpty()) {
                    FilledIconButton(onClick = onMic, enabled = !ui.busy) { Icon(KtIcons.Mic, "Grabar nota de voz") }
                } else {
                    FilledIconButton(onClick = onSend, enabled = !ui.busy) { Icon(Icons.AutoMirrored.Filled.Send, "Enviar") }
                }
            }
        }
    }
}
