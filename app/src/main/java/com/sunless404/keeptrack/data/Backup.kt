package com.sunless404.keeptrack.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Copia de seguridad completa: plantilla, historial, días especiales y ajustes. */
@Serializable
data class Backup(
    val app: String = "keeptrack",
    val version: Int = 2,
    val exportedAt: Long = 0,
    val blocks: List<Block> = emptyList(),
    val entries: List<DayEntry> = emptyList(),
    val days: List<DayInfo> = emptyList(),
    val tasks: List<Task> = emptyList(),
    val gymSessions: List<GymSession> = emptyList(),
    val gymSets: List<GymSet> = emptyList(),
    val settings: BackupSettings = BackupSettings(),
) {
    fun toJson(): String = json.encodeToString(serializer(), this)

    companion object {
        private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }

        /** Lanza una excepción si el texto no es una copia de seguridad válida. */
        fun fromJson(text: String): Backup {
            val b = json.decodeFromString(serializer(), text)
            // "rutina" es el identificador de las copias hechas antes del cambio de nombre.
            require(b.app == "keeptrack" || b.app == "rutina") { "El archivo no es una copia de seguridad de KeepTrack" }
            require(b.version <= 2) { "La copia es de una versión más nueva de la app" }
            return b
        }
    }
}

@Serializable
data class BackupSettings(
    val defaultLeadMin: Int = 0,
    val sound: Boolean = true,
    val vibrate: Boolean = true,
    val quietEnabled: Boolean = false,
    val quietStartMin: Int = 23 * 60,
    val quietEndMin: Int = 7 * 60,
    val endOfDayEnabled: Boolean = true,
    val endOfDayMin: Int = 22 * 60,
)

fun Prefs.toBackup() = BackupSettings(
    defaultLeadMin, sound, vibrate, quietEnabled, quietStartMin, quietEndMin, endOfDayEnabled, endOfDayMin,
)

fun Prefs.restore(s: BackupSettings) {
    defaultLeadMin = s.defaultLeadMin
    sound = s.sound
    vibrate = s.vibrate
    quietEnabled = s.quietEnabled
    quietStartMin = s.quietStartMin
    quietEndMin = s.quietEndMin
    endOfDayEnabled = s.endOfDayEnabled
    endOfDayMin = s.endOfDayMin
}
