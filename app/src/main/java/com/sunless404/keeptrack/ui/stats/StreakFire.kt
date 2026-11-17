package com.sunless404.keeptrack.ui.stats

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sunless404.keeptrack.domain.StreakLevel
import com.sunless404.keeptrack.ui.common.KtIcons
import kotlin.math.sin

/**
 * Icono de un hábito que "arde" más cuanto mayor es la racha:
 * - Chispa: el icono se enciende en color.
 * - Llama: aparece una llama pequeña y un halo suave.
 * - Hoguera: la llama crece y el halo late.
 * - Incendio y Leyenda: halo intenso y chispas que suben.
 */
@Composable
fun StreakIcon(icon: ImageVector, days: Int, idle: Color, size: Dp = 56.dp) {
    val level = StreakLevel.of(days)
    val color = Color(level.color)
    val tier = level.ordinal // 0..5
    val t = rememberInfiniteTransition(label = "fuego")
    val pulse by t.animateFloat(
        0f, 1f, infiniteRepeatable(tween(1400 - tier * 150, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "latido",
    )
    val flicker by t.animateFloat(-6f, 6f, infiniteRepeatable(tween(260, easing = LinearEasing), RepeatMode.Reverse), label = "parpadeo")
    val rise by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1800, easing = LinearEasing)), label = "chispas")

    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        if (tier >= 2) {
            // Halo que late: más grande e intenso en niveles altos.
            val strength = 0.18f + 0.1f * (tier - 2)
            Canvas(Modifier.fillMaxSize().scale(0.85f + 0.15f * pulse + 0.05f * tier)) {
                drawCircle(
                    Brush.radialGradient(listOf(color.copy(alpha = strength + 0.12f * pulse), Color.Transparent)),
                    radius = this.size.minDimension / 2,
                )
            }
        }
        if (tier >= 4) {
            // Chispas que suben y se apagan.
            Canvas(Modifier.fillMaxSize()) {
                val n = if (tier == 5) 7 else 5
                for (i in 0 until n) {
                    val phase = (rise + i.toFloat() / n) % 1f
                    val x = this.size.width * (0.2f + 0.6f * ((i * 37 % 100) / 100f)) + sin((phase + i) * 6f) * 4f
                    val y = this.size.height * (0.85f - 0.8f * phase)
                    drawCircle(color.copy(alpha = (1f - phase) * 0.9f), radius = 2.2f * (1f - phase) + 0.8f, center = Offset(x, y))
                }
            }
        }
        Icon(
            icon, null,
            tint = if (tier == 0) idle else color,
            modifier = Modifier.size(size * 0.5f).scale(if (tier >= 1) 1f + 0.06f * pulse else 1f),
        )
        if (tier >= 2) {
            val flameSize = size * (0.26f + 0.06f * (tier - 2))
            Icon(
                KtIcons.Flame, null, tint = color,
                modifier = Modifier.align(Alignment.BottomEnd).offset(x = 2.dp, y = 2.dp)
                    .size(flameSize).rotate(flicker * 0.6f).scale(0.9f + 0.15f * pulse),
            )
        }
    }
}
