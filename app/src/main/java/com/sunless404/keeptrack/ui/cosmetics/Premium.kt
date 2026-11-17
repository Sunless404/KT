package com.sunless404.keeptrack.ui.cosmetics

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.lerp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

private val PI_F = PI.toFloat()
private val UP = -PI_F / 2
private fun c(hex: Long) = Color(hex)
private fun deg(a: Float) = a * 180f / PI_F

/**
 * Marcos de colección, versión premium. Estructura de cada uno:
 * anillo con material + remate superior + adornos laterales + pieza inferior + efectos.
 */
object Frames {

    // ======================= ALUCARD (Hellsing) =======================
    private val BLOOD = c(0xFFB3001B)
    private val BLOOD_HOT = c(0xFFFF2A3D)
    private val BLOOD_DARK = c(0xFF3D0008)
    private val OBSIDIAN = c(0xFF120709)

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

    private val alucardCount = FrameStyle("alucard_count", "Conde no muerto", "Alucard",
        back = { t, c, r ->
            smoke(c, r * 1.05f, t, Color.Black.copy(alpha = 0.85f), n = 20, reach = r * 0.45f, speed = 0.3f)
            bloom(c, r * 1.6f, BLOOD, 0.3f + 0.1f * tri(t * 2))
            // Alas de murciélago a los lados.
            val flap = tri(t * 2)
            batWing(Offset(c.x - r * 0.95f, c.y - r * 0.05f), -1f, r * 0.72f, c(0xFF1A0508), c(0xFF5C0E18), flap)
            batWing(Offset(c.x + r * 0.95f, c.y - r * 0.05f), 1f, r * 0.72f, c(0xFF1A0508), c(0xFF5C0E18), flap)
        },
        front = { t, c, r ->
            // Anillo de obsidiana con pinchos góticos.
            for (i in 0 until 12) {
                val a = TAU * i / 12
                spike(c, r * 1.12f, a, r * (if (i % 3 == 0) 0.2f else 0.11f), 0.08f,
                    Brush.radialGradient(listOf(c(0xFF4A0A12), OBSIDIAN), c, r * 1.35f), BLOOD.copy(alpha = 0.7f))
            }
            bevelRing(c, r * 1.0f, r * 1.14f, c(0xFF2A060C), c(0xFF8E1C2A), OBSIDIAN)
            // Remate: ojo rojo en un arco gótico.
            val e = Offset(c.x, c.y - r * 1.32f)
            val arch = Path().apply {
                moveTo(e.x - r * 0.32f, e.y + r * 0.18f)
                quadraticTo(e.x - r * 0.32f, e.y - r * 0.2f, e.x, e.y - r * 0.36f)
                quadraticTo(e.x + r * 0.32f, e.y - r * 0.2f, e.x + r * 0.32f, e.y + r * 0.18f)
                close()
            }
            drawPath(arch, Brush.verticalGradient(listOf(c(0xFF3A0A10), OBSIDIAN), e.y - r * 0.36f, e.y + r * 0.18f))
            drawPath(arch, BLOOD, style = Stroke(1.5f))
            val open = (sin(t * TAU) * 1.6f + 0.4f).coerceIn(0.05f, 1f)
            bloom(e, r * 0.3f, BLOOD_HOT, 0.5f * open)
            drawOval(Brush.radialGradient(listOf(c(0xFFFFD45A), BLOOD_HOT, BLOOD), e, r * 0.16f),
                Offset(e.x - r * 0.17f, e.y - r * 0.08f * open), Size(r * 0.34f, r * 0.16f * open))
            drawOval(Color.Black, Offset(e.x - r * 0.025f, e.y - r * 0.07f * open), Size(r * 0.05f, r * 0.14f * open))
            // Pieza inferior: rubí que gotea.
            val g = Offset(c.x, c.y + r * 1.2f)
            gem(g, r * 0.11f, BLOOD_HOT, t)
            for (i in 0 until 2) {
                val ph = frac(t * 1.2f + i * 0.5f)
                drawCircle(BLOOD.copy(alpha = 1 - ph), 3f, Offset(g.x + (i - 0.5f) * r * 0.08f, g.y + r * 0.15f + ph * r * 0.3f))
            }
            sparks(c, r * 1.1f, t, BLOOD_HOT, n = 8, speed = 0.4f, size = 1.6f)
        })

    val alucard = listOf(alucardSeal, alucardCount)

    // ======================= AMATERASU (Itachi) =======================
    private val INK = c(0xFF07070B)
    private val SHARINGAN = c(0xFFD7141E)
    private val CRIMSON = c(0xFF8B0A1A)

    /** Mangekyō de Itachi: tres aspas curvas alrededor de una pupila. */
    private fun DrawScope.mangekyo(at: Offset, s: Float, rot: Float) {
        drawCircle(Brush.radialGradient(listOf(c(0xFFFF3B3B), SHARINGAN, c(0xFF6E0008)), at, s), s, at)
        rotate(rot, at) {
            for (k in 0 until 3) {
                val a = TAU * k / 3
                val blade = Path().apply {
                    moveTo(polar(at, s * 0.2f, a).x, polar(at, s * 0.2f, a).y)
                    val tip = polar(at, s * 0.98f, a + 1.2f)
                    val c1 = polar(at, s * 1.05f, a + 0.25f)
                    quadraticTo(c1.x, c1.y, tip.x, tip.y)
                    val c2 = polar(at, s * 0.55f, a + 0.95f)
                    quadraticTo(c2.x, c2.y, polar(at, s * 0.2f, a + 0.7f).x, polar(at, s * 0.2f, a + 0.7f).y)
                    close()
                }
                drawPath(blade, INK)
            }
        }
        drawCircle(INK, s * 0.22f, at)
        drawCircle(INK, s, at, style = Stroke(s * 0.08f))
    }

    private val amaterasuFlames = FrameStyle("amaterasu_flames", "Amaterasu", "Amaterasu",
        back = { t, c, r ->
            bloom(c, r * 1.65f, CRIMSON, 0.4f)
            // Fuego negro denso y lento que sube por los lados (borde carmesí).
            fire(c, r * 1.05f, t, c(0xFFB0142A).copy(alpha = 0.9f), c(0xFF5A0A14), c(0xFF2A0508),
                count = 16, len = r * 0.62f, speed = 0.6f, from = PI_F * 0.05f, to = PI_F * 0.95f + PI_F)
            fire(c, r * 1.05f, t + 0.1f, INK, INK, c(0xFF1A060A), count = 16, len = r * 0.52f, speed = 0.6f, additive = false,
                from = PI_F * 0.05f, to = PI_F * 0.95f + PI_F)
            sparks(c, r, t, c(0xFFFF4A2A), n = 10, speed = 0.45f, size = 1.8f)
        },
        front = { t, c, r ->
            bevelRing(c, r * 1.0f, r * 1.13f, c(0xFF1A1A22), c(0xFF5A5A66), INK)
            drawCircle(SHARINGAN, r * 1.065f, c, style = Stroke(1.2f))
            // Remate: Mangekyō de Itachi con un anillo de llama negra.
            val e = Offset(c.x, c.y - r * 1.3f)
            fire(e, r * 0.22f, t, c(0xFFB0142A), INK, INK, count = 10, len = r * 0.16f, speed = 0.8f, additive = false)
            mangekyo(e, r * 0.22f, t * 120f)
            // Pieza inferior: cuervo posado con ojo rojo que aletea de vez en cuando.
            val perch = Offset(c.x + r * 0.15f, c.y + r * 1.12f)
            val flap = if (frac(t) > 0.8f) tri((frac(t) - 0.8f) * 10f) else 0.5f
            crow(perch, r * 0.32f, flap, INK, 0f)
            drawCircle(SHARINGAN, r * 0.025f, Offset(perch.x + r * 0.17f, perch.y - r * 0.03f), blendMode = BlendMode.Plus)
        })

    private val tsukuyomi = FrameStyle("amaterasu_tsukuyomi", "Tsukuyomi", "Amaterasu",
        back = { t, c, r ->
            // Mundo rojo invertido: cielo carmesí con luna negra y nubes.
            drawCircle(Brush.radialGradient(listOf(c(0xFFE0182A), c(0xFF7A0010), c(0xFF2A0006)), c, r * 1.6f), r * 1.55f, c)
            val moon = Offset(c.x + r * 0.85f, c.y - r * 0.85f)
            drawCircle(INK, r * 0.38f, moon)
            drawCircle(SHARINGAN, r * 0.38f, moon, style = Stroke(2f))
            for (i in 0 until 4) {
                val x = c.x - r * 1.5f + frac(t * 0.2f + i * 0.25f) * r * 3f
                val y = c.y - r * (0.7f + 0.2f * (i % 2))
                drawOval(INK.copy(alpha = 0.55f), Offset(x - r * 0.35f, y - r * 0.07f), Size(r * 0.7f, r * 0.14f))
            }
            // Cuervos que cruzan.
            for (i in 0 until 4) {
                val ph = frac(t * 0.5f + i * 0.25f)
                crow(Offset(c.x - r * 1.5f + ph * r * 3f, c.y + r * (0.4f + 0.3f * sin(ph * 6 + i))), r * 0.16f, tri(t * 6 + i), INK, 0f)
            }
        },
        front = { t, c, r ->
            bevelRing(c, r * 1.0f, r * 1.12f, c(0xFF2A0006), SHARINGAN, INK)
            // Remate y base: nubes rojas con borde blanco (estética de la organización).
            for ((at, s) in listOf(Offset(c.x, c.y - r * 1.2f) to 1f, Offset(c.x, c.y + r * 1.2f) to 0.8f)) {
                val w = r * 0.55f * s
                val cloud = Path().apply {
                    moveTo(at.x - w, at.y + w * 0.15f)
                    quadraticTo(at.x - w * 1.05f, at.y - w * 0.3f, at.x - w * 0.55f, at.y - w * 0.28f)
                    quadraticTo(at.x - w * 0.4f, at.y - w * 0.6f, at.x, at.y - w * 0.42f)
                    quadraticTo(at.x + w * 0.4f, at.y - w * 0.6f, at.x + w * 0.55f, at.y - w * 0.28f)
                    quadraticTo(at.x + w * 1.05f, at.y - w * 0.3f, at.x + w, at.y + w * 0.15f)
                    close()
                }
                drawPath(cloud, c(0xFFC8101E))
                drawPath(cloud, Color.White, style = Stroke(2f))
            }
            bloom(Offset(c.x, c.y - r * 1.2f), r * 0.4f, SHARINGAN, 0.3f + 0.2f * tri(t * 2))
        })

    val amaterasu = listOf(amaterasuFlames, tsukuyomi)

    // ======================= SUSANOO =======================
    private val SUS_B = c(0xFF3D8BFF)
    private val SUS_B_LIGHT = c(0xFFCFE4FF)
    private val SUS_P = c(0xFF9B5CFF)
    private val SUS_P_LIGHT = c(0xFFE2CCFF)

    private fun DrawScope.chakraHorns(top: Offset, s: Float, col: Color, light: Color) {
        for (side in listOf(-1f, 1f)) {
            val horn = Path().apply {
                moveTo(top.x + side * s * 0.15f, top.y + s * 0.3f)
                cubicTo(top.x + side * s * 0.5f, top.y - s * 0.1f, top.x + side * s * 0.65f, top.y - s * 0.7f, top.x + side * s * 0.95f, top.y - s * 0.95f)
                cubicTo(top.x + side * s * 0.55f, top.y - s * 0.55f, top.x + side * s * 0.35f, top.y - s * 0.05f, top.x + side * s * 0.05f, top.y + s * 0.1f)
                close()
            }
            drawPath(horn, Brush.linearGradient(listOf(light.copy(alpha = 0.9f), col.copy(alpha = 0.6f)), top, Offset(top.x + side * s, top.y - s)))
            drawPath(horn, light, style = Stroke(1.2f))
        }
    }

    private val madara = FrameStyle("susanoo_madara", "Susanoo perfecto (Madara)", "Susanoo",
        back = { t, c, r ->
            bloom(c, r * 1.75f, SUS_B, 0.45f)
            fire(c, r * 1.15f, t, SUS_B.copy(alpha = 0.55f), SUS_B.copy(alpha = 0.32f), SUS_B_LIGHT.copy(alpha = 0.35f), count = 24, len = r * 0.4f, speed = 1.2f)
            // Hombreras de armadura a los lados (escamas superpuestas).
            for (side in listOf(-1f, 1f)) for (k in 0 until 4) {
                val y = c.y - r * 0.35f + k * r * 0.26f
                val plate = Path().apply {
                    addRoundRect(androidx.compose.ui.geometry.RoundRect(
                        Rect(c.x + side * r * (1.08f + k * 0.04f) - r * 0.22f, y - r * 0.11f, c.x + side * r * (1.08f + k * 0.04f) + r * 0.22f, y + r * 0.11f),
                        CornerRadius(r * 0.08f)))
                }
                drawPath(plate, Brush.verticalGradient(listOf(SUS_B_LIGHT.copy(alpha = 0.75f), SUS_B.copy(alpha = 0.6f)), y - r * 0.11f, y + r * 0.11f))
                drawPath(plate, SUS_B_LIGHT, style = Stroke(1f))
            }
        },
        front = { t, c, r ->
            // Anillo de cristal azul.
            bevelRing(c, r * 1.0f, r * 1.13f, SUS_B.copy(alpha = 0.75f), SUS_B_LIGHT, c(0xFF0E2A66), rotation = t * 0.2f)
            // Remate: casco con cuernos de chakra y joya central.
            val top = Offset(c.x, c.y - r * 1.18f)
            chakraHorns(top, r * 0.45f, SUS_B, SUS_B_LIGHT)
            gem(Offset(top.x, top.y + r * 0.02f), r * 0.09f, c(0xFF7FC4FF), t)
            // Base: tres magatama que giran.
            for (i in 0 until 3) {
                val a = PI_F / 2 + (i - 1) * 0.35f + sin(t * TAU) * 0.05f
                magatama(polar(c, r * 1.27f, a), r * 0.08f, a + t * TAU, SUS_B_LIGHT)
            }
            sparks(c, r * 1.05f, t, SUS_B_LIGHT, n = 10, speed = 0.7f, size = 1.6f)
        })

    private val sasuke = FrameStyle("susanoo_sasuke", "Susanoo de Sasuke", "Susanoo",
        back = { t, c, r ->
            bloom(c, r * 1.7f, SUS_P, 0.45f)
            fire(c, r * 1.12f, t, SUS_P.copy(alpha = 0.5f), SUS_P.copy(alpha = 0.3f), SUS_P_LIGHT.copy(alpha = 0.35f), count = 22, len = r * 0.38f, speed = 1.3f)
            // Costillas detrás.
            for (side in listOf(-1f, 1f)) for (k in 0 until 4) {
                val y = c.y - r * 0.55f + k * r * 0.38f
                val rib = Path().apply {
                    moveTo(c.x + side * r * 0.5f, y)
                    cubicTo(c.x + side * r * 1.3f, y - r * 0.25f, c.x + side * r * 1.45f, y + r * 0.3f, c.x + side * r * 1.0f, y + r * 0.5f)
                }
                neon(rib, SUS_P, r * 0.05f, SUS_P_LIGHT, 0.6f + 0.4f * tri(t * 1.5f - k * 0.12f))
            }
            // Chidori: rayos azules que saltan alrededor.
            val seed = (t * 16).toInt()
            for (i in 0 until 2) {
                val a = TAU * ((seed * 0.29f + i * 0.5f) % 1f)
                lightning(polar(c, r * 1.12f, a), polar(c, r * 1.55f, a + 0.3f), seed + i * 7, c(0xFF8FD3FF), 1.6f, branches = 1)
            }
        },
        front = { t, c, r ->
            bevelRing(c, r * 1.0f, r * 1.12f, SUS_P.copy(alpha = 0.75f), SUS_P_LIGHT, c(0xFF2A0E66))
            // Remate: cresta en media luna.
            val top = Offset(c.x, c.y - r * 1.2f)
            val crescent = Path().apply {
                moveTo(top.x - r * 0.5f, top.y - r * 0.15f)
                quadraticTo(top.x, top.y + r * 0.25f, top.x + r * 0.5f, top.y - r * 0.15f)
                quadraticTo(top.x, top.y + r * 0.05f, top.x - r * 0.5f, top.y - r * 0.15f)
                close()
            }
            drawPath(crescent, Brush.horizontalGradient(listOf(SUS_P, SUS_P_LIGHT, SUS_P), top.x - r * 0.5f, top.x + r * 0.5f))
            drawPath(crescent, SUS_P_LIGHT, style = Stroke(1.2f))
            gem(Offset(top.x, top.y + r * 0.1f), r * 0.08f, SUS_P, t)
            // Base: flecha de chakra cargada.
            val pull = tri(t)
            val arrow = Path().apply { moveTo(c.x - r * 0.6f, c.y + r * 1.25f); lineTo(c.x + r * 0.6f, c.y + r * 1.25f) }
            neon(arrow, SUS_P, 2.2f, Color.White, 0.6f + 0.4f * pull)
            val head = Path().apply {
                moveTo(c.x + r * 0.72f, c.y + r * 1.25f); lineTo(c.x + r * 0.55f, c.y + r * 1.16f); lineTo(c.x + r * 0.55f, c.y + r * 1.34f); close()
            }
            drawPath(head, SUS_P_LIGHT)
            bloom(Offset(c.x + r * 0.7f, c.y + r * 1.25f), r * 0.25f * (0.5f + pull), SUS_P_LIGHT, 0.7f)
        })

    val susanoo = listOf(madara, sasuke)

    // ======================= KILLUA (HxH) =======================
    private val VOLT = c(0xFF62D0FF)
    private val VOLT_LIGHT = c(0xFFE6F8FF)
    private val SILVER = c(0xFFB8C4D2)

    private val godspeed = FrameStyle("killua_godspeed", "Godspeed", "Killua",
        back = { t, c, r ->
            val flash = if ((t * 10).toInt() % 7 == 0) 0.5f else 0f
            bloom(c, r * 1.7f, VOLT, 0.3f + flash)
            // Pelo blanco en punta detrás del remate.
            for (i in 0 until 15) {
                val a = UP + (i - 7f) * 0.13f
                val h = r * (0.3f + 0.2f * (((i * 7) % 5) / 5f)) * (0.92f + 0.08f * wave(t, i, 1.5f))
                val spikePath = Path().apply {
                    moveTo(polar(c, r * 1.05f, a - 0.08f).x, polar(c, r * 1.05f, a - 0.08f).y)
                    quadraticTo(polar(c, r * 1.2f + h * 0.6f, a - 0.02f).x, polar(c, r * 1.2f + h * 0.6f, a - 0.02f).y, polar(c, r * 1.08f + h, a + 0.06f).x, polar(c, r * 1.08f + h, a + 0.06f).y)
                    lineTo(polar(c, r * 1.05f, a + 0.08f).x, polar(c, r * 1.05f, a + 0.08f).y)
                    close()
                }
                drawPath(spikePath, Brush.radialGradient(listOf(Color.White, c(0xFFC9D3E2)), c, r * 1.1f + h))
            }
            sparks(c, r * 1.05f, t, VOLT_LIGHT, n = 16, speed = 1.6f, swirl = true, size = 1.6f)
            // Rayos entre los nodos laterales, por fuera del anillo (la foto tapa lo de dentro).
            val seed = (t * 24).toInt()
            val l = Offset(c.x - r * 1.16f, c.y)
            val rgt = Offset(c.x + r * 1.16f, c.y)
            lightning(l, polar(c, r * 1.3f, UP - 0.9f), seed, VOLT, 2f, branches = 1)
            lightning(polar(c, r * 1.3f, UP - 0.9f), Offset(c.x, c.y - r * 1.3f), seed + 1, VOLT, 2f, branches = 1)
            lightning(Offset(c.x, c.y - r * 1.3f), polar(c, r * 1.3f, UP + 0.9f), seed + 2, VOLT, 2f, branches = 1)
            lightning(polar(c, r * 1.3f, UP + 0.9f), rgt, seed + 3, VOLT, 2f, branches = 1)
        },
        front = { t, c, r ->
            bevelRing(c, r * 1.0f, r * 1.12f, SILVER, Color.White, c(0xFF3A4656))
            // Nodos laterales con un arco eléctrico que salta entre ellos por arriba.
            val l = Offset(c.x - r * 1.16f, c.y)
            val rgt = Offset(c.x + r * 1.16f, c.y)
            for (n in listOf(l, rgt)) { bloom(n, r * 0.22f, VOLT, 0.7f); drawCircle(VOLT_LIGHT, r * 0.06f, n) }
            // Remate: rayo emblema.
            val e = Offset(c.x, c.y - r * 1.25f)
            val bolt = Path().apply {
                moveTo(e.x + r * 0.06f, e.y - r * 0.2f); lineTo(e.x - r * 0.1f, e.y + r * 0.02f); lineTo(e.x + r * 0.01f, e.y + r * 0.02f)
                lineTo(e.x - r * 0.06f, e.y + r * 0.22f); lineTo(e.x + r * 0.11f, e.y - r * 0.04f); lineTo(e.x, e.y - r * 0.04f); close()
            }
            bloom(e, r * 0.3f, VOLT, 0.6f)
            drawPath(bolt, VOLT_LIGHT)
            // Base: dos yo-yos colgando que se balancean.
            for (side in listOf(-1f, 1f)) {
                val sw = sin(t * TAU + side) * 0.2f
                val anchor = Offset(c.x + side * r * 0.35f, c.y + r * 1.08f)
                val y = Offset(anchor.x + sin(sw) * r * 0.3f, anchor.y + r * 0.3f)
                drawLine(c(0xFFDDE6EF), anchor, y, 1.2f)
                drawCircle(Brush.radialGradient(listOf(Color.White, c(0xFF7D8C9C)), Offset(y.x - r * 0.03f, y.y - r * 0.03f), r * 0.12f), r * 0.1f, y)
                drawCircle(VOLT, r * 0.1f, y, style = Stroke(1.5f))
            }
        })

    private val zoldyck = FrameStyle("killua_zoldyck", "Mirada Zoldyck", "Killua",
        back = { t, c, r ->
            drawCircle(Brush.radialGradient(listOf(c(0xFF2A1E4A), Color.Transparent), c, r * 1.65f), r * 1.65f, c)
            smoke(c, r, t, c(0xFF6B4CFF).copy(alpha = 0.35f), n = 14, reach = r * 0.35f, speed = 0.4f)
        },
        front = { t, c, r ->
            bevelRing(c, r * 1.0f, r * 1.12f, c(0xFF2B2440), c(0xFF8E7CC3), c(0xFF0C0A14))
            // Remate: ojos felinos de asesino que se entrecierran.
            val e = Offset(c.x, c.y - r * 1.28f)
            val open = 0.35f + 0.65f * ((sin(t * TAU) + 1) / 2)
            for (side in listOf(-1f, 1f)) {
                val p = Offset(e.x + side * r * 0.22f, e.y)
                val eye = Path().apply {
                    moveTo(p.x - r * 0.17f, p.y + side * 0f)
                    quadraticTo(p.x, p.y - r * 0.13f * open, p.x + r * 0.17f, p.y - r * 0.03f * side)
                    quadraticTo(p.x, p.y + r * 0.08f * open, p.x - r * 0.17f, p.y)
                    close()
                }
                bloom(p, r * 0.25f, VOLT, 0.4f)
                drawPath(eye, Brush.radialGradient(listOf(VOLT_LIGHT, VOLT, c(0xFF1E5D9C)), p, r * 0.17f))
                drawOval(Color.Black, Offset(p.x - r * 0.015f, p.y - r * 0.06f * open), Size(r * 0.03f, r * 0.12f * open))
            }
            // Zarpazos en diagonal que aparecen en orden.
            for (k in 0 until 3) {
                val show = ((frac(t) - k * 0.08f) * 4f).coerceIn(0f, 1f) * (1 - ((frac(t) - 0.75f) * 4f).coerceIn(0f, 1f))
                if (show <= 0f) continue
                val from = Offset(c.x + r * (0.85f + k * 0.12f), c.y + r * (0.35f - k * 0.05f))
                val to = Offset(c.x + r * (1.35f + k * 0.08f), c.y + r * (0.95f - k * 0.05f))
                neon(Path().apply { moveTo(from.x, from.y); lineTo(from.x + (to.x - from.x) * show, from.y + (to.y - from.y) * show) }, VOLT, 2.2f)
            }
            // Base: dos dagas cruzadas.
            val b = Offset(c.x, c.y + r * 1.25f)
            sword(b, r * 0.3f, 50f, SILVER)
            sword(b, r * 0.3f, -50f, SILVER)
        })

    val killua = listOf(godspeed, zoldyck)

    // ======================= BATMAN (DC) =======================
    private val GOTHAM = c(0xFF0B1220)
    private val SIGNAL = c(0xFFFFE27A)
    private val GUNMETAL = c(0xFF3B4352)

    private val darkKnight = FrameStyle("batman_knight", "Caballero oscuro", "Batman",
        back = { t, c, r ->
            val flash = if (frac(t) in 0.62f..0.66f || frac(t) in 0.7f..0.72f) 1f else 0f
            drawCircle(c(0xFF9FB4D9).copy(alpha = 0.3f * flash), r * 1.65f, c, blendMode = BlendMode.Plus)
            // Capa que envuelve el marco por detrás, con festones abajo.
            val wind = sin(t * TAU) * r * 0.05f
            val cape = Path().apply {
                moveTo(c.x - r * 0.3f, c.y - r * 1.0f)
                cubicTo(c.x - r * 1.6f, c.y - r * 0.6f, c.x - r * 1.7f + wind, c.y + r * 0.7f, c.x - r * 1.5f + wind, c.y + r * 1.45f)
                for (k in 0 until 6) {
                    val x0 = c.x - r * 1.5f + k * r * 3f / 6
                    val x1 = x0 + r * 3f / 6
                    quadraticTo((x0 + x1) / 2, c.y + r * 1.1f + wind * (k % 2), x1 + if (k == 5) wind else 0f, c.y + r * 1.45f)
                }
                cubicTo(c.x + r * 1.7f + wind, c.y + r * 0.7f, c.x + r * 1.6f, c.y - r * 0.6f, c.x + r * 0.3f, c.y - r * 1.0f)
                close()
            }
            drawPath(cape, Brush.horizontalGradient(listOf(c(0xFF111622), c(0xFF283146), c(0xFF111622)), c.x - r * 1.6f, c.x + r * 1.6f))
            drawPath(cape, c(0xFF6C7A96).copy(alpha = 0.5f + 0.5f * flash), style = Stroke(1.5f))
            // Lluvia.
            for (i in 0 until 14) {
                val ph = frac(t * 2.5f + i * 0.37f)
                val x = c.x + r * ((i * 29 % 32) / 10f - 1.6f)
                drawLine(c(0x66A9BCD6), Offset(x, c.y - r * 1.6f + ph * r * 3.2f), Offset(x - 3, c.y - r * 1.48f + ph * r * 3.2f), 1f)
            }
        },
        front = { t, c, r ->
            // Anillo de acero con el cinturón: cápsulas doradas.
            bevelRing(c, r * 1.0f, r * 1.13f, GUNMETAL, c(0xFF9AA6B8), c(0xFF151A22))
            for (i in 0 until 8) {
                val a = PI_F * 0.15f + PI_F * 0.7f * i / 7
                val p = polar(c, r * 1.065f, a)
                rotate(deg(a) + 90, p) {
                    drawRoundRect(Brush.verticalGradient(listOf(c(0xFFFFE08A), c(0xFFB8860B)), p.y - r * 0.07f, p.y + r * 0.07f),
                        Offset(p.x - r * 0.075f, p.y - r * 0.065f), Size(r * 0.15f, r * 0.13f), CornerRadius(3f))
                }
            }
            // Remate: orejas de la máscara + emblema en óvalo.
            val top = Offset(c.x, c.y - r * 1.22f)
            for (side in listOf(-1f, 1f)) {
                val ear = Path().apply {
                    moveTo(c.x + side * r * 0.22f, c.y - r * 1.05f)
                    lineTo(c.x + side * r * 0.36f, c.y - r * 1.55f)
                    lineTo(c.x + side * r * 0.5f, c.y - r * 0.98f)
                    close()
                }
                drawPath(ear, Brush.verticalGradient(listOf(c(0xFF2A3346), c(0xFF11161F)), c.y - r * 1.55f, c.y - r))
                drawPath(ear, c(0xFF6C7A96), style = Stroke(1.2f))
            }
            drawOval(Brush.radialGradient(listOf(SIGNAL, c(0xFFE0B23A)), top, r * 0.3f), Offset(top.x - r * 0.3f, top.y - r * 0.17f), Size(r * 0.6f, r * 0.34f))
            drawOval(c(0xFF6B5420), Offset(top.x - r * 0.3f, top.y - r * 0.17f), Size(r * 0.6f, r * 0.34f), style = Stroke(1.5f))
            bat(top, r * 0.24f, 0.5f, GOTHAM)
            bloom(top, r * 0.45f, SIGNAL, 0.25f + 0.15f * tri(t * 2))
            // Base: batarang plateado.
            val b = Offset(c.x, c.y + r * 1.24f)
            bat(b, r * 0.2f, 0.5f, c(0xFFB9C3D1))
        })

    private val gothamNight = FrameStyle("batman_gotham", "Noches de Gotham", "Batman",
        back = { t, c, r ->
            drawCircle(Brush.verticalGradient(listOf(c(0xFF2A1F3D), c(0xFF0D1424)), c.y - r * 1.6f, c.y + r), r * 1.6f, c)
            val moon = Offset(c.x - r * 0.85f, c.y - r * 0.9f)
            bloom(moon, r * 0.8f, c(0xFFFFF4D1), 0.35f)
            drawCircle(c(0xFFFFF1C9), r * 0.32f, moon)
            drawCircle(c(0xFFE8D9B0), r * 0.07f, Offset(moon.x + r * 0.1f, moon.y + r * 0.05f))
            // Haz de la batiseñal en el cielo.
            val sweep = sin(t * TAU) * 0.25f
            val top = polar(c, r * 1.3f, UP + 0.4f + sweep)
            val beam = Path().apply { moveTo(c.x + r * 1.0f, c.y + r * 1.5f); lineTo(top.x - r * 0.3f, top.y); lineTo(top.x + r * 0.3f, top.y); close() }
            drawPath(beam, Brush.linearGradient(listOf(SIGNAL.copy(alpha = 0.03f), SIGNAL.copy(alpha = 0.3f)), Offset(c.x + r, c.y + r * 1.5f), top), blendMode = BlendMode.Plus)
            drawOval(SIGNAL.copy(alpha = 0.85f), Offset(top.x - r * 0.3f, top.y - r * 0.18f), Size(r * 0.6f, r * 0.36f))
            bat(top, r * 0.22f, 0.5f, GOTHAM)
            // Murciélagos cruzando la luna.
            for (i in 0 until 6) {
                val ph = frac(t * 0.5f + i * 0.04f)
                bat(Offset(c.x - r * 1.6f + ph * r * 3.2f, c.y - r * 0.95f + sin(ph * 6 + i) * r * 0.1f + (i % 3) * r * 0.08f), r * 0.07f, tri(t * 10 + i * 0.3f), Color.Black)
            }
            // Skyline en arco abajo con ventanas que se encienden.
            for (i in 0 until 18) {
                val a = PI_F * 0.12f + PI_F * 0.76f * i / 17
                val h = r * (0.28f + ((i * 37) % 10) / 20f)
                val base = polar(c, r * 1.6f, a)
                rotate(deg(a) - 90, base) {
                    drawRect(c(0xFF141A28), Offset(base.x - r * 0.09f, base.y - h), Size(r * 0.18f, h))
                    if ((i + (t * 6).toInt()) % 3 == 0) drawRect(SIGNAL, Offset(base.x - r * 0.03f, base.y - h * 0.6f), Size(r * 0.05f, r * 0.05f))
                }
            }
        },
        front = { _, c, r -> bevelRing(c, r * 1.0f, r * 1.1f, GUNMETAL, c(0xFF8A96A8), c(0xFF151A22)) })

    val batman = listOf(darkKnight, gothamNight)

    // ======================= SENTRY (Marvel) =======================
    private val SUN = c(0xFFFFD34D)
    private val SUN_LIGHT = c(0xFFFFF6D6)
    private val GOLD_DARK = c(0xFF8A5A0B)
    private val VOID = c(0xFF120720)
    private val VOID_RIM = c(0xFF6A2BC2)

    private val goldenGuardian = FrameStyle("sentry_guardian", "Guardián dorado", "Sentry",
        back = { t, c, r ->
            bloom(c, r * 1.85f, SUN, 0.5f)
            rotate(t * 30f, c) {
                for (i in 0 until 20) {
                    val a = TAU * i / 20
                    val l = r * (0.55f + 0.15f * wave(t, i, 2f))
                    val ray = Path().apply {
                        moveTo(polar(c, r * 1.05f, a - 0.05f).x, polar(c, r * 1.05f, a - 0.05f).y)
                        lineTo(polar(c, r * 1.05f + l, a).x, polar(c, r * 1.05f + l, a).y)
                        lineTo(polar(c, r * 1.05f, a + 0.05f).x, polar(c, r * 1.05f, a + 0.05f).y)
                        close()
                    }
                    drawPath(ray, Brush.radialGradient(listOf(SUN_LIGHT.copy(alpha = 0.8f), SUN.copy(alpha = 0f)), c, r * 1.05f + l), blendMode = BlendMode.Plus)
                }
            }
            // Alas de ángel doradas a los lados.
            val flap = (sin(t * TAU) + 1) / 2
            featherWing(Offset(c.x - r * 0.95f, c.y - r * 0.15f), -1f, r * 0.75f, c(0xFFFFF1C2), SUN, GOLD_DARK.copy(alpha = 0.5f), flap)
            featherWing(Offset(c.x + r * 0.95f, c.y - r * 0.15f), 1f, r * 0.75f, c(0xFFFFF1C2), SUN, GOLD_DARK.copy(alpha = 0.5f), flap)
        },
        front = { t, c, r ->
            bevelRing(c, r * 1.0f, r * 1.14f, c(0xFFE0A93A), SUN_LIGHT, GOLD_DARK, rotation = t * 0.15f)
            // Filigrana: puntos y rombos alrededor.
            for (i in 0 until 16) {
                val a = TAU * i / 16
                if (i % 4 == 0) continue
                drawCircle(SUN_LIGHT, 1.6f, polar(c, r * 1.07f, a))
            }
            // Remate: sol con estrella y halo.
            val top = Offset(c.x, c.y - r * 1.28f)
            bloom(top, r * 0.5f, SUN_LIGHT, 0.6f)
            for (k in 0 until 8) {
                spike(top, r * 0.1f, TAU * k / 8 + t * TAU * 0.2f, r * (if (k % 2 == 0) 0.2f else 0.12f), 0.25f,
                    Brush.radialGradient(listOf(SUN_LIGHT, SUN), top, r * 0.3f), GOLD_DARK.copy(alpha = 0.5f))
            }
            drawCircle(Brush.radialGradient(listOf(Color.White, SUN), top, r * 0.11f), r * 0.11f, top)
            // Base: gema dorada.
            gem(Offset(c.x, c.y + r * 1.22f), r * 0.1f, SUN, t)
            sparks(c, r * 1.1f, t, SUN_LIGHT, n = 10, speed = 0.5f, size = 1.6f)
        })

    private val theVoid = FrameStyle("sentry_void", "The Void", "Sentry",
        back = { t, c, r ->
            drawCircle(Brush.radialGradient(listOf(VOID, VOID.copy(alpha = 0.85f), Color.Transparent), c, r * 1.75f), r * 1.75f, c)
            for (i in 0 until 13) {
                val a = TAU * i / 13 + sin(t * TAU + i) * 0.1f
                val len = r * (0.5f + 0.18f * wave(t, i, 0.8f))
                val m = polar(c, r + len * 0.5f, a + 0.35f * sin(t * TAU + i * 0.7f))
                val e = polar(c, r + len, a - 0.3f * sin(t * TAU * 1.3f + i))
                val tent = Path().apply { moveTo(polar(c, r * 0.95f, a).x, polar(c, r * 0.95f, a).y); quadraticTo(m.x, m.y, e.x, e.y) }
                drawPath(tent, VOID_RIM.copy(alpha = 0.55f), style = Stroke(r * 0.12f, cap = StrokeCap.Round))
                drawPath(tent, VOID, style = Stroke(r * 0.085f, cap = StrokeCap.Round))
            }
            smoke(c, r * 1.1f, t, Color.Black.copy(alpha = 0.7f), n = 14, reach = r * 0.4f, speed = 0.3f)
        },
        front = { t, c, r ->
            bevelRing(c, r * 1.0f, r * 1.12f, c(0xFF1E0E33), VOID_RIM, Color.Black)
            // Remate: rostro del Void, dos ojos rojos que se abren.
            val open = (sin(t * TAU) * 1.8f - 0.2f).coerceIn(0f, 1f)
            val e = Offset(c.x, c.y - r * 1.3f)
            drawOval(Brush.radialGradient(listOf(c(0xFF2A1048), VOID), e, r * 0.35f), Offset(e.x - r * 0.38f, e.y - r * 0.2f), Size(r * 0.76f, r * 0.4f))
            for (side in listOf(-1f, 1f)) {
                val p = Offset(e.x + side * r * 0.16f, e.y)
                bloom(p, r * 0.22f, c(0xFFFF2244), 0.6f * open)
                drawOval(c(0xFFFF4D6A), Offset(p.x - r * 0.1f, p.y - r * 0.025f * open), Size(r * 0.2f, r * 0.05f * open + 0.5f))
            }
            // Base: gema oscura con núcleo violeta.
            gem(Offset(c.x, c.y + r * 1.22f), r * 0.1f, VOID_RIM, t)
        })

    val sentry = listOf(goldenGuardian, theVoid)

    // ======================= IRON MAN (Marvel) =======================
    private val ARC = c(0xFF7FE7FF)
    private val ARC_LIGHT = c(0xFFE8FCFF)
    private val HOT_ROD = c(0xFFA3111A)
    private val GOLD = c(0xFFE0A93A)

    private val markArmor = FrameStyle("ironman_mark", "Armadura Mark", "Iron Man",
        back = { t, c, r ->
            bloom(c, r * 1.6f, GOLD, 0.2f)
            // Propulsores laterales con llama azul.
            for (side in listOf(-1f, 1f)) {
                val p = Offset(c.x + side * r * 1.32f, c.y + r * 0.35f)
                val flick = 0.8f + 0.2f * wave(t, if (side > 0) 1 else 2, 4f)
                val jet = Path().apply {
                    moveTo(p.x - r * 0.08f, p.y); lineTo(p.x, p.y + r * 0.45f * flick); lineTo(p.x + r * 0.08f, p.y); close()
                }
                drawPath(jet, Brush.verticalGradient(listOf(ARC_LIGHT, ARC.copy(alpha = 0f)), p.y, p.y + r * 0.45f), blendMode = BlendMode.Plus)
            }
        },
        front = { t, c, r ->
            // Placas roja y doradas con remaches.
            val n = 6
            for (i in 0 until n) {
                val a0 = UP + TAU * i / n + 0.03f
                val a1 = a0 + TAU / n - 0.06f
                val red = i % 3 != 0
                metal(ringSegment(c, r * 1.0f, r * 1.2f, a0, a1), if (red) HOT_ROD else GOLD, if (red) c(0xFFFF6B6B) else c(0xFFFFF0B3),
                    if (red) c(0xFF4A0508) else c(0xFF6B4A10), frac(t * 0.6f + i / n.toFloat()), Rect(c, r * 1.2f))
                for (k in 0..1) drawCircle(c(0xFF2A2A2A), 1.4f, polar(c, r * 1.1f, a0 + (a1 - a0) * (0.2f + 0.6f * k)))
            }
            // Propulsores laterales (carcasa).
            for (side in listOf(-1f, 1f)) {
                val p = Offset(c.x + side * r * 1.32f, c.y + r * 0.3f)
                drawRoundRect(Brush.verticalGradient(listOf(c(0xFFC7CDD4), c(0xFF5A626B)), p.y - r * 0.15f, p.y + r * 0.1f),
                    Offset(p.x - r * 0.1f, p.y - r * 0.15f), Size(r * 0.2f, r * 0.25f), CornerRadius(4f))
                bloom(Offset(p.x, p.y + r * 0.08f), r * 0.12f, ARC, 0.8f)
            }
            // Remate: casco dorado con ojos que se encienden.
            val top = Offset(c.x, c.y - r * 1.3f)
            val helm = Path().apply {
                moveTo(top.x - r * 0.28f, top.y - r * 0.12f)
                quadraticTo(top.x, top.y - r * 0.38f, top.x + r * 0.28f, top.y - r * 0.12f)
                lineTo(top.x + r * 0.22f, top.y + r * 0.18f)
                lineTo(top.x, top.y + r * 0.26f)
                lineTo(top.x - r * 0.22f, top.y + r * 0.18f)
                close()
            }
            metal(helm, GOLD, c(0xFFFFF0B3), c(0xFF6B4A10), frac(t * 0.5f), Rect(top, r * 0.3f))
            val glowEyes = 0.6f + 0.4f * tri(t * 2)
            for (side in listOf(-1f, 1f)) {
                val eye = Offset(top.x + side * r * 0.1f, top.y - r * 0.02f)
                bloom(eye, r * 0.12f, ARC, glowEyes)
                drawRect(ARC_LIGHT, Offset(eye.x - r * 0.06f, eye.y - r * 0.015f), Size(r * 0.12f, r * 0.03f))
            }
            // Base: reactor arc.
            val b = Offset(c.x, c.y + r * 1.25f)
            drawCircle(Brush.radialGradient(listOf(c(0xFFC7CDD4), c(0xFF4A525B)), b, r * 0.16f), r * 0.16f, b)
            bloom(b, r * 0.3f, ARC, 0.6f + 0.3f * tri(t * 3))
            drawCircle(ARC_LIGHT, r * 0.08f, b)
            val tri3 = Path().apply { for (k in 0..3) { val p = polar(b, r * 0.12f, UP + TAU * k / 3); if (k == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) } }
            drawPath(tri3, ARC, style = Stroke(1.5f))
        })

    private val arcReactor = FrameStyle("ironman_reactor", "Reactor arc", "Iron Man",
        back = { t, c, r -> bloom(c, r * 1.7f, ARC, 0.45f + 0.2f * tri(t * 2)) },
        front = { t, c, r ->
            metal(ringSegment(c, r * 1.0f, r * 1.45f, 0f, TAU * 0.9999f), c(0xFF7C8794), c(0xFFE6EDF3), c(0xFF2C333B), frac(t * 0.5f), Rect(c, r * 1.45f))
            for (i in 0 until 10) {
                val a0 = TAU * i / 10 + 0.05f
                val coil = ringSegment(c, r * 1.1f, r * 1.34f, a0, a0 + TAU / 10 - 0.1f)
                drawPath(coil, Brush.radialGradient(listOf(c(0xFFE39A5B), c(0xFF8A4A22)), c, r * 1.34f))
                for (k in 1..4) {
                    val a = a0 + (TAU / 10 - 0.1f) * k / 5
                    drawLine(c(0xFF5A2E14), polar(c, r * 1.1f, a), polar(c, r * 1.34f, a), 1.1f)
                }
                drawLine(ARC.copy(alpha = 0.6f + 0.4f * wave(t, i, 3f)), polar(c, r * 1.07f, a0 - 0.05f), polar(c, r * 1.37f, a0 - 0.05f), 3f, blendMode = BlendMode.Plus)
            }
            neonRing(c, r * 1.04f, ARC, 2f, ARC_LIGHT)
            neonRing(c, r * 1.41f, ARC, 1.5f, ARC_LIGHT, 0.7f)
            // Remate: triángulo del Mark VI que pulsa.
            val top = Offset(c.x, c.y - r * 1.48f)
            val tri3 = Path().apply { for (k in 0..3) { val p = polar(top, r * 0.16f, PI_F / 2 + TAU * k / 3); if (k == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) } }
            neon(tri3, ARC, 2f, ARC_LIGHT, 0.7f + 0.3f * tri(t * 3))
        })

    val ironMan = listOf(markArmor, arcReactor)

    // ======================= SPIDER-MAN (Marvel) =======================
    private val SPIDEY_RED = c(0xFFD3202A)
    private val SPIDEY_BLUE = c(0xFF1F55C0)

    private val spideySuit = FrameStyle("spiderman_suit", "Trepamuros", "Spider-Man",
        back = { t, c, r ->
            // Telaraña con rocío detrás, anclada a las esquinas.
            val silk = c(0xCCEDEDED)
            for (i in 0 until 16) drawLine(silk, polar(c, r * 0.95f, TAU * i / 16), polar(c, r * 1.65f, TAU * i / 16), 0.9f)
            for (k in 0 until 4) {
                val rr = r * (1.18f + k * 0.12f)
                val p = Path()
                for (i in 0..16) {
                    val a = TAU * i / 16
                    val pt = polar(c, rr, a)
                    if (i == 0) p.moveTo(pt.x, pt.y) else { val mid = polar(c, rr * 0.95f, a - TAU / 32); p.quadraticTo(mid.x, mid.y, pt.x, pt.y) }
                }
                drawPath(p, silk, style = Stroke(0.9f))
            }
            for (i in 0 until 10) {
                val p = polar(c, r * (1.24f + (i % 3) * 0.12f), TAU * ((i * 0.618f) % 1f))
                val sp = wave(t, i, 1.5f)
                drawCircle(Color.White.copy(alpha = 0.5f + 0.5f * sp), 2f, p)
                if (sp > 0.85f) starGlint(p, 7f, Color.White)
            }
        },
        front = { t, c, r ->
            // Anillo del traje: rojo arriba con malla negra, azul abajo.
            val band = ringSegment(c, r * 1.0f, r * 1.16f, 0f, TAU * 0.9999f)
            drawPath(band, Brush.sweepGradient(listOf(SPIDEY_BLUE, SPIDEY_RED, SPIDEY_RED, SPIDEY_RED, SPIDEY_BLUE, SPIDEY_BLUE, SPIDEY_BLUE), c))
            for (i in 0 until 36) drawLine(Color.Black.copy(alpha = 0.5f), polar(c, r * 1.0f, TAU * i / 36), polar(c, r * 1.16f, TAU * i / 36), 0.9f)
            drawCircle(Color.Black.copy(alpha = 0.5f), r * 1.08f, c, style = Stroke(0.9f))
            drawCircle(Color.White.copy(alpha = 0.5f), r * 1.16f, c, style = Stroke(1f))
            // Remate: araña grande del pecho, sobre un rombo rojo.
            val top = Offset(c.x, c.y - r * 1.27f)
            val badge = Path().apply { moveTo(top.x, top.y - r * 0.28f); lineTo(top.x + r * 0.3f, top.y); lineTo(top.x, top.y + r * 0.28f); lineTo(top.x - r * 0.3f, top.y); close() }
            drawPath(badge, Brush.verticalGradient(listOf(c(0xFFFF4A52), SPIDEY_RED), top.y - r * 0.28f, top.y + r * 0.28f))
            drawPath(badge, Color.Black, style = Stroke(1.5f))
            spider(top, r * 0.2f, Color.Black, t)
            // Base: araña que baja colgada y se balancea.
            val sw = sin(t * TAU) * 0.3f
            val anchor = Offset(c.x, c.y + r * 1.16f)
            val end = Offset(anchor.x + sin(sw) * r * 0.35f, anchor.y + r * 0.38f)
            drawLine(c(0xFFE6E6E6), anchor, end, 1.1f)
            spider(end, r * 0.13f, Color.Black, t * 3)
        })

    private val spiderVerse = FrameStyle("spiderman_verse", "Spider-Verse", "Spider-Man",
        back = { t, c, r ->
            // Fondo de cómic: trama de puntos y rayos de impacto.
            for (ring in 0 until 5) {
                val rr = r * (1.12f + ring * 0.1f)
                val n = 28 + ring * 6
                for (i in 0 until n) drawCircle(c(0xFFFF4FA3).copy(alpha = 0.6f - ring * 0.08f), 2.6f - ring * 0.35f, polar(c, rr, TAU * i / n + ring * 0.1f))
            }
            rotate(t * 20f, c) {
                for (i in 0 until 12) {
                    val a = TAU * i / 12
                    drawLine(c(0xFF00E5FF).copy(alpha = 0.25f), polar(c, r * 1.2f, a), polar(c, r * 1.65f, a), 2f)
                }
            }
        },
        front = { t, c, r ->
            val glitch = if ((t * 20).toInt() % 6 == 0) r * 0.07f else r * 0.015f
            drawCircle(c(0xFF00E5FF), r + 4, Offset(c.x - glitch, c.y), style = Stroke(5f), blendMode = BlendMode.Plus)
            drawCircle(c(0xFFFF2E63), r + 4, Offset(c.x + glitch, c.y), style = Stroke(5f), blendMode = BlendMode.Plus)
            drawCircle(Color.Black, r + 4, c, style = Stroke(2f))
            if ((t * 20).toInt() % 6 == 0) for (k in 0 until 3) {
                val y = c.y + r * (((k * 41) % 20) / 10f - 1f)
                drawRect(c(0xFF00E5FF).copy(alpha = 0.6f), Offset(c.x - r * 1.5f, y), Size(r * 3f, 3f), blendMode = BlendMode.Plus)
            }
            // Remate: bocadillo "THWIP!" estilo cómic.
            val b = Offset(c.x + r * 0.8f, c.y - r * 1.15f)
            val pop = tri(t * 2)
            scale(0.75f + 0.25f * pop, b) {
                val star = Path()
                for (k in 0..18) {
                    val rr = if (k % 2 == 0) r * 0.38f else r * 0.24f
                    val p = polar(b, rr, TAU * k / 18)
                    if (k == 0) star.moveTo(p.x, p.y) else star.lineTo(p.x, p.y)
                }
                drawPath(star, c(0xFFFFE600))
                drawPath(star, Color.Black, style = Stroke(2f))
                spider(b, r * 0.13f, Color.Black, t)
            }
            // Base: máscara con ojos blancos.
            val m = Offset(c.x, c.y + r * 1.25f)
            drawOval(SPIDEY_RED, Offset(m.x - r * 0.28f, m.y - r * 0.18f), Size(r * 0.56f, r * 0.36f))
            for (side in listOf(-1f, 1f)) {
                val eye = Path().apply {
                    moveTo(m.x + side * r * 0.04f, m.y - r * 0.02f)
                    quadraticTo(m.x + side * r * 0.2f, m.y - r * 0.16f, m.x + side * r * 0.22f, m.y + r * 0.02f)
                    quadraticTo(m.x + side * r * 0.12f, m.y + r * 0.06f, m.x + side * r * 0.04f, m.y - r * 0.02f)
                    close()
                }
                drawPath(eye, Color.White)
                drawPath(eye, Color.Black, style = Stroke(2f))
            }
        })

    val spiderMan = listOf(spideySuit, spiderVerse)

    // ======================= ASTA (Black Clover) =======================
    private val ANTI = c(0xFF0D0D0D)
    private val ANTI_EDGE = c(0xFFD6D6D6)
    private val CLOVER = c(0xFF2FAF4F)

    private val antiMagic = FrameStyle("asta_antimagic", "Antimagia", "Asta",
        back = { t, c, r ->
            bloom(c, r * 1.6f, Color.White, 0.06f)
            fire(c, r * 1.05f, t, ANTI_EDGE.copy(alpha = 0.85f), c(0xFF6A6A6A), c(0xFF2A2A2A), count = 32, len = r * 0.55f, speed = 2.4f, additive = false, width = 0.09f)
            fire(c, r * 1.05f, t + 0.07f, ANTI, ANTI, c(0xFF1A1A1A), count = 32, len = r * 0.47f, speed = 2.4f, additive = false, width = 0.08f)
            // Espada mata-demonios gigante detrás, en diagonal.
            sword(Offset(c.x + r * 0.95f, c.y + r * 0.1f), r * 1.1f, 30f, c(0xFF2A2A2A))
            sword(Offset(c.x + r * 0.95f, c.y + r * 0.1f), r * 1.05f, 30f, c(0xFF3C3C3C))
            for (i in 0 until 10) {
                val ph = frac(t * 0.9f + i * 0.1f)
                rect(polar(c, r * (1.25f + ph * 0.4f), TAU * ((i * 0.37f) % 1f)), 5f * (1 - ph), 5f * (1 - ph), ANTI)
            }
        },
        front = { t, c, r ->
            bevelRing(c, r * 1.0f, r * 1.12f, c(0xFF1C1C1C), c(0xFF8A8A8A), Color.Black)
            drawCircle(ANTI_EDGE, r * 1.15f, c, style = Stroke(1f))
            // Remate: grimorio negro con trébol de cinco hojas.
            val bob = sin(t * TAU) * r * 0.04f
            val at = Offset(c.x, c.y - r * 1.3f + bob)
            rotate(sin(t * TAU) * 4f, at) {
                drawRoundRect(Brush.verticalGradient(listOf(c(0xFF2A2A2A), c(0xFF0A0A0A)), at.y - r * 0.22f, at.y + r * 0.22f),
                    Offset(at.x - r * 0.3f, at.y - r * 0.22f), Size(r * 0.6f, r * 0.44f), CornerRadius(4f))
                drawRoundRect(c(0xFF8A7A50), Offset(at.x - r * 0.3f, at.y - r * 0.22f), Size(r * 0.6f, r * 0.44f), CornerRadius(4f), style = Stroke(1.5f))
                clover(at, r * 0.15f, 5, c(0xFF3A3A3A))
                clover(at, r * 0.12f, 5, Color.Black)
            }
            bloom(at, r * 0.4f, CLOVER, 0.15f + 0.1f * tri(t * 2))
            // Base: tela negra rasgada.
            val b = Offset(c.x, c.y + r * 1.15f)
            val cloth = Path().apply {
                moveTo(b.x - r * 0.4f, b.y)
                lineTo(b.x + r * 0.4f, b.y)
                lineTo(b.x + r * 0.3f, b.y + r * 0.25f + sin(t * TAU) * 3f)
                lineTo(b.x + r * 0.1f, b.y + r * 0.15f)
                lineTo(b.x - r * 0.05f, b.y + r * 0.32f + sin(t * TAU + 1) * 3f)
                lineTo(b.x - r * 0.2f, b.y + r * 0.16f)
                lineTo(b.x - r * 0.35f, b.y + r * 0.26f)
                close()
            }
            drawPath(cloth, ANTI)
            drawPath(cloth, ANTI_EDGE, style = Stroke(1f))
        })

    private val blackAsta = FrameStyle("asta_black", "Black Asta", "Asta",
        back = { t, c, r ->
            // Aura negra en espiral.
            rotate(t * 120f, c) {
                for (k in 0 until 4) drawArc(
                    Brush.sweepGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f), Color.Transparent), c),
                    k * 90f, 70f, false, Offset(c.x - r * 1.3f, c.y - r * 1.3f), Size(r * 2.6f, r * 2.6f), style = Stroke(r * 0.3f),
                )
            }
            val flap = sin(t * TAU) * r * 0.08f
            val wing = Path().apply {
                moveTo(c.x - r * 0.6f, c.y - r * 0.6f)
                cubicTo(c.x - r * 0.95f, c.y - r * 1.9f - flap, c.x - r * 1.85f, c.y - r * 1.5f - flap, c.x - r * 1.7f, c.y - r * 0.1f)
                quadraticTo(c.x - r * 1.45f, c.y - r * 0.4f, c.x - r * 1.4f, c.y + r * 0.15f)
                quadraticTo(c.x - r * 1.15f, c.y - r * 0.15f, c.x - r * 1.05f, c.y + r * 0.4f)
                quadraticTo(c.x - r * 0.9f, c.y, c.x - r * 0.75f, c.y + r * 0.25f)
                close()
            }
            drawPath(wing, Brush.radialGradient(listOf(c(0xFF2E2E2E), ANTI), Offset(c.x - r, c.y - r * 0.6f), r * 1.4f))
            for (k in 0 until 3) {
                val bone = Path().apply {
                    moveTo(c.x - r * 0.65f, c.y - r * 0.55f)
                    quadraticTo(c.x - r * (1.05f + k * 0.15f), c.y - r * (1.35f - k * 0.25f) - flap, c.x - r * (1.7f - k * 0.3f), c.y - r * (0.1f - k * 0.17f))
                }
                drawPath(bone, c(0xFF5A5A5A), style = Stroke(1.5f))
            }
            drawPath(wing, ANTI_EDGE, style = Stroke(1.5f))
        },
        front = { t, c, r ->
            bevelRing(c, r * 1.0f, r * 1.12f, c(0xFF1C1C1C), c(0xFF7A7A7A), Color.Black)
            // Cuerno a la derecha.
            val horn = Path().apply {
                moveTo(c.x + r * 0.3f, c.y - r * 1.02f)
                quadraticTo(c.x + r * 0.55f, c.y - r * 1.6f, c.x + r * 1.0f, c.y - r * 1.65f)
                quadraticTo(c.x + r * 0.7f, c.y - r * 1.3f, c.x + r * 0.62f, c.y - r * 0.9f)
                close()
            }
            drawPath(horn, Brush.linearGradient(listOf(c(0xFF3A3A3A), ANTI), Offset(c.x + r * 0.3f, c.y - r), Offset(c.x + r, c.y - r * 1.6f)))
            drawPath(horn, ANTI_EDGE, style = Stroke(1.2f))
            // Ojo rojo que destella.
            val e = Offset(c.x + r * 0.45f, c.y - r * 0.92f)
            bloom(e, r * 0.2f, c(0xFFFF3030), 0.4f + 0.5f * tri(t * 2))
            // Base: trébol de cinco hojas negro con borde.
            val b = Offset(c.x, c.y + r * 1.24f)
            drawCircle(ANTI, r * 0.2f, b)
            drawCircle(ANTI_EDGE, r * 0.2f, b, style = Stroke(1.2f))
            clover(b, r * 0.15f, 5, c(0xFF2E2E2E), rotation = sin(t * TAU) * 0.15f)
        })

    val asta = listOf(antiMagic, blackAsta)

    // ======================= CHROLLO (HxH) =======================
    private val TROUPE = c(0xFF0B0B10)
    private val BONE = c(0xFFEDE8DC)
    private val NEN = c(0xFF9B5CFF)

    private val dancho = FrameStyle("chrollo_dancho", "Danchō", "Chrollo",
        back = { t, c, r ->
            bloom(c, r * 1.6f, NEN, 0.15f)
            // Patas de araña que salen por detrás y se mueven.
            for (side in listOf(-1f, 1f)) for (k in 0 until 4) {
                val a = (if (side < 0) PI_F else 0f) + side * (-0.7f + k * 0.45f)
                val bend = sin(t * TAU + k + side) * 0.08f
                val root = polar(c, r * 0.95f, a)
                val knee = polar(c, r * 1.45f, a - side * 0.25f + bend)
                val foot = polar(c, r * 1.62f, a + side * 0.18f + bend)
                drawLine(c(0xFF22222A), root, knee, r * 0.06f, StrokeCap.Round)
                drawLine(c(0xFF22222A), knee, foot, r * 0.045f, StrokeCap.Round)
                drawLine(c(0xFF6E6E7C), root, knee, r * 0.015f, StrokeCap.Round)
            }
            // Cuello de piel negra.
            for (i in 0 until 50) {
                val a = TAU * i / 50
                val h = r * (0.16f + 0.08f * wave(t, i, 0.6f))
                val tip = polar(c, r * 1.05f + h, a + 0.04f * sin(t * TAU + i))
                drawLine(c(0xFF1E1E26), polar(c, r * 0.98f, a), tip, r * 0.09f, StrokeCap.Round)
                drawLine(c(0xFF5E5E6C), polar(c, r * 1.08f, a), tip, r * 0.022f, StrokeCap.Round)
            }
        },
        front = { t, c, r ->
            bevelRing(c, r * 1.0f, r * 1.08f, c(0xFF1A1A22), BONE, TROUPE)
            // Remate: cruz invertida colgando de una cadena.
            val top = Offset(c.x, c.y - r * 1.28f + sin(t * TAU) * 2f)
            drawLine(BONE.copy(alpha = 0.7f), Offset(c.x, c.y - r * 1.08f), Offset(top.x, top.y + r * 0.2f), 1.2f)
            bloom(top, r * 0.3f, BONE, 0.25f)
            cross(top, r * 0.2f, BONE, inverted = true, width = r * 0.055f)
            // Base: tatuaje de la Brigada (araña con el 0).
            val b = Offset(c.x, c.y + r * 1.24f)
            drawCircle(BONE, r * 0.22f, b)
            drawCircle(TROUPE, r * 0.22f, b, style = Stroke(1.5f))
            spider(b, r * 0.2f, TROUPE, t)
            drawCircle(BONE, r * 0.05f, Offset(b.x, b.y + r * 0.04f))
            drawCircle(TROUPE, r * 0.032f, Offset(b.x, b.y + r * 0.04f), style = Stroke(1.4f))
        })

    private val skillHunter = FrameStyle("chrollo_skillhunter", "Skill Hunter", "Chrollo",
        back = { t, c, r ->
            bloom(c, r * 1.7f, NEN, 0.35f)
            for (i in 0 until 7) {
                val a = TAU * (i / 7f + t * 0.2f)
                val p = polar(c, r * 1.4f, a)
                rotate(deg(a) + 90 + sin(t * TAU * 2 + i) * 12, p) {
                    drawRect(Brush.verticalGradient(listOf(BONE, c(0xFFC9C2B0)), p.y - r * 0.14f, p.y + r * 0.14f), Offset(p.x - r * 0.11f, p.y - r * 0.14f), Size(r * 0.22f, r * 0.28f))
                    for (l in 0 until 3) drawLine(c(0xFF8A8270), Offset(p.x - r * 0.07f, p.y - r * 0.07f + l * r * 0.05f), Offset(p.x + r * 0.07f, p.y - r * 0.07f + l * r * 0.05f), 0.8f)
                    drawCircle(NEN, r * 0.035f, Offset(p.x, p.y + r * 0.08f), style = Stroke(1.2f))
                }
            }
            sparks(c, r * 1.1f, t, NEN, n = 12, speed = 0.6f, swirl = true, size = 1.6f)
        },
        front = { t, c, r ->
            bevelRing(c, r * 1.0f, r * 1.1f, c(0xFF2A2233), c(0xFFB9A4E6), TROUPE)
            // Remate: libro abierto que pasa páginas.
            val at = Offset(c.x, c.y - r * 1.28f)
            val flip = tri(t * 2)
            val left = Path().apply { moveTo(at.x, at.y); lineTo(at.x - r * 0.45f, at.y - r * 0.12f); lineTo(at.x - r * 0.45f, at.y + r * 0.22f); lineTo(at.x, at.y + r * 0.32f); close() }
            val right = Path().apply { moveTo(at.x, at.y); lineTo(at.x + r * 0.45f, at.y - r * 0.12f); lineTo(at.x + r * 0.45f, at.y + r * 0.22f); lineTo(at.x, at.y + r * 0.32f); close() }
            drawPath(left, c(0xFF3A2C4A)); drawPath(right, c(0xFF3A2C4A))
            drawPath(left, NEN, style = Stroke(1f)); drawPath(right, NEN, style = Stroke(1f))
            val px = at.x + r * 0.43f * (1 - 2 * flip)
            val page = Path().apply {
                moveTo(at.x, at.y); lineTo(px, at.y - r * 0.1f - r * 0.15f * (1 - abs(1 - 2 * flip)))
                lineTo(px, at.y + r * 0.2f); lineTo(at.x, at.y + r * 0.3f); close()
            }
            drawPath(page, BONE)
            bloom(Offset(at.x, at.y - r * 0.1f), r * 0.35f, NEN, 0.3f + 0.3f * flip)
            // Base: cruz invertida pequeña.
            cross(Offset(c.x, c.y + r * 1.22f), r * 0.14f, BONE, inverted = true, width = r * 0.04f)
        })

    val chrollo = listOf(dancho, skillHunter)

    val all: List<List<FrameStyle>> = listOf(alucard, amaterasu, susanoo, killua, batman, sentry, ironMan, spiderMan, asta, chrollo)
}
