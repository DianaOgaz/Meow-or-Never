package com.example.cepillagato.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cepillagato.game.CatEvent
import com.example.cepillagato.game.CatZone
import com.example.cepillagato.game.GameState
import com.example.cepillagato.game.GameViewModel
import com.example.cepillagato.game.Phase
import com.example.cepillagato.ui.theme.Cafe
import com.example.cepillagato.ui.theme.Fondo
import com.example.cepillagato.ui.theme.Naranja
import com.example.cepillagato.ui.theme.Peligro
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun GameScreen(vm: GameViewModel = viewModel()) {
    val s = vm.state
    val haptic = LocalHapticFeedback.current

    // Vibra cuando ocurre un suceso o una mordida.
    LaunchedEffect(s.eventCounter) {
        if (s.eventCounter > 0) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Fondo)
            .safeDrawingPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Header(s)
        Spacer(Modifier.height(8.dp))
        AnnoyanceBar(s.annoyance)

        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(if (s.phase == Phase.ABDUCTING || s.phase == Phase.ABDUCTED) Color(0xFF1B1F3B) else Color(0xFFFFEBD2))
        ) {
            CatPlayground(s, onBrush = vm::onBrush, onBrushEnd = vm::onBrushEnd)

            EventBanner(s, Modifier.align(Alignment.TopCenter).padding(12.dp))

            if (s.phase == Phase.BITTEN) {
                EndCard(
                    title = "¡ÑAM! Te mordió 😾",
                    body = "Aguantaste ${fmt(s.score)} s.\nVuelves a empezar desde 0.",
                    record = s.newRecord,
                    button = "Intentar de nuevo",
                    color = Peligro,
                    onClick = vm::restart,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            if (s.phase == Phase.ABDUCTED) {
                EndCard(
                    title = "Abducido por un OVNI 🛸",
                    body = "¡Suceso ÚNICO! El gato se fue a otra galaxia.\nPuntaje final: ${fmt(s.score)} s.",
                    record = s.newRecord,
                    button = "Adoptar otro gato",
                    color = Color(0xFFE09F3E),
                    onClick = vm::restart,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        HintText(s)
        Spacer(Modifier.height(8.dp))
        Bitacora(s)
    }
}

@Composable
private fun Header(s: GameState) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text("Tiempo cepillando", fontSize = 13.sp, color = Cafe.copy(alpha = 0.7f))
            Row(verticalAlignment = Alignment.Bottom) {
                Text("${fmt(s.score)} s", fontSize = 36.sp, fontWeight = FontWeight.Black, color = Cafe)
                if (s.isPurring) {
                    Text("  x2", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7B2CBF))
                }
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("Récord", fontSize = 13.sp, color = Cafe.copy(alpha = 0.7f))
            Text("🏆 ${fmt(s.best)} s", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Cafe)
        }
    }
}

@Composable
private fun AnnoyanceBar(annoyance: Float) {
    val anim by animateFloatAsState(annoyance / 100f, label = "fastidio")
    val color by animateColorAsState(
        when {
            annoyance < 40f -> Color(0xFF52B788)
            annoyance < 75f -> Color(0xFFF4A261)
            else -> Peligro
        }, label = "colorFastidio"
    )
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Fastidio del gato", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Cafe)
            Text("${annoyance.toInt()}%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
        }
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0x22000000))
        ) {
            Box(
                Modifier
                    .fillMaxWidth(anim.coerceIn(0f, 1f))
                    .height(16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color)
            )
        }
    }
}

private data class FurParticle(val pos: Offset, val vel: Offset, val life: Float)

@Composable
private fun CatPlayground(
    s: GameState,
    onBrush: (CatZone?, Float) -> Unit,
    onBrushEnd: () -> Unit
) {
    val brush by rememberUpdatedState(onBrush)
    val brushEnd by rememberUpdatedState(onBrushEnd)
    var brushPos by remember { mutableStateOf<Offset?>(null) }
    val particles = remember { mutableStateListOf<FurParticle>() }
    val textMeasurer = rememberTextMeasurer()

    val transition = rememberInfiniteTransition(label = "gato")
    val tailSway by transition.animateFloat(
        -1f, 1f, infiniteRepeatable(tween(1300, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "cola"
    )
    val loop by transition.animateFloat(
        0f, 1f, infiniteRepeatable(tween(1200, easing = LinearEasing)), label = "ciclo"
    )
    val blinkCycle by transition.animateFloat(
        0f, 1f, infiniteRepeatable(tween(3500, easing = LinearEasing)), label = "parpadeo"
    )

    // Física de los pelitos que salen al cepillar.
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            withFrameMillis { t ->
                val dt = if (last == 0L) 0f else (t - last) / 1000f
                last = t
                for (i in particles.indices.reversed()) {
                    val p = particles[i]
                    val life = p.life - dt * 1.2f
                    if (life <= 0f) particles.removeAt(i)
                    else particles[i] = p.copy(
                        pos = p.pos + p.vel * dt,
                        vel = Offset(p.vel.x * 0.98f, p.vel.y + 300f * dt),
                        life = life
                    )
                }
            }
        }
    }

    Canvas(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { brushPos = it },
                    onDragEnd = { brushPos = null; brushEnd() },
                    onDragCancel = { brushPos = null; brushEnd() },
                    onDrag = { change, drag ->
                        val pos = change.position
                        brushPos = pos
                        val geom = CatGeometry(size.width.toFloat(), size.height.toFloat())
                        val zone = geom.zoneAt(pos)
                        val dtMs = max(1L, change.uptimeMillis - change.previousUptimeMillis)
                        val speedDp = drag.getDistance() / dtMs * 1000f / density
                        brush(zone, speedDp)
                        if (zone != null && particles.size < 80 && Random.nextFloat() < 0.5f) {
                            particles.add(
                                FurParticle(
                                    pos, Offset(Random.nextFloat() * 200f - 100f, -Random.nextFloat() * 180f),
                                    life = 1f
                                )
                            )
                        }
                    }
                )
            }
    ) {
        val g = CatGeometry(size.width, size.height)
        val mood = moodOf(s)

        // Sacudida cuando está muy enojado o gruñe.
        val shaking = s.phase == Phase.PLAYING && (s.annoyance > 80f || s.event == CatEvent.GRUNE)
        val shakeX = if (shaking) sin(loop * 6.28f * 12f) * g.u * 0.08f else 0f
        val purrY = if (s.isPurring) sin(loop * 6.28f * 8f) * g.u * 0.03f else 0f

        if (s.event == CatEvent.ORINA && s.phase == Phase.PLAYING) {
            drawPuddle(g, (s.eventTimeLeft / CatEvent.ORINA.durationSec).coerceIn(0.3f, 1f))
        }

        // OVNI
        val abducting = s.phase == Phase.ABDUCTING || s.phase == Phase.ABDUCTED
        val p = s.ufoProgress
        val ufoCenter = Offset(g.cx, -2f * g.u + (size.height * 0.14f + 2f * g.u) * (p / 0.25f).coerceAtMost(1f))
        val lift = ((p - 0.3f) / 0.6f).coerceIn(0f, 1f)

        if (abducting && p > 0.2f) drawBeam(ufoCenter, g.groundY, g.u, ((p - 0.2f) / 0.1f).coerceIn(0f, 1f))

        if (s.phase != Phase.ABDUCTED) {
            withTransform({
                translate(shakeX, purrY - lift * (g.cy - ufoCenter.y))
                scale(1f - 0.85f * lift, 1f - 0.85f * lift, pivot = g.bodyCenter)
            }) {
                drawCat(g, mood, s.annoyance, tailSway, blink = blinkCycle > 0.95f)
            }
        }

        if (abducting) drawUfo(ufoCenter, g.u, loop)
        if (s.isPurring && s.phase == Phase.PLAYING) drawPurr(g, loop, textMeasurer)

        for (pt in particles) {
            drawCircle(Naranja.copy(alpha = pt.life.coerceIn(0f, 1f)), g.u * 0.08f, pt.pos)
        }
        brushPos?.let { if (s.phase == Phase.PLAYING || s.phase == Phase.READY) drawBrush(it - Offset(0f, g.u * 0.9f), g.u) }
    }
}

@Composable
private fun EventBanner(s: GameState, modifier: Modifier = Modifier) {
    val e = s.event
    AnimatedVisibility(
        visible = e != null && s.phase != Phase.ABDUCTED,
        enter = slideInVertically() + fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        if (e != null) {
            Surface(shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 6.dp) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "SUCESO ${e.rarity.label}", fontSize = 11.sp, fontWeight = FontWeight.Black,
                        color = Color.White,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(e.rarity.color)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(e.title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Cafe)
                    Text(e.description, fontSize = 13.sp, color = Cafe.copy(alpha = 0.8f), textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun HintText(s: GameState) {
    val (text, color) = when {
        s.phase == Phase.READY -> "Desliza el dedo sobre el gato para cepillarlo 🐱" to Cafe
        s.phase != Phase.PLAYING -> "" to Cafe
        s.isCleaning -> "Limpiando el desastre... 🧻" to Cafe
        s.tooFast -> "¡Más suave! Lo estás cepillando muy brusco" to Peligro
        s.zone != null -> s.zone.hint to if (s.zone == CatZone.BELLY || s.zone == CatZone.TAIL) Peligro else Color(0xFF2D6A4F)
        s.annoyance > 0f -> "Descansando... el gato se calma" to Cafe.copy(alpha = 0.7f)
        else -> "Sigue cepillando" to Cafe
    }
    Text(
        text, Modifier.fillMaxWidth().height(24.dp), textAlign = TextAlign.Center,
        fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = color
    )
}

@Composable
private fun Bitacora(s: GameState) {
    Column {
        Text("Bitácora de sucesos", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Cafe.copy(alpha = 0.7f))
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (e in CatEvent.entries) {
                val found = e in s.discovered
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (found) e.rarity.color.copy(alpha = 0.15f) else Color(0x11000000))
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(e.rarity.label, fontSize = 10.sp, fontWeight = FontWeight.Black, color = e.rarity.color)
                    Text(
                        if (found) eventShortName(e) else "???",
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Cafe
                    )
                }
            }
        }
    }
}

private fun eventShortName(e: CatEvent) = when (e) {
    CatEvent.ORINA -> "Orina"
    CatEvent.GRUNE -> "Gruñido"
    CatEvent.RONRONEA -> "Ronroneo"
    CatEvent.OVNI -> "OVNI"
}

@Composable
private fun EndCard(
    title: String,
    body: String,
    record: Boolean,
    button: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier.padding(24.dp), shape = RoundedCornerShape(20.dp), color = Color.White, shadowElevation = 10.dp) {
        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, fontSize = 24.sp, fontWeight = FontWeight.Black, color = color, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(body, fontSize = 15.sp, color = Cafe, textAlign = TextAlign.Center)
            if (record) {
                Spacer(Modifier.height(6.dp))
                Text("🏆 ¡Nuevo récord!", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE09F3E))
            }
            Spacer(Modifier.height(14.dp))
            Button(onClick = onClick) { Text(button) }
        }
    }
}

private fun fmt(v: Float) = String.format(java.util.Locale.US, "%.1f", v)
