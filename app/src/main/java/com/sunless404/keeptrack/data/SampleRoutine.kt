package com.sunless404.keeptrack.data

import com.sunless404.keeptrack.data.Category.*

/** Rutina de ejemplo que se carga la primera vez que se abre la app. */
object SampleRoutine {

    private class Row(val days: IntRange, val start: String, val end: String, val title: String, val cat: Category)

    private fun min(hhmm: String) = hhmm.substringBefore(':').toInt() * 60 + hhmm.substringAfter(':').toInt()

    private val rows = listOf(
        Row(1..5, "07:00", "07:30", "Ducha y aseo", HIGIENE),
        Row(1..5, "07:30", "08:00", "Desayuno", COMIDA),
        Row(1..5, "08:00", "08:30", "Ida a clase", DESPLAZAMIENTO),
        Row(1..5, "08:30", "14:30", "Clases", CLASE),
        Row(1..5, "14:30", "15:00", "Vuelta a casa", DESPLAZAMIENTO),
        Row(1..5, "15:00", "16:00", "Comida", COMIDA),
        Row(1..5, "16:00", "17:00", "Tiempo libre", LIBRE),
        Row(1..5, "20:30", "21:00", "Ducha", HIGIENE),
        Row(1..5, "21:00", "21:45", "Cena", COMIDA),
        Row(1..5, "21:45", "22:30", "Lectura", LECTURA),
        Row(1..5, "22:30", "23:00", "Tiempo libre", LIBRE),
        Row(1..5, "23:00", "07:00", "Dormir", SUENO),

        Row(6..6, "09:00", "09:45", "Desayuno", COMIDA),
        Row(6..6, "10:00", "12:00", "Estudio", ESTUDIO),
        Row(6..6, "12:00", "13:30", "Gimnasio", GIMNASIO),
        Row(6..6, "14:30", "15:30", "Comida", COMIDA),
        Row(6..6, "15:30", "21:00", "Tiempo libre", LIBRE),
        Row(6..6, "21:00", "22:00", "Cena", COMIDA),
        Row(6..6, "22:00", "23:00", "Lectura", LECTURA),
        Row(6..6, "23:30", "08:30", "Dormir", SUENO),

        Row(7..7, "09:00", "09:45", "Desayuno", COMIDA),
        Row(7..7, "10:00", "14:00", "Tiempo libre", LIBRE),
        Row(7..7, "14:30", "15:30", "Comida", COMIDA),
        Row(7..7, "17:00", "19:00", "Estudio", ESTUDIO),
        Row(7..7, "21:00", "21:45", "Cena", COMIDA),
        Row(7..7, "21:45", "22:30", "Lectura", LECTURA),
        Row(7..7, "23:00", "07:00", "Dormir", SUENO),
    )

    private fun afternoons(): List<Block> = (1..5).flatMap { day ->
        if (day == 2 || day == 4) listOf(
            Block(title = "Academia de inglés", category = ACADEMIA, dayOfWeek = day, startMin = 17 * 60, endMin = 19 * 60),
            Block(title = "Estudio", category = ESTUDIO, dayOfWeek = day, startMin = 19 * 60, endMin = 20 * 60 + 30),
        ) else listOf(
            Block(title = "Estudio", category = ESTUDIO, dayOfWeek = day, startMin = 17 * 60, endMin = 19 * 60),
            Block(title = "Gimnasio", category = GIMNASIO, dayOfWeek = day, startMin = 19 * 60, endMin = 20 * 60 + 30),
        )
    }

    fun blocks(): List<Block> = rows.flatMap { r ->
        r.days.map { day ->
            Block(title = r.title, category = r.cat, dayOfWeek = day, startMin = min(r.start), endMin = min(r.end))
        }
    } + afternoons()
}
