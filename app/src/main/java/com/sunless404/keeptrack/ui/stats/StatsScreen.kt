package com.sunless404.keeptrack.ui.stats

import android.app.Application
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import com.sunless404.keeptrack.domain.Habit
import com.sunless404.keeptrack.domain.StreakLevel
import com.sunless404.keeptrack.ui.common.HelpTexts
import com.sunless404.keeptrack.ui.common.TopActions
import com.sunless404.keeptrack.ui.common.appear
import com.sunless404.keeptrack.ui.common.KtIcons
import com.sunless404.keeptrack.ui.common.ProgressRing
import com.sunless404.keeptrack.ui.common.SectionLabel
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.sunless404.keeptrack.App
import com.sunless404.keeptrack.domain.CategoryHours
import com.sunless404.keeptrack.domain.Stats
import com.sunless404.keeptrack.domain.Streak
import com.sunless404.keeptrack.domain.WeekSummary
import com.sunless404.keeptrack.domain.fmtDuration
import com.sunless404.keeptrack.domain.fmtShortDate
import com.sunless404.keeptrack.domain.mondayOf
import com.sunless404.keeptrack.ui.common.ScreenScaffold
import com.sunless404.keeptrack.ui.common.composeColor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class StatsUi(
    val monday: LocalDate = mondayOf(LocalDate.now()),
    val hours: List<CategoryHours> = emptyList(),
    val streaks: List<Streak> = emptyList(),
    val history: List<WeekSummary> = emptyList(),
)

class StatsViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as App).repo
    private val thisMonday = mondayOf(LocalDate.now())
    private val monday = MutableStateFlow(thisMonday)

    val ui = combine(
        repo.observeRange(thisMonday.minusWeeks(HISTORY_WEEKS), thisMonday.plusDays(6)),
        monday,
    ) { days, m ->
        val today = LocalDate.now().toEpochDay()
        val week = (0L..6L).flatMap { days[m.toEpochDay() + it]?.entries.orEmpty() }
        StatsUi(
            monday = m,
            hours = Stats.hoursByCategory(week),
            streaks = Stats.streaks(days.values, today),
            history = Stats.history(days.values, today),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUi())

    fun shift(weeks: Long) {
        val m = monday.value.plusWeeks(weeks)
        if (m <= thisMonday && m >= thisMonday.minusWeeks(HISTORY_WEEKS)) monday.value = m
    }

    fun setWeek(mondayEpoch: Long) {
        monday.value = LocalDate.ofEpochDay(mondayEpoch)
    }

    companion object {
        const val HISTORY_WEEKS = 26L
    }
}

@Composable
fun StatsScreen(vm: StatsViewModel, onOpenGym: () -> Unit, onOpenProfile: () -> Unit) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    // Se activa al entrar: las animaciones arrancan desde cero.
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }

    LazyColumn(
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                TopActions("Estadísticas", HelpTexts.STATS, onOpenProfile)
            }
        }
        item { SectionLabel("Rachas") }
        item {
            val rows = ui.streaks.chunked(2)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                rows.forEachIndexed { r, pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        pair.forEachIndexed { c, s ->
                            StreakCard(
                                s,
                                visible = shown,
                                delayMs = (r * 2 + c) * 90,
                                onClick = if (s.habit == Habit.GIMNASIO) onOpenGym else null,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
        item {
            Text(
                "Festivos, días libres y días sin ese bloque no cuentan. Sueño: cuenta si lo marcas a ±${Stats.SLEEP_TOLERANCE_MIN} min de su inicio.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }

        item {
            SectionLabel("Horas por categoría") {
                IconButton(onClick = { vm.shift(-1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Semana anterior") }
                IconButton(onClick = { vm.shift(1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Semana siguiente") }
            }
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.appear(5),
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Semana del ${fmtShortDate(ui.monday)} al ${fmtShortDate(ui.monday.plusDays(6))}",
                        style = MaterialTheme.typography.labelLarge,
                    )
                    val planned = ui.hours.sumOf { it.plannedMin }
                    val done = ui.hours.sumOf { it.doneMin }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ProgressRing(if (planned == 0) 0f else done.toFloat() / planned, size = 64.dp, stroke = 7.dp) {
                            Text("${if (planned == 0) 0 else done * 100 / planned}%", style = MaterialTheme.typography.labelLarge)
                        }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text("${fmtDuration(done)} hechas", style = MaterialTheme.typography.titleMedium)
                            Text("de ${fmtDuration(planned)} previstas", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (ui.hours.isEmpty()) Text("Sin datos esta semana.", style = MaterialTheme.typography.bodySmall)
                    ui.hours.forEach { h -> HoursRow(h, shown) }
                }
            }
        }

        item { SectionLabel("Historial de semanas") }
        if (ui.history.isEmpty()) item { Text("Aún no hay semanas registradas.", style = MaterialTheme.typography.bodySmall) }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer), shape = MaterialTheme.shapes.large, modifier = Modifier.appear(6)) {
                Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    ui.history.forEach { w ->
                        val m = LocalDate.ofEpochDay(w.mondayEpoch)
                        val sel = m == ui.monday
                        Row(
                            Modifier.fillMaxWidth().clickable { vm.setWeek(w.mondayEpoch) }.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "${fmtShortDate(m)} – ${fmtShortDate(m.plusDays(6))}",
                                modifier = Modifier.width(120.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                            )
                            AnimatedBar(w.percent / 100f, MaterialTheme.colorScheme.secondary, shown, Modifier.weight(1f))
                            Text("${w.percent}%", modifier = Modifier.width(48.dp), style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.End)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StreakCard(s: Streak, visible: Boolean, delayMs: Int, onClick: (() -> Unit)?, modifier: Modifier) {
    val count by animateIntAsState(if (visible) s.current else 0, tween(900, delayMs, FastOutSlowInEasing), label = "racha")
    val level = StreakLevel.of(s.current)
    val levelColor = Color(level.color)
    val toNext by animateFloatAsState(if (visible) StreakLevel.progress(s.current) else 0f, tween(1000, delayMs + 200, FastOutSlowInEasing), label = "nivel")
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = MaterialTheme.shapes.large,
        modifier = modifier
            .appear(delayMs / 90)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Box {
        // La llama, mediana, en la esquina superior derecha.
        Box(Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 8.dp)) {
            StreakFlame(s.current, 40.dp, idle = MaterialTheme.colorScheme.outline)
        }
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    when (s.habit) {
                        Habit.ESTUDIO -> KtIcons.Study
                        Habit.GIMNASIO -> KtIcons.Dumbbell
                        Habit.LECTURA -> KtIcons.Book
                        Habit.SUENO -> KtIcons.Bed
                    },
                    null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text("  ${s.habit.label}", style = MaterialTheme.typography.labelLarge, maxLines = 1, modifier = Modifier.weight(1f).padding(end = 34.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "$count", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold,
                    color = if (s.current > 0) levelColor else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(if (s.current == 1) " día" else " días", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 10.dp))
            }
            val next = level.next
            Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(toNext).clip(CircleShape).background(next?.let { Color(it.color) } ?: levelColor))
            }
            Spacer(Modifier.height(6.dp))
            Text(
                when {
                    next == null -> "Llama al máximo · mejor ${s.best}"
                    next.minDays - s.current == 1 -> "Mañana mejora la llama · mejor ${s.best}"
                    else -> "${next.minDays - s.current} días para mejorar la llama · mejor ${s.best}"
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
        }
        }
    }
}

@Composable
private fun AnimatedBar(fraction: Float, color: Color, visible: Boolean, modifier: Modifier = Modifier) {
    val f by animateFloatAsState(if (visible) fraction.coerceIn(0f, 1f) else 0f, tween(800, easing = FastOutSlowInEasing), label = "barra")
    Box(modifier.height(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(f).clip(CircleShape).background(color))
    }
}

@Composable
private fun HoursRow(h: CategoryHours, visible: Boolean) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(h.category.composeColor))
            Text("  ${h.category.label}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Text(
                "${fmtDuration(h.doneMin)} / ${fmtDuration(h.plannedMin)}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(4.dp))
        AnimatedBar(if (h.plannedMin == 0) 0f else h.doneMin.toFloat() / h.plannedMin, h.category.composeColor, visible)
    }
}
