package com.sunless404.keeptrack.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.sunless404.keeptrack.data.SampleRoutine
import com.sunless404.keeptrack.domain.AssistantLogic
import com.sunless404.keeptrack.domain.ChatMessage
import com.sunless404.keeptrack.domain.RoutineJson
import com.sunless404.keeptrack.ui.assistant.AssistantBubble
import com.sunless404.keeptrack.ui.assistant.Typing
import com.sunless404.keeptrack.ui.assistant.UserBubble
import com.sunless404.keeptrack.ui.assistant.Welcome
import com.sunless404.keeptrack.ui.theme.KeepTrackTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Capturas del chat del asistente con mensajes de ejemplo (sin llamar a la IA). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h860dp-night-xxhdpi")
class ChatScreenshotTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun bienvenida() {
        rule.setContent {
            KeepTrackTheme { Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp)) { Welcome {} } }
        }
        rule.mainClock.advanceTimeBy(2000)
        rule.onRoot().captureRoboImage("build/capturas/8-asistente-inicio.png")
    }

    @Test
    fun conversacion() {
        val current = SampleRoutine.blocks()
        val changed = current.map { if (it.title == "Gimnasio") it.copy(dayOfWeek = if (it.dayOfWeek == 1) 2 else 4) else it }
        val reply = AssistantLogic.parseReply(
            """{"mensaje":"Hecho: he pasado el gimnasio a martes y jueves de 19:00 a 20:30 y he movido el estudio para que no se pisen. También te apunto el dentista.","rutina":${RoutineJson.encode(changed)},"tareas":[{"titulo":"Dentista","fecha":"2026-11-20","hora":"17:00","descripcion":"Revisión anual","recordatorio":"16:30"}]}""",
            current,
        )
        rule.setContent {
            KeepTrackTheme {
                Column(
                    Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    UserBubble(ChatMessage(fromUser = true, text = "Pásame el gimnasio a martes y jueves por la tarde y apúntame el dentista el viernes a las 17:00"))
                    AssistantBubble(ChatMessage(fromUser = false, text = reply.message, routine = reply.routine, tasks = reply.tasks), {}, {})
                    Typing()
                }
            }
        }
        rule.mainClock.advanceTimeBy(1500)
        rule.onRoot().captureRoboImage("build/capturas/9-asistente-chat.png")
    }
}
