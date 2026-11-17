package com.sunless404.keeptrack.ui.stats

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sunless404.keeptrack.domain.StreakLevel
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Llama de una racha. Con más días cambia de color (naranja → rojo → rosa → violeta →
 * azul → blanco dorado), crece, parpadea más rápido y gana halo, chispas y rayos.
 */
@Composable
fun StreakFlame(days: Int, size: Dp = 56.dp, idle: Color = Color(0xFF6B7785)) {
    val level = StreakLevel.of(days)
    val tier = level.ordinal // 0..7
    val t by rememberInfiniteTransition(label = "llama").animateFloat(
        0f, 1f, infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "t",
    )
    Canvas(Modifier.size(size)) {
        if (tier == 0) drawIdleFlame(idle) else drawFlame(t, tier, Color(level.color), Color(level.core))
    }
}

private fun flamePath(c: Offset, w: Float, h: Float, t: Float, seed: Float, sway: Float): Path {
    val s1 = sin((t + seed) * 2 * PI).toFloat()
    val s2 = sin((t * 2 + seed * 1.7f) * 2 * PI).toFloat()
    val tipX = c.x + s1 * w * sway
    val tipY = c.y - h + s2 * h * 0.05f
    return Path().apply {
        moveTo(c.x, c.y + h * 0.18f)
        cubicTo(c.x - w * 1.05f, c.y + h * 0.12f, c.x - w * 0.95f, c.y - h * 0.45f, c.x - w * 0.35f + s2 * w * 0.08f, c.y - h * 0.6f)
        quadraticTo(c.x - w * 0.1f + s1 * w * 0.15f, c.y - h * 0.8f, tipX, tipY)
        quadraticTo(c.x + w * 0.2f + s1 * w * 0.15f, c.y - h * 0.75f, c.x + w * 0.45f - s2 * w * 0.08f, c.y - h * 0.55f)
        cubicTo(c.x + w * 1.0f, c.y - h * 0.35f, c.x + w * 1.05f, c.y + h * 0.12f, c.x, c.y + h * 0.18f)
        close()
    }
}

private fun DrawScope.drawIdleFlame(color: Color) {
    val c = Offset(size.width / 2, size.height * 0.68f)
    drawPath(flamePath(c, size.width * 0.26f, size.height * 0.55f, 0f, 0f, 0f), color, style = Stroke(size.width * 0.05f))
}

private fun DrawScope.drawFlame(t: Float, tier: Int, color: Color, core: Color) {
    val w = size.width
    val c = Offset(w / 2, size.height * 0.7f)
    val grow = 0.82f + 0.03f * tier
    val fw = w * 0.28f * grow
    val fh = size.height * 0.58f * grow
    val speed = 1f + tier * 0.25f
    val sway = 0.12f + 0.03f * tier

    // Halo: aparece a partir de los 7 días y late más fuerte cuanto más alto.
    if (tier >= 3) {
        val pulse = 0.5f + 0.5f * sin(t * 2 * PI * speed).toFloat()
        drawCircle(
            Brush.radialGradient(listOf(color.copy(alpha = 0.18f + 0.06f * tier * pulse), Color.Transparent), Offset(c.x, c.y - fh * 0.35f), w * 0.55f),
            w * 0.55f, Offset(c.x, c.y - fh * 0.35f),
        )
    }
    // Rayos de "supernova".
    if (tier >= 7) {
        for (i in 0 until 12) {
            val a = (i / 12f + t * 0.15f) * 2 * PI.toFloat()
            val len = w * (0.42f + 0.06f * sin(t * 2 * PI * 3 + i).toFloat())
            val o = Offset(c.x, c.y - fh * 0.4f)
            drawLine(core.copy(alpha = 0.5f), Offset(o.x + cos(a) * w * 0.25f, o.y + sin(a) * w * 0.25f), Offset(o.x + cos(a) * len, o.y + sin(a) * len), w * 0.015f, blendMode = BlendMode.Plus)
        }
    }
    // Capas de la llama: exterior, media y núcleo.
    drawPath(flamePath(c, fw, fh, t * speed, 0f, sway), Brush.verticalGradient(listOf(color.copy(alpha = 0.6f), color), c.y - fh, c.y))
    drawPath(flamePath(Offset(c.x, c.y + fh * 0.02f), fw * 0.68f, fh * 0.72f, t * speed, 0.33f, sway * 1.2f), Brush.verticalGradient(listOf(core.copy(alpha = 0.8f), color), c.y - fh, c.y))
    drawPath(flamePath(Offset(c.x, c.y + fh * 0.04f), fw * 0.36f, fh * 0.42f, t * speed, 0.66f, sway * 1.4f), core, blendMode = BlendMode.Plus)

    // Chispas que suben: desde los 14 días, más numerosas después.
    if (tier >= 4) {
        val n = 3 + (tier - 4) * 3
        for (i in 0 until n) {
            val ph = (t * speed * 0.8f + i.toFloat() / n) % 1f
            val x = c.x + fw * 1.1f * sin((i * 2.3f + ph * 4f)) * (0.4f + 0.6f * ph)
            val y = c.y - fh * (0.5f + ph * 0.9f)
            drawCircle(core.copy(alpha = (1 - ph) * 0.9f), w * 0.025f * (1 - ph * 0.5f), Offset(x, y), blendMode = BlendMode.Plus)
        }
    }
}
