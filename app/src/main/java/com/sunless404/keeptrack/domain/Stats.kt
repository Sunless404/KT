package com.sunless404.keeptrack.domain

import com.sunless404.keeptrack.data.Category
import com.sunless404.keeptrack.data.DayData
import com.sunless404.keeptrack.data.DayEntry
import com.sunless404.keeptrack.data.DayType
import com.sunless404.keeptrack.data.Status
import java.time.LocalDate
import java.time.ZoneId

data class CategoryHours(val category: Category, val plannedMin: Int, val doneMin: Int)

enum class Habit(val label: String, val category: Category) {
    ESTUDIO("Estudio", Category.ESTUDIO),
    GIMNASIO("Gimnasio", Category.GIMNASIO),
    LECTURA("Lectura", Category.LECTURA),
    SUENO("Sueño a su hora", Category.SUENO),
}

data class Streak(val habit: Habit, val current: Int, val best: Int)

data class WeekSummary(val mondayEpoch: Long, val total: Int, val done: Int) {
    val percent: Int get() = if (total == 0) 0 else done * 100 / total
}

object Stats {
    /** Margen para considerar que el sueño se marcó "a su hora". */
    const val SLEEP_TOLERANCE_MIN = 30

    /** Minutos previstos y hechos por categoría. */
    fun hoursByCategory(entries: List<DayEntry>): List<CategoryHours> =
        entries.groupBy { it.category }.map { (cat, list) ->
            CategoryHours(
                cat,
                plannedMin = list.sumOf { it.durationMin },
                doneMin = list.filter { it.status == Status.HECHO }.sumOf { it.durationMin },
            )
        }.sortedBy { it.category.ordinal }

    private fun sleepOnTime(e: DayEntry, zone: ZoneId): Boolean {
        val marked = e.markedAt ?: return false
        val start = LocalDate.ofEpochDay(e.epochDay).atStartOfDay(zone).plusMinutes(e.startMin.toLong())
            .toInstant().toEpochMilli()
        return kotlin.math.abs(marked - start) <= SLEEP_TOLERANCE_MIN * 60_000L
    }

    /**
     * Resultado de un hábito en un día: true = cumplido, false = fallado,
     * null = no cuenta (festivo o libre, sin bloques de ese hábito, u hoy aún sin terminar).
     */
    fun outcome(day: DayData, habit: Habit, todayEpoch: Long, zone: ZoneId): Boolean? {
        if (day.type != DayType.NORMAL || day.epochDay > todayEpoch) return null
        val list = day.entries.filter { it.category == habit.category }
        if (list.isEmpty()) return null
        val ok = list.all { it.status == Status.HECHO && (habit != Habit.SUENO || sleepOnTime(it, zone)) }
        return if (!ok && day.epochDay == todayEpoch) null else ok
    }

    /** Rachas de días seguidos por hábito. Los días que no cuentan ni suman ni rompen. */
    fun streaks(days: Collection<DayData>, todayEpoch: Long, zone: ZoneId = ZoneId.systemDefault()): List<Streak> {
        val sorted = days.sortedBy { it.epochDay }
        return Habit.entries.map { habit ->
            val results = sorted.mapNotNull { outcome(it, habit, todayEpoch, zone) }
            var best = 0
            var run = 0
            results.forEach { ok ->
                run = if (ok) run + 1 else 0
                best = maxOf(best, run)
            }
            Streak(habit, current = results.takeLastWhile { it }.size, best = best)
        }
    }

    /** Resumen por semana (lunes a domingo), de la más reciente a la más antigua. */
    fun history(days: Collection<DayData>, todayEpoch: Long): List<WeekSummary> =
        days.filter { it.epochDay <= todayEpoch && it.entries.isNotEmpty() }
            .groupBy { mondayOf(LocalDate.ofEpochDay(it.epochDay)).toEpochDay() }
            .map { (monday, list) ->
                val entries = list.flatMap { it.entries }
                WeekSummary(monday, entries.size, entries.count { it.status == Status.HECHO })
            }
            .sortedByDescending { it.mondayEpoch }
}
