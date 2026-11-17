package com.sunless404.keeptrack.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.sunless404.keeptrack.data.SampleRoutine
import com.sunless404.keeptrack.domain.RoutineParser
import com.sunless404.keeptrack.ui.edit.AiStep
import com.sunless404.keeptrack.ui.edit.ImportMenu
import com.sunless404.keeptrack.ui.edit.PreviewStep
import com.sunless404.keeptrack.ui.edit.TemplatesStep
import com.sunless404.keeptrack.ui.edit.TextStep
import com.sunless404.keeptrack.ui.theme.KeepTrackTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Capturas de las pantallas de importar rutina. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h860dp-night-xxhdpi")
class ImportScreensTest {
    @get:Rule
    val rule = createComposeRule()

    private val sample = """
        Estudio de lunes a viernes de 8 a 10
        Gimnasio martes y jueves a las 18:00 durante 1h30
        Leer todos los días 30 min a las 23:00
        Piano los miércoles por la tarde
    """.trimIndent()

    @Test
    fun pantallas() {
        val screens: List<Pair<String, @Composable () -> Unit>> = listOf(
            "menu" to { ImportMenu(hasRoutine = true) {} },
            "texto" to { TextStep(onClose = {}) {} },
            "ia" to { AiStep(SampleRoutine.blocks(), onClose = {}) {} },
            "plantillas" to { TemplatesStep(onClose = {}) {} },
            "vista-previa" to {
                PreviewStep(RoutineParser.parse(sample), "Texto", SampleRoutine.blocks(), true, {}, {}) { _, _ -> }
            },
        )
        var idx by mutableIntStateOf(0)
        rule.setContent {
            KeepTrackTheme {
                androidx.compose.material3.Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { screens[idx].second() }
            }
        }
        screens.forEachIndexed { i, (name, _) ->
            idx = i
            rule.waitForIdle()
            rule.onRoot().captureRoboImage("build/capturas/importar-$name.png")
        }
    }
}
