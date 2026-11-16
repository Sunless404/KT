package com.sunless404.keeptrack.ui.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.sunless404.keeptrack.data.Task
import com.sunless404.keeptrack.domain.fmtLongDate
import com.sunless404.keeptrack.domain.fmtTime
import com.sunless404.keeptrack.ui.common.TimePickerDialog
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/** Cuándo avisar de una tarea. */
private enum class RemindMode(val label: String) { AT_TIME("A su hora"), BEFORE("Antes"), CUSTOM("Otra hora") }

/** Momento del recordatorio en epoch millis. */
fun remindAtMillis(date: LocalDate, minuteOfDay: Int): Long =
    date.atStartOfDay(ZoneId.systemDefault()).plusMinutes(minuteOfDay.toLong()).toInstant().toEpochMilli()

/**
 * Crear o modificar una tarea: nombre, día, hora opcional, descripción opcional
 * y recordatorio opcional.
 */
@Composable
fun TaskEditorDialog(initial: Task, onDismiss: () -> Unit, onDelete: (() -> Unit)? = null, onSave: (Task) -> Unit) {
    val zone = ZoneId.systemDefault()
    var title by remember { mutableStateOf(initial.title) }
    var date by remember { mutableStateOf(LocalDate.ofEpochDay(initial.epochDay)) }
    var time by remember { mutableStateOf(initial.timeMin) }
    var description by remember { mutableStateOf(initial.description) }
    var notify by remember { mutableStateOf(initial.notify) }
    val initialRemindMin = initial.remindAt?.let {
        val t = Instant.ofEpochMilli(it).atZone(zone)
        t.hour * 60 + t.minute
    }
    var mode by remember {
        mutableStateOf(
            when {
                initialRemindMin == null || initialRemindMin == initial.timeMin -> RemindMode.AT_TIME
                else -> RemindMode.CUSTOM
            }
        )
    }
    var before by remember { mutableStateOf(10) }
    var customMin by remember { mutableStateOf(initialRemindMin ?: (9 * 60)) }
    var picking by remember { mutableStateOf("") } // "date", "time", "remind"

    val remindMin: Int = when {
        mode == RemindMode.CUSTOM || time == null -> customMin
        mode == RemindMode.BEFORE -> (time!! - before).coerceAtLeast(0)
        else -> time!!
    }
    val valid = title.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id == 0L) "Nueva tarea" else "Editar tarea") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    label = { Text("Nombre") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedButton(onClick = { picking = "date" }, modifier = Modifier.fillMaxWidth()) {
                    Text("Día: ${fmtLongDate(date)}")
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { picking = "time" }, modifier = Modifier.weight(1f)) {
                        Text(time?.let { "Hora: ${fmtTime(it)}" } ?: "Sin hora")
                    }
                    if (time != null) TextButton(onClick = { time = null }) { Text("Quitar") }
                }
                OutlinedTextField(
                    value = description, onValueChange = { description = it },
                    label = { Text("Descripción (opcional)") },
                    minLines = 2,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Recordatorio", style = MaterialTheme.typography.titleSmall)
                        Text("Te llega una notificación", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = notify, onCheckedChange = { notify = it })
                }
                if (notify) {
                    if (time != null) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            RemindMode.entries.forEach { m ->
                                FilterChip(selected = mode == m, onClick = { mode = m }, label = { Text(m.label) })
                            }
                        }
                    }
                    if (time != null && mode == RemindMode.BEFORE) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(5, 10, 15, 30, 60, 120).forEach { m ->
                                FilterChip(
                                    selected = before == m, onClick = { before = m },
                                    label = { Text(if (m < 60) "$m min" else "${m / 60} h") },
                                )
                            }
                        }
                    }
                    if (time == null || mode == RemindMode.CUSTOM) {
                        OutlinedButton(onClick = { picking = "remind" }) { Text("Avisar a las ${fmtTime(customMin)}") }
                    }
                    Text(
                        "Te avisará el ${fmtLongDate(date).lowercase()} a las ${fmtTime(remindMin)}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                if (onDelete != null) {
                    TextButton(onClick = onDelete) { Text("Borrar tarea", color = MaterialTheme.colorScheme.error) }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = {
                onSave(
                    initial.copy(
                        title = title.trim(),
                        epochDay = date.toEpochDay(),
                        timeMin = time,
                        description = description.trim(),
                        notify = notify,
                        remindAt = if (notify) remindAtMillis(date, remindMin) else null,
                    )
                )
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )

    when (picking) {
        "date" -> {
            val state = rememberDatePickerState(
                initialSelectedDateMillis = date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
            )
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
        "time" -> TimePickerDialog(time ?: (12 * 60), "Hora de la tarea", onDismiss = { picking = "" }) {
            time = it
            picking = ""
        }
        "remind" -> TimePickerDialog(customMin, "Hora del recordatorio", onDismiss = { picking = "" }) {
            customMin = it
            mode = RemindMode.CUSTOM
            picking = ""
        }
    }
}
