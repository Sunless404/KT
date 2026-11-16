package com.sunless404.keeptrack.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.sunless404.keeptrack.App
import com.sunless404.keeptrack.ai.AiException
import com.sunless404.keeptrack.ai.AiModel
import com.sunless404.keeptrack.ai.Gemini
import kotlinx.coroutines.launch

/** Configuración del asistente: clave gratuita de Google AI Studio y modelo. */
@Composable
fun AiSection() {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as App
    val scope = rememberCoroutineScope()
    var key by remember { mutableStateOf(app.secrets.apiKey) }
    var saved by remember { mutableStateOf(key.isNotBlank()) }
    var model by remember { mutableStateOf(app.prefs.aiModel) }
    var models by remember { mutableStateOf<List<AiModel>>(emptyList()) }
    var status by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    Text("Asistente IA", style = MaterialTheme.typography.titleMedium)
    Text(
        "Usa Google Gemini con su plan gratuito: no pide tarjeta. Pasos:\n" +
            "1. Pulsa «Conseguir clave» e inicia sesión con tu cuenta de Google.\n" +
            "2. Crea una clave de API y cópiala.\n" +
            "3. Pégala aquí y pulsa «Guardar y probar».",
        style = MaterialTheme.typography.bodySmall,
    )
    OutlinedButton(onClick = {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Gemini.KEY_URL)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }) { Text("Conseguir clave") }
    OutlinedTextField(
        value = key,
        onValueChange = { key = it.trim(); saved = false },
        label = { Text("Clave de API de Gemini") },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth(),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(enabled = !busy && key.isNotBlank(), onClick = {
            app.secrets.apiKey = key
            saved = true
            busy = true
            status = "Comprobando…"
            scope.launch {
                status = try {
                    models = Gemini.listModels(key)
                    if (models.isNotEmpty() && models.none { it.id == model }) {
                        model = models.first().id
                        app.prefs.aiModel = model
                    }
                    "Clave correcta. Modelo en uso: $model"
                } catch (e: AiException) {
                    e.message
                }
                busy = false
            }
        }) { Text("Guardar y probar") }
        if (saved) OutlinedButton(onClick = {
            app.secrets.apiKey = ""
            key = ""
            saved = false
            models = emptyList()
            status = "Clave borrada."
        }) { Text("Borrar clave") }
    }
    status?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    if (models.isNotEmpty()) {
        Text("Modelo (los «flash» son los del plan gratuito):", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            models.take(8).forEach { m ->
                FilterChip(
                    selected = model == m.id,
                    onClick = { model = m.id; app.prefs.aiModel = m.id },
                    label = { Text(m.id) },
                )
            }
        }
    }
    Text(
        "La clave se guarda cifrada en el móvil y no se incluye en la copia de seguridad. " +
            "Solo se conecta a Internet cuando pulsas «Generar» o «Guardar y probar».",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
