package com.sunless404.keeptrack.ui.today

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sunless404.keeptrack.App
import com.sunless404.keeptrack.data.DayEntry
import com.sunless404.keeptrack.data.DayType
import com.sunless404.keeptrack.data.Status
import com.sunless404.keeptrack.data.Task
import com.sunless404.keeptrack.notify.Alarms
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

/** Todo lo que muestra una página (un día) de Hoy. */
data class TodayUi(
    val date: LocalDate = LocalDate.now(),
    val today: LocalDate = LocalDate.now(),
    /** Minutos desde las 00:00 de hoy. */
    val nowMin: Int = 0,
    val entries: List<DayEntry> = emptyList(),
    /** Bloques del día anterior que terminan en este (p. ej. el sueño). */
    val carry: List<DayEntry> = emptyList(),
    val dayType: DayType = DayType.NORMAL,
    val tasks: List<Task> = emptyList(),
) {
    val isToday get() = date == today
    val isFuture get() = date > today

    /** Bloques del día sin los quitados de hoy. */
    val visibleEntries get() = entries.filter { it.status != Status.SALTADO }

    private val total get() = visibleEntries.size + tasks.size
    val done get() = visibleEntries.count { it.status == Status.HECHO } + tasks.count { it.done }
    val totalCount get() = total
    val progress get() = if (total == 0) 0f else done.toFloat() / total

    /** Bloque en curso (solo si se está viendo hoy). */
    val current: DayEntry?
        get() = if (!isToday) null else
            visibleEntries.lastOrNull { nowMin >= it.startMin && nowMin < it.endMin }
                ?: carry.firstOrNull { it.status != Status.SALTADO && nowMin < it.endMin - 1440 }

    val next: DayEntry?
        get() = if (!isToday) null else visibleEntries.firstOrNull { it.startMin > nowMin && it.status != Status.HECHO }
}

class TodayViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as App).repo

    val now: StateFlow<LocalDateTime> = flow {
        while (true) {
            repo.ensureMaterialized()
            emit(LocalDateTime.now())
            delay(15_000)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LocalDateTime.now())

    /** Peticiones de saltar a una fecha (desde otra pantalla o el calendario). */
    private val _jump = MutableSharedFlow<LocalDate>(extraBufferCapacity = 1)
    val jump: SharedFlow<LocalDate> = _jump

    fun setDate(d: LocalDate) {
        _jump.tryEmit(d)
    }

    fun page(date: LocalDate): Flow<TodayUi> = combine(
        repo.observeRange(date.minusDays(1), date),
        repo.observeTasks(date, date),
        now,
    ) { days, tasks, now ->
        val day = days[date.toEpochDay()]
        TodayUi(
            date = date,
            today = now.toLocalDate(),
            nowMin = now.hour * 60 + now.minute,
            entries = day?.entries.orEmpty(),
            carry = days[date.toEpochDay() - 1]?.entries.orEmpty().filter { it.endMin > 1440 },
            dayType = day?.type ?: DayType.NORMAL,
            tasks = tasks,
        )
    }

    fun setStatus(entry: DayEntry, status: Status) {
        if (entry.id == 0L) return
        viewModelScope.launch {
            repo.setStatus(
                entry.id, status,
                snoozeUntil = if (status == Status.POSPUESTO) System.currentTimeMillis() + Alarms.SNOOZE_MS else null,
            )
        }
    }

    fun applyUnforeseen(changed: List<DayEntry>) {
        viewModelScope.launch { repo.applyUnforeseen(changed) }
    }

    fun setDayType(date: LocalDate, type: DayType) {
        viewModelScope.launch { repo.setDayType(date, type) }
    }

    fun setTaskDone(task: Task, done: Boolean) {
        viewModelScope.launch { repo.setTaskDone(task.id, done) }
    }

    fun saveTask(task: Task) {
        viewModelScope.launch { repo.saveTask(task) }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch { repo.deleteTask(task.id) }
    }
}
