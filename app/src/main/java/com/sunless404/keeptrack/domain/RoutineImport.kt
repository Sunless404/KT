package com.sunless404.keeptrack.domain

import com.sunless404.keeptrack.data.Block

/** Rutina escrita como texto legible, que [RoutineParser] vuelve a leer tal cual. */
object RoutineText {
    private val names = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")

    /** "Lunes a viernes", "Sábado y domingo", "Lunes, miércoles y viernes". */
    fun daysLabel(days: Set<Int>): String {
        val d = days.sorted()
        if (d.size >= 3 && d.last() - d.first() == d.size - 1) return "${names[d.first() - 1]} a ${names[d.last() - 1].lowercase()}"
        val parts = d.mapIndexed { i, x -> if (i == 0) names[x - 1] else names[x - 1].lowercase() }
        return if (parts.size == 1) parts[0] else parts.dropLast(1).joinToString(", ") + " y " + parts.last()
    }

    /** Agrupa los días que tienen exactamente los mismos bloques. */
    fun encode(blocks: List<Block>): String {
        val perDay = (1..7).associateWith { day ->
            blocks.filter { it.dayOfWeek == day }.sortedBy { it.startMin }
                .map { "${fmtTime(it.startMin)}-${fmtTime(it.endMin)} ${it.title} [${RoutineJson.categoryKey(it.category)}]" }
        }.filterValues { it.isNotEmpty() }
        val groups = perDay.entries.groupBy({ it.value }, { it.key })
        return groups.entries.sortedBy { it.value.min() }.joinToString("\n\n") { (lines, days) ->
            daysLabel(days.toSet()) + "\n" + lines.joinToString("\n")
        }
    }
}

/** Rutinas tipo para empezar sin escribir nada. */
data class RoutineTemplate(val name: String, val description: String, val text: String) {
    val blocks get() = RoutineParser.parse(text).blocks
}

object RoutineTemplates {
    val all = listOf(
        RoutineTemplate(
            "Estudiante", "Clases por la mañana, estudio por la tarde y deporte tres días.",
            """
            Lunes a viernes
            07:00-07:30 Ducha y aseo [higiene]
            07:30-08:00 Desayuno [comida]
            08:00-14:30 Clases [clase]
            14:30-15:30 Comida [comida]
            16:00-18:30 Estudio [estudio]
            21:00-21:45 Cena [comida]
            22:00-22:45 Lectura [lectura]
            23:00-07:00 Dormir [sueno]

            Lunes, miércoles y viernes
            19:00-20:30 Gimnasio [gimnasio]

            Sábado y domingo
            09:30-10:00 Desayuno [comida]
            10:30-12:30 Estudio [estudio]
            14:00-15:00 Comida [comida]
            21:00-22:00 Cena [comida]
            00:00-09:00 Dormir [sueno]
            """.trimIndent(),
        ),
        RoutineTemplate(
            "Jornada de oficina", "Trabajo de 9 a 18 con pausa para comer y entreno al salir.",
            """
            Lunes a viernes
            07:00-07:45 Desayuno y ducha [higiene]
            08:15-09:00 Ida al trabajo [desplazamiento]
            09:00-14:00 Trabajo [otro]
            14:00-15:00 Comida [comida]
            15:00-18:00 Trabajo [otro]
            18:00-18:45 Vuelta a casa [desplazamiento]
            19:00-20:00 Gimnasio [gimnasio]
            21:00-21:45 Cena [comida]
            22:00-22:30 Lectura [lectura]
            23:00-07:00 Dormir [sueno]

            Sábado y domingo
            09:30-10:15 Desayuno [comida]
            11:00-12:00 Deporte al aire libre [gimnasio]
            14:00-15:00 Comida [comida]
            21:00-22:00 Cena [comida]
            00:00-09:00 Dormir [sueno]
            """.trimIndent(),
        ),
        RoutineTemplate(
            "Estudio intensivo", "Para oposiciones o exámenes: bloques largos con descansos.",
            """
            Lunes a sábado
            07:00-07:30 Desayuno [comida]
            07:30-10:30 Estudio: temario [estudio]
            10:30-11:00 Descanso [libre]
            11:00-14:00 Estudio: repaso [estudio]
            14:00-15:30 Comida [comida]
            15:30-18:30 Estudio: test y prácticos [estudio]
            18:30-19:30 Deporte [gimnasio]
            21:00-21:45 Cena [comida]
            22:00-22:30 Lectura [lectura]
            23:00-07:00 Dormir [sueno]

            Domingo
            10:00-12:00 Repaso semanal [estudio]
            14:00-15:00 Comida [comida]
            21:00-22:00 Cena [comida]
            23:00-07:00 Dormir [sueno]
            """.trimIndent(),
        ),
        RoutineTemplate(
            "Turno de tarde", "Trabajo de 15 a 23 con las mañanas para ti.",
            """
            Lunes a viernes
            08:30-09:15 Desayuno [comida]
            09:30-11:00 Gimnasio [gimnasio]
            11:30-13:00 Estudio o proyectos [estudio]
            13:00-14:00 Comida [comida]
            14:15-15:00 Ida al trabajo [desplazamiento]
            15:00-23:00 Trabajo [otro]
            23:00-23:45 Vuelta y cena [comida]
            00:30-08:30 Dormir [sueno]

            Sábado y domingo
            10:00-10:45 Desayuno [comida]
            14:00-15:00 Comida [comida]
            21:00-22:00 Cena [comida]
            00:30-09:30 Dormir [sueno]
            """.trimIndent(),
        ),
    )
}

/** Formatos de archivo que entiende la importación, con una plantilla lista para copiar. */
data class FileFormat(val name: String, val extension: String, val description: String, val sample: String)

object ImportFormats {
    val all = listOf(
        FileFormat(
            "JSON", ".json",
            "Una lista de bloques. \"dias\", \"inicio\", \"fin\" y \"titulo\" son obligatorios; \"categoria\", \"notas\" y \"aviso\" son opcionales.",
            """
            {
              "bloques": [
                {
                  "titulo": "Clases",
                  "categoria": "clase",
                  "dias": ["lunes", "martes", "miercoles", "jueves", "viernes"],
                  "inicio": "08:00",
                  "fin": "14:00"
                },
                {
                  "titulo": "Gimnasio",
                  "categoria": "gimnasio",
                  "dias": ["martes", "jueves"],
                  "inicio": "18:00",
                  "fin": "19:30",
                  "aviso": true
                }
              ]
            }
            """.trimIndent(),
        ),
        FileFormat(
            "CSV por filas", ".csv",
            "Una fila por bloque. Separado por comas o punto y coma, como lo guarda Excel o Google Sheets.",
            """
            Día;Inicio;Fin;Actividad;Categoría
            Lunes a viernes;08:00;14:00;Clases;clase
            Martes y jueves;18:00;19:30;Gimnasio;gimnasio
            Todos los días;23:00;07:00;Dormir;sueno
            """.trimIndent(),
        ),
        FileFormat(
            "CSV en cuadrícula", ".csv",
            "Como un horario de clase: una columna por día y una fila por franja horaria. Las celdas vacías se ignoran.",
            """
            Hora;Lunes;Martes;Miércoles;Jueves;Viernes
            08:00-09:00;Matemáticas;Inglés;Matemáticas;Historia;Física
            09:00-10:00;Lengua;Física;Química;Inglés;Lengua
            """.trimIndent(),
        ),
        FileFormat(
            "Texto", ".txt",
            "El nombre del día y debajo sus bloques, o frases con el día y la hora. La categoría, opcional, entre corchetes.",
            """
            Lunes a viernes
            08:00-14:00 Clases [clase]
            16:00-18:00 Estudio

            Gimnasio martes y jueves de 18 a 19:30
            """.trimIndent(),
        ),
    )
}
