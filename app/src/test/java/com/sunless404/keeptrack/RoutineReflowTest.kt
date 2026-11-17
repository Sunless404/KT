package com.sunless404.keeptrack

import com.sunless404.keeptrack.data.Block
import com.sunless404.keeptrack.data.Category
import com.sunless404.keeptrack.domain.ReflowMode
import com.sunless404.keeptrack.domain.RoutineReflow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutineReflowTest {
    private var nextId = 1L
    private fun t(s: String) = s.substringBefore(':').toInt() * 60 + s.substringAfter(':').toInt()
    private fun b(title: String, cat: Category, from: String, to: String) =
        Block(id = nextId++, title = title, category = cat, dayOfWeek = 1, startMin = t(from), endMin = t(to))
    private fun List<Block>.at(title: String) = first { it.title == title }.let { "%02d:%02d-%02d:%02d".format(it.startMin / 60, it.startMin % 60, it.endMin / 60, it.endMin % 60) }

    private val day = listOf(
        b("Clases", Category.CLASE, "08:30", "14:30"),
        b("Comida", Category.COMIDA, "15:00", "16:00"),
        b("Estudio", Category.ESTUDIO, "16:00", "18:00"),
        b("Gimnasio", Category.GIMNASIO, "18:00", "19:00"),
        b("Lectura", Category.LECTURA, "19:00", "20:00"),
        b("Cena", Category.COMIDA, "21:00", "21:45"),
        b("Leer más", Category.LECTURA, "22:00", "23:00"),
        b("Dormir", Category.SUENO, "23:00", "07:00"),
    )

    @Test
    fun spreadGivesFreedTimeToFlexibleBlocksOfThatStretch() {
        val r = RoutineReflow.remove(day, day.first { it.title == "Gimnasio" }, ReflowMode.SPREAD, morning = t("07:00"))
        assertEquals("16:00-18:40", r.blocks.at("Estudio"))
        assertEquals("18:40-20:00", r.blocks.at("Lectura"))
        assertEquals("21:00-21:45", r.blocks.at("Cena"))
        assertEquals("23:00-07:00", r.blocks.at("Dormir"))
        assertEquals(2, r.changed.size)
    }

    @Test
    fun pullForwardMovesTheRestEarlier() {
        val r = RoutineReflow.remove(day, day.first { it.title == "Gimnasio" }, ReflowMode.PULL_FORWARD)
        assertEquals("16:00-18:00", r.blocks.at("Estudio"))
        assertEquals("18:00-19:00", r.blocks.at("Lectura"))
    }

    @Test
    fun pullForwardBeforeSleepMeansGoingToBedEarlier() {
        val r = RoutineReflow.remove(day, day.first { it.title == "Leer más" }, ReflowMode.PULL_FORWARD)
        assertEquals("22:00-07:00", r.blocks.at("Dormir"))
    }

    @Test
    fun keepGapChangesNothingElse() {
        val r = RoutineReflow.remove(day, day.first { it.title == "Gimnasio" }, ReflowMode.KEEP_GAP)
        assertTrue(r.changed.isEmpty())
        assertEquals(day.size - 1, r.blocks.size)
    }

    @Test
    fun addingShrinksFlexibleNeighboursButNotFixedOnes() {
        val piano = Block(title = "Piano", category = Category.ACADEMIA, dayOfWeek = 1, startMin = t("17:00"), endMin = t("18:00"), fixed = false)
        val r = RoutineReflow.place(day, piano, morning = t("07:00"))
        assertTrue(r.reason, r.ok)
        assertEquals("16:00-17:00", r.blocks.at("Estudio"))
        assertEquals("17:00-18:00", r.blocks.at("Piano"))
        assertEquals("18:00-19:00", r.blocks.at("Gimnasio"))
        assertEquals("15:00-16:00", r.blocks.at("Comida"))
    }

    @Test
    fun neverCutsSleepBelowEightHours() {
        val serie = Block(title = "Serie", category = Category.LIBRE, dayOfWeek = 1, startMin = t("22:00"), endMin = t("23:30"))
        val r = RoutineReflow.place(day, serie)
        assertFalse(r.ok)
        // "Leer más" baja a su mínimo (30 min) y aun así faltan 60 min sin tocar el sueño.
        assertEquals(60, r.missingMin)
    }

    @Test
    fun cannotOverlapFixedBlocks() {
        val x = Block(title = "Médico", category = Category.OTRO, dayOfWeek = 1, startMin = t("14:00"), endMin = t("15:00"))
        val r = RoutineReflow.place(day, x)
        assertFalse(r.ok)
        assertTrue(r.reason!!.contains("Clases"))
    }

    @Test
    fun lengtheningAnExistingBlockPushesTheNextOnes() {
        val longer = day.first { it.title == "Estudio" }.copy(endMin = t("18:30"))
        val r = RoutineReflow.place(day, longer)
        assertTrue(r.reason, r.ok)
        assertEquals("16:00-18:30", r.blocks.at("Estudio"))
        // Usa el hueco libre de 20:00 a 21:00: lo de después se corre media hora.
        assertEquals("18:30-19:30", r.blocks.at("Gimnasio"))
        assertEquals("19:30-20:30", r.blocks.at("Lectura"))
    }
}
