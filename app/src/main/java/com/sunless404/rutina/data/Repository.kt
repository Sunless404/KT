package com.sunless404.rutina.data

import androidx.room.withTransaction
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

    companion object {
        private const val MAX_BACKFILL_DAYS = 60L
    }
}
