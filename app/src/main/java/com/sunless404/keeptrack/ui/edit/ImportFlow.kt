package com.sunless404.keeptrack.ui.edit

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sunless404.keeptrack.data.Block
import com.sunless404.keeptrack.domain.ImportFormats
import com.sunless404.keeptrack.domain.ParseResult
import com.sunless404.keeptrack.domain.RoutineParser
import com.sunless404.keeptrack.domain.RoutineTemplate
import com.sunless404.keeptrack.domain.RoutineTemplates
import com.sunless404.keeptrack.domain.RoutineText
import com.sunless404.keeptrack.domain.conflictsOf
import com.sunless404.keeptrack.domain.dayName
import com.sunless404.keeptrack.domain.fmtDuration
import com.sunless404.keeptrack.domain.fmtTime
import com.sunless404.keeptrack.ui.common.KtIcons
import com.sunless404.keeptrack.ui.common.composeColor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/** Texto o archivo que otra app ha compartido con KeepTrack, pendiente de importar. */
object SharedImport {
    val text = MutableStateFlow<String?>(null)

    /** Lee lo compartido por otra app (texto o archivo). */
    fun receive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND) return
        val shared = intent.getStringExtra(Intent.EXTRA_TEXT)
            ?: @Suppress("DEPRECATION") (intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri)?.let { readText(context, it).getOrNull() }
        if (!shared.isNullOrBlank()) text.value = shared
    }
}

/** Cómo se aplica lo importado. */
enum class ImportMode { REPLACE_ALL, REPLACE_DAYS, ADD }

private enum class Step { TEXT, FILE, TEMPLATES, PREVIEW }

private const val MAX_FILE = 1_000_000

/** Lee un archivo de texto elegido por el usuario (UTF-8, sin BOM). */
internal fun readText(context: Context, uri: Uri): Result<String> = runCatching {
    val name = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
        if (it.moveToFirst()) it.getString(0) else null
    }.orEmpty().lowercase()
    if (name.endsWith(".xlsx") || name.endsWith(".xls") || name.endsWith(".ods")) {
        error("Las hojas de Excel no se leen directamente. En Excel o Google Sheets usa «Guardar como» o «Descargar» en formato CSV y abre ese archivo.")
    }
    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: error("No se pudo abrir el archivo")
    if (bytes.size > MAX_FILE) error("El archivo es demasiado grande para ser una rutina")
    val text = bytes.decodeToString().removePrefix("﻿")
    if (text.contains("\"exportedAt\"")) {
        error("Es una copia de seguridad completa de KeepTrack. Restáurala desde Ajustes › Copia de seguridad.")
    }
    text
}

/** Botón de la cabecera de Rutina. */
@Composable
fun ImportButton(onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick, contentPadding = PaddingValues(start = 12.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)) {
        Icon(KtIcons.Import, null, Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Importar")
    }
}

/**
 * Importar rutina: menú de opciones (hoja inferior) y, después, el paso elegido a pantalla
 * completa, siempre con vista previa antes de aplicar nada.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportFlow(
    current: List<Block>,
    menuOpen: Boolean,
    sharedText: String?,
    onCloseMenu: () -> Unit,
    onSharedConsumed: () -> Unit,
    onApply: (List<Block>, ImportMode) -> Unit,
) {
    val context = LocalContext.current
    var step by remember { mutableStateOf<Step?>(null) }
    var from by remember { mutableStateOf(Step.TEXT) }
    var result by remember { mutableStateOf<ParseResult?>(null) }
    var source by remember { mutableStateOf("") }
    var fileError by remember { mutableStateOf<String?>(null) }

    fun preview(text: String, origin: String, back: Step) {
        result = RoutineParser.parse(text)
        source = origin
        from = back
        step = Step.PREVIEW
    }

    LaunchedEffect(sharedText) {
        if (sharedText != null) {
            preview(sharedText, "Compartido desde otra app", Step.TEXT)
            onSharedConsumed()
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        readText(context, uri)
            .onSuccess { preview(it, "Archivo", Step.FILE) }
            .onFailure { fileError = it.message }
    }

    if (menuOpen) {
        val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val scope = rememberCoroutineScope()
        fun choose(action: () -> Unit) {
            scope.launch { sheet.hide() }.invokeOnCompletion { onCloseMenu(); action() }
        }
        ModalBottomSheet(onDismissRequest = onCloseMenu, sheetState = sheet) {
            ImportMenu(hasRoutine = current.isNotEmpty()) { option ->
                choose {
                    when (option) {
                        MenuOption.TEXT -> step = Step.TEXT
                        MenuOption.FILE -> step = Step.FILE
                        MenuOption.TEMPLATES -> step = Step.TEMPLATES
                        MenuOption.SHARE -> {
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Mi rutina de KeepTrack")
                                putExtra(Intent.EXTRA_TEXT, RoutineText.encode(current))
                            }
                            context.startActivity(Intent.createChooser(send, "Compartir rutina"))
                        }
                    }
                }
            }
        }
    }

    fileError?.let { msg ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { fileError = null },
            title = { Text("No se puede importar") },
            text = { Text(msg) },
            confirmButton = { TextButton(onClick = { fileError = null }) { Text("Entendido") } },
        )
    }

    val s = step ?: return
    Dialog(
        onDismissRequest = { step = null },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            BackHandler { step = if (s == Step.PREVIEW && source != "Compartido desde otra app") from else null }
            AnimatedContent(
                s,
                transitionSpec = {
                    val dir = if (targetState == Step.PREVIEW) 1 else -1
                    (slideInHorizontally(tween(280)) { it / 6 * dir } + fadeIn(tween(280))) togetherWith
                        (slideOutHorizontally(tween(220)) { -it / 6 * dir } + fadeOut(tween(180)))
                },
                label = "importar",
            ) { st ->
                when (st) {
                    Step.TEXT -> TextStep(onClose = { step = null }) { preview(it, "Texto", Step.TEXT) }
                    Step.FILE -> FileStep(onClose = { step = null }) {
                        picker.launch(arrayOf("text/*", "application/json", "application/csv", "application/vnd.ms-excel", "application/octet-stream"))
                    }
                    Step.TEMPLATES -> TemplatesStep(onClose = { step = null }) { t ->
                        result = RoutineParser.parse(t.text)
                        source = "Plantilla «${t.name}»"
                        from = Step.TEMPLATES
                        step = Step.PREVIEW
                    }
                    Step.PREVIEW -> PreviewStep(
                        result = result ?: return@AnimatedContent,
                        source = source,
                        current = current,
                        canGoBack = source != "Compartido desde otra app",
                        onBack = { step = from },
                        onClose = { step = null },
                        onApply = { blocks, mode -> onApply(blocks, mode); step = null },
                    )
                }
            }
        }
    }
}

internal enum class MenuOption { TEXT, FILE, TEMPLATES, SHARE }

/** Contenido de la hoja inferior con las formas de importar. */
@Composable
internal fun ImportMenu(hasRoutine: Boolean, onPick: (MenuOption) -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Importar rutina", style = MaterialTheme.typography.titleLarge)
        Text(
            "Antes de cambiar nada verás una vista previa.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        OptionRow(KtIcons.Paste, "Escribir o pegar", "Texto, tablas o frases como «Gimnasio lunes y jueves de 18 a 19:30»") { onPick(MenuOption.TEXT) }
        OptionRow(KtIcons.File, "Abrir archivo", "JSON, CSV de Excel o Google Sheets, o TXT. Con plantillas para copiar") { onPick(MenuOption.FILE) }
        OptionRow(KtIcons.Templates, "Plantillas", "Empieza con una rutina tipo y ajústala a tu gusto") { onPick(MenuOption.TEMPLATES) }
        if (hasRoutine) {
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            TextButton(onClick = { onPick(MenuOption.SHARE) }) {
                Icon(KtIcons.Export, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Compartir mi rutina como texto")
            }
        }
    }
}

@Composable
private fun OptionRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
            }
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Esqueleto de cada paso: barra superior, contenido con scroll y botón principal abajo. */
@Composable
private fun StepScaffold(
    title: String,
    onClose: () -> Unit,
    navIcon: ImageVector = Icons.Default.Close,
    bottom: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars).imePadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(navIcon, "Cerrar") }
            Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 4.dp))
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
        Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp)) { bottom() }
    }
}

@Composable
private fun PasteChip(onPaste: (String) -> Unit) {
    val clipboard = LocalClipboardManager.current
    OutlinedButton(onClick = { clipboard.getText()?.text?.let(onPaste) }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
        Icon(KtIcons.Paste, null, Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text("Pegar")
    }
}

private val textExamples = listOf(
    "Frases" to "Gimnasio lunes, miércoles y viernes de 18 a 19:30\nLeer todos los días 30 min a las 23:00",
    "Por días" to "Lunes a viernes\n08:00-14:00 Clases\n16:00-18:00 Estudio",
    "Tabla" to "| Hora | Lunes | Martes |\n|---|---|---|\n| 08:00-09:00 | Inglés | Mates |",
)

@Composable
internal fun TextStep(onClose: () -> Unit, onReview: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    StepScaffold(
        "Escribir o pegar", onClose,
        bottom = { Button(onClick = { onReview(text) }, enabled = text.isNotBlank(), modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("Revisar") } },
    ) {
        Text(
            "Escribe tu rutina como te salga o pega un horario. Cada línea es un bloque.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = text, onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth().heightIn(min = 220.dp),
            placeholder = { Text("Estudio de lunes a viernes de 8 a 10\nGimnasio martes y jueves a las 18:00 durante 1h30") },
            textStyle = MaterialTheme.typography.bodyMedium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PasteChip { text = if (text.isBlank()) it else text.trimEnd() + "\n" + it }
            if (text.isNotEmpty()) TextButton(onClick = { text = "" }) { Text("Borrar") }
        }
        Text("Formatos que entiende", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
        textExamples.forEach { (name, sample) ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(name, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                        TextButton(onClick = { text = sample }) { Text("Usar ejemplo") }
                    }
                    Text(sample, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Text(
            "Truco: añade la categoría entre corchetes, por ejemplo «Piano de 18 a 19 [academia]».",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun NumberedCard(n: Int, title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(26.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                    Text("$n", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                }
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 10.dp))
            }
            content()
        }
    }
}

@Composable
internal fun FileStep(onClose: () -> Unit, onPick: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf<String?>(null) }
    StepScaffold(
        "Abrir archivo", onClose,
        bottom = {
            Button(onClick = onPick, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                Icon(KtIcons.File, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Elegir archivo")
            }
        },
    ) {
        Text(
            "Elige un archivo del móvil, Drive o Descargas. Si aún no lo tienes, copia una de estas plantillas, " +
                "rellénala (o pídesela a una IA) y guárdala como archivo.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ImportFormats.all.forEach { f ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(f.name, style = MaterialTheme.typography.titleMedium)
                            Text(f.extension, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                        OutlinedButton(
                            onClick = { clipboard.setText(AnnotatedString(f.sample)); copied = f.name },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Icon(if (copied == f.name) Icons.Default.Check else KtIcons.Paste, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (copied == f.name) "Copiado" else "Copiar")
                        }
                    }
                    Text(f.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    // Como un editor de código: sin cortar líneas, con scroll horizontal.
                    Box(
                        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                            .horizontalScroll(rememberScrollState()).padding(10.dp),
                    ) {
                        Text(f.sample, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), softWrap = false)
                    }
                }
            }
        }
        Text(
            "Categorías: clase, academia, estudio, gimnasio, desplazamiento, comida, sueno, lectura, higiene, libre u otro. " +
                "Si no la pones, se deduce del nombre.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun TemplatesStep(onClose: () -> Unit, onPick: (RoutineTemplate) -> Unit) {
    StepScaffold("Plantillas", onClose) {
        Text(
            "Elige una para empezar. Podrás revisarla antes de aplicarla y cambiar lo que quieras después.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        RoutineTemplates.all.forEach { t ->
            val blocks = remember(t) { t.blocks }
            val hours = blocks.groupBy { it.category }.mapValues { (_, v) -> v.sumOf { it.durationMin } }
                .entries.filter { it.key.name != "SUENO" && it.key.name != "COMIDA" }.sortedByDescending { it.value }.take(3)
            Card(
                onClick = { onPick(t) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(t.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Icon(Icons.Default.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(t.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    WeekStrip(blocks)
                    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        hours.forEach { (cat, min) ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(8.dp).clip(CircleShape).background(cat.composeColor))
                                Text(" ${cat.label} ${(min + 30) / 60} h/sem", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Miniatura de la semana: una barra por día con los bloques coloreados por categoría. */
@Composable
private fun WeekStrip(blocks: List<Block>) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
        (1..7).forEach { day ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.fillMaxWidth().height(42.dp).clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                ) {
                    androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                        blocks.filter { it.dayOfWeek == day }.forEach { b ->
                            val segs = if (b.crossesMidnight) listOf(b.startMin to 1440) else listOf(b.startMin to b.endMin)
                            segs.forEach { (a, z) ->
                                drawRect(
                                    b.category.composeColor,
                                    androidx.compose.ui.geometry.Offset(0f, size.height * a / 1440f),
                                    androidx.compose.ui.geometry.Size(size.width, size.height * (z - a) / 1440f),
                                )
                            }
                        }
                    }
                }
                Text(dayName(day).take(1), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
internal fun PreviewStep(
    result: ParseResult,
    source: String,
    current: List<Block>,
    canGoBack: Boolean,
    onBack: () -> Unit,
    onClose: () -> Unit,
    onApply: (List<Block>, ImportMode) -> Unit,
) {
    val blocks = result.blocks
    val days = blocks.map { it.dayOfWeek }.toSortedSet()
    var mode by remember(result) {
        mutableStateOf(if (current.isEmpty() || days.size == 7) ImportMode.REPLACE_ALL else ImportMode.REPLACE_DAYS)
    }
    var showErrors by remember(result) { mutableStateOf(blocks.isEmpty()) }
    val overlaps = blocks.count { conflictsOf(it, blocks).isNotEmpty() }

    StepScaffold(
        "Vista previa",
        onClose = if (canGoBack) onBack else onClose,
        navIcon = if (canGoBack) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Close,
        bottom = {
            if (blocks.isEmpty()) {
                OutlinedButton(onClick = if (canGoBack) onBack else onClose, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    Text(if (canGoBack) "Volver y corregir" else "Cerrar")
                }
            } else {
                Button(onClick = { onApply(blocks, mode) }, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    Text("Importar ${blocks.size} bloques")
                }
            }
        },
    ) {
        // Resumen
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(source, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    Stat("${blocks.size}", "bloques")
                    Stat("${days.size}", if (days.size == 1) "día" else "días")
                    if (result.errors.isNotEmpty()) Stat("${result.errors.size}", "sin entender", MaterialTheme.colorScheme.error)
                }
                if (blocks.isNotEmpty()) WeekStrip(blocks)
            }
        }

        if (blocks.isEmpty()) {
            Text(
                "No he podido entender ningún bloque. Revisa las líneas de abajo: cada una necesita el día, la hora y la actividad.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (overlaps > 0) {
            Notice("$overlaps bloques se solapan entre sí. Puedes importarlos y ajustarlos después.")
        }
        if (result.errors.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.clickable { showErrors = !showErrors }.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                        Text(
                            if (result.errors.size == 1) "  1 línea no se ha entendido" else "  ${result.errors.size} líneas no se han entendido",
                            style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f),
                        )
                        Text(if (showErrors) "Ocultar" else "Ver", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    }
                    AnimatedVisibility(showErrors) {
                        Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            result.errors.take(30).forEach { e ->
                                Column {
                                    Text(
                                        e.text.trim().ifEmpty { "Línea ${e.line}" }, maxLines = 2, overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                    )
                                    Text(e.reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                }
                            }
                            if (result.errors.size > 30) Text("…y ${result.errors.size - 30} más", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        if (blocks.isNotEmpty()) {
            Text("Cómo aplicarla", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 4.dp))
            val dayList = RoutineText.daysLabel(days)
            ModeOption(mode == ImportMode.REPLACE_ALL, "Sustituir toda la rutina", "La rutina actual se cambia por esta") { mode = ImportMode.REPLACE_ALL }
            if (current.isNotEmpty() && days.size < 7) {
                ModeOption(mode == ImportMode.REPLACE_DAYS, "Sustituir solo esos días", "$dayList. El resto se queda como está") { mode = ImportMode.REPLACE_DAYS }
            }
            if (current.isNotEmpty()) {
                ModeOption(mode == ImportMode.ADD, "Añadir a la rutina actual", "No se borra nada; puede haber solapes") { mode = ImportMode.ADD }
            }

            Text("Bloques", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 4.dp))
            days.forEach { day ->
                val list = blocks.filter { it.dayOfWeek == day }.sortedBy { it.startMin }
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(dayName(day), style = MaterialTheme.typography.titleMedium)
                        list.forEach { b ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(width = 4.dp, height = 28.dp).clip(CircleShape).background(b.category.composeColor))
                                Text(
                                    "${fmtTime(b.startMin)}–${fmtTime(b.endMin)}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 10.dp).width(92.dp),
                                )
                                Text(b.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                Text(b.category.label, style = MaterialTheme.typography.labelSmall, color = b.category.composeColor)
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun Stat(value: String, label: String, color: Color = MaterialTheme.colorScheme.onSurface) {
    Column {
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Notice(text: String) {
    Row(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Warning, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.tertiary)
        Text(text, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 10.dp))
    }
}

@Composable
private fun ModeOption(selected: Boolean, title: String, subtitle: String, onClick: () -> Unit) {
    val border = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    Row(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).border(1.dp, border, MaterialTheme.shapes.medium)
            .clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Column(Modifier.padding(start = 4.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
