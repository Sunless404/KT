package com.sunless404.keeptrack.domain

import com.sunless404.keeptrack.data.Block
import com.sunless404.keeptrack.data.Category
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import java.text.Normalizer

enum class ImportFormat(val label: String) { TEXTO("Texto"), JSON("JSON") }

/** Línea (o bloque JSON) que no se ha podido entender. [line] empieza en 1. */
data class LineError(val line: Int, val text: String, val reason: String)

data class ParseResult(
    val format: ImportFormat,
    val blocks: List<Block>,
    val errors: List<LineError>,
)

/** Convierte texto libre (formato A) o JSON (formato B) en bloques de rutina. */
object RoutineParser {

    fun parse(input: String): ParseResult {
        val payload = extractPayload(input)
        val trimmed = payload.trim()
        return when {
            trimmed.startsWith("{") || trimmed.startsWith("[") -> parseJson(trimmed)
            looksLikeCsv(trimmed) -> parseText(csvToTable(trimmed))
            else -> parseText(payload)
        }
    }

    private fun stripCodeFence(s: String): String {
        if (!s.startsWith("```")) return s
        return s.lines().drop(1).dropLastWhile { it.trim().startsWith("```") || it.isBlank() }.joinToString("\n").trim()
    }

    /**
     * Respuestas copiadas de una IA: se queda con el bloque de código (```json …```) o con el
     * objeto JSON que contiene "bloques", y descarta el texto de alrededor.
     */
    fun extractPayload(input: String): String {
        val t = input.trim()
        if (t.startsWith("```")) return stripCodeFence(t)
        Regex("```[a-zA-Z]*\\s*\\n([\\s\\S]*?)```").find(t)?.let { return it.groupValues[1] }
        if (!t.startsWith("{") && !t.startsWith("[")) {
            val key = Regex("\"(?:bloques|blocks)\"").find(t)
            if (key != null) {
                val open = t.lastIndexOf('{', key.range.first)
                val close = t.lastIndexOf('}')
                if (open >= 0 && close > open) return t.substring(open, close + 1)
            }
        }
        return input
    }

    // ---------- CSV (Excel, Google Sheets) ----------

    private fun csvSeparator(line: String): Char? =
        listOf('\t', ';', ',').firstOrNull { sep -> line.count { it == sep } >= 1 && splitCsv(line, sep).size >= 2 }

    /** CSV con cabecera reconocible (hora, inicio, actividad, día o nombres de días). */
    fun looksLikeCsv(text: String): Boolean {
        val first = text.lineSequence().firstOrNull { it.isNotBlank() } ?: return false
        if ('|' in first) return false
        val sep = csvSeparator(first) ?: return false
        val cells = splitCsv(first, sep).map { normalize(it) }
        return cells.any { it in csvHeaderWords || it.startsWith("hora") } || cells.count { dayTokens[it] != null && it.length > 2 } >= 2
    }

    private val csvHeaderWords = setOf("hora", "horario", "inicio", "fin", "actividad", "titulo", "bloque", "dia", "dias", "categoria", "duracion")

    private fun splitCsv(line: String, sep: Char): List<String> {
        val out = ArrayList<String>()
        val cur = StringBuilder()
        var quoted = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            when {
                ch == '"' && quoted && line.getOrNull(i + 1) == '"' -> { cur.append('"'); i++ }
                ch == '"' -> quoted = !quoted
                ch == sep && !quoted -> { out += cur.toString().trim(); cur.clear() }
                else -> cur.append(ch)
            }
            i++
        }
        out += cur.toString().trim()
        return out
    }

    /** Convierte el CSV en una tabla Markdown, que ya sabe leer el formato de texto. */
    private fun csvToTable(text: String): String {
        val first = text.lineSequence().first { it.isNotBlank() }
        val sep = csvSeparator(first) ?: return text
        return text.lines().joinToString("\n") { line ->
            if (line.isBlank()) "" else "| " + splitCsv(line, sep).joinToString(" | ") { it.replace("|", "/") } + " |"
        }
    }

    // ---------- Utilidades ----------

    fun normalize(s: String): String =
        Normalizer.normalize(s.lowercase(ES), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "").trim()

    private val dayTokens: Map<String, Int> = buildMap {
        listOf(
            listOf("lunes", "lun", "lu", "l"),
            listOf("martes", "mar", "ma", "m"),
            listOf("miercoles", "mie", "mi", "x"),
            listOf("jueves", "jue", "ju", "j"),
            listOf("viernes", "vie", "vi", "v"),
            listOf("sabado", "sab", "sa", "s"),
            listOf("domingo", "dom", "do", "d"),
        ).forEachIndexed { i, names -> names.forEach { put(it, i + 1) } }
    }
    private val rangeWords = setOf("a", "al", "hasta", "-", "–", "—", "−")
    private val listWords = setOf(",", "y", "e", "/", "&", "+")
    private val fillerWords = setOf("de", "del", "los", "el")

    /**
     * Días que nombra un texto como "Lunes", "mié", "Lunes a viernes",
     * "L-V" o "martes y jueves". Devuelve null si no es una lista de días.
     */
    fun parseDays(raw: String): Set<Int>? {
        val s = normalize(raw.replace(Regex("[#*_`>:.]"), " "))
        if (s.isEmpty()) return null
        when (s) {
            "todos los dias", "todos", "diario", "cada dia", "a diario" -> return (1..7).toSet()
            "entre semana", "laborables", "dias laborables" -> return (1..5).toSet()
            "fin de semana", "finde" -> return setOf(6, 7)
        }
        // Cualquier otro carácter (dígitos, paréntesis…) indica que no es una cabecera de día.
        if (!s.matches(Regex("[a-z\\s\\-–—−,/&+]+"))) return null
        val tokens = Regex("[a-z]+|[-–—−,/&+]").findAll(s).map { it.value }.filter { it !in fillerWords }.toList()
        if (tokens.isEmpty()) return null
        val out = LinkedHashSet<Int>()
        var i = 0
        while (i < tokens.size) {
            val from = dayTokens[tokens[i]] ?: return null
            if (i + 2 < tokens.size && tokens[i + 1] in rangeWords) {
                val to = dayTokens[tokens.getOrNull(i + 2) ?: return null] ?: return null
                var d = from
                while (true) {
                    out += d
                    if (d == to) break
                    d = d % 7 + 1
                }
                i += 3
            } else {
                out += from
                i += 1
            }
            if (i < tokens.size) {
                if (tokens[i] !in listWords) return null
                i += 1
                if (i >= tokens.size) return null
            }
        }
        // Una letra suelta ("a", "y", "m"…) es demasiado ambigua salvo en rangos o listas.
        if (tokens.size == 1 && tokens[0].length == 1) return null
        return out
    }

    private const val TIME = """(\d{1,2})(?:\s*(?:[:.]|h)\s*(\d{2}))?(?:\s*h\b)?"""
    private val timeOnly = Regex("^$TIME$", RegexOption.IGNORE_CASE)
    private val rangeRegex = Regex(
        """^$TIME\s*(?:[-–—−]|\s+a\s+|\s+hasta\s+)\s*$TIME(?![0-9])(.*)$""",
        RegexOption.IGNORE_CASE,
    )

    private fun toMin(h: String, m: String?): Int? {
        val hh = h.toInt()
        val mm = m?.toIntOrNull() ?: 0
        if (mm > 59) return null
        if (hh == 24 && mm == 0) return 0
        if (hh > 23) return null
        return hh * 60 + mm
    }

    /** "08:00", "8h", "8h30", "8.30" → minutos; null si no es una hora válida. */
    fun parseTime(raw: String): Int? {
        val m = timeOnly.find(raw.trim()) ?: return null
        return toMin(m.groupValues[1], m.groupValues[2].ifEmpty { null })
    }

    /** "1h", "45 min", "1h 30min", "1,5 h", "1:30" → minutos. */
    fun parseDuration(raw: String): Int? {
        val s = normalize(raw)
        Regex("""^(\d+):(\d{2})$""").find(s)?.let { return it.groupValues[1].toInt() * 60 + it.groupValues[2].toInt() }
        Regex("""^(\d+(?:[.,]\d+)?)\s*h(?:oras?|rs?)?\.?\s*(?:y\s*)?(?:(\d+)\s*(?:m|min|mins|minutos|')?\.?)?$""").find(s)?.let {
            val h = it.groupValues[1].replace(',', '.').toDouble()
            val m = it.groupValues[2].toIntOrNull() ?: 0
            return (h * 60).toInt() + m
        }
        Regex("""^(\d+)\s*(?:m|min|mins|minutos|')\.?$""").find(s)?.let { return it.groupValues[1].toInt() }
        return null
    }

    private val categoryNames: Map<String, Category> = buildMap {
        Category.entries.forEach { put(normalize(it.label), it); put(it.name.lowercase(), it) }
        put("clases", Category.CLASE)
        put("gym", Category.GIMNASIO); put("deporte", Category.GIMNASIO)
        put("transporte", Category.DESPLAZAMIENTO); put("trayecto", Category.DESPLAZAMIENTO)
        put("dormir", Category.SUENO)
        put("leer", Category.LECTURA)
        put("ocio", Category.LIBRE); put("descanso", Category.LIBRE); put("tiempo libre", Category.LIBRE)
    }

    private val keywords: List<Pair<Category, List<String>>> = listOf(
        Category.SUENO to listOf("dormir", "sueno", "siesta", "acostar"),
        Category.GIMNASIO to listOf("gimnasio", "gym", "entren", "deporte", "correr", "pesas", "natacion", "futbol", "yoga", "crossfit"),
        Category.ACADEMIA to listOf("academia"),
        Category.DESPLAZAMIENTO to listOf("ida", "vuelta", "trayecto", "desplaz", "bus", "autobus", "metro", "tren", "camino", "viaje", "transporte", "coche"),
        Category.COMIDA to listOf("desayun", "comida", "comer", "almuerzo", "almorzar", "cena", "merienda", "merendar"),
        Category.LECTURA to listOf("lectura", "leer", "libro"),
        Category.HIGIENE to listOf("ducha", "aseo", "higiene", "dientes", "afeitar"),
        Category.ESTUDIO to listOf("estudi", "deberes", "repaso", "repasar", "tarea", "examen", "apuntes"),
        Category.LIBRE to listOf("libre", "ocio", "descanso", "descansar", "relax", "juegos", "series", "tele"),
        Category.CLASE to listOf("clase", "instituto", "universidad", "facultad", "colegio", "asignatura"),
    )

    /** Categoría escrita por el usuario ("clase", "Sueño", "gym"…); null si no se reconoce. */
    fun categoryFromName(raw: String): Category? = categoryNames[normalize(raw)]

    /** Deduce la categoría a partir de palabras clave del título. */
    fun inferCategory(title: String): Category {
        val t = normalize(title)
        for ((cat, words) in keywords) {
            if (words.any { Regex("\\b" + Regex.escape(it)).containsMatchIn(t) }) return cat
        }
        return Category.OTRO
    }

    private fun resolveCategory(explicit: String?, title: String): Category {
        if (explicit.isNullOrBlank()) return inferCategory(title)
        return categoryFromName(explicit) ?: inferCategory(explicit).takeIf { it != Category.OTRO } ?: inferCategory(title)
    }

    // ---------- Formato A: texto ----------

    private class Pending(val line: Int, val text: String, val days: Set<Int>, val start: Int, val title: String, val cat: String?)

    private val bracket = Regex("""\[([^\]]*)]""")

    private fun parseText(input: String): ParseResult {
        val blocks = ArrayList<Block>()
        val errors = ArrayList<LineError>()
        var days: Set<Int>? = null
        // Columnas de la tabla Markdown en curso.
        var cols: Map<String, Int> = defaultCols
        // Horario en cuadrícula: columna -> día (cabecera "Hora | Lunes | Martes …").
        var wide: Map<Int, Int>? = null
        // Celdas de la fila anterior de la cuadrícula que no tenían hora de fin.
        var widePending: List<Pending> = emptyList()
        // Fila de tabla con hora de inicio pero sin fin ni duración: termina donde empiece la siguiente.
        var pending: Pending? = null

        fun add(line: Int, text: String, d: Set<Int>, start: Int, end: Int, title: String, cat: String?) {
            if (start == end) {
                errors += LineError(line, text, "La hora de inicio y la de fin son iguales")
                return
            }
            d.sorted().forEach { day ->
                blocks += Block(
                    title = title, category = resolveCategory(cat, title),
                    dayOfWeek = day, startMin = start, endMin = end,
                )
            }
        }

        fun flushPending(nextStart: Int?) {
            val p = pending ?: return
            pending = null
            if (nextStart == null) errors += LineError(p.line, p.text, "Falta la hora de fin o la duración")
            else add(p.line, p.text, p.days, p.start, nextStart, p.title, p.cat)
        }

        input.lines().forEachIndexed { idx, rawLine ->
            val lineNo = idx + 1
            val line = rawLine.trim()
            if (line.isEmpty()) return@forEachIndexed

            // Tabla Markdown
            if (line.startsWith("|") || line.count { it == '|' } >= 2) {
                val cells = line.trim('|').split('|').map { it.trim() }
                if (cells.all { it.matches(Regex(":?-{2,}:?")) || it.isEmpty() }) return@forEachIndexed
                val norm = cells.map { normalize(it.replace("*", "")) }
                val dayCols = norm.mapIndexedNotNull { i, c -> dayTokens[c]?.takeIf { c.length > 2 }?.let { i to it } }.toMap()
                if (dayCols.size >= 2) {
                    wide = dayCols
                    widePending = emptyList()
                    cols = headerCols(norm)
                    return@forEachIndexed
                }
                if (norm.any { it == "hora" || it == "horario" || it == "actividad" || it == "inicio" || it.startsWith("hora ") }) {
                    cols = headerCols(norm)
                    wide = null
                    return@forEachIndexed
                }
                wide?.let { dayMap ->
                    val timeCol = cols["hora"] ?: 0
                    val hora = cells.getOrNull(timeCol).orEmpty()
                    val range = rangeRegex.find(hora)?.takeIf { it.groupValues[5].isBlank() }
                    val s = range?.let { toMin(it.groupValues[1], it.groupValues[2].ifEmpty { null }) } ?: parseTime(hora)
                    val e = range?.let { toMin(it.groupValues[3], it.groupValues[4].ifEmpty { null }) }
                    if (s == null) {
                        errors += LineError(lineNo, rawLine, "Fila sin hora válida")
                        return@forEachIndexed
                    }
                    widePending.forEach { p -> add(p.line, p.text, p.days, p.start, s, p.title, p.cat) }
                    widePending = emptyList()
                    val next = ArrayList<Pending>()
                    dayMap.forEach { (col, day) ->
                        var title = cells.getOrNull(col)?.replace("*", "")?.trim().orEmpty()
                        if (title.isEmpty() || title.all { it in "-–—·." }) return@forEach
                        var cat: String? = null
                        bracket.find(title)?.let { m -> cat = m.groupValues[1]; title = title.replace(m.value, "").trim() }
                        if (e != null) add(lineNo, rawLine, setOf(day), s, e, title, cat)
                        else next += Pending(lineNo, rawLine, setOf(day), s, title, cat)
                    }
                    widePending = next
                    return@forEachIndexed
                }
                fun cell(name: String) = cols[name]?.let { cells.getOrNull(it) }?.takeIf { it.isNotBlank() }
                val rowDays = cell("dia")?.let { parseDays(it) ?: dayTokens[normalize(it)]?.let { d -> setOf(d) } } ?: days
                val hora = cell("hora") ?: cell("inicio")?.let { st -> cell("fin")?.let { "$st-$it" } ?: st }
                var title = cell("actividad")?.replace("*", "")?.trim().orEmpty()
                var cat = cell("categoria")
                bracket.find(title)?.let { m ->
                    cat = cat ?: m.groupValues[1]
                    title = title.replace(m.value, "").trim()
                }
                when {
                    hora == null -> errors += LineError(lineNo, rawLine, "Fila sin hora")
                    title.isEmpty() -> errors += LineError(lineNo, rawLine, "Fila sin actividad")
                    rowDays == null -> errors += LineError(lineNo, rawLine, "Antes de esta fila falta el nombre del día")
                    else -> {
                        val range = rangeRegex.find(hora)
                        if (range != null && range.groupValues[5].isBlank()) {
                            val s = toMin(range.groupValues[1], range.groupValues[2].ifEmpty { null })
                            val e = toMin(range.groupValues[3], range.groupValues[4].ifEmpty { null })
                            if (s == null || e == null) errors += LineError(lineNo, rawLine, "Hora no válida")
                            else {
                                flushPending(s)
                                add(lineNo, rawLine, rowDays, s, e, title, cat)
                            }
                        } else {
                            val s = parseTime(hora)
                            if (s == null) errors += LineError(lineNo, rawLine, "Hora no válida")
                            else {
                                flushPending(s)
                                val dur = cell("duracion")?.let { parseDuration(it) }
                                if (dur != null && dur in 1..1440) add(lineNo, rawLine, rowDays, s, (s + dur) % 1440, title, cat)
                                else pending = Pending(lineNo, rawLine, rowDays, s, title, cat)
                            }
                        }
                    }
                }
                return@forEachIndexed
            }

            // Fuera de una tabla: lo pendiente ya no puede cerrarse con la fila siguiente.
            val body = line.replace(Regex("^(?:[-*•·]\\s+|\\d+[.)]\\s+)"), "")
            val range = rangeRegex.find(body) ?: rangeRegex.find(line)
            if (range != null) {
                val s = toMin(range.groupValues[1], range.groupValues[2].ifEmpty { null })
                val e = toMin(range.groupValues[3], range.groupValues[4].ifEmpty { null })
                var rest = range.groupValues[5].trim().trimStart(':', '-', '–', '—', '|', '·', '.', ')').trim()
                var cat: String? = null
                bracket.findAll(rest).lastOrNull()?.let { m ->
                    cat = m.groupValues[1]
                    rest = rest.replace(m.value, "").trim()
                }
                val d = days
                when {
                    s == null || e == null -> errors += LineError(lineNo, rawLine, "Hora no válida")
                    rest.isEmpty() -> errors += LineError(lineNo, rawLine, "Falta el título del bloque")
                    d == null -> errors += LineError(lineNo, rawLine, "Antes de esta línea falta el nombre del día")
                    else -> {
                        flushPending(s)
                        add(lineNo, rawLine, d, s, e, rest, cat)
                    }
                }
                return@forEachIndexed
            }

            val header = parseDays(line)
            if (header != null) {
                flushPending(null)
                days = header
                cols = defaultCols
                wide = null
                return@forEachIndexed
            }
            val phrase = parsePhrase(body)
            if (phrase != null) {
                val d = phrase.days ?: days
                when {
                    phrase.title.isEmpty() -> errors += LineError(lineNo, rawLine, "Falta qué actividad es")
                    d == null -> errors += LineError(lineNo, rawLine, "Falta el día (por ejemplo «lunes a viernes»)")
                    phrase.end == null -> errors += LineError(lineNo, rawLine, "Falta la hora de fin o la duración")
                    else -> {
                        flushPending(phrase.start)
                        add(lineNo, rawLine, d, phrase.start, phrase.end, phrase.title, phrase.cat)
                    }
                }
                return@forEachIndexed
            }
            errors += LineError(lineNo, rawLine, "No se reconoce como día ni como bloque (HH:MM-HH:MM Título)")
        }
        flushPending(null)
        widePending.forEach { errors += LineError(it.line, it.text, "Falta la hora de fin de la última fila") }
        return ParseResult(ImportFormat.TEXTO, blocks, errors.sortedBy { it.line })
    }

    // ---------- Frases ----------

    /** Lo que se ha entendido de una frase como «Gimnasio martes y jueves de 18 a 19:30». */
    data class Phrase(val title: String, val days: Set<Int>?, val start: Int, val end: Int?, val cat: String?)

    private const val DAY_WORD = "(?:lunes|martes|miercoles|jueves|viernes|sabados?|domingos?)"
    private val dayGroup = Regex("""\b$DAY_WORD(?:\s*(?:,|\by\b|\be\b|\ba\b|\bal\b|-|–)\s*$DAY_WORD)*\b""")
    private val specialDays = listOf(
        "todos los dias" to (1..7), "cada dia" to (1..7), "a diario" to (1..7), "diariamente" to (1..7),
        "entre semana" to (1..5), "dias laborables" to (1..5), "de lunes a viernes" to (1..5),
        "los fines de semana" to (6..7), "fines de semana" to (6..7), "fin de semana" to (6..7),
    )
    private const val PT = """(\d{1,2})(?:\s*[:.h]\s*(\d{2}))?(?:\s*h\b)?"""
    private const val PERIOD = """(\s*(?:de la (?:manana|tarde|noche)|por la (?:manana|tarde|noche)|am|pm|a\.m\.|p\.m\.))?"""
    private val phraseRange = Regex("""(?:\bde(?:sde)?\s+(?:las?\s+)?)?\b$PT\s*(?:-|–|—|\ba\b|\bhasta\b)\s*(?:las?\s+)?$PT$PERIOD""")
    private val phraseAt = Regex("""\ba\s+las?\s+$PT$PERIOD""")
    private val phraseDur = Regex("""(?:\bdurante\s+|\bpor\s+)?(\d+(?:[.,]\d+)?\s*h(?:oras?|rs?)?(?:\s*(?:y\s*)?\d{1,2}\s*(?:min|minutos|m)?\b)?|\d+\s*(?:min|minutos)\b|media hora|una hora y media|hora y media|una hora|dos horas)""")
    private val fillers = setOf("de", "del", "los", "las", "el", "la", "cada", "a", "al", "y", "e", "durante", "por", "desde", "hasta", "en", "-", "–", ",", ":")

    private fun withPeriod(min: Int, period: String): Int {
        val p = period.trim()
        val pm = p.contains("tarde") || p.contains("noche") || p.startsWith("pm") || p.startsWith("p.")
        return if (pm && min < 12 * 60) min + 12 * 60 else min
    }

    private fun durationWords(raw: String): Int? = when (val t = raw.trim()) {
        "media hora" -> 30
        "una hora" -> 60
        "hora y media", "una hora y media" -> 90
        "dos horas" -> 120
        else -> parseDuration(t.removePrefix("durante").removePrefix("por").trim())
    }

    /**
     * Frase con los datos dentro, en cualquier orden: «Estudio de lunes a viernes de 8 a 10»,
     * «Gimnasio martes y jueves a las 18:00 durante 1h30», «Leer todos los días 30 min a las 23:00».
     * Devuelve null si la línea no tiene ninguna hora.
     */
    fun parsePhrase(line: String): Phrase? {
        var original = line.trim()
        var cat: String? = null
        bracket.findAll(original).lastOrNull()?.let { m -> cat = m.groupValues[1]; original = original.replace(m.value, " ") }
        val n = normalize(original)
        // Si al normalizar cambia la longitud, el título sale del texto normalizado.
        val source = if (n.length == original.length) original else n
        val cut = BooleanArray(n.length)
        fun remove(r: IntRange) { for (i in r) if (i in cut.indices) cut[i] = true }

        var start: Int? = null
        var end: Int? = null
        val range = phraseRange.find(n)
        if (range != null) {
            val g = range.groupValues
            var s = toMin(g[1], g[2].ifEmpty { null })
            var e = toMin(g[3], g[4].ifEmpty { null })
            if (s != null && e != null && g[5].isNotBlank()) {
                val pe = withPeriod(e, g[5])
                if (pe != e) { e = pe; if (s < 12 * 60 && s + 12 * 60 <= e) s += 12 * 60 }
            }
            start = s; end = e
            remove(range.range)
        } else {
            val at = phraseAt.find(n) ?: return null
            start = toMin(at.groupValues[1], at.groupValues[2].ifEmpty { null })?.let { withPeriod(it, at.groupValues[3]) }
            remove(at.range)
            phraseDur.find(n)?.let { d ->
                val dur = durationWords(d.value)
                if (dur != null && dur in 1..1440 && start != null) end = (start + dur) % 1440
                remove(d.range)
            }
        }
        if (start == null) return null

        var days: Set<Int>? = null
        for ((phrase, range2) in specialDays) {
            val i = n.indexOf(phrase)
            if (i >= 0) { days = range2.toSet(); remove(i until i + phrase.length); break }
        }
        if (days == null) {
            dayGroup.find(n)?.let { m ->
                days = parseDays(m.value.replace("sabados", "sabado").replace("domingos", "domingo"))
                if (days != null) remove(m.range)
            }
        }
        // Expresiones de momento del día que ya no aportan nada al título.
        Regex("""\b(?:por|de) la (?:manana|tarde|noche)\b""").findAll(n).forEach { remove(it.range) }

        val kept = buildString { source.forEachIndexed { i, ch -> append(if (i < cut.size && cut[i]) ' ' else ch) } }
        val words = kept.split(Regex("\\s+")).filter { it.isNotBlank() }.toMutableList()
        while (words.isNotEmpty() && normalize(words.first()).trim(',', '.', ':') in fillers) words.removeAt(0)
        while (words.isNotEmpty() && normalize(words.last()).trim(',', '.', ':') in fillers) words.removeAt(words.lastIndex)
        val title = words.joinToString(" ").trim(',', '.', ':', ';', ' ').replaceFirstChar { it.uppercase() }
        return Phrase(title, days, start, end, cat)
    }

    private val defaultCols = mapOf("hora" to 0, "actividad" to 1, "duracion" to 2)

    private fun headerCols(norm: List<String>): Map<String, Int> = buildMap {
        norm.forEachIndexed { i, c ->
            when {
                c.startsWith("hora") -> putIfAbsent("hora", i)
                c == "inicio" || c == "empieza" || c == "desde" -> putIfAbsent("inicio", i)
                c == "fin" || c == "termina" || c == "hasta" -> putIfAbsent("fin", i)
                c.startsWith("actividad") || c == "bloque" || c == "titulo" || c == "tarea" -> putIfAbsent("actividad", i)
                c.startsWith("duracion") -> putIfAbsent("duracion", i)
                c.startsWith("categoria") || c == "tipo" -> putIfAbsent("categoria", i)
                c == "dia" || c == "dias" -> putIfAbsent("dia", i)
            }
        }
        if (!containsKey("hora") && !containsKey("inicio")) put("hora", 0)
        if (!containsKey("actividad")) put("actividad", 1)
    }

    // ---------- Formato B: JSON ----------

    private val json = Json { isLenient = true; ignoreUnknownKeys = true }

    private fun JsonObject.any(vararg keys: String): JsonElement? = keys.firstNotNullOfOrNull { this[it] }
    private fun JsonElement.text(): String? = (this as? JsonPrimitive)?.content

    private fun parseJson(input: String): ParseResult {
        val root = try {
            json.parseToJsonElement(input)
        } catch (e: Exception) {
            return ParseResult(
                ImportFormat.JSON, emptyList(),
                listOf(LineError(1, input.lineSequence().firstOrNull().orEmpty().take(80), "JSON mal formado: ${e.message?.lineSequence()?.firstOrNull()}")),
            )
        }
        val array: JsonArray = when (root) {
            is JsonArray -> root
            is JsonObject -> root.any("bloques", "blocks") as? JsonArray
            else -> null
        }
            ?: return ParseResult(
                ImportFormat.JSON, emptyList(),
                listOf(LineError(1, "", "El JSON debe tener una lista \"bloques\"")),
            )

        val blocks = ArrayList<Block>()
        val errors = ArrayList<LineError>()
        array.forEachIndexed { i, el ->
            val n = i + 1
            val text = el.toString().take(120)
            fun err(reason: String) {
                errors += LineError(n, text, "Bloque $n: $reason")
            }
            val o = el as? JsonObject ?: return@forEachIndexed err("no es un objeto")
            val title = o.any("titulo", "title")?.text()?.trim().orEmpty()
            if (title.isEmpty()) return@forEachIndexed err("falta \"titulo\"")
            val start = o.any("inicio", "start")?.text()?.let { parseTime(it) }
                ?: return@forEachIndexed err("\"inicio\" falta o no es una hora HH:MM")
            val end = o.any("fin", "end")?.text()?.let { parseTime(it) }
                ?: return@forEachIndexed err("\"fin\" falta o no es una hora HH:MM")
            if (start == end) return@forEachIndexed err("\"inicio\" y \"fin\" son iguales")
            val days: Set<Int>? = when (val d = o.any("dias", "dia", "days")) {
                is JsonArray -> d.flatMap { item -> jsonDay(item) ?: return@forEachIndexed err("día no válido: $item") }.toSet()
                null -> null
                else -> jsonDay(d)
            }
            if (days.isNullOrEmpty()) return@forEachIndexed err("\"dias\" falta o no es válido")
            val catRaw = o.any("categoria", "category")?.text()
            val cat = resolveCategory(catRaw, title)
            val notify = (o.any("aviso", "notify") as? JsonPrimitive)?.booleanOrNull ?: cat.notifiesByDefault
            val fixed = (o.any("fijo", "fixed") as? JsonPrimitive)?.booleanOrNull ?: cat.fixedByDefault
            val lead = (o.any("antelacion", "antelacionMin", "lead") as? JsonPrimitive)?.intOrNull?.coerceIn(0, 180)
            val notes = o.any("notas", "notes")?.text().orEmpty()
            days.sorted().forEach { day ->
                blocks += Block(
                    title = title, category = cat, dayOfWeek = day, startMin = start, endMin = end,
                    notes = notes, notify = notify, leadMin = lead, fixed = fixed,
                )
            }
        }
        return ParseResult(ImportFormat.JSON, blocks, errors)
    }

    private fun jsonDay(el: JsonElement): Set<Int>? {
        val p = el as? JsonPrimitive ?: return null
        p.intOrNull?.let { return if (it in 1..7) setOf(it) else null }
        return parseDays(p.content) ?: dayTokens[normalize(p.content)]?.let { setOf(it) }
    }
}
