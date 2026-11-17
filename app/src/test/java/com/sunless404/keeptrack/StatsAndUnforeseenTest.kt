package com.sunless404.keeptrack

import com.sunless404.keeptrack.data.Category
import com.sunless404.keeptrack.data.DayData
import com.sunless404.keeptrack.data.DayEntry
import com.sunless404.keeptrack.data.DayType
import com.sunless404.keeptrack.data.Status
import com.sunless404.keeptrack.domain.Gap
import com.sunless404.keeptrack.domain.Habit
import com.sunless404.keeptrack.domain.Stats
import com.sunless404.keeptrack.domain.Unforeseen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class StatsAndUnforeseenTest {
    private val zone = ZoneOffset.UTC
    private var nextId = 1L

    private fun e(
        day: Long, cat: Category, start: Int, end: Int,
        status: Status = Status.PENDIENTE, markedAt: Long? = null,
    ) = DayEntry(
        id = nextId++, epochDay = day, blockId = null, title = cat.label, category = cat,
        startMin = start, endMin = end, status = status, markedAt = markedAt,
    )

    private fun day(d: Long, type: DayType = DayType.NORMAL, vararg entries: DayEntry) = DayData(d, entries.toList(), type)

    // ---------- Estadísticas ----------

    @Test
    fun hoursByCategory() {
        val h = Stats.hoursByCategory(
            listOf(
                e(1, Category.ESTUDIO, 600, 720, Status.HECHO),
                e(2, Category.ESTUDIO, 600, 660),
                e(1, Category.GIMNASIO, 1140, 1230, Status.SALTADO),
            )
        )
        assertEquals(180, h.first { it.category == Category.ESTUDIO }.plannedMin)
        assertEquals(120, h.first { it.category == Category.ESTUDIO }.doneMin)
        // Lo quitado de un día no cuenta como previsto.
        assertTrue(h.none { it.category == Category.GIMNASIO })
    }

    @Test
    fun removedBlockDoesNotBreakStreak() {
        val days = listOf(
            day(1, DayType.NORMAL, e(1, Category.ESTUDIO, 600, 660, Status.HECHO)),
            day(2, DayType.NORMAL, e(2, Category.ESTUDIO, 600, 660, Status.SALTADO)),
            day(3, DayType.NORMAL, e(3, Category.ESTUDIO, 600, 660, Status.HECHO)),
        )
        assertEquals(2, Stats.streaks(days, todayEpoch = 3).first { it.habit == Habit.ESTUDIO }.current)
    }

    @Test
    fun streakCountsConsecutiveDaysAndSkipsDaysWithoutHabit() {
        val days = listOf(
            day(1, DayType.NORMAL, e(1, Category.ESTUDIO, 600, 660, Status.HECHO)),
            day(2, DayType.NORMAL, e(2, Category.ESTUDIO, 600, 660)), // fallo
            day(3, DayType.NORMAL, e(3, Category.ESTUDIO, 600, 660, Status.HECHO)),
            day(4, DayType.NORMAL, e(4, Category.COMIDA, 600, 660)), // sin estudio: no cuenta
            day(5, DayType.NORMAL, e(5, Category.ESTUDIO, 600, 660, Status.HECHO)),
        )
        val s = Stats.streaks(days, todayEpoch = 5, zone = zone).first { it.habit == Habit.ESTUDIO }
        assertEquals(2, s.current)
        assertEquals(2, s.best)
    }

    @Test
    fun holidayDoesNotBreakStreak() {
        val days = listOf(
            day(1, DayType.NORMAL, e(1, Category.GIMNASIO, 600, 660, Status.HECHO)),
            day(2, DayType.FESTIVO, e(2, Category.GIMNASIO, 600, 660)),
            day(3, DayType.LIBRE, e(3, Category.GIMNASIO, 600, 660, Status.SALTADO)),
            day(4, DayType.NORMAL, e(4, Category.GIMNASIO, 600, 660, Status.HECHO)),
        )
        assertEquals(2, Stats.streaks(days, 4, zone).first { it.habit == Habit.GIMNASIO }.current)
    }

    @Test
    fun unfinishedTodayDoesNotBreakStreakButPastDayDoes() {
        val days = listOf(
            day(1, DayType.NORMAL, e(1, Category.LECTURA, 600, 660, Status.HECHO)),
            day(2, DayType.NORMAL, e(2, Category.LECTURA, 600, 660)),
        )
        assertEquals(1, Stats.streaks(days, todayEpoch = 2, zone = zone).first { it.habit == Habit.LECTURA }.current)
        assertEquals(0, Stats.streaks(days, todayEpoch = 3, zone = zone).first { it.habit == Habit.LECTURA }.current)
    }

    @Test
    fun sleepStreakNeedsMarkingOnTime() {
        fun at(day: Long, min: Int) = LocalDate.ofEpochDay(day).atStartOfDay(zone).plusMinutes(min.toLong()).toInstant().toEpochMilli()
        val days = listOf(
            day(1, DayType.NORMAL, e(1, Category.SUENO, 1380, 1860, Status.HECHO, at(1, 1395))), // 23:15, a su hora
            day(2, DayType.NORMAL, e(2, Category.SUENO, 1380, 1860, Status.HECHO, at(2, 1380 + 90))), // 00:30, tarde
            day(3, DayType.NORMAL, e(3, Category.SUENO, 1380, 1860, Status.HECHO, at(3, 1360))), // 22:40, a su hora
        )
        val s = Stats.streaks(days, 4, zone).first { it.habit == Habit.SUENO }
        assertEquals(1, s.current)
        assertEquals(1, s.best)
    }

    @Test
    fun historyGroupsByWeek() {
        val monday = LocalDate.of(2026, 11, 9).toEpochDay()
        val days = listOf(
            day(monday, DayType.NORMAL, e(monday, Category.ESTUDIO, 600, 660, Status.HECHO), e(monday, Category.COMIDA, 700, 760)),
            day(monday + 6, DayType.NORMAL, e(monday + 6, Category.ESTUDIO, 600, 660, Status.HECHO)),
            day(monday + 7, DayType.NORMAL, e(monday + 7, Category.ESTUDIO, 600, 660)),
        )
        val h = Stats.history(days, monday + 7)
        assertEquals(listOf(monday + 7, monday), h.map { it.mondayEpoch })
        assertEquals(66, h[1].percent)
    }

    // ---------- Imprevistos ----------

    private val today = listOf(
        e(10, Category.CLASE, 510, 870),      // 08:30-14:30
        e(10, Category.COMIDA, 900, 960),     // 15:00-16:00
        e(10, Category.ESTUDIO, 1020, 1140),  // 17:00-19:00
        e(10, Category.SUENO, 1380, 1860),    // 23:00-07:00
    )
    private val carry = listOf(e(9, Category.SUENO, 1380, 1860)) // hasta las 07:00 de hoy

    @Test
    fun freeSlotsNeverIncludeSleep() {
        val slots = Unforeseen.freeSlots(today, carry, fromMin = 0, durationMin = 60)
        assertEquals(listOf(Gap(420, 510), Gap(960, 1020), Gap(1140, 1380)), slots)
    }

    @Test
    fun freeSlotsStartFromNowAndIgnoreMovedBlock() {
        val study = today[2]
        val slots = Unforeseen.freeSlots(today, carry, fromMin = 930, durationMin = 120, ignoreId = study.id)
        assertEquals(listOf(Gap(960, 1380)), slots)
    }

    @Test
    fun moveAndShortenKeepOrCutDuration() {
        val moved = Unforeseen.move(today[2], 1200)
        assertEquals(1200, moved.startMin)
        assertEquals(1320, moved.endMin)
        assertEquals(1080, Unforeseen.shorten(today[2], 60).endMin)
        assertEquals(1140, Unforeseen.shorten(today[2], 999).endMin)
    }

    @Test
    fun delayRestMovesOnlyPendingFutureNonSleepBlocks() {
        val changed = Unforeseen.delayRest(today, nowMin = 880, minutes = 30)
        assertEquals(listOf(Category.COMIDA, Category.ESTUDIO), changed.map { it.category })
        assertEquals(listOf(930, 1050), changed.map { it.startMin })
        assertFalse(Unforeseen.entersSleep(today, carry, changed))
    }

    @Test
    fun delayIntoSleepIsDetected() {
        val changed = Unforeseen.delayRest(today, nowMin = 880, minutes = 300)
        assertTrue(Unforeseen.entersSleep(today, carry, changed))
        assertTrue(Unforeseen.sleepHits(today, carry, 300, 400).isNotEmpty()) // 05:00, sueño de ayer
        assertTrue(Unforeseen.sleepHits(today, carry, 600, 700).isEmpty())
    }
}
