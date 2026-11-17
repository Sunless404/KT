package com.sunless404.keeptrack.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sunless404.keeptrack.data.DayEntry
import com.sunless404.keeptrack.domain.Unforeseen
import com.sunless404.keeptrack.domain.fmtDuration
import com.sunless404.keeptrack.domain.fmtRange
import com.sunless404.keeptrack.domain.fmtTime
import com.sunless404.keeptrack.ui.common.TimePickerDialog

/** Cambios de un bloque solo para ese día. */
enum class AdjustKind { MOVE, SHORTEN, DELAY }

/**
 * Ajustes de un bloque solo para hoy: cambiarlo de hora (con huecos sugeridos), acortarlo
 * o retrasarlo junto con lo que viene después. Nada entra en el sueño sin confirmarlo.
 */
@Composable
fun AdjustDialog(ui: TodayUi, entry: DayEntry, kind: AdjustKind, onDismiss: () -> Unit, onApply: (List<DayEntry>) -> Unit) {
    var pickTime by remember { mutableStateOf(false) }
    var needsConfirm by remember { mutableStateOf<List<DayEntry>?>(null) }

    fun submit(changed: List<DayEntry>) {
        if (changed.isEmpty()) return onDismiss()
        if (Unforeseen.entersSleep(ui.entries, ui.carry, changed)) needsConfirm = changed else onApply(changed)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                when (kind) {
                    AdjustKind.MOVE -> "Cambiar hora hoy"
                    AdjustKind.SHORTEN -> "Acortar hoy"
                    AdjustKind.DELAY -> "Retrasar desde aquí"
                }
            )
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "${entry.title} · ${fmtRange(entry.startMin, entry.endMin)}",
                    style = MaterialTheme.typography.titleSmall,
                )
                Text("Solo cambia este día. Tu rutina no se toca.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                when (kind) {
                    AdjustKind.MOVE -> {
                        val from = if (ui.isToday) ui.nowMin else 0
                        val slots = Unforeseen.freeSlots(ui.entries, ui.carry, from, entry.durationMin, entry.id)
                        if (slots.isEmpty()) Text("No queda ningún hueco libre donde quepa. Elige otra hora.")
                        else Text("Huecos libres:", style = MaterialTheme.typography.labelLarge)
                        slots.take(6).forEach { g ->
                            OutlinedButton(onClick = { submit(listOf(Unforeseen.move(entry, g.start))) }, modifier = Modifier.fillMaxWidth()) {
                                Text("${fmtRange(g.start, g.start + entry.durationMin)}  (libre hasta ${fmtTime(g.end)})")
                            }
                        }
                        TextButton(onClick = { pickTime = true }) { Text("Elegir otra hora…") }
                    }

                    AdjustKind.SHORTEN -> {
                        Text("Nueva duración:", style = MaterialTheme.typography.labelLarge)
                        val options = (listOf(5, 10, 15, 30, 45, 60).map { entry.durationMin - it } + entry.durationMin / 2)
                            .filter { it >= 5 }.distinct().sortedDescending()
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            options.forEach { d ->
                                FilterChip(
                                    selected = false,
                                    onClick = { submit(listOf(Unforeseen.shorten(entry, d))) },
                                    label = { Text("${fmtDuration(d)} (hasta ${fmtTime(entry.startMin + d)})") },
                                )
                            }
                        }
                    }

                    AdjustKind.DELAY -> {
                        val affected = Unforeseen.delayFrom(ui.entries, entry, 0)
                        Text(
                            if (affected.size <= 1) "Se retrasa este bloque." else
                                "Se retrasan este bloque y los ${affected.size - 1} siguientes pendientes. El sueño no se mueve.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(5, 10, 15, 30, 45, 60, 90).forEach { n ->
                                FilterChip(
                                    selected = false,
                                    onClick = { submit(Unforeseen.delayFrom(ui.entries, entry, n)) },
                                    label = { Text("$n min") },
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )

    if (pickTime) {
        TimePickerDialog(entry.startMin % 1440, "Nueva hora de inicio", onDismiss = { pickTime = false }) { m ->
            pickTime = false
            submit(listOf(Unforeseen.move(entry, m)))
        }
    }

    needsConfirm?.let { changed ->
        AlertDialog(
            onDismissRequest = { needsConfirm = null },
            title = { Text("Entra en tu hora de sueño") },
            text = {
                Text(
                    "Con este cambio, " + changed.filter {
                        Unforeseen.sleepHits(ui.entries, ui.carry, it.startMin, it.endMin, it.id).isNotEmpty()
                    }.joinToString { "«${it.title}» (${fmtRange(it.startMin, it.endMin)})" } +
                        " quedaría dentro del bloque de sueño. ¿Quieres programarlo igualmente?"
                )
            },
            confirmButton = { TextButton(onClick = { needsConfirm = null; onApply(changed) }) { Text("Sí, programar") } },
            dismissButton = { TextButton(onClick = { needsConfirm = null }) { Text("No") } },
        )
    }
}
