package com.sunless404.keeptrack.ui.cosmetics

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
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
    private val EMBER = Color(0xFFFF3B2E)

    // Proporciones medidas en las imágenes.
    private const val RING_HOLE = 0.5553f // radio del hueco / medio ancho del anillo

    val alucardNoble = FrameStyle(
        id = "alucard_noble", name = "Conde carmesí", collection = "Alucard",
        accent = BLOOD, scale = 2.3f,
        images = listOf(
            R.drawable.frame_alucard_ring, R.drawable.frame_alucard_ring_runes,
            R.drawable.frame_alucard_crest, R.drawable.frame_alucard_wing, R.drawable.frame_alucard_cross,
        ),
        backImages = { t, c, r, img ->
            val ph = t * TAU
            // Aura oscura que respira.
            bloom(c, r * 2.0f, Color(0xFF6A0010), 0.35f + 0.1f * sin(ph))
            // Alas: aletean dos veces por ciclo, la derecha un pelín desfasada.
            val flap = sin(ph * 2)
            val wingW = r * 1.3f
            piece(img[3], 0.88f, 0.45f, Offset(c.x - r * 1.35f, c.y - r * 0.3f), wingW, degrees = 18f + 10f * flap)
            piece(img[3], 0.88f, 0.45f, Offset(c.x + r * 1.35f, c.y - r * 0.3f), wingW, degrees = -(18f + 10f * sin(ph * 2 - 0.25f)), flip = true)
            // Brasas que suben por detrás del anillo.
            embers(Offset(c.x, c.y + r * 0.6f), r * 1.7f, t, 16, EMBER.copy(alpha = 0.9f), size = r * 0.03f, height = r * 1.6f)
        },
        frontImages = { t, c, r, img ->
            val ph = t * TAU
            val ringW = 2 * r * 0.98f / RING_HOLE
            piece(img[0], 0.5f, 0.5f, c, ringW)
            // Runas: laten y una onda de brillo recorre el anillo.
            val pulse = 0.55f + 0.45f * sin(ph * 2)
            piece(img[1], 0.5f, 0.5f, c, ringW, alpha = pulse, blend = BlendMode.Plus)
            piece(img[1], 0.5f, 0.5f, c, ringW * 1.01f, degrees = t * 360f, alpha = 0.25f * pulse, blend = BlendMode.Plus)
            // Cruz colgante que se balancea.
            val swing = 7f * sin(ph)
            val pivot = Offset(c.x, c.y + r * 1.32f)
            val crossW = r * 0.55f
            piece(img[4], 0.5f, 0.03f, pivot, crossW, degrees = swing)
            // Gota de sangre que cae de la punta una vez por ciclo.
            val crossH = crossW * img[4].height / img[4].width
            val tipLocal = crossH * 0.95f
            val rad = Math.toRadians(swing.toDouble()).toFloat()
            val tip = Offset(pivot.x - sin(rad) * tipLocal, pivot.y + kotlin.math.cos(rad) * tipLocal)
            val fall = frac(t * 1f)
            if (fall < 0.6f) {
                val k = fall / 0.6f
                val drop = Offset(tip.x, tip.y + r * 0.9f * k * k)
                drawCircle(BLOOD.copy(alpha = 1f - k), r * 0.035f * (1f - 0.3f * k), drop)
            }
            // Remate con el ojo, flotando un poco; el ojo late.
            val bob = r * 0.04f * sin(ph)
            val eye = Offset(c.x, c.y - r * 1.5f + bob)
            piece(img[2], 0.5f, 0.48f, eye, r * 2.0f)
            bloom(eye, r * (0.32f + 0.06f * sin(ph * 3)), BLOOD, 0.45f + 0.25f * sin(ph * 3))
        },
    )
}
