package com.sunless404.rutina.domain

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

val ES: Locale = Locale("es", "ES")

val DAY_NAMES = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")
val DAY_SHORT = listOf("L", "M", "X", "J", "V", "S", "D")

/** Nombre del día, 1 = lunes … 7 = domingo. */
fun dayName(dow: Int): String = DAY_NAMES[dow - 1]

/** Minutos desde medianoche a "HH:mm" (acepta valores de más de 24 h). */
fun fmtTime(min: Int): String {
    val m = ((min % 1440) + 1440) % 1440
    return "%02d:%02d".format(m / 60, m % 60)
}

fun fmtRange(startMin: Int, endMin: Int): String = "${fmtTime(startMin)}–${fmtTime(endMin)}"

/** Duración legible: "45 min", "1 h", "1 h 30 min". */
fun fmtDuration(min: Int): String {
    val h = min / 60
    val m = min % 60
    return when {
        h == 0 -> "$m min"
        m == 0 -> "$h h"
        else -> "$h h $m min"
    }
}

private val longDate = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", ES)
private val shortDate = DateTimeFormatter.ofPattern("d MMM", ES)

fun fmtLongDate(date: LocalDate): String = date.format(longDate).replaceFirstChar { it.uppercase(ES) }
fun fmtShortDate(date: LocalDate): String = date.format(shortDate)

fun mondayOf(date: LocalDate): LocalDate = date.minusDays((date.dayOfWeek.value - 1).toLong())
