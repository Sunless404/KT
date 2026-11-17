package com.sunless404.keeptrack.domain

/** Nivel de una racha según los días seguidos: cuanto más alto, más "arde" su icono. */
enum class StreakLevel(val minDays: Int, val label: String, val color: Long) {
    NONE(0, "Sin racha", 0xFF6B7785),
    SPARK(1, "Chispa", 0xFFFFC857),
    FLAME(3, "Llama", 0xFFFF9F43),
    BONFIRE(7, "Hoguera", 0xFFFF6B35),
    BLAZE(14, "Incendio", 0xFFFF3D3D),
    LEGEND(30, "Leyenda", 0xFFB46CFF);

    val next: StreakLevel? get() = entries.getOrNull(ordinal + 1)

    companion object {
        fun of(days: Int): StreakLevel = entries.last { days >= it.minDays }

        /** Avance hacia el siguiente nivel (0..1); 1 en el último nivel. */
        fun progress(days: Int): Float {
            val l = of(days)
            val n = l.next ?: return 1f
            return (days - l.minDays).toFloat() / (n.minDays - l.minDays)
        }
    }
}
