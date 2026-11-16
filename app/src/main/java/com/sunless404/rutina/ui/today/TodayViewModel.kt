package com.sunless404.rutina.ui.today

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sunless404.rutina.App
import com.sunless404.rutina.data.DayEntry
import com.sunless404.rutina.data.DayType
import com.sunless404.rutina.data.Status
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

data class TodayUi(
    val date: LocalDate = LocalDate.now(),
    val today: LocalDate = LocalDate.now(),
    /** Minutos desde las 00:00 de hoy. */
    val nowMin: Int = 0,
    val entries: List<DayEntry> = emptyList(),
    /** Bloques del día anterior que terminan en este (p. ej. el sueño). */
    val carry: List<DayEntry> = emptyList(),
    val dayType: DayType = DayType.NORMAL,
) {
    val isToday get() = date == today
    val isFuture get() = date > today

    val done get() = entries.count { it.status == Status.HECHO }
    val progress get() = if (entries.isEmpty()) 0f else done.toFloat() / entries.size

    /** Bloque en curso (solo si se está viendo hoy). */
    val current: DayEntry?
        get() = if (!isToday) null else
            entries.lastOrNull { nowMin >= it.startMin && nowMin < it.endMin }
                ?: carry.firstOrNull { nowMin < it.endMin - 1440 }

    val next: DayEntry?
        get() = if (!isToday) null else entries.firstOrNull { it.startMin > nowMin }
}

class TodayViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as App).repo
    private val date = MutableStateFlow(LocalDate.now())

    private val clock = flow {
        while (true) {
            repo.ensureMaterialized()
            emit(LocalDateTime.now())
            delay(15_000)
        }
    }

    val ui: StateFlow<TodayUi> = combine(
        date.flatMapLatest { d -> repo.observeRange(d.minusDays(1), d).map { d to it } },
        clock,
    ) { (d, days), now ->
        val day = days[d.toEpochDay()]
        TodayUi(
            date = d,
            today = now.toLocalDate(),
            nowMin = now.hour * 60 + now.minute,
            entries = day?.entries.orEmpty(),
            carry = days[d.toEpochDay() - 1]?.entries.orEmpty().filter { it.endMin > 1440 },
            dayType = day?.type ?: DayType.NORMAL,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUi())

    fun setDate(d: LocalDate) {
        date.value = d
    }

    fun shiftDate(days: Long) {
        date.value = date.value.plusDays(days)
    }

    fun setStatus(entry: DayEntry, status: Status) {
        if (entry.id == 0L) return
        viewModelScope.launch { repo.setStatus(entry.id, status) }
    }
}
