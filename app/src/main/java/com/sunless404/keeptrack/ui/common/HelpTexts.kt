package com.sunless404.keeptrack.ui.common

/** Explicaciones breves del botón "?" de cada apartado. */
object HelpTexts {
    val TODAY = listOf(
        "Desliza a izquierda o derecha para ver otros días, o toca un día de la semana de arriba.",
        "La bolita de la línea avanza con la hora y marca lo que te toca ahora; «Ir» te lleva hasta ahí.",
        "Marca el tick cuando acabes algo: pasa a «Hechas», al final, y arriba solo queda lo pendiente.",
        "Toca un bloque para quitarlo de hoy, cambiarlo de hora, acortarlo o retrasar desde ahí. Solo cambia ese día, no tu rutina.",
        "«Compromisos» son las citas y cosas puntuales de ese día; añádelos con el +.",
        "Toca «Día normal» para marcar el día como festivo o libre: no cuenta en rachas ni avisa.",
    )
    val TASKS = listOf(
        "La agenda muestra tus compromisos bajo cada día; hoy aparece en un círculo y los de prioridad alta en rojo.",
        "Toca un día para ver sus compromisos y marcarlos como hechos.",
        "El botón + crea uno: solo necesita título y fecha. La hora de inicio y fin, el aviso, la repetición y las notas son opcionales.",
        "Los compromisos también aparecen en Hoy, en «Compromisos».",
    )
    val STATS = listOf(
        "Las rachas cuentan días seguidos cumpliendo cada hábito. Cuantos más días, más cambia la llama: de naranja a rojo, rosa, violeta, azul y blanco dorado (1, 3, 7, 14, 30, 60 y 100 días).",
        "Festivos, días libres y días sin ese bloque no rompen la racha.",
        "«Horas por categoría» compara lo previsto con lo hecho en la semana; cambia de semana con las flechas.",
        "Pista: toca la racha de gimnasio…",
    )
    val ROUTINE = listOf(
        "Es tu plantilla semanal: toca un día para desplegar sus bloques.",
        "Toca un bloque para modificarlo; con ⋮ puedes duplicarlo o borrarlo.",
        "«Añadir bloque» crea uno nuevo y «Copiar día» copia el día en otros.",
        "Si dos bloques se pisan verás un aviso en rojo. Cambiar la plantilla no altera los días pasados.",
    )
    val ASSISTANT = listOf(
        "Pide en lenguaje normal crear o cambiar tu rutina o añadir compromisos.",
        "Puedes adjuntar una foto (por ejemplo tu horario de clases), hacer una con la cámara o mandar una nota de voz con el micro.",
        "Los cambios aparecen en una tarjeta y solo se guardan si pulsas «Aplicar».",
        "Usa Google Gemini gratis; la clave se configura en Perfil > Ajustes.",
    )
    val GYM = listOf(
        "Registra cada sesión con sus ejercicios, series, peso y repeticiones.",
        "Puntos semanales: 10 por sesión (hasta 5), extra por cada récord y por semanas seguidas yendo.",
        "La clasificación histórica mide tu fuerza relativa a tu peso corporal.",
        "Con cuenta, añade amigos por su usuario para competir.",
    )
}
