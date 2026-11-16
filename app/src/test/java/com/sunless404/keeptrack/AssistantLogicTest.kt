package com.sunless404.keeptrack

import com.sunless404.keeptrack.data.SampleRoutine
import com.sunless404.keeptrack.domain.AssistantLogic
import com.sunless404.keeptrack.domain.Attachment
import com.sunless404.keeptrack.domain.ChatMessage
import com.sunless404.keeptrack.domain.RoutineJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class AssistantLogicTest {
    private val current = SampleRoutine.blocks()

    @Test
    fun plainMessageWithoutChanges() {
        val r = AssistantLogic.parseReply("""{"mensaje":"Hola","rutina":null,"tareas":[]}""", current)
        assertEquals("Hola", r.message)
        assertNull(r.routine)
        assertTrue(r.tasks.isEmpty())
    }

    @Test
    fun unchangedRoutineHasNoDiff() {
        val json = """{"mensaje":"Igual","rutina":${RoutineJson.encode(current)},"tareas":[]}"""
        val r = AssistantLogic.parseReply(json, current).routine!!
        assertEquals(0, r.added)
        assertEquals(0, r.removed)
    }

    @Test
    fun routineChangeIsCounted() {
        val changed = current.filterNot { it.title == "Lectura" && it.dayOfWeek == 7 } +
            current.first().copy(title = "Yoga", dayOfWeek = 7, startMin = 600, endMin = 660)
        val r = AssistantLogic.parseReply("""{"mensaje":"Hecho","rutina":${RoutineJson.encode(changed)}}""", current).routine!!
        assertEquals(1, r.added)
        assertEquals(1, r.removed)
    }

    @Test
    fun tasksAreParsedWithReminder() {
        val r = AssistantLogic.parseReply(
            """{"mensaje":"Apuntado","tareas":[
              {"titulo":"Dentista","fecha":"2026-11-20","hora":"17:00","descripcion":"Revisión","recordatorio":"16:30"},
              {"titulo":"Sin fecha"},
              {"titulo":"Comprar pan","fecha":"2026-11-21","hora":null,"recordatorio":null}
            ]}""",
            current, ZoneOffset.UTC,
        )
        assertEquals(2, r.tasks.size)
        val t = r.tasks[0]
        assertEquals(LocalDate.of(2026, 11, 20).toEpochDay(), t.epochDay)
        assertEquals(17 * 60, t.timeMin)
        assertTrue(t.notify)
        assertEquals(LocalDate.of(2026, 11, 20).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli() + 990 * 60_000L, t.remindAt)
        assertFalse(r.tasks[1].notify)
    }

    @Test
    fun nonJsonReplyIsShownAsText() {
        assertEquals("Lo siento", AssistantLogic.parseReply("Lo siento", current).message)
        assertEquals("Hola", AssistantLogic.parseReply("```json\n{\"mensaje\":\"Hola\"}\n```", current).message)
    }

    @Test
    fun onlyLastUserMessageCarriesAttachments() {
        val img = Attachment("image/jpeg", byteArrayOf(1, 2, 3))
        val history = listOf(
            ChatMessage(fromUser = true, text = "Mira", attachments = listOf(img)),
            ChatMessage(fromUser = false, text = "Vale"),
            ChatMessage(fromUser = true, text = "Otra", attachments = listOf(img)),
            ChatMessage(fromUser = false, text = "Error", error = true),
        )
        val c = AssistantLogic.contents(history).toString()
        assertEquals(1, Regex("inlineData").findAll(c).count())
        assertTrue(c.contains("adjuntó 1 imagen"))
        assertFalse(c.contains("Error"))
    }
}
