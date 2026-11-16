package com.sunless404.keeptrack

import com.sunless404.keeptrack.data.GymSession
import com.sunless404.keeptrack.data.GymSet
import com.sunless404.keeptrack.domain.GymScore
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class GymScoreTest {
    // Miércoles; la semana empieza el lunes 9.
    private val today = LocalDate.of(2026, 11, 11)
    private fun day(d: LocalDate) = d.toEpochDay()

    @Test
    fun epley() {
        assertEquals(100.0, GymScore.e1rm(100.0, 1), 0.001)
        assertEquals(116.667, GymScore.e1rm(100.0, 5), 0.001)
        // A partir de 12 repeticiones ya no sube.
        assertEquals(GymScore.e1rm(50.0, 12), GymScore.e1rm(50.0, 20), 0.001)
        assertEquals(0.0, GymScore.e1rm(0.0, 5), 0.001)
    }

    @Test
    fun weeklyPointsCountSessionsPrsAndStreak() {
        val sessions = listOf(
            GymSession(1, day(today.minusWeeks(1)), bodyWeightKg = 80.0),
            GymSession(2, day(today.minusDays(2)), bodyWeightKg = 80.0), // lunes
            GymSession(3, day(today), bodyWeightKg = 80.0),
        )
        val sets = listOf(
            GymSet(sessionId = 1, exercise = "Press banca", weightKg = 80.0, reps = 5),
            GymSet(sessionId = 2, exercise = "press  BANCA", weightKg = 84.0, reps = 5), // +5 % → 25 pts
            GymSet(sessionId = 3, exercise = "Sentadilla", weightKg = 100.0, reps = 3), // ejercicio nuevo → 5 pts
        )
        val s = GymScore.summarize(sessions, sets, today)
        assertEquals(2, s.sessionsThisWeek)
        assertEquals(1, s.prsThisWeek)
        assertEquals(2, s.streakWeeks)
        // 2 sesiones × 10 + 25 + 5 + racha 2 × 5
        assertEquals(20 + 25 + 5 + 10, s.weekPoints)
        assertEquals(2, s.records.size)
        // Fuerza relativa: banca 98/80 + sentadilla 110/80.
        assertEquals((84 * (1 + 5 / 30.0)) / 80 * 100 + 110.0 / 80 * 100, s.strengthScore.toDouble(), 1.0)
    }

    @Test
    fun streakToleratesNoSessionYetThisWeek() {
        val sessions = listOf(
            GymSession(1, day(today.minusWeeks(1))),
            GymSession(2, day(today.minusWeeks(2))),
            GymSession(3, day(today.minusWeeks(4))),
        )
        assertEquals(2, GymScore.streakWeeks(sessions, today))
    }

    @Test
    fun withoutBodyWeightThereIsNoRelativeScore() {
        val s = GymScore.summarize(
            listOf(GymSession(1, day(today))),
            listOf(GymSet(sessionId = 1, exercise = "Dominadas", weightKg = 10.0, reps = 8)),
            today,
        )
        assertEquals(0, s.strengthScore)
        assertEquals(null, s.records.single().relative)
    }
}
