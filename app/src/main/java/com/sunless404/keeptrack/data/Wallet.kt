package com.sunless404.keeptrack.data

import com.sunless404.keeptrack.domain.EconomyRules
import com.sunless404.keeptrack.domain.LevelInfo
import com.sunless404.keeptrack.domain.RewardKind
import com.sunless404.keeptrack.domain.mondayOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate

/** Aviso para la interfaz cuando se gana algo. */
data class RewardEvent(
    val xp: Int,
    val coins: Int,
    val label: String,
    /** Celebración grande (confeti) en lugar del aviso pequeño. */
    val celebrate: Boolean = false,
    val levelUp: LevelInfo? = null,
)

data class WalletState(
    val totalXp: Int = 0,
    val coins: Int = 0,
    val level: LevelInfo = EconomyRules.level(0),
    val today: Int = 0,
    val week: Int = 0,
    val month: Int = 0,
    val history: List<Reward> = emptyList(),
)

/**
 * Experiencia y monedas. Todo pasa por un registro de movimientos con clave única,
 * así no se puede cobrar dos veces lo mismo y al desmarcar algo se devuelve.
 */
class Wallet(private val dao: RoutineDao) {
    private val mutex = Mutex()
    private val _events = MutableSharedFlow<RewardEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<RewardEvent> = _events

    fun observe(): Flow<WalletState> = dao.observeRewards().map { list ->
        val today = LocalDate.now()
        val t = today.toEpochDay()
        val monday = mondayOf(today).toEpochDay()
        val first = today.withDayOfMonth(1).toEpochDay()
        fun earned(from: Long) = list.filter { it.capped && it.coins > 0 && it.epochDay in from..t }.sumOf { it.coins }
        val xp = list.sumOf { it.xp }
        WalletState(
            totalXp = xp,
            coins = list.sumOf { it.coins },
            level = EconomyRules.level(xp),
            today = earned(t),
            week = earned(monday),
            month = earned(first),
            history = list,
        )
    }

    /**
     * Concede una recompensa si no se había concedido ya. [sourceDay] es el día al que
     * pertenece lo marcado: solo cuenta hoy o ayer.
     */
    suspend fun grant(
        key: String,
        kind: RewardKind,
        sourceDay: Long = LocalDate.now().toEpochDay(),
        coins: Int = kind.coins,
        xp: Int = kind.xp,
        label: String = kind.label,
        celebrate: Boolean = false,
    ) {
        val event = mutex.withLock {
            if (dao.reward(key) != null) return@withLock null
            val today = LocalDate.now()
            val t = today.toEpochDay()
            if (sourceDay < t - EconomyRules.GRACE_DAYS || sourceDay > t) return@withLock null
            val before = EconomyRules.level(dao.totalXp())
            val granted = if (kind.capped) {
                EconomyRules.capCoins(
                    coins,
                    earnedToday = dao.cappedCoinsBetween(t, t),
                    earnedWeek = dao.cappedCoinsBetween(mondayOf(today).toEpochDay(), t),
                    earnedMonth = dao.cappedCoinsBetween(today.withDayOfMonth(1).toEpochDay(), t),
                )
            } else coins
            dao.insertReward(
                Reward(
                    key = key, kind = kind.name, xp = xp, coins = granted, capped = kind.capped,
                    epochDay = t, createdAt = System.currentTimeMillis(), label = label,
                )
            )
            val after = EconomyRules.level(dao.totalXp())
            // Cada nivel nuevo regala monedas (fuera del tope).
            for (n in before.level + 1..after.level) {
                if (dao.reward("level:$n") == null) {
                    dao.insertReward(
                        Reward(
                            key = "level:$n", kind = RewardKind.LEVEL_UP.name, xp = 0, coins = RewardKind.LEVEL_UP.coins,
                            capped = false, epochDay = t, createdAt = System.currentTimeMillis(), label = "Nivel $n",
                        )
                    )
                }
            }
            RewardEvent(
                xp = xp,
                coins = granted + if (after.level > before.level) RewardKind.LEVEL_UP.coins * (after.level - before.level) else 0,
                label = label,
                celebrate = celebrate || after.level > before.level,
                levelUp = after.takeIf { it.level > before.level },
            )
        }
        event?.let { _events.tryEmit(it) }
    }

    /** Devuelve lo ganado con [key] (al desmarcar o borrar). */
    suspend fun revoke(key: String) = mutex.withLock { dao.deleteReward(key) }
}
