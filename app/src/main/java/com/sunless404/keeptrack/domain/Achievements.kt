package com.sunless404.keeptrack.domain

/** Totales que usan el perfil y los logros. */
data class ProfileTotals(
    val blocksDone: Int = 0,
    val tasksDone: Int = 0,
    val gymSessions: Int = 0,
    val gymExercises: Int = 0,
    val bestStreak: Int = 0,
    val bestByHabit: Map<Habit, Int> = emptyMap(),
    val currentByHabit: Map<Habit, Int> = emptyMap(),
)

object Totals {
    fun compute(
        days: Collection<com.sunless404.keeptrack.data.DayData>,
        tasks: List<com.sunless404.keeptrack.data.Task>,
        sessions: List<com.sunless404.keeptrack.data.GymSession>,
        sets: List<com.sunless404.keeptrack.data.GymSet>,
        todayEpoch: Long,
    ): Pair<ProfileTotals, List<Streak>> {
        val streaks = Stats.streaks(days, todayEpoch)
        return ProfileTotals(
            blocksDone = days.sumOf { d -> d.entries.count { it.status == com.sunless404.keeptrack.data.Status.HECHO } },
            tasksDone = tasks.count { it.done },
            gymSessions = sessions.size,
            gymExercises = sets.map { GymScore.normalize(it.exercise) }.distinct().size,
            bestStreak = streaks.maxOfOrNull { it.best } ?: 0,
            bestByHabit = streaks.associate { it.habit to it.best },
            currentByHabit = streaks.associate { it.habit to it.current },
        ) to streaks
    }
}

enum class AchievementIcon { STEP, BLOCKS, FIRE, BOOK, BED, DUMBBELL, TASKS, TROPHY }

data class Achievement(
    val title: String,
    val description: String,
    val icon: AchievementIcon,
    val current: Int,
    val goal: Int,
) {
    val unlocked get() = current >= goal
    val progress get() = (current.toFloat() / goal).coerceIn(0f, 1f)
}

object Achievements {
    fun of(t: ProfileTotals): List<Achievement> = listOf(
        Achievement("Primer paso", "Marca tu primer bloque como hecho", AchievementIcon.STEP, t.blocksDone, 1),
        Achievement("Constancia", "Completa 100 bloques", AchievementIcon.BLOCKS, t.blocksDone, 100),
        Achievement("Máquina", "Completa 500 bloques", AchievementIcon.BLOCKS, t.blocksDone, 500),
        Achievement("En llamas", "Consigue una racha de 7 días", AchievementIcon.FIRE, t.bestStreak, 7),
        Achievement("Imparable", "Consigue una racha de 30 días", AchievementIcon.FIRE, t.bestStreak, 30),
        Achievement("Ratón de biblioteca", "14 días seguidos leyendo", AchievementIcon.BOOK, t.bestByHabit[Habit.LECTURA] ?: 0, 14),
        Achievement("Buen descanso", "7 noches seguidas durmiendo a tu hora", AchievementIcon.BED, t.bestByHabit[Habit.SUENO] ?: 0, 7),
        Achievement("Al hierro", "Registra tu primera sesión de gimnasio", AchievementIcon.DUMBBELL, t.gymSessions, 1),
        Achievement("Polivalente", "Entrena 5 ejercicios distintos", AchievementIcon.TROPHY, t.gymExercises, 5),
        Achievement("Organizado", "Completa 20 tareas", AchievementIcon.TASKS, t.tasksDone, 20),
    )
}
