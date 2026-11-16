package com.sunless404.keeptrack.ui.gym

import android.app.Application
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.sunless404.keeptrack.App
import com.sunless404.keeptrack.data.GymSession
import com.sunless404.keeptrack.data.GymSet
import com.sunless404.keeptrack.domain.GymScore
import com.sunless404.keeptrack.domain.GymSummary
import com.sunless404.keeptrack.domain.dayName
import com.sunless404.keeptrack.domain.fmtShortDate
import com.sunless404.keeptrack.ui.account.FriendsLeaderboard
import com.sunless404.keeptrack.ui.common.KtIcons
import com.sunless404.keeptrack.ui.theme.Brand
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class GymUi(
    val sessions: List<GymSession> = emptyList(),
    val sets: Map<Long, List<GymSet>> = emptyMap(),
    val summary: GymSummary = GymScore.summarize(emptyList(), emptyList(), LocalDate.now()),
)

class GymViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as App).repo

    val ui = combine(repo.observeGymSessions(), repo.observeGymSets()) { sessions, sets ->
        GymUi(sessions, sets.groupBy { it.sessionId }, GymScore.summarize(sessions, sets, LocalDate.now()))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GymUi())

    fun save(session: GymSession, sets: List<GymSet>) = viewModelScope.launch { repo.saveGymSession(session, sets) }
    fun delete(id: Long) = viewModelScope.launch { repo.deleteGymSession(id) }
}

@Composable
fun GymScreen(vm: GymViewModel, onBack: () -> Unit) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf<Pair<GymSession, List<GymSet>>?>(null) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            if (tab == 0) ExtendedFloatingActionButton(
                onClick = {
                    editing = GymSession(epochDay = LocalDate.now().toEpochDay(), bodyWeightKg = ui.summary.bodyWeightKg) to emptyList()
                },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Nueva sesión") },
            )
        },
    ) { inner ->
        LazyColumn(
            Modifier.fillMaxSize().padding(inner).windowInsetsPadding(WindowInsets.statusBars),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") }
                    Column {
                        Text("Gimnasio", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            "Has encontrado la sección secreta",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            item { Header(ui.summary) }
            item {
                PrimaryTabRow(selectedTabIndex = tab, containerColor = MaterialTheme.colorScheme.surface) {
                    listOf("Sesiones", "Récords", "Clasificación").forEachIndexed { i, t ->
                        Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t) })
                    }
                }
            }
            when (tab) {
                0 -> {
                    if (ui.sessions.isEmpty()) item {
                        Text(
                            "Aún no hay sesiones. Registra la primera con «Nueva sesión».",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(8.dp),
                        )
                    }
                    items(ui.sessions, key = { it.id }) { s ->
                        SessionCard(s, ui.sets[s.id].orEmpty()) { editing = s to ui.sets[s.id].orEmpty() }
                    }
                }
                1 -> {
                    if (ui.summary.records.isEmpty()) item { Text("Sin récords todavía.", modifier = Modifier.padding(8.dp)) }
                    items(ui.summary.records, key = { GymScore.normalize(it.exercise) }) { r ->
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer), shape = MaterialTheme.shapes.medium) {
                            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(r.exercise, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "${kg(r.bestSet.weightKg)} × ${r.bestSet.reps} · ${fmtShortDate(LocalDate.ofEpochDay(r.epochDay))}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("1RM ≈ ${kg(r.bestE1rm)}", style = MaterialTheme.typography.titleSmall)
                                    r.relative?.let {
                                        Text("× %.2f tu peso".format(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                    }
                                }
                            }
                        }
                    }
                }
                else -> item { Leaderboard(ui.summary) }
            }
        }
    }

    editing?.let { (session, sets) ->
        SessionEditorDialog(
            session = session,
            sets = sets,
            known = ui.summary.records.map { it.exercise },
            onDismiss = { editing = null },
            onDelete = if (session.id != 0L) ({ vm.delete(session.id); editing = null }) else null,
        ) { s, list ->
            vm.save(s, list)
            editing = null
        }
    }
}

fun kg(v: Double): String = if (v % 1.0 == 0.0) "${v.toInt()} kg" else "%.1f kg".format(v)

@Composable
private fun Header(s: GymSummary) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val points by animateIntAsState(if (shown) s.weekPoints else 0, tween(1000, easing = FastOutSlowInEasing), label = "puntos")
    val strength by animateIntAsState(if (shown) s.strengthScore else 0, tween(1200, easing = FastOutSlowInEasing), label = "fuerza")
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = MaterialTheme.shapes.large,
    ) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("ESTA SEMANA", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("$points", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, color = Brand.Fire)
                    Text(" pts", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                }
                Text(
                    "${s.sessionsThisWeek} sesiones · ${s.prsThisWeek} récords · racha ${s.streakWeeks} sem.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(KtIcons.Trophy, null, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.secondary)
                Text("$strength", style = MaterialTheme.typography.headlineSmall)
                Text("fuerza", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SessionCard(s: GymSession, sets: List<GymSet>, onClick: () -> Unit) {
    val date = LocalDate.ofEpochDay(s.epochDay)
    val volume = sets.sumOf { it.weightKg * it.reps }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
                    Icon(KtIcons.Dumbbell, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("${dayName(date.dayOfWeek.value)} ${fmtShortDate(date)}", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${sets.size} series · volumen ${kg(volume)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            val byEx = sets.groupBy { it.exercise }
            if (byEx.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                byEx.forEach { (ex, list) ->
                    Text(
                        "$ex: " + list.joinToString("  ") { "${kg(it.weightKg).removeSuffix(" kg")}×${it.reps}" },
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun Leaderboard(s: GymSummary) {
    var weekly by rememberSaveable { mutableStateOf(true) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            SegmentedButton(selected = weekly, onClick = { weekly = true }, shape = SegmentedButtonDefaults.itemShape(0, 2)) { Text("Semanal") }
            SegmentedButton(selected = !weekly, onClick = { weekly = false }, shape = SegmentedButtonDefaults.itemShape(1, 2)) { Text("Histórica") }
        }
        Text(
            if (weekly) "Tus puntos esta semana: ${s.weekPoints}" else "Tu fuerza relativa: ${s.strengthScore}",
            style = MaterialTheme.typography.titleSmall,
        )
        FriendsLeaderboard(weekly)
        Text(
            "Cómo se puntúa: 10 pts por sesión (hasta 5 por semana); por cada récord, 5 × % de mejora " +
                "(entre 5 y 50; un ejercicio nuevo da 5); y 5 pts por semana seguida yendo (hasta 25). " +
                "La histórica suma tu fuerza relativa (máximo estimado ÷ peso corporal) en tus 5 mejores ejercicios.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
