package com.sunless404.keeptrack.domain

/**
 * Nivel de la llama de una racha según los días seguidos. No tiene nombre visible:
 * solo cambia el color y la animación de la llama.
 * Los nombres de las constantes no se cambian: forman parte de las claves de las recompensas ya cobradas.
 */
enum class StreakLevel(val minDays: Int, val color: Long, val core: Long) {
    NONE(0, 0xFF6B7785, 0xFF9AA5B1),
    SPARK(1, 0xFFFF9F43, 0xFFFFE08A),
    FLAME(3, 0xFFFF6B1A, 0xFFFFD25A),
    BONFIRE(7, 0xFFFF3B2F, 0xFFFFB347),
    BLAZE(14, 0xFFFF2D87, 0xFFFFB3D9),
    LEGEND(30, 0xFFA855F7, 0xFFE9C6FF),
    NOVA(60, 0xFF3B82F6, 0xFFBFE3FF),
    SUPERNOVA(100, 0xFFFFE9A8, 0xFFFFFFFF);

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
