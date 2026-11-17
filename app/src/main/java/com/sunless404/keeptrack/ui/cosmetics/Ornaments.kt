package com.sunless404.keeptrack.ui.cosmetics

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import kotlin.math.sin

/**
 * Piezas de ornamento para marcos "de juego": anillos con material, gemas,
 * alas y remates. Todas se dibujan con degradados para dar volumen.
 */

/** Anillo con bisel: material con luz arriba-izquierda, filo claro y sombra interior. */
internal fun DrawScope.bevelRing(c: Offset, r1: Float, r2: Float, base: Color, light: Color, dark: Color, rotation: Float = 0f) {
    val band = ringSegment(c, r1, r2, 0f, TAU * 0.9999f)
    rotate(rotation * 360f, c) {
        drawPath(band, Brush.sweepGradient(listOf(light, base, dark, base, light, base, dark, base, light), c))
    }
    drawCircle(lerp(light, Color.White, 0.4f).copy(alpha = 0.8f), r2, c, style = Stroke(1.2f))
    drawCircle(dark, r1, c, style = Stroke(1.6f))
    // Línea de brillo a media banda.
    drawCircle(light.copy(alpha = 0.35f), (r1 + r2) / 2, c, style = Stroke(0.8f))
}

/** Gema facetada (rombo) con destello que aparece cada cierto tiempo. */
internal fun DrawScope.gem(at: Offset, s: Float, color: Color, t: Float, tall: Float = 1.3f) {
    val top = Offset(at.x, at.y - s * tall)
    val bottom = Offset(at.x, at.y + s * tall)
    val left = Offset(at.x - s, at.y)
    val right = Offset(at.x + s, at.y)
    val mid = Offset(at.x, at.y - s * 0.15f)
    fun tri(a: Offset, b: Offset, cc: Offset, col: Color) = drawPath(Path().apply { moveTo(a.x, a.y); lineTo(b.x, b.y); lineTo(cc.x, cc.y); close() }, col)
    bloom(at, s * 2.4f, color, 0.35f)
    tri(top, left, mid, lerp(color, Color.White, 0.45f))
    tri(top, right, mid, lerp(color, Color.White, 0.15f))
    tri(left, bottom, mid, lerp(color, Color.Black, 0.25f))
    tri(right, bottom, mid, lerp(color, Color.Black, 0.5f))
    drawPath(Path().apply { moveTo(top.x, top.y); lineTo(right.x, right.y); lineTo(bottom.x, bottom.y); lineTo(left.x, left.y); close() }, lerp(color, Color.Black, 0.6f), style = Stroke(1f))
    val tw = ((sin(t * TAU * 2) + 1) / 2).let { if (it > 0.75f) (it - 0.75f) * 4 else 0f }
    if (tw > 0f) starGlint(Offset(at.x - s * 0.35f, at.y - s * 0.55f), s * 1.1f * tw, Color.White)
}

/** Destello de cuatro puntas. */
internal fun DrawScope.starGlint(c: Offset, r: Float, color: Color) {
    val p = Path().apply {
        moveTo(c.x, c.y - r)
        quadraticTo(c.x, c.y, c.x + r, c.y)
        quadraticTo(c.x, c.y, c.x, c.y + r)
        quadraticTo(c.x, c.y, c.x - r, c.y)
        quadraticTo(c.x, c.y, c.x, c.y - r)
        close()
    }
    drawPath(p, color, blendMode = BlendMode.Plus)
}

/** Ala de plumas (de ángel). [side] −1 izquierda, 1 derecha; [flap] 0..1. */
internal fun DrawScope.featherWing(root: Offset, side: Float, s: Float, base: Color, tip: Color, edge: Color, flap: Float) {
    val rows = listOf(Triple(7, 1.0f, 0f), Triple(6, 0.78f, 0.12f), Triple(5, 0.55f, 0.22f))
    rows.forEach { (n, len, off) ->
        for (i in 0 until n) {
            val a = -0.35f - flap * 0.15f + i * (1.25f / n) // de arriba hacia abajo
            val l = s * len * (1f - i * 0.06f)
            val dirX = side * kotlin.math.cos(a)
            val dirY = kotlin.math.sin(a)
            val start = Offset(root.x + side * s * off, root.y + s * off * 0.3f)
            val end = Offset(start.x + dirX * l, start.y + dirY * l)
            val nx = -dirY * l * 0.13f
            val ny = dirX * l * 0.13f
            val p = Path().apply {
                moveTo(start.x, start.y)
                quadraticTo(start.x + (end.x - start.x) * 0.5f + nx, start.y + (end.y - start.y) * 0.5f + ny, end.x, end.y)
                quadraticTo(start.x + (end.x - start.x) * 0.5f - nx, start.y + (end.y - start.y) * 0.5f - ny, start.x, start.y)
                close()
            }
            drawPath(p, Brush.linearGradient(listOf(base, tip), start, end))
            drawPath(p, edge, style = Stroke(0.8f))
        }
    }
}

/** Ala de murciélago con dedos y membrana festoneada. */
internal fun DrawScope.batWing(root: Offset, side: Float, s: Float, membrane: Color, bone: Color, flap: Float) {
    val lift = (flap - 0.5f) * s * 0.25f
    val fingers = listOf(Offset(0.55f, -0.75f), Offset(0.95f, -0.45f), Offset(1.05f, -0.05f), Offset(0.85f, 0.35f))
    val pts = fingers.map { Offset(root.x + side * it.x * s, root.y + it.y * s - lift * (1 - it.y)) }
    val elbow = Offset(root.x + side * s * 0.35f, root.y - s * 0.55f - lift)
    val p = Path().apply {
        moveTo(root.x, root.y)
        lineTo(elbow.x, elbow.y)
        lineTo(pts[0].x, pts[0].y)
        for (i in 1 until pts.size) {
            val prev = pts[i - 1]
            val cur = pts[i]
            val ctrl = Offset((prev.x + cur.x) / 2 - side * s * 0.12f, (prev.y + cur.y) / 2 + s * 0.04f)
            quadraticTo(ctrl.x, ctrl.y, cur.x, cur.y)
        }
        quadraticTo(root.x + side * s * 0.4f, root.y + s * 0.2f, root.x, root.y + s * 0.15f)
        close()
    }
    drawPath(p, Brush.radialGradient(listOf(lerp(membrane, Color.White, 0.12f), membrane), elbow, s))
    pts.forEach { drawLine(bone, elbow, it, s * 0.035f, StrokeCap.Round) }
    drawLine(bone, root, elbow, s * 0.05f, StrokeCap.Round)
    drawPath(p, bone.copy(alpha = 0.7f), style = Stroke(1f))
}

/** Pincho/remate triangular apuntando hacia fuera en el ángulo [a]. */
internal fun DrawScope.spike(c: Offset, r: Float, a: Float, len: Float, width: Float, fill: Brush, edge: Color) {
    val p = Path().apply {
        val b1 = polar(c, r, a - width)
        val b2 = polar(c, r, a + width)
        val tip = polar(c, r + len, a)
        moveTo(b1.x, b1.y); lineTo(tip.x, tip.y); lineTo(b2.x, b2.y); close()
    }
    drawPath(p, fill)
    drawPath(p, edge, style = Stroke(1f))
}
