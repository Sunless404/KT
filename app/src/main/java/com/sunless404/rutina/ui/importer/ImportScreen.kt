package com.sunless404.rutina.ui.importer

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.sunless404.rutina.App
import com.sunless404.rutina.data.Block
import com.sunless404.rutina.domain.AiPrompt
import com.sunless404.rutina.domain.Analysis
import com.sunless404.rutina.domain.DAY_SHORT
import com.sunless404.rutina.domain.ImportFormat
import com.sunless404.rutina.domain.ParseResult
import com.sunless404.rutina.domain.RoutineAnalysis
import com.sunless404.rutina.domain.RoutineParser
import com.sunless404.rutina.domain.dayName
import com.sunless404.rutina.domain.fmtDuration
import com.sunless404.rutina.domain.fmtRange
import com.sunless404.rutina.domain.fmtTime
import com.sunless404.rutina.ui.common.ScreenScaffold
import com.sunless404.rutina.ui.common.composeColor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ImportMode(val label: String, val help: String) {
    SUSTITUIR("Sustituir la rutina actual", "Borra toda la plantilla y deja solo lo importado."),
    FUSIONAR("Fusionar", "Añade lo importado a la plantilla actual; los bloques idénticos no se duplican."),
    DIAS("Importar solo algunos días", "Sustituye únicamente los días que elijas."),
}

data class ImportUi(
    val text: String = "",
    val result: ParseResult? = null,
    val analysis: Analysis? = null,
    /** Correcciones de líneas no entendidas: nº de línea → texto nuevo. */
    val fixes: Map<Int, String> = emptyMap(),
    val mode: ImportMode = ImportMode.SUSTITUIR,
    val days: Set<Int> = emptySet(),
    val done: String? = null,
)

class ImportViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as App).repo
    private val _ui = MutableStateFlow(ImportUi())
    val ui = _ui.asStateFlow()

    fun setText(t: String) = _ui.update { it.copy(text = t, result = null, analysis = null, fixes = emptyMap(), done = null) }
    fun setMode(m: ImportMode) = _ui.update { it.copy(mode = m) }
    fun toggleDay(d: Int) = _ui.update { it.copy(days = if (d in it.days) it.days - d else it.days + d) }
    fun setFix(line: Int, text: String) = _ui.update { it.copy(fixes = it.fixes + (line to text)) }

    fun analyze() = _ui.update { s ->
        // Aplica las correcciones sobre el texto original (solo en formato texto).
        val text = if (s.result?.format == ImportFormat.TEXTO && s.fixes.isNotEmpty()) {
            s.text.lines().mapIndexed { i, l -> s.fixes[i + 1] ?: l }.joinToString("\n")
        } else s.text
        val r = RoutineParser.parse(text)
        s.copy(
            text = text, result = r, analysis = RoutineAnalysis.analyze(r.blocks), fixes = emptyMap(),
            days = r.blocks.map { it.dayOfWeek }.toSet(), done = null,
        )
    }

    fun confirm() {
        val s = _ui.value
        val blocks = s.result?.blocks ?: return
        viewModelScope.launch {
            val n: Int
            when (s.mode) {
                ImportMode.SUSTITUIR -> { repo.replaceTemplate(blocks); n = blocks.size }
                ImportMode.DIAS -> {
                    repo.replaceTemplate(blocks, onlyDays = s.days)
                    n = blocks.count { it.dayOfWeek in s.days }
                }
                ImportMode.FUSIONAR -> {
                    val existing = repo.allBlocks().map { it.key() }.toSet()
                    val fresh = blocks.filter { it.key() !in existing }
                    repo.mergeTemplate(fresh)
                    n = fresh.size
                }
            }
            _ui.value = ImportUi(done = "Importados $n bloques.")
        }
    }

    private fun Block.key() = listOf(dayOfWeek, startMin, endMin, title.lowercase())
}

@Composable
fun ImportScreen(vm: ImportViewModel, onBack: () -> Unit) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val clipboard = ctx.getSystemService(ClipboardManager::class.java)

    ScreenScaffold(
        title = "Importar rutina",
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ui.done?.let {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Text(it, Modifier.fillMaxWidth().padding(12.dp))
                }
            }
            Text(
                "Pega tu horario como texto (un día por cabecera y una línea «HH:MM-HH:MM Título [categoría]» " +
                    "por bloque), una tabla Markdown o un JSON. El formato se detecta solo.",
                style = MaterialTheme.typography.bodySmall,
            )
            OutlinedTextField(
                value = ui.text,
                onValueChange = vm::setText,
                modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp, max = 320.dp),
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                placeholder = { Text("Lunes\n08:00-09:00 DIW [clase]\n12:30-12:45 Vuelta a casa") },
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(enabled = ui.text.isNotBlank(), onClick = vm::analyze) { Text("Analizar") }
                OutlinedButton(onClick = {
                    clipboard.primaryClip?.getItemAt(0)?.coerceToText(ctx)?.toString()?.let(vm::setText)
                }) { Text("Pegar") }
                OutlinedButton(onClick = {
                    clipboard.setPrimaryClip(ClipData.newPlainText("Prompt para IA", AiPrompt.TEXT))
                    Toast.makeText(ctx, "Prompt copiado. Pégalo en tu asistente y añade tus horarios.", Toast.LENGTH_LONG).show()
                }) { Text("Copiar prompt para IA") }
            }

            val r = ui.result
            val a = ui.analysis
            if (r != null && a != null) Preview(ui, r, a, vm)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Preview(ui: ImportUi, r: ParseResult, a: Analysis, vm: ImportViewModel) {
    val error = MaterialTheme.colorScheme.error
    HorizontalDivider()
    Text("Vista previa", style = MaterialTheme.typography.titleMedium)
    Text(
        "Formato detectado: ${r.format.label} · ${r.blocks.size} bloques reconocidos · ${r.errors.size} sin entender",
        style = MaterialTheme.typography.bodySmall,
    )

    // Líneas no entendidas
    if (r.errors.isNotEmpty()) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Líneas no entendidas", style = MaterialTheme.typography.titleSmall)
                r.errors.forEach { e ->
                    if (r.format == ImportFormat.TEXTO) {
                        OutlinedTextField(
                            value = ui.fixes[e.line] ?: e.text,
                            onValueChange = { vm.setFix(e.line, it) },
                            label = { Text("Línea ${e.line}") },
                            supportingText = { Text(e.reason) },
                            isError = true,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        Text(e.reason, color = error, style = MaterialTheme.typography.bodyMedium)
                        if (e.text.isNotBlank()) Text(e.text, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                    }
                }
                if (r.format == ImportFormat.TEXTO) {
                    Text("Corrige las líneas (o déjalas vacías para descartarlas) y vuelve a analizar.", style = MaterialTheme.typography.bodySmall)
                    Button(onClick = vm::analyze) { Text("Volver a analizar") }
                } else {
                    Text("Corrige el JSON arriba y vuelve a analizar.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }

    // Bloques por día
    (1..7).forEach { day ->
        val list = r.blocks.filter { it.dayOfWeek == day }.sortedBy { it.startMin }
        if (list.isEmpty()) return@forEach
        Text("${dayName(day)} · ${list.size} bloques", style = MaterialTheme.typography.titleSmall)
        list.forEach { b ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(b.category.composeColor))
                Text(
                    "  ${fmtRange(b.startMin, b.endMin)}${if (b.crossesMidnight) " (+1)" else ""}  ${b.title} · ${b.category.label}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        a.gaps[day]?.takeIf { it.isNotEmpty() }?.let { gaps ->
            Text(
                "Huecos sin asignar: " + gaps.joinToString { "${fmtTime(it.start)}–${fmtTime(it.end)}" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (a.overlaps.isNotEmpty()) {
        Text("Solapamientos", style = MaterialTheme.typography.titleSmall, color = error)
        a.overlaps.forEach { (x, y) ->
            Text(
                "${DAY_SHORT[x.dayOfWeek - 1]} ${fmtRange(x.startMin, x.endMin)} ${x.title}  ↔  " +
                    "${DAY_SHORT[y.dayOfWeek - 1]} ${fmtRange(y.startMin, y.endMin)} ${y.title}",
                style = MaterialTheme.typography.bodySmall, color = error,
            )
        }
    }

    Text("Sueño por noche", style = MaterialTheme.typography.titleSmall)
    if (a.sleepByNight.isEmpty()) {
        Text("No hay bloques de sueño en lo importado.", style = MaterialTheme.typography.bodySmall)
    } else {
        Text(
            a.sleepByNight.entries.joinToString("\n") { (d, m) -> "Noche del ${dayName(d).lowercase()}: ${fmtDuration(m)}" },
            style = MaterialTheme.typography.bodySmall,
        )
    }

    if (r.blocks.isEmpty()) return

    HorizontalDivider()
    Text("Cómo importar", style = MaterialTheme.typography.titleMedium)
    ImportMode.entries.forEach { m ->
        Row(
            Modifier.fillMaxWidth().selectable(selected = ui.mode == m, onClick = { vm.setMode(m) }),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = ui.mode == m, onClick = { vm.setMode(m) })
            Column {
                Text(m.label)
                Text(m.help, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    if (ui.mode == ImportMode.DIAS) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            r.blocks.map { it.dayOfWeek }.distinct().sorted().forEach { d ->
                FilterChip(selected = d in ui.days, onClick = { vm.toggleDay(d) }, label = { Text(dayName(d)) })
            }
        }
    }
    Button(
        enabled = ui.mode != ImportMode.DIAS || ui.days.isNotEmpty(),
        onClick = vm::confirm,
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Confirmar importación") }
    Text("El historial de días pasados no cambia.", style = MaterialTheme.typography.bodySmall)
}
