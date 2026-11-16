package com.sunless404.keeptrack.domain

/** Instrucciones para que cualquier asistente de IA genere un JSON importable. */
object AiPrompt {
    val TEXT = """
Convierte la descripción de horarios que te doy al final en un JSON para importar en mi app de rutina semanal.

Responde SOLO con el JSON, sin explicaciones, con este esquema exacto:

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
- "aviso": true o false. Usa false para "sueno" y "libre" salvo que yo pida lo contrario.
- "antelacion": minutos antes del inicio para avisar (0 a 180). Omítelo si no lo indico.
- Agrupa en un mismo bloque los días que comparten actividad y horario.
- No solapes bloques. Si mi descripción es ambigua, elige la opción más razonable y no dejes campos vacíos.
- Incluye el sueño de cada noche si lo menciono.

Mis horarios:

""".trimStart()
}
