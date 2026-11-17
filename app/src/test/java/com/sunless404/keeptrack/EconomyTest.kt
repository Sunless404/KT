package com.sunless404.keeptrack

import com.sunless404.keeptrack.data.DayData
import com.sunless404.keeptrack.data.DayEntry
import com.sunless404.keeptrack.data.Category
import com.sunless404.keeptrack.data.DayType
import com.sunless404.keeptrack.data.Status
import com.sunless404.keeptrack.domain.EconomyRules
import com.sunless404.keeptrack.domain.Habit
import com.sunless404.keeptrack.domain.Stats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EconomyTest {
    @Test
    fun levelCurve() {
        assertEquals(1, EconomyRules.level(0).level)
        assertEquals(1, EconomyRules.level(99).level)
        assertEquals(2, EconomyRules.level(100).level)
        // Nivel 10: 100 × 9^1,5 = 2700 XP (unas 4 semanas a ~100 XP/día).
        assertEquals(2700, EconomyRules.xpToReach(10))
        assertEquals(10, EconomyRules.level(2700).level)
        assertEquals("Constante", EconomyRules.level(2700).title)
        assertEquals(100, EconomyRules.level(10_000_000).level)
        assertEquals(1f, EconomyRules.level(10_000_000).progress, 0.001f)
    }

    @Test
    fun progressInsideLevel() {
        val l = EconomyRules.level(150) // nivel 2: de 100 a 282
        assertEquals(2, l.level)
        assertEquals(50, l.xpInLevel)
        assertEquals(EconomyRules.xpToReach(3) - 100, l.xpForNext)
    }

    @Test
    fun capsLimitCoins() {
        assertEquals(15, EconomyRules.capCoins(15, 0, 0, 0))
        assertEquals(5, EconomyRules.capCoins(15, 115, 200, 300)) // tope diario
        assertEquals(0, EconomyRules.capCoins(15, 120, 200, 300))
        assertEquals(3, EconomyRules.capCoins(15, 10, 597, 900)) // tope semanal
        assertEquals(1, EconomyRules.capCoins(15, 10, 100, 1999)) // tope mensual
        assertEquals(0, EconomyRules.capCoins(15, 200, 0, 0)) // nunca negativo
    }

    @Test
    fun streakStartIsStable() {
        fun day(d: Long, done: Boolean?, type: DayType = DayType.NORMAL) = DayData(
            d,
            if (done == null) emptyList() else listOf(
                DayEntry(id = d, epochDay = d, blockId = 1, title = "Estudio", category = Category.ESTUDIO, startMin = 600, endMin = 660,
                    status = if (done) Status.HECHO else Status.PENDIENTE)
            ),
            type,
        )
        val days = listOf(day(1, false), day(2, true), day(3, null), day(4, true, DayType.FESTIVO), day(5, true))
        val s = Stats.streaks(days, 6).first { it.habit == Habit.ESTUDIO }
        assertEquals(2, s.current)
        assertEquals(2L, s.currentStart) // días sin estudio o festivos no mueven el inicio
        assertNull(Stats.streaks(listOf(day(1, false)), 2).first { it.habit == Habit.ESTUDIO }.currentStart)
    }
}
