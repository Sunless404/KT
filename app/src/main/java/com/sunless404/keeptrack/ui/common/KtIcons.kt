package com.sunless404.keeptrack.ui.common

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/** Iconos propios de trazo (24×24) para no depender de la librería de iconos extendida. */
object KtIcons {
    private fun icon(name: String, vararg strokes: String, fills: List<String> = emptyList(), width: Float = 2f): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            strokes.forEach {
                addPath(
                    pathData = PathParser().parsePathString(it).toNodes(),
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = width,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
            fills.forEach {
                addPath(pathData = PathParser().parsePathString(it).toNodes(), fill = SolidColor(Color.Black))
            }
        }.build()

    val Today = icon(
        "today",
        "M8,12 a4,4 0 1,0 8,0 a4,4 0 1,0 -8,0",
        "M12,2.5 V4.5 M12,19.5 V21.5 M2.5,12 H4.5 M19.5,12 H21.5 M5.3,5.3 L6.7,6.7 M17.3,17.3 L18.7,18.7 M5.3,18.7 L6.7,17.3 M17.3,6.7 L18.7,5.3",
    )
    val Tasks = icon(
        "tasks",
        "M5,5 H19 a1.5,1.5 0 0,1 1.5,1.5 V19 a1.5,1.5 0 0,1 -1.5,1.5 H5 a1.5,1.5 0 0,1 -1.5,-1.5 V6.5 A1.5,1.5 0 0,1 5,5 Z",
        "M3.5,9.5 H20.5 M8,3 V6.5 M16,3 V6.5",
        "M8.5,15 L10.7,17 L15.5,12.5",
    )
    val Stats = icon("stats", "M5,20 V13 M12,20 V5 M19,20 V9 M3,20.5 H21")
    val Routine = icon(
        "routine",
        "M9,6 H20 M9,12 H20 M9,18 H20",
        fills = listOf(
            "M3.5,6 a1.5,1.5 0 1,0 3,0 a1.5,1.5 0 1,0 -3,0",
            "M3.5,12 a1.5,1.5 0 1,0 3,0 a1.5,1.5 0 1,0 -3,0",
            "M3.5,18 a1.5,1.5 0 1,0 3,0 a1.5,1.5 0 1,0 -3,0",
        ),
    )
    val Assistant = icon(
        "assistant",
        "M5,4.5 H19 a2,2 0 0,1 2,2 V15 a2,2 0 0,1 -2,2 H11 L6.5,20.5 V17 H5 a2,2 0 0,1 -2,-2 V6.5 a2,2 0 0,1 2,-2 Z",
        "M12,7.5 L12.9,9.9 L15.3,10.8 L12.9,11.7 L12,14.1 L11.1,11.7 L8.7,10.8 L11.1,9.9 Z",
        width = 1.8f,
    )
    val Mic = icon(
        "mic",
        "M12,3 a3,3 0 0,1 3,3 V12 a3,3 0 0,1 -6,0 V6 a3,3 0 0,1 3,-3 Z",
        "M5.5,11.5 a6.5,6.5 0 0,0 13,0 M12,18 V21.5 M8.5,21.5 H15.5",
    )
    val Camera = icon(
        "camera",
        "M4,7.5 H7.5 L9.3,5 H14.7 L16.5,7.5 H20 a1.5,1.5 0 0,1 1.5,1.5 V18 a1.5,1.5 0 0,1 -1.5,1.5 H4 a1.5,1.5 0 0,1 -1.5,-1.5 V9 A1.5,1.5 0 0,1 4,7.5 Z",
        "M8.5,13 a3.5,3.5 0 1,0 7,0 a3.5,3.5 0 1,0 -7,0",
    )
    val Image = icon(
        "image",
        "M5,4 H19 a1.5,1.5 0 0,1 1.5,1.5 V18.5 a1.5,1.5 0 0,1 -1.5,1.5 H5 a1.5,1.5 0 0,1 -1.5,-1.5 V5.5 A1.5,1.5 0 0,1 5,4 Z",
        "M3.5,16.5 L8.5,11.5 L13,16 L15.5,13.5 L20.5,18.5",
        "M14.5,8.5 a1.5,1.5 0 1,0 3,0 a1.5,1.5 0 1,0 -3,0",
    )
    val Stop = icon("stop", fills = listOf("M7,7 H17 V17 H7 Z"))
    val Dumbbell = icon(
        "dumbbell",
        "M2.5,10 V14 M21.5,10 V14 M7.5,12 H16.5",
        "M5,7 V17 M19,7 V17",
        width = 2.6f,
    )
    val Help = icon(
        "help",
        "M12,2.75 a9.25,9.25 0 1,0 0.01,0 Z",
        "M9.3,9.3 a2.8,2.8 0 1,1 4.1,2.5 C12.6,12.2 12,12.8 12,13.8 V14.3",
        fills = listOf("M10.9,17.4 a1.1,1.1 0 1,0 2.2,0 a1.1,1.1 0 1,0 -2.2,0"),
    )
    val Person = icon(
        "person",
        "M8,8 a4,4 0 1,0 8,0 a4,4 0 1,0 -8,0",
        "M4,20.5 C4.8,16.6 8,14.5 12,14.5 C16,14.5 19.2,16.6 20,20.5",
    )
    val Flame = icon(
        "flame",
        fills = listOf(
            "M12,2.5 C12.6,5.6 15.5,7.7 17.3,10.2 C18.5,11.9 19,13.4 19,15 a7,7 0 0,1 -14,0 C5,12.8 6.1,10.9 7.6,9.6 C7.7,11.4 8.6,12.6 10,13.1 C9.4,9.5 10.6,5.4 12,2.5 Z"
        ),
    )
    val Pencil = icon("pencil", "M4,20 L4.8,16.2 L15.8,5.2 a2,2 0 0,1 2.9,0 l0.1,0.1 a2,2 0 0,1 0,2.9 L7.8,19.2 Z", "M14,7 L17,10")
    val Sparkle = icon(
        "sparkle",
        fills = listOf("M12,2 L13.9,8.6 L20.5,10.5 L13.9,12.4 L12,19 L10.1,12.4 L3.5,10.5 L10.1,8.6 Z", "M18.5,15.5 L19.3,17.7 L21.5,18.5 L19.3,19.3 L18.5,21.5 L17.7,19.3 L15.5,18.5 L17.7,17.7 Z"),
    )

    /** Moneda de la app. */
    val Coin = icon(
        "coin",
        "M12,2.75 a9.25,9.25 0 1,0 0.01,0 Z",
        "M12,6.5 a5.5,5.5 0 1,0 0.01,0 Z",
        fills = listOf("M12,8.6 L13.1,11 L15.4,12 L13.1,13 L12,15.4 L10.9,13 L8.6,12 L10.9,11 Z"),
        width = 1.6f,
    )
    val Bolt = icon("bolt", fills = listOf("M13.5,2 L4.5,13.5 H11 L10,22 L19.5,9.5 H13 Z"))

    /** Escritorio con lámpara (estudio). */
    val Study = icon(
        "study",
        "M2.5,15.5 H21.5 M4.5,15.5 V21 M19.5,15.5 V21",
        "M14,15.5 H19 M16.5,15.5 L13.5,10.5 L16,6.8",
        "M13.6,4.2 L19.2,6.4 L16.6,9.6 Z",
        "M7,15.5 V11.5 H11 V15.5",
    )

    /** Libro abierto (lectura). */
    val Book = icon(
        "book",
        "M12,6.5 C10,5 7,4.6 3,5 V18.5 C7,18.1 10,18.6 12,20 C14,18.6 17,18.1 21,18.5 V5 C17,4.6 14,5 12,6.5 Z",
        "M12,6.5 V20",
    )

    /** Cama (sueño). */
    val Bed = icon(
        "bed",
        "M3,6 V19.5 M3,15.5 H21 V19.5 M21,15.5 V12.5 a2.5,2.5 0 0,0 -2.5,-2.5 H11 V15.5",
        "M5.2,12.2 a1.8,1.8 0 1,0 3.6,0 a1.8,1.8 0 1,0 -3.6,0",
    )

    val Trophy = icon(
        "trophy",
        "M7,4 H17 V10 a5,5 0 0,1 -10,0 Z",
        "M7,6 H4 V8 a3,3 0 0,0 3,3 M17,6 H20 V8 a3,3 0 0,1 -3,3 M12,15 V18 M8,21 H16 M9.5,18 H14.5 V21 H9.5 Z",
    )
    val Bell = icon(
        "bell",
        "M6,16.5 V11 a6,6 0 0,1 12,0 V16.5 L19.5,18 H4.5 Z",
        "M10,20.5 a2,2 0 0,0 4,0",
    )
    val Import = icon(
        "import",
        "M12,3.5 V14.5", "M7.5,10 L12,14.5 L16.5,10",
        "M4,15.5 V18.5 a2,2 0 0,0 2,2 H18 a2,2 0 0,0 2,-2 V15.5",
    )
    val Paste = icon(
        "paste",
        "M8,4.5 H6.5 a2,2 0 0,0 -2,2 V19 a2,2 0 0,0 2,2 H17.5 a2,2 0 0,0 2,-2 V6.5 a2,2 0 0,0 -2,-2 H16",
        "M9,3 H15 V6 H9 Z", "M8,11 H16 M8,14.5 H16 M8,18 H13",
    )
    val File = icon(
        "file",
        "M14,3 H6.5 a1.5,1.5 0 0,0 -1.5,1.5 V19.5 a1.5,1.5 0 0,0 1.5,1.5 H17.5 a1.5,1.5 0 0,0 1.5,-1.5 V8 Z",
        "M14,3 V8 H19", "M8.5,13 H15.5 M8.5,16.5 H13",
    )
    val Templates = icon(
        "templates",
        "M5,4 H9.5 a1,1 0 0,1 1,1 V9.5 a1,1 0 0,1 -1,1 H5 a1,1 0 0,1 -1,-1 V5 a1,1 0 0,1 1,-1 Z",
        "M14.5,4 H19 a1,1 0 0,1 1,1 V9.5 a1,1 0 0,1 -1,1 H14.5 a1,1 0 0,1 -1,-1 V5 a1,1 0 0,1 1,-1 Z",
        "M5,13.5 H9.5 a1,1 0 0,1 1,1 V19 a1,1 0 0,1 -1,1 H5 a1,1 0 0,1 -1,-1 V14.5 a1,1 0 0,1 1,-1 Z",
        "M14.5,13.5 H19 a1,1 0 0,1 1,1 V19 a1,1 0 0,1 -1,1 H14.5 a1,1 0 0,1 -1,-1 V14.5 a1,1 0 0,1 1,-1 Z",
    )
    val Export = icon(
        "export",
        "M12,14.5 V3.5", "M7.5,8 L12,3.5 L16.5,8",
        "M4,15.5 V18.5 a2,2 0 0,0 2,2 H18 a2,2 0 0,0 2,-2 V15.5",
    )
    val Clock = icon("clock", "M3.5,12 a8.5,8.5 0 1,0 17,0 a8.5,8.5 0 1,0 -17,0", "M12,7 V12 L15.5,14")
    val Repeat = icon(
        "repeat",
        "M4,11 V9.5 a3,3 0 0,1 3,-3 H18.5", "M15.5,3.5 L18.5,6.5 L15.5,9.5",
        "M20,13 V14.5 a3,3 0 0,1 -3,3 H5.5", "M8.5,20.5 L5.5,17.5 L8.5,14.5",
    )
    val Flag = icon("flag", "M5.5,21 V4", "M5.5,4.5 H17 L14.5,8.5 L17,12.5 H5.5")
    val Notes = icon("notes", "M4.5,6 H19.5 M4.5,10.5 H19.5 M4.5,15 H14.5 M4.5,19.5 H11")
    val Calendar = icon(
        "calendar",
        "M5,5 H19 a1.5,1.5 0 0,1 1.5,1.5 V19 a1.5,1.5 0 0,1 -1.5,1.5 H5 a1.5,1.5 0 0,1 -1.5,-1.5 V6.5 A1.5,1.5 0 0,1 5,5 Z",
        "M3.5,9.5 H20.5 M8,3 V6.5 M16,3 V6.5",
        fills = listOf("M7,12.5 h2.5 v2.5 h-2.5 Z", "M11,12.5 h2.5 v2.5 h-2.5 Z", "M15,12.5 h2.5 v2.5 h-2.5 Z", "M7,16.5 h2.5 v2.5 h-2.5 Z"),
    )
    val Lock = icon(
        "lock",
        "M6.5,10.5 H17.5 a1.5,1.5 0 0,1 1.5,1.5 V19 a1.5,1.5 0 0,1 -1.5,1.5 H6.5 a1.5,1.5 0 0,1 -1.5,-1.5 V12 a1.5,1.5 0 0,1 1.5,-1.5 Z",
        "M8,10.5 V7.5 a4,4 0 0,1 8,0 V10.5",
    )
}
