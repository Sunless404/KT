package com.sunless404.rutina.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sunless404.rutina.App
import com.sunless404.rutina.data.Backup
import com.sunless404.rutina.data.restore
import com.sunless404.rutina.data.toBackup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

/** Exportar e importar todos los datos en un archivo JSON elegido por el usuario. */
@Composable
fun BackupSection() {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as App
    var message by remember { mutableStateOf<String?>(null) }
    var toRestore by remember { mutableStateOf<Backup?>(null) }

    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        app.scope.launch {
            message = try {
                val backup = app.repo.exportBackup(app.prefs.toBackup())
                withContext(Dispatchers.IO) {
                    ctx.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(backup.toJson().toByteArray()) }
                }
                "Copia guardada: ${backup.blocks.size} bloques de plantilla y ${backup.entries.size} registros."
            } catch (e: Exception) {
                "No se pudo exportar: ${e.message}"
            }
        }
    }

    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        app.scope.launch {
            try {
                val text = withContext(Dispatchers.IO) {
                    ctx.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() }
                }
                toRestore = Backup.fromJson(text)
            } catch (e: Exception) {
                message = "El archivo no es una copia válida: ${e.message?.lineSequence()?.firstOrNull()}"
            }
        }
    }

    Text("Copia de seguridad", style = MaterialTheme.typography.titleMedium)
    Text(
        "Guarda la plantilla, el historial, los días especiales y los ajustes en un archivo JSON. " +
            "Nada sale del móvil salvo el archivo que tú guardes.",
        style = MaterialTheme.typography.bodySmall,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { export.launch("rutina-${LocalDate.now()}.json") }) { Text("Exportar") }
        OutlinedButton(onClick = { import.launch(arrayOf("application/json", "text/plain", "*/*")) }) { Text("Importar") }
    }
    message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }

    toRestore?.let { b ->
        AlertDialog(
            onDismissRequest = { toRestore = null },
            title = { Text("Restaurar copia") },
            text = {
                Text(
                    "La copia tiene ${b.blocks.size} bloques de plantilla y ${b.entries.size} registros. " +
                        "Se borrarán TODOS los datos actuales de la app y se sustituirán por los de la copia."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    toRestore = null
                    app.scope.launch {
                        message = try {
                            app.prefs.restore(b.settings)
                            app.repo.restoreBackup(b)
                            "Copia restaurada."
                        } catch (e: Exception) {
                            "No se pudo restaurar: ${e.message}"
                        }
                    }
                }) { Text("Sustituir todo") }
            },
            dismissButton = { TextButton(onClick = { toRestore = null }) { Text("Cancelar") } },
        )
    }
}
