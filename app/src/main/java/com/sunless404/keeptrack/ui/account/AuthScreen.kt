package com.sunless404.keeptrack.ui.account

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sunless404.keeptrack.App
import com.sunless404.keeptrack.R
import com.sunless404.keeptrack.ui.common.appear
import com.sunless404.keeptrack.ui.theme.Brand
import kotlinx.coroutines.launch

/** Primera pantalla: entrar, crear cuenta o seguir como invitado. */
@Composable
fun AuthScreen(onGuest: () -> Unit) {
    val app = LocalContext.current.applicationContext as App
    val cloud = app.cloud
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    fun run(block: suspend () -> Unit) {
        busy = true
        message = null
        scope.launch {
            try {
                block()
            } catch (e: Exception) {
                message = e.message
            }
            busy = false
        }
    }

    // Logo: entra con rebote y luego "respira" con un halo.
    val logo = remember { Animatable(0f) }
    LaunchedEffect(Unit) { logo.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow)) }
    val t = rememberInfiniteTransition(label = "halo")
    val glow by t.animateFloat(0.6f, 1f, infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "glow")

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Brand.Slate, MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.background))
        ).windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Column(
            Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(72.dp))
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(150.dp)) {
                Canvas(Modifier.fillMaxSize().scale(glow)) {
                    drawCircle(Brush.radialGradient(listOf(Brand.Accent.copy(alpha = 0.35f), Color.Transparent)))
                }
                Image(
                    painterResource(R.drawable.ic_launcher_foreground), "KeepTrack",
                    modifier = Modifier.size(110.dp).clip(RoundedCornerShape(30.dp)).background(Brand.Slate)
                        .graphicsLayer {
                            val p = logo.value
                            alpha = p.coerceIn(0f, 1f)
                            scaleX = 0.6f + 0.4f * p
                            scaleY = 0.6f + 0.4f * p
                            rotationZ = (1f - p) * -20f
                        },
                )
            }
            Spacer(Modifier.height(16.dp))
            Text("KeepTrack", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.appear(1))
            Text(
                "Tu rutina, tus tareas y tus rachas, siempre contigo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.appear(2),
            )
            Spacer(Modifier.height(28.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth().appear(3),
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SignInForm(
                        intro = "Con una cuenta guardas todo en la nube y compites con tus amigos.",
                        busy = busy,
                        message = message,
                        onSignIn = { e, p -> run { cloud.signIn(e, p) } },
                        onSignUp = { e, p, u -> run { cloud.signUp(e, p, u) } },
                        onReset = { e -> run { cloud.resetPassword(e); message = "Te hemos enviado un correo para cambiar la contraseña." } },
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "Como invitado todo se guarda solo en este móvil. Podrás crear la cuenta más tarde desde tu perfil.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.appear(4).padding(bottom = 24.dp),
            )
        }
        // Va después del formulario para quedar por encima: si no, el área desplazable
        // del formulario se queda con los toques y el botón no responde.
        TextButton(onClick = onGuest, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).appear(6)) {
            Text("Entrar como invitado")
        }
    }
}
