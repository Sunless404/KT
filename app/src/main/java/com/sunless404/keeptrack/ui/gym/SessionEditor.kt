package com.sunless404.keeptrack.ui.gym

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sunless404.keeptrack.data.GymSession
import com.sunless404.keeptrack.data.GymSet
import com.sunless404.keeptrack.domain.fmtLongDate
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private val COMMON = listOf(
    "Press banca", "Sentadilla", "Peso muerto", "Press militar", "Dominadas",
    "Remo con barra", "Fondos", "Hip thrust", "Prensa", "Curl de bíceps",
)

/** Serie en edición: textos libres para que el teclado numérico funcione cómodo. */
private class SetRow(val exercise: String, weight: String, reps: String) {
    var weight by mutableStateOf(weight)
    var reps by mutableStateOf(reps)
}

private fun num(s: String): Double? = s.replace(',', '.').toDoubleOrNull()

@Composable
fun SessionEditorDialog(
    session: GymSession,
    sets: List<GymSet>,
    known: List<String>,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)?,
    onSave: (GymSession, List<GymSet>) -> Unit,
) {
    var date by remember { mutableStateOf(LocalDate.ofEpochDay(session.epochDay)) }
    var body by remember { mutableStateOf(session.bodyWeightKg?.let { kg(it).removeSuffix(" kg") } ?: "") }
    var notes by remember { mutableStateOf(session.notes) }
    val rows = remember {
        mutableStateListOf<SetRow>().apply {
            sets.forEach { add(SetRow(it.exercise, kg(it.weightKg).removeSuffix(" kg"), it.reps.toString())) }
        }
    }
    var newExercise by remember { mutableStateOf("") }
    var pickDate by remember { mutableStateOf(false) }
    val suggestions = (known + COMMON).distinctBy { it.lowercase() }.take(12)

    fun addExercise(name: String) {
        val n = name.trim()
        if (n.isNotEmpty()) rows.add(SetRow(n, "", ""))
        newExercise = ""
    }

    val valid = rows.any { num(it.weight) != null && (it.reps.toIntOrNull() ?: 0) > 0 }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (session.id == 0L) "Nueva sesión" else "Editar sesión") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { pickDate = true }, modifier = Modifier.fillMaxWidth()) { Text(fmtLongDate(date)) }
                OutlinedTextField(
                    value = body, onValueChange = { body = it },
                    label = { Text("Tu peso corporal (kg)") },
                    supportingText = { Text("Para la fuerza relativa. Opcional.") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                rows.groupBy { it.exercise }.forEach { (ex, list) ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)) {
                        Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(ex, style = MaterialTheme.typography.titleSmall)
                            list.forEachIndexed { i, r ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("${i + 1}", modifier = Modifier.width(20.dp), style = MaterialTheme.typography.labelLarge)
                                    OutlinedTextField(
                                        value = r.weight, onValueChange = { r.weight = it },
                                        label = { Text("kg") }, singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(1f),
                                    )
                                    Text(" × ")
                                    OutlinedTextField(
                                        value = r.reps, onValueChange = { v -> r.reps = v.filter { it.isDigit() }.take(3) },
                                        label = { Text("reps") }, singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                    )
                                    IconButton(onClick = { rows.remove(r) }, modifier = Modifier.size(36.dp)) {
                                        Icon(Icons.Default.Close, "Quitar serie", Modifier.size(18.dp))
                                    }
                                }
                            }
                            TextButton(onClick = {
                                val last = list.lastOrNull()
                                val at = rows.indexOf(last) + 1
                                rows.add(at, SetRow(ex, last?.weight ?: "", last?.reps ?: ""))
                            }) { Text("+ Serie") }
                        }
                    }
                }
                Text("Añadir ejercicio", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    suggestions.forEach { s -> FilterChip(selected = false, onClick = { addExercise(s) }, label = { Text(s) }) }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newExercise, onValueChange = { newExercise = it },
                        label = { Text("Otro ejercicio") }, singleLine = true, modifier = Modifier.weight(1f),
                    )
                    TextButton(enabled = newExercise.isNotBlank(), onClick = { addExercise(newExercise) }) { Text("Añadir") }
                }
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notas (opcional)") }, modifier = Modifier.fillMaxWidth())
                if (onDelete != null) TextButton(onClick = onDelete) { Text("Borrar sesión", color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = {
                val list = rows.mapNotNull { r ->
                    val w = num(r.weight) ?: return@mapNotNull null
                    val reps = r.reps.toIntOrNull()?.takeIf { it > 0 } ?: return@mapNotNull null
                    GymSet(sessionId = session.id, exercise = r.exercise, weightKg = w, reps = reps)
                }
                onSave(session.copy(epochDay = date.toEpochDay(), notes = notes.trim(), bodyWeightKg = num(body)?.takeIf { it in 25.0..300.0 }), list)
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )

    if (pickDate) {
        val state = rememberDatePickerState(initialSelectedDateMillis = date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                    pickDate = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text("Cancelar") } },
        ) { DatePicker(state = state) }
    }
}

