package com.sunless404.keeptrack.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.sunless404.keeptrack.App
import com.sunless404.keeptrack.cloud.CloudException
import com.sunless404.keeptrack.cloud.LeaderRow
import com.sunless404.keeptrack.ui.theme.Brand
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val stamp = DateTimeFormatter.ofPattern("dd/MM HH:mm")
private fun fmtStamp(ms: Long) = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).format(stamp)

/** Cuenta y nube: login/registro, elección tras iniciar sesión y estado de la sincronización. */
@Composable
fun AccountPanel(intro: String) {
    val app = LocalContext.current.applicationContext as App
    val cloud = app.cloud
    if (!cloud.available) {
        Text(
            "La nube no está disponible en esta compilación (falta app/google-services.json).",
            style = MaterialTheme.typography.bodySmall,
        )
        return
    }
    val account by cloud.account.collectAsState()
    val pending by cloud.pendingChoice.collectAsState()
    val lastSync by cloud.lastSync.collectAsState()
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    fun run(block: suspend () -> Unit) {
        busy = true
        message = null
        scope.launch {
            try {
                block()
            } catch (e: CloudException) {
                message = e.message
            } catch (e: Exception) {
                message = e.message
            }
            busy = false
        }
    }

    val acc = account
    when {
        acc == null -> SignInForm(intro, busy, message, onSignIn = { e, p -> run { cloud.signIn(e, p) } },
            onSignUp = { e, p, u -> run { cloud.signUp(e, p, u) } },
            onReset = { e -> run { cloud.resetPassword(e); message = "Te hemos enviado un correo para cambiar la contraseña." } })

        pending != null -> {
            var confirm by remember { mutableStateOf<Boolean?>(null) }
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer), shape = MaterialTheme.shapes.large) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Ya hay datos en tu cuenta", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Guardados el ${fmtStamp(pending!!.updatedAt)}" +
                            (pending!!.device.takeIf { it.isNotBlank() }?.let { " desde $it" } ?: "") +
                            ". ¿Con qué datos te quedas?",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Button(enabled = !busy, onClick = { confirm = true }, modifier = Modifier.fillMaxWidth()) { Text("Usar los de la nube") }
                    OutlinedButton(enabled = !busy, onClick = { confirm = false }, modifier = Modifier.fillMaxWidth()) { Text("Usar los de este móvil") }
                    message?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                }
            }
            confirm?.let { useCloud ->
                AlertDialog(
                    onDismissRequest = { confirm = null },
                    title = { Text(if (useCloud) "Usar los datos de la nube" else "Usar los datos de este móvil") },
                    text = {
                        Text(
                            if (useCloud) "Se borrarán la rutina, las tareas, el historial y el gimnasio de este móvil y se cargarán los de tu cuenta."
                            else "Se sobrescribirán los datos guardados en tu cuenta con los de este móvil."
                        )
                    },
                    confirmButton = { TextButton(onClick = { confirm = null; run { cloud.resolveChoice(useCloud) } }) { Text("Continuar") } },
                    dismissButton = { TextButton(onClick = { confirm = null }) { Text("Cancelar") } },
                )
            }
        }

        else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        (acc.username ?: acc.email).take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(acc.username?.let { "@$it" } ?: "Sesión iniciada", style = MaterialTheme.typography.titleMedium)
                    Text(acc.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(
                lastSync?.let { "Última copia en la nube: ${fmtStamp(it)}. Se sincroniza sola al hacer cambios." }
                    ?: "Tus datos se guardan en la nube automáticamente al hacer cambios.",
                style = MaterialTheme.typography.bodySmall,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(enabled = !busy, onClick = { run { cloud.uploadNow(); message = "Datos subidos a la nube." } }) { Text("Sincronizar ahora") }
                TextButton(enabled = !busy, onClick = { cloud.signOut() }) { Text("Cerrar sesión") }
                if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            }
            message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
private fun SignInForm(
    intro: String,
    busy: Boolean,
    message: String?,
    onSignIn: (String, String) -> Unit,
    onSignUp: (String, String, String) -> Unit,
    onReset: (String) -> Unit,
) {
    var register by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(intro, style = MaterialTheme.typography.bodyMedium)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            SegmentedButton(selected = !register, onClick = { register = false }, shape = SegmentedButtonDefaults.itemShape(0, 2)) { Text("Entrar") }
            SegmentedButton(selected = register, onClick = { register = true }, shape = SegmentedButtonDefaults.itemShape(1, 2)) { Text("Crear cuenta") }
        }
        if (register) {
            OutlinedTextField(
                value = username, onValueChange = { v -> username = v.lowercase().filter { it.isLetterOrDigit() || it == '_' || it == '.' }.take(20) },
                label = { Text("Nombre de usuario") }, supportingText = { Text("Así te encontrarán tus amigos. No se puede cambiar.") },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
        }
        OutlinedTextField(
            value = email, onValueChange = { email = it.trim() }, label = { Text("Correo") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = password, onValueChange = { password = it }, label = { Text("Contraseña") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            supportingText = if (register) ({ Text("Mínimo 6 caracteres.") }) else null,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                enabled = !busy && email.isNotBlank() && password.length >= 6 && (!register || username.length >= 3),
                onClick = { if (register) onSignUp(email, password, username) else onSignIn(email, password) },
            ) { Text(if (register) "Crear cuenta" else "Entrar") }
            if (!register) TextButton(enabled = !busy && email.isNotBlank(), onClick = { onReset(email) }) { Text("¿Olvidaste la contraseña?") }
            if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        }
        message?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    }
}

/** Clasificación entre amigos (necesita sesión). */
@Composable
fun FriendsLeaderboard(weekly: Boolean) {
    val app = LocalContext.current.applicationContext as App
    val cloud = app.cloud
    val account by cloud.account.collectAsState()
    if (account == null || !cloud.available) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer), shape = MaterialTheme.shapes.large) {
            Column(Modifier.padding(16.dp)) {
                AccountPanel("Crea una cuenta o entra para competir con tus amigos. Tus datos también quedarán guardados en la nube.")
            }
        }
        return
    }
    val scope = rememberCoroutineScope()
    var rows by remember { mutableStateOf<List<LeaderRow>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var friend by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    fun reload() {
        scope.launch {
            busy = true
            try {
                cloud.publishScore()
                rows = cloud.leaderboard()
                error = null
            } catch (e: Exception) {
                error = e.message
            }
            busy = false
        }
    }
    LaunchedEffect(account?.uid) { reload() }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val sorted = rows.orEmpty().sortedByDescending { if (weekly) it.weekPoints else it.strength }
        sorted.forEachIndexed { i, r ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (r.isMe) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                ),
                shape = MaterialTheme.shapes.medium,
            ) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${i + 1}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (i == 0) Brand.Fire else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(36.dp),
                    )
                    Text(
                        "@${r.username}" + if (r.isMe) " (tú)" else "",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Text(if (weekly) "${r.weekPoints} pts" else "${r.strength}", style = MaterialTheme.typography.titleMedium)
                    if (!r.isMe) {
                        IconButton(onClick = {
                            scope.launch {
                                runCatching { cloud.removeFriend(r.uid) }.onFailure { error = it.message }
                                reload()
                            }
                        }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Close, "Quitar amigo", Modifier.size(16.dp)) }
                    }
                }
            }
        }
        if (rows != null && rows!!.size <= 1) {
            Text("Añade a tus amigos por su nombre de usuario para competir.", style = MaterialTheme.typography.bodySmall)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = friend, onValueChange = { friend = it.trim().lowercase() },
                label = { Text("Añadir amigo (usuario)") }, singleLine = true, modifier = Modifier.weight(1f),
            )
            TextButton(enabled = friend.length >= 3 && !busy, onClick = {
                scope.launch {
                    try {
                        cloud.addFriend(friend)
                        friend = ""
                        error = null
                    } catch (e: Exception) {
                        error = e.message
                    }
                    reload()
                }
            }) { Text("Añadir") }
            IconButton(onClick = { reload() }, enabled = !busy) { Icon(Icons.Default.Refresh, "Actualizar") }
        }
        if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        Text(
            "Ves a la gente que tú añades. Para que ellos te vean a ti, tienen que añadirte también.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
