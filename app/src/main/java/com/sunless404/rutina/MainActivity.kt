package com.sunless404.rutina

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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sunless404.rutina.ui.edit.EditScreen
import com.sunless404.rutina.ui.settings.SettingsScreen
import com.sunless404.rutina.ui.theme.RutinaTheme
import com.sunless404.rutina.ui.today.TodayScreen
import com.sunless404.rutina.ui.today.TodayViewModel
import com.sunless404.rutina.ui.week.WeekScreen

enum class Screen(val label: String, val icon: ImageVector?) {
    HOY("Hoy", Icons.Default.Home),
    SEMANA("Semana", Icons.Default.DateRange),
    RUTINA("Rutina", Icons.AutoMirrored.Filled.List),
    AJUSTES("Ajustes", null),
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { RutinaTheme { RutinaApp() } }
    }
}

@Composable
fun RutinaApp() {
    var screen by rememberSaveable { mutableStateOf(Screen.HOY) }
    val todayVm: TodayViewModel = viewModel()

    BackHandler(enabled = screen != Screen.HOY) { screen = Screen.HOY }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar {
                Screen.entries.filter { it.icon != null }.forEach { s ->
                    NavigationBarItem(
                        selected = screen == s,
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
                Screen.RUTINA -> EditScreen(viewModel())
                Screen.AJUSTES -> SettingsScreen(onBack = { screen = Screen.HOY })
            }
        }
    }
}
