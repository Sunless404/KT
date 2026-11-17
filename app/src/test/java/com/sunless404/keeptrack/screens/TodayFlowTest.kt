package com.sunless404.keeptrack.screens

import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.sunless404.keeptrack.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Capturas de Hoy (línea de tiempo y hechas), la Agenda y el formulario de compromisos. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h860dp-night-xxhdpi")
class TodayFlowTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private fun settle() = repeat(15) { rule.mainClock.advanceTimeBy(100); Thread.sleep(60); rule.waitForIdle() }

    @Test
    fun hoyYAgenda() {
        settle()
        rule.onAllNodesWithText("Entrar como invitado").fetchSemanticsNodes().firstOrNull()?.let {
            rule.onAllNodesWithText("Entrar como invitado")[0].performClick()
        }
        settle()
        rule.onRoot().captureRoboImage("build/capturas/hoy-1.png")
        // Marcar dos cosas como hechas.
        // Marca la primera casilla visible en pantalla, dos veces.
        fun clickVisibleToggle() {
            val nodes = rule.onAllNodes(isToggleable()).fetchSemanticsNodes()
            val i = nodes.indexOfFirst { it.boundsInRoot.top > 400f && it.boundsInRoot.bottom < 2300f && it.boundsInRoot.left >= 0f && it.boundsInRoot.right <= 1200f }
            if (i >= 0) rule.onAllNodes(isToggleable())[i].performClick()
        }
        clickVisibleToggle(); settle()
        clickVisibleToggle(); settle()
        rule.onRoot().captureRoboImage("build/capturas/hoy-2-marcadas.png")
        rule.onAllNodesWithText("HECHAS", substring = true).fetchSemanticsNodes().firstOrNull()?.let {
            rule.onAllNodesWithText("HECHAS", substring = true)[0].performClick(); settle()
            rule.onRoot().captureRoboImage("build/capturas/hoy-3-hechas.png")
        }
        rule.onAllNodesWithText("Agenda")[0].performClick(); settle()
        rule.onRoot().captureRoboImage("build/capturas/agenda.png")
    }
}
