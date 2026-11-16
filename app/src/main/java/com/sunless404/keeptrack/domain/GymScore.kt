package com.sunless404.keeptrack.domain

import com.sunless404.keeptrack.data.GymSession
import com.sunless404.keeptrack.data.GymSet
import java.time.LocalDate
import kotlin.math.roundToInt

/** Mejor marca de un ejercicio. */
data class ExerciseRecord(
    val exercise: String,
    val bestE1rm: Double,
    val bestSet: GymSet,
    val epochDay: Long,
    /** Máximo estimado dividido por el peso corporal (null si no hay peso). */
    val relative: Double?,
)

data class GymSummary(
    val weekPoints: Int,
    val sessionsThisWeek: Int,
    val prsThisWeek: Int,
    val streakWeeks: Int,
    /** Fuerza relativa total: suma de los 5 mejores ejercicios × 100. */
    val strengthScore: Int,
    val records: List<ExerciseRecord>,
    val bodyWeightKg: Double?,
)

/**
 * Sistema de puntos del gimnasio.
 *
 * - Máximo estimado a 1 repetición (Epley): peso × (1 + reps / 30), contando hasta 12 reps.
 * - Fuerza relativa: ese máximo dividido por el peso corporal, para comparar a gente de distinto peso.
 * - Puntos semanales (se reinician el lunes):
 *   constancia: 10 por sesión, hasta 5 sesiones;
 *   récords: por cada ejercicio que mejora su máximo, 5 × % de mejora (entre 5 y 50);
 *            un ejercicio nuevo da 5;
 *   racha: 5 por cada semana seguida con al menos una sesión, hasta 25.
 * - Clasificación histórica: fuerza relativa total.
 */
object GymScore {
    const val POINTS_PER_SESSION = 10
    const val MAX_SESSIONS = 5
    const val NEW_EXERCISE_POINTS = 5
    const val MAX_PR_POINTS = 50
    const val STREAK_POINTS = 5
    const val MAX_STREAK_POINTS = 25

    fun e1rm(weightKg: Double, reps: Int): Double = when {
        reps <= 0 || weightKg <= 0 -> 0.0
        reps == 1 -> weightKg
        else -> weightKg * (1 + reps.coerceAtMost(12) / 30.0)
    }

    /** Nombre de ejercicio normalizado: sin tildes, mayúsculas ni espacios de más. */
    fun normalize(name: String): String = RoutineParser.normalize(name).replace(Regex("\\s+"), " ")

    fun summarize(
        sessions: List<GymSession>,
        sets: List<GymSet>,
        today: LocalDate,
        fallbackBodyWeight: Double? = null,
    ): GymSummary {
        val byId = sessions.associateBy { it.id }
        val monday = mondayOf(today).toEpochDay()
        val bodyWeight = sessions.sortedByDescending { it.epochDay }.firstNotNullOfOrNull { it.bodyWeightKg } ?: fallbackBodyWeight

        // Mejores marcas: históricas, y solo de antes de esta semana (para detectar récords nuevos).
        val valid = sets.filter { byId[it.sessionId] != null && it.reps > 0 && it.weightKg > 0 }
        val groups = valid.groupBy { normalize(it.exercise) }
        val records = groups.map { (_, list) ->
            val best = list.maxBy { e1rm(it.weightKg, it.reps) }
            val e = e1rm(best.weightKg, best.reps)
            ExerciseRecord(best.exercise.trim(), e, best, byId.getValue(best.sessionId).epochDay, bodyWeight?.let { e / it })
        }.sortedByDescending { it.relative ?: it.bestE1rm }

        var prPoints = 0
        var prs = 0
        groups.forEach { (_, list) ->
            val before = list.filter { byId.getValue(it.sessionId).epochDay < monday }.maxOfOrNull { e1rm(it.weightKg, it.reps) }
            val week = list.filter { byId.getValue(it.sessionId).epochDay in monday..today.toEpochDay() }.maxOfOrNull { e1rm(it.weightKg, it.reps) }
                ?: return@forEach
            when {
                before == null -> prPoints += NEW_EXERCISE_POINTS
                week > before -> {
                    prs++
                    prPoints += (5 * (week / before - 1) * 100).roundToInt().coerceIn(5, MAX_PR_POINTS)
                }
            }
        }

        val weekSessions = sessions.count { it.epochDay in monday..today.toEpochDay() }
        val streak = streakWeeks(sessions, today)
        val points = minOf(weekSessions, MAX_SESSIONS) * POINTS_PER_SESSION + prPoints +
            minOf(streak * STREAK_POINTS, MAX_STREAK_POINTS)

        val strength = records.mapNotNull { it.relative }.sortedDescending().take(5).sum()
        return GymSummary(
            weekPoints = points,
            sessionsThisWeek = weekSessions,
            prsThisWeek = prs,
            streakWeeks = streak,
            strengthScore = (strength * 100).roundToInt(),
            records = records,
            bodyWeightKg = bodyWeight,
        )
    }

    /** Semanas seguidas con alguna sesión, contando la actual o, si aún no hay, desde la anterior. */
    fun streakWeeks(sessions: List<GymSession>, today: LocalDate): Int {
        val weeks = sessions.map { mondayOf(LocalDate.ofEpochDay(it.epochDay)) }.toSet()
        var w = mondayOf(today)
        if (w !in weeks) w = w.minusWeeks(1)
        var n = 0
        while (w in weeks) {
            n++
            w = w.minusWeeks(1)
        }
        return n
    }
}
