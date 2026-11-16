package com.sunless404.keeptrack.domain

import com.sunless404.keeptrack.data.Block
import com.sunless404.keeptrack.data.Category
import com.sunless404.keeptrack.data.DAY_MIN

/** Hueco sin asignar dentro de un día, en minutos desde las 00:00 ([end] puede ser 1440). */
data class Gap(val start: Int, val end: Int)

data class Analysis(
    val overlaps: List<Pair<Block, Block>>,
    /** Huecos por día (1..7), solo para los días que tienen algún bloque. */
    val gaps: Map<Int, List<Gap>>,
    /** Minutos de sueño de la noche que empieza cada día (1..7). */
    val sleepByNight: Map<Int, Int>,
)

object RoutineAnalysis {

    fun analyze(blocks: List<Block>): Analysis = Analysis(
        overlaps = overlappingPairs(blocks).map { (i, j) -> blocks[i] to blocks[j] },
        gaps = gaps(blocks),
        sleepByNight = sleepByNight(blocks),
    )

    fun gaps(blocks: List<Block>): Map<Int, List<Gap>> {
        val covered = BooleanArray(WEEK_MIN)
        blocks.forEach { b ->
            val s = b.weekStart()
            for (m in s until s + b.durationMin) covered[m % WEEK_MIN] = true
        }
        return blocks.map { it.dayOfWeek }.distinct().sorted().associateWith { day ->
            val base = (day - 1) * DAY_MIN
            val out = ArrayList<Gap>()
            var start = -1
            for (m in 0..DAY_MIN) {
                val free = m < DAY_MIN && !covered[base + m]
                if (free && start < 0) start = m
                if (!free && start >= 0) {
                    out += Gap(start, m)
                    start = -1
                }
            }
            out
        }
    }

    /**
     * Sueño por noche. Un bloque de sueño que empieza antes de las 12:00 cuenta
     * para la noche del día anterior (p. ej. acostarse a las 00:30).
     */
    fun sleepByNight(blocks: List<Block>): Map<Int, Int> {
        val out = sortedMapOf<Int, Int>()
        blocks.filter { it.category == Category.SUENO }.forEach { b ->
            val night = if (b.startMin < 12 * 60) (b.dayOfWeek + 5) % 7 + 1 else b.dayOfWeek
            out[night] = (out[night] ?: 0) + b.durationMin
        }
        return out
    }

    /** Bloques de sueño de la plantilla que coinciden con [candidate]. */
    fun sleepConflicts(candidate: Block, blocks: List<Block>): List<Block> =
        blocks.filter { it.category == Category.SUENO && it.id != candidate.id && overlaps(candidate, it) }
}
