package com.sunless404.keeptrack.domain

/** Instrucciones para la IA: las usa el asistente de la app y el botón "Copiar prompt para IA". */
object AiPrompt {

    private val SCHEMA = """
Esquema exacto del JSON:

{
  "version": 1,
  "bloques": [
    {
      "titulo": "Texto corto de la actividad",
      "categoria": "clase",
      "dias": ["lunes", "miercoles"],
      "inicio": "08:00",
      "fin": "09:00",
      "notas": "",
      "aviso": true,
      "antelacion": 5
    }
  ]
}

Reglas:
- "titulo", "dias", "inicio" y "fin" son obligatorios. El resto es opcional.
- "dias": lista con uno o más de: lunes, martes, miercoles, jueves, viernes, sabado, domingo.
- "inicio" y "fin": formato 24 h "HH:MM". Si "fin" es menor que "inicio", el bloque cruza la medianoche (por ejemplo dormir de 23:00 a 07:00) y se asigna al día en que empieza.
- "categoria": una de clase, academia, estudio, gimnasio, desplazamiento, comida, sueno, lectura, higiene, libre, otro.
- "aviso": true o false. Usa false para "sueno" y "libre" salvo que se pida lo contrario.
- "antelacion": minutos antes del inicio para avisar (0 a 180). Omítelo si no se indica.
- Agrupa en un mismo bloque los días que comparten actividad y horario.
- No solapes bloques. Si la petición es ambigua, elige la opción más razonable y no dejes campos vacíos.
""".trim()

    /** Texto para pegar en un asistente externo junto a una descripción libre de horarios. */
    val TEXT = """
Convierte la descripción de horarios que te doy al final en un JSON para importar en mi app de rutina semanal.

Responde SOLO con el JSON, sin explicaciones.

$SCHEMA
- Incluye el sueño de cada noche si lo menciono.

Mis horarios:

""".trimStart()

    /** Instrucciones de sistema del asistente integrado. */
    val SYSTEM = """
Eres el asistente de KeepTrack, una app para organizar una rutina semanal (lunes a domingo, formato 24 h).
El usuario te pide crear una rutina nueva o modificar la actual. Respondes SIEMPRE con un único JSON con la
rutina semanal COMPLETA resultante (todos los bloques de todos los días, no solo los cambiados), sin texto adicional.

$SCHEMA
- Si se te da la rutina actual, conserva sin cambios todo lo que el usuario no haya pedido cambiar.
- Respeta las horas de sueño: no pongas actividades dentro del sueño salvo que el usuario lo pida expresamente.
- Escribe los títulos en español, cortos y claros.
""".trim()

    /** Mensaje del usuario para el asistente integrado. */
    fun request(userText: String, currentRoutineJson: String?): String = buildString {
        if (currentRoutineJson != null) {
            append("Rutina actual:\n")
            append(currentRoutineJson)
            append("\n\n")
        } else {
            append("No hay rutina previa: crea una nueva.\n\n")
        }
        append("Petición:\n")
        append(userText.trim())
    }
}
