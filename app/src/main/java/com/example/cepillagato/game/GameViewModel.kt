package com.example.cepillagato.game

import android.app.Application
import android.content.Context
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

class GameViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("cepilla_gato", Context.MODE_PRIVATE)

    var state by mutableStateOf(
        GameState(best = prefs.getFloat(KEY_BEST, 0f), discovered = loadDiscovered())
    )
        private set

    // Entrada del jugador (se actualiza desde la UI en cada movimiento del dedo).
    private var currentZone: CatZone? = null
    private var currentSpeedDp = 0f
    private var lastMoveAt = 0L
    private var eventAccumulator = 0f

    init {
        viewModelScope.launch {
            var last = SystemClock.uptimeMillis()
            while (isActive) {
                delay(16)
                val now = SystemClock.uptimeMillis()
                val dt = ((now - last) / 1000f).coerceAtMost(0.1f)
                last = now
                tick(now, dt)
            }
        }
    }

    /** Llamado mientras el dedo se mueve. [zone] es null si el dedo está fuera del gato. */
    fun onBrush(zone: CatZone?, speedDpPerSec: Float) {
        if (state.phase == Phase.READY && zone != null) {
            state = state.copy(phase = Phase.PLAYING)
        }
        currentZone = zone
        currentSpeedDp = speedDpPerSec
        if (zone != null) lastMoveAt = SystemClock.uptimeMillis()
    }

    fun onBrushEnd() {
        currentZone = null
    }

    fun restart() {
        currentZone = null
        eventAccumulator = 0f
        state = GameState(best = state.best, discovered = state.discovered)
    }

    private fun tick(now: Long, dt: Float) {
        var s = state

        // Temporizadores
        val eventLeft = (s.eventTimeLeft - dt).coerceAtLeast(0f)
        s = s.copy(
            eventTimeLeft = eventLeft,
            event = if (eventLeft > 0f || s.phase == Phase.ABDUCTING || s.phase == Phase.ABDUCTED) s.event else null,
            purrTimeLeft = (s.purrTimeLeft - dt).coerceAtLeast(0f),
            cleaningTimeLeft = (s.cleaningTimeLeft - dt).coerceAtLeast(0f)
        )

        when (s.phase) {
            Phase.PLAYING -> s = updatePlaying(s, now, dt)
            Phase.ABDUCTING -> {
                val p = s.ufoProgress + dt / ABDUCTION_SECONDS
                s = if (p >= 1f) s.copy(ufoProgress = 1f, phase = Phase.ABDUCTED, isBrushing = false)
                else s.copy(ufoProgress = p, isBrushing = false)
            }
            else -> s = s.copy(isBrushing = false)
        }
        state = s
    }

    private fun updatePlaying(start: GameState, now: Long, dt: Float): GameState {
        var s = start
        val zone = currentZone
        val brushing = zone != null && now - lastMoveAt < 150 && !s.isCleaning
        val tooFast = brushing && currentSpeedDp > FAST_DP_PER_SEC
        var annoy = s.annoyance
        var score = s.score

        if (brushing) {
            // Entre más tiempo aguantes, más rápido se fastidia.
            val difficulty = 1f + s.score / 45f
            var rate = zone!!.annoyPerSecond * difficulty
            if (tooFast) rate += 30f
            if (s.isPurring) rate *= 0.3f
            annoy += rate * dt
            score += dt * if (s.isPurring) 2f else 1f

            eventAccumulator += dt
            if (eventAccumulator >= 1f) {
                eventAccumulator -= 1f
                if (s.event == null) rollEvent()?.let { s = applyEvent(s.copy(annoyance = annoy, score = score), it) }
                annoy = s.annoyance
                score = s.score
                if (s.phase != Phase.PLAYING) return s
            }
        } else {
            // Si lo dejas descansar, se calma.
            annoy -= 12f * dt
        }

        annoy = annoy.coerceIn(0f, 100f)
        s = s.copy(annoyance = annoy, score = score, isBrushing = brushing, zone = if (brushing) zone else null, tooFast = tooFast)

        if (annoy >= 100f) s = bite(s)
        return s
    }

    /** Tira los dados: del más raro al más común. */
    private fun rollEvent(): CatEvent? {
        val r = Random.nextFloat()
        var acc = 0f
        for (e in listOf(CatEvent.OVNI, CatEvent.RONRONEA, CatEvent.GRUNE, CatEvent.ORINA)) {
            acc += e.chancePerSecond
            if (r < acc) return e
        }
        return null
    }

    private fun applyEvent(start: GameState, e: CatEvent): GameState {
        var s = start.copy(event = e, eventTimeLeft = e.durationSec, eventCounter = start.eventCounter + 1)
        if (e !in s.discovered) {
            val disc = s.discovered + e
            saveDiscovered(disc)
            s = s.copy(discovered = disc)
        }
        return when (e) {
            CatEvent.ORINA -> s.copy(
                score = (s.score - 3f).coerceAtLeast(0f),
                cleaningTimeLeft = e.durationSec,
                annoyance = (s.annoyance - 15f).coerceAtLeast(0f)
            )
            CatEvent.GRUNE -> s.copy(annoyance = (s.annoyance + 25f).coerceAtMost(99f))
            CatEvent.RONRONEA -> s.copy(annoyance = 0f, purrTimeLeft = e.durationSec)
            CatEvent.OVNI -> {
                // Final legendario: el puntaje se conserva como récord.
                val record = s.score > s.best
                if (record) saveBest(s.score)
                s.copy(
                    phase = Phase.ABDUCTING, ufoProgress = 0f, isBrushing = false,
                    best = maxOf(s.best, s.score), newRecord = record
                )
            }
        }
    }

    private fun bite(s: GameState): GameState {
        val record = s.score > s.best
        if (record) saveBest(s.score)
        return s.copy(
            phase = Phase.BITTEN,
            best = maxOf(s.best, s.score),
            newRecord = record,
            isBrushing = false,
            event = null,
            eventCounter = s.eventCounter + 1
        )
    }

    private fun saveBest(v: Float) = prefs.edit().putFloat(KEY_BEST, v).apply()

    private fun loadDiscovered(): Set<CatEvent> =
        prefs.getStringSet(KEY_DISCOVERED, emptySet()).orEmpty()
            .mapNotNull { name -> CatEvent.entries.firstOrNull { it.name == name } }
            .toSet()

    private fun saveDiscovered(set: Set<CatEvent>) =
        prefs.edit().putStringSet(KEY_DISCOVERED, set.map { it.name }.toSet()).apply()

    companion object {
        private const val KEY_BEST = "best"
        private const val KEY_DISCOVERED = "discovered"
        const val FAST_DP_PER_SEC = 1400f
        const val ABDUCTION_SECONDS = 4f
    }
}
