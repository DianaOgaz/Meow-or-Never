package com.example.cepillagato.ui

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.cepillagato.game.CatEvent
import com.example.cepillagato.game.CatZone
import com.example.cepillagato.game.GameState
import com.example.cepillagato.game.Phase
import com.example.cepillagato.ui.theme.Cafe
import com.example.cepillagato.ui.theme.Crema
import com.example.cepillagato.ui.theme.Naranja
import com.example.cepillagato.ui.theme.NaranjaOscuro
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

/** Geometría del gato (acostado de lado), compartida entre el dibujo y la detección de zonas. */
class CatGeometry(w: Float, h: Float) {
    val u = min(w / 10f, h / 7.5f)
    val cx = w / 2f
    val cy = h * 0.55f

    val bodyCenter = Offset(cx + 0.5f * u, cy + 0.5f * u)
    val bodyRx = 3.0f * u
    val bodyRy = 1.7f * u

    val bellyCenter = Offset(cx + 0.5f * u, cy + 1.45f * u)
    val bellyRx = 2.2f * u
    val bellyRy = 0.75f * u

    val headCenter = Offset(cx - 2.6f * u, cy - 0.9f * u)
    val headR = 1.6f * u

    val tailStart = Offset(cx + 3.2f * u, cy + 0.9f * u)
    val groundY = cy + 2.5f * u

    fun zoneAt(p: Offset): CatZone? {
        if (hypot(p.x - headCenter.x, p.y - headCenter.y) <= headR * 1.1f) return CatZone.HEAD
        if (inEllipse(p, bellyCenter, bellyRx, bellyRy)) return CatZone.BELLY
        if (inEllipse(p, bodyCenter, bodyRx, bodyRy)) return CatZone.BACK
        if (p.x in (cx + 3.0f * u)..(cx + 5.2f * u) && p.y in (cy - 2.6f * u)..(cy + 1.3f * u)) return CatZone.TAIL
        return null
    }

    private fun inEllipse(p: Offset, c: Offset, rx: Float, ry: Float): Boolean {
        val dx = (p.x - c.x) / rx
        val dy = (p.y - c.y) / ry
        return dx * dx + dy * dy <= 1f
    }
}

enum class Mood { HAPPY, CALM, ANNOYED, ANGRY, BITING }

fun moodOf(s: GameState): Mood = when {
    s.phase == Phase.BITTEN -> Mood.BITING
    s.event == CatEvent.GRUNE -> Mood.ANGRY
    s.isPurring -> Mood.HAPPY
    s.annoyance >= 80f -> Mood.ANGRY
    s.annoyance >= 50f -> Mood.ANNOYED
    s.isBrushing && s.annoyance < 30f -> Mood.HAPPY
    else -> Mood.CALM
}

private fun ovalAt(c: Offset, rx: Float, ry: Float) = Pair(Offset(c.x - rx, c.y - ry), Size(rx * 2, ry * 2))

fun DrawScope.drawCat(g: CatGeometry, mood: Mood, annoyance: Float, tailSway: Float, blink: Boolean) {
    val u = g.u
    val angry = mood == Mood.ANGRY || mood == Mood.BITING
    val fur = lerp(Naranja, Color(0xFFE2553A), (annoyance / 100f) * 0.6f)
    val furDark = lerp(NaranjaOscuro, Color(0xFFA8321E), (annoyance / 100f) * 0.6f)

    // --- Cola ---
    val tailEnd = Offset(g.cx + 4.5f * u + tailSway * 0.6f * u, g.cy - 2.0f * u)
    val tail = Path().apply {
        moveTo(g.tailStart.x, g.tailStart.y)
        quadraticTo(g.cx + 5.0f * u, g.cy + 0.8f * u, tailEnd.x, tailEnd.y)
    }
    drawPath(tail, fur, style = Stroke(width = if (angry) 0.9f * u else 0.55f * u, cap = StrokeCap.Round))
    drawCircle(furDark, radius = if (angry) 0.45f * u else 0.3f * u, center = tailEnd)

    // --- Patas ---
    for (x in listOf(-1.6f, -0.8f, 1.6f, 2.4f)) {
        drawRoundRect(
            furDark, topLeft = Offset(g.cx + x * u, g.cy + 1.5f * u), size = Size(0.6f * u, 1.0f * u),
            cornerRadius = CornerRadius(0.3f * u)
        )
    }

    // --- Cuerpo ---
    ovalAt(g.bodyCenter, g.bodyRx, g.bodyRy).let { (tl, sz) -> drawOval(fur, tl, sz) }
    // Rayas del lomo
    for (i in 0..2) {
        val x = g.cx + (-0.6f + i * 1.2f) * u
        drawLine(furDark, Offset(x, g.cy - 1.15f * u), Offset(x + 0.3f * u, g.cy - 0.2f * u), 0.25f * u, StrokeCap.Round)
    }
    // Panza
    ovalAt(g.bellyCenter, g.bellyRx, g.bellyRy).let { (tl, sz) -> drawOval(Crema, tl, sz) }

    // --- Cabeza ---
    val hc = g.headCenter
    val r = g.headR
    val earH = if (angry) 0.6f else 1.1f
    for (side in listOf(-1f, 1f)) {
        val ear = Path().apply {
            moveTo(hc.x + side * 0.4f * r, hc.y - 0.85f * r)
            lineTo(hc.x + side * (if (angry) 1.15f else 0.85f) * r, hc.y - (0.6f + earH) * r)
            lineTo(hc.x + side * 1.0f * r, hc.y - 0.3f * r)
            close()
        }
        drawPath(ear, fur)
        val inner = Path().apply {
            moveTo(hc.x + side * 0.55f * r, hc.y - 0.8f * r)
            lineTo(hc.x + side * (if (angry) 1.05f else 0.83f) * r, hc.y - (0.45f + earH) * r)
            lineTo(hc.x + side * 0.9f * r, hc.y - 0.45f * r)
            close()
        }
        drawPath(inner, Color(0xFFF7B5B5))
    }
    drawCircle(fur, r, hc)
    // Hocico
    ovalAt(Offset(hc.x, hc.y + 0.45f * r), 0.55f * r, 0.35f * r).let { (tl, sz) -> drawOval(Crema, tl, sz) }

    // Ojos
    val eyeL = Offset(hc.x - 0.42f * r, hc.y - 0.1f * r)
    val eyeR = Offset(hc.x + 0.42f * r, hc.y - 0.1f * r)
    val eyeStroke = Stroke(width = 0.07f * r, cap = StrokeCap.Round)
    for (e in listOf(eyeL, eyeR)) {
        when (mood) {
            Mood.HAPPY -> drawArc(
                Cafe, 180f, 180f, false,
                topLeft = Offset(e.x - 0.18f * r, e.y - 0.08f * r), size = Size(0.36f * r, 0.3f * r), style = eyeStroke
            )
            Mood.CALM -> if (blink) {
                drawLine(Cafe, Offset(e.x - 0.15f * r, e.y), Offset(e.x + 0.15f * r, e.y), 0.07f * r, StrokeCap.Round)
            } else {
                drawCircle(Cafe, 0.17f * r, e)
                drawCircle(Color.White, 0.06f * r, Offset(e.x + 0.06f * r, e.y - 0.06f * r))
            }
            Mood.ANNOYED -> {
                ovalAt(e, 0.18f * r, 0.09f * r).let { (tl, sz) -> drawOval(Cafe, tl, sz) }
                drawLine(Cafe, Offset(e.x - 0.22f * r, e.y - 0.12f * r), Offset(e.x + 0.22f * r, e.y - 0.12f * r), 0.06f * r)
            }
            Mood.ANGRY, Mood.BITING -> {
                drawCircle(Color(0xFFFFE066), 0.16f * r, e)
                ovalAt(e, 0.04f * r, 0.13f * r).let { (tl, sz) -> drawOval(Cafe, tl, sz) }
            }
        }
    }
    if (angry) { // cejas enojadas
        drawLine(Cafe, Offset(eyeL.x - 0.25f * r, eyeL.y - 0.32f * r), Offset(eyeL.x + 0.2f * r, eyeL.y - 0.15f * r), 0.08f * r, StrokeCap.Round)
        drawLine(Cafe, Offset(eyeR.x + 0.25f * r, eyeR.y - 0.32f * r), Offset(eyeR.x - 0.2f * r, eyeR.y - 0.15f * r), 0.08f * r, StrokeCap.Round)
    }
    if (mood == Mood.HAPPY) { // cachetes
        for (e in listOf(eyeL, eyeR)) {
            ovalAt(Offset(e.x, e.y + 0.32f * r), 0.16f * r, 0.08f * r).let { (tl, sz) -> drawOval(Color(0x66FF7A8A), tl, sz) }
        }
    }

    // Nariz
    val nose = Path().apply {
        moveTo(hc.x - 0.1f * r, hc.y + 0.25f * r)
        lineTo(hc.x + 0.1f * r, hc.y + 0.25f * r)
        lineTo(hc.x, hc.y + 0.37f * r)
        close()
    }
    drawPath(nose, Color(0xFFE86A7A))

    // Boca
    when (mood) {
        Mood.ANGRY, Mood.BITING -> {
            val big = mood == Mood.BITING
            val mc = Offset(hc.x, hc.y + (if (big) 0.62f else 0.55f) * r)
            val mw = (if (big) 0.38f else 0.22f) * r
            val mh = (if (big) 0.28f else 0.13f) * r
            ovalAt(mc, mw, mh).let { (tl, sz) -> drawOval(Color(0xFF5A1A1A), tl, sz) }
            for (side in listOf(-1f, 1f)) { // colmillos
                val fang = Path().apply {
                    moveTo(mc.x + side * 0.55f * mw, mc.y - mh * 0.9f)
                    lineTo(mc.x + side * 0.25f * mw, mc.y - mh * 0.9f)
                    lineTo(mc.x + side * 0.4f * mw, mc.y - mh * 0.9f + (if (big) 0.22f else 0.12f) * r)
                    close()
                }
                drawPath(fang, Color.White)
            }
        }
        else -> {
            val my = hc.y + 0.37f * r
            drawArc(Cafe, 0f, 180f, false, Offset(hc.x - 0.24f * r, my - 0.1f * r), Size(0.24f * r, 0.2f * r), style = eyeStroke)
            drawArc(Cafe, 0f, 180f, false, Offset(hc.x, my - 0.1f * r), Size(0.24f * r, 0.2f * r), style = eyeStroke)
        }
    }

    // Bigotes
    for (side in listOf(-1f, 1f)) {
        for (k in -1..1) {
            drawLine(
                Cafe.copy(alpha = 0.7f),
                Offset(hc.x + side * 0.45f * r, hc.y + 0.4f * r + k * 0.08f * r),
                Offset(hc.x + side * 1.25f * r, hc.y + 0.3f * r + k * 0.2f * r),
                0.03f * r
            )
        }
    }
}

fun DrawScope.drawPuddle(g: CatGeometry, alpha: Float) {
    val u = g.u
    ovalAt(Offset(g.cx + 1.8f * u, g.groundY + 0.15f * u), 1.8f * u, 0.4f * u).let { (tl, sz) ->
        drawOval(Color(0xFFF4D03F).copy(alpha = 0.75f * alpha), tl, sz)
    }
    ovalAt(Offset(g.cx + 1.3f * u, g.groundY + 0.08f * u), 0.5f * u, 0.12f * u).let { (tl, sz) ->
        drawOval(Color.White.copy(alpha = 0.5f * alpha), tl, sz)
    }
}

fun DrawScope.drawPurr(g: CatGeometry, phase: Float, tm: TextMeasurer) {
    val hc = g.headCenter
    val u = g.u
    for (i in 0..2) {
        val p = (phase + i / 3f) % 1f
        val pos = Offset(hc.x - 0.8f * u + i * 1.2f * u, hc.y - 2.6f * u - p * 1.5f * u)
        drawText(
            tm, "♥", topLeft = pos,
            style = TextStyle(color = Color(0xFFE63970).copy(alpha = 1f - p), fontSize = 28.sp)
        )
    }
    drawText(
        tm, "prrrr", topLeft = Offset(hc.x + 1.4f * u, hc.y - 1.8f * u + sin(phase * 6.28f) * 4f),
        style = TextStyle(color = Cafe.copy(alpha = 0.8f), fontSize = 18.sp, fontWeight = FontWeight.Bold)
    )
}

fun DrawScope.drawUfo(center: Offset, u: Float, lights: Float) {
    // Cúpula
    drawArc(
        Color(0xFF9AD1F5), 180f, 180f, true,
        topLeft = Offset(center.x - 1.0f * u, center.y - 1.0f * u), size = Size(2.0f * u, 2.0f * u)
    )
    // Platillo
    ovalAt(center, 2.4f * u, 0.55f * u).let { (tl, sz) -> drawOval(Color(0xFF8D99AE), tl, sz) }
    ovalAt(Offset(center.x, center.y - 0.12f * u), 2.0f * u, 0.3f * u).let { (tl, sz) -> drawOval(Color(0xFFB8C1CC), tl, sz) }
    val colors = listOf(Color(0xFFFF595E), Color(0xFFFFCA3A), Color(0xFF8AC926), Color(0xFF1982C4))
    for (i in 0..4) {
        val c = colors[(i + (lights * 4).toInt()) % colors.size]
        drawCircle(c, 0.16f * u, Offset(center.x + (i - 2) * 0.9f * u, center.y + 0.15f * u))
    }
}

fun DrawScope.drawBeam(from: Offset, toY: Float, u: Float, alpha: Float) {
    val beam = Path().apply {
        moveTo(from.x - 0.8f * u, from.y)
        lineTo(from.x + 0.8f * u, from.y)
        lineTo(from.x + 3.8f * u, toY)
        lineTo(from.x - 3.8f * u, toY)
        close()
    }
    drawPath(beam, Color(0xFFB9F57A).copy(alpha = 0.35f * alpha))
}

fun DrawScope.drawBrush(pos: Offset, u: Float) {
    rotate(-25f, pivot = pos) {
        drawRoundRect(
            Color(0xFF8D5A3B), topLeft = Offset(pos.x + 0.3f * u, pos.y - 0.2f * u),
            size = Size(2.2f * u, 0.4f * u), cornerRadius = CornerRadius(0.2f * u)
        )
        drawRoundRect(
            Color(0xFFC08552), topLeft = Offset(pos.x - 0.7f * u, pos.y - 0.4f * u),
            size = Size(1.1f * u, 0.55f * u), cornerRadius = CornerRadius(0.12f * u)
        )
        for (i in 0..6) {
            val x = pos.x - 0.6f * u + i * 0.15f * u
            drawLine(Color(0xFF444444), Offset(x, pos.y + 0.15f * u), Offset(x, pos.y + 0.45f * u), 0.05f * u)
        }
    }
}
