package com.sunless404.keeptrack.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sunless404.keeptrack.data.Repeat
import com.sunless404.keeptrack.data.Task
import com.sunless404.keeptrack.domain.fmtLongDate
import com.sunless404.keeptrack.domain.fmtTime
import com.sunless404.keeptrack.ui.common.KtIcons
import com.sunless404.keeptrack.ui.common.WheelTimePicker
import androidx.compose.foundation.border
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/** Momento del recordatorio en epoch millis. */
fun remindAtMillis(date: LocalDate, minuteOfDay: Int): Long =
    date.atStartOfDay(ZoneId.systemDefault()).plusMinutes(minuteOfDay.toLong()).toInstant().toEpochMilli()

/** Color de la prioridad alta. */
val HighPriority = Color(0xFFE5484D)

/**
 * Una opción de recordatorio. [offsetMin]: minutos antes de la hora de inicio (con hora) o
 * minuto del día del aviso, [dayBefore] si es el día anterior (sin hora).
 */
private data class Remind(val label: String, val offsetMin: Int, val relative: Boolean, val dayBefore: Boolean = false)

private val withTime = listOf(Remind("1 h antes", 60, true), Remind("30 min antes", 30, true), Remind("5 min antes", 5, true))

/** Sin hora no hay referencia para "antes": solo "Sin aviso" u "Otra hora". */
private val allDay = emptyList<Remind>()

private fun Remind.at(date: LocalDate, start: Int?): Long =
    if (relative && start != null) remindAtMillis(date, start) - offsetMin * 60_000L
    else remindAtMillis(if (dayBefore) date.minusDays(1) else date, offsetMin)

/**
 * Crear o editar un compromiso. Solo el título y la fecha son obligatorios:
 * sin hora es de todo el día; con inicio y sin fin, se marca al acabar.
 */
@OptIn(ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun TaskEditorDialog(initial: Task, onDismiss: () -> Unit, onDelete: (() -> Unit)? = null, onSave: (Task) -> Unit) {
    val isNew = initial.id == 0L
    val today = LocalDate.now()
    var title by remember { mutableStateOf(initial.title) }
    var date by remember { mutableStateOf(LocalDate.ofEpochDay(initial.epochDay)) }
    var start by remember { mutableStateOf(initial.timeMin) }
    var end by remember { mutableStateOf(initial.endMin) }
    var notes by remember { mutableStateOf(initial.description) }
    var repeat by remember { mutableStateOf(initial.repeat) }
    var high by remember { mutableStateOf(initial.highPriority) }
    var remindAt by remember { mutableStateOf(if (initial.notify) initial.remindAt else null) }
    var picking by remember { mutableStateOf("") } // date
    var wheel by remember { mutableStateOf("") } // start, end, remind: rueda abierta en el formulario
    var confirmDelete by remember { mutableStateOf(false) }

    val options = if (start != null) withTime else allDay
    val chosen = remindAt?.let { r -> options.firstOrNull { it.at(date, start) == r } }
    // Al cambiar fecha u hora, el recordatorio elegido se recoloca.
    var lastChoice by remember { mutableStateOf(chosen) }
    LaunchedEffect(date, start != null, start) {
        lastChoice?.let { c ->
            val o = if (c in options) c else options.firstOrNull()
            lastChoice = o
            remindAt = o?.at(date, start)
        }
    }

    val valid = title.isNotBlank()
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { if (isNew) runCatching { focus.requestFocus() } }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars).imePadding()) {
                // Barra superior
                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Cerrar") }
                    Text(
                        if (isNew) "Nuevo compromiso" else "Editar compromiso",
                        style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f).padding(start = 4.dp),
                    )
                    Button(
                        enabled = valid,
                        onClick = {
                            onSave(
                                initial.copy(
                                    title = title.trim(),
                                    epochDay = date.toEpochDay(),
                                    timeMin = start,
                                    endMin = if (start != null) end else null,
                                    description = notes.trim(),
                                    notify = remindAt != null,
                                    remindAt = remindAt,
                                    repeat = repeat,
                                    priority = if (high) 1 else 0,
                                )
                            )
                        },
                        modifier = Modifier.padding(end = 8.dp),
                    ) { Text("Guardar") }
                }

                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    TextField(
                        value = title, onValueChange = { title = it },
                        placeholder = { Text("Título", style = MaterialTheme.typography.headlineSmall) },
                        textStyle = MaterialTheme.typography.headlineSmall,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                        ),
                        modifier = Modifier.fillMaxWidth().padding(start = 40.dp, end = 12.dp).focusRequester(focus),
                    )
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))

                    // Fecha
                    Section(KtIcons.Calendar) {
                        Text(
                            fmtLongDate(date), style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.clip(MaterialTheme.shapes.small).clickable { picking = "date" }.padding(vertical = 4.dp),
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Pill("Hoy", date == today) { date = today }
                            Pill("Mañana", date == today.plusDays(1)) { date = today.plusDays(1) }
                            Pill("Otra fecha", date != today && date != today.plusDays(1)) { picking = "date" }
                        }
                    }

                    // Hora
                    Section(KtIcons.Clock) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            TimeChip("Inicio", start, active = wheel == "start", onClick = {
                                if (start == null) start = ((java.time.LocalTime.now().hour + 1) % 24) * 60
                                wheel = if (wheel == "start") "" else "start"
                            }, onClear = { start = null; end = null; wheel = "" })
                            if (start != null) {
                                Text("–", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                TimeChip("Fin", end, active = wheel == "end", onClick = {
                                    if (end == null) end = (start!! + 60) % 1440
                                    wheel = if (wheel == "end") "" else "end"
                                }, onClear = { end = null; if (wheel == "end") wheel = "" })
                            }
                        }
                        androidx.compose.animation.AnimatedVisibility(wheel == "start" || wheel == "end") {
                            WheelPanel(
                                minute = (if (wheel == "end") end else start) ?: 600,
                                onChange = { m ->
                                    if (wheel == "end") end = m
                                    else {
                                        // Mantiene la duración si ya había fin.
                                        val old = start
                                        start = m
                                        end?.let { e -> if (old != null) end = ((e - old + 1440) % 1440 + m) % 1440 }
                                    }
                                },
                                onDone = { wheel = "" },
                            )
                        }
                        Text(
                            when {
                                start == null -> "Sin hora: todo el día"
                                end == null -> "Sin hora de fin: márcalo cuando acabes"
                                else -> "De ${fmtTime(start!!)} a ${fmtTime(end!!)}"
                            },
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    // Recordatorio
                    Section(KtIcons.Bell) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                            Pill("Sin aviso", remindAt == null) { remindAt = null; lastChoice = null }
                            options.forEach { o ->
                                Pill(o.label, chosen == o) { remindAt = o.at(date, start); lastChoice = o }
                            }
                            Pill(
                                if (remindAt != null && chosen == null) "Otra: " + describe(remindAt!!, date) else "Otra hora",
                                remindAt != null && chosen == null,
                            ) {
                                if (remindAt == null || chosen != null) {
                                    remindAt = remindAtMillis(date, start?.let { (it - 15 + 1440) % 1440 } ?: (9 * 60))
                                    lastChoice = null
                                }
                                wheel = if (wheel == "remind") "" else "remind"
                            }
                        }
                        androidx.compose.animation.AnimatedVisibility(wheel == "remind" && remindAt != null && chosen == null) {
                            val z = remindAt?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()) }
                            WheelPanel(
                                minute = z?.let { it.hour * 60 + it.minute } ?: 540,
                                caption = "Aviso el ${fmtLongDate(date).lowercase()}",
                                onChange = { m -> remindAt = remindAtMillis(date, m); lastChoice = null },
                                onDone = { wheel = "" },
                            )
                        }
                    }

                    // Repetir
                    Section(KtIcons.Repeat) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Repeat.labels.forEach { (mode, label) -> Pill(label, repeat == mode) { repeat = mode } }
                        }
                        if (repeat != Repeat.NONE) {
                            Text(
                                "Al marcarlo como hecho aparece el siguiente.",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    // Prioridad
                    Section(KtIcons.Flag, tint = if (high) HighPriority else null) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { high = !high }) {
                            Column(Modifier.weight(1f)) {
                                Text("Prioridad alta", style = MaterialTheme.typography.bodyLarge)
                                Text("Se destaca en Hoy y en la Agenda", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(checked = high, onCheckedChange = { high = it })
                        }
                    }

                    // Notas
                    Section(KtIcons.Notes) {
                        TextField(
                            value = notes, onValueChange = { notes = it },
                            placeholder = { Text("Añadir notas") },
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                            ),
                            shape = MaterialTheme.shapes.medium,
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    if (onDelete != null) {
                        TextButton(onClick = { confirmDelete = true }, modifier = Modifier.padding(start = 40.dp, top = 8.dp)) {
                            Text("Eliminar compromiso", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    Spacer(Modifier.navigationBarsPadding().height(24.dp))
                }
            }
        }
    }

    when (picking) {
        "date" -> {
            val state = rememberDatePickerState(initialSelectedDateMillis = date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
            DatePickerDialog(
                onDismissRequest = { picking = "" },
                confirmButton = {
                    TextButton(onClick = {
                        state.selectedDateMillis?.let { date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                        picking = ""
                    }) { Text("Aceptar") }
                },
                dismissButton = { TextButton(onClick = { picking = "" }) { Text("Cancelar") } },
            ) { DatePicker(state = state) }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Eliminar compromiso") },
            text = { Text("¿Eliminar «${title.trim()}»? No se puede deshacer.") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete?.invoke() }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") } },
        )
    }
}

private fun describe(millis: Long, date: LocalDate): String {
    val z = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
    val time = fmtTime(z.hour * 60 + z.minute)
    return if (z.toLocalDate() == date) time else "${z.dayOfMonth}/${z.monthValue} $time"
}

@Composable
private fun Section(icon: ImageVector, tint: Color? = null, content: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 6.dp)) {
        Icon(icon, null, Modifier.padding(top = 4.dp).size(22.dp), tint = tint ?: MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.weight(1f).padding(start = 18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { content() }
    }
}

@Composable
private fun Pill(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected, onClick = onClick, label = { Text(label) },
        shape = CircleShape,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    )
}

@Composable
private fun RowScope.TimeChip(label: String, value: Int?, active: Boolean, onClick: () -> Unit, onClear: () -> Unit) {
    Row(
        Modifier.clip(CircleShape)
            .background(if (value != null) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh)
            .then(if (active) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier)
            .clickable(onClick = onClick)
            .padding(start = 14.dp, end = if (value != null) 4.dp else 14.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (value == null) {
            Text("+ $label", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                Text(fmtTime(value), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
            Box(Modifier.width(4.dp))
            IconButton(onClick = onClear, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Close, "Quitar", Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
    }
}

/** Ruedas de hora dentro del formulario, con botón para cerrarlas. */
@Composable
private fun WheelPanel(minute: Int, onChange: (Int) -> Unit, onDone: () -> Unit, caption: String? = null) {
    Column(
        Modifier.fillMaxWidth().padding(top = 4.dp).clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainer).padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        caption?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        WheelTimePicker(minute, onChange)
        TextButton(onClick = onDone) { Text("Listo") }
    }
}
