package com.sunless404.rutina.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Categorías de bloque. [color] en ARGB. */
enum class Category(val label: String, val color: Long, val notifiesByDefault: Boolean = true) {
    CLASE("Clase", 0xFF3F51B5),
    ACADEMIA("Academia", 0xFFAB47BC),
    ESTUDIO("Estudio", 0xFF1E88E5),
    GIMNASIO("Gimnasio", 0xFFE53935),
    DESPLAZAMIENTO("Desplazamiento", 0xFF78909C),
    COMIDA("Comida", 0xFFFB8C00),
    SUENO("Sueño", 0xFF4527A0, notifiesByDefault = false),
    LECTURA("Lectura", 0xFF00897B),
    HIGIENE("Higiene", 0xFF26C6DA),
    LIBRE("Libre", 0xFF7CB342, notifiesByDefault = false),
    OTRO("Otro", 0xFF8D6E63);
}

enum class Status(val label: String) {
    PENDIENTE("Pendiente"),
    HECHO("Hecho"),
    SALTADO("Saltado"),
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
@Entity(tableName = "days")
data class DayInfo(
    @PrimaryKey val epochDay: Long,
    val type: DayType = DayType.NORMAL,
    val materialized: Boolean = false,
)
