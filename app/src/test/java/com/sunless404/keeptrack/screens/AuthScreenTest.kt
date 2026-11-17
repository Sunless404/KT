package com.sunless404.keeptrack.screens

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.sunless404.keeptrack.ui.account.AuthScreen
import com.sunless404.keeptrack.ui.theme.KeepTrackTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** El botón "Entrar como invitado" tiene que responder al toque. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h860dp-night-xxhdpi")
class AuthScreenTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun guestButtonWorks() {
        var guest = false
        rule.setContent { KeepTrackTheme { AuthScreen(onGuest = { guest = true }) } }
        rule.mainClock.advanceTimeBy(1500)
        rule.onNodeWithText("Entrar como invitado").performClick()
        rule.waitForIdle()
        assertTrue("El botón de invitado no recibió el toque", guest)
    }
}
