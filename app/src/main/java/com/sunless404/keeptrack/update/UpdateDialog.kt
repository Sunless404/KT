package com.sunless404.keeptrack.update

import android.content.ActivityNotFoundException
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.sunless404.keeptrack.App
import com.sunless404.keeptrack.BuildConfig
import com.sunless404.keeptrack.ui.common.KtIcons
import com.sunless404.keeptrack.ui.theme.Brand
import kotlinx.coroutines.launch

/** Aviso de versión nueva: se comprueba al abrir la app y al volver a ella. */
@Composable
fun UpdateDialog() {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as App
    val updater = app.updater
    val state by updater.state.collectAsState()
    val scope = rememberCoroutineScope()
    var needPermission by remember { mutableStateOf(false) }

    LifecycleResumeEffect(Unit) {
        scope.launch { updater.check() }
        // Al volver de los ajustes de "instalar apps desconocidas", seguir con la instalación.
        val s = updater.state.value
        if (needPermission && s is UpdateState.ReadyToInstall && updater.canInstall()) {
            needPermission = false
            ctx.startActivity(updater.installIntent(s.file))
        }
        onPauseOrDispose { }
    }

    fun install(s: UpdateState.ReadyToInstall) {
        if (!updater.canInstall()) {
            needPermission = true
            ctx.startActivity(updater.installPermissionIntent())
            return
        }
        try {
            ctx.startActivity(updater.installIntent(s.file))
        } catch (_: ActivityNotFoundException) {
        }
    }

    val release = when (val s = state) {
        is UpdateState.Available -> s.release
        is UpdateState.Downloading -> s.release
        is UpdateState.ReadyToInstall -> s.release
        is UpdateState.Failed -> s.release
        UpdateState.Idle -> null
    } ?: return

    LaunchedEffect(state) {
        // En cuanto termina la descarga, se abre el instalador.
        (state as? UpdateState.ReadyToInstall)?.let { install(it) }
    }

    AlertDialog(
        onDismissRequest = { if (state !is UpdateState.Downloading) updater.postpone() },
        icon = {
            Box(Modifier.size(48.dp).clip(CircleShape).background(Brand.Accent.copy(alpha = 0.2f)), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Icon(KtIcons.Sparkle, null, tint = Brand.Accent)
            }
        },
        title = { Text("Nueva versión disponible") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("${release.name} (tienes la 1.${BuildConfig.VERSION_CODE.takeIf { it > 1 } ?: 0})", style = MaterialTheme.typography.titleSmall)
                if (release.notes.isNotBlank()) {
                    Text(
                        release.notes,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.heightIn(max = 220.dp).verticalScroll(rememberScrollState()),
                    )
                }
                when (val s = state) {
                    is UpdateState.Downloading -> {
                        val p by animateFloatAsState(s.progress, tween(300, easing = FastOutSlowInEasing), label = "descarga")
                        Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
                            Box(Modifier.fillMaxHeight().fillMaxWidth(p).clip(CircleShape).background(Brand.Accent))
                        }
                        Text("Descargando… ${(s.progress * 100).toInt()} %", style = MaterialTheme.typography.bodySmall)
                    }
                    is UpdateState.ReadyToInstall -> Text(
                        if (updater.canInstall()) "Descargada. Confirma la instalación en la ventana de Android."
                        else "Permite a KeepTrack instalar aplicaciones (solo la primera vez) y vuelve aquí.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    is UpdateState.Failed -> Text(s.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    else -> Text("Tus datos se conservan al actualizar.", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            when (val s = state) {
                is UpdateState.Available, is UpdateState.Failed -> Button(onClick = { scope.launch { updater.download(release) } }) {
                    Text(if (s is UpdateState.Failed) "Reintentar" else "Actualizar")
                }
                is UpdateState.ReadyToInstall -> Button(onClick = { install(s) }) { Text("Instalar") }
                else -> {}
            }
        },
        dismissButton = {
            if (state !is UpdateState.Downloading) TextButton(onClick = { updater.postpone() }) { Text("Más tarde") }
        },
    )
}
