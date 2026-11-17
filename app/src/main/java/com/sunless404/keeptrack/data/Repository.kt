package com.sunless404.keeptrack.data

import androidx.room.withTransaction
import com.sunless404.keeptrack.domain.Achievements
import com.sunless404.keeptrack.domain.RewardKind
import com.sunless404.keeptrack.domain.StreakLevel
import com.sunless404.keeptrack.domain.Totals
import com.sunless404.keeptrack.domain.streakLevelCoins
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate

/** Bloques de un día junto con su tipo (normal, festivo, libre). */
data class DayData(
    val epochDay: Long,
    val entries: List<DayEntry>,
    val type: DayType = DayType.NORMAL,
)

class Repository(private val db: AppDatabase) {
    private val dao = db.dao()
    val wallet = Wallet(dao)
    private val mutex = Mutex()

    /** Se invoca cuando cambia algo que afecta a avisos o widget. */
    var onChanged: () -> Unit = {}

    // ---------- Historial ----------

    /**
     * Congela en el historial todos los días hasta hoy que aún no lo estén,
     * copiando la plantilla vigente. Se llama al abrir la app, al sonar una
     * alarma y antes de cualquier cambio en la plantilla.
     */
    suspend fun ensureMaterialized(today: LocalDate = LocalDate.now()) = mutex.withLock {
        val todayEpoch = today.toEpochDay()
        val last = dao.lastMaterialized()
        if (last != null && last >= todayEpoch) return@withLock
        db.withTransaction {
            val from = maxOf((last ?: (todayEpoch - 1)) + 1, todayEpoch - MAX_BACKFILL_DAYS)
            for (day in from..todayEpoch) {
                val dow = LocalDate.ofEpochDay(day).dayOfWeek.value
                dao.insertEntries(dao.blocksFor(dow).map { it.toEntry(day) })
                val info = dao.dayInfo(day) ?: DayInfo(day)
                dao.upsertDay(info.copy(materialized = true))
            }
        }
    }

    /** Días [from]..[to]: historial para fechas hasta hoy, plantilla para las futuras. */
    fun observeRange(from: LocalDate, to: LocalDate): Flow<Map<Long, DayData>> {
        val a = from.toEpochDay()
        val b = to.toEpochDay()
        return combine(
            dao.observeEntriesBetween(a, b),
            dao.observeBlocks(),
            dao.observeDaysBetween(a, b),
        ) { entries, blocks, days ->
            val today = LocalDate.now().toEpochDay()
            val byDay = entries.groupBy { it.epochDay }
            val types = days.associate { it.epochDay to it.type }
            (a..b).associateWith { day ->
                val list = if (day <= today) {
                    byDay[day].orEmpty()
                } else {
                    val dow = LocalDate.ofEpochDay(day).dayOfWeek.value
                    blocks.filter { it.dayOfWeek == dow }.map { it.toEntry(day) }
                }
                DayData(day, list, types[day] ?: DayType.NORMAL)
            }
        }
    }

    suspend fun range(from: LocalDate, to: LocalDate): Map<Long, DayData> = observeRange(from, to).first()

    suspend fun entriesBetween(from: LocalDate, to: LocalDate): List<DayEntry> =
        dao.entriesBetween(from.toEpochDay(), to.toEpochDay())

    suspend fun entry(id: Long): DayEntry? = dao.entry(id)

    suspend fun setStatus(entryId: Long, status: Status, snoozeUntil: Long? = null) {
        val e = dao.entry(entryId) ?: return
        dao.updateEntry(
            e.copy(
                status = status,
                markedAt = if (status == Status.PENDIENTE) null else System.currentTimeMillis(),
                snoozeUntil = if (status == Status.POSPUESTO) snoozeUntil else null,
            )
        )
        if (status == Status.HECHO) wallet.grant("block:${e.id}", RewardKind.BLOCK, e.epochDay, label = e.title)
        else wallet.revoke("block:${e.id}")
        checkDayComplete(e.epochDay)
        onChanged()
    }

    /** Día completo: todos los bloques y tareas de ese día hechos. */
    private suspend fun checkDayComplete(epochDay: Long) {
        val entries = dao.entriesBetween(epochDay, epochDay)
        val tasks = dao.tasksBetween(epochDay, epochDay)
        val type = dao.dayInfo(epochDay)?.type ?: DayType.NORMAL
        val complete = type == DayType.NORMAL && entries.size + tasks.size > 0 &&
            entries.all { it.status == Status.HECHO } && tasks.all { it.done }
        if (complete) wallet.grant("day:$epochDay", RewardKind.DAY_COMPLETE, epochDay, label = "¡Día completo!", celebrate = true)
        else wallet.revoke("day:$epochDay")
    }

    /**
     * Rachas que suben de nivel y logros desbloqueados. Cada uno se cobra una vez:
     * las rachas, una por nivel y por racha (si se rompe y se vuelve a empezar, cuenta de nuevo).
     */
    suspend fun evaluateMilestones() {
        val today = LocalDate.now()
        val days = range(today.minusDays(400), today)
        val (totals, streaks) = Totals.compute(
            days.values, dao.tasksBetween(today.minusDays(400).toEpochDay(), today.plusDays(400).toEpochDay()),
            dao.allGymSessions(), dao.allGymSets(), today.toEpochDay(),
        )
        streaks.forEach { st ->
            val start = st.currentStart ?: return@forEach
            StreakLevel.entries.filter { it != StreakLevel.NONE && it.minDays <= st.current }.forEach { lvl ->
                wallet.grant(
                    "streak:${st.habit}:${lvl.name}:$start", RewardKind.STREAK_LEVEL,
                    coins = streakLevelCoins(lvl), label = "${st.habit.label}: ${lvl.label}", celebrate = true,
                )
            }
        }
        Achievements.of(totals).filter { it.unlocked }.forEach { a ->
            wallet.grant("ach:${a.title}", RewardKind.ACHIEVEMENT, label = "Logro: ${a.title}", celebrate = true)
        }
    }

    /** Cambios de un imprevisto: solo afectan a esas entradas y dejan de seguir a la plantilla. */
    suspend fun applyUnforeseen(entries: List<DayEntry>) {
        dao.updateEntries(entries.map { it.copy(modified = true) })
        onChanged()
    }

    /** Marca un día como normal, festivo o libre. */
    suspend fun setDayType(day: LocalDate, type: DayType) {
        val info = dao.dayInfo(day.toEpochDay()) ?: DayInfo(day.toEpochDay())
        dao.upsertDay(info.copy(type = type))
        onChanged()
    }

    // ---------- Tareas ----------

    fun observeTasks(from: LocalDate, to: LocalDate): Flow<List<Task>> =
        dao.observeTasks(from.toEpochDay(), to.toEpochDay())

    suspend fun pendingReminders(): List<Task> = dao.pendingReminders()

    suspend fun saveTask(task: Task) {
        dao.upsertTask(task)
        onChanged()
    }

    suspend fun addTasks(tasks: List<Task>) {
        dao.insertTasks(tasks)
        onChanged()
    }

    suspend fun setTaskDone(id: Long, done: Boolean) {
        val t = dao.task(id) ?: return
        dao.upsertTask(t.copy(done = done, doneAt = if (done) System.currentTimeMillis() else null))
        if (done) wallet.grant("task:$id", RewardKind.TASK, t.epochDay, label = t.title) else wallet.revoke("task:$id")
        checkDayComplete(t.epochDay)
        onChanged()
    }

    suspend fun deleteTask(id: Long) {
        val t = dao.task(id)
        dao.deleteTask(id)
        wallet.revoke("task:$id")
        t?.let { checkDayComplete(it.epochDay) }
        onChanged()
    }

    // ---------- Gimnasio ----------

    fun observeGymSessions(): Flow<List<GymSession>> = dao.observeGymSessions()
    fun observeGymSets(): Flow<List<GymSet>> = dao.observeGymSets()

    /** Guarda la sesión con sus series (sustituye las anteriores). Devuelve el id. */
    suspend fun saveGymSession(session: GymSession, sets: List<GymSet>): Long {
        val id = db.withTransaction {
            val newId = dao.upsertGymSession(session).let { if (session.id != 0L) session.id else it }
            dao.deleteGymSetsOf(newId)
            dao.insertGymSets(sets.mapIndexed { i, s -> s.copy(id = 0, sessionId = newId, position = i) })
            newId
        }
        if (session.id == 0L) wallet.grant("gym:$id", RewardKind.GYM_SESSION, session.epochDay, label = "Sesión de gimnasio")
        onChanged()
        return id
    }

    suspend fun deleteGymSession(id: Long) {
        db.withTransaction {
            dao.deleteGymSetsOf(id)
            dao.deleteGymSession(id)
        }
        wallet.revoke("gym:$id")
        onChanged()
    }

    // ---------- Plantilla ----------

    fun observeBlocks(): Flow<List<Block>> = dao.observeBlocks()

    suspend fun allBlocks(): List<Block> = dao.allBlocks()

    private suspend fun editTemplate(edit: suspend () -> Unit) {
        // Primero se congela lo pasado con la plantilla antigua.
        ensureMaterialized()
        db.withTransaction {
            edit()
            resyncToday()
        }
        onChanged()
    }

    /** Hoy: los bloques aún sin tocar siguen a la plantilla; lo ya marcado se conserva. */
    private suspend fun resyncToday() {
        val today = LocalDate.now()
        val day = today.toEpochDay()
        dao.deleteUntouched(day)
        val kept = dao.entriesBetween(day, day).mapNotNull { it.blockId }.toSet()
        dao.insertEntries(
            dao.blocksFor(today.dayOfWeek.value).filter { it.id !in kept }.map { it.toEntry(day) }
        )
    }

    suspend fun saveBlock(block: Block) = editTemplate {
        if (block.id == 0L) dao.insertBlock(block) else dao.updateBlock(block)
    }

    /** Guarda un bloque en varios días: actualiza el original si su día sigue marcado. */
    suspend fun saveBlockForDays(block: Block, days: Set<Int>) = editTemplate {
        if (block.id != 0L) {
            if (block.dayOfWeek in days) dao.updateBlock(block) else dao.deleteBlock(block.id)
        }
        val newDays = if (block.id != 0L) days - block.dayOfWeek else days
        dao.insertBlocks(newDays.sorted().map { block.copy(id = 0, dayOfWeek = it) })
    }

    suspend fun deleteBlock(id: Long) = editTemplate { dao.deleteBlock(id) }

    suspend fun duplicateBlock(id: Long) = editTemplate {
        dao.block(id)?.let { dao.insertBlock(it.copy(id = 0, title = it.title + " (copia)")) }
    }

    /** Copia todos los bloques de [from] en [to], sustituyendo los que hubiera. */
    suspend fun copyDay(from: Int, to: Int) = editTemplate {
        if (from == to) return@editTemplate
        val source = dao.blocksFor(from)
        dao.deleteBlocksOfDay(to)
        dao.insertBlocks(source.map { it.copy(id = 0, dayOfWeek = to) })
    }

    /** Sustituye la plantilla; si [onlyDays] no es null, solo esos días. */
    suspend fun replaceTemplate(blocks: List<Block>, onlyDays: Set<Int>? = null) = editTemplate {
        if (onlyDays == null) dao.deleteAllBlocks() else onlyDays.forEach { dao.deleteBlocksOfDay(it) }
        dao.insertBlocks(blocks.filter { onlyDays == null || it.dayOfWeek in onlyDays }.map { it.copy(id = 0) })
    }

    /** Añade bloques a la plantilla sin borrar los existentes. */
    suspend fun mergeTemplate(blocks: List<Block>) = editTemplate {
        dao.insertBlocks(blocks.map { it.copy(id = 0) })
    }

    // ---------- Copia de seguridad ----------

    suspend fun exportBackup(settings: BackupSettings): Backup {
        ensureMaterialized()
        return Backup(
            exportedAt = System.currentTimeMillis(),
            blocks = dao.allBlocks(),
            entries = dao.allEntries(),
            days = dao.allDays(),
            tasks = dao.allTasks(),
            gymSessions = dao.allGymSessions(),
            gymSets = dao.allGymSets(),
            rewards = dao.allRewards(),
            settings = settings,
        )
    }

    /** Sustituye todos los datos por los de la copia. */
    suspend fun restoreBackup(backup: Backup) {
        mutex.withLock {
            db.withTransaction {
                dao.deleteAllBlocks()
                dao.deleteAllEntries()
                dao.deleteAllDays()
                dao.deleteAllTasks()
                dao.deleteAllGymSets()
                dao.deleteAllGymSessions()
                dao.deleteAllRewards()
                dao.insertBlocks(backup.blocks)
                dao.insertTasks(backup.tasks)
                backup.gymSessions.forEach { dao.upsertGymSession(it) }
                dao.insertGymSets(backup.gymSets)
                dao.insertRewards(backup.rewards)
                dao.insertEntries(backup.entries)
                dao.upsertDays(backup.days)
            }
        }
        ensureMaterialized()
        onChanged()
    }

    companion object {
        private const val MAX_BACKFILL_DAYS = 60L
    }
}
