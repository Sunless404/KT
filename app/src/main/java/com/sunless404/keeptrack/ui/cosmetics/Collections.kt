package com.sunless404.keeptrack.ui.cosmetics

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

private val PI_F = PI.toFloat()
private val UP = -PI_F / 2
private fun c(hex: Long) = Color(hex)
private fun deg(a: Float) = a * 180f / PI_F

/** Marcos de las colecciones (segunda versión, más trabajada). */
object Frames {

    // =============== ALUCARD (Hellsing) ===============
    private val BLOOD = c(0xFFB3001B)
    private val BLOOD_HOT = c(0xFFFF2A3D)
    private val BLOOD_DARK = c(0xFF3D0008)

    /** El que te gustó: se queda igual. */
    private val alucardSeal = FrameStyle("alucard_c", "Sello Cromwell", "Alucard",
        back = { t, c, r -> glow(c, r * 1.5f, BLOOD, 0.35f + 0.15f * tri(t * 2)) },
        front = { t, c, r ->
            rotate(t * 360f / 2, c) {
                ring(c, r * 1.12f, BLOOD, 2f)
                ring(c, r * 1.32f, BLOOD, 2f)
                for (i in 0 until 24) {
                    val a = TAU * i / 24
                    val len = if (i % 3 == 0) 0.2f else 0.1f
                    drawLine(BLOOD, polar(c, r * 1.12f, a), polar(c, r * (1.12f + len), a), 2f)
                }
                val star = Path()
                for (k in 0..5) {
                    val p = polar(c, r * 1.32f, UP + TAU * (k * 2 % 5) / 5)
                    if (k == 0) star.moveTo(p.x, p.y) else star.lineTo(p.x, p.y)
                }
                drawPath(star, BLOOD.copy(alpha = 0.5f), style = Stroke(1.5f))
            }
            ring(c, r + 3, BLOOD_DARK, 5f)
        })

    val alucard = listOf(
        alucardSeal,
        FrameStyle("alucard_e", "Mil ojos", "Alucard",
            back = { t, c, r ->
                // Oscuridad que respira, y de ella se abren ojos rojos a destiempo.
                smoke(c, r * 0.95f, t, Color.Black.copy(alpha = 0.9f), n = 22, reach = r * 0.5f, speed = 0.35f)
                bloom(c, r * 1.5f, BLOOD, 0.25f)
            },
            front = { t, c, r ->
                for (i in 0 until 9) {
                    val a = TAU * i / 9 + 0.3f
                    val e = polar(c, r * (1.18f + 0.1f * ((i * 7) % 3)), a)
                    val open = (sin((t * 2 + i * 0.37f) * TAU) * 1.4f - 0.2f).coerceIn(0f, 1f)
                    if (open <= 0f) continue
                    val w = r * 0.13f
                    rotate(deg(a) + 90, e) {
                        val lid = Path().apply {
                            moveTo(e.x - w, e.y)
                            quadraticTo(e.x, e.y - w * 0.75f * open, e.x + w, e.y)
                            quadraticTo(e.x, e.y + w * 0.55f * open, e.x - w, e.y)
                            close()
                        }
                        bloom(e, w * 2f, BLOOD_HOT, 0.4f * open)
                        drawPath(lid, Brush.radialGradient(listOf(c(0xFFFFE066), BLOOD_HOT, BLOOD), e, w))
                        drawOval(Color.Black, Offset(e.x - w * 0.09f, e.y - w * 0.4f * open), Size(w * 0.18f, w * 0.8f * open))
                    }
                }
                neonRing(c, r + 3, BLOOD, 2.5f, BLOOD_HOT)
            }),
        FrameStyle("alucard_f", "Familiares", "Alucard",
            back = { t, c, r ->
                bloom(c, r * 1.55f, BLOOD, 0.35f)
                // Niebla de sangre que gira.
                rotate(t * 90f, c) {
                    for (k in 0 until 3) drawArc(
                        Brush.sweepGradient(listOf(Color.Transparent, BLOOD.copy(alpha = 0.55f), Color.Transparent), c),
                        k * 120f, 100f, false, Offset(c.x - r * 1.25f, c.y - r * 1.25f), Size(r * 2.5f, r * 2.5f), style = Stroke(r * 0.28f),
                    )
                }
            },
            front = { t, c, r ->
                neonRing(c, r + 3, BLOOD, 3f, BLOOD_HOT)
                // Enjambre de murciélagos en espiral, con ojos rojos.
                for (i in 0 until 9) {
                    val ph = frac(t * 0.6f + i / 9f)
                    val a = TAU * ph * 1.3f + i
                    val rr = r * (1.08f + 0.35f * ph)
                    val p = polar(c, rr, a)
                    val s = r * (0.1f + 0.08f * (1 - abs(ph - 0.5f) * 2))
                    bat(p, s, tri(t * 8 + i * 0.3f), c(0xFF0B0003).copy(alpha = 1f - ph * 0.5f), deg(a) + 90)
                    drawCircle(BLOOD_HOT.copy(alpha = 1f - ph), s * 0.08f, p, blendMode = BlendMode.Plus)
                }
                // Gotas que caen desde abajo.
                for (i in 0 until 3) {
                    val ph = frac(t * 1.1f + i * 0.33f)
                    val x = c.x + (i - 1) * r * 0.35f
                    drawCircle(BLOOD.copy(alpha = 1 - ph), 3.5f, Offset(x, c.y + r + 6 + ph * r * 0.45f))
                }
            }),
    )

    // =============== AMATERASU (Itachi) ===============
    private val INK = c(0xFF07070B)
    private val SHARINGAN = c(0xFFD7141E)
    private val EMBER = c(0xFFFF4A2A)

    val amaterasu = listOf(
        FrameStyle("amaterasu_a", "Amaterasu", "Amaterasu",
            back = { t, c, r ->
                // Fuego negro y espeso (lento) con borde carmesí y brasas dentro.
                bloom(c, r * 1.6f, c(0xFF8B0A1A), 0.45f)
                fire(c, r, t, c(0xFFB0142A).copy(alpha = 0.9f), c(0xFF5A0A14), c(0xFF2A0508), count = 24, len = r * 0.62f, speed = 0.7f, additive = true)
                fire(c, r, t + 0.13f, INK, INK, c(0xFF15050A), count = 24, len = r * 0.5f, speed = 0.7f, additive = false)
            },
            front = { t, c, r ->
                drawCircle(INK, r + 4, c, style = Stroke(7f))
                sparks(c, r * 1.05f, t, EMBER, n = 10, speed = 0.5f, size = 2f)
            }),
        FrameStyle("amaterasu_b", "Bandada de cuervos", "Amaterasu",
            back = { t, c, r ->
                // Luna roja del genjutsu detrás.
                val moon = Offset(c.x + r * 0.75f, c.y - r * 0.75f)
                bloom(moon, r * 0.9f, SHARINGAN, 0.55f)
                drawCircle(c(0xFFB3121C), r * 0.42f, moon)
                drawCircle(c(0xFF8C0E16), r * 0.42f, Offset(moon.x - r * 0.08f, moon.y + r * 0.06f), style = Stroke(r * 0.06f))
            },
            front = { t, c, r ->
                drawCircle(INK, r + 3, c, style = Stroke(6f))
                drawCircle(SHARINGAN, r + 3, c, style = Stroke(1.5f))
                // Cuervos que salen del marco y se deshacen en plumas.
                for (i in 0 until 6) {
                    val ph = frac(t * 0.5f + i / 6f)
                    val a = UP + TAU * (i / 6f) + ph * 1.2f
                    val p = polar(c, r * (1.1f + ph * 0.4f), a)
                    val alpha = if (ph < 0.75f) 1f else (1 - ph) * 4
                    crow(p, r * 0.28f, tri(t * 6 + i * 0.4f), INK.copy(alpha = alpha), deg(a) - 90)
                    drawCircle(SHARINGAN.copy(alpha = alpha), r * 0.025f, polar(p, r * 0.1f, a + 1.2f), blendMode = BlendMode.Plus)
                    if (ph > 0.6f) for (k in 0 until 3) {
                        val fp = (ph - 0.6f) * 2.5f
                        feather(Offset(p.x + (k - 1) * r * 0.12f * fp, p.y + r * 0.3f * fp), r * 0.07f, fp * 300f + k * 60, c(0xFF2A2A33).copy(alpha = 1 - fp))
                    }
                }
            }),
        FrameStyle("amaterasu_c", "Mangekyō Sharingan", "Amaterasu",
            back = { t, c, r ->
                // Iris del Sharingan: anillo rojo con el patrón de tres aspas curvas que gira.
                val act = tri(t) // se acelera y se calma
                bloom(c, r * 1.55f, SHARINGAN, 0.35f + 0.3f * act)
                drawCircle(Brush.radialGradient(listOf(c(0xFFFF3B3B), SHARINGAN, c(0xFF6E0008)), c, r * 1.4f), r * 1.4f, c)
                rotate(deg(t * TAU) + act * 120f, c) {
                    for (k in 0 until 3) {
                        val a = TAU * k / 3
                        val blade = Path().apply {
                            moveTo(polar(c, r * 0.9f, a).x, polar(c, r * 0.9f, a).y)
                            val tip = polar(c, r * 1.4f, a + 1.05f)
                            val c1 = polar(c, r * 1.5f, a + 0.2f)
                            quadraticTo(c1.x, c1.y, tip.x, tip.y)
                            val c2 = polar(c, r * 1.05f, a + 0.85f)
                            quadraticTo(c2.x, c2.y, polar(c, r * 0.9f, a + 0.55f).x, polar(c, r * 0.9f, a + 0.55f).y)
                            close()
                        }
                        drawPath(blade, INK)
                    }
                }
                drawCircle(INK, r * 1.4f, c, style = Stroke(r * 0.06f))
            },
            front = { _, c, r -> drawCircle(INK, r + 2, c, style = Stroke(4f)) }),
    )

    // =============== SUSANOO (Madara / Sasuke) ===============
    private val SUS_P = c(0xFF9B5CFF)
    private val SUS_P_LIGHT = c(0xFFE2CCFF)
    private val SUS_B = c(0xFF3D8BFF)
    private val SUS_B_LIGHT = c(0xFFCFE4FF)

    val susanoo = listOf(
        FrameStyle("susanoo_a", "Susanoo esquelético", "Susanoo",
            back = { t, c, r ->
                bloom(c, r * 1.7f, SUS_P, 0.4f)
                fire(c, r * 1.1f, t, SUS_P.copy(alpha = 0.55f), SUS_P.copy(alpha = 0.35f), SUS_P_LIGHT.copy(alpha = 0.4f), count = 20, len = r * 0.45f, speed = 1.1f, width = 0.18f)
                // Costillas de chakra que abrazan la foto por detrás, con un pulso que las recorre.
                for (side in listOf(-1f, 1f)) for (k in 0 until 5) {
                    val y = c.y - r * 0.8f + k * r * 0.4f
                    val rib = Path().apply {
                        moveTo(c.x + side * r * 0.4f, y)
                        cubicTo(c.x + side * r * 1.3f, y - r * 0.3f, c.x + side * r * 1.5f, y + r * 0.35f, c.x + side * r * 0.95f, y + r * 0.55f)
                    }
                    neon(rib, SUS_P, r * 0.06f, SUS_P_LIGHT, 0.55f + 0.45f * tri(t * 1.5f - k * 0.12f))
                }
            },
            front = { t, c, r -> neonRing(c, r + 2, SUS_P, 2f, SUS_P_LIGHT, 0.7f + 0.3f * tri(t * 2)) }),
        FrameStyle("susanoo_b", "Armadura de Madara", "Susanoo",
            back = { t, c, r ->
                bloom(c, r * 1.7f, SUS_B, 0.45f)
                fire(c, r * 1.3f, t, SUS_B.copy(alpha = 0.5f), SUS_B.copy(alpha = 0.3f), SUS_B_LIGHT.copy(alpha = 0.35f), count = 22, len = r * 0.28f, speed = 1.3f)
            },
            front = { t, c, r ->
                // Escamas de armadura superpuestas (dos filas), de cristal azul.
                for (row in 0 until 2) {
                    val n = 14
                    for (i in 0 until n) {
                        val a0 = TAU * (i + row * 0.5f) / n
                        val r1 = r * (1.04f + row * 0.13f)
                        val seg = ringSegment(c, r1, r1 + r * 0.17f, a0 + 0.02f, a0 + TAU / n - 0.02f)
                        val lit = wave(t, i + row * 20, 1.2f)
                        drawPath(seg, Brush.radialGradient(listOf(SUS_B_LIGHT.copy(alpha = 0.55f + 0.3f * lit), SUS_B.copy(alpha = 0.5f)), c, r1 + r * 0.17f))
                        drawPath(seg, SUS_B_LIGHT.copy(alpha = 0.9f), style = Stroke(1.3f))
                    }
                }
                neonRing(c, r + 2, SUS_B, 2f, SUS_B_LIGHT)
            }),
        FrameStyle("susanoo_c", "Arco de Sasuke", "Susanoo",
            back = { t, c, r ->
                bloom(c, r * 1.6f, SUS_P, 0.4f)
                smoke(c, r, t, SUS_P.copy(alpha = 0.35f), n = 16, reach = r * 0.35f, speed = 0.6f)
                // Arco de chakra a la izquierda; la cuerda y la flecha quedan detrás de la foto.
                val pull = tri(t)
                val bowTop = Offset(c.x - r * 0.45f, c.y - r * 1.32f)
                val bowBot = Offset(c.x - r * 0.45f, c.y + r * 1.32f)
                val nock = Offset(c.x + r * 0.2f * pull, c.y)
                val string = Path().apply { moveTo(bowTop.x, bowTop.y); lineTo(nock.x, nock.y); lineTo(bowBot.x, bowBot.y) }
                neon(string, SUS_P_LIGHT, 1.5f, Color.White, 0.8f)
                val arrow = Path().apply { moveTo(nock.x, nock.y); lineTo(c.x - r * 1.7f, c.y) }
                neon(arrow, SUS_P, 2.5f, Color.White, 0.6f + 0.4f * pull)
                bloom(Offset(c.x - r * 1.7f, c.y), r * 0.3f * (0.5f + pull), SUS_P_LIGHT, 0.7f)
            },
            front = { _, c, r ->
                neonRing(c, r + 3, SUS_P, 2.5f, SUS_P_LIGHT)
                val bow = Path().apply {
                    moveTo(c.x - r * 0.45f, c.y - r * 1.32f)
                    quadraticTo(c.x - r * 1.75f, c.y, c.x - r * 0.45f, c.y + r * 1.32f)
                }
                neon(bow, SUS_P, r * 0.07f, SUS_P_LIGHT)
            }),
    )

    // =============== KILLUA (HxH) ===============
    private val VOLT = c(0xFF62D0FF)
    private val VOLT_LIGHT = c(0xFFE6F8FF)

    val killua = listOf(
        FrameStyle("killua_a", "Godspeed", "Killua",
            back = { t, c, r ->
                val flash = if ((t * 10).toInt() % 7 == 0) 0.6f else 0f
                bloom(c, r * 1.65f, VOLT, 0.3f + flash)
                sparks(c, r, t, VOLT_LIGHT, n = 18, speed = 1.6f, swirl = true, size = 1.8f)
            },
            front = { t, c, r ->
                neonRing(c, r + 3, VOLT, 2.5f)
                val seed = (t * 24).toInt()
                for (i in 0 until 4) {
                    val a = TAU * ((i * 0.27f + seed * 0.083f) % 1f)
                    lightning(polar(c, r + 3, a), polar(c, r * 1.45f, a + 0.35f), seed * 3 + i, VOLT, 2f, branches = 2)
                }
            }),
        FrameStyle("killua_b", "Rhythm Echo", "Killua",
            back = { t, c, r -> bloom(c, r * 1.5f, VOLT, 0.25f) },
            front = { t, c, r ->
                // Imágenes residuales: copias del anillo que se quedan atrás.
                for (k in 3 downTo 1) {
                    val a = TAU * (t - k * 0.04f)
                    val o = polar(c, r * 0.1f, a)
                    drawCircle(VOLT.copy(alpha = 0.18f * (4 - k)), r + 3, o, style = Stroke(3f))
                }
                neonRing(c, r + 3, VOLT_LIGHT, 2.5f, Color.White)
                // Yo-yo con cuerda.
                val a = TAU * t * 1.5f
                val y = polar(c, r * 1.32f, a)
                drawLine(c(0xFFDDE6EF), polar(c, r + 3, a - 0.5f), y, 1.2f)
                drawCircle(Brush.radialGradient(listOf(c(0xFFEFF5FA), c(0xFF8A99A8)), y, r * 0.15f), r * 0.15f, y)
                drawCircle(VOLT, r * 0.15f, y, style = Stroke(2f))
                bloom(y, r * 0.25f, VOLT, 0.5f)
            }),
        FrameStyle("killua_c", "Asesino Zoldyck", "Killua",
            back = { t, c, r ->
                bloom(c, r * 1.55f, c(0xFF6B4CFF), 0.3f)
                // Pelo blanco en punta alrededor (estética de Killua).
                for (i in 0 until 18) {
                    val a = UP + (i - 8.5f) * 0.17f
                    val h = r * (0.22f + 0.18f * ((i * 7) % 5) / 5f) * (0.9f + 0.1f * wave(t, i))
                    val spike = Path().apply {
                        moveTo(polar(c, r * 0.98f, a - 0.09f).x, polar(c, r * 0.98f, a - 0.09f).y)
                        lineTo(polar(c, r + h, a + 0.05f).x, polar(c, r + h, a + 0.05f).y)
                        lineTo(polar(c, r * 0.98f, a + 0.09f).x, polar(c, r * 0.98f, a + 0.09f).y)
                        close()
                    }
                    drawPath(spike, Brush.radialGradient(listOf(Color.White, c(0xFFC8D2E0)), c, r + h))
                }
            },
            front = { t, c, r ->
                drawCircle(c(0xFF2B2440), r + 3, c, style = Stroke(5f))
                // Zarpazos que aparecen en orden con chispas.
                for (k in 0 until 3) {
                    val show = ((frac(t) - k * 0.1f) * 4f).coerceIn(0f, 1f) * (1 - ((frac(t) - 0.7f) * 3.3f).coerceIn(0f, 1f))
                    val from = Offset(c.x + r * (0.45f + k * 0.18f), c.y - r * 1.2f)
                    val to = Offset(c.x + r * (1.15f + k * 0.12f), c.y - r * (0.2f - k * 0.05f))
                    val end = Offset(from.x + (to.x - from.x) * show, from.y + (to.y - from.y) * show)
                    if (show > 0f) neon(Path().apply { moveTo(from.x, from.y); lineTo(end.x, end.y) }, VOLT, 2.5f)
                }
            }),
    )

    // =============== BATMAN (DC) ===============
    private val GOTHAM = c(0xFF0B1220)
    private val SIGNAL = c(0xFFFFE27A)

    val batman = listOf(
        FrameStyle("batman_a", "Batiseñal", "Batman",
            back = { t, c, r ->
                drawCircle(Brush.verticalGradient(listOf(c(0xFF1B2740), GOTHAM), c.y - r * 1.45f, c.y + r * 1.45f), r * 1.45f, c)
                // Nubes que se mueven.
                for (i in 0 until 5) {
                    val x = c.x + r * (((i * 0.37f + t * 0.15f) % 1f) * 3f - 1.5f)
                    val y = c.y - r * (0.85f + 0.12f * (i % 2))
                    drawOval(c(0xFF2A3754).copy(alpha = 0.8f), Offset(x - r * 0.4f, y - r * 0.1f), Size(r * 0.8f, r * 0.22f))
                }
                // Haz de luz que barre y proyecta el emblema en las nubes.
                val sweep = sin(t * TAU) * 0.22f
                val top = polar(c, r * 1.12f, UP + sweep)
                val beam = Path().apply {
                    moveTo(c.x + r * 0.9f, c.y + r * 1.45f)
                    lineTo(top.x - r * 0.38f, top.y)
                    lineTo(top.x + r * 0.38f, top.y)
                    close()
                }
                drawPath(beam, Brush.linearGradient(listOf(SIGNAL.copy(alpha = 0.04f), SIGNAL.copy(alpha = 0.35f)), Offset(c.x + r, c.y + r * 1.4f), top), blendMode = BlendMode.Plus)
                drawOval(Brush.radialGradient(listOf(SIGNAL, SIGNAL.copy(alpha = 0.5f)), top, r * 0.4f), Offset(top.x - r * 0.4f, top.y - r * 0.24f), Size(r * 0.8f, r * 0.48f))
                bat(top, r * 0.3f, 0.5f, GOTHAM)
                // Lluvia.
                for (i in 0 until 16) {
                    val ph = frac(t * 2.5f + i * 0.37f)
                    val x = c.x + r * ((i * 29 % 30) / 10f - 1.5f)
                    val y = c.y - r * 1.45f + ph * r * 2.9f
                    drawLine(c(0x88A9BCD6), Offset(x, y), Offset(x - 3, y + r * 0.12f), 1f)
                }
            },
            front = { _, c, r -> drawCircle(c(0xFF2B354A), r + 2, c, style = Stroke(4f)) }),
        FrameStyle("batman_b", "Caballero oscuro", "Batman",
            back = { t, c, r ->
                val flash = if (frac(t) in 0.62f..0.66f || frac(t) in 0.7f..0.72f) 1f else 0f
                drawCircle(c(0xFF9FB4D9).copy(alpha = 0.35f * flash), r * 1.5f, c, blendMode = BlendMode.Plus)
                // Capa que envuelve la foto, con pliegues y borde iluminado.
                val wind = sin(t * TAU) * r * 0.05f
                val cape = Path().apply {
                    moveTo(c.x - r * 0.3f, c.y - r * 0.95f)
                    cubicTo(c.x - r * 1.5f, c.y - r * 0.6f, c.x - r * 1.6f + wind, c.y + r * 0.6f, c.x - r * 1.35f + wind, c.y + r * 1.35f)
                    for (k in 0 until 6) {
                        val x0 = c.x - r * 1.35f + k * r * 2.7f / 6
                        val x1 = x0 + r * 2.7f / 6
                        quadraticTo((x0 + x1) / 2, c.y + r * 1.0f + wind * (k % 2), x1 + if (k == 5) wind else 0f, c.y + r * 1.35f)
                    }
                    cubicTo(c.x + r * 1.6f + wind, c.y + r * 0.6f, c.x + r * 1.5f, c.y - r * 0.6f, c.x + r * 0.3f, c.y - r * 0.95f)
                    close()
                }
                drawPath(cape, Brush.horizontalGradient(listOf(c(0xFF141A26), c(0xFF242D40), c(0xFF141A26)), c.x - r * 1.5f, c.x + r * 1.5f))
                drawPath(cape, c(0xFF6C7A96).copy(alpha = 0.6f + 0.4f * flash), style = Stroke(1.5f))
                // Orejas de la máscara sobre el marco.
                for (side in listOf(-1f, 1f)) {
                    val ear = Path().apply {
                        moveTo(c.x + side * r * 0.25f, c.y - r * 0.97f)
                        lineTo(c.x + side * r * 0.42f, c.y - r * 1.38f)
                        lineTo(c.x + side * r * 0.55f, c.y - r * 0.85f)
                        close()
                    }
                    drawPath(ear, c(0xFF141A26))
                    drawPath(ear, c(0xFF6C7A96), style = Stroke(1.2f))
                }
            },
            front = { _, c, r -> drawCircle(c(0xFF1C2333), r + 2, c, style = Stroke(5f)) }),
        FrameStyle("batman_c", "Gotham", "Batman",
            back = { t, c, r ->
                drawCircle(Brush.verticalGradient(listOf(c(0xFF2A1F3D), c(0xFF0D1424)), c.y - r * 1.45f, c.y + r), r * 1.45f, c)
                // Luna con bandada de murciélagos cruzando.
                val moon = Offset(c.x - r * 0.75f, c.y - r * 0.8f)
                bloom(moon, r * 0.7f, c(0xFFFFF4D1), 0.35f)
                drawCircle(c(0xFFFFF1C9), r * 0.3f, moon)
                for (i in 0 until 6) {
                    val ph = frac(t * 0.5f + i * 0.04f)
                    val p = Offset(c.x - r * 1.5f + ph * r * 3f, c.y - r * 0.9f + sin(ph * 6 + i) * r * 0.1f + (i % 3) * r * 0.08f)
                    bat(p, r * 0.07f, tri(t * 10 + i * 0.3f), Color.Black)
                }
                // Skyline en arco alrededor de la parte de abajo, con ventanas.
                for (i in 0 until 16) {
                    val a = PI_F * 0.15f + PI_F * 0.7f * i / 15
                    val h = r * (0.25f + ((i * 37) % 10) / 22f)
                    val base = polar(c, r * 1.45f, a)
                    rotate(deg(a) - 90, base) {
                        drawRect(c(0xFF151B2A), Offset(base.x - r * 0.09f, base.y - h), Size(r * 0.18f, h))
                        if ((i + (t * 6).toInt()) % 3 == 0) drawRect(SIGNAL, Offset(base.x - r * 0.03f, base.y - h * 0.6f), Size(r * 0.05f, r * 0.05f))
                    }
                }
            },
            front = { _, c, r -> drawCircle(c(0xFF2B354A), r + 2, c, style = Stroke(4f)) }),
    )

    // =============== SENTRY (Marvel) ===============
    private val SUN = c(0xFFFFD34D)
    private val SUN_LIGHT = c(0xFFFFF6D6)
    private val VOID = c(0xFF120720)
    private val VOID_RIM = c(0xFF6A2BC2)

    val sentry = listOf(
        FrameStyle("sentry_a", "Guardián dorado", "Sentry",
            back = { t, c, r ->
                bloom(c, r * 1.8f, SUN, 0.55f)
                // Corona solar con dos juegos de rayos girando en sentidos opuestos.
                for ((dir, n, len) in listOf(Triple(1f, 18, 0.5f), Triple(-1f, 12, 0.32f))) {
                    rotate(dir * t * 60f, c) {
                        for (i in 0 until n) {
                            val a = TAU * i / n
                            val l = r * len * (0.7f + 0.3f * wave(t, i, 2f))
                            val ray = Path().apply {
                                moveTo(polar(c, r, a - 0.06f).x, polar(c, r, a - 0.06f).y)
                                lineTo(polar(c, r + l, a).x, polar(c, r + l, a).y)
                                lineTo(polar(c, r, a + 0.06f).x, polar(c, r, a + 0.06f).y)
                                close()
                            }
                            drawPath(ray, Brush.radialGradient(listOf(SUN_LIGHT, SUN.copy(alpha = 0f)), c, r + l), blendMode = BlendMode.Plus)
                        }
                    }
                }
            },
            front = { t, c, r ->
                neonRing(c, r + 3, SUN, 3f, SUN_LIGHT)
                // Destello que recorre el anillo.
                val p = polar(c, r + 3, UP + t * TAU)
                bloom(p, r * 0.35f, SUN_LIGHT, 0.9f)
                drawLine(SUN_LIGHT, Offset(p.x - r * 0.25f, p.y), Offset(p.x + r * 0.25f, p.y), 1.5f, blendMode = BlendMode.Plus)
                drawLine(SUN_LIGHT, Offset(p.x, p.y - r * 0.25f), Offset(p.x, p.y + r * 0.25f), 1.5f, blendMode = BlendMode.Plus)
            }),
        FrameStyle("sentry_b", "The Void", "Sentry",
            back = { t, c, r ->
                drawCircle(Brush.radialGradient(listOf(VOID, VOID.copy(alpha = 0.8f), Color.Transparent), c, r * 1.6f), r * 1.6f, c)
                // Tentáculos de sombra que se retuercen, con borde violeta.
                for (i in 0 until 11) {
                    val a = TAU * i / 11 + sin(t * TAU + i) * 0.1f
                    val len = r * (0.45f + 0.15f * wave(t, i, 0.8f))
                    val m = polar(c, r + len * 0.5f, a + 0.35f * sin(t * TAU + i * 0.7f))
                    val e = polar(c, r + len, a - 0.3f * sin(t * TAU * 1.3f + i))
                    val tent = Path().apply {
                        moveTo(polar(c, r * 0.95f, a).x, polar(c, r * 0.95f, a).y)
                        quadraticTo(m.x, m.y, e.x, e.y)
                    }
                    drawPath(tent, VOID_RIM.copy(alpha = 0.5f), style = Stroke(r * 0.11f, cap = StrokeCap.Round))
                    drawPath(tent, VOID, style = Stroke(r * 0.08f, cap = StrokeCap.Round))
                }
            },
            front = { t, c, r ->
                drawCircle(VOID, r + 3, c, style = Stroke(6f))
                drawCircle(VOID_RIM, r + 3, c, style = Stroke(1.5f))
                // Dos ojos rojos que se abren en la oscuridad.
                val open = (sin(t * TAU) * 2f - 0.5f).coerceIn(0f, 1f)
                for (side in listOf(-1f, 1f)) {
                    val e = Offset(c.x + side * r * 0.32f, c.y - r * 1.22f)
                    bloom(e, r * 0.25f, c(0xFFFF2244), 0.6f * open)
                    drawOval(c(0xFFFF4D6A), Offset(e.x - r * 0.12f, e.y - r * 0.03f * open), Size(r * 0.24f, r * 0.06f * open + 0.5f))
                }
            }),
        FrameStyle("sentry_c", "Dualidad", "Sentry",
            back = { t, c, r ->
                rotate(t * 60f, c) {
                    clipHalf(c, r * 1.6f, true) { bloom(c, r * 1.6f, SUN, 0.65f) }
                    clipHalf(c, r * 1.6f, false) { drawCircle(Brush.radialGradient(listOf(VOID, Color.Transparent), c, r * 1.6f), r * 1.6f, c) }
                    // Costura de luz entre la luz y la sombra.
                    val seam = Path().apply { moveTo(c.x - r * 1.45f, c.y); lineTo(c.x + r * 1.45f, c.y) }
                    neon(seam, SUN, 1.5f, SUN_LIGHT, 0.7f)
                }
            },
            front = { t, c, r ->
                rotate(t * 60f, c) {
                    drawArc(SUN, 180f, 180f, false, Offset(c.x - r - 3, c.y - r - 3), Size((r + 3) * 2, (r + 3) * 2), style = Stroke(5f))
                    drawArc(VOID_RIM, 0f, 180f, false, Offset(c.x - r - 3, c.y - r - 3), Size((r + 3) * 2, (r + 3) * 2), style = Stroke(5f))
                }
            }),
    )

    // =============== IRON MAN (Marvel) ===============
    private val ARC = c(0xFF7FE7FF)
    private val ARC_LIGHT = c(0xFFE8FCFF)
    private val HOT_ROD = c(0xFFA3111A)
    private val GOLD = c(0xFFE0A93A)

    val ironMan = listOf(
        FrameStyle("ironman_a", "Reactor arc", "Iron Man",
            back = { t, c, r -> bloom(c, r * 1.65f, ARC, 0.4f + 0.2f * tri(t * 2)) },
            front = { t, c, r ->
                // Carcasa metálica.
                val shell = ringSegment(c, r * 1.02f, r * 1.42f, 0f, TAU * 0.9999f)
                metal(shell, c(0xFF7C8794), c(0xFFE6EDF3), c(0xFF2C333B), frac(t * 0.5f), Rect(c, r * 1.42f))
                // Bobinas de cobre (10) con luz azul entre ellas.
                for (i in 0 until 10) {
                    val a0 = TAU * i / 10 + 0.05f
                    val coil = ringSegment(c, r * 1.1f, r * 1.32f, a0, a0 + TAU / 10 - 0.1f)
                    drawPath(coil, Brush.radialGradient(listOf(c(0xFFE39A5B), c(0xFF8A4A22)), c, r * 1.32f))
                    for (k in 1..3) {
                        val a = a0 + (TAU / 10 - 0.1f) * k / 4
                        drawLine(c(0xFF5A2E14), polar(c, r * 1.1f, a), polar(c, r * 1.32f, a), 1.2f)
                    }
                    val gap = a0 - 0.05f
                    drawLine(ARC.copy(alpha = 0.6f + 0.4f * wave(t, i, 3f)), polar(c, r * 1.08f, gap), polar(c, r * 1.34f, gap), 3f, blendMode = BlendMode.Plus)
                }
                neonRing(c, r * 1.05f, ARC, 2f, ARC_LIGHT)
                neonRing(c, r * 1.38f, ARC, 1.5f, ARC_LIGHT, 0.6f)
            }),
        FrameStyle("ironman_b", "Armadura Mark", "Iron Man",
            back = { _, c, r -> bloom(c, r * 1.5f, GOLD, 0.2f) },
            front = { t, c, r ->
                // Placas rojas y doradas con biseles y un brillo que las recorre.
                val n = 6
                for (i in 0 until n) {
                    val a0 = UP + TAU * i / n + 0.03f
                    val a1 = a0 + TAU / n - 0.06f
                    val outer = ringSegment(c, r * 1.02f, r * 1.38f, a0, a1)
                    val red = i % 3 != 0
                    metal(outer, if (red) HOT_ROD else GOLD, if (red) c(0xFFFF6B6B) else c(0xFFFFF0B3), if (red) c(0xFF4A0508) else c(0xFF6B4A10), frac(t * 0.7f + i / n.toFloat()), Rect(c, r * 1.38f))
                    val inner = ringSegment(c, r * 1.12f, r * 1.28f, a0 + 0.12f, a1 - 0.12f)
                    drawPath(inner, Color.Black.copy(alpha = 0.18f))
                }
                // Luces de los propulsores en los huecos.
                for (i in 0 until n) {
                    val a = UP + TAU * i / n
                    bloom(polar(c, r * 1.2f, a), r * 0.12f, ARC, 0.5f + 0.4f * wave(t, i, 2f))
                }
            }),
        FrameStyle("ironman_c", "HUD de J.A.R.V.I.S.", "Iron Man",
            back = { _, c, r -> drawCircle(Brush.radialGradient(listOf(ARC.copy(alpha = 0.12f), Color.Transparent), c, r * 1.5f), r * 1.5f, c) },
            front = { t, c, r ->
                rotate(t * 360f, c) {
                    for (i in 0 until 60) {
                        val a = TAU * i / 60
                        val big = i % 5 == 0
                        drawLine(ARC.copy(alpha = if (big) 0.9f else 0.35f), polar(c, r * 1.1f, a), polar(c, r * (if (big) 1.2f else 1.15f), a), if (big) 1.8f else 1f)
                    }
                }
                rotate(-t * 180f, c) {
                    for (k in 0 until 3) neonArc(c, r * 1.32f, k * 120f, 70f, ARC, 1.8f, ARC_LIGHT)
                }
                // Retícula que se fija en un objetivo.
                val lock = frac(t * 1.5f)
                val target = polar(c, r * 1.32f, UP + lock * TAU)
                val s = r * (0.25f - 0.12f * (lock * 3).coerceAtMost(1f))
                for (q in 0 until 4) {
                    val corner = Path().apply {
                        val sx = if (q % 2 == 0) -1 else 1
                        val sy = if (q < 2) -1 else 1
                        moveTo(target.x + sx * s, target.y + sy * s * 0.5f)
                        lineTo(target.x + sx * s, target.y + sy * s)
                        lineTo(target.x + sx * s * 0.5f, target.y + sy * s)
                    }
                    neon(corner, c(0xFFFFC857), 1.3f, Color.White, 0.9f)
                }
                neonRing(c, r + 2, ARC, 1.8f, ARC_LIGHT)
            }),
    )

    // =============== SPIDER-MAN (Marvel) ===============
    private val SPIDEY_RED = c(0xFFD3202A)
    private val SPIDEY_BLUE = c(0xFF1F55C0)

    val spiderMan = listOf(
        FrameStyle("spiderman_a", "Telaraña con rocío", "Spider-Man",
            back = { t, c, r ->
                val silk = c(0xCCEDEDED)
                val spokes = 14
                for (i in 0 until spokes) {
                    val a = TAU * i / spokes + 0.07f * ((i * 3) % 2)
                    drawLine(silk, polar(c, r * 0.9f, a), polar(c, r * 1.5f, a), 1f)
                }
                // Espiral de seda que cuelga entre radios.
                for (k in 0 until 4) {
                    val rr = r * (1.07f + k * 0.12f)
                    val p = Path()
                    for (i in 0..spokes) {
                        val a = TAU * i / spokes
                        val pt = polar(c, rr, a)
                        if (i == 0) p.moveTo(pt.x, pt.y) else {
                            val mid = polar(c, rr * 0.95f, a - TAU / spokes / 2)
                            p.quadraticTo(mid.x, mid.y, pt.x, pt.y)
                        }
                    }
                    drawPath(p, silk, style = Stroke(1f))
                }
                // Gotas de rocío que brillan.
                for (i in 0 until 10) {
                    val a = TAU * ((i * 0.618f) % 1f)
                    val p = polar(c, r * (1.13f + (i % 3) * 0.12f), a)
                    val sp = wave(t, i, 1.5f)
                    drawCircle(Color.White.copy(alpha = 0.5f + 0.5f * sp), 2.2f, p)
                    if (sp > 0.85f) bloom(p, 9f, Color.White, 0.7f)
                }
            },
            front = { _, c, r ->
                drawArc(SPIDEY_RED, 180f, 180f, false, Offset(c.x - r - 3, c.y - r - 3), Size((r + 3) * 2, (r + 3) * 2), style = Stroke(5f))
                drawArc(SPIDEY_BLUE, 0f, 180f, false, Offset(c.x - r - 3, c.y - r - 3), Size((r + 3) * 2, (r + 3) * 2), style = Stroke(5f))
            }),
        FrameStyle("spiderman_b", "Traje", "Spider-Man",
            front = { t, c, r ->
                // Anillo con el traje: rojo arriba con malla negra, azul a los lados, y la araña del pecho.
                val band = ringSegment(c, r * 1.02f, r * 1.36f, 0f, TAU * 0.9999f)
                drawPath(band, Brush.sweepGradient(listOf(SPIDEY_BLUE, SPIDEY_RED, SPIDEY_RED, SPIDEY_RED, SPIDEY_BLUE, SPIDEY_BLUE, SPIDEY_BLUE), c))
                for (i in 0 until 32) {
                    val a = TAU * i / 32
                    drawLine(Color.Black.copy(alpha = 0.55f), polar(c, r * 1.02f, a), polar(c, r * 1.36f, a), 1f)
                }
                for (k in 1..2) drawCircle(Color.Black.copy(alpha = 0.55f), r * (1.02f + k * 0.11f), c, style = Stroke(1f))
                // Brillo que recorre el traje.
                val sh = UP + t * TAU
                drawArc(Color.White.copy(alpha = 0.25f), deg(sh), 30f, false, Offset(c.x - r * 1.19f, c.y - r * 1.19f), Size(r * 2.38f, r * 2.38f), style = Stroke(r * 0.3f), blendMode = BlendMode.Plus)
                val at = Offset(c.x, c.y - r * 1.19f)
                drawCircle(SPIDEY_RED, r * 0.17f, at)
                spider(at, r * 0.16f, Color.Black, t)
            }),
        FrameStyle("spiderman_c", "Spider-Verse", "Spider-Man",
            back = { t, c, r ->
                // Puntos de cómic (trama) alrededor.
                for (ring in 0 until 4) {
                    val rr = r * (1.08f + ring * 0.1f)
                    val n = 26 + ring * 6
                    for (i in 0 until n) {
                        val a = TAU * i / n + ring * 0.1f
                        drawCircle(c(0xFFFF4FA3).copy(alpha = 0.55f), 2.6f - ring * 0.5f, polar(c, rr, a))
                    }
                }
            },
            front = { t, c, r ->
                // Fallo de imagen: aberración cromática que tiembla a ratos.
                val glitch = if ((t * 20).toInt() % 6 == 0) r * 0.08f else r * 0.02f
                drawCircle(c(0xFF00E5FF), r + 3, Offset(c.x - glitch, c.y), style = Stroke(4f), blendMode = BlendMode.Plus)
                drawCircle(c(0xFFFF2E63), r + 3, Offset(c.x + glitch, c.y), style = Stroke(4f), blendMode = BlendMode.Plus)
                drawCircle(Color.White, r + 3, c, style = Stroke(1.5f))
                if ((t * 20).toInt() % 6 == 0) {
                    for (k in 0 until 3) {
                        val y = c.y + r * (((k * 41) % 20) / 10f - 1f)
                        drawRect(c(0xFF00E5FF).copy(alpha = 0.6f), Offset(c.x - r * 1.4f, y), Size(r * 2.8f, 3f), blendMode = BlendMode.Plus)
                    }
                }
                // Onomatopeya tipo cómic.
                val bubble = Offset(c.x + r * 1.0f, c.y - r * 1.05f)
                val pop = tri(t * 2)
                scale(0.7f + 0.3f * pop, bubble) {
                    val star = Path()
                    for (k in 0..16) {
                        val rr = if (k % 2 == 0) r * 0.3f else r * 0.18f
                        val p = polar(bubble, rr, TAU * k / 16)
                        if (k == 0) star.moveTo(p.x, p.y) else star.lineTo(p.x, p.y)
                    }
                    drawPath(star, c(0xFFFFE600))
                    drawPath(star, Color.Black, style = Stroke(2f))
                }
            }),
    )

    // =============== ASTA (Black Clover) ===============
    private val ANTI = c(0xFF0D0D0D)
    private val ANTI_EDGE = c(0xFFD6D6D6)
    private val CLOVER = c(0xFF2FAF4F)

    val asta = listOf(
        FrameStyle("asta_a", "Antimagia", "Asta",
            back = { t, c, r ->
                bloom(c, r * 1.55f, Color.White, 0.08f)
                // Llamas de antimagia: negras, irregulares, con borde blanco tipo tinta de manga.
                fire(c, r, t, ANTI_EDGE.copy(alpha = 0.8f), c(0xFF6A6A6A), c(0xFF2A2A2A), count = 30, len = r * 0.58f, speed = 2.6f, additive = false, width = 0.1f)
                fire(c, r, t + 0.07f, ANTI, ANTI, c(0xFF1A1A1A), count = 30, len = r * 0.5f, speed = 2.6f, additive = false, width = 0.09f)
                // Partículas negras que se desprenden.
                for (i in 0 until 10) {
                    val ph = frac(t * 0.9f + i * 0.1f)
                    val a = TAU * ((i * 0.37f) % 1f)
                    val p = polar(c, r * (1.2f + ph * 0.4f), a)
                    rect(p, 5f * (1 - ph), 5f * (1 - ph), ANTI)
                }
            },
            front = { _, c, r ->
                drawCircle(ANTI, r + 3, c, style = Stroke(6f))
                drawCircle(ANTI_EDGE, r + 6, c, style = Stroke(1.2f))
            }),
        FrameStyle("asta_b", "Grimorio de cinco hojas", "Asta",
            back = { t, c, r -> bloom(c, r * 1.5f, CLOVER, 0.18f + 0.12f * tri(t * 2)) },
            front = { t, c, r ->
                drawCircle(ANTI, r + 3, c, style = Stroke(6f))
                // Grimorio negro que flota arriba, con el trébol de cinco hojas en la tapa.
                val bob = sin(t * TAU) * r * 0.05f
                val at = Offset(c.x, c.y - r * 1.2f + bob)
                val w = r * 0.62f
                val h = r * 0.48f
                rotate(sin(t * TAU) * 4f, at) {
                    drawRoundRect(c(0xFF151515), Offset(at.x - w / 2, at.y - h / 2), Size(w, h), androidx.compose.ui.geometry.CornerRadius(4f))
                    drawRoundRect(c(0xFF6A5A3A), Offset(at.x - w / 2, at.y - h / 2), Size(w, h), androidx.compose.ui.geometry.CornerRadius(4f), style = Stroke(1.5f))
                    clover(at, h * 0.36f, 5, c(0xFF2A2A2A))
                    clover(at, h * 0.3f, 5, c(0xFF0A0A0A))
                    drawCircle(CLOVER, 2f, at)
                }
                bloom(at, r * 0.5f, CLOVER, 0.25f)
                // Hojas que caen.
                for (i in 0 until 4) {
                    val ph = frac(t * 0.7f + i * 0.25f)
                    val p = Offset(c.x + (i - 1.5f) * r * 0.35f + sin(ph * 8 + i) * r * 0.1f, c.y - r * 0.9f + ph * r * 2.3f)
                    clover(p, r * 0.06f, 1, CLOVER.copy(alpha = 1 - ph), rotation = ph * 6)
                }
            }),
        FrameStyle("asta_c", "Modo demonio", "Asta",
            back = { t, c, r ->
                bloom(c, r * 1.6f, c(0xFF404040), 0.3f)
                // Ala de demonio a un lado, con membranas y borde blanco; aletea.
                val flap = sin(t * TAU) * r * 0.08f
                val wing = Path().apply {
                    moveTo(c.x - r * 0.55f, c.y - r * 0.55f)
                    cubicTo(c.x - r * 0.9f, c.y - r * 1.75f - flap, c.x - r * 1.7f, c.y - r * 1.4f - flap, c.x - r * 1.6f, c.y - r * 0.15f)
                    quadraticTo(c.x - r * 1.35f, c.y - r * 0.4f, c.x - r * 1.3f, c.y + r * 0.1f)
                    quadraticTo(c.x - r * 1.08f, c.y - r * 0.2f, c.x - r * 1.0f, c.y + r * 0.35f)
                    quadraticTo(c.x - r * 0.85f, c.y - r * 0.05f, c.x - r * 0.7f, c.y + r * 0.2f)
                    close()
                }
                drawPath(wing, Brush.radialGradient(listOf(c(0xFF2B2B2B), ANTI), Offset(c.x - r, c.y - r * 0.6f), r * 1.3f))
                for (k in 0 until 3) {
                    val bone = Path().apply {
                        moveTo(c.x - r * 0.62f, c.y - r * 0.5f)
                        quadraticTo(c.x - r * (1.0f + k * 0.15f), c.y - r * (1.3f - k * 0.25f) - flap, c.x - r * (1.6f - k * 0.3f), c.y - r * (0.15f - k * 0.17f))
                    }
                    drawPath(bone, c(0xFF5A5A5A), style = Stroke(1.5f))
                }
                drawPath(wing, ANTI_EDGE, style = Stroke(1.5f))
                // Cuerno a un lado.
                val horn = Path().apply {
                    moveTo(c.x + r * 0.35f, c.y - r * 0.92f)
                    quadraticTo(c.x + r * 0.6f, c.y - r * 1.45f, c.x + r * 0.95f, c.y - r * 1.5f)
                    quadraticTo(c.x + r * 0.7f, c.y - r * 1.2f, c.x + r * 0.6f, c.y - r * 0.82f)
                    close()
                }
                drawPath(horn, ANTI)
                drawPath(horn, ANTI_EDGE, style = Stroke(1.2f))
            },
            front = { t, c, r ->
                drawCircle(ANTI, r + 3, c, style = Stroke(6f))
                val e = Offset(c.x + r * 0.5f, c.y - r * 0.98f)
                bloom(e, r * 0.18f, c(0xFFFF3030), 0.4f + 0.4f * tri(t * 2))
            }),
    )

    // =============== CHROLLO (HxH) ===============
    private val TROUPE = c(0xFF0B0B10)
    private val BONE = c(0xFFEDE8DC)
    private val NEN = c(0xFF9B5CFF)

    val chrollo = listOf(
        FrameStyle("chrollo_a", "Líder de la Brigada", "Chrollo",
            back = { t, c, r ->
                // Cuello de piel negra del abrigo, esponjoso.
                for (i in 0 until 46) {
                    val a = TAU * i / 46
                    val h = r * (0.16f + 0.1f * wave(t, i, 0.6f))
                    val tip = polar(c, r + h, a + 0.05f * sin(t * TAU + i))
                    drawLine(c(0xFF22222A), polar(c, r * 0.97f, a), tip, r * 0.1f, StrokeCap.Round)
                    drawLine(c(0xFF6E6E7C), polar(c, r * 1.06f, a), tip, r * 0.025f, StrokeCap.Round)
                }
            },
            front = { t, c, r ->
                // Cruz invertida en la frente y la araña de la Brigada con su número 0.
                val top = Offset(c.x, c.y - r * 1.25f + sin(t * TAU) * 2f)
                bloom(top, r * 0.35f, BONE, 0.3f)
                cross(top, r * 0.2f, BONE, inverted = true, width = r * 0.05f)
                val at = Offset(c.x, c.y + r * 1.2f)
                drawCircle(BONE, r * 0.3f, at)
                spider(at, r * 0.26f, TROUPE, t)
                drawCircle(BONE, r * 0.07f, Offset(at.x, at.y + r * 0.05f))
                drawCircle(TROUPE, r * 0.045f, Offset(at.x, at.y + r * 0.05f), style = Stroke(1.6f))
            }),
        FrameStyle("chrollo_b", "Ladrón de habilidades", "Chrollo",
            back = { t, c, r ->
                bloom(c, r * 1.6f, NEN, 0.3f)
                // Páginas robadas que orbitan, cada una con su símbolo.
                for (i in 0 until 6) {
                    val a = TAU * (i / 6f + t * 0.25f)
                    val p = polar(c, r * 1.3f, a)
                    rotate(deg(a) + 90 + sin(t * TAU * 2 + i) * 10, p) {
                        drawRect(BONE.copy(alpha = 0.9f), Offset(p.x - r * 0.1f, p.y - r * 0.13f), Size(r * 0.2f, r * 0.26f))
                        drawCircle(NEN, r * 0.04f, p, style = Stroke(1.5f))
                    }
                }
            },
            front = { t, c, r ->
                drawCircle(TROUPE, r + 3, c, style = Stroke(6f))
                // Libro abierto abajo que pasa páginas y suelta chispas de nen.
                val at = Offset(c.x, c.y + r * 1.15f)
                val flip = tri(t * 2)
                val left = Path().apply { moveTo(at.x, at.y); lineTo(at.x - r * 0.45f, at.y - r * 0.12f); lineTo(at.x - r * 0.45f, at.y + r * 0.22f); lineTo(at.x, at.y + r * 0.32f); close() }
                val right = Path().apply { moveTo(at.x, at.y); lineTo(at.x + r * 0.45f, at.y - r * 0.12f); lineTo(at.x + r * 0.45f, at.y + r * 0.22f); lineTo(at.x, at.y + r * 0.32f); close() }
                drawPath(left, c(0xFF2A2233)); drawPath(right, c(0xFF2A2233))
                val px = at.x + r * 0.43f * (1 - 2 * flip)
                val page = Path().apply {
                    moveTo(at.x, at.y); lineTo(px, at.y - r * 0.1f - r * 0.15f * (1 - abs(1 - 2 * flip)))
                    lineTo(px, at.y + r * 0.2f); lineTo(at.x, at.y + r * 0.3f); close()
                }
                drawPath(page, BONE)
                cross(Offset(at.x - r * 0.22f, at.y + r * 0.07f), r * 0.08f, BONE, inverted = true, width = 2f)
                sparks(Offset(at.x, at.y - r * 0.2f), r * 0.3f, t, NEN, n = 8, speed = 1f, size = 1.8f)
            }),
        FrameStyle("chrollo_c", "Réquiem", "Chrollo",
            back = { t, c, r ->
                drawCircle(Brush.radialGradient(listOf(TROUPE.copy(alpha = 0.9f), Color.Transparent), c, r * 1.6f), r * 1.6f, c)
                // Cruces invertidas que aparecen y se desvanecen alrededor, como en un funeral.
                for (i in 0 until 8) {
                    val ph = frac(t * 0.5f + i / 8f)
                    val a = TAU * i / 8
                    val p = polar(c, r * (1.25f + 0.1f * ph), a)
                    cross(p, r * 0.1f, BONE.copy(alpha = (1 - abs(ph - 0.5f) * 2)), inverted = true, width = 2f)
                }
            },
            front = { _, c, r ->
                drawCircle(BONE.copy(alpha = 0.9f), r + 3, c, style = Stroke(1.5f))
                drawCircle(TROUPE, r + 6, c, style = Stroke(3f))
            }),
    )

    val all: List<List<FrameStyle>> = listOf(alucard, amaterasu, susanoo, killua, batman, sentry, ironMan, spiderMan, asta, chrollo)
}

/** Dibuja solo la mitad superior ([top]) o inferior de un círculo. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.clipHalf(c: Offset, r: Float, top: Boolean, block: androidx.compose.ui.graphics.drawscope.DrawScope.() -> Unit) {
    val rect = if (top) Rect(c.x - r, c.y - r, c.x + r, c.y) else Rect(c.x - r, c.y, c.x + r, c.y + r)
    drawContext.canvas.save()
    drawContext.canvas.clipRect(rect)
    block()
    drawContext.canvas.restore()
}
