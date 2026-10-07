package com.example.cepillagato.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Fondo = Color(0xFFFFF6E9)
val Naranja = Color(0xFFF2994A)
val NaranjaOscuro = Color(0xFFC96F2A)
val Crema = Color(0xFFFFE8CC)
val Cafe = Color(0xFF3B2A1A)
val Peligro = Color(0xFFD62828)

private val Esquema = lightColorScheme(
    primary = NaranjaOscuro,
    onPrimary = Color.White,
    secondary = Color(0xFF2A9D8F),
    background = Fondo,
    surface = Color.White,
    onBackground = Cafe,
    onSurface = Cafe
)

@Composable
fun CepillaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Esquema, content = content)
}
