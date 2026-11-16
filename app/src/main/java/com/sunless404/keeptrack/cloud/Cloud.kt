package com.sunless404.keeptrack.cloud

import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import com.sunless404.keeptrack.App
import com.sunless404.keeptrack.data.Backup
import com.sunless404.keeptrack.data.restore
import com.sunless404.keeptrack.data.toBackup
import com.sunless404.keeptrack.domain.GymScore
import com.sunless404.keeptrack.domain.mondayOf
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class CloudException(message: String) : Exception(message)

data class Account(val uid: String, val email: String, val username: String?)

data class LeaderRow(
    val uid: String,
    val username: String,
    /** Puntos de la semana actual (0 si su último dato es de otra semana). */
    val weekPoints: Int,
    val strength: Int,
    val isMe: Boolean,
)

/** Datos que hay en la nube, para decidir qué hacer al iniciar sesión en un móvil. */
data class CloudBackupInfo(val updatedAt: Long, val device: String)

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { c ->
    addOnSuccessListener { c.resume(it) }
    addOnFailureListener { c.resumeWithException(it) }
    addOnCanceledListener { c.cancel() }
}

/**
 * Cuenta, sincronización y clasificación con Firebase (Auth + Firestore).
 *
 * Firestore:
 * - users/{uid}: perfil público (usuario y puntuaciones), lo leen otros usuarios con sesión.
 * - users/{uid}/friends/{uid}: amigos que has añadido. Privado.
 * - users/{uid}/sync/meta y users/{uid}/sync/part_N: copia completa de tus datos. Privado.
 * - usernames/{nombre}: reserva el nombre de usuario para que sea único.
 */
class Cloud(private val app: App) {
    val available: Boolean = FirebaseApp.getApps(app).isNotEmpty()

    private val auth: FirebaseAuth? = if (available) FirebaseAuth.getInstance() else null
    private val db: FirebaseFirestore? = if (available) FirebaseFirestore.getInstance() else null

    private val _account = MutableStateFlow<Account?>(null)
    val account: StateFlow<Account?> = _account.asStateFlow()

    private val _lastSync = MutableStateFlow<Long?>(null)
    val lastSync: StateFlow<Long?> = _lastSync.asStateFlow()

    /** Copia en la nube pendiente de decidir (restaurar o sobrescribir) tras iniciar sesión. */
    private val _pendingChoice = MutableStateFlow<CloudBackupInfo?>(null)
    val pendingChoice: StateFlow<CloudBackupInfo?> = _pendingChoice.asStateFlow()

    private val syncLock = Mutex()
    private var uploadJob: Job? = null

    /** Mientras se espera la decisión del usuario no se sube nada, para no pisar la nube. */
    private val syncEnabled get() = _account.value != null && _pendingChoice.value == null &&
        app.prefs.sp.getBoolean(PREF_SYNC_READY + _account.value?.uid, false)

    init {
        auth?.addAuthStateListener { a ->
            val u = a.currentUser
            if (u == null) {
                _account.value = null
            } else {
                _account.value = Account(u.uid, u.email.orEmpty(), _account.value?.takeIf { it.uid == u.uid }?.username)
                app.scope.launch { loadProfile(u.uid) }
            }
        }
    }

    private fun users() = db!!.collection("users")
    private fun requireUid(): String = _account.value?.uid ?: throw CloudException("Inicia sesión primero.")

    private suspend fun loadProfile(uid: String) {
        try {
            val doc = users().document(uid).get().await()
            val name = doc.getString("username")
            _account.value = _account.value?.copy(username = name)
            app.prefs.sp.edit().putString("cloudUsername", name).apply()
            if (!app.prefs.sp.getBoolean(PREF_SYNC_READY + uid, false)) checkCloudBackup(uid) else publishScore()
        } catch (_: Exception) {
            _account.value = _account.value?.copy(username = app.prefs.sp.getString("cloudUsername", null))
        }
    }

    private fun friendly(e: Exception): CloudException = CloudException(
        when (e) {
            is CloudException -> e.message.orEmpty()
            is FirebaseAuthWeakPasswordException -> "La contraseña es demasiado débil: usa al menos 6 caracteres."
            is FirebaseAuthUserCollisionException -> "Ya existe una cuenta con ese correo."
            is FirebaseAuthInvalidUserException -> "No hay ninguna cuenta con ese correo."
            is FirebaseAuthInvalidCredentialsException -> "Correo o contraseña incorrectos."
            is FirebaseAuthException -> "Error de la cuenta: ${e.message}"
            is FirebaseFirestoreException -> when (e.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                    "La nube ha rechazado la operación. Revisa que las reglas de Firestore estén publicadas."
                FirebaseFirestoreException.Code.UNAVAILABLE -> "Sin conexión. Se reintentará más tarde."
                else -> "Error de la nube: ${e.message}"
            }
            else -> "No se pudo completar: ${e.message}"
        }
    )

    // ---------- Cuenta ----------

    suspend fun signUp(email: String, password: String, rawUsername: String) {
        val username = SyncCodec.normalizeUsername(rawUsername)
            ?: throw CloudException("El usuario debe tener de 3 a 20 caracteres: letras, números, punto o guion bajo.")
        try {
            val taken = db!!.collection("usernames").document(username).get().await().exists()
            if (taken) throw CloudException("Ese nombre de usuario ya está cogido.")
        } catch (e: FirebaseFirestoreException) {
            // Sin sesión no se puede leer: se comprueba al reservarlo, más abajo.
            if (e.code != FirebaseFirestoreException.Code.PERMISSION_DENIED) throw friendly(e)
        }
        try {
            val user = auth!!.createUserWithEmailAndPassword(email.trim(), password).await().user!!
            try {
                val batch = db!!.batch()
                batch.set(db.collection("usernames").document(username), mapOf("uid" to user.uid))
                batch.set(
                    users().document(user.uid),
                    mapOf("username" to username, "createdAt" to FieldValue.serverTimestamp(), "weekPoints" to 0, "strength" to 0, "weekKey" to ""),
                )
                batch.commit().await()
            } catch (e: Exception) {
                // Si el nombre ya estaba cogido, se deshace la cuenta recién creada.
                user.delete().await()
                throw if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED)
                    CloudException("Ese nombre de usuario ya está cogido.") else e
            }
            _account.value = Account(user.uid, user.email.orEmpty(), username)
            // Cuenta nueva: lo que hay en el móvil pasa a ser la copia de la nube.
            app.prefs.sp.edit().putBoolean(PREF_SYNC_READY + user.uid, true).apply()
            uploadNow()
            publishScore()
        } catch (e: Exception) {
            throw friendly(e)
        }
    }

    suspend fun signIn(email: String, password: String) {
        try {
            auth!!.signInWithEmailAndPassword(email.trim(), password).await()
        } catch (e: Exception) {
            throw friendly(e)
        }
    }

    suspend fun resetPassword(email: String) {
        try {
            auth!!.sendPasswordResetEmail(email.trim()).await()
        } catch (e: Exception) {
            throw friendly(e)
        }
    }

    fun signOut() {
        uploadJob?.cancel()
        _pendingChoice.value = null
        auth?.signOut()
    }

    // ---------- Sincronización ----------

    private suspend fun checkCloudBackup(uid: String) {
        val meta = users().document(uid).collection("sync").document("meta").get().await()
        if (meta.exists()) {
            _pendingChoice.value = CloudBackupInfo(meta.getLong("updatedAt") ?: 0, meta.getString("device").orEmpty())
        } else {
            app.prefs.sp.edit().putBoolean(PREF_SYNC_READY + uid, true).apply()
            uploadNow()
        }
    }

    /** Tras iniciar sesión en un móvil: quedarse con lo de la nube o con lo del móvil. */
    suspend fun resolveChoice(useCloud: Boolean) {
        val uid = requireUid()
        try {
            if (useCloud) downloadAndRestore() else Unit
            _pendingChoice.value = null
            app.prefs.sp.edit().putBoolean(PREF_SYNC_READY + uid, true).apply()
            if (!useCloud) uploadNow()
            publishScore()
        } catch (e: Exception) {
            throw friendly(e)
        }
    }

    /** Programa una subida (se agrupan los cambios seguidos en una sola). */
    fun scheduleUpload() {
        if (!syncEnabled) return
        uploadJob?.cancel()
        uploadJob = app.scope.launch {
            delay(15_000)
            runCatching { uploadNow() }
            publishScore()
        }
    }

    suspend fun uploadNow() = syncLock.withLock {
        if (!syncEnabled) return@withLock
        val uid = requireUid()
        try {
            val json = app.repo.exportBackup(app.prefs.toBackup()).toJson()
            val parts = SyncCodec.encode(json)
            val sync = users().document(uid).collection("sync")
            val old = sync.document("meta").get().await().getLong("parts")?.toInt() ?: 0
            val batch = db!!.batch()
            parts.forEachIndexed { i, p -> batch.set(sync.document("part_$i"), mapOf("data" to p)) }
            for (i in parts.size until old) batch.delete(sync.document("part_$i"))
            val now = System.currentTimeMillis()
            batch.set(sync.document("meta"), mapOf("parts" to parts.size, "updatedAt" to now, "device" to android.os.Build.MODEL))
            batch.commit().await()
            _lastSync.value = now
        } catch (e: Exception) {
            throw friendly(e)
        }
    }

    private suspend fun downloadAndRestore() {
        val uid = requireUid()
        val sync = users().document(uid).collection("sync")
        val meta = sync.document("meta").get().await()
        val n = meta.getLong("parts")?.toInt() ?: throw CloudException("No hay datos en la nube.")
        val parts = (0 until n).map { sync.document("part_$it").get().await().getString("data").orEmpty() }
        val backup = Backup.fromJson(SyncCodec.decode(parts))
        app.prefs.restore(backup.settings)
        app.repo.restoreBackup(backup)
        _lastSync.value = meta.getLong("updatedAt")
    }

    // ---------- Gimnasio: clasificación ----------

    /** Publica tus puntos en tu perfil público para que te vean tus amigos. */
    suspend fun publishScore() {
        val uid = _account.value?.uid ?: return
        val sessions = app.repo.observeGymSessions().first()
        val sets = app.repo.observeGymSets().first()
        val today = LocalDate.now()
        val s = GymScore.summarize(sessions, sets, today)
        try {
            users().document(uid).set(
                mapOf(
                    "weekPoints" to s.weekPoints.coerceIn(0, MAX_WEEK_POINTS),
                    "strength" to s.strengthScore.coerceIn(0, MAX_STRENGTH),
                    "weekKey" to mondayOf(today).toString(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
                SetOptions.merge(),
            ).await()
        } catch (_: Exception) {
            // Se reintenta en la próxima publicación.
        }
    }

    suspend fun addFriend(rawUsername: String) {
        val me = requireUid()
        val name = SyncCodec.normalizeUsername(rawUsername) ?: throw CloudException("Nombre de usuario no válido.")
        try {
            val doc = db!!.collection("usernames").document(name).get().await()
            val uid = doc.getString("uid") ?: throw CloudException("No existe ningún usuario «$name».")
            if (uid == me) throw CloudException("Ese eres tú.")
            users().document(me).collection("friends").document(uid)
                .set(mapOf("username" to name, "since" to FieldValue.serverTimestamp())).await()
        } catch (e: Exception) {
            throw friendly(e)
        }
    }

    suspend fun removeFriend(uid: String) {
        val me = requireUid()
        try {
            users().document(me).collection("friends").document(uid).delete().await()
        } catch (e: Exception) {
            throw friendly(e)
        }
    }

    /** Tú y tus amigos, ordenados por la clasificación elegida. */
    suspend fun leaderboard(): List<LeaderRow> {
        val me = requireUid()
        try {
            val week = mondayOf(LocalDate.now()).toString()
            val friendIds = users().document(me).collection("friends").get().await().documents.map { it.id }
            return (listOf(me) + friendIds).mapNotNull { uid ->
                val d = users().document(uid).get().await()
                if (!d.exists()) return@mapNotNull null
                LeaderRow(
                    uid = uid,
                    username = d.getString("username") ?: "?",
                    weekPoints = if (d.getString("weekKey") == week) (d.getLong("weekPoints") ?: 0).toInt() else 0,
                    strength = (d.getLong("strength") ?: 0).toInt(),
                    isMe = uid == me,
                )
            }
        } catch (e: Exception) {
            throw friendly(e)
        }
    }

    companion object {
        private const val PREF_SYNC_READY = "syncReady_"
        // Límites que también comprueban las reglas de Firestore.
        const val MAX_WEEK_POINTS = 1000
        const val MAX_STRENGTH = 5000
    }
}
