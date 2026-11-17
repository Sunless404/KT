package com.sunless404.keeptrack.ui.cosmetics

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** Utilidades de dibujo para los marcos animados. Ángulos en radianes; 0 = derecha, −π/2 = arriba. */
internal const val TAU = (2 * PI).toFloat()

internal fun polar(c: Offset, r: Float, a: Float) = Offset(c.x + r * cos(a), c.y + r * sin(a))

/** Ruido suave y repetible (sin aleatoriedad real) para que las llamas parpadeen. */
internal fun wave(t: Float, i: Int, speed: Float = 1f): Float =
    (sin(t * TAU * speed + i * 1.7f) * 0.5f + sin(t * TAU * speed * 2.3f + i * 0.9f) * 0.3f + 0.8f) / 1.6f

internal fun DrawScope.glow(c: Offset, r: Float, color: Color, alpha: Float = 0.5f) {
    drawCircle(Brush.radialGradient(listOf(color.copy(alpha = alpha), Color.Transparent), c, r), r, c)
}

internal fun DrawScope.ring(c: Offset, r: Float, color: Color, width: Float) {
    drawCircle(color, r, c, style = Stroke(width))
}

internal fun DrawScope.sweepRing(c: Offset, r: Float, colors: List<Color>, width: Float, rotation: Float = 0f) {
    rotate(rotation * 360f, c) { drawCircle(Brush.sweepGradient(colors, c), r, c, style = Stroke(width)) }
}

/** Corona de llamas alrededor del círculo. */
internal fun DrawScope.flameRing(
    c: Offset, r: Float, t: Float, colors: List<Color>, count: Int = 22, length: Float = r * 0.32f,
    width: Float = 0.16f, speed: Float = 1.5f, from: Float = 0f, to: Float = TAU,
) {
    for (i in 0 until count) {
        val a = from + (to - from) * (i + 0.5f) / count
        val h = length * (0.55f + 0.45f * wave(t, i, speed))
        val sway = (wave(t, i + 7, speed * 1.3f) - 0.5f) * 0.25f
        val p = Path().apply {
            moveTo(polar(c, r, a - width).x, polar(c, r, a - width).y)
            val tip = polar(c, r + h, a + sway)
            val c1 = polar(c, r + h * 0.55f, a - width * 0.9f)
            val c2 = polar(c, r + h * 0.55f, a + width * 0.9f)
            quadraticTo(c1.x, c1.y, tip.x, tip.y)
            quadraticTo(c2.x, c2.y, polar(c, r, a + width).x, polar(c, r, a + width).y)
            close()
        }
        drawPath(p, Brush.radialGradient(colors, c, r + length))
    }
}

/** Partículas que giran alrededor. */
internal fun DrawScope.orbit(c: Offset, r: Float, t: Float, n: Int, color: Color, size: Float, speed: Float = 1f, wobble: Float = 0f) {
    for (i in 0 until n) {
        val a = TAU * (i.toFloat() / n + t * speed)
        val rr = r + sin(t * TAU * 2 + i) * wobble
        drawCircle(color, size * (0.6f + 0.4f * wave(t, i)), polar(c, rr, a))
    }
}

/** Chispas que suben y se apagan. */
internal fun DrawScope.embers(c: Offset, r: Float, t: Float, n: Int, color: Color, size: Float = 3f, height: Float = r * 0.9f) {
    for (i in 0 until n) {
        val phase = (t * 1.3f + i.toFloat() / n) % 1f
        val x = c.x + r * (((i * 53) % 100) / 50f - 1f) * 0.9f + sin((phase + i) * 9f) * 4f
        val y = c.y + r * 0.7f - height * 1.6f * phase
        drawCircle(color.copy(alpha = (1 - phase) * color.alpha), size * (1 - phase * 0.6f), Offset(x, y))
    }
}

/** Murciélago: [flap] de 0 a 1 abre y cierra las alas. */
internal fun DrawScope.bat(at: Offset, s: Float, flap: Float, color: Color, angle: Float = 0f) {
    val lift = (flap - 0.5f) * s * 0.6f
    val p = Path().apply {
        moveTo(at.x, at.y - s * 0.15f)
        lineTo(at.x - s * 0.18f, at.y - s * 0.05f)
        quadraticTo(at.x - s * 0.6f, at.y - s * 0.35f - lift, at.x - s, at.y - lift)
        quadraticTo(at.x - s * 0.8f, at.y + s * 0.05f, at.x - s * 0.7f, at.y + s * 0.25f)
        quadraticTo(at.x - s * 0.55f, at.y + s * 0.05f, at.x - s * 0.4f, at.y + s * 0.25f)
        quadraticTo(at.x - s * 0.25f, at.y + s * 0.05f, at.x, at.y + s * 0.3f)
        quadraticTo(at.x + s * 0.25f, at.y + s * 0.05f, at.x + s * 0.4f, at.y + s * 0.25f)
        quadraticTo(at.x + s * 0.55f, at.y + s * 0.05f, at.x + s * 0.7f, at.y + s * 0.25f)
        quadraticTo(at.x + s * 0.8f, at.y + s * 0.05f, at.x + s, at.y - lift)
        quadraticTo(at.x + s * 0.6f, at.y - s * 0.35f - lift, at.x + s * 0.18f, at.y - s * 0.05f)
        close()
    }
    rotate(angle, at) { drawPath(p, color) }
}

/** Cuervo de perfil, con aleteo. */
internal fun DrawScope.crow(at: Offset, s: Float, flap: Float, color: Color, angle: Float = 0f) {
    val w = (flap - 0.5f) * s * 0.9f
    val body = Path().apply {
        moveTo(at.x - s * 0.6f, at.y)
        quadraticTo(at.x - s * 0.1f, at.y - s * 0.22f, at.x + s * 0.45f, at.y - s * 0.08f)
        lineTo(at.x + s * 0.7f, at.y - s * 0.02f)
        lineTo(at.x + s * 0.45f, at.y + s * 0.06f)
        quadraticTo(at.x, at.y + s * 0.2f, at.x - s * 0.6f, at.y)
        close()
        moveTo(at.x - s * 0.05f, at.y - s * 0.05f)
        quadraticTo(at.x - s * 0.3f, at.y - s * 0.6f - w, at.x - s * 0.55f, at.y - s * 0.75f - w)
        quadraticTo(at.x - s * 0.2f, at.y - s * 0.25f, at.x + s * 0.2f, at.y - s * 0.08f)
        close()
        moveTo(at.x - s * 0.6f, at.y)
        lineTo(at.x - s * 0.85f, at.y - s * 0.1f)
        lineTo(at.x - s * 0.8f, at.y + s * 0.08f)
        close()
    }
    rotate(angle, at) { drawPath(body, color) }
}

/** Rayo quebrado entre dos puntos; [seed] cambia la forma. */
internal fun DrawScope.bolt(from: Offset, to: Offset, seed: Int, color: Color, width: Float, segments: Int = 7) {
    val p = Path().apply {
        moveTo(from.x, from.y)
        val dx = to.x - from.x
        val dy = to.y - from.y
        val len = kotlin.math.sqrt(dx * dx + dy * dy)
        val nx = -dy / len
        val ny = dx / len
        for (i in 1 until segments) {
            val f = i.toFloat() / segments
            val off = (((seed * 31 + i * 17) % 13) / 6.5f - 1f) * len * 0.12f
            lineTo(from.x + dx * f + nx * off, from.y + dy * f + ny * off)
        }
        lineTo(to.x, to.y)
    }
    drawPath(p, color.copy(alpha = 0.35f), style = Stroke(width * 3, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(p, color, style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(p, Color.White.copy(alpha = 0.8f), style = Stroke(width * 0.4f, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

/** Magatama (coma) apuntando en la dirección [a]. */
internal fun DrawScope.magatama(at: Offset, s: Float, a: Float, color: Color) {
    drawCircle(color, s * 0.5f, at)
    val tail = Path().apply {
        moveTo(polar(at, s * 0.5f, a - 1.4f).x, polar(at, s * 0.5f, a - 1.4f).y)
        val tip = polar(at, s * 1.3f, a + 0.9f)
        val ctrl = polar(at, s * 1.2f, a - 0.4f)
        quadraticTo(ctrl.x, ctrl.y, tip.x, tip.y)
        val ctrl2 = polar(at, s * 0.7f, a + 0.6f)
        quadraticTo(ctrl2.x, ctrl2.y, polar(at, s * 0.5f, a + 0.4f).x, polar(at, s * 0.5f, a + 0.4f).y)
        close()
    }
    drawPath(tail, color)
}

/** Pluma. */
internal fun DrawScope.feather(at: Offset, s: Float, angle: Float, color: Color) {
    val p = Path().apply {
        moveTo(at.x, at.y - s)
        quadraticTo(at.x + s * 0.35f, at.y - s * 0.2f, at.x, at.y + s)
        quadraticTo(at.x - s * 0.35f, at.y - s * 0.2f, at.x, at.y - s)
        close()
    }
    rotate(angle, at) {
        drawPath(p, color)
        drawLine(color.copy(alpha = 0.6f), Offset(at.x, at.y - s), Offset(at.x, at.y + s * 1.3f), 1.5f)
    }
}

/** Cruz (invertida si [inverted]). */
internal fun DrawScope.cross(at: Offset, s: Float, color: Color, inverted: Boolean = false, width: Float = s * 0.18f) {
    val bar = if (inverted) at.y + s * 0.35f else at.y - s * 0.35f
    drawLine(color, Offset(at.x, at.y - s), Offset(at.x, at.y + s), width, StrokeCap.Round)
    drawLine(color, Offset(at.x - s * 0.55f, bar), Offset(at.x + s * 0.55f, bar), width, StrokeCap.Round)
}

/** Araña con 8 patas. */
internal fun DrawScope.spider(at: Offset, s: Float, color: Color, legPhase: Float = 0f) {
    drawCircle(color, s * 0.32f, Offset(at.x, at.y + s * 0.2f))
    drawCircle(color, s * 0.2f, Offset(at.x, at.y - s * 0.2f))
    for (side in listOf(-1f, 1f)) for (k in 0 until 4) {
        val y0 = at.y - s * 0.1f + k * s * 0.13f
        val bend = sin(legPhase * TAU + k) * s * 0.06f
        val knee = Offset(at.x + side * s * 0.55f, y0 - s * 0.25f + bend)
        val foot = Offset(at.x + side * s * 0.85f, y0 + s * (k - 1.5f) * 0.18f)
        drawLine(color, Offset(at.x, y0), knee, s * 0.06f, StrokeCap.Round)
        drawLine(color, knee, foot, s * 0.06f, StrokeCap.Round)
    }
}

/** Trébol de [leaves] hojas. */
internal fun DrawScope.clover(at: Offset, s: Float, leaves: Int, color: Color, rotation: Float = 0f) {
    for (i in 0 until leaves) {
        val a = rotation + TAU * i / leaves - PI.toFloat() / 2
        val p = Path().apply {
            moveTo(at.x, at.y)
            val l = polar(at, s, a - 0.45f)
            val rr = polar(at, s, a + 0.45f)
            val tip = polar(at, s * 0.75f, a)
            cubicTo(l.x, l.y, polar(at, s * 1.15f, a - 0.15f).x, polar(at, s * 1.15f, a - 0.15f).y, tip.x, tip.y)
            cubicTo(polar(at, s * 1.15f, a + 0.15f).x, polar(at, s * 1.15f, a + 0.15f).y, rr.x, rr.y, at.x, at.y)
            close()
        }
        drawPath(p, color)
    }
}

/** Silueta de murciélago del emblema (alas extendidas y quietas). */
internal fun DrawScope.batEmblem(at: Offset, s: Float, color: Color) = bat(at, s, 0.5f, color)

/** Espada simple vertical, rotada [angle] grados. */
internal fun DrawScope.sword(at: Offset, s: Float, angle: Float, color: Color) {
    rotate(angle, at) {
        val blade = Path().apply {
            moveTo(at.x, at.y - s)
            lineTo(at.x + s * 0.09f, at.y + s * 0.35f)
            lineTo(at.x - s * 0.09f, at.y + s * 0.35f)
            close()
        }
        drawPath(blade, color)
        drawLine(color, Offset(at.x - s * 0.3f, at.y + s * 0.38f), Offset(at.x + s * 0.3f, at.y + s * 0.38f), s * 0.08f, StrokeCap.Round)
        drawLine(color, Offset(at.x, at.y + s * 0.38f), Offset(at.x, at.y + s * 0.65f), s * 0.1f, StrokeCap.Round)
    }
}

internal fun DrawScope.rect(at: Offset, w: Float, h: Float, color: Color) =
    drawRect(color, Offset(at.x - w / 2, at.y - h / 2), Size(w, h))

internal fun frac(x: Float) = x - kotlin.math.floor(x)
internal fun tri(x: Float) = 1f - abs(frac(x) * 2f - 1f)
