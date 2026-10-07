# Meow or Never 🐱

Juego para Android (Kotlin + Jetpack Compose). Cepilla al gato el mayor tiempo posible sin que se fastidie: si el fastidio llega a 100 %, te muerde y vuelves a empezar desde 0.

## Cómo abrirlo
1. Android Studio → **File › Open** → selecciona la carpeta `CepillaAlGato`.
2. Espera la sincronización de Gradle (JDK 17+, Android SDK 35).
3. Ejecuta en un emulador o teléfono (Android 8.0+).

## Cómo se juega
- Desliza el dedo sobre el gato para cepillarlo. Solo suma tiempo mientras cepillas.
- Cada zona fastidia distinto: **cabeza** (le encanta) < **lomo** < **cola** < **panza** (¡trampa!).
- Cepillar muy rápido lo fastidia más. Si sueltas, se calma poco a poco.
- Entre más aguantes, más rápido se fastidia.

## Sucesos (probabilidad por cada segundo cepillando)
| Rareza | Suceso | Prob. | Efecto |
|---|---|---|---|
| Común | Te orina | 6 % | −3 s y 2.5 s limpiando sin poder cepillar |
| Normal | Te gruñe | 3 % | +25 de fastidio |
| Raro | Ronronea | 0.8 % | Fastidio a 0 y puntos x2 por 8 s |
| Único | Lo abduce un OVNI | 0.1 % | Fin legendario; tu puntaje cuenta como récord |

El récord y los sucesos descubiertos (bitácora) se guardan en el teléfono.

