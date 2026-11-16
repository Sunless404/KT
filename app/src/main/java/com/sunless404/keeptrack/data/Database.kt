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
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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

    // Tareas
    @Query("SELECT * FROM tasks WHERE epochDay BETWEEN :from AND :to ORDER BY epochDay, timeMin IS NULL, timeMin, id")
    fun observeTasks(from: Long, to: Long): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE epochDay BETWEEN :from AND :to ORDER BY epochDay, timeMin IS NULL, timeMin, id")
    suspend fun tasksBetween(from: Long, to: Long): List<Task>

    @Query("SELECT * FROM tasks WHERE notify = 1 AND done = 0 AND remindAt IS NOT NULL")
    suspend fun pendingReminders(): List<Task>

    @Query("SELECT * FROM tasks")
    suspend fun allTasks(): List<Task>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun task(id: Long): Task?

    @Upsert
    suspend fun upsertTask(task: Task): Long

    @Insert
    suspend fun insertTasks(tasks: List<Task>)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTask(id: Long)

    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()

    // Gimnasio
    @Query("SELECT * FROM gym_sessions ORDER BY epochDay DESC, id DESC")
    fun observeGymSessions(): Flow<List<GymSession>>

    @Query("SELECT * FROM gym_sets ORDER BY sessionId, position, id")
    fun observeGymSets(): Flow<List<GymSet>>

    @Query("SELECT * FROM gym_sessions")
    suspend fun allGymSessions(): List<GymSession>

    @Query("SELECT * FROM gym_sets")
    suspend fun allGymSets(): List<GymSet>

    @Upsert
    suspend fun upsertGymSession(session: GymSession): Long

    @Query("DELETE FROM gym_sessions WHERE id = :id")
    suspend fun deleteGymSession(id: Long)

    @Query("DELETE FROM gym_sets WHERE sessionId = :sessionId")
    suspend fun deleteGymSetsOf(sessionId: Long)

    @Insert
    suspend fun insertGymSets(sets: List<GymSet>)

    @Query("DELETE FROM gym_sessions")
    suspend fun deleteAllGymSessions()

    @Query("DELETE FROM gym_sets")
    suspend fun deleteAllGymSets()
}

@Database(
    entities = [Block::class, DayEntry::class, DayInfo::class, Task::class, GymSession::class, GymSet::class],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): RoutineDao

    companion object {
        /** v1 → v2: tareas y gimnasio. Las sentencias salen del esquema exportado en app/schemas. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `tasks` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `epochDay` INTEGER NOT NULL, `timeMin` INTEGER, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `notify` INTEGER NOT NULL, `remindAt` INTEGER, `done` INTEGER NOT NULL, `doneAt` INTEGER)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_epochDay` ON `tasks` (`epochDay`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `gym_sessions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `epochDay` INTEGER NOT NULL, `notes` TEXT NOT NULL, `bodyWeightKg` REAL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_gym_sessions_epochDay` ON `gym_sessions` (`epochDay`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `gym_sets` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `sessionId` INTEGER NOT NULL, `exercise` TEXT NOT NULL, `weightKg` REAL NOT NULL, `reps` INTEGER NOT NULL, `position` INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_gym_sets_sessionId` ON `gym_sets` (`sessionId`)")
            }
        }

        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "keeptrack.db")
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
