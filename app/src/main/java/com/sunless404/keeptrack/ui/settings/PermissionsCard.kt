package com.sunless404.keeptrack.ui.settings

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.sunless404.keeptrack.App

private data class PermState(val notifications: Boolean, val exact: Boolean, val battery: Boolean)

private fun readState(ctx: Context): PermState {
    val app = ctx.applicationContext as App
    val notif = NotificationManagerCompat.from(ctx).areNotificationsEnabled() &&
        (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(
            ctx, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED)
    val battery = ctx.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(ctx.packageName)
    return PermState(notif, app.alarms.canScheduleExact(), battery)
}

private fun Context.open(intent: Intent) {
    try {
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Exception) {
        startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

/**
 * Explica y pide lo necesario para que los avisos lleguen: notificaciones,
 * alarmas exactas y exclusión de la optimización de batería.
 * Con [always] = false solo aparece si falta algo.
 */
@SuppressLint("BatteryLife")
@Composable
fun PermissionsCard(always: Boolean = false, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as App
    var state by remember { mutableStateOf(readState(ctx)) }
    var hideBattery by remember { mutableStateOf(app.prefs.hideBatteryHint) }
    val pkg = Uri.parse("package:${ctx.packageName}")

    LifecycleResumeEffect(Unit) {
        state = readState(ctx)
        onPauseOrDispose { }
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        state = readState(ctx)
        if (!granted) {
            // Denegado antes: Android ya no muestra el diálogo, hay que ir a los ajustes.
            ctx.open(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
            )
        }
    }

    val batteryPending = !state.battery && (always || !hideBattery)
    if (!always && state.notifications && state.exact && !batteryPending) return

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text("Permisos para los avisos", style = MaterialTheme.typography.titleMedium)

            Item(
                ok = state.notifications,
                title = "Notificaciones",
                why = "Sin este permiso Android no deja mostrar ningún aviso de tus bloques.",
                button = "Permitir notificaciones",
            ) {
                if (Build.VERSION.SDK_INT >= 33) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                else ctx.open(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
                )
            }

            Item(
                ok = state.exact,
                title = "Alarmas exactas",
                why = "Permite avisar en el minuto justo aunque la app esté cerrada o el móvil en reposo. " +
                    "Sin él los avisos pueden llegar varios minutos tarde. Activa «Permitir alarmas y recordatorios».",
                button = "Abrir ajuste de alarmas",
            ) {
                if (Build.VERSION.SDK_INT >= 31) ctx.open(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, pkg))
            }

            if (state.battery || batteryPending) Item(
                ok = state.battery,
                title = "Sin optimización de batería",
                why = "Algunos móviles cierran las apps en segundo plano y los avisos dejan de llegar. " +
                    "Excluye KeepTrack de la optimización de batería. En Xiaomi, Huawei, Oppo o Samsung activa además " +
                    "«Inicio automático» y batería «Sin restricciones» en los ajustes de la app.",
                button = "Excluir de la optimización",
                secondary = if (always) null else "No mostrar más",
                onSecondary = {
                    app.prefs.hideBatteryHint = true
                    hideBattery = true
                },
            ) {
                ctx.open(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, pkg))
            }
        }
    }
}

@Composable
private fun Item(
    ok: Boolean,
    title: String,
    why: String,
    button: String,
    secondary: String? = null,
    onSecondary: () -> Unit = {},
    onClick: () -> Unit,
) {
    Spacer(Modifier.height(8.dp))
    Text((if (ok) "✓ " else "✗ ") + title, style = MaterialTheme.typography.titleSmall)
    if (!ok) {
        Text(why, style = MaterialTheme.typography.bodySmall)
        Row {
            Button(onClick = onClick) { Text(button) }
            if (secondary != null) TextButton(onClick = onSecondary) { Text(secondary) }
        }
    }
}
