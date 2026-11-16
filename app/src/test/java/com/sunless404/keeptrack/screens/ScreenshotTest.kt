package com.sunless404.keeptrack.screens

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureRoboImage
import com.sunless404.keeptrack.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Capturas de las pantallas principales con la rutina de ejemplo.
 * Solo se ejecuta a mano: ./gradlew testDebugUnitTest -Pcapturas
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h860dp-night-xxhdpi")
class ScreenshotTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private fun settle() {
        repeat(20) {
            Thread.sleep(100)
            rule.waitForIdle()
        }
    }

    private fun shot(name: String) {
        settle()
        rule.onRoot().captureRoboImage("build/capturas/$name.png")
    }

    private fun tab(label: String) {
        rule.onAllNodesWithText(label)[0].performClick()
    }

    @Test
    fun pantallas() {
        shot("1-hoy")
        tab("Tareas"); shot("2-tareas")
        tab("Estadísticas"); shot("3-estadisticas")
        tab("Rutina"); shot("4-rutina")
        tab("Asistente"); shot("5-asistente")
        tab("Estadísticas"); settle()
        rule.onAllNodesWithText("Gimnasio")[0].performClick(); shot("6-gimnasio")
    }
}
