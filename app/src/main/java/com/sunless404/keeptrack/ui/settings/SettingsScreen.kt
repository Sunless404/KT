package com.sunless404.keeptrack.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sunless404.keeptrack.App
import com.sunless404.keeptrack.domain.fmtTime
import com.sunless404.keeptrack.ui.common.ScreenScaffold
import com.sunless404.keeptrack.ui.common.TimePickerDialog
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(onBack: () -> Unit, extra: @Composable ColumnScope.() -> Unit = {}) {
    val app = LocalContext.current.applicationContext as App
    val prefs = app.prefs
    fun resync() {
        app.scope.launch { app.alarms.sync() }
    }

    var lead by remember { mutableIntStateOf(prefs.defaultLeadMin) }
    var sound by remember { mutableStateOf(prefs.sound) }
    var vibrate by remember { mutableStateOf(prefs.vibrate) }
    var quiet by remember { mutableStateOf(prefs.quietEnabled) }
    var quietStart by remember { mutableIntStateOf(prefs.quietStartMin) }
    var quietEnd by remember { mutableIntStateOf(prefs.quietEndMin) }
    var eod by remember { mutableStateOf(prefs.endOfDayEnabled) }
    var eodMin by remember { mutableIntStateOf(prefs.endOfDayMin) }
    var picking by remember { mutableStateOf("") }

    ScreenScaffold(
        title = "Ajustes",
        navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") }
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Avisos", style = MaterialTheme.typography.titleMedium)
            Text("Antelación por defecto", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(0, 5, 10, 15, 30).forEach { m ->
                    FilterChip(
                        selected = lead == m,
                        onClick = { lead = m; prefs.defaultLeadMin = m; resync() },
                        label = { Text(if (m == 0) "Al empezar" else "$m min antes") },
                    )
                }
            }
            SwitchRow("Sonido", sound) { sound = it; prefs.sound = it }
            SwitchRow("Vibración", vibrate) { vibrate = it; prefs.vibrate = it }

            SwitchRow("Horas de silencio", quiet) { quiet = it; prefs.quietEnabled = it }
            if (quiet) {
                Text(
                    "En este tramo los avisos llegan sin sonido ni vibración.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { picking = "qs" }) { Text("Desde ${fmtTime(quietStart)}") }
                    OutlinedButton(onClick = { picking = "qe" }) { Text("Hasta ${fmtTime(quietEnd)}") }
                }
            }

            SwitchRow("Aviso de fin de día si quedan bloques sin marcar", eod) {
                eod = it; prefs.endOfDayEnabled = it; resync()
            }
            if (eod) OutlinedButton(onClick = { picking = "eod" }) { Text("A las ${fmtTime(eodMin)}") }

            OutlinedButton(onClick = { app.alarms.notifyTest() }) { Text("Enviar aviso de prueba") }

            Spacer(Modifier.height(4.dp))
            PermissionsCard(always = true)

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            extra()
        }
    }

    if (picking.isNotEmpty()) {
        val (initial, title) = when (picking) {
            "qs" -> quietStart to "Inicio del silencio"
            "qe" -> quietEnd to "Fin del silencio"
            else -> eodMin to "Aviso de fin de día"
        }
        TimePickerDialog(initial, title, onDismiss = { picking = "" }) { m ->
            when (picking) {
                "qs" -> { quietStart = m; prefs.quietStartMin = m }
                "qe" -> { quietEnd = m; prefs.quietEndMin = m }
                else -> { eodMin = m; prefs.endOfDayMin = m; resync() }
            }
            picking = ""
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
