package com.sunless404.keeptrack.ui.cosmetics

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import com.sunless404.keeptrack.ui.common.Avatar
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Dibujo de un marco: [t] va de 0 a 1 en bucle, [c] es el centro y [r] el radio de la foto. */
typealias FramePainter = DrawScope.(t: Float, c: Offset, r: Float) -> Unit

/** Marco de perfil: una capa detrás de la foto y otra delante. */
class FrameStyle(
    val id: String,
    val name: String,
    val collection: String,
    val back: FramePainter = { _, _, _ -> },
    val front: FramePainter = { _, _, _ -> },
    /** Color del brillo interior que tiñe el borde de la foto (transparente = sin brillo). */
    val accent: Color = Color.Transparent,
    /** Imágenes del marco (drawables); se pasan cargadas a [backImages] y [frontImages]. */
    val images: List<Int> = emptyList(),
    val backImages: ImagePainter? = null,
    val frontImages: ImagePainter? = null,
    /** Tamaño del lienzo respecto a la foto. */
    val scale: Float = 1.7f,
)

/** Foto de perfil con su marco animado. El marco ocupa un 70 % más que la foto (para alas y remates). */
@Composable
fun FramedAvatar(photo: ByteArray?, name: String, size: Dp, frame: FrameStyle?, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    if (frame == null) {
        Avatar(photo, name, size, modifier, onClick)
        return
    }
    val t by rememberInfiniteTransition(label = "marco").animateFloat(
        0f, 1f, infiniteRepeatable(tween(4000, easing = LinearEasing)), label = "t",
    )
    val images = frame.images.map { ImageBitmap.imageResource(it) }
    Box(modifier.size(size * frame.scale), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            frame.back(this, t, center, size.toPx() / 2)
            frame.backImages?.invoke(this, t, center, size.toPx() / 2, images)
        }
        Avatar(photo, name, size, onClick = onClick)
        Canvas(Modifier.fillMaxSize()) {
            val r = size.toPx() / 2
            // Brillo interior: el color del marco "entra" un poco en la foto, con un leve pulso.
            if (frame.accent.alpha > 0f) {
                val pulse = 0.75f + 0.25f * kotlin.math.sin(t * 2 * PI.toFloat())
                drawCircle(
                    Brush.radialGradient(
                        0.78f to Color.Transparent, 1f to frame.accent.copy(alpha = 0.55f * pulse),
                        center = center, radius = r,
                    ),
                    r, center,
                )
            }
            frame.front(this, t, center, r)
            frame.frontImages?.invoke(this, t, center, r, images)
        }
    }
}

private val PI_F = PI.toFloat()
private val UP = -PI_F / 2

private fun c(hex: Long) = Color(hex)

