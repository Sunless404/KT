package com.sunless404.keeptrack

import com.sunless404.keeptrack.data.Block
import com.sunless404.keeptrack.data.Category
import com.sunless404.keeptrack.domain.Gap
import com.sunless404.keeptrack.domain.ImportFormat
import com.sunless404.keeptrack.domain.RoutineAnalysis
import com.sunless404.keeptrack.domain.RoutineParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutineParserTest {

    private fun parse(s: String) = RoutineParser.parse(s.trimIndent())
    private fun Block.range() = "%02d:%02d-%02d:%02d".format(startMin / 60, startMin % 60, endMin / 60, endMin % 60)

    // ---------- Formato A ----------

    @Test
    fun basicExampleFromSpec() {
        val r = parse(
            """
            Lunes
            08:00-09:00 DIW [clase]
            12:30-12:45 Vuelta a casa [desplazamiento]
            """
        )
        assertEquals(ImportFormat.TEXTO, r.format)
        assertTrue(r.errors.isEmpty())
        assertEquals(2, r.blocks.size)
        with(r.blocks[0]) {
            assertEquals("DIW", title)
            assertEquals(Category.CLASE, category)
            assertEquals(1, dayOfWeek)
            assertEquals("08:00-09:00", range())
        }
        assertEquals(Category.DESPLAZAMIENTO, r.blocks[1].category)
    }

    @Test
    fun dashVariantsAndHourSeparators() {
        val r = parse(
            """
            Martes
            08:00 – 09:00 Uno
            09:00—10:00 Dos
            10h-11h Tres
            11h30-12h15 Cuatro
            12.15 - 13.00 Cinco
            13:00 a 14:00 Seis
            8-9 Siete
            15:00h-16:00h Ocho
            """
        )
        assertTrue(r.errors.toString(), r.errors.isEmpty())
        assertEquals(
            listOf("08:00-09:00", "09:00-10:00", "10:00-11:00", "11:30-12:15", "12:15-13:00", "13:00-14:00", "08:00-09:00", "15:00-16:00"),
            r.blocks.map { it.range() },
        )
        assertEquals(listOf("Uno", "Dos", "Tres", "Cuatro", "Cinco", "Seis", "Siete", "Ocho"), r.blocks.map { it.title })
        assertTrue(r.blocks.all { it.dayOfWeek == 2 })
    }

    @Test
    fun titleStartingWithHIsNotEaten() {
        val r = parse("Lunes\n09:00-10:00 Historia")
        assertEquals("Historia", r.blocks.single().title)
    }

    @Test
    fun dayNamesWithAndWithoutAccentAndAbbreviated() {
        val r = parse(
            """
            miércoles
            08:00-09:00 A
            MIERCOLES:
            09:00-10:00 B
            Mié
            10:00-11:00 C
            sab
            10:00-11:00 D
            ## Sábado
            11:00-12:00 E
            **Dom**
            10:00-11:00 F
            """
        )
        assertTrue(r.errors.toString(), r.errors.isEmpty())
        assertEquals(listOf(3, 3, 3, 6, 6, 7), r.blocks.map { it.dayOfWeek })
    }

    @Test
    fun dayRangesAndLists() {
        assertEquals(setOf(1, 2, 3, 4, 5), RoutineParser.parseDays("Lunes a viernes"))
        assertEquals(setOf(1, 2, 3, 4, 5), RoutineParser.parseDays("L-V"))
        assertEquals(setOf(2, 4), RoutineParser.parseDays("Martes y jueves"))
        assertEquals(setOf(1, 3, 5), RoutineParser.parseDays("lun, mié, vie"))
        assertEquals(setOf(6, 7, 1), RoutineParser.parseDays("sábado a lunes"))
        assertEquals(setOf(6, 7), RoutineParser.parseDays("Fin de semana"))
        assertNull(RoutineParser.parseDays("Notas varias"))
        assertNull(RoutineParser.parseDays("y"))
        assertNull(RoutineParser.parseDays("Lunes 3"))
    }

    @Test
    fun multiDayHeaderCreatesOneBlockPerDay() {
        val r = parse("Lunes a viernes\n23:00-07:00 Dormir")
        assertEquals(5, r.blocks.size)
        assertTrue(r.blocks.all { it.category == Category.SUENO && it.crossesMidnight && it.durationMin == 480 })
        assertFalse(r.blocks.first().notify)
    }

    @Test
    fun categoryInferredFromTitle() {
        val r = parse(
            """
            Jueves
            07:00-07:20 Ducha
            07:30-08:00 Desayuno
            08:00-08:30 Ida a clase
            08:30-14:30 Clase de programación
            17:00-18:00 Academia de inglés
            18:00-19:30 Estudiar BD
            19:30-20:30 Gym
            21:30-22:00 Leer
            22:00-23:00 Tiempo libre
            23:00-07:00 Dormir
            12:00-12:10 Llamar al banco
            """
        )
        assertEquals(
            listOf(
                Category.HIGIENE, Category.COMIDA, Category.DESPLAZAMIENTO, Category.CLASE, Category.ACADEMIA,
                Category.ESTUDIO, Category.GIMNASIO, Category.LECTURA, Category.LIBRE, Category.SUENO, Category.OTRO,
            ),
            r.blocks.map { it.category },
        )
    }

    @Test
    fun comidaIsNotMistakenForIda() {
        assertEquals(Category.COMIDA, RoutineParser.inferCategory("Comida"))
    }

    @Test
    fun explicitCategoryWinsAndAcceptsAccentsAndSynonyms() {
        val r = parse(
            """
            Viernes
            10:00-11:00 Comida de empresa [Sueño]
            11:00-12:00 Algo [GYM]
            12:00-13:00 Otra cosa [inventada]
            """
        )
        assertEquals(listOf(Category.SUENO, Category.GIMNASIO, Category.OTRO), r.blocks.map { it.category })
        assertEquals("Comida de empresa", r.blocks[0].title)
    }

    @Test
    fun bulletsAndNumberedLines() {
        val r = parse("Lunes\n- 08:00-09:00 Uno\n* 09:00-10:00 Dos\n1. 10:00-11:00 Tres\n• 11:00-12:00: Cuatro")
        assertTrue(r.errors.toString(), r.errors.isEmpty())
        assertEquals(listOf("Uno", "Dos", "Tres", "Cuatro"), r.blocks.map { it.title })
    }

    @Test
    fun markdownTableWithRangeAndDuration() {
        val r = parse(
            """
            ### Lunes
            | Hora | Actividad | Duración |
            |------|-----------|----------|
            | 08:00-09:00 | DIW [clase] | 1h |
            | 09:00 | Estudio | 1h 30min |
            | 10:30 | Descanso | 45 min |
            | 11:15 | Gimnasio | 1,5 h |
            """
        )
        assertTrue(r.errors.toString(), r.errors.isEmpty())
        assertEquals(listOf("08:00-09:00", "09:00-10:30", "10:30-11:15", "11:15-12:45"), r.blocks.map { it.range() })
        assertEquals(Category.CLASE, r.blocks[0].category)
        assertEquals("DIW", r.blocks[0].title)
    }

    @Test
    fun tableRowWithoutDurationEndsAtNextRow() {
        val r = parse(
            """
            Martes
            | Hora | Actividad |
            |---|---|
            | 08:00 | Uno |
            | 09:30 | Dos |
            """
        )
        assertEquals(listOf("08:00-09:30"), r.blocks.map { it.range() })
        assertEquals(1, r.errors.size)
        assertEquals(5, r.errors[0].line)
    }

    @Test
    fun tableWithDayAndCategoryColumns() {
        val r = parse(
            """
            | Día | Hora | Actividad | Categoría |
            |---|---|---|---|
            | Lunes | 08:00-09:00 | Mates | clase |
            | Martes y jueves | 18:00-19:00 | Pesas | gimnasio |
            """
        )
        assertTrue(r.errors.toString(), r.errors.isEmpty())
        assertEquals(listOf(1, 2, 4), r.blocks.map { it.dayOfWeek })
        assertEquals(Category.GIMNASIO, r.blocks[1].category)
    }

    // ---------- Casos mal formados ----------

    @Test
    fun blockBeforeAnyDayIsAnError() {
        val r = parse("08:00-09:00 Sin día\nLunes\n09:00-10:00 Con día")
        assertEquals(1, r.blocks.size)
        assertEquals(1, r.errors.single().line)
    }

    @Test
    fun malformedLinesAreReportedWithLineNumbers() {
        val r = parse(
            """
            Lunes
            08:00-09:00 Bien
            esto no es nada
            25:00-26:00 Hora imposible
            10:75-11:00 Minutos imposibles
            12:00-12:00 Duración cero
            13:00-14:00
            13:00 Sin fin
            """
        )
        assertEquals(listOf("Bien"), r.blocks.map { it.title })
        assertEquals(listOf(3, 4, 5, 6, 7, 8), r.errors.map { it.line })
    }

    @Test
    fun emptyAndBlankInput() {
        assertTrue(parse("").blocks.isEmpty())
        assertTrue(parse("").errors.isEmpty())
        assertTrue(RoutineParser.parse("   \n\n  ").errors.isEmpty())
    }

    @Test
    fun midnightHandling() {
        val r = parse("Sábado\n22:00-24:00 Cine\n00:00-08:00 Dormir")
        assertEquals("22:00-00:00", r.blocks[0].range())
        assertEquals(120, r.blocks[0].durationMin)
        assertEquals(480, r.blocks[1].durationMin)
    }

    // ---------- Formato B ----------

    @Test
    fun jsonIsDetectedAndParsed() {
        val r = parse(
            """
            {
              "version": 1,
              "bloques": [
                {"titulo": "DIW", "categoria": "clase", "dias": ["lunes", "miércoles"], "inicio": "08:00", "fin": "09:00",
                 "notas": "Aula 2", "aviso": true, "antelacion": 5},
                {"titulo": "Dormir", "dias": [1, 2], "inicio": "23:00", "fin": "07:00"}
              ]
            }
            """
        )
        assertEquals(ImportFormat.JSON, r.format)
        assertTrue(r.errors.toString(), r.errors.isEmpty())
        assertEquals(4, r.blocks.size)
        with(r.blocks[0]) {
            assertEquals(Category.CLASE, category)
            assertEquals("Aula 2", notes)
            assertEquals(5, leadMin)
            assertTrue(notify)
        }
        assertEquals(3, r.blocks[1].dayOfWeek)
        assertEquals(Category.SUENO, r.blocks[2].category)
        assertFalse(r.blocks[2].notify)
    }

    @Test
    fun jsonInsideCodeFenceAndBareArray() {
        val r = RoutineParser.parse("```json\n[{\"titulo\":\"Gym\",\"dias\":\"L-V\",\"inicio\":\"19:00\",\"fin\":\"20:00\"}]\n```")
        assertEquals(ImportFormat.JSON, r.format)
        assertEquals(5, r.blocks.size)
        assertEquals(Category.GIMNASIO, r.blocks[0].category)
    }

    @Test
    fun malformedJsonGivesErrorNotCrash() {
        val r = parse("""{"bloques": [ {"titulo": "x", """)
        assertEquals(ImportFormat.JSON, r.format)
        assertTrue(r.blocks.isEmpty())
        assertEquals(1, r.errors.size)
    }

    @Test
    fun jsonBlocksWithMissingOrBadFields() {
        val r = parse(
            """
            {"bloques": [
              {"dias": ["lunes"], "inicio": "08:00", "fin": "09:00"},
              {"titulo": "Sin inicio", "dias": ["lunes"], "fin": "09:00"},
              {"titulo": "Hora mala", "dias": ["lunes"], "inicio": "99:00", "fin": "09:00"},
              {"titulo": "Día malo", "dias": ["lunex"], "inicio": "08:00", "fin": "09:00"},
              {"titulo": "Sin días", "inicio": "08:00", "fin": "09:00"},
              {"titulo": "Día 9", "dias": [9], "inicio": "08:00", "fin": "09:00"},
              "no soy un objeto",
              {"titulo": "Bueno", "dias": ["domingo"], "inicio": "08:00", "fin": "09:00"}
            ]}
            """
        )
        assertEquals(listOf("Bueno"), r.blocks.map { it.title })
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 7), r.errors.map { it.line })
    }

    @Test
    fun jsonWithoutBlockList() {
        val r = parse("""{"cosas": 3}""")
        assertTrue(r.blocks.isEmpty())
        assertEquals(1, r.errors.size)
    }

    // ---------- Análisis de la vista previa ----------

    @Test
    fun analysisFindsOverlapsGapsAndSleep() {
        val r = parse(
            """
            Lunes
            08:00-10:00 Clase
            09:30-11:00 Estudio
            23:00-07:00 Dormir
            Martes
            07:00-08:00 Desayuno
            00:30-01:00 Dormir otra vez
            """
        )
        val a = RoutineAnalysis.analyze(r.blocks)
        // Clase/Estudio y los dos bloques de sueño de la madrugada del martes.
        assertEquals(2, a.overlaps.size)
        assertEquals(listOf(Gap(0, 480), Gap(660, 1380)), a.gaps[1])
        assertEquals(listOf(Gap(480, 1440)), a.gaps[2])
        // Noche del lunes: 8 h + 30 min del bloque que empieza el martes de madrugada.
        assertEquals(510, a.sleepByNight[1])
    }

    // ---------- Frases, CSV, cuadrículas y respuestas de IA ----------

    @Test
    fun phrases() {
        val r = parse(
            """
            Estudio de lunes a viernes de 8 a 10
            Gimnasio martes y jueves a las 18:00 durante 1h30
            Leer todos los días 30 min a las 23:00
            De 17:00 a 18:30 clases de inglés los miércoles
            Fútbol los sábados de 5 a 7 de la tarde
            """
        )
        assertTrue(r.errors.toString(), r.errors.isEmpty())
        val byTitle = r.blocks.groupBy { it.title }
        assertEquals(setOf("Estudio", "Gimnasio", "Leer", "Clases de inglés", "Fútbol"), byTitle.keys)
        assertEquals((1..5).toList(), byTitle.getValue("Estudio").map { it.dayOfWeek })
        assertEquals("08:00-10:00", byTitle.getValue("Estudio")[0].range())
        assertEquals(listOf(2, 4), byTitle.getValue("Gimnasio").map { it.dayOfWeek })
        assertEquals("18:00-19:30", byTitle.getValue("Gimnasio")[0].range())
        assertEquals(Category.GIMNASIO, byTitle.getValue("Gimnasio")[0].category)
        assertEquals(7, byTitle.getValue("Leer").size)
        assertEquals("23:00-23:30", byTitle.getValue("Leer")[0].range())
        assertEquals(Category.LECTURA, byTitle.getValue("Leer")[0].category)
        assertEquals(listOf(3), byTitle.getValue("Clases de inglés").map { it.dayOfWeek })
        assertEquals("17:00-18:30", byTitle.getValue("Clases de inglés")[0].range())
        assertEquals("17:00-19:00", byTitle.getValue("Fútbol")[0].range())
        assertEquals(listOf(6), byTitle.getValue("Fútbol").map { it.dayOfWeek })
    }

    @Test
    fun phraseUnderDayHeader() {
        val r = parse(
            """
            Lunes
            Piano de 18 a 19
            """
        )
        assertTrue(r.errors.toString(), r.errors.isEmpty())
        assertEquals(1, r.blocks.single().dayOfWeek)
        assertEquals("Piano", r.blocks.single().title)
    }

    @Test
    fun csvFromSpreadsheet() {
        val r = parse(
            """
            Día;Inicio;Fin;Actividad;Categoría
            Lunes a viernes;08:00;14:00;Clases;clase
            Martes;18:00;19:30;"Gimnasio; piernas";gym
            Sábado;10:00;12:00;Estudio;
            """
        )
        assertTrue(r.errors.toString(), r.errors.isEmpty())
        assertEquals(7, r.blocks.size)
        val gym = r.blocks.single { it.dayOfWeek == 2 && it.category == Category.GIMNASIO }
        assertEquals("Gimnasio; piernas", gym.title)
        assertEquals("18:00-19:30", gym.range())
    }

    @Test
    fun timetableGrid() {
        val r = parse(
            """
            Hora,Lunes,Martes,Miércoles
            08:00-09:00,Matemáticas,Inglés,
            09:00-10:00,Historia,-,Física
            """
        )
        assertTrue(r.errors.toString(), r.errors.isEmpty())
        assertEquals(4, r.blocks.size)
        assertEquals("Inglés", r.blocks.single { it.dayOfWeek == 2 }.title)
        assertEquals("09:00-10:00", r.blocks.single { it.dayOfWeek == 3 }.range())
    }

    @Test
    fun aiAnswerWithTextAroundJson() {
        val r = parse(
            """
            ¡Claro! Aquí tienes tu rutina:

            ```json
            {"bloques": [{"titulo": "Correr", "dias": ["lunes"], "inicio": "07:00", "fin": "07:45"}]}
            ```
            Espero que te sirva.
            """
        )
        assertEquals(ImportFormat.JSON, r.format)
        assertEquals("Correr", r.blocks.single().title)
    }
}
