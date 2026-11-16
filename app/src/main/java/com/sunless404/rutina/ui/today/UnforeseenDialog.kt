package com.sunless404.rutina.ui.today

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
import com.sunless404.rutina.data.Category
import com.sunless404.rutina.data.DayEntry
import com.sunless404.rutina.data.Status
import com.sunless404.rutina.domain.Unforeseen
import com.sunless404.rutina.domain.fmtDuration
import com.sunless404.rutina.domain.fmtRange
import com.sunless404.rutina.domain.fmtTime
import com.sunless404.rutina.ui.common.TimePickerDialog

private enum class Step { MENU, MOVE, SHORTEN, DELAY }

/**
 * Imprevistos de hoy: mover un bloque a un hueco, acortarlo o retrasar lo que queda.
 * Nada entra en el bloque de sueño sin una confirmación explícita.
 */
@Composable
fun UnforeseenDialog(ui: TodayUi, onDismiss: () -> Unit, onApply: (List<DayEntry>) -> Unit) {
    var step by remember { mutableStateOf(Step.MENU) }
    var selected by remember { mutableStateOf<DayEntry?>(null) }
    var pickTime by remember { mutableStateOf(false) }
    // Cambios que pisan el sueño y esperan confirmación.
    var needsConfirm by remember { mutableStateOf<List<DayEntry>?>(null) }

    val open = ui.entries.filter { it.status == Status.PENDIENTE || it.status == Status.POSPUESTO }
    val movable = open.filter { it.category != Category.SUENO }
    val shortenable = open.filter { it.endMin > ui.nowMin && it.durationMin > 5 }

    fun submit(changed: List<DayEntry>) {
        if (changed.isEmpty()) return onDismiss()
        if (Unforeseen.entersSleep(ui.entries, ui.carry, changed)) needsConfirm = changed else onApply(changed)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                when (step) {
                    Step.MENU -> "Imprevisto"
                    Step.MOVE -> "Mover un bloque"
                    Step.SHORTEN -> "Acortar un bloque"
                    Step.DELAY -> "Retrasar lo que queda"
                }
            )
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                when (step) {
                    Step.MENU -> {
                        Text("Solo cambia el día de hoy. La plantilla semanal no se toca.", style = MaterialTheme.typography.bodySmall)
                        OutlinedButton(onClick = { step = Step.MOVE }, modifier = Modifier.fillMaxWidth()) { Text("Mover un bloque a otro hueco") }
                        OutlinedButton(onClick = { step = Step.SHORTEN }, modifier = Modifier.fillMaxWidth()) { Text("Acortar un bloque") }
                        OutlinedButton(onClick = { step = Step.DELAY }, modifier = Modifier.fillMaxWidth()) { Text("Retrasar todo lo que queda") }
                    }

                    Step.MOVE -> {
                        val sel = selected
                        if (sel == null) {
                            EntryPicker(movable) { selected = it }
                        } else {
                            Text("${sel.title} · ${fmtDuration(sel.durationMin)}", style = MaterialTheme.typography.titleSmall)
                            val slots = Unforeseen.freeSlots(ui.entries, ui.carry, ui.nowMin, sel.durationMin, sel.id)
                            if (slots.isEmpty()) Text("No queda ningún hueco libre hoy donde quepa.")
                            else Text("Huecos libres sugeridos:", style = MaterialTheme.typography.labelLarge)
                            slots.forEach { g ->
                                OutlinedButton(
                                    onClick = { submit(listOf(Unforeseen.move(sel, g.start))) },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(
                                        "${fmtRange(g.start, g.start + sel.durationMin)}  (hueco ${fmtTime(g.start)}–${fmtTime(g.end)})"
                                    )
                                }
                            }
                            TextButton(onClick = { pickTime = true }) { Text("Otra hora…") }
                        }
                    }

                    Step.SHORTEN -> {
                        val sel = selected
                        if (sel == null) {
                            EntryPicker(shortenable) { selected = it }
                        } else {
                            Text(
                                "${sel.title} · ahora dura ${fmtDuration(sel.durationMin)}",
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text("Nueva duración:", style = MaterialTheme.typography.labelLarge)
                            val options = (listOf(5, 10, 15, 30, 45, 60).map { sel.durationMin - it } + sel.durationMin / 2)
                                .filter { it >= 5 }.distinct().sortedDescending()
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                options.forEach { d ->
                                    FilterChip(
                                        selected = false,
                                        onClick = { submit(listOf(Unforeseen.shorten(sel, d))) },
                                        label = { Text("${fmtDuration(d)} (hasta ${fmtTime(sel.startMin + d)})") },
                                    )
                                }
                            }
                        }
                    }

                    Step.DELAY -> {
                        val rest = Unforeseen.remaining(ui.entries, ui.nowMin)
                        if (rest.isEmpty()) {
                            Text("No queda ningún bloque pendiente por empezar hoy.")
                        } else {
                            Text(
                                "Se retrasan ${rest.size} bloques pendientes: " + rest.joinToString { it.title } +
                                    ". El sueño no se mueve.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(5, 10, 15, 30, 45, 60, 90).forEach { n ->
                                    FilterChip(
                                        selected = false,
                                        onClick = { submit(Unforeseen.delayRest(ui.entries, ui.nowMin, n)) },
                                        label = { Text("$n min") },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } },
        dismissButton = {
            if (step != Step.MENU) TextButton(onClick = {
                if (selected != null) selected = null else step = Step.MENU
            }) { Text("Atrás") }
        },
    )

    if (pickTime) {
        val sel = selected
        TimePickerDialog(sel?.startMin?.rem(1440) ?: 0, "Nueva hora de inicio", onDismiss = { pickTime = false }) { m ->
            pickTime = false
            if (sel != null) submit(listOf(Unforeseen.move(sel, m)))
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
            confirmButton = {
                TextButton(onClick = { needsConfirm = null; onApply(changed) }) { Text("Sí, programar") }
            },
            dismissButton = { TextButton(onClick = { needsConfirm = null }) { Text("No") } },
        )
    }
}

@Composable
private fun EntryPicker(entries: List<DayEntry>, onPick: (DayEntry) -> Unit) {
    if (entries.isEmpty()) {
        Text("No hay bloques pendientes que se puedan cambiar.")
        return
    }
    Text("Elige el bloque:", style = MaterialTheme.typography.labelLarge)
    entries.forEach { e ->
        OutlinedButton(onClick = { onPick(e) }, modifier = Modifier.fillMaxWidth()) {
            Text("${fmtRange(e.startMin, e.endMin)}  ${e.title}")
        }
    }
}
