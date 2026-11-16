package com.sunless404.rutina.ui.stats

import android.app.Application
import androidx.compose.foundation.clickable
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
import com.sunless404.rutina.App
import com.sunless404.rutina.domain.CategoryHours
import com.sunless404.rutina.domain.Stats
import com.sunless404.rutina.domain.Streak
import com.sunless404.rutina.domain.WeekSummary
import com.sunless404.rutina.domain.fmtDuration
import com.sunless404.rutina.domain.fmtShortDate
import com.sunless404.rutina.domain.mondayOf
import com.sunless404.rutina.ui.common.ScreenScaffold
import com.sunless404.rutina.ui.common.composeColor
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
fun StatsScreen(vm: StatsViewModel) {
    val ui by vm.ui.collectAsStateWithLifecycle()

    ScreenScaffold(title = "Estadísticas") { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { vm.shift(-1) }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Semana anterior")
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Horas por categoría", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Semana del ${fmtShortDate(ui.monday)} al ${fmtShortDate(ui.monday.plusDays(6))}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    IconButton(onClick = { vm.shift(1) }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Semana siguiente")
                    }
                }
            }
            if (ui.hours.isEmpty()) item { Text("Sin datos esta semana.") }
            items(ui.hours, key = { it.category }) { h -> HoursRow(h) }

            item {
                Spacer(Modifier.height(8.dp))
                Text("Rachas", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Días seguidos cumpliendo el hábito. Festivos, días libres y días sin ese bloque no cuentan. " +
                        "Sueño a su hora: marcado como hecho a ±${Stats.SLEEP_TOLERANCE_MIN} min de su inicio.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(ui.streaks, key = { it.habit }) { s ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(s.habit.label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                if (s.current == 1) "1 día" else "${s.current} días",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = s.habit.category.composeColor,
                            )
                            Text("Mejor: ${s.best}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                Text("Historial de semanas", style = MaterialTheme.typography.titleMedium)
            }
            if (ui.history.isEmpty()) item { Text("Aún no hay semanas registradas.") }
            items(ui.history, key = { it.mondayEpoch }) { w ->
                val m = LocalDate.ofEpochDay(w.mondayEpoch)
                Column(Modifier.fillMaxWidth().clickable { vm.setWeek(w.mondayEpoch) }.padding(vertical = 4.dp)) {
                    Row {
                        Text(
                            "${fmtShortDate(m)} – ${fmtShortDate(m.plusDays(6))}",
                            modifier = Modifier.weight(1f),
                            fontWeight = if (m == ui.monday) FontWeight.Bold else FontWeight.Normal,
                        )
                        Text("${w.percent} % (${w.done}/${w.total})")
                    }
                    LinearProgressIndicator(progress = { w.percent / 100f }, modifier = Modifier.fillMaxWidth().height(6.dp))
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun HoursRow(h: CategoryHours) {
    Column(Modifier.fillMaxWidth()) {
        Row {
            Text(h.category.label, modifier = Modifier.weight(1f))
            Text(
                "${fmtDuration(h.doneMin)} de ${fmtDuration(h.plannedMin)}",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        LinearProgressIndicator(
            progress = { if (h.plannedMin == 0) 0f else h.doneMin.toFloat() / h.plannedMin },
            color = h.category.composeColor,
            modifier = Modifier.fillMaxWidth().height(10.dp),
        )
    }
}
