package com.sunless404.keeptrack

import com.sunless404.keeptrack.data.SampleRoutine
import com.sunless404.keeptrack.domain.ExternalAiPrompt
import com.sunless404.keeptrack.domain.RoutineParser
import com.sunless404.keeptrack.domain.RoutineTemplates
import com.sunless404.keeptrack.domain.RoutineText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutineImportTest {
    @Test
    fun templatesParseWithoutErrors() {
        RoutineTemplates.all.forEach { t ->
            val r = RoutineParser.parse(t.text)
            assertTrue("${t.name}: ${r.errors}", r.errors.isEmpty())
            assertTrue(t.name, r.blocks.map { it.dayOfWeek }.toSet() == (1..7).toSet())
        }
    }

    @Test
    fun exportedTextImportsBackTheSame() {
        val original = SampleRoutine.blocks()
        val text = RoutineText.encode(original)
        val back = RoutineParser.parse(text)
        assertTrue(back.errors.toString(), back.errors.isEmpty())
        fun key(b: com.sunless404.keeptrack.data.Block) = listOf(b.dayOfWeek, b.startMin, b.endMin, b.title, b.category)
        assertEquals(original.map(::key).toSet(), back.blocks.map(::key).toSet())
    }

    @Test
    fun daysLabel() {
        assertEquals("Lunes a viernes", RoutineText.daysLabel((1..5).toSet()))
        assertEquals("Sábado y domingo", RoutineText.daysLabel(setOf(6, 7)))
        assertEquals("Lunes, miércoles y viernes", RoutineText.daysLabel(setOf(1, 3, 5)))
    }

    @Test
    fun promptIncludesRequestAndFormat() {
        val p = ExternalAiPrompt.build("Más gimnasio", SampleRoutine.blocks(), keepCurrent = true)
        assertTrue(p.contains("Más gimnasio"))
        assertTrue(p.contains("\"bloques\""))
        assertTrue(p.contains("Dormir"))
    }
}
