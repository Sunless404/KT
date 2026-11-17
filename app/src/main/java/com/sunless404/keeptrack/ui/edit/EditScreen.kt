package com.sunless404.keeptrack.ui.edit

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.sunless404.keeptrack.App
import com.sunless404.keeptrack.data.Block
import com.sunless404.keeptrack.data.Category
import com.sunless404.keeptrack.domain.DAY_NAMES
import com.sunless404.keeptrack.domain.DAY_SHORT
import com.sunless404.keeptrack.domain.conflictsOf
import com.sunless404.keeptrack.domain.dayName
import com.sunless404.keeptrack.domain.fmtDuration
import com.sunless404.keeptrack.domain.fmtRange
import com.sunless404.keeptrack.domain.fmtTime
import com.sunless404.keeptrack.ui.common.ScreenScaffold
import com.sunless404.keeptrack.ui.common.TimePickerDialog
import com.sunless404.keeptrack.ui.common.composeColor
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class EditViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as App).repo

    val blocks = repo.observeBlocks().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun save(block: Block, days: Set<Int>) = viewModelScope.launch { repo.saveBlockForDays(block, days) }
    fun delete(id: Long) = viewModelScope.launch { repo.deleteBlock(id) }
    fun duplicate(id: Long) = viewModelScope.launch { repo.duplicateBlock(id) }
    fun copyDay(from: Int, to: Int) = viewModelScope.launch { repo.copyDay(from, to) }

    /** Rutina anterior a la última importación, para poder deshacerla. */
    data class ImportUndo(val count: Int, val before: List<Block>)
    val importUndo = kotlinx.coroutines.flow.MutableStateFlow<ImportUndo?>(null)

    fun applyImport(blocks: List<Block>, mode: ImportMode) = viewModelScope.launch {
        val before = repo.allBlocks()
        when (mode) {
            ImportMode.REPLACE_ALL -> repo.replaceTemplate(blocks)
            ImportMode.REPLACE_DAYS -> repo.replaceTemplate(blocks, blocks.map { it.dayOfWeek }.toSet())
            ImportMode.ADD -> repo.mergeTemplate(blocks)
        }
        importUndo.value = ImportUndo(blocks.size, before)
    }

    fun undoImport() = viewModelScope.launch {
        val u = importUndo.value ?: return@launch
        importUndo.value = null
        repo.replaceTemplate(u.before)
    }
}

@Composable
internal fun BlockEditorDialog(
    initial: Block,
    all: List<Block>,
    onDismiss: () -> Unit,
    onSave: (Block, Set<Int>) -> Unit,
) {
    var title by remember { mutableStateOf(initial.title) }
    var category by remember { mutableStateOf(initial.category) }
    var start by remember { mutableIntStateOf(initial.startMin) }
    var end by remember { mutableIntStateOf(initial.endMin) }
    var days by remember { mutableStateOf(setOf(initial.dayOfWeek)) }
    var notes by remember { mutableStateOf(initial.notes) }
    var notify by remember { mutableStateOf(if (initial.id == 0L) category.notifiesByDefault else initial.notify) }
    var lead by remember { mutableStateOf(initial.leadMin?.toString() ?: "") }
    var picking by remember { mutableStateOf(0) } // 1 = inicio, 2 = fin

    val candidate = initial.copy(
        title = title.trim(), category = category, startMin = start, endMin = end,
        notes = notes.trim(), notify = notify, leadMin = lead.toIntOrNull()?.coerceIn(0, 180),
    )
    val conflicts = days.flatMap { d -> conflictsOf(candidate.copy(dayOfWeek = d), all) }.distinctBy { it.id }
    val valid = title.isNotBlank() && days.isNotEmpty() && start != end

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id == 0L) "Nuevo bloque" else "Modificar bloque") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    label = { Text("Título") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { picking = 1 }) { Text("Inicio ${fmtTime(start)}") }
                    OutlinedButton(onClick = { picking = 2 }) { Text("Fin ${fmtTime(end)}") }
                }
                if (start != end) {
                    Text(
                        "Duración: ${fmtDuration(candidate.durationMin)}" +
                            if (candidate.crossesMidnight) " · termina al día siguiente" else "",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Text("Categoría", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Category.entries.forEach { c ->
                        FilterChip(
                            selected = category == c,
                            onClick = {
                                category = c
                                if (initial.id == 0L) notify = c.notifiesByDefault
                            },
                            label = { Text(c.label) },
                        )
                    }
                }
                Text("Días", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    DAY_SHORT.forEachIndexed { i, s ->
                        val d = i + 1
                        FilterChip(
                            selected = d in days,
                            onClick = { days = if (d in days) days - d else days + d },
                            label = { Text(s) },
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Aviso", modifier = Modifier.weight(1f))
                    Switch(checked = notify, onCheckedChange = { notify = it })
                }
                if (notify) {
                    OutlinedTextField(
                        value = lead, onValueChange = { v -> lead = v.filter { it.isDigit() }.take(3) },
                        label = { Text("Minutos de antelación") },
                        placeholder = { Text("Por defecto (Ajustes)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                    )
                }
                OutlinedTextField(
                    value = notes, onValueChange = { notes = it },
                    label = { Text("Notas (opcional)") }, modifier = Modifier.fillMaxWidth(),
                )
                if (conflicts.isNotEmpty()) {
                    Text(
                        "⚠ Se solapa con: " + conflicts.joinToString {
                            "${it.title} (${DAY_SHORT[it.dayOfWeek - 1]} ${fmtRange(it.startMin, it.endMin)})"
                        } + ". Puedes guardarlo igualmente.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = { TextButton(enabled = valid, onClick = { onSave(candidate, days) }) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )

    if (picking != 0) {
        TimePickerDialog(
            initialMin = if (picking == 1) start else end,
            title = if (picking == 1) "Hora de inicio" else "Hora de fin",
            onDismiss = { picking = 0 },
        ) { m ->
            if (picking == 1) start = m else end = m
            picking = 0
        }
    }
}

@Composable
internal fun CopyDayDialog(from: Int, onDismiss: () -> Unit, onConfirm: (Set<Int>) -> Unit) {
    var targets by remember { mutableStateOf(emptySet<Int>()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Copiar ${dayName(from).lowercase()} en…") },
        text = {
            Column {
                Text("Los bloques de los días elegidos se sustituyen por los del ${dayName(from).lowercase()}.")
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    (1..7).filter { it != from }.forEach { d ->
                        FilterChip(
                            selected = d in targets,
                            onClick = { targets = if (d in targets) targets - d else targets + d },
                            label = { Text(dayName(d)) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = targets.isNotEmpty(), onClick = { onConfirm(targets) }) { Text("Copiar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
