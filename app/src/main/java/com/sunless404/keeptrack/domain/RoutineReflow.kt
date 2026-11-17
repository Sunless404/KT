package com.sunless404.keeptrack.domain

import com.sunless404.keeptrack.data.Block
import com.sunless404.keeptrack.data.Category

/** Qué hacer con el tiempo que deja libre un bloque borrado. */
enum class ReflowMode(val label: String, val description: String) {
    SPREAD("Repartir", "El tiempo se reparte entre los bloques flexibles de ese tramo"),
    PULL_FORWARD("Adelantar lo siguiente", "Lo que va después empieza antes; el tiempo queda al final del tramo"),
    KEEP_GAP("Dejar el hueco libre", "No se mueve nada"),
}

/**
 * Resultado de reajustar un día. [blocks] es el día completo resultante; [changed], los bloques
 * que cambian (antes → después). Si [missingMin] > 0, no cabe y no debe aplicarse.
 */
data class ReflowResult(
    val blocks: List<Block>,
    val changed: List<Pair<Block, Block>>,
    val missingMin: Int = 0,
    val reason: String? = null,
) {
    val ok get() = missingMin == 0 && reason == null
}

/**
 * Reajusta un día de la rutina cuando se borra, añade o alarga un bloque.
 *
 * Los bloques fijos son anclas: los cambios solo afectan al tramo entre dos fijos. El sueño
 * tiene la hora de despertar fija y la de acostarse flexible, y nunca baja de [MIN_SLEEP].
 * Los flexibles no bajan de la mitad de lo que duran (ni de 15 min).
 */
object RoutineReflow {
    const val MIN_SLEEP = 8 * 60

    private class Item(val block: Block, var start: Int, var dur: Int) {
        val sleep get() = block.category == Category.SUENO
        val min get() = when {
            sleep -> if (block.durationMin >= MIN_SLEEP) MIN_SLEEP else block.durationMin
            else -> maxOf(minOf(15, block.durationMin), block.durationMin / 2)
        }
        val end get() = start + dur
    }

    /** Un tramo: bloques flexibles entre [l] y [r]. Si el último es el sueño, termina justo en [r]. */
    private class Stretch(val l: Int, val r: Int, val items: MutableList<Item>)

    private fun isAnchor(b: Block) = b.category != Category.SUENO && (b.fixed || b.crossesMidnight)

    private fun absEnd(b: Block) = b.startMin + b.durationMin

    /** Divide el día en tramos separados por anclas (fijos y finales de sueño). */
    private fun stretches(day: List<Block>, morning: Int, extraAnchor: Pair<Int, Int>? = null): List<Stretch> {
        val out = ArrayList<Stretch>()
        var l = morning
        var cur = ArrayList<Item>()
        fun close(r: Int, next: Int) {
            out += Stretch(l, r, cur)
            cur = ArrayList()
            l = next
        }
        val events = day.sortedBy { it.startMin }
        var anchorDone = extraAnchor == null
        for (b in events) {
            if (!anchorDone && b.startMin >= extraAnchor!!.first) {
                close(extraAnchor.first, extraAnchor.second)
                anchorDone = true
            }
            when {
                isAnchor(b) -> close(b.startMin, absEnd(b))
                b.category == Category.SUENO -> {
                    // El sueño que pisa el bloque añadido pasa al tramo siguiente.
                    if (!anchorDone && absEnd(b) > extraAnchor!!.first) {
                        close(extraAnchor.first, extraAnchor.second)
                        anchorDone = true
                    }
                    cur += Item(b, b.startMin, b.durationMin)
                    close(absEnd(b), absEnd(b))
                }
                else -> cur += Item(b, b.startMin, b.durationMin)
            }
        }
        if (!anchorDone) close(extraAnchor!!.first, extraAnchor.second)
        close(maxOf(DAY, cur.maxOfOrNull { it.end } ?: DAY, l), l)
        return out
    }

    /** Huecos entre bloques de un tramo (n + 1 valores, nunca negativos). */
    private fun gaps(s: Stretch): IntArray {
        val it = s.items
        val g = IntArray(it.size + 1)
        if (it.isEmpty()) { g[0] = s.r - s.l; return g }
        g[0] = it.first().start - s.l
        for (i in 0 until it.size - 1) g[i + 1] = it[i + 1].start - it[i].end
        g[it.size] = s.r - it.last().end
        for (i in g.indices) if (g[i] < 0) g[i] = 0
        return g
    }

    private fun pack(s: Stretch, g: IntArray) {
        var pos = s.l + g[0]
        s.items.forEachIndexed { i, item ->
            item.start = pos
            pos += item.dur + g[i + 1]
        }
    }

    /** Reparte [amount] minutos (positivo alarga, negativo acorta) entre [items] según su duración o su margen. */
    private fun distribute(items: List<Item>, amount: Int): Int {
        if (items.isEmpty() || amount == 0) return amount
        var left = amount
        if (amount > 0) {
            val total = items.sumOf { it.dur }
            items.forEachIndexed { i, item ->
                val share = if (i == items.lastIndex) left else (amount.toLong() * item.dur / total).toInt()
                item.dur += share
                left -= share
            }
            return 0
        }
        var need = -amount
        val cap = items.sumOf { it.dur - it.min }
        if (cap <= 0) return -need
        val take = minOf(need, cap)
        var taken = 0
        items.forEachIndexed { i, item ->
            val room = item.dur - item.min
            val share = if (i == items.lastIndex) minOf(room, take - taken) else (take.toLong() * room / cap).toInt()
            item.dur -= share
            taken += share
        }
        // Redondeos: lo que falte, de quien aún tenga margen.
        for (item in items) {
            if (taken >= take) break
            val more = minOf(item.dur - item.min, take - taken)
            item.dur -= more
            taken += more
        }
        need -= taken
        return -need
    }

    /**
     * Encaja un tramo que se ha quedado corto: primero usa los huecos (desde el lado del bloque
     * añadido), luego acorta los flexibles y, por último, retrasa la hora de dormir.
     * Devuelve los minutos que no caben.
     */
    private fun fit(s: Stretch, forward: Boolean): Int {
        val items = s.items
        if (items.isEmpty()) return 0
        val g = gaps(s)
        val avail = s.r - s.l
        var deficit = items.sumOf { it.dur } - avail
        if (deficit > 0) {
            deficit = -distribute(items.filter { !it.sleep }, -deficit)
            if (deficit > 0) deficit = -distribute(items.filter { it.sleep }, -deficit)
            if (deficit > 0) return deficit
        }
        val free = avail - items.sumOf { it.dur }
        var excess = g.sum() - free
        val order = if (forward) g.indices else g.indices.reversed()
        for (i in order) {
            if (excess <= 0) break
            val cut = minOf(g[i], excess)
            g[i] -= cut
            excess -= cut
        }
        if (excess < 0) {
            // Ha sobrado sitio al acortar: queda como hueco al final (antes del sueño, si lo hay).
            val at = if (items.last().sleep) items.size - 1 else items.size
            g[at] += -excess
        }
        pack(s, g)
        return 0
    }

    private fun result(original: List<Block>, stretches: List<Stretch>, extra: List<Block> = emptyList(), missing: Int = 0, reason: String? = null): ReflowResult {
        val moved = stretches.flatMap { it.items }.associate { it.block to it }
        val blocks = original.map { b ->
            val m = moved[b] ?: return@map b
            b.copy(startMin = m.start % DAY, endMin = (m.start + m.dur) % DAY)
        } + extra
        val changed = original.zip(blocks).filter { (a, b) -> a.startMin != b.startMin || a.endMin != b.endMin }
        return ReflowResult(blocks.sortedBy { it.startMin }, changed, missing, reason)
    }

    /** Borra [removed] de [day] y reparte el tiempo libre según [mode]. [morning]: hora de despertar de ese día. */
    fun remove(day: List<Block>, removed: Block, mode: ReflowMode, morning: Int = 0): ReflowResult {
        val rest = day.filter { it !== removed && it.id.let { id -> id == 0L || id != removed.id } }
        if (mode == ReflowMode.KEEP_GAP) return ReflowResult(rest.sortedBy { it.startMin }, emptyList())
        val all = stretches(rest, morning)
        val s = all.firstOrNull { removed.startMin >= it.l && removed.startMin < it.r }
            ?: return ReflowResult(rest.sortedBy { it.startMin }, emptyList())
        val freed = removed.durationMin
        val g = gaps(s)
        val k = s.items.count { it.start < removed.startMin }
        val cut = minOf(freed, g[k])
        g[k] -= cut
        val flexible = s.items.filter { !it.sleep }
        val sleep = s.items.lastOrNull()?.takeIf { it.sleep }
        when (mode) {
            ReflowMode.SPREAD -> when {
                flexible.isNotEmpty() -> distribute(flexible, cut)
                sleep != null -> sleep.dur += cut
                else -> g[k] += cut
            }
            ReflowMode.PULL_FORWARD -> if (sleep != null) sleep.dur += cut else g[s.items.size] += cut
            ReflowMode.KEEP_GAP -> Unit
        }
        pack(s, g)
        return result(rest, all)
    }

    /**
     * Coloca [added] (nuevo o con su hora cambiada) a su hora y hace sitio en los tramos de al lado.
     * Si [added] ya estaba en el día (mismo id), se sustituye.
     */
    fun place(day: List<Block>, added: Block, morning: Int = 0): ReflowResult {
        val rest = day.filter { added.id == 0L || it.id != added.id }
        val s = added.startMin
        val e = s + added.durationMin
        // Pisar un bloque fijo no se puede arreglar moviendo nada.
        rest.filter { isAnchor(it) }.firstOrNull { it.startMin < e && s < absEnd(it) }?.let { f ->
            return ReflowResult(rest + added, emptyList(), minOf(e, absEnd(f)) - maxOf(s, f.startMin), "Pisa «${f.title}», que es fijo")
        }
        if (s < morning) return ReflowResult(rest + added, emptyList(), morning - s, "Empieza antes de que termine el sueño de la noche anterior")
        val all = stretches(rest, morning, s to e)
        var missing = 0
        all.forEach { st ->
            val overlaps = st.items.any { it.start < st.l || it.end > st.r } ||
                st.items.zipWithNext().any { (a, b) -> a.end > b.start }
            if (overlaps) missing += fit(st, forward = st.l == e)
        }
        val sleepShort = all.flatMap { it.items }.any { it.sleep && it.dur < it.min }
        return when {
            missing > 0 -> ReflowResult(rest + added, emptyList(), missing, "No hay sitio: faltan ${fmtDuration(missing)} sin tocar los fijos ni bajar de 8 h de sueño")
            sleepShort -> ReflowResult(rest + added, emptyList(), 1, "El sueño bajaría de 8 h")
            else -> result(rest, all, extra = listOf(added))
        }
    }

    private const val DAY = 1440
}
