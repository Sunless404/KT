package com.sunless404.keeptrack

import com.sunless404.keeptrack.data.Backup
import com.sunless404.keeptrack.data.BackupSettings
import com.sunless404.keeptrack.data.Block
import com.sunless404.keeptrack.data.Category
import com.sunless404.keeptrack.data.DayEntry
import com.sunless404.keeptrack.data.DayInfo
import com.sunless404.keeptrack.data.DayType
import com.sunless404.keeptrack.data.Status
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupTest {
    @Test
    fun roundTrip() {
        val b = Backup(
            exportedAt = 123,
            blocks = listOf(Block(id = 7, title = "Dormir", category = Category.SUENO, dayOfWeek = 1, startMin = 1380, endMin = 420)),
            entries = listOf(
                DayEntry(
                    id = 3, epochDay = 20000, blockId = 7, title = "Dormir", category = Category.SUENO,
                    startMin = 1380, endMin = 1860, status = Status.HECHO, markedAt = 99, modified = true,
                )
            ),
            days = listOf(DayInfo(20000, DayType.FESTIVO, materialized = true)),
            settings = BackupSettings(defaultLeadMin = 10, sound = false),
        )
        assertEquals(b, Backup.fromJson(b.toJson()))
    }

    @Test
    fun acceptsBackupsFromBeforeTheRename() {
        assertEquals(0, Backup.fromJson("""{"app":"rutina","version":1}""").blocks.size)
    }

    @Test
    fun rejectsForeignOrBrokenFiles() {
        assertThrows(Exception::class.java) { Backup.fromJson("""{"app":"otra"}""") }
        assertThrows(Exception::class.java) { Backup.fromJson("no es json") }
        assertThrows(Exception::class.java) { Backup.fromJson("""{"app":"keeptrack","version":99}""") }
    }
}
