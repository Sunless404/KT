package com.sunless404.keeptrack

import com.sunless404.keeptrack.domain.Achievements
import com.sunless404.keeptrack.domain.ProfileTotals
import com.sunless404.keeptrack.domain.StreakLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StreakLevelTest {
    @Test
    fun levelsByDays() {
        assertEquals(StreakLevel.NONE, StreakLevel.of(0))
        assertEquals(StreakLevel.SPARK, StreakLevel.of(2))
        assertEquals(StreakLevel.FLAME, StreakLevel.of(3))
        assertEquals(StreakLevel.BONFIRE, StreakLevel.of(13))
        assertEquals(StreakLevel.BLAZE, StreakLevel.of(14))
        assertEquals(StreakLevel.LEGEND, StreakLevel.of(59))
        assertEquals(StreakLevel.NOVA, StreakLevel.of(60))
        assertEquals(StreakLevel.SUPERNOVA, StreakLevel.of(400))
    }

    @Test
    fun progressToNextLevel() {
        assertEquals(0f, StreakLevel.progress(3), 0.001f)
        assertEquals(0.5f, StreakLevel.progress(5), 0.001f)
        assertEquals(0.5f, StreakLevel.progress(45), 0.001f)
        assertEquals(1f, StreakLevel.progress(100), 0.001f)
    }

    @Test
    fun achievementsUnlock() {
        val none = Achievements.of(ProfileTotals())
        assertTrue(none.none { it.unlocked })
        val some = Achievements.of(ProfileTotals(blocksDone = 120, bestStreak = 8, gymSessions = 1))
        assertEquals(setOf("Primer paso", "Constancia", "En llamas", "Al hierro"), some.filter { it.unlocked }.map { it.title }.toSet())
    }
}
