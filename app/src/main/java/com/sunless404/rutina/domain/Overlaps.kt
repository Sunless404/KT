package com.sunless404.rutina.domain

import com.sunless404.rutina.data.Block
import com.sunless404.rutina.data.DAY_MIN

const val WEEK_MIN = 7 * DAY_MIN

/** Inicio del bloque en minutos desde el lunes a las 00:00. */
fun Block.weekStart(): Int = (dayOfWeek - 1) * DAY_MIN + startMin

/** true si los dos bloques coinciden en algún minuto de la semana (que es circular). */
fun overlaps(a: Block, b: Block): Boolean {
    val a0 = a.weekStart()
    val a1 = a0 + a.durationMin
    val b0 = b.weekStart()
    val b1 = b0 + b.durationMin
    return listOf(-WEEK_MIN, 0, WEEK_MIN).any { shift -> a0 < b1 + shift && b0 + shift < a1 }
}

/** Parejas de índices de [blocks] que se solapan. */
fun overlappingPairs(blocks: List<Block>): List<Pair<Int, Int>> {
    val out = ArrayList<Pair<Int, Int>>()
    for (i in blocks.indices) for (j in i + 1 until blocks.size) {
        if (overlaps(blocks[i], blocks[j])) out += i to j
    }
    return out
}

/** Bloques de [others] que se solapan con [block] (se ignora el propio bloque por id). */
fun conflictsOf(block: Block, others: List<Block>): List<Block> =
    others.filter { (it.id == 0L || it.id != block.id) && it !== block && overlaps(block, it) }
