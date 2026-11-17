package com.sunless404.keeptrack.screens

import androidx.compose.ui.test.junit4.createComposeRule
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
    }
}
