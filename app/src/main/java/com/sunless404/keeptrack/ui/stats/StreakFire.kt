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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sunless404.keeptrack.domain.StreakLevel
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val TWO_PI = (2 * PI).toFloat()

/**
 * Llama de una racha, limpia y siempre animada:
 * - Todas: la llama respira, la punta se mece y el núcleo late.
 * - 7 días: halo suave que pulsa.
 * - 14: destellos de estrella alrededor.
 * - 30: órbita de partículas.
 * - 60: dos llamas pequeñas a los lados.
 * - 100: corona de rayos girando.
 * Con más días, el ritmo es más vivo.
 */
@Composable
fun StreakFlame(days: Int, size: Dp = 56.dp, idle: Color = Color(0xFF6B7785)) {
    val level = StreakLevel.of(days)
    val t by rememberInfiniteTransition(label = "llama").animateFloat(
        0f, 1f, infiniteRepeatable(tween(3000, easing = LinearEasing)), label = "t",
    )
    Canvas(Modifier.size(size)) {
        if (level == StreakLevel.NONE) drawIdle(t, idle)
        else drawLit(t, level.ordinal, Color(level.color), Color(level.core))
    }
}

/** Silueta de llama: base redonda, cintura y punta curvada. [sway] mueve la punta (−1..1). */
private fun flame(cx: Float, base: Float, w: Float, h: Float, sway: Float): Path = Path().apply {
    val tipX = cx + sway * w * 0.35f
    val tipY = base - h
    moveTo(cx, base)
    // Lado izquierdo: base redonda que sube hasta la punta.
    cubicTo(cx - w * 0.62f, base, cx - w * 0.62f, base - h * 0.42f, cx - w * 0.38f, base - h * 0.62f)
    cubicTo(cx - w * 0.2f, base - h * 0.78f, tipX - w * 0.05f, base - h * 0.86f, tipX, tipY)
    // Lado derecho: baja con una pequeña lengua secundaria.
    cubicTo(tipX + w * 0.12f, base - h * 0.84f, cx + w * 0.42f, base - h * 0.72f, cx + w * 0.47f, base - h * 0.5f)
    cubicTo(cx + w * 0.52f, base - h * 0.62f, cx + w * 0.6f, base - h * 0.55f, cx + w * 0.62f, base - h * 0.42f)
    cubicTo(cx + w * 0.7f, base - h * 0.12f, cx + w * 0.4f, base, cx, base)
    close()
}

private fun DrawScope.drawIdle(t: Float, color: Color) {
    val w = size.width
    val breathe = 1f + 0.03f * sin(t * TWO_PI)
    scale(breathe, Offset(w / 2, size.height * 0.88f)) {
        val p = flame(w / 2, size.height * 0.86f, w * 0.52f, size.height * 0.7f, 0f)
        drawPath(p, color.copy(alpha = 0.18f))
        drawPath(p, color.copy(alpha = 0.7f), style = Stroke(w * 0.045f))
    }
}

private fun DrawScope.drawLit(t: Float, tier: Int, color: Color, core: Color) {
    val w = size.width
    val h = size.height
    val cx = w / 2
    val base = h * 0.86f
    val speed = 1f + 0.18f * tier
    val ph = t * TWO_PI * speed
    val sway = sin(ph) * 0.5f + sin(ph * 2.3f) * 0.2f
    val breathe = 1f + 0.035f * sin(ph * 1.5f)
    val fw = w * (0.46f + 0.015f * tier)
    val fh = h * (0.62f + 0.02f * tier)
    val dark = lerp(color, Color.Black, 0.35f)

    // Brillo suave en la base, siempre.
    drawOval(
        Brush.radialGradient(listOf(color.copy(alpha = 0.35f), Color.Transparent), Offset(cx, base), w * 0.4f),
        Offset(cx - w * 0.4f, base - w * 0.12f), Size(w * 0.8f, w * 0.24f),
    )
    // 7+: halo que pulsa.
    if (tier >= 3) {
        val p = 0.5f + 0.5f * sin(ph)
        val r = w * (0.42f + 0.04f * p)
        val c = Offset(cx, base - fh * 0.45f)
        drawCircle(Brush.radialGradient(listOf(color.copy(alpha = 0.16f + 0.1f * p), Color.Transparent), c, r), r, c)
    }
    // 100: corona de rayos que gira.
    if (tier >= 7) {
        val c = Offset(cx, base - fh * 0.45f)
        rotate(t * 360f / 3, c) {
            for (i in 0 until 16) {
                val a = TWO_PI * i / 16
                val len = w * (0.4f + 0.05f * sin(ph * 2 + i))
                drawLine(
                    Brush.linearGradient(listOf(core.copy(alpha = 0.7f), Color.Transparent), Offset(c.x + cos(a) * w * 0.22f, c.y + sin(a) * w * 0.22f), Offset(c.x + cos(a) * len, c.y + sin(a) * len)),
                    Offset(c.x + cos(a) * w * 0.22f, c.y + sin(a) * w * 0.22f), Offset(c.x + cos(a) * len, c.y + sin(a) * len),
                    w * (if (i % 2 == 0) 0.03f else 0.018f), StrokeCap.Round,
                )
            }
        }
    }
    // 30+: órbita de partículas (elipse inclinada que gira).
    if (tier >= 5) {
        val c = Offset(cx, base - fh * 0.42f)
        for (i in 0 until 5) {
            val a = TWO_PI * (i / 5f + t * 0.8f)
            val x = c.x + cos(a) * w * 0.4f
            val y = c.y + sin(a) * w * 0.12f
            val front = sin(a) > 0
            drawCircle(core.copy(alpha = if (front) 0.95f else 0.4f), w * (if (front) 0.028f else 0.018f), Offset(x, y))
        }
    }
    // 60+: llamas pequeñas a los lados, desfasadas.
    if (tier >= 6) {
        for (side in listOf(-1f, 1f)) {
            val s2 = sin(ph * 1.3f + side) * 0.6f
            val sx = cx + side * fw * 0.55f
            drawPath(flame(sx, base, fw * 0.42f, fh * (0.5f + 0.04f * sin(ph * 2 + side)), s2), Brush.verticalGradient(listOf(core, color, dark), base - fh * 0.55f, base))
        }
    }

    // Llama principal: exterior, interior y núcleo.
    scale(1f / breathe, breathe, Offset(cx, base)) {
        drawPath(flame(cx, base, fw, fh, sway), Brush.verticalGradient(listOf(lerp(color, core, 0.25f), color, dark), base - fh, base))
        val inner = flame(cx, base, fw * 0.58f, fh * 0.62f, sway * 1.3f)
        drawPath(inner, Brush.verticalGradient(listOf(core, lerp(core, color, 0.4f)), base - fh * 0.62f, base))
        // Núcleo que late.
        val pulse = 0.85f + 0.15f * sin(ph * 2.5f)
        drawOval(
            Brush.radialGradient(listOf(Color.White.copy(alpha = 0.85f), core.copy(alpha = 0f)), Offset(cx, base - fh * 0.18f), fw * 0.22f * pulse),
            Offset(cx - fw * 0.2f * pulse, base - fh * 0.32f * pulse), Size(fw * 0.4f * pulse, fh * 0.3f * pulse),
        )
        // Brillo lateral fino para dar volumen.
        val shine = Path().apply {
            moveTo(cx - fw * 0.38f, base - fh * 0.2f)
            quadraticTo(cx - fw * 0.42f, base - fh * 0.5f, cx - fw * 0.2f, base - fh * 0.7f)
        }
        drawPath(shine, Color.White.copy(alpha = 0.35f), style = Stroke(w * 0.025f, cap = StrokeCap.Round))
    }

    // 14+: destellos de estrella que aparecen y desaparecen.
    if (tier >= 4) {
        val spots = listOf(Offset(-0.38f, -0.78f), Offset(0.4f, -0.62f), Offset(0.3f, -0.95f), Offset(-0.45f, -0.4f))
        spots.take(2 + (tier - 4).coerceAtMost(2)).forEachIndexed { i, s ->
            val tw = ((sin(ph * 1.7f + i * 2.1f) + 1f) / 2f)
            if (tw > 0.3f) sparkle(Offset(cx + s.x * w, base + s.y * fh), w * 0.07f * tw, core.copy(alpha = tw))
        }
    }
}

/** Estrella de cuatro puntas. */
private fun DrawScope.sparkle(c: Offset, r: Float, color: Color) {
    val p = Path().apply {
        moveTo(c.x, c.y - r)
        quadraticTo(c.x, c.y, c.x + r, c.y)
        quadraticTo(c.x, c.y, c.x, c.y + r)
        quadraticTo(c.x, c.y, c.x - r, c.y)
        quadraticTo(c.x, c.y, c.x, c.y - r)
        close()
    }
    drawPath(p, color, blendMode = BlendMode.SrcOver)
}
