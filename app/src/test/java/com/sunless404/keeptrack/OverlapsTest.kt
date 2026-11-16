package com.sunless404.keeptrack

import com.sunless404.keeptrack.data.Block
import com.sunless404.keeptrack.data.Category
import com.sunless404.keeptrack.domain.overlappingPairs
import com.sunless404.keeptrack.domain.overlaps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OverlapsTest {
    private fun b(day: Int, start: String, end: String) = Block(
        title = "x", category = Category.OTRO, dayOfWeek = day,
        startMin = start.substringBefore(':').toInt() * 60 + start.substringAfter(':').toInt(),
        endMin = end.substringBefore(':').toInt() * 60 + end.substringAfter(':').toInt(),
    )

    @Test
    fun contiguousBlocksDoNotOverlap() {
        assertFalse(overlaps(b(1, "08:00", "09:00"), b(1, "09:00", "10:00")))
    }

    @Test
    fun sameDayOverlap() {
        assertTrue(overlaps(b(1, "08:00", "09:30"), b(1, "09:00", "10:00")))
    }

    @Test
    fun differentDaysDoNotOverlap() {
        assertFalse(overlaps(b(1, "08:00", "09:00"), b(2, "08:00", "09:00")))
    }

    @Test
    fun sleepCrossingMidnightOverlapsNextMorning() {
        assertTrue(overlaps(b(1, "23:00", "07:00"), b(2, "06:30", "07:30")))
        assertFalse(overlaps(b(1, "23:00", "07:00"), b(2, "07:00", "07:30")))
    }

    @Test
    fun sundayNightWrapsToMonday() {
        assertTrue(overlaps(b(7, "23:00", "07:00"), b(1, "06:00", "06:30")))
        assertTrue(overlaps(b(1, "06:00", "06:30"), b(7, "23:00", "07:00")))
    }

    @Test
    fun durationOfCrossingBlock() {
        assertEquals(480, b(1, "23:00", "07:00").durationMin)
    }

    @Test
    fun pairsAreReportedOnce() {
        val list = listOf(b(1, "08:00", "10:00"), b(1, "09:00", "11:00"), b(1, "12:00", "13:00"))
        assertEquals(listOf(0 to 1), overlappingPairs(list))
    }
}
