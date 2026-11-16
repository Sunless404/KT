package com.sunless404.keeptrack.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sunless404.keeptrack.data.DayEntry
import com.sunless404.keeptrack.data.Status
import com.sunless404.keeptrack.data.Category

val Category.composeColor: Color get() = Color(color)

/** Scaffold de pantalla: solo aplica el margen de la barra de estado (la barra inferior es global). */
@Composable
fun ScreenScaffold(
    title: String,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(title) }, navigationIcon = navigationIcon, actions = actions) },
        floatingActionButton = floatingActionButton,
        contentWindowInsets = WindowInsets.statusBars,
        content = content,
    )
}

/** Selector de hora en formato 24 h. Devuelve minutos desde las 00:00. */
@Composable
fun TimePickerDialog(initialMin: Int, title: String, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    val state = rememberTimePickerState(initialHour = initialMin / 60, initialMinute = initialMin % 60, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Column { TimePicker(state = state) } },
        confirmButton = { TextButton(onClick = { onConfirm(state.hour * 60 + state.minute) }) { Text("Aceptar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

/** Título de sección pequeño en mayúsculas. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, trailing: @Composable () -> Unit = {}) {
    Row(modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.2.sp,
            modifier = Modifier.weight(1f),
        )
        trailing()
    }
}

/** Anillo de progreso animado con texto en el centro. */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
    stroke: Dp = 8.dp,
    color: Color = MaterialTheme.colorScheme.secondary,
    content: @Composable () -> Unit = {},
) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), tween(700), label = "ring")
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val s = stroke.toPx()
            val arc = Size(this.size.width - s, this.size.height - s)
            val tl = Offset(s / 2, s / 2)
            drawArc(track, 0f, 360f, false, tl, arc, style = Stroke(s))
            drawArc(color, -90f, 360f * animated, false, tl, arc, style = Stroke(s, cap = StrokeCap.Round))
        }
        content()
    }
}

/** Franja de 24 h con cada bloque en su color; los hechos opacos, el resto atenuados. */
@Composable
fun DayStrip(entries: List<DayEntry>, modifier: Modifier = Modifier, height: Dp = 10.dp) {
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    Canvas(modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(50))) {
        drawRect(track)
        entries.forEach { e ->
            val start = e.startMin.coerceIn(0, 1440)
            val end = e.endMin.coerceIn(0, 1440)
            val alpha = when (e.status) {
                Status.HECHO -> 1f
                Status.SALTADO -> 0.25f
                else -> 0.7f
            }
            drawRect(
                e.category.composeColor.copy(alpha = alpha),
                topLeft = Offset(size.width * start / 1440f, 0f),
                size = Size(size.width * (end - start) / 1440f, size.height),
            )
        }
    }
}
