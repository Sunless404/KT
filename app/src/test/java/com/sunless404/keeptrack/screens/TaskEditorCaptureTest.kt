package com.sunless404.keeptrack.screens

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureScreenRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Captura del formulario de compromisos. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h860dp-night-xxhdpi")
class TaskEditorCaptureTest {
    @get:Rule
    val rule = createComposeRule()

    private fun settle() = repeat(10) { rule.mainClock.advanceTimeBy(100); rule.waitForIdle() }

    @Test
    fun formulario() {
        val day = java.time.LocalDate.now().toEpochDay()
        rule.setContent {
            com.sunless404.keeptrack.ui.theme.KeepTrackTheme {
                com.sunless404.keeptrack.ui.tasks.TaskEditorDialog(
                    com.sunless404.keeptrack.data.Task(epochDay = day, title = "Dentista", timeMin = 17 * 60, description = "Llevar la tarjeta sanitaria", priority = 1, notify = true,
                        remindAt = com.sunless404.keeptrack.ui.tasks.remindAtMillis(java.time.LocalDate.ofEpochDay(day), 16 * 60 + 30)),
                    onDismiss = {}, onDelete = {},
                ) {}
            }
        }
        settle()
        captureScreenRoboImage("build/capturas/formulario.png")
        rule.onAllNodesWithText("Inicio")[0].performClick()
        settle()
        captureScreenRoboImage("build/capturas/formulario-rueda.png")
    }

    @Test
    fun borrarConReajuste() {
        fun t(s: String) = s.substringBefore(':').toInt() * 60 + s.substringAfter(':').toInt()
        var id = 1L
        fun b(title: String, cat: com.sunless404.keeptrack.data.Category, from: String, to: String) =
            com.sunless404.keeptrack.data.Block(id = id++, title = title, category = cat, dayOfWeek = 1, startMin = t(from), endMin = t(to))
        val day = listOf(
            b("Clases", com.sunless404.keeptrack.data.Category.CLASE, "08:30", "14:30"),
            b("Comida", com.sunless404.keeptrack.data.Category.COMIDA, "15:00", "16:00"),
            b("Estudio", com.sunless404.keeptrack.data.Category.ESTUDIO, "16:00", "18:00"),
            b("Gimnasio", com.sunless404.keeptrack.data.Category.GIMNASIO, "18:00", "19:00"),
            b("Lectura", com.sunless404.keeptrack.data.Category.LECTURA, "19:00", "20:00"),
            b("Cena", com.sunless404.keeptrack.data.Category.COMIDA, "21:00", "21:45"),
            b("Dormir", com.sunless404.keeptrack.data.Category.SUENO, "23:00", "07:00"),
        )
        rule.setContent {
            com.sunless404.keeptrack.ui.theme.KeepTrackTheme {
                com.sunless404.keeptrack.ui.edit.DeleteReflowDialog(day[3], day, onDismiss = {}) {}
            }
        }
        settle()
        captureScreenRoboImage("build/capturas/borrar-reajuste.png")
    }
}
