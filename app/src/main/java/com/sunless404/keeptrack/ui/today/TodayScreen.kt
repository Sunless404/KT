package com.sunless404.keeptrack.ui.today

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.sp
import com.sunless404.keeptrack.data.Repeat
import com.sunless404.keeptrack.ui.tasks.HighPriority
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
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
import com.sunless404.keeptrack.ui.common.HelpTexts
import com.sunless404.keeptrack.ui.common.KtIcons
import com.sunless404.keeptrack.ui.common.TopActions
import com.sunless404.keeptrack.ui.common.appear
import com.sunless404.keeptrack.ui.common.rememberTick
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
fun TodayScreen(vm: TodayViewModel, onOpenProfile: () -> Unit) {
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
            TopActions("Hoy", HelpTexts.TODAY, onOpenProfile) {
                IconButton(onClick = { pickDate = true }) { Icon(Icons.Default.DateRange, "Elegir fecha") }
            }
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

// ---------- Página de un día ----------

/** Fila de la línea de tiempo de pendientes: un bloque o la marca de "ahora" en un hueco. */
private sealed interface TimelineItem {
    val key: String
    /** Inicio y fin en minutos del día visto (el sueño de la noche anterior empieza en 0). */
    val from: Int
    val to: Int

    data class Block(val entry: DayEntry, val carry: Boolean) : TimelineItem {
        override val key get() = "e${entry.id}-${entry.startMin}"
        override val from get() = if (carry) 0 else entry.startMin
        override val to get() = if (carry) entry.endMin - 1440 else entry.endMin
    }

    data class Now(val minute: Int, val label: String) : TimelineItem {
        override val key get() = "ahora"
        override val from get() = minute
        override val to get() = minute
    }
}

/** Qué fracción de [from]..[to] ha pasado a la hora [now] (0 antes, 1 después). */
private fun passed(from: Int, to: Int, now: Int): Float = when {
    now <= from -> 0f
    now >= to -> 1f
    else -> (now - from).toFloat() / (to - from)
}

@Composable
private fun DayPage(ui: TodayUi, vm: TodayViewModel) {
    var editingTask by remember { mutableStateOf<Task?>(null) }
    var adjusting by remember { mutableStateOf<Pair<DayEntry, AdjustKind>?>(null) }
    var removed by remember { mutableStateOf<DayEntry?>(null) }
    var showDone by rememberSaveable { mutableStateOf(false) }
    val list = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val now = ui.nowMin

    // Pendientes: compromisos sin hacer y bloques sin hacer, en orden.
    val pendingTasks = ui.tasks.filter { !it.done }.sortedWith(compareBy({ it.timeMin ?: -1 }, { -it.priority }))
    val doneTasks = ui.tasks.filter { it.done }
    val pendingEntries = (ui.carry.filter { it.status != Status.HECHO && it.status != Status.SALTADO }.map { TimelineItem.Block(it, true) } +
        ui.visibleEntries.filter { it.status != Status.HECHO }.map { TimelineItem.Block(it, false) })
    val doneEntries = ui.visibleEntries.filter { it.status == Status.HECHO }
    val current = ui.current?.takeIf { it.status != Status.HECHO }
    // En un hueco (o con el bloque actual ya hecho), la bolita va entre los bloques.
    val timeline: List<TimelineItem> = if (!ui.isToday || current != null || pendingEntries.isEmpty()) pendingEntries else {
        val next = ui.next
        val label = when {
            ui.current != null -> "«${ui.current!!.title}» ya está hecho"
            next != null -> "Libre hasta las ${fmtTime(next.startMin)}"
            else -> "Libre el resto del día"
        }
        val at = pendingEntries.indexOfFirst { it.from > now }.let { if (it < 0) pendingEntries.size else it }
        pendingEntries.take(at) + TimelineItem.Now(now, label) + pendingEntries.drop(at)
    }
    val doneCount = doneTasks.size + doneEntries.size

    // Índices de las filas de la lista, para poder saltar a "ahora".
    var index = 0
    val idxSummary = index++
    val idxPermissions = if (ui.isToday) index++ else -1
    val idxNow = if (ui.isToday) index++ else -1
    index++ // cabecera de compromisos
    index += maxOf(pendingTasks.size, 1)
    index++ // cabecera de rutina
    if (ui.isFuture) index++
    val firstTimeline = index
    val nowIndex = timeline.indexOfFirst {
        (it is TimelineItem.Now) || (it is TimelineItem.Block && it.entry.id == current?.id)
    }.let { if (it < 0) -1 else firstTimeline + it }
    fun goToNow() {
        if (nowIndex >= 0) scope.launch { list.animateScrollToItem((nowIndex - 1).coerceAtLeast(0)) }
    }
    // Al abrir hoy, baja hasta lo que toca ahora.
    var scrolled by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(nowIndex) {
        if (!scrolled && ui.isToday && nowIndex > firstTimeline + 1) {
            scrolled = true
            list.scrollToItem((nowIndex - 1).coerceAtLeast(0))
        }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = list,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "resumen") { Box(Modifier.appear(0)) { SummaryCard(ui, onType = { vm.setDayType(ui.date, it) }) } }
            if (ui.isToday) item(key = "permisos") { PermissionsCard() }
            if (ui.isToday) item(key = "ahora-card") {
                Box(Modifier.appear(1)) { NowCard(ui, current, onGo = ::goToNow) }
            }

            item(key = "h-compromisos") {
                SectionLabel("Compromisos") {
                    IconButton(onClick = { editingTask = Task(epochDay = ui.date.toEpochDay(), title = "") }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Add, "Nuevo compromiso", Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            if (pendingTasks.isEmpty()) item(key = "sin-compromisos") {
                Text(
                    if (doneTasks.isEmpty()) "Nada para este día." else "Todo hecho.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
            items(pendingTasks, key = { "t${it.id}" }) { t ->
                Box(Modifier.animateItem()) {
                    TaskRow(
                        t, late = ui.isToday && ((t.endMin ?: t.timeMin)?.let { it < now } ?: false) || ui.date < ui.today,
                        onDone = { vm.setTaskDone(t, true) }, onClick = { editingTask = t },
                    )
                }
            }

            item(key = "h-rutina") { SectionLabel("Rutina") }
            if (ui.isFuture) item(key = "futuro") {
                Text(
                    "Día futuro: se muestra la plantilla; podrás marcarla ese día.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
            itemsIndexed(timeline, key = { _, it -> it.key }) { i, item ->
                val first = i == 0
                val last = i == timeline.lastIndex
                Box(Modifier.animateItem()) {
                    when (item) {
                        is TimelineItem.Now -> NowMarker(item, first, last)
                        is TimelineItem.Block -> EntryRow(
                            e = item.entry,
                            carry = item.carry,
                            progress = if (ui.isToday) passed(item.from, item.to, now) else if (ui.date < ui.today) 1f else 0f,
                            current = item.entry.id == current?.id,
                            late = (ui.isToday && item.to <= now) || ui.date < ui.today,
                            first = first, last = last,
                            enabled = !ui.isFuture,
                            onDone = { vm.setStatus(item.entry, Status.HECHO) },
                            onRemove = { vm.setStatus(item.entry, Status.SALTADO); removed = item.entry },
                            onAdjust = { kind -> adjusting = item.entry to kind },
                        )
                    }
                }
            }
            if (timeline.isEmpty()) item(key = "sin-rutina") {
                Text(
                    if (doneEntries.isEmpty()) "No hay bloques de rutina este día." else "Rutina de hoy completada.",
                    modifier = Modifier.padding(start = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (doneCount > 0) {
                item(key = "h-hechas") {
                    Row(
                        Modifier.fillMaxWidth().padding(top = 8.dp).clip(MaterialTheme.shapes.medium)
                            .clickable { showDone = !showDone }.padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.Check, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                        Text(
                            "  HECHAS · $doneCount",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.2.sp,
                            modifier = Modifier.weight(1f),
                        )
                        val arrow by animateFloatAsState(if (showDone) 180f else 0f, label = "flecha")
                        Icon(Icons.Default.KeyboardArrowDown, if (showDone) "Ocultar" else "Mostrar", Modifier.rotate(arrow), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (showDone) {
                    val done = (doneTasks.map { Triple("dt${it.id}", it.timeMin ?: -1, it as Any) } +
                        doneEntries.map { Triple("de${it.id}", it.startMin, it as Any) }).sortedBy { it.second }
                    items(done, key = { it.first }) { (_, _, item) ->
                        Box(Modifier.animateItem()) {
                            when (item) {
                                is Task -> DoneRow(item.title, item.timeMin?.let { fmtTime(it) } ?: "Compromiso", null) { vm.setTaskDone(item, false) }
                                is DayEntry -> DoneRow(item.title, "${fmtTime(item.startMin)}–${fmtTime(item.endMin)}", item.category.composeColor) {
                                    vm.setStatus(item, Status.PENDIENTE)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Aviso para deshacer "Quitar de hoy".
        AnimatedVisibility(
            visible = removed != null,
            enter = fadeIn() + slideInVertically { it }, exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
        ) {
            val r = removed
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.inverseSurface),
                shape = MaterialTheme.shapes.medium,
            ) {
                Row(Modifier.padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "«${r?.title.orEmpty()}» quitado de hoy",
                        color = MaterialTheme.colorScheme.inverseOnSurface,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    TextButton(onClick = { r?.let { vm.setStatus(it, Status.PENDIENTE) }; removed = null }) {
                        Text("Deshacer", color = MaterialTheme.colorScheme.inversePrimary)
                    }
                }
            }
        }
        LaunchedEffect(removed) {
            if (removed != null) {
                kotlinx.coroutines.delay(5_000)
                removed = null
            }
        }
    }

    adjusting?.let { (entry, kind) ->
        AdjustDialog(ui, entry, kind, onDismiss = { adjusting = null }) { changed ->
            vm.applyUnforeseen(changed)
            adjusting = null
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
                    "${ui.visibleEntries.size} de rutina · ${ui.tasks.size} " + if (ui.tasks.size == 1) "compromiso" else "compromisos",
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
private fun NowCard(ui: TodayUi, current: DayEntry?, onGo: () -> Unit) {
    val next = ui.next
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "AHORA · ${fmtTime(ui.nowMin)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                )
                if (current != null) {
                    val end = if (current.epochDay == ui.date.toEpochDay()) current.endMin else current.endMin - 1440
                    Text(current.title, style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Quedan ${fmtDuration(end - ui.nowMin)}", style = MaterialTheme.typography.bodyMedium)
                } else {
                    Text(if (ui.current != null) "Hecho" else "Hueco libre", style = MaterialTheme.typography.headlineSmall)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    next?.let { "Siguiente: ${it.title} en ${fmtDuration(it.startMin - ui.nowMin)}" } ?: "No queda nada más hoy",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            FilledTonalButton(onClick = onGo, contentPadding = PaddingValues(horizontal = 16.dp)) {
                Text("Ir")
                Icon(Icons.Default.KeyboardArrowDown, null, Modifier.size(18.dp))
            }
        }
    }
}

/**
 * Tachado animado: al marcar, la línea cruza el texto y, tras un instante, se avisa con
 * [onFinished] para mover el elemento a "Hechas".
 */
@Composable
private fun rememberFinish(onFinished: () -> Unit): Pair<Float, () -> Unit> {
    var finishing by remember { mutableStateOf(false) }
    val strike by animateFloatAsState(if (finishing) 1f else 0f, tween(320), label = "tachado")
    LaunchedEffect(finishing) {
        if (finishing) {
            kotlinx.coroutines.delay(520)
            onFinished()
        }
    }
    return strike to { finishing = true }
}

private fun Modifier.strike(progress: Float, color: Color) = drawWithContent {
    drawContent()
    if (progress > 0f) {
        val y = size.height / 2
        drawLine(color, Offset(0f, y), Offset(size.width * progress, y), strokeWidth = 2.dp.toPx())
    }
}

@Composable
private fun TaskRow(t: Task, late: Boolean, onDone: () -> Unit, onClick: () -> Unit) {
    val tick = rememberTick()
    val (strike, finish) = rememberFinish(onDone)
    val accent = if (t.highPriority) HighPriority else MaterialTheme.colorScheme.primary
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().alpha(1f - 0.35f * strike).clickable(onClick = onClick),
    ) {
        Row(Modifier.height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(4.dp).fillMaxHeight().background(accent.copy(alpha = if (t.highPriority) 1f else 0.5f)))
            Checkbox(checked = strike > 0f, onCheckedChange = { if (it) { tick(); finish() } })
            Column(Modifier.weight(1f).padding(vertical = 10.dp)) {
                Text(
                    t.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.strike(strike, MaterialTheme.colorScheme.onSurface),
                )
                val time = t.timeMin?.let { s -> fmtTime(s) + (t.endMin?.let { "–" + fmtTime(it) } ?: "") } ?: "Todo el día"
                val sub = listOfNotNull(time, t.description.takeIf { it.isNotBlank() }?.lineSequence()?.first()).joinToString(" · ")
                Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (late) LateTag()
            if (t.highPriority) Icon(KtIcons.Flag, "Prioridad alta", Modifier.padding(start = 6.dp).size(16.dp), tint = HighPriority)
            if (t.notify) Icon(KtIcons.Bell, "Con recordatorio", Modifier.padding(start = 6.dp).size(16.dp), tint = MaterialTheme.colorScheme.secondary)
            if (t.repeat != Repeat.NONE) Icon(KtIcons.Repeat, "Se repite", Modifier.padding(start = 6.dp).size(16.dp), tint = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.width(12.dp))
        }
    }
}

@Composable
private fun LateTag() {
    Text(
        "Atrasado",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f)).padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

/** Columna de la línea de tiempo: línea continua, tramo recorrido en color y la bolita de "ahora". */
@Composable
private fun Track(
    progress: Float,
    dotColor: Color,
    first: Boolean,
    last: Boolean,
    showNow: Boolean,
    nowFraction: Float,
    modifier: Modifier = Modifier,
) {
    val base = MaterialTheme.colorScheme.outlineVariant
    val fill = MaterialTheme.colorScheme.primary
    val bg = MaterialTheme.colorScheme.background
    val pulse by rememberInfiniteTransition(label = "pulso").animateFloat(
        0f, 1f, infiniteRepeatable(tween(1600, easing = LinearEasing)), label = "p",
    )
    val nowF by animateFloatAsState(nowFraction, tween(800), label = "ahora")
    Canvas(modifier.width(24.dp).fillMaxHeight()) {
        val x = size.width / 2
        val w = 2.dp.toPx()
        val pad = 3.dp.toPx()
        val ext = 5.dp.toPx() // une la línea con la fila de al lado (salta el espacio entre filas)
        val dotY = 16.dp.toPx()
        val top = if (first) dotY else -ext
        val bottom = if (last) (if (showNow) pad + (size.height - 2 * pad) * nowF else dotY) else size.height + ext
        drawLine(base, Offset(x, top), Offset(x, maxOf(bottom, top)), w, cap = StrokeCap.Round)
        // Tramo ya recorrido.
        val passedY = if (showNow) pad + (size.height - 2 * pad) * nowF else if (progress >= 1f) size.height + ext else top
        if (passedY > top) drawLine(fill, Offset(x, top), Offset(x, minOf(passedY, if (last && !showNow) dotY else passedY)), w, cap = StrokeCap.Round)
        if (showNow) {
            val y = pad + (size.height - 2 * pad) * nowF
            drawCircle(fill.copy(alpha = 0.25f * (1 - pulse)), 7.dp.toPx() + 7.dp.toPx() * pulse, Offset(x, y))
            drawCircle(bg, 8.dp.toPx(), Offset(x, y))
            drawCircle(fill, 6.dp.toPx(), Offset(x, y))
            drawCircle(Color.White, 2.dp.toPx(), Offset(x, y))
        } else {
            drawCircle(bg, 6.dp.toPx(), Offset(x, dotY))
            drawCircle(dotColor, 5.dp.toPx(), Offset(x, dotY), style = Stroke(2.dp.toPx()))
            if (progress >= 1f) drawCircle(dotColor, 3.dp.toPx(), Offset(x, dotY))
        }
    }
}

@Composable
private fun NowMarker(item: TimelineItem.Now, first: Boolean, last: Boolean) {
    Row(Modifier.fillMaxWidth().height(40.dp)) {
        Box(Modifier.width(52.dp).fillMaxHeight(), contentAlignment = Alignment.CenterEnd) {
            Text(fmtTime(item.minute), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
        Track(progress = 0f, dotColor = MaterialTheme.colorScheme.primary, first = first, last = last, showNow = true, nowFraction = 0.5f)
        Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.CenterStart) {
            Text(item.label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun EntryRow(
    e: DayEntry,
    carry: Boolean,
    progress: Float,
    current: Boolean,
    late: Boolean,
    first: Boolean,
    last: Boolean,
    enabled: Boolean,
    onDone: () -> Unit,
    onRemove: () -> Unit,
    onAdjust: (AdjustKind) -> Unit,
) {
    val tick = rememberTick()
    var menu by remember { mutableStateOf(false) }
    val (strike, finish) = rememberFinish(onDone)
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).alpha(1f - 0.35f * strike)) {
        // Columna de horas
        Column(Modifier.width(52.dp).padding(top = 10.dp), horizontalAlignment = Alignment.End) {
            Text(fmtTime(e.startMin), style = MaterialTheme.typography.labelLarge, fontWeight = if (current) FontWeight.Bold else FontWeight.Normal)
            Text(fmtTime(e.endMin), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Track(progress, e.category.composeColor, first, last, showNow = current, nowFraction = progress)
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
                            maxLines = 2, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.strike(strike, MaterialTheme.colorScheme.onSurface),
                        )
                        val extra = buildList {
                            if (carry) add("desde ayer")
                            add(e.category.label)
                            add(fmtDuration(e.durationMin))
                            if (e.status == Status.POSPUESTO) add("pospuesto")
                            if (e.modified) add("cambiado hoy")
                        }.joinToString(" · ")
                        Text(extra, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (late && !current) LateTag()
                    Checkbox(checked = strike > 0f, enabled = enabled, onCheckedChange = { if (it) { tick(); finish() } })
                }
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text("Hecho") }, leadingIcon = { Icon(Icons.Default.Check, null) }, onClick = { menu = false; tick(); finish() })
                DropdownMenuItem(text = { Text("Quitar de hoy") }, leadingIcon = { Icon(Icons.Default.Close, null) }, onClick = { menu = false; onRemove() })
                HorizontalDivider()
                DropdownMenuItem(text = { Text("Cambiar hora hoy") }, leadingIcon = { Icon(KtIcons.Clock, null) }, onClick = { menu = false; onAdjust(AdjustKind.MOVE) })
                DropdownMenuItem(text = { Text("Acortar hoy") }, leadingIcon = { Icon(KtIcons.Stop, null) }, onClick = { menu = false; onAdjust(AdjustKind.SHORTEN) })
                DropdownMenuItem(text = { Text("Retrasar desde aquí") }, leadingIcon = { Icon(KtIcons.Repeat, null) }, onClick = { menu = false; onAdjust(AdjustKind.DELAY) })
            }
        }
    }
}

/** Fila de la sección "Hechas": tachada, atenuada; al desmarcarla vuelve a pendientes. */
@Composable
private fun DoneRow(title: String, subtitle: String, color: Color?, onUndo: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = true, onCheckedChange = { if (!it) onUndo() })
        if (color != null) Box(Modifier.size(8.dp).clip(CircleShape).background(color.copy(alpha = 0.6f)))
        Column(Modifier.weight(1f).padding(start = if (color != null) 10.dp else 0.dp, top = 8.dp, bottom = 8.dp)) {
            Text(
                title, style = MaterialTheme.typography.bodyMedium, textDecoration = TextDecoration.LineThrough,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
        }
    }
}
