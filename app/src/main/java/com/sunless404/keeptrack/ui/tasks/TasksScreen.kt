package com.sunless404.keeptrack.ui.tasks

import android.app.Application
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.sunless404.keeptrack.App
import com.sunless404.keeptrack.data.Task
import com.sunless404.keeptrack.domain.DAY_SHORT
import com.sunless404.keeptrack.domain.ES
import com.sunless404.keeptrack.domain.fmtLongDate
import com.sunless404.keeptrack.domain.fmtTime
import com.sunless404.keeptrack.domain.mondayOf
import com.sunless404.keeptrack.ui.common.HelpTexts
import com.sunless404.keeptrack.ui.common.KtIcons
import com.sunless404.keeptrack.ui.common.TopActions
import com.sunless404.keeptrack.ui.common.appear
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class TasksViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as App).repo
    val month = MutableStateFlow(YearMonth.now())

    /** Tareas de las 6 semanas visibles, agrupadas por día. */
    val tasks = month.flatMapLatest { m ->
        val first = mondayOf(m.atDay(1))
        repo.observeTasks(first, first.plusDays(41)).map { list -> list.groupBy { it.epochDay } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun shift(months: Long) {
        month.value = month.value.plusMonths(months)
    }

    fun save(t: Task) = viewModelScope.launch { repo.saveTask(t) }
    fun delete(t: Task) = viewModelScope.launch { repo.deleteTask(t.id) }
    fun setDone(t: Task, done: Boolean) = viewModelScope.launch { repo.setTaskDone(t.id, done) }
}

private val monthFmt = DateTimeFormatter.ofPattern("MMMM yyyy", ES)

@Composable
fun TasksScreen(vm: TasksViewModel, onOpenProfile: () -> Unit) {
    val month by vm.month.collectAsStateWithLifecycle()
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    val today = LocalDate.now()
    var openDay by rememberSaveable { mutableStateOf<Long?>(null) }
    var editing by remember { mutableStateOf<Task?>(null) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editing = Task(epochDay = (openDay ?: today.toEpochDay()), title = "") },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Nueva tarea") },
            )
        },
    ) { inner ->
        Column(
            Modifier.fillMaxSize().padding(inner).windowInsetsPadding(WindowInsets.statusBars)
                .verticalScroll(rememberScrollState()).padding(horizontal = 12.dp),
        ) {
            Row(Modifier.fillMaxWidth().padding(start = 8.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Tareas", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        month.format(monthFmt).replaceFirstChar { it.uppercase(ES) },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (month != YearMonth.now()) TextButton(onClick = { vm.month.value = YearMonth.now() }) { Text("Hoy") }
                IconButton(onClick = { vm.shift(-1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Mes anterior") }
                IconButton(onClick = { vm.shift(1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Mes siguiente") }
                TopActions("Tareas", HelpTexts.TASKS, onOpenProfile)
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth()) {
                DAY_SHORT.forEach {
                    Text(
                        it, Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            AnimatedContent(
                targetState = month,
                transitionSpec = {
                    val dir = if (targetState > initialState) 1 else -1
                    slideInHorizontally { it * dir } togetherWith slideOutHorizontally { -it * dir }
                },
                label = "mes",
            ) { m -> Box(Modifier.appear(1)) { MonthGrid(m, today, tasks) { openDay = it.toEpochDay() } } }
            Spacer(Modifier.height(96.dp))
        }
    }

    openDay?.let { day ->
        DaySheet(
            date = LocalDate.ofEpochDay(day),
            tasks = tasks[day].orEmpty(),
            onDismiss = { openDay = null },
            onAdd = { editing = Task(epochDay = day, title = "") },
            onEdit = { editing = it },
            onCheck = { t, done -> vm.setDone(t, done) },
        )
    }
    editing?.let { t ->
        TaskEditorDialog(
            initial = t,
            onDismiss = { editing = null },
            onDelete = if (t.id != 0L) ({ vm.delete(t); editing = null }) else null,
        ) {
            vm.save(it)
            editing = null
        }
    }
}

@Composable
private fun MonthGrid(month: YearMonth, today: LocalDate, tasks: Map<Long, List<Task>>, onClick: (LocalDate) -> Unit) {
    val first = mondayOf(month.atDay(1))
    val weeks = if (first.plusDays(35).month == month.month) 6 else 5
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(weeks) { w ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(7) { d ->
                    val date = first.plusDays((w * 7 + d).toLong())
                    DayCell(
                        date = date,
                        inMonth = date.month == month.month,
                        isToday = date == today,
                        tasks = tasks[date.toEpochDay()].orEmpty(),
                        modifier = Modifier.weight(1f),
                    ) { onClick(date) }
                }
            }
        }
    }
}

@Composable
private fun DayCell(date: LocalDate, inMonth: Boolean, isToday: Boolean, tasks: List<Task>, modifier: Modifier, onClick: () -> Unit) {
    val alpha = if (inMonth) 1f else 0.35f
    Column(
        modifier
            .heightIn(min = 84.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = if (inMonth) 1f else 0.4f))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(28.dp).clip(CircleShape)
                .then(if (isToday) Modifier.background(MaterialTheme.colorScheme.primary) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "${date.dayOfMonth}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                color = (if (isToday) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface).copy(alpha = alpha),
            )
        }
        Spacer(Modifier.height(4.dp))
        tasks.take(2).forEach { t ->
            Text(
                t.title,
                fontSize = 9.sp,
                lineHeight = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textDecoration = if (t.done) TextDecoration.LineThrough else null,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = alpha),
                modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = if (t.done) 0.4f else 1f))
                    .padding(horizontal = 3.dp),
            )
        }
        if (tasks.size > 2) {
            Text("+${tasks.size - 2}", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DaySheet(
    date: LocalDate,
    tasks: List<Task>,
    onDismiss: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (Task) -> Unit,
    onCheck: (Task, Boolean) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Text(fmtLongDate(date), style = MaterialTheme.typography.headlineSmall)
            Text(
                if (tasks.isEmpty()) "Nada pendiente" else "${tasks.count { !it.done }} pendientes de ${tasks.size}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 8.dp),
                modifier = Modifier.heightIn(max = 480.dp),
            ) {
                items(tasks, key = { it.id }) { t ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth().clickable { onEdit(t) },
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                            Checkbox(checked = t.done, onCheckedChange = { onCheck(t, it) })
                            Column(Modifier.weight(1f).padding(top = 10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        t.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        textDecoration = if (t.done) TextDecoration.LineThrough else null,
                                        modifier = Modifier.weight(1f),
                                    )
                                    t.timeMin?.let {
                                        Text(fmtTime(it), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                                    }
                                    if (t.notify) {
                                        Spacer(Modifier.width(6.dp))
                                        Icon(KtIcons.Bell, "Recordatorio", Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                                    }
                                }
                                if (t.description.isNotBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(t.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
            FilledTonalButton(onClick = onAdd, modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                Icon(Icons.Default.Add, null)
                Text(" Añadir tarea este día")
            }
        }
    }
}
