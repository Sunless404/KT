package com.sunless404.keeptrack.ui.account

import android.app.Application
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.sunless404.keeptrack.App
import com.sunless404.keeptrack.data.Status
import com.sunless404.keeptrack.domain.Achievement
import com.sunless404.keeptrack.domain.AchievementIcon
import com.sunless404.keeptrack.domain.Achievements
import com.sunless404.keeptrack.domain.ES
import com.sunless404.keeptrack.domain.GymScore
import com.sunless404.keeptrack.domain.ProfileTotals
import com.sunless404.keeptrack.domain.Stats
import com.sunless404.keeptrack.ui.common.AnimatedNumber
import com.sunless404.keeptrack.ui.common.Avatar
import com.sunless404.keeptrack.ui.common.HelpButton
import com.sunless404.keeptrack.ui.common.KtIcons
import com.sunless404.keeptrack.ui.common.SectionLabel
import com.sunless404.keeptrack.ui.common.appear
import com.sunless404.keeptrack.ui.theme.Brand
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ProfileViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as App).repo
    private val today = LocalDate.now()

    val totals = combine(
        repo.observeRange(today.minusDays(365), today),
        repo.observeTasks(today.minusDays(365), today.plusDays(365)),
        repo.observeGymSessions(),
        repo.observeGymSets(),
    ) { days, tasks, sessions, sets ->
        val streaks = Stats.streaks(days.values, today.toEpochDay())
        ProfileTotals(
            blocksDone = days.values.sumOf { d -> d.entries.count { it.status == Status.HECHO } },
            tasksDone = tasks.count { it.done },
            gymSessions = sessions.size,
            gymExercises = sets.map { GymScore.normalize(it.exercise) }.distinct().size,
            bestStreak = streaks.maxOfOrNull { it.best } ?: 0,
            bestByHabit = streaks.associate { it.habit to it.best },
            currentByHabit = streaks.associate { it.habit to it.current },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileTotals())
}

private val sinceFmt = DateTimeFormatter.ofPattern("MMMM 'de' yyyy", ES)

@Composable
fun ProfileScreen(vm: ProfileViewModel, onBack: () -> Unit, onSettings: () -> Unit, onSignIn: () -> Unit) {
    val app = LocalContext.current.applicationContext as App
    val profile by app.profile.profile.collectAsState()
    val account by app.cloud.account.collectAsState()
    val totals by vm.totals.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) app.scope.launch {
            try {
                app.profile.setPhoto(app.profile.photoFromUri(uri))
            } catch (e: Exception) {
                error = "No se pudo usar esa imagen: ${e.message}"
            }
        }
    }
    val name = profile.displayName.ifBlank { account?.username ?: "Invitado" }

    LazyColumn(
        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") }
                Text("Perfil", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                HelpButton(
                    "Perfil",
                    listOf(
                        "Toca tu foto para elegir otra de la galería.",
                        "«Editar perfil» cambia tu nombre visible y tu biografía. Tus amigos los ven en la clasificación.",
                        "Los logros se desbloquean solos al completar bloques, rachas, tareas y sesiones de gimnasio.",
                        "Desde aquí entras en Ajustes y gestionas tu cuenta.",
                    ),
                )
                IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, "Ajustes") }
            }
        }
        item {
            Column(Modifier.fillMaxWidth().appear(0), horizontalAlignment = Alignment.CenterHorizontally) {
                Box {
                    Box(
                        Modifier.size(112.dp).clip(CircleShape)
                            .background(Brush.sweepGradient(listOf(Brand.Accent, Brand.Fire, Color(0xFFB46CFF), Brand.Accent))),
                        contentAlignment = Alignment.Center,
                    ) {
                        Avatar(profile.photo, profile.displayName, 104.dp, Modifier.border(3.dp, MaterialTheme.colorScheme.background, CircleShape)) {
                            pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                    }
                    Box(
                        Modifier.align(Alignment.BottomEnd).offset((-4).dp, (-4).dp).size(30.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center,
                    ) { Icon(KtIcons.Camera, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimary) }
                }
                Spacer(Modifier.height(12.dp))
                Text(name, style = MaterialTheme.typography.headlineSmall)
                account?.username?.let { Text("@$it", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary) }
                if (profile.bio.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(profile.bio, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Miembro desde " + Instant.ofEpochMilli(profile.memberSince).atZone(ZoneId.systemDefault()).format(sinceFmt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                FilledTonalButton(onClick = { editing = true }) {
                    Icon(KtIcons.Pencil, null, Modifier.size(16.dp))
                    Text("  Editar perfil")
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("Bloques hechos", totals.blocksDone, KtIcons.Routine, 1, Modifier.weight(1f))
                StatTile("Mejor racha", totals.bestStreak, KtIcons.Flame, 2, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("Tareas hechas", totals.tasksDone, KtIcons.Tasks, 3, Modifier.weight(1f))
                StatTile("Sesiones de gym", totals.gymSessions, KtIcons.Dumbbell, 4, Modifier.weight(1f))
            }
        }
        val list = Achievements.of(totals)
        item { SectionLabel("Logros · ${list.count { it.unlocked }}/${list.size}") }
        list.chunked(2).forEachIndexed { r, pair ->
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    pair.forEachIndexed { c, a -> AchievementTile(a, 5 + r * 2 + c, Modifier.weight(1f)) }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        item { SectionLabel("Cuenta") }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.appear(10),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (account == null) {
                        Text("Estás como invitado", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Tus datos solo están en este móvil. Crea una cuenta para guardarlos en la nube y competir con tus amigos.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Button(onClick = onSignIn, enabled = app.cloud.available) { Text("Crear cuenta o entrar") }
                    } else {
                        AccountPanel("")
                    }
                }
            }
        }
    }

    if (editing) {
        var n by remember { mutableStateOf(profile.displayName) }
        var b by remember { mutableStateOf(profile.bio) }
        AlertDialog(
            onDismissRequest = { editing = false },
            title = { Text("Editar perfil") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = n, onValueChange = { n = it.take(40) }, label = { Text("Nombre visible") }, singleLine = true,
                        supportingText = { Text("${n.length}/40") }, modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = b, onValueChange = { b = it.take(160) }, label = { Text("Biografía") }, minLines = 3,
                        supportingText = { Text("${b.length}/160") }, modifier = Modifier.fillMaxWidth(),
                    )
                    if (profile.photo != null) TextButton(onClick = { app.profile.setPhoto(null) }) { Text("Quitar foto") }
                }
            },
            confirmButton = { TextButton(onClick = { app.profile.update(n, b); editing = false }) { Text("Guardar") } },
            dismissButton = { TextButton(onClick = { editing = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun StatTile(label: String, value: Int, icon: ImageVector, index: Int, modifier: Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = MaterialTheme.shapes.large,
        modifier = modifier.appear(index),
    ) {
        Column(Modifier.padding(14.dp)) {
            Icon(icon, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.height(6.dp))
            AnimatedNumber(value, MaterialTheme.typography.headlineMedium)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AchievementTile(a: Achievement, index: Int, modifier: Modifier) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val p by animateFloatAsState(if (shown) a.progress else 0f, tween(900, 150 + index * 40, FastOutSlowInEasing), label = "logro")
    val icon = when (a.icon) {
        AchievementIcon.STEP -> KtIcons.Today
        AchievementIcon.BLOCKS -> KtIcons.Routine
        AchievementIcon.FIRE -> KtIcons.Flame
        AchievementIcon.BOOK -> KtIcons.Book
        AchievementIcon.BED -> KtIcons.Bed
        AchievementIcon.DUMBBELL -> KtIcons.Dumbbell
        AchievementIcon.TASKS -> KtIcons.Tasks
        AchievementIcon.TROPHY -> KtIcons.Trophy
    }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (a.unlocked) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        ),
        shape = MaterialTheme.shapes.large,
        modifier = modifier.appear(index),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(36.dp).clip(CircleShape)
                        .background(if (a.unlocked) Brand.Fire.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, null, Modifier.size(20.dp).alpha(if (a.unlocked) 1f else 0.45f), tint = if (a.unlocked) Brand.Fire else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.width(10.dp))
                Text(a.title, style = MaterialTheme.typography.titleSmall, maxLines = 2)
            }
            Spacer(Modifier.height(6.dp))
            Text(a.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, minLines = 2, maxLines = 2)
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth().height(5.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(p).clip(CircleShape).background(if (a.unlocked) Brand.Fire else MaterialTheme.colorScheme.secondary))
            }
            Text(
                if (a.unlocked) "Conseguido" else "${a.current.coerceAtMost(a.goal)}/${a.goal}",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
