package com.sunless404.keeptrack.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** Categorías de bloque. [color] en ARGB. */
enum class Category(val label: String, val color: Long, val notifiesByDefault: Boolean = true, val fixedByDefault: Boolean = false) {
    CLASE("Clase", 0xFF3F51B5, fixedByDefault = true),
    ACADEMIA("Academia", 0xFFAB47BC, fixedByDefault = true),
    ESTUDIO("Estudio", 0xFF1E88E5),
    GIMNASIO("Gimnasio", 0xFFE53935),
    DESPLAZAMIENTO("Desplazamiento", 0xFF78909C, fixedByDefault = true),
    COMIDA("Comida", 0xFFFB8C00, fixedByDefault = true),
    SUENO("Sueño", 0xFF4527A0, notifiesByDefault = false),
    LECTURA("Lectura", 0xFF00897B),
    HIGIENE("Higiene", 0xFF26C6DA),
    LIBRE("Libre", 0xFF7CB342, notifiesByDefault = false),
    OTRO("Otro", 0xFF8D6E63);
}

enum class Status(val label: String) {
    PENDIENTE("Pendiente"),
    HECHO("Hecho"),
    SALTADO("Quitado"),
    POSPUESTO("Pospuesto"),
}

enum class DayType(val label: String) {
    NORMAL("Normal"),
    FESTIVO("Festivo"),
    LIBRE("Día libre"),
}

const val DAY_MIN = 1440

/**
 * Bloque de la plantilla semanal. Una fila por día de la semana.
 * [startMin] y [endMin] son minutos desde las 00:00 (0..1439).
 * Si [endMin] <= [startMin] el bloque cruza la medianoche y termina al día siguiente.
 */
@Serializable
@Entity(tableName = "blocks")
data class Block(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: Category,
    /** 1 = lunes … 7 = domingo */
    val dayOfWeek: Int,
    val startMin: Int,
    val endMin: Int,
    val notes: String = "",
    val notify: Boolean = category.notifiesByDefault,
    /** Minutos de antelación del aviso; null = valor por defecto de Ajustes. */
    val leadMin: Int? = null,
    /** Fijo: no se mueve ni cambia de duración al reajustar la rutina (clases, academia…). */
    @androidx.room.ColumnInfo(defaultValue = "0") val fixed: Boolean = category.fixedByDefault,
) {
    val durationMin: Int
        get() = ((endMin - startMin + DAY_MIN) % DAY_MIN).let { if (it == 0) DAY_MIN else it }

    val crossesMidnight: Boolean get() = endMin <= startMin

    fun toEntry(epochDay: Long) = DayEntry(
        epochDay = epochDay,
        blockId = id,
        title = title,
        category = category,
        startMin = startMin,
        endMin = startMin + durationMin,
        notes = notes,
        notify = notify,
        leadMin = leadMin,
    )
}

/**
 * Bloque concreto de una fecha: copia congelada de la plantilla más su estado.
 * [startMin]/[endMin] son minutos desde las 00:00 de [epochDay]; [endMin] puede
 * pasar de 1440 si el bloque termina al día siguiente.
 */
@Serializable
@Entity(tableName = "day_entries", indices = [Index("epochDay")])
data class DayEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val blockId: Long?,
    val title: String,
    val category: Category,
    val startMin: Int,
    val endMin: Int,
    val notes: String = "",
    val notify: Boolean = true,
    val leadMin: Int? = null,
    val status: Status = Status.PENDIENTE,
    /** Momento real en que se marcó (epoch millis). */
    val markedAt: Long? = null,
    /** true si un imprevisto lo ha cambiado: la plantilla ya no lo resincroniza. */
    val modified: Boolean = false,
    /** Si está pospuesto, cuándo vuelve a avisar (epoch millis). */
    val snoozeUntil: Long? = null,
) {
    val durationMin: Int get() = endMin - startMin
}

/** Estado de una fecha: materializada (historial congelado) y tipo de día. */
@Serializable
@Entity(tableName = "days")
data class DayInfo(
    @PrimaryKey val epochDay: Long,
    val type: DayType = DayType.NORMAL,
    val materialized: Boolean = false,
)

/** Tarea puntual de una fecha (los "pluses" de Hoy). */
@Serializable
@Entity(tableName = "tasks", indices = [Index("epochDay")])
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    /** Minutos desde las 00:00; null = sin hora. */
    val timeMin: Int? = null,
    val title: String,
    val description: String = "",
    val notify: Boolean = false,
    /** Momento del recordatorio (epoch millis). */
    val remindAt: Long? = null,
    val done: Boolean = false,
    val doneAt: Long? = null,
    /** Hora de fin (minutos desde las 00:00); null = sin hora de fin. */
    val endMin: Int? = null,
    /** 0 normal, 1 alta. */
    @androidx.room.ColumnInfo(defaultValue = "0") val priority: Int = 0,
    /** Cada cuánto se repite: ver [Repeat]. */
    @androidx.room.ColumnInfo(defaultValue = "0") val repeat: Int = Repeat.NONE,
) {
    val highPriority get() = priority > 0
}

/** Repetición de un compromiso: al marcarlo como hecho se crea el siguiente. */
object Repeat {
    const val NONE = 0
    const val DAILY = 1
    const val WEEKLY = 2
    const val MONTHLY = 3

    val labels = linkedMapOf(NONE to "No se repite", DAILY to "Cada día", WEEKLY to "Cada semana", MONTHLY to "Cada mes")

    fun next(day: java.time.LocalDate, mode: Int): java.time.LocalDate? = when (mode) {
        DAILY -> day.plusDays(1)
        WEEKLY -> day.plusWeeks(1)
        MONTHLY -> day.plusMonths(1)
        else -> null
    }
}

/** Sesión de gimnasio de un día. */
@Serializable
@Entity(tableName = "gym_sessions", indices = [Index("epochDay")])
data class GymSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val notes: String = "",
    /** Peso corporal en kg el día de la sesión (para la fuerza relativa). */
    val bodyWeightKg: Double? = null,
)

/** Serie de un ejercicio dentro de una sesión. */
@Serializable
@Entity(tableName = "gym_sets", indices = [Index("sessionId")])
data class GymSet(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exercise: String,
    val weightKg: Double,
    val reps: Int,
    val position: Int = 0,
)

/**
 * Movimiento de experiencia y monedas. [key] identifica el origen (por ejemplo "block:12")
 * y es único: así nada se cobra dos veces, y al desmarcar se borra y se devuelve.
 */
@Serializable
@Entity(tableName = "rewards", indices = [Index("key", unique = true), Index("epochDay")])
data class Reward(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val key: String,
    val kind: String,
    val xp: Int,
    /** Monedas concedidas tras aplicar el tope (negativas = gasto en la tienda). */
    val coins: Int,
    /** Si cuenta para los topes diario/semanal/mensual. */
    val capped: Boolean,
    /** Día (epochDay) en que se ganó, para los topes. */
    val epochDay: Long,
    val createdAt: Long,
    val label: String,
)
