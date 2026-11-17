package com.sunless404.keeptrack.ui.cosmetics

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.sunless404.keeptrack.R
import kotlin.math.roundToInt
import kotlin.math.sin

/** Dibujo de un marco hecho con imágenes: recibe las imágenes de [FrameStyle.images] ya cargadas. */
typealias ImagePainter = DrawScope.(t: Float, c: Offset, r: Float, img: List<ImageBitmap>) -> Unit

/**
 * Dibuja una pieza de forma que su punto de anclaje ([ax], [ay], en fracciones de la imagen)
 * caiga en [at], con ancho [width], girada [degrees] alrededor del anclaje y, si [flip], en espejo.
 */
internal fun DrawScope.piece(
    img: ImageBitmap, ax: Float, ay: Float, at: Offset, width: Float,
    degrees: Float = 0f, flip: Boolean = false, alpha: Float = 1f, blend: BlendMode = BlendMode.SrcOver,
) {
    val w = width.roundToInt()
    val h = (width * img.height / img.width).roundToInt()
    translate(at.x, at.y) {
        rotate(degrees, Offset.Zero) {
            scale(if (flip) -1f else 1f, 1f, Offset.Zero) {
                drawImage(
                    img, IntOffset.Zero, IntSize(img.width, img.height),
                    IntOffset((-ax * w).roundToInt(), (-ay * h).roundToInt()), IntSize(w, h),
                    alpha = alpha, blendMode = blend, filterQuality = FilterQuality.High,
                )
            }
        }
    }
}

/** Marcos hechos con piezas ilustradas y animadas por código. */
object ImageFrames {
    private val BLOOD = Color(0xFFE0182A)
    private val BLOOD_DARK = Color(0xFF5A0610)

    /** Un anillo ilustrado que encaja con la foto: [hole] = radio del hueco / medio ancho de la imagen. */
    private fun DrawScope.ringImage(img: ImageBitmap, c: Offset, r: Float, hole: Float, grow: Float = 1f, alpha: Float = 1f, blend: BlendMode = BlendMode.SrcOver, degrees: Float = 0f) =
        piece(img, 0.5f, 0.5f, c, 2 * r * 0.98f / hole * grow, degrees = degrees, alpha = alpha, blend = blend)

    /** Sello Cromwell: dos círculos con marcas y una estrella de cinco puntas, girando. */
    private fun DrawScope.cromwell(c: Offset, r1: Float, r2: Float, rot: Float, color: Color, width: Float) {
        rotate(rot, c) {
            drawCircle(color, r1, c, style = Stroke(width))
            drawCircle(color, r2, c, style = Stroke(width))
            for (i in 0 until 36) {
                val a = TAU * i / 36
                val len = if (i % 3 == 0) (r2 - r1) * 0.7f else (r2 - r1) * 0.35f
                drawLine(color, polar(c, r1, a), polar(c, r1 + len, a), width * 0.8f)
            }
            val star = Path()
            for (k in 0..5) {
                val pt = polar(c, r2 * 1.06f, -PI_F / 2 + TAU * (k * 2 % 5) / 5)
                if (k == 0) star.moveTo(pt.x, pt.y) else star.lineTo(pt.x, pt.y)
            }
            drawPath(star, color, style = Stroke(width))
        }
    }

    /** Alucard: anillo gótico de runas con el sello Cromwell girando detrás. */
    val alucardCromwell = FrameStyle(
        id = "alucard_cromwell", name = "Sello Cromwell", collection = "Alucard",
        accent = BLOOD, scale = 2.4f,
        images = listOf(R.drawable.frame_alucard_ring, R.drawable.frame_alucard_ring_runes),
        backImages = { t, c, r, _ ->
            val ph = t * TAU
            val pulse = 0.5f + 0.5f * sin(ph * 2)
            bloom(c, r * 2.1f, BLOOD_DARK, 0.45f + 0.15f * pulse)
            // El sello gira despacio; una copia difusa debajo hace de resplandor.
            val rot = t * 360f / 4
            cromwell(c, r * 1.72f, r * 1.9f, rot, BLOOD.copy(alpha = 0.25f + 0.2f * pulse), r * 0.07f)
            cromwell(c, r * 1.72f, r * 1.9f, rot, BLOOD.copy(alpha = 0.85f), r * 0.022f)
            // Un segundo círculo de marcas, más fino, en sentido contrario.
            rotate(-t * 360f / 2, c) {
                for (i in 0 until 60) {
                    val a = TAU * i / 60
                    drawLine(BLOOD.copy(alpha = 0.5f), polar(c, r * 1.98f, a), polar(c, r * (if (i % 5 == 0) 2.1f else 2.03f), a), r * 0.012f)
                }
            }
        },
        frontImages = { t, c, r, img ->
            val pulse = 0.5f + 0.5f * sin(t * TAU * 2)
            ringImage(img[0], c, r, ALUCARD_HOLE)
            ringImage(img[1], c, r, ALUCARD_HOLE, alpha = 0.4f + 0.6f * pulse, blend = BlendMode.Plus)
            ringImage(img[1], c, r, ALUCARD_HOLE, grow = 1.012f, alpha = 0.35f * pulse, blend = BlendMode.Plus)
        },
    )

    private val SPECTRAL_BLUE = Color(0xFF3FA9FF)
    private val SPECTRAL_CYAN = Color(0xFFB8ECFF)
    private val SPECTRAL_PURPLE = Color(0xFF9A4DFF)
    private val SPECTRAL_LILAC = Color(0xFFE2C4FF)

    /** Susanoo azul: armadura de placas con llamas que suben y un latido de energía. */
    val susanooBlue = FrameStyle(
        id = "susanoo_azul", name = "Susanoo azul", collection = "Susanoo",
        accent = SPECTRAL_BLUE, scale = 2.4f,
        images = listOf(R.drawable.frame_susanoo_blue, R.drawable.frame_susanoo_blue_glow),
        backImages = { t, c, r, _ ->
            bloom(c, r * 2.2f, SPECTRAL_BLUE, 0.3f + 0.1f * sin(t * TAU))
            fire(c, r * 1.85f, t, SPECTRAL_BLUE.copy(alpha = 0.45f), SPECTRAL_BLUE.copy(alpha = 0.35f), SPECTRAL_CYAN.copy(alpha = 0.3f),
                count = 22, len = r * 0.4f, speed = 1.2f)
        },
        frontImages = { t, c, r, img ->
            val ph = t * TAU
            val breathe = 1f + 0.012f * sin(ph * 2)
            ringImage(img[0], c, r, SUSANOO_BLUE_HOLE, grow = breathe)
            ringImage(img[1], c, r, SUSANOO_BLUE_HOLE, grow = breathe, alpha = 0.25f + 0.35f * (0.5f + 0.5f * sin(ph * 2)), blend = BlendMode.Plus)
            embers(Offset(c.x, c.y + r * 0.5f), r * 1.9f, t, 16, SPECTRAL_CYAN.copy(alpha = 0.9f), size = r * 0.022f, height = r * 1.7f)
        },
    )

    /** Susanoo morado: costillas espectrales que respiran y llamas violetas. */
    val susanooPurple = FrameStyle(
        id = "susanoo_morado", name = "Susanoo morado", collection = "Susanoo",
        accent = SPECTRAL_PURPLE, scale = 2.3f,
        images = listOf(R.drawable.frame_susanoo_purple, R.drawable.frame_susanoo_purple_glow),
        backImages = { t, c, r, _ ->
            bloom(c, r * 2.1f, SPECTRAL_PURPLE, 0.3f + 0.12f * sin(t * TAU))
            fire(c, r * 1.6f, t, SPECTRAL_PURPLE.copy(alpha = 0.4f), SPECTRAL_PURPLE.copy(alpha = 0.3f), SPECTRAL_LILAC.copy(alpha = 0.25f),
                count = 20, len = r * 0.45f, speed = 1.3f, from = PI_F * 1.05f, to = PI_F * 1.95f)
        },
        frontImages = { t, c, r, img ->
            val ph = t * TAU
            // Respiración: el anillo se ensancha un poco a lo ancho, como un tórax.
            val b = 0.5f + 0.5f * sin(ph * 1.5f)
            scale(1f + 0.02f * b, 1f + 0.008f * b, c) {
                ringImage(img[0], c, r, SUSANOO_PURPLE_HOLE)
                ringImage(img[1], c, r, SUSANOO_PURPLE_HOLE, alpha = 0.2f + 0.45f * b, blend = BlendMode.Plus)
            }
            embers(Offset(c.x, c.y + r * 0.5f), r * 1.7f, t, 16, SPECTRAL_LILAC.copy(alpha = 0.9f), size = r * 0.022f, height = r * 1.6f)
        },
    )

    // Proporciones medidas en las imágenes.
    private const val ALUCARD_HOLE = 0.5553f
    private const val SUSANOO_BLUE_HOLE = 0.4474f
    private const val SUSANOO_PURPLE_HOLE = 0.4316f
    private val PI_F = Math.PI.toFloat()

    val all = listOf(alucardCromwell, susanooBlue, susanooPurple)
}
