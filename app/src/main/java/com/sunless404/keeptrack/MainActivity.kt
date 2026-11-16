package com.sunless404.keeptrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sunless404.keeptrack.ui.assistant.AssistantScreen
import com.sunless404.keeptrack.ui.common.KtIcons
import com.sunless404.keeptrack.ui.edit.RoutineScreen
import com.sunless404.keeptrack.ui.gym.GymScreen
import com.sunless404.keeptrack.ui.settings.AiSection
import com.sunless404.keeptrack.ui.settings.BackupSection
import com.sunless404.keeptrack.ui.settings.SettingsScreen
import com.sunless404.keeptrack.ui.stats.StatsScreen
import com.sunless404.keeptrack.ui.tasks.TasksScreen
import com.sunless404.keeptrack.ui.theme.KeepTrackTheme
import com.sunless404.keeptrack.ui.today.TodayScreen
import com.sunless404.keeptrack.ui.today.TodayViewModel

enum class Screen(val label: String, val icon: ImageVector?) {
    HOY("Hoy", KtIcons.Today),
    TAREAS("Tareas", KtIcons.Tasks),
    ESTADISTICAS("Estadísticas", KtIcons.Stats),
    RUTINA("Rutina", KtIcons.Routine),
    ASISTENTE("Asistente", KtIcons.Assistant),
    AJUSTES("Ajustes", null),
    GIMNASIO("Gimnasio", null),
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { KeepTrackTheme { KeepTrackApp() } }
    }
}

@Composable
fun KeepTrackApp() {
    var screen by rememberSaveable { mutableStateOf(Screen.HOY) }
    var previous by rememberSaveable { mutableStateOf(Screen.HOY) }
    fun go(s: Screen) {
        previous = screen
        screen = s
    }
    val todayVm: TodayViewModel = viewModel()

    BackHandler(enabled = screen != Screen.HOY) {
        screen = when (screen) {
            Screen.GIMNASIO -> Screen.ESTADISTICAS
            Screen.AJUSTES -> previous.takeIf { it != Screen.AJUSTES } ?: Screen.HOY
            else -> Screen.HOY
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                Screen.entries.filter { it.icon != null }.forEach { s ->
                    NavigationBarItem(
                        selected = screen == s || (s == Screen.ESTADISTICAS && screen == Screen.GIMNASIO),
                        onClick = { go(s) },
                        icon = { Icon(s.icon!!, null) },
                        label = { Text(s.label, maxLines = 1, softWrap = false, overflow = TextOverflow.Visible, style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp)) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
            AnimatedContent(screen, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "pantalla") { s ->
                when (s) {
                    Screen.HOY -> TodayScreen(todayVm, onOpenSettings = { go(Screen.AJUSTES) })
                    Screen.TAREAS -> TasksScreen(viewModel())
                    Screen.ESTADISTICAS -> StatsScreen(viewModel(), onOpenGym = { go(Screen.GIMNASIO) })
                    Screen.RUTINA -> RoutineScreen(viewModel())
                    Screen.ASISTENTE -> AssistantScreen(viewModel(), onOpenSettings = { go(Screen.AJUSTES) })
                    Screen.GIMNASIO -> GymScreen(viewModel(), onBack = { screen = Screen.ESTADISTICAS })
                    Screen.AJUSTES -> SettingsScreen(onBack = { screen = previous.takeIf { it != Screen.AJUSTES } ?: Screen.HOY }) {
                        AiSection()
                        HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        BackupSection()
                    }
                }
            }
        }
    }
}
