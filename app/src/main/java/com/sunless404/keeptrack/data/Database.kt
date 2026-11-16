package com.sunless404.keeptrack.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {
    // Plantilla
    @Query("SELECT * FROM blocks ORDER BY dayOfWeek, startMin")
    fun observeBlocks(): Flow<List<Block>>

    @Query("SELECT * FROM blocks ORDER BY dayOfWeek, startMin")
    suspend fun allBlocks(): List<Block>

    @Query("SELECT * FROM blocks WHERE dayOfWeek = :dow ORDER BY startMin")
    suspend fun blocksFor(dow: Int): List<Block>

    @Query("SELECT * FROM blocks WHERE id = :id")
    suspend fun block(id: Long): Block?

    @Insert
    suspend fun insertBlock(block: Block): Long

    @Insert
    suspend fun insertBlocks(blocks: List<Block>)

    @Update
    suspend fun updateBlock(block: Block)

    @Query("DELETE FROM blocks WHERE id = :id")
    suspend fun deleteBlock(id: Long)

    @Query("DELETE FROM blocks WHERE dayOfWeek = :dow")
    suspend fun deleteBlocksOfDay(dow: Int)

    @Query("DELETE FROM blocks")
    suspend fun deleteAllBlocks()

    // Registro diario
    @Query("SELECT * FROM day_entries WHERE epochDay BETWEEN :from AND :to ORDER BY epochDay, startMin")
    fun observeEntriesBetween(from: Long, to: Long): Flow<List<DayEntry>>

    @Query("SELECT * FROM day_entries WHERE epochDay BETWEEN :from AND :to ORDER BY epochDay, startMin")
    suspend fun entriesBetween(from: Long, to: Long): List<DayEntry>

    @Query("SELECT * FROM day_entries ORDER BY epochDay, startMin")
    suspend fun allEntries(): List<DayEntry>

    @Query("SELECT * FROM day_entries WHERE id = :id")
    suspend fun entry(id: Long): DayEntry?

    @Insert
    suspend fun insertEntries(entries: List<DayEntry>)

    @Insert
    suspend fun insertEntry(entry: DayEntry): Long

    @Update
    suspend fun updateEntry(entry: DayEntry)

    @Update
    suspend fun updateEntries(entries: List<DayEntry>)

    @Query("DELETE FROM day_entries WHERE epochDay = :day AND status = 'PENDIENTE' AND modified = 0 AND blockId IS NOT NULL")
    suspend fun deleteUntouched(day: Long)

    @Query("DELETE FROM day_entries")
    suspend fun deleteAllEntries()

    // Días
    @Query("SELECT MAX(epochDay) FROM days WHERE materialized = 1")
    suspend fun lastMaterialized(): Long?

    @Query("SELECT * FROM days WHERE epochDay = :day")
    suspend fun dayInfo(day: Long): DayInfo?

    @Query("SELECT * FROM days WHERE epochDay BETWEEN :from AND :to")
    fun observeDaysBetween(from: Long, to: Long): Flow<List<DayInfo>>

    @Query("SELECT * FROM days")
    suspend fun allDays(): List<DayInfo>

    @Upsert
    suspend fun upsertDay(day: DayInfo)

    @Upsert
    suspend fun upsertDays(days: List<DayInfo>)

    @Query("DELETE FROM days")
    suspend fun deleteAllDays()
}

@Database(entities = [Block::class, DayEntry::class, DayInfo::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): RoutineDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "keeptrack.db").build()
    }
}
