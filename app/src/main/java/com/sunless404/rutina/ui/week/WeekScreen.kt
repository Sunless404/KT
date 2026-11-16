package com.sunless404.rutina.ui.week

import android.app.Application
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.sunless404.rutina.App
import com.sunless404.rutina.data.DayData
import com.sunless404.rutina.data.DayEntry
import com.sunless404.rutina.data.DayType
import com.sunless404.rutina.data.Status
import com.sunless404.rutina.domain.dayName
import com.sunless404.rutina.domain.fmtShortDate
import com.sunless404.rutina.domain.fmtTime
import com.sunless404.rutina.domain.mondayOf
import com.sunless404.rutina.ui.common.ScreenScaffold
import com.sunless404.rutina.ui.common.composeColor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class WeekUi(val monday: LocalDate, val days: List<DayData> = emptyList())

class WeekViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as App).repo
    private val monday = MutableStateFlow(mondayOf(LocalDate.now()))

    val ui = monday.flatMapLatest { m ->
        repo.observeRange(m, m.plusDays(6)).map { WeekUi(m, it.values.sortedBy { d -> d.epochDay }) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WeekUi(monday.value))

    fun shift(weeks: Long) {
        monday.value = monday.value.plusWeeks(weeks)
    }

    fun goToday() {
        monday.value = mondayOf(LocalDate.now())
    }
}

@Composable
fun WeekScreen(vm: WeekViewModel, onOpenDay: (LocalDate) -> Unit) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val today = LocalDate.now()

    ScreenScaffold(
        title = "Semana",
        actions = {
            if (ui.monday != mondayOf(today)) TextButton(onClick = vm::goToday) { Text("Esta semana") }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    IconButton(onClick = { vm.shift(-1) }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Semana anterior")
                    }
                    Text(
                        "${fmtShortDate(ui.monday)} – ${fmtShortDate(ui.monday.plusDays(6))}",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { vm.shift(1) }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Semana siguiente")
                    }
                }
            }
            items(ui.days, key = { it.epochDay }) { day ->
                val date = LocalDate.ofEpochDay(day.epochDay)
                DayCard(day, date, isToday = date == today, isFuture = date > today) { onOpenDay(date) }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun DayCard(day: DayData, date: LocalDate, isToday: Boolean, isFuture: Boolean, onClick: () -> Unit) {
    val done = day.entries.count { it.status == Status.HECHO }
    val pct = if (day.entries.isEmpty()) 0 else done * 100 / day.entries.size
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isToday) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${dayName(date.dayOfWeek.value)} ${fmtShortDate(date)}" +
                        if (day.type != DayType.NORMAL) " · ${day.type.label}" else "",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    if (isFuture) "${day.entries.size} bloques" else "$pct % ($done/${day.entries.size})",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Spacer(Modifier.height(6.dp))
            DayStrip(day.entries)
            Spacer(Modifier.height(4.dp))
            Text(
                day.entries.joinToString("  ·  ") { "${fmtTime(it.startMin)} ${it.title}" },
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Franja de 24 h con cada bloque en su color; los hechos opacos, el resto atenuados. */
@Composable
fun DayStrip(entries: List<DayEntry>, modifier: Modifier = Modifier) {
    val track = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier.fillMaxWidth().height(14.dp)) {
        drawRect(track.copy(alpha = 0.4f))
        entries.forEach { e ->
            val start = e.startMin.coerceIn(0, 1440)
            val end = e.endMin.coerceIn(0, 1440)
            val color: Color = e.category.composeColor.copy(
                alpha = when (e.status) {
                    Status.HECHO -> 1f
                    Status.SALTADO -> 0.2f
                    else -> 0.55f
                }
            )
            drawRect(
                color,
                topLeft = Offset(size.width * start / 1440f, 0f),
                size = Size(size.width * (end - start) / 1440f, size.height),
            )
        }
    }
}
