package com.sunless404.keeptrack.screens

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import com.sunless404.keeptrack.data.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Las migraciones conservan los datos y dejan la base de datos como espera Room. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    @Test
    fun v2toV3KeepsData() {
        helper.createDatabase("prueba.db", 2).use { db ->
            db.execSQL("INSERT INTO tasks (epochDay, timeMin, title, description, notify, remindAt, done, doneAt) VALUES (20000, 600, 'Dentista', '', 1, NULL, 0, NULL)")
            db.execSQL("INSERT INTO blocks (title, category, dayOfWeek, startMin, endMin, notes, notify, leadMin) VALUES ('Estudio', 'ESTUDIO', 1, 600, 660, '', 1, NULL)")
        }
        val db = helper.runMigrationsAndValidate("prueba.db", 3, true, AppDatabase.MIGRATION_2_3)
        db.query("SELECT title FROM tasks").use { c ->
            c.moveToFirst()
            assertEquals("Dentista", c.getString(0))
        }
        db.query("SELECT COUNT(*) FROM blocks").use { c ->
            c.moveToFirst()
            assertEquals(1, c.getInt(0))
        }
        db.execSQL("INSERT INTO rewards (`key`, kind, xp, coins, capped, epochDay, createdAt, label) VALUES ('block:1', 'BLOCK', 5, 2, 1, 20000, 0, 'x')")
    }

    @Test
    fun v3toV4AddsTaskFields() {
        helper.createDatabase("prueba4.db", 3).use { db ->
            db.execSQL("INSERT INTO tasks (epochDay, timeMin, title, description, notify, remindAt, done, doneAt) VALUES (20000, 600, 'Dentista', '', 1, NULL, 0, NULL)")
        }
        val db = helper.runMigrationsAndValidate("prueba4.db", 4, true, AppDatabase.MIGRATION_3_4)
        db.query("SELECT title, endMin, priority, `repeat` FROM tasks").use { c ->
            c.moveToFirst()
            assertEquals("Dentista", c.getString(0))
            assertEquals(true, c.isNull(1))
            assertEquals(0, c.getInt(2))
            assertEquals(0, c.getInt(3))
        }
    }
}
