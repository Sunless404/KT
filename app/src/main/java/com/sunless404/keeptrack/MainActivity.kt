package com.sunless404.keeptrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.sunless404.keeptrack.ui.account.AccountPanel
import com.sunless404.keeptrack.ui.account.AuthScreen
import com.sunless404.keeptrack.ui.account.ProfileScreen
import com.sunless404.keeptrack.ui.assistant.AssistantScreen
import com.sunless404.keeptrack.ui.common.KtIcons
import com.sunless404.keeptrack.ui.common.RewardOverlay
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
import com.sunless404.keeptrack.update.UpdateDialog

enum class Screen(val label: String, val icon: ImageVector?) {
    HOY("Hoy", KtIcons.Today),
    TAREAS("Tareas", KtIcons.Tasks),
    ESTADISTICAS("Estadísticas", KtIcons.Stats),
    RUTINA("Rutina", KtIcons.Routine),
    ASISTENTE("Asistente", KtIcons.Assistant),
    PERFIL("Perfil", null),
    AJUSTES("Ajustes", null),
    GIMNASIO("Gimnasio", null),
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) com.sunless404.keeptrack.ui.edit.SharedImport.receive(this, intent)
        setContent { KeepTrackTheme { Root() } }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        com.sunless404.keeptrack.ui.edit.SharedImport.receive(this, intent)
    }
}

/** Login o invitado al entrar; después, la app. */
@Composable
fun Root() {
    val app = LocalContext.current.applicationContext as App
    val account by app.cloud.account.collectAsState()
    var guest by rememberSaveable { mutableStateOf(app.prefs.guest || !app.cloud.available) }
    val showAuth = account == null && !guest
    AnimatedContent(showAuth, transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(300)) }, label = "auth") { auth ->
        if (auth) {
            AuthScreen(onGuest = { app.prefs.guest = true; guest = true })
        } else {
            KeepTrackApp(onWantsAccount = { app.prefs.guest = false; guest = false })
        }
    }
    RewardOverlay()
    UpdateDialog()
}

@Composable
fun KeepTrackApp(onWantsAccount: () -> Unit) {
    var screen by rememberSaveable { mutableStateOf(Screen.HOY) }
    var previous by rememberSaveable { mutableStateOf(Screen.HOY) }
    fun go(s: Screen) {
        if (s == screen) return
        previous = screen
        screen = s
    }
    val todayVm: TodayViewModel = viewModel()
    val tabs = Screen.entries.filter { it.icon != null }
    // Algo compartido desde otra app: se abre Rutina para importarlo.
    val shared by com.sunless404.keeptrack.ui.edit.SharedImport.text.collectAsState()
    LaunchedEffect(shared) { if (shared != null) go(Screen.RUTINA) }

    BackHandler(enabled = screen != Screen.HOY) {
        screen = when (screen) {
            Screen.GIMNASIO -> Screen.ESTADISTICAS
            Screen.AJUSTES -> Screen.PERFIL
            Screen.PERFIL -> previous.takeIf { it.icon != null } ?: Screen.HOY
            else -> Screen.HOY
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                tabs.forEach { s ->
                    NavigationBarItem(
                        selected = screen == s || (s == Screen.ESTADISTICAS && screen == Screen.GIMNASIO),
                        onClick = { go(s) },
                        icon = { Icon(s.icon!!, null) },
                        label = {
                            Text(
                                s.label, maxLines = 1, softWrap = false, overflow = TextOverflow.Visible,
                                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.secondaryContainer),
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
            AnimatedContent(
                screen,
                transitionSpec = {
                    val from = tabs.indexOf(initialState)
                    val to = tabs.indexOf(targetState)
                    if (from >= 0 && to >= 0) {
                        // Entre pestañas: desliza hacia el lado de la pestaña elegida.
                        val dir = if (to > from) 1 else -1
                        (slideInHorizontally(tween(320)) { it / 5 * dir } + fadeIn(tween(320))) togetherWith
                            (slideOutHorizontally(tween(260)) { -it / 5 * dir } + fadeOut(tween(200)))
                    } else {
                        (scaleIn(tween(320), initialScale = 0.94f) + fadeIn(tween(320))) togetherWith fadeOut(tween(200))
                    }
                },
                label = "pantalla",
            ) { s ->
                val profile = { go(Screen.PERFIL) }
                when (s) {
                    Screen.HOY -> TodayScreen(todayVm, onOpenProfile = profile)
                    Screen.TAREAS -> TasksScreen(viewModel(), onOpenProfile = profile)
                    Screen.ESTADISTICAS -> StatsScreen(viewModel(), onOpenGym = { go(Screen.GIMNASIO) }, onOpenProfile = profile)
                    Screen.RUTINA -> RoutineScreen(viewModel(), onOpenProfile = profile)
                    Screen.ASISTENTE -> AssistantScreen(viewModel(), onOpenSettings = { go(Screen.AJUSTES) }, onOpenProfile = profile)
                    Screen.GIMNASIO -> GymScreen(viewModel(), onBack = { screen = Screen.ESTADISTICAS })
                    Screen.PERFIL -> ProfileScreen(
                        viewModel(),
                        onBack = { screen = previous.takeIf { it.icon != null } ?: Screen.HOY },
                        onSettings = { go(Screen.AJUSTES) },
                        onSignIn = onWantsAccount,
                    )
                    Screen.AJUSTES -> SettingsScreen(onBack = { screen = Screen.PERFIL }) {
                        Text("Cuenta y nube", style = MaterialTheme.typography.titleMedium)
                        AccountPanel("Con una cuenta, tu rutina, tareas, historial y gimnasio se guardan en la nube y los recuperas si cambias de móvil.")
                        HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        AiSection()
                        HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        BackupSection()
                        HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        VersionSection()
                    }
                }
            }
            CloudChoiceDialog()
        }
    }
}

/** Tras iniciar sesión con datos en la nube: elegir con cuáles quedarse, estés donde estés. */
@Composable
private fun CloudChoiceDialog() {
    val app = LocalContext.current.applicationContext as App
    val pending by app.cloud.pendingChoice.collectAsState()
    if (pending != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = {},
            title = { Text("Ya hay datos en tu cuenta") },
            text = { AccountPanel("") },
            confirmButton = {},
        )
    }
}

/** Versión instalada y botón para buscar actualizaciones a mano. */
@Composable
private fun VersionSection() {
    val app = LocalContext.current.applicationContext as App
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var result by androidx.compose.runtime.remember { mutableStateOf<String?>(null) }
    var busy by androidx.compose.runtime.remember { mutableStateOf(false) }
    Text("Versión", style = MaterialTheme.typography.titleMedium)
    Text("KeepTrack ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyMedium)
    androidx.compose.material3.OutlinedButton(enabled = !busy, onClick = {
        busy = true
        result = null
        scope.launch {
            val error = app.updater.check(force = true)
            result = error ?: if (app.updater.state.value is com.sunless404.keeptrack.update.UpdateState.Idle) "Tienes la última versión." else null
            busy = false
        }
    }) { Text(if (busy) "Comprobando…" else "Buscar actualizaciones") }
    result?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
}
