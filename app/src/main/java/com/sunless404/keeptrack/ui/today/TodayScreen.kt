package com.sunless404.keeptrack.ui.today

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sunless404.keeptrack.data.DayEntry
import com.sunless404.keeptrack.data.DayType
import com.sunless404.keeptrack.data.Status
import com.sunless404.keeptrack.data.Task
import com.sunless404.keeptrack.domain.DAY_SHORT
import com.sunless404.keeptrack.domain.dayName
import com.sunless404.keeptrack.domain.fmtDuration
import com.sunless404.keeptrack.domain.fmtShortDate
import com.sunless404.keeptrack.domain.fmtTime
import com.sunless404.keeptrack.domain.mondayOf
import com.sunless404.keeptrack.ui.common.KtIcons
import com.sunless404.keeptrack.ui.common.ProgressRing
import com.sunless404.keeptrack.ui.common.SectionLabel
import com.sunless404.keeptrack.ui.common.composeColor
import com.sunless404.keeptrack.ui.settings.PermissionsCard
import com.sunless404.keeptrack.ui.tasks.TaskEditorDialog
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Páginas del deslizador: la central es hoy. */
private const val PAGES = 4001
private const val CENTER = PAGES / 2

@Composable
fun TodayScreen(vm: TodayViewModel, onOpenSettings: () -> Unit) {
    val now by vm.now.collectAsStateWithLifecycle()
    val today = now.toLocalDate()
    val pager = rememberPagerState(initialPage = CENTER) { PAGES }
    val scope = rememberCoroutineScope()
    fun dateOf(page: Int): LocalDate = today.plusDays((page - CENTER).toLong())
    fun pageOf(date: LocalDate): Int = (CENTER + (date.toEpochDay() - today.toEpochDay())).toInt().coerceIn(0, PAGES - 1)
    val selected = dateOf(pager.currentPage)
    var pickDate by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.jump.collect { pager.scrollToPage(pageOf(it)) } }

    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)) {
        // Cabecera
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    when (selected) {
                        today -> "Hoy"
                        today.plusDays(1) -> "Mañana"
                        today.minusDays(1) -> "Ayer"
                        else -> dayName(selected.dayOfWeek.value)
                    },
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    "${dayName(selected.dayOfWeek.value)} ${fmtShortDate(selected)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (selected != today) {
                TextButton(onClick = { scope.launch { pager.animateScrollToPage(CENTER) } }) { Text("Hoy") }
            }
            IconButton(onClick = { pickDate = true }) { Icon(Icons.Default.DateRange, "Elegir fecha") }
            IconButton(onClick = onOpenSettings) { Icon(Icons.Default.Settings, "Ajustes") }
        }
        WeekStrip(selected, today) { scope.launch { pager.animateScrollToPage(pageOf(it)) } }
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize(), beyondViewportPageCount = 1) { page ->
            val date = dateOf(page)
            val flow = remember(date) { vm.page(date) }
            val ui by flow.collectAsState(initial = null)
            ui?.let { DayPage(it, vm) }
        }
    }

    if (pickDate) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = selected.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        val d = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                        scope.launch { pager.scrollToPage(pageOf(d)) }
                    }
                    pickDate = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text("Cancelar") } },
        ) { DatePicker(state = state) }
    }
}

/** Los 7 días de la semana del día elegido. */
@Composable
private fun WeekStrip(selected: LocalDate, today: LocalDate, onPick: (LocalDate) -> Unit) {
    val monday = mondayOf(selected)
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        (0L..6L).forEach { i ->
            val d = monday.plusDays(i)
            val isSel = d == selected
            val bg by animateColorAsState(
                if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer, label = "day",
            )
            val fg = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            Column(
                Modifier
                    .width(44.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(bg)
                    .then(if (d == today && !isSel) Modifier.border(1.5.dp, MaterialTheme.colorScheme.secondary, RoundedCornerShape(16.dp)) else Modifier)
                    .clickable { onPick(d) }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(DAY_SHORT[i.toInt()], style = MaterialTheme.typography.labelSmall, color = fg.copy(alpha = 0.7f))
                Text("${d.dayOfMonth}", style = MaterialTheme.typography.titleMedium, color = fg)
            }
        }
    }
}

@Composable
private fun DayPage(ui: TodayUi, vm: TodayViewModel) {
    var unforeseen by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<Task?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { SummaryCard(ui, onType = { vm.setDayType(ui.date, it) }) }
        if (ui.isToday) item { PermissionsCard() }
        if (ui.isToday) item { NowCard(ui, onUnforeseen = { unforeseen = true }) }

        item {
            SectionLabel("Extras") {
                TextButton(onClick = { editingTask = Task(epochDay = ui.date.toEpochDay(), title = "") }) {
                    Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                    Text(" Añadir")
                }
            }
        }
        if (ui.tasks.isEmpty()) item {
            Text(
                "Sin tareas extra este día.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
        items(ui.tasks, key = { "t${it.id}" }) { t ->
            TaskRow(t, onCheck = { vm.setTaskDone(t, it) }, onClick = { editingTask = t })
        }

        item { SectionLabel("Rutina") }
        if (ui.isFuture) item {
            Text(
                "Día futuro: se muestra la plantilla; podrás marcarla ese día.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
        items(ui.carry, key = { "c${it.id}" }) { e ->
            EntryRow(e, carry = true, current = ui.current?.id == e.id, enabled = true) { vm.setStatus(e, it) }
        }
        items(ui.entries, key = { "${it.id}-${it.blockId}-${it.startMin}" }) { e ->
            EntryRow(e, carry = false, current = ui.isToday && ui.current?.id == e.id, enabled = !ui.isFuture) { vm.setStatus(e, it) }
        }
        if (ui.entries.isEmpty()) item {
            Text("No hay bloques de rutina este día.", modifier = Modifier.padding(start = 4.dp), style = MaterialTheme.typography.bodySmall)
        }
    }

    if (unforeseen) {
        UnforeseenDialog(ui, onDismiss = { unforeseen = false }) { changed ->
            vm.applyUnforeseen(changed)
            unforeseen = false
        }
    }
    editingTask?.let { t ->
        TaskEditorDialog(
            initial = t,
            onDismiss = { editingTask = null },
            onDelete = if (t.id != 0L) ({ vm.deleteTask(t); editingTask = null }) else null,
        ) {
            vm.saveTask(it)
            editingTask = null
        }
    }
}

@Composable
private fun SummaryCard(ui: TodayUi, onType: (DayType) -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            ProgressRing(ui.progress, size = 76.dp) {
                Text("${(ui.progress * 100).toInt()}%", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text("${ui.done} de ${ui.totalCount} completados", style = MaterialTheme.typography.titleMedium)
                Text(
                    "${ui.entries.size} de rutina · ${ui.tasks.size} extra",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Box {
                    AssistChip(
                        onClick = { menu = true },
                        label = {
                            Text(if (ui.dayType == DayType.NORMAL) "Día normal" else "${ui.dayType.label} · sin rachas ni avisos")
                        },
                    )
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DayType.entries.forEach { t ->
                            DropdownMenuItem(text = { Text(t.label) }, onClick = { menu = false; onType(t) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NowCard(ui: TodayUi, onUnforeseen: () -> Unit) {
    val cur = ui.current
    val next = ui.next
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("AHORA", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
            if (cur != null) {
                val left = (if (cur.epochDay == ui.date.toEpochDay()) cur.endMin else cur.endMin - 1440) - ui.nowMin
                Text(cur.title, style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Quedan ${fmtDuration(left)}", style = MaterialTheme.typography.bodyMedium)
            } else {
                Text("Hueco libre", style = MaterialTheme.typography.headlineSmall)
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    next?.let { "Siguiente: ${it.title} en ${fmtDuration(it.startMin - ui.nowMin)}" } ?: "No queda nada más hoy",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                )
                FilledTonalButton(onClick = onUnforeseen, contentPadding = PaddingValues(horizontal = 14.dp)) { Text("Imprevisto") }
            }
        }
    }
}

@Composable
private fun TaskRow(t: Task, onCheck: (Boolean) -> Unit, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(start = 4.dp, end = 12.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = t.done, onCheckedChange = onCheck)
            Column(Modifier.weight(1f)) {
                Text(
                    t.title,
                    style = MaterialTheme.typography.titleSmall,
                    textDecoration = if (t.done) TextDecoration.LineThrough else null,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                val sub = listOfNotNull(t.timeMin?.let { fmtTime(it) }, t.description.takeIf { it.isNotBlank() }).joinToString(" · ")
                if (sub.isNotEmpty()) {
                    Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (t.notify) Icon(KtIcons.Bell, "Con recordatorio", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
private fun EntryRow(e: DayEntry, carry: Boolean, current: Boolean, enabled: Boolean, onStatus: (Status) -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val done = e.status == Status.HECHO
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        // Columna de horas
        Column(Modifier.width(52.dp).padding(top = 10.dp), horizontalAlignment = Alignment.End) {
            Text(fmtTime(e.startMin), style = MaterialTheme.typography.labelLarge, fontWeight = if (current) FontWeight.Bold else FontWeight.Normal)
            Text(fmtTime(e.endMin), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        // Línea de tiempo
        Box(Modifier.width(24.dp).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
            Box(Modifier.width(2.dp).fillMaxHeight().background(MaterialTheme.colorScheme.outlineVariant))
            Box(
                Modifier.padding(top = 13.dp).size(if (current) 14.dp else 10.dp).clip(CircleShape)
                    .background(if (done || current) e.category.composeColor else MaterialTheme.colorScheme.surfaceContainerHighest)
                    .border(2.dp, e.category.composeColor, CircleShape),
            )
        }
        Box(Modifier.weight(1f).padding(vertical = 3.dp)) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (current) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                ),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth().combinedClickable(enabled = enabled, onClick = { menu = true }, onLongClick = { menu = true }),
            ) {
                Row(Modifier.height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(5.dp).fillMaxHeight().background(e.category.composeColor))
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 10.dp)) {
                        Text(
                            e.title,
                            style = MaterialTheme.typography.titleSmall,
                            textDecoration = if (e.status == Status.SALTADO) TextDecoration.LineThrough else null,
                            maxLines = 2, overflow = TextOverflow.Ellipsis,
                        )
                        val extra = buildList {
                            if (carry) add("desde ayer")
                            add(e.category.label)
                            add(fmtDuration(e.durationMin))
                            if (e.status == Status.SALTADO || e.status == Status.POSPUESTO) add(e.status.label.lowercase())
                            if (e.modified) add("cambiado hoy")
                        }.joinToString(" · ")
                        Text(extra, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Checkbox(checked = done, enabled = enabled, onCheckedChange = { onStatus(if (it) Status.HECHO else Status.PENDIENTE) })
                }
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text("Hecho") }, onClick = { menu = false; onStatus(Status.HECHO) })
                DropdownMenuItem(text = { Text("Saltar") }, onClick = { menu = false; onStatus(Status.SALTADO) })
                DropdownMenuItem(text = { Text("Posponer 10 min") }, onClick = { menu = false; onStatus(Status.POSPUESTO) })
                DropdownMenuItem(text = { Text("Dejar pendiente") }, onClick = { menu = false; onStatus(Status.PENDIENTE) })
            }
        }
    }
}
