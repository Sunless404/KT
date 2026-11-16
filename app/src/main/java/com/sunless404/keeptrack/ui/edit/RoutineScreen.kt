package com.sunless404.keeptrack.ui.edit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sunless404.keeptrack.data.Block
import com.sunless404.keeptrack.data.Category
import com.sunless404.keeptrack.domain.conflictsOf
import com.sunless404.keeptrack.domain.dayName
import com.sunless404.keeptrack.domain.fmtDuration
import com.sunless404.keeptrack.domain.fmtTime
import com.sunless404.keeptrack.ui.common.DayStrip
import com.sunless404.keeptrack.ui.common.KtIcons
import com.sunless404.keeptrack.ui.common.composeColor
import java.time.LocalDate

/** Rutina semanal: lista de lunes a domingo; cada día se despliega con su línea de tiempo. */
@Composable
fun RoutineScreen(vm: EditViewModel) {
    val blocks by vm.blocks.collectAsStateWithLifecycle()
    val today = LocalDate.now().dayOfWeek.value
    var expanded by rememberSaveable { mutableStateOf(setOf(today)) }
    var editing by remember { mutableStateOf<Block?>(null) }
    var copyFrom by remember { mutableStateOf<Int?>(null) }
    var toDelete by remember { mutableStateOf<Block?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Column(Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)) {
                Text("Rutina", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Tu plantilla semanal. Toca un día para verla y editarla.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items((1..7).toList(), key = { it }) { day ->
            val list = blocks.filter { it.dayOfWeek == day }.sortedBy { it.startMin }
            DayCard(
                day = day,
                isToday = day == today,
                blocks = list,
                all = blocks,
                open = day in expanded,
                onToggle = { expanded = if (day in expanded) expanded - day else expanded + day },
                onAdd = {
                    val start = list.filter { !it.crossesMidnight }.maxOfOrNull { it.endMin } ?: (8 * 60)
                    editing = Block(
                        title = "", category = Category.OTRO, dayOfWeek = day,
                        startMin = start.coerceAtMost(23 * 60), endMin = (start + 60).coerceAtMost(1439),
                    )
                },
                onEdit = { editing = it },
                onDuplicate = { vm.duplicate(it.id) },
                onDelete = { toDelete = it },
                onCopy = { copyFrom = day },
            )
        }
    }

    editing?.let { b ->
        BlockEditorDialog(b, blocks, onDismiss = { editing = null }) { block, days ->
            vm.save(block, days)
            editing = null
        }
    }
    toDelete?.let { b ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Borrar bloque") },
            text = { Text("¿Borrar «${b.title}» del ${dayName(b.dayOfWeek).lowercase()}? El historial de días pasados no cambia.") },
            confirmButton = { TextButton(onClick = { vm.delete(b.id); toDelete = null }) { Text("Borrar") } },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Cancelar") } },
        )
    }
    copyFrom?.let { from ->
        CopyDayDialog(from = from, onDismiss = { copyFrom = null }) { targets ->
            targets.forEach { vm.copyDay(from, it) }
            copyFrom = null
        }
    }
}

@Composable
private fun DayCard(
    day: Int,
    isToday: Boolean,
    blocks: List<Block>,
    all: List<Block>,
    open: Boolean,
    onToggle: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (Block) -> Unit,
    onDuplicate: (Block) -> Unit,
    onDelete: (Block) -> Unit,
    onCopy: () -> Unit,
) {
    val arrow by animateFloatAsState(if (open) 180f else 0f, label = "flecha")
    val totals = blocks.groupBy { it.category }.mapValues { (_, v) -> v.sumOf { it.durationMin } }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isToday) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainer,
        ),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.clickable(onClick = onToggle).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(dayName(day), style = MaterialTheme.typography.titleLarge)
                        if (isToday) {
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "HOY",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondary,
                                modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.secondary).padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                    }
                    Text(
                        if (blocks.isEmpty()) "Sin bloques" else
                            "${blocks.size} bloques · " + totals.entries.sortedByDescending { it.value }
                                .filter { it.key != Category.SUENO && it.key != Category.LIBRE }.take(2)
                                .joinToString { "${it.key.label.lowercase()} ${fmtDuration(it.value)}" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
                Icon(Icons.Default.KeyboardArrowDown, if (open) "Plegar" else "Desplegar", Modifier.rotate(arrow))
            }
            Spacer(Modifier.height(10.dp))
            DayStrip(blocks.map { it.toEntry(0) }, height = 8.dp)
        }
        AnimatedVisibility(visible = open, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Column(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                blocks.forEach { b ->
                    BlockRow(b, conflicts = conflictsOf(b, all), onEdit = { onEdit(b) }, onDuplicate = { onDuplicate(b) }, onDelete = { onDelete(b) })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                    FilledTonalButton(onClick = onAdd, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                        Text(" Añadir bloque")
                    }
                    OutlinedButton(onClick = onCopy) { Text("Copiar día") }
                }
            }
        }
    }
}

@Composable
private fun BlockRow(b: Block, conflicts: List<Block>, onEdit: () -> Unit, onDuplicate: () -> Unit, onDelete: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().height(IntrinsicSize.Min).clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.5f)).clickable(onClick = onEdit),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(5.dp).fillMaxHeight().background(b.category.composeColor))
        Column(Modifier.width(64.dp).padding(start = 10.dp, top = 10.dp, bottom = 10.dp)) {
            Text(fmtTime(b.startMin), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Text(
                fmtTime(b.endMin) + if (b.crossesMidnight) "⁺¹" else "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
            Text(b.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(b.category.composeColor))
                Text(
                    "  ${b.category.label} · ${fmtDuration(b.durationMin)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (b.notify) {
                    Spacer(Modifier.width(6.dp))
                    Icon(KtIcons.Bell, "Con aviso", Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (conflicts.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                    Text(
                        " Se solapa con " + conflicts.joinToString { it.title },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        Box {
            IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, "Más") }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text("Modificar") }, onClick = { menu = false; onEdit() })
                DropdownMenuItem(text = { Text("Duplicar") }, onClick = { menu = false; onDuplicate() })
                DropdownMenuItem(text = { Text("Borrar") }, onClick = { menu = false; onDelete() })
            }
        }
    }
}
