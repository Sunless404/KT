package com.sunless404.keeptrack.ui.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sunless404.keeptrack.data.Block
import com.sunless404.keeptrack.data.Category
import com.sunless404.keeptrack.domain.ReflowMode
import com.sunless404.keeptrack.domain.ReflowResult
import com.sunless404.keeptrack.domain.RoutineReflow
import com.sunless404.keeptrack.domain.dayName
import com.sunless404.keeptrack.domain.fmtDuration
import com.sunless404.keeptrack.domain.fmtRange
import com.sunless404.keeptrack.domain.overlaps
import com.sunless404.keeptrack.ui.common.KtIcons
import com.sunless404.keeptrack.ui.common.composeColor

/** Hora a la que termina el sueño de la noche anterior a [day] (0 si no hay). */
internal fun morningOf(all: List<Block>, day: Int): Int {
    val prev = (day + 5) % 7 + 1
    return all.filter { it.dayOfWeek == prev && it.category == Category.SUENO && it.crossesMidnight }.maxOfOrNull { it.endMin } ?: 0
}

/** Borrar un bloque eligiendo qué hacer con su tiempo, con vista previa del día. */
@Composable
fun DeleteReflowDialog(block: Block, all: List<Block>, onDismiss: () -> Unit, onConfirm: (List<Block>) -> Unit) {
    val day = all.filter { it.dayOfWeek == block.dayOfWeek }
    var mode by remember { mutableStateOf(if (block.fixed) ReflowMode.KEEP_GAP else ReflowMode.SPREAD) }
    val result = remember(mode) { RoutineReflow.remove(day, block, mode, morningOf(all, block.dayOfWeek)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Borrar «${block.title}»") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Quedan libres ${fmtDuration(block.durationMin)} el ${dayName(block.dayOfWeek).lowercase()}. ¿Qué hago con ese tiempo?",
                    style = MaterialTheme.typography.bodyMedium,
                )
                ReflowMode.entries.forEach { m ->
                    Option(m == mode, m.label, m.description) { mode = m }
                }
                Preview(listOf(block.dayOfWeek to result))
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(result.blocks) }) { Text("Borrar", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

/**
 * Al añadir o cambiar un bloque que pisa a otros: propone hacerle sitio en cada día
 * (recortando o moviendo los flexibles) o guardarlo tal cual.
 */
@Composable
fun PlaceReflowDialog(results: List<Pair<Int, ReflowResult>>, onDismiss: () -> Unit, onApply: () -> Unit, onPlain: () -> Unit) {
    val ok = results.all { it.second.ok }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (ok) "Hacer sitio" else "No cabe") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (ok) "Se solapa con otros bloques. Así quedaría haciéndole sitio sin tocar los fijos ni bajar de 8 h de sueño:"
                    else "No se puede hacer sitio sin tocar los bloques fijos o sin bajar de 8 h de sueño.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Preview(results)
            }
        },
        confirmButton = {
            if (ok) TextButton(onClick = onApply) { Text("Aplicar ajuste") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDismiss) { Text("Cancelar") }
                TextButton(onClick = onPlain) { Text("Guardar sin ajustar") }
            }
        },
    )
}

/** Cálculo del reajuste al guardar [candidate] en [days]; null si no pisa nada en ningún día. */
internal fun placeResults(candidate: Block, days: Set<Int>, all: List<Block>): List<Pair<Int, ReflowResult>>? {
    val results = days.sorted().map { d ->
        val b = if (d == candidate.dayOfWeek) candidate else candidate.copy(id = 0, dayOfWeek = d)
        val dayBlocks = all.filter { it.dayOfWeek == d }
        val others = dayBlocks.filter { b.id == 0L || it.id != b.id }
        d to (if (others.any { overlaps(it, b) }) RoutineReflow.place(dayBlocks, b, morningOf(all, d)) else null)
    }
    if (results.all { it.second == null }) return null
    return results.mapNotNull { (d, r) -> r?.let { d to it } }
}

@Composable
private fun Option(selected: Boolean, title: String, subtitle: String, onClick: () -> Unit) {
    val border = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    Row(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).border(1.dp, border, MaterialTheme.shapes.medium)
            .clickable(onClick = onClick).padding(end = 8.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Lista de cambios por día: hora antigua tachada y la nueva. */
@Composable
private fun Preview(results: List<Pair<Int, ReflowResult>>) {
    Column(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(12.dp).heightIn(max = 320.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        results.forEach { (day, r) ->
            if (results.size > 1) Text(dayName(day), style = MaterialTheme.typography.labelLarge)
            when {
                !r.ok -> Text(r.reason ?: "No cabe", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                r.changed.isEmpty() -> Text("No se mueve ningún otro bloque.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> r.changed.forEach { (old, new) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(new.category.composeColor))
                        Text(
                            new.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(start = 8.dp),
                        )
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                fmtRange(old.startMin, old.endMin), style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                textDecoration = TextDecoration.LineThrough, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(fmtRange(new.startMin, new.endMin), style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Monospace))
                        }
                    }
                }
            }
        }
    }
}
