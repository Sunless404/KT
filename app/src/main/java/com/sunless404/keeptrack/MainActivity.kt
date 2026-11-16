package com.sunless404.keeptrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sunless404.keeptrack.ui.edit.EditScreen
import com.sunless404.keeptrack.ui.importer.ImportScreen
import com.sunless404.keeptrack.ui.settings.AiSection
import com.sunless404.keeptrack.ui.settings.BackupSection
import com.sunless404.keeptrack.ui.settings.SettingsScreen
import com.sunless404.keeptrack.ui.stats.StatsScreen
import com.sunless404.keeptrack.ui.theme.KeepTrackTheme
import com.sunless404.keeptrack.ui.today.TodayScreen
import com.sunless404.keeptrack.ui.today.TodayViewModel
import com.sunless404.keeptrack.ui.week.WeekScreen

enum class Screen(val label: String, val icon: ImageVector?) {
    HOY("Hoy", Icons.Default.Home),
    SEMANA("Semana", Icons.Default.DateRange),
    ESTADISTICAS("Estadísticas", Icons.Default.Star),
    RUTINA("Rutina", Icons.AutoMirrored.Filled.List),
    AJUSTES("Ajustes", null),
    IMPORTAR("Importar", null),
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
    val todayVm: TodayViewModel = viewModel()

    BackHandler(enabled = screen != Screen.HOY) {
        screen = if (screen == Screen.IMPORTAR) Screen.RUTINA else Screen.HOY
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar {
                Screen.entries.filter { it.icon != null }.forEach { s ->
                    NavigationBarItem(
                        selected = screen == s || (s == Screen.RUTINA && screen == Screen.IMPORTAR),
                        onClick = { screen = s },
                        icon = { Icon(s.icon!!, null) },
                        label = { Text(s.label) },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
            when (screen) {
                Screen.HOY -> TodayScreen(todayVm) {
                    IconButton(onClick = { screen = Screen.AJUSTES }) { Icon(Icons.Default.Settings, "Ajustes") }
                }
                Screen.SEMANA -> WeekScreen(viewModel()) { date ->
                    todayVm.setDate(date)
                    screen = Screen.HOY
                }
                Screen.ESTADISTICAS -> StatsScreen(viewModel())
                Screen.RUTINA -> EditScreen(viewModel(), onImport = { screen = Screen.IMPORTAR })
                Screen.IMPORTAR -> ImportScreen(viewModel(), onBack = { screen = Screen.RUTINA }, onOpenSettings = { screen = Screen.AJUSTES })
                Screen.AJUSTES -> SettingsScreen(onBack = { screen = Screen.HOY }) {
                    AiSection()
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    BackupSection()
                }
            }
        }
    }
}
