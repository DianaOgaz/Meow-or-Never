package com.example.cepillagato.game

import androidx.compose.ui.graphics.Color

/** Zonas del gato que puedes cepillar. Cada una lo fastidia a distinta velocidad (puntos por segundo). */
enum class CatZone(val annoyPerSecond: Float, val hint: String) {
    HEAD(3f, "Le encanta en la cabeza... prrr"),
    BACK(6f, "Bien, el lomo le gusta"),
    TAIL(22f, "¡A la cola no le gusta!"),
    BELLY(38f, "¡LA PANZA NO! ¡Es una trampa!")
}

/** Rareza de cada suceso. */
enum class Rarity(val label: String, val color: Color) {
    COMUN("COMÚN", Color(0xFF8D99AE)),
    NORMAL("NORMAL", Color(0xFF2A9D8F)),
    RARO("RARO", Color(0xFF7B2CBF)),
    UNICO("ÚNICO", Color(0xFFE09F3E))
}

/**
 * Sucesos aleatorios. [chancePerSecond] es la probabilidad de que ocurra
 * en cada segundo que pasas cepillando.
 */
enum class CatEvent(
    val rarity: Rarity,
    val title: String,
    val description: String,
    val chancePerSecond: Float,
    val durationSec: Float
) {
    ORINA(
        Rarity.COMUN, "¡El gato te orinó! 💦",
        "Pierdes 3 s y tienes que limpiar antes de seguir.",
        chancePerSecond = 0.06f, durationSec = 2.5f
    ),
    GRUNE(
        Rarity.NORMAL, "Grrrrrr...",
        "Te gruñó: su fastidio sube de golpe. ¡Cuidado!",
        chancePerSecond = 0.03f, durationSec = 2f
    ),
    RONRONEA(
        Rarity.RARO, "Prrrrrr ♥",
        "¡Ronronea! Fastidio a 0 y puntos x2 por 8 s.",
        chancePerSecond = 0.008f, durationSec = 8f
    ),
    OVNI(
        Rarity.UNICO, "¡¡UN OVNI!! 🛸",
        "Unos extraterrestres se llevaron al gato...",
        chancePerSecond = 0.001f, durationSec = 4f
    )
}

enum class Phase { READY, PLAYING, BITTEN, ABDUCTING, ABDUCTED }

data class GameState(
    val phase: Phase = Phase.READY,
    /** Segundos cepillados en esta partida (el puntaje). */
    val score: Float = 0f,
    val best: Float = 0f,
    /** 0..100. Si llega a 100, te muerde. */
    val annoyance: Float = 0f,
    val isBrushing: Boolean = false,
    val zone: CatZone? = null,
    val tooFast: Boolean = false,
    val event: CatEvent? = null,
    val eventTimeLeft: Float = 0f,
    /** Se incrementa cada vez que ocurre un suceso (sirve para vibrar). */
    val eventCounter: Int = 0,
    val purrTimeLeft: Float = 0f,
    val cleaningTimeLeft: Float = 0f,
    val ufoProgress: Float = 0f,
    val discovered: Set<CatEvent> = emptySet(),
    val newRecord: Boolean = false
) {
    val isPurring get() = purrTimeLeft > 0f
    val isCleaning get() = cleaningTimeLeft > 0f
}
