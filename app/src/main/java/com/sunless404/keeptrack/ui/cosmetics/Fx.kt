package com.sunless404.keeptrack.ui.cosmetics

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Efectos de más calidad para los marcos: neón, llamas en capas, humo, chispas y rayos. */

/** Trazo de neón: halo ancho + halo medio + color + núcleo blanco. */
internal fun DrawScope.neon(path: Path, color: Color, width: Float, core: Color = Color.White, intensity: Float = 1f) {
    drawPath(path, color.copy(alpha = 0.10f * intensity), style = Stroke(width * 6, cap = StrokeCap.Round, join = StrokeJoin.Round), blendMode = BlendMode.Plus)
    drawPath(path, color.copy(alpha = 0.25f * intensity), style = Stroke(width * 3, cap = StrokeCap.Round, join = StrokeJoin.Round), blendMode = BlendMode.Plus)
    drawPath(path, color.copy(alpha = intensity.coerceAtMost(1f)), style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(path, core.copy(alpha = 0.8f * intensity.coerceAtMost(1f)), style = Stroke(width * 0.35f, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

internal fun DrawScope.neonRing(c: Offset, r: Float, color: Color, width: Float, core: Color = Color.White, intensity: Float = 1f) =
    neon(Path().apply { addOval(Rect(c, r)) }, color, width, core, intensity)

internal fun DrawScope.neonArc(c: Offset, r: Float, start: Float, sweep: Float, color: Color, width: Float, core: Color = Color.White) =
    neon(Path().apply { arcTo(Rect(c, r), start, sweep, true) }, color, width, core)

internal fun DrawScope.bloom(c: Offset, r: Float, color: Color, alpha: Float = 0.6f) {
    drawCircle(Brush.radialGradient(listOf(color.copy(alpha = alpha), color.copy(alpha = alpha * 0.35f), Color.Transparent), c, r), r, c, blendMode = BlendMode.Plus)
}

/** Lengua de fuego que sale del círculo en el ángulo [a]. */
private fun tongue(c: Offset, r: Float, a: Float, len: Float, width: Float, sway: Float): Path = Path().apply {
    val b1 = polar(c, r, a - width)
    val b2 = polar(c, r, a + width)
    val tip = polar(c, r + len, a + sway)
    val m1 = polar(c, r + len * 0.5f, a - width * 1.1f + sway * 0.4f)
    val m2 = polar(c, r + len * 0.55f, a + width * 0.9f + sway * 0.6f)
    moveTo(b1.x, b1.y)
    quadraticTo(m1.x, m1.y, tip.x, tip.y)
    quadraticTo(m2.x, m2.y, b2.x, b2.y)
    close()
}

/**
 * Fuego en tres capas (exterior, medio, núcleo), con lenguas de distinto tamaño que se
 * mueven a ritmos distintos. [additive] = las capas suman luz (fuego brillante); false = fuego oscuro.
 */
internal fun DrawScope.fire(
    c: Offset, r: Float, t: Float, outer: Color, mid: Color, core: Color,
    count: Int = 28, len: Float = r * 0.45f, speed: Float = 1.6f, additive: Boolean = true,
    from: Float = 0f, to: Float = TAU, width: Float = 0.13f,
) {
    val mode = if (additive) BlendMode.Plus else BlendMode.SrcOver
    val layers = listOf(Triple(outer, 1f, 1f), Triple(mid, 0.72f, 0.8f), Triple(core, 0.42f, 0.6f))
    layers.forEachIndexed { li, (col, lf, wf) ->
        for (i in 0 until count) {
            val a = from + (to - from) * (i + 0.5f + li * 0.3f) / count
            val h = len * lf * (0.45f + 0.55f * wave(t, i * 3 + li, speed))
            val sway = (wave(t, i + 11 + li, speed * 1.4f) - 0.5f) * 0.3f
            drawPath(
                tongue(c, r * 0.98f, a, h, width * wf, sway),
                Brush.radialGradient(listOf(col, col.copy(alpha = col.alpha * 0.6f), col.copy(alpha = 0f)), c, r + h),
                blendMode = mode,
            )
        }
    }
}

/** Humo: bolas translúcidas que salen hacia fuera y se desvanecen. */
internal fun DrawScope.smoke(c: Offset, r: Float, t: Float, color: Color, n: Int = 18, reach: Float = r * 0.5f, speed: Float = 0.5f) {
    for (i in 0 until n) {
        val ph = frac(t * speed + i.toFloat() / n)
        val a = TAU * ((i * 0.618f) % 1f) + sin(ph * 3 + i) * 0.2f
        val p = polar(c, r + reach * ph, a)
        val size = r * (0.12f + 0.22f * ph)
        drawCircle(Brush.radialGradient(listOf(color.copy(alpha = color.alpha * (1 - ph)), Color.Transparent), p, size), size, p)
    }
}

/** Chispas con estela que suben (o giran si [swirl]). */
internal fun DrawScope.sparks(c: Offset, r: Float, t: Float, color: Color, n: Int = 14, speed: Float = 0.8f, swirl: Boolean = false, size: Float = 2.2f) {
    for (i in 0 until n) {
        val ph = frac(t * speed + i.toFloat() / n)
        val head: Offset
        val tail: Offset
        if (swirl) {
            val a = TAU * (i.toFloat() / n) + ph * TAU * 0.5f
            val rr = r * (1.0f + 0.45f * ph)
            head = polar(c, rr, a)
            tail = polar(c, rr * 0.96f, a - 0.12f)
        } else {
            val x = c.x + r * 1.2f * (((i * 61) % 100) / 50f - 1f)
            val y = c.y + r * 0.9f - r * 2.2f * ph
            head = Offset(x + sin(ph * 6 + i) * 4f, y)
            tail = Offset(head.x, head.y + r * 0.12f)
        }
        val alpha = (1 - ph) * color.alpha
        drawLine(color.copy(alpha = alpha * 0.5f), tail, head, size, StrokeCap.Round, blendMode = BlendMode.Plus)
        drawCircle(color.copy(alpha = alpha), size * 0.9f, head, blendMode = BlendMode.Plus)
    }
}

/** Rayo con ramas, que cambia de forma con [seed]. */
internal fun DrawScope.lightning(from: Offset, to: Offset, seed: Int, color: Color, width: Float, branches: Int = 2, depth: Int = 0) {
    val dx = to.x - from.x
    val dy = to.y - from.y
    val len = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
    val nx = -dy / len
    val ny = dx / len
    val seg = 8
    val pts = (0..seg).map { i ->
        val f = i.toFloat() / seg
        val j = if (i == 0 || i == seg) 0f else (((seed * 73 + i * 37 + depth * 11) % 17) / 8.5f - 1f) * len * 0.11f
        Offset(from.x + dx * f + nx * j, from.y + dy * f + ny * j)
    }
    val p = Path().apply { moveTo(pts[0].x, pts[0].y); pts.drop(1).forEach { lineTo(it.x, it.y) } }
    neon(p, color, width)
    if (depth < 1) repeat(branches) { b ->
        val start = pts[2 + (seed + b * 3) % (seg - 3)]
        val ang = kotlin.math.atan2(dy, dx) + if (b % 2 == 0) 0.7f else -0.7f
        val end = Offset(start.x + cos(ang) * len * 0.35f, start.y + sin(ang) * len * 0.35f)
        lightning(start, end, seed + b * 5 + 1, color, width * 0.6f, 0, depth + 1)
    }
}

/** Relleno metálico: degradado con un brillo que recorre la pieza. */
internal fun DrawScope.metal(path: Path, base: Color, light: Color, dark: Color, shine: Float, bounds: Rect) {
    drawPath(
        path,
        Brush.linearGradient(
            0f to dark, 0.35f to base, (shine - 0.08f).coerceIn(0.36f, 0.9f) to base,
            shine.coerceIn(0.37f, 0.95f) to light, (shine + 0.08f).coerceIn(0.38f, 1f) to base, 1f to dark,
            start = Offset(bounds.left, bounds.top), end = Offset(bounds.right, bounds.bottom),
        ),
    )
    drawPath(path, dark.copy(alpha = 0.8f), style = Stroke(1.2f))
}

/** Segmento de anillo (para placas, cristales, etc.). */
internal fun ringSegment(c: Offset, r1: Float, r2: Float, a0: Float, a1: Float): Path = Path().apply {
    val deg0 = a0 * 180f / kotlin.math.PI.toFloat()
    val sweep = (a1 - a0) * 180f / kotlin.math.PI.toFloat()
    arcTo(Rect(c, r2), deg0, sweep, true)
    arcTo(Rect(c, r1), deg0 + sweep, -sweep, false)
    close()
}
