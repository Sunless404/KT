package com.sunless404.keeptrack.domain

import com.sunless404.keeptrack.data.Category
import com.sunless404.keeptrack.data.DayEntry
import com.sunless404.keeptrack.data.Status

/**
 * Lógica de imprevistos: cambios que solo afectan a un día.
 * Todos los minutos son desde las 00:00 del día; [carry] son los bloques del
 * día anterior que terminan en este (su endMin pasa de 1440).
 */
object Unforeseen {

    private fun busy(entries: List<DayEntry>, carry: List<DayEntry>, ignoreId: Long): List<Gap> =
        (entries.filter { it.id != ignoreId && it.status != Status.SALTADO }.map { Gap(it.startMin, it.endMin) } +
            carry.map { Gap(0, it.endMin - 1440) }).sortedBy { it.start }

    /**
     * Huecos libres de hoy desde [fromMin] donde cabe un bloque de [durationMin].
     * El sueño cuenta como ocupado, así que nunca se sugiere nada dentro de él.
     */
    fun freeSlots(
        entries: List<DayEntry>,
        carry: List<DayEntry>,
        fromMin: Int,
        durationMin: Int,
        ignoreId: Long = 0,
    ): List<Gap> {
        val out = ArrayList<Gap>()
        var cursor = fromMin
        for (b in busy(entries, carry, ignoreId)) {
            if (b.start - cursor >= durationMin) out += Gap(cursor, b.start)
            cursor = maxOf(cursor, b.end)
        }
        if (1440 - cursor >= durationMin) out += Gap(cursor, 1440)
        return out
    }

    /** Bloques de sueño que pisaría un bloque colocado en [start]..[end]. */
    fun sleepHits(entries: List<DayEntry>, carry: List<DayEntry>, start: Int, end: Int, ignoreId: Long = 0): List<DayEntry> =
        entries.filter { it.category == Category.SUENO && it.id != ignoreId && start < it.endMin && it.startMin < end } +
            carry.filter { it.category == Category.SUENO && start < it.endMin - 1440 }

    fun move(entry: DayEntry, newStart: Int): DayEntry =
        entry.copy(startMin = newStart, endMin = newStart + entry.durationMin)

    fun shorten(entry: DayEntry, newDurationMin: Int): DayEntry =
        entry.copy(endMin = entry.startMin + newDurationMin.coerceIn(1, entry.durationMin))

    /** Bloques que aún no han empezado y siguen pendientes; el sueño no se retrasa. */
    fun remaining(entries: List<DayEntry>, nowMin: Int): List<DayEntry> = entries.filter {
        it.startMin >= nowMin && it.category != Category.SUENO &&
            (it.status == Status.PENDIENTE || it.status == Status.POSPUESTO)
    }

    /** Retrasa [minutes] todo lo que queda del día. Devuelve solo las entradas cambiadas. */
    fun delayRest(entries: List<DayEntry>, nowMin: Int, minutes: Int): List<DayEntry> =
        remaining(entries, nowMin).map { move(it, it.startMin + minutes) }

    /** true si, tras aplicar [changed], algún bloque cambiado entra en un bloque de sueño. */
    fun entersSleep(entries: List<DayEntry>, carry: List<DayEntry>, changed: List<DayEntry>): Boolean =
        changed.any { sleepHits(entries, carry, it.startMin, it.endMin, it.id).isNotEmpty() }

    /** Retrasa [minutes] el bloque [from] y los pendientes que empiezan después; el sueño no se mueve. */
    fun delayFrom(entries: List<DayEntry>, from: DayEntry, minutes: Int): List<DayEntry> =
        entries.filter {
            (it.id == from.id || it.startMin > from.startMin) && it.category != Category.SUENO &&
                (it.status == Status.PENDIENTE || it.status == Status.POSPUESTO)
        }.map { move(it, it.startMin + minutes) }
}
