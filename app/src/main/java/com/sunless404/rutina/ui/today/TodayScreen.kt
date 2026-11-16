package com.sunless404.rutina.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sunless404.rutina.data.DayEntry
import com.sunless404.rutina.data.DayType
import com.sunless404.rutina.data.Status
import com.sunless404.rutina.domain.fmtDuration
import com.sunless404.rutina.domain.fmtLongDate
import com.sunless404.rutina.domain.fmtRange
import com.sunless404.rutina.ui.common.ScreenScaffold
import com.sunless404.rutina.ui.common.composeColor
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
fun TodayScreen(vm: TodayViewModel, extraActions: @Composable () -> Unit = {}) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    var pickDate by remember { mutableStateOf(false) }

    ScreenScaffold(
        title = if (ui.isToday) "Hoy" else "Día",
        actions = {
            if (!ui.isToday) TextButton(onClick = { vm.setDate(LocalDate.now()) }) { Text("Ir a hoy") }
            IconButton(onClick = { pickDate = true }) { Icon(Icons.Default.DateRange, "Elegir fecha") }
            extraActions()
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item { DateHeader(ui, onPrev = { vm.shiftDate(-1) }, onNext = { vm.shiftDate(1) }) }
            item { ProgressHeader(ui) }
            if (ui.isToday) item { NowCard(ui) }
            if (ui.isFuture) item {
                Text(
                    "Día futuro: se muestra la plantilla. Aún no se puede marcar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(ui.carry, key = { "c${it.id}" }) { e ->
                EntryRow(e, carry = true, current = ui.current?.id == e.id, enabled = true) { vm.setStatus(e, it) }
            }
            items(ui.entries, key = { "${it.id}-${it.blockId}-${it.startMin}" }) { e ->
                EntryRow(
                    e,
                    carry = false,
                    current = ui.isToday && ui.current?.id == e.id,
                    enabled = !ui.isFuture,
                ) { vm.setStatus(e, it) }
            }
            if (ui.entries.isEmpty()) item {
                Text("No hay bloques para este día.", modifier = Modifier.padding(16.dp))
            }
            item { Spacer(Modifier.height(72.dp)) }
        }
    }

    if (pickDate) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = ui.date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        vm.setDate(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    pickDate = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text("Cancelar") } },
        ) { DatePicker(state = state) }
    }
}

@Composable
private fun DateHeader(ui: TodayUi, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        IconButton(onClick = onPrev) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Día anterior") }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(fmtLongDate(ui.date), style = MaterialTheme.typography.titleMedium)
            if (ui.dayType != DayType.NORMAL) {
                Text(
                    "${ui.dayType.label} · no cuenta en rachas",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
        }
        IconButton(onClick = onNext) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Día siguiente") }
    }
}

@Composable
private fun ProgressHeader(ui: TodayUi) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
        Row {
            Text("Progreso del día", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            Text(
                "${ui.done}/${ui.entries.size} · ${(ui.progress * 100).toInt()} %",
                style = MaterialTheme.typography.labelLarge,
            )
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(progress = { ui.progress }, modifier = Modifier.fillMaxWidth().height(8.dp))
    }
}

@Composable
private fun NowCard(ui: TodayUi) {
    val cur = ui.current
    val next = ui.next
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp)) {
            if (cur != null) {
                val left = (if (cur.epochDay == ui.date.toEpochDay()) cur.endMin else cur.endMin - 1440) - ui.nowMin
                Text("Ahora", style = MaterialTheme.typography.labelMedium)
                Text(cur.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text("Quedan ${fmtDuration(left)}", style = MaterialTheme.typography.bodyMedium)
            } else {
                Text("Ahora: hueco libre", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(6.dp))
            if (next != null) {
                Text(
                    "Siguiente: ${next.title} en ${fmtDuration(next.startMin - ui.nowMin)}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                Text("No queda nada más hoy.", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun EntryRow(
    e: DayEntry,
    carry: Boolean,
    current: Boolean,
    enabled: Boolean,
    onStatus: (Status) -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    val container = if (current) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
    Box {
        Card(
            colors = CardDefaults.cardColors(containerColor = container),
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    enabled = enabled,
                    onClick = { menu = true },
                    onLongClick = { menu = true },
                ),
        ) {
            Row(Modifier.height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(8.dp).fillMaxHeight().background(e.category.composeColor))
                Column(Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text(
                        (if (carry) "Desde ayer · " else "") + fmtRange(e.startMin, e.endMin) +
                            " · " + fmtDuration(e.durationMin),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        e.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (current) FontWeight.Bold else FontWeight.Normal,
                        textDecoration = if (e.status == Status.SALTADO) TextDecoration.LineThrough else null,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val extra = buildList {
                        add(e.category.label)
                        if (current) add("en curso")
                        if (e.status == Status.SALTADO || e.status == Status.POSPUESTO) add(e.status.label)
                    }.joinToString(" · ")
                    Text(extra, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (e.notes.isNotBlank()) {
                        Text(e.notes, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
                Checkbox(
                    checked = e.status == Status.HECHO,
                    enabled = enabled,
                    onCheckedChange = { onStatus(if (it) Status.HECHO else Status.PENDIENTE) },
                )
            }
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(text = { Text("Hecho") }, onClick = { menu = false; onStatus(Status.HECHO) })
            DropdownMenuItem(text = { Text("Saltar") }, onClick = { menu = false; onStatus(Status.SALTADO) })
            DropdownMenuItem(text = { Text("Posponer") }, onClick = { menu = false; onStatus(Status.POSPUESTO) })
            DropdownMenuItem(text = { Text("Dejar pendiente") }, onClick = { menu = false; onStatus(Status.PENDIENTE) })
        }
    }
}
