package com.sunless404.keeptrack.ui.common

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sunless404.keeptrack.App
import com.sunless404.keeptrack.data.RewardEvent
import com.sunless404.keeptrack.ui.theme.Brand
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Vibraciones de la app: un toque suave al marcar y un patrón de "éxito" al celebrar. */
object Haptics {
    fun tick(view: View) {
        view.performHapticFeedback(
            if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.VIRTUAL_KEY
        )
    }

    fun success(context: Context) {
        val v: Vibrator = if (Build.VERSION.SDK_INT >= 31) {
            context.getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }
        if (!v.hasVibrator()) return
        v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 40, 70, 40, 70, 120), intArrayOf(0, 120, 0, 180, 0, 255), -1))
    }
}

/** Toque suave para usar en los ticks: `val tick = rememberTick(); onCheckedChange = { tick(); … }`. */
@Composable
fun rememberTick(): () -> Unit {
    val view = LocalView.current
    return remember(view) { { Haptics.tick(view) } }
}

/**
 * Capa por encima de la app: un aviso pequeño "+5 XP +2" al ganar algo y una
 * celebración con confeti al completar el día, subir una racha, un logro o de nivel.
 */
@Composable
fun RewardOverlay() {
    val app = LocalContext.current.applicationContext as App
    var small by remember { mutableStateOf<RewardEvent?>(null) }
    var big by remember { mutableStateOf<RewardEvent?>(null) }
    val ctx = LocalContext.current

    LaunchedEffect(Unit) {
        app.repo.wallet.events.collect { e ->
            if (e.celebrate) {
                big = e
                Haptics.success(ctx)
            } else {
                small = e
            }
        }
    }
    LaunchedEffect(small) {
        if (small != null) {
            delay(1600)
            small = null
        }
    }
    LaunchedEffect(big) {
        if (big != null) {
            delay(2800)
            big = null
        }
    }

    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = small != null,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter).windowInsetsPadding(WindowInsets.statusBars).padding(top = 8.dp),
        ) {
            val e = small ?: return@AnimatedVisibility
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.inverseSurface, shadowElevation = 6.dp) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    RewardAmounts(e, MaterialTheme.colorScheme.inverseOnSurface)
                }
            }
        }

        big?.let { e ->
            Confetti(Modifier.fillMaxSize())
            AnimatedVisibility(
                visible = true,
                enter = scaleIn(spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow), initialScale = 0.6f) + fadeIn(),
                exit = scaleOut() + fadeOut(),
                modifier = Modifier.align(Alignment.Center),
            ) {
                Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainerHigh, shadowElevation = 12.dp) {
                    Column(
                        Modifier.padding(horizontal = 28.dp, vertical = 22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        val lvl = e.levelUp
                        if (lvl != null) {
                            Text("¡NIVEL ${lvl.level}!", style = MaterialTheme.typography.headlineMedium, color = Brand.Fire, fontWeight = FontWeight.Bold)
                            Text(lvl.title, style = MaterialTheme.typography.titleMedium)
                        }
                        Text(e.label, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                        Row(verticalAlignment = Alignment.CenterVertically) { RewardAmounts(e, MaterialTheme.colorScheme.onSurface) }
                    }
                }
            }
        }
    }
}

@Composable
private fun RewardAmounts(e: RewardEvent, color: Color) {
    if (e.xp > 0) {
        Icon(KtIcons.Bolt, null, Modifier.size(16.dp), tint = Brand.Accent)
        Text(" +${e.xp} XP", color = color, style = MaterialTheme.typography.labelLarge)
    }
    if (e.coins > 0) {
        Spacer(Modifier.width(12.dp))
        Icon(KtIcons.Coin, null, Modifier.size(16.dp), tint = Color(0xFFFFC857))
        Text(" +${e.coins}", color = color, style = MaterialTheme.typography.labelLarge)
    }
    if (e.xp == 0 && e.coins == 0) {
        Text("Tope de monedas alcanzado", color = color, style = MaterialTheme.typography.labelLarge)
    }
}

private val CONFETTI = listOf(
    Color(0xFFFF8A3D), Color(0xFF7FB2E5), Color(0xFFFFC857), Color(0xFFB46CFF), Color(0xFF4ADE80), Color(0xFFFF5E7E),
)

private class Piece(val angle: Float, val speed: Float, val color: Color, val spin: Float, val w: Float, val h: Float)

/** Confeti que sale del centro hacia arriba y cae con gravedad. */
@Composable
fun Confetti(modifier: Modifier = Modifier) {
    val pieces = remember {
        List(70) {
            Piece(
                angle = (-90f + Random.nextFloat() * 140f - 70f) * (Math.PI / 180f).toFloat(),
                speed = 900f + Random.nextFloat() * 900f,
                color = CONFETTI.random(),
                spin = Random.nextFloat() * 720f - 360f,
                w = 8f + Random.nextFloat() * 8f,
                h = 5f + Random.nextFloat() * 5f,
            )
        }
    }
    val t = remember { Animatable(0f) }
    LaunchedEffect(Unit) { t.animateTo(1f, tween(2600, easing = LinearEasing)) }
    Canvas(modifier) {
        val time = t.value * 2.6f
        val origin = Offset(size.width / 2, size.height * 0.55f)
        pieces.forEach { p ->
            val x = origin.x + cos(p.angle) * p.speed * time * 0.55f
            val y = origin.y + sin(p.angle) * p.speed * time + 900f * time * time
            val alpha = (1f - t.value).coerceIn(0f, 1f)
            rotate(p.spin * time, Offset(x, y)) {
                drawRect(p.color.copy(alpha = alpha), Offset(x - p.w / 2, y - p.h / 2), Size(p.w, p.h))
            }
        }
    }
}
