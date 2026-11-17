package com.sunless404.keeptrack.domain

import kotlin.math.pow

/** Qué da cada acción: experiencia (sin tope) y monedas (con tope). */
enum class RewardKind(val xp: Int, val coins: Int, val label: String, val capped: Boolean = true) {
    BLOCK(5, 2, "Bloque hecho"),
    TASK(5, 3, "Tarea hecha"),
    DAY_COMPLETE(25, 15, "Día completo"),
    GYM_SESSION(20, 10, "Sesión de gimnasio"),
    ACHIEVEMENT(100, 50, "Logro"),
    /** Fuera del tope: subir una racha cuesta días reales y no se puede acelerar. */
    STREAK_LEVEL(50, 0, "Racha sube de nivel", capped = false),
    /** Las de subir de nivel no cuentan para el tope: ya las limita la experiencia. */
    LEVEL_UP(0, 50, "Subida de nivel", capped = false),
}

/** Monedas por cada nivel de racha alcanzado. */
fun streakLevelCoins(level: StreakLevel): Int = when (level) {
    StreakLevel.NONE -> 0
    StreakLevel.SPARK -> 20
    StreakLevel.FLAME -> 40
    StreakLevel.BONFIRE -> 80
    StreakLevel.BLAZE -> 150
    StreakLevel.LEGEND -> 300
    StreakLevel.NOVA -> 450
    StreakLevel.SUPERNOVA -> 600
}

/** Nivel del usuario a partir de la experiencia total. */
data class LevelInfo(val level: Int, val xpInLevel: Int, val xpForNext: Int, val title: String) {
    val progress: Float get() = if (xpForNext == 0) 1f else xpInLevel.toFloat() / xpForNext
}

object EconomyRules {
    const val DAILY_CAP = 120
    const val WEEKLY_CAP = 600
    const val MONTHLY_CAP = 2000
    const val MAX_LEVEL = 100

    /** Las recompensas por marcar algo solo cuentan para hoy y ayer (nada de rellenar el pasado). */
    const val GRACE_DAYS = 1

    /** Experiencia total para llegar al nivel [n]: 100 × (n − 1)^1,5. */
    fun xpToReach(n: Int): Int = (100 * (n - 1).coerceAtLeast(0).toDouble().pow(1.5)).toInt()

    fun level(totalXp: Int): LevelInfo {
        var n = 1
        while (n < MAX_LEVEL && totalXp >= xpToReach(n + 1)) n++
        val base = xpToReach(n)
        val next = if (n >= MAX_LEVEL) 0 else xpToReach(n + 1) - base
        return LevelInfo(n, (totalXp - base).coerceAtLeast(0), next, title(n))
    }

    fun title(level: Int): String = when {
        level >= 100 -> "Inmortal"
        level >= 75 -> "Mito"
        level >= 50 -> "Leyenda"
        level >= 40 -> "Maestro"
        level >= 30 -> "Imparable"
        level >= 20 -> "Disciplinado"
        level >= 10 -> "Constante"
        else -> "Novato"
    }

    /** Niveles que regalan un marco exclusivo (se podrán equipar cuando llegue la tienda). */
    val FRAME_LEVELS = setOf(10, 25, 50, 100)

    /**
     * Monedas que se conceden de verdad, respetando los topes.
     * [earnedToday], [earnedWeek] y [earnedMonth] son las ya ganadas (con tope) en cada periodo.
     */
    fun capCoins(raw: Int, earnedToday: Int, earnedWeek: Int, earnedMonth: Int): Int =
        minOf(raw, DAILY_CAP - earnedToday, WEEKLY_CAP - earnedWeek, MONTHLY_CAP - earnedMonth).coerceAtLeast(0)
}
