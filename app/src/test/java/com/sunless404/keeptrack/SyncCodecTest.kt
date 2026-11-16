package com.sunless404.keeptrack

import com.sunless404.keeptrack.cloud.SyncCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SyncCodecTest {
    @Test
    fun roundTripSmallAndEmpty() {
        assertEquals("{\"a\":1}", SyncCodec.decode(SyncCodec.encode("{\"a\":1}")))
        assertEquals("", SyncCodec.decode(SyncCodec.encode("")))
    }

    @Test
    fun largeDataIsSplitAndRebuilt() {
        // Datos poco comprimibles para forzar varias partes.
        val rnd = Random(1)
        val big = (1..1_500_000).map { ('a'..'z').random(rnd) }.joinToString("")
        val parts = SyncCodec.encode(big)
        assertTrue(parts.size > 1)
        assertTrue(parts.all { it.length <= SyncCodec.CHUNK_CHARS })
        assertEquals(big, SyncCodec.decode(parts))
    }

    @Test
    fun usernames() {
        assertEquals("sunless404", SyncCodec.normalizeUsername("  Sunless404 "))
        assertEquals("a.b_c", SyncCodec.normalizeUsername("a.b_c"))
        assertNull(SyncCodec.normalizeUsername("ab"))
        assertNull(SyncCodec.normalizeUsername("con espacio"))
        assertNull(SyncCodec.normalizeUsername("ñandú"))
        assertNull(SyncCodec.normalizeUsername("x".repeat(21)))
    }
}
