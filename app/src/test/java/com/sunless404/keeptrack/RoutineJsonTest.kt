package com.sunless404.keeptrack

import com.sunless404.keeptrack.data.SampleRoutine
import com.sunless404.keeptrack.domain.AiPrompt
import com.sunless404.keeptrack.domain.RoutineJson
import com.sunless404.keeptrack.domain.RoutineParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutineJsonTest {
    private fun key(b: com.sunless404.keeptrack.data.Block) =
        listOf(b.dayOfWeek, b.startMin, b.endMin, b.title, b.category, b.notes, b.notify, b.leadMin)

    @Test
    fun encodedRoutineParsesBackIdentically() {
        val blocks = SampleRoutine.blocks().mapIndexed { i, b ->
            if (i == 0) b.copy(notes = "Con \"comillas\"", leadMin = 5) else b
        }
        val json = RoutineJson.encode(blocks).toString()
        val r = RoutineParser.parse(json)
        assertTrue(r.errors.toString(), r.errors.isEmpty())
        assertEquals(blocks.map(::key).sortedBy { it.toString() }, r.blocks.map(::key).sortedBy { it.toString() })
    }

    @Test
    fun sameBlockOnSeveralDaysIsGrouped() {
        val json = RoutineJson.encode(SampleRoutine.blocks())
        val groups = json["bloques"].toString()
        // "Dormir" 23:00-07:00 de lunes a viernes y domingo va en un solo bloque.
        assertTrue(groups.contains("""["lunes","martes","miercoles","jueves","viernes","domingo"]"""))
    }

    @Test
    fun requestIncludesCurrentRoutineOnlyWhenGiven() {
        assertTrue(AiPrompt.request("hola", "{}").contains("Rutina actual"))
        assertTrue(AiPrompt.request("hola", null).contains("crea una nueva"))
    }
}
