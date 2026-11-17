package com.sunless404.keeptrack.ui.common

import android.graphics.BitmapFactory
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sunless404.keeptrack.App
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Entrada animada: aparece desde abajo con un pequeño rebote, escalonada por [index].
 * Se usa en tarjetas y elementos de lista de toda la app.
 */
fun Modifier.appear(index: Int = 0, offset: Float = 48f): Modifier = composed {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay((index.coerceAtMost(12) * 45).toLong())
        launch { progress.animateTo(1f, spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessLow)) }
    }
    graphicsLayer {
        val p = progress.value
        alpha = p.coerceIn(0f, 1f)
        translationY = (1f - p) * offset
        val s = 0.94f + 0.06f * p
        scaleX = s
        scaleY = s
    }
}

/** Número que cuenta desde 0 hasta [value] al aparecer y al cambiar. */
@Composable
fun AnimatedNumber(value: Int, style: TextStyle, modifier: Modifier = Modifier, durationMs: Int = 900, color: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified) {
    var target by remember { mutableStateOf(0) }
    LaunchedEffect(value) { target = value }
    val shown by animateIntAsState(target, tween(durationMs, easing = FastOutSlowInEasing), label = "numero")
    Text("$shown", style = style, modifier = modifier, color = color)
}

/** Botón "?" que abre una explicación breve del apartado. */
@Composable
fun HelpButton(title: String, points: List<String>) {
    var open by remember { mutableStateOf(false) }
    IconButton(onClick = { open = true }) { Icon(KtIcons.Help, "Ayuda") }
    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            icon = { Icon(KtIcons.Help, null) },
            title = { Text(title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    points.forEachIndexed { i, p ->
                        Row(Modifier.appear(i, 20f)) {
                            Text("•  ", color = MaterialTheme.colorScheme.secondary)
                            Text(p, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { open = false }) { Text("Entendido") } },
        )
    }
}

/** Foto de perfil redonda; sin foto, la inicial sobre fondo de color. */
@Composable
fun Avatar(photo: ByteArray?, name: String, size: Dp, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val bmp = remember(photo) { photo?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() } }
    Box(
        modifier.size(size).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (bmp != null) {
            Image(bmp, "Foto de perfil", Modifier.size(size), contentScale = ContentScale.Crop)
        } else if (name.isNotBlank()) {
            Text(
                name.trim().take(1).uppercase(),
                style = if (size >= 64.dp) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        } else {
            Icon(KtIcons.Person, null, Modifier.size(size * 0.6f), tint = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}

/** Ayuda y foto de perfil: van arriba a la derecha en todos los apartados. */
@Composable
fun TopActions(helpTitle: String, help: List<String>, onProfile: () -> Unit, extra: @Composable () -> Unit = {}) {
    val app = LocalContext.current.applicationContext as App
    val profile by app.profile.profile.collectAsState()
    Row(verticalAlignment = Alignment.CenterVertically) {
        extra()
        HelpButton(helpTitle, help)
        Avatar(
            profile.photo, profile.displayName, 34.dp,
            Modifier.padding(start = 4.dp, end = 8.dp).semantics { contentDescription = "Perfil" },
            onClick = onProfile,
        )
    }
}
