package com.sunless404.keeptrack.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.abs

private val ROW: Dp = 40.dp
private const val VISIBLE = 5
private const val LOOPS = 400

/**
 * Rueda que gira sin fin con [count] valores. [selected] es el índice elegido;
 * [onSelect] se llama al soltar cuando la rueda se detiene en otro valor.
 */
@Composable
private fun Wheel(count: Int, selected: Int, label: (Int) -> String, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val middle = LOOPS / 2 * count
    val state = rememberLazyListState(initialFirstVisibleItemIndex = middle + selected)
    val fling = rememberSnapFlingBehavior(state)
    val current by rememberUpdatedState(onSelect)
    // Índice del elemento centrado (el primero visible, porque hay relleno de 2 filas arriba).
    val centered by remember {
        derivedStateOf {
            val info = state.layoutInfo
            val half = (info.viewportStartOffset + info.viewportEndOffset) / 2
            info.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2 - half) }?.index ?: state.firstVisibleItemIndex
        }
    }
    LaunchedEffect(state) {
        snapshotFlow { state.isScrollInProgress to centered }
            .distinctUntilChanged()
            .collect { (moving, idx) -> if (!moving) current(idx % count) }
    }
    LazyColumn(
        state = state,
        flingBehavior = fling,
        contentPadding = PaddingValues(vertical = ROW * (VISIBLE / 2)),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.height(ROW * VISIBLE),
    ) {
        items(count * LOOPS) { i ->
            val dist = abs(i - centered).coerceAtMost(3)
            Box(Modifier.height(ROW).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    label(i % count),
                    style = if (dist == 0) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge,
                    fontWeight = if (dist == 0) FontWeight.SemiBold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .alpha(if (dist == 0) 1f else 0.55f - 0.15f * (dist - 1))
                        .graphicsLayer { val sc = 1f - 0.08f * dist; scaleX = sc; scaleY = sc },
                )
            }
        }
    }
}

/**
 * Selector de hora con dos ruedas (horas y minutos de [step] en [step]).
 * [minute] en minutos desde las 00:00.
 */
@Composable
fun WheelTimePicker(minute: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier, step: Int = 5) {
    val h = (minute / 60) % 24
    val m = (minute % 60) / step
    val latest by rememberUpdatedState(minute)
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        // Banda de la fila elegida.
        Box(
            Modifier.width(200.dp).height(ROW).clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.secondaryContainer),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Wheel(24, h, { "%02d".format(it) }, { nh -> onChange(nh * 60 + latest % 60) }, Modifier.width(80.dp))
            Text(":", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Wheel(60 / step, m, { "%02d".format(it * step) }, { nm -> onChange((latest / 60) * 60 + nm * step) }, Modifier.width(80.dp))
        }
    }
}
