package nl.marcel.peakping

import androidx.compose.ui.graphics.Color

enum class ThemeMode { SYSTEM, DARK, LIGHT }

enum class UnitSystem { METRIC, IMPERIAL }

// Brand palette
val Navy   = Color(0xFF0C447C)
val Ocean  = Color(0xFF185FA5)
val Sky    = Color(0xFF378ADD)
val Summit = Color(0xFF00AAB3)
val Signal = Color(0xFF9FE1CB)

// Primary accent used across rings, radio buttons, dividers
val AccentGreen = Summit

data class AppColors(
    val bg: Color,
    val text: Color,
    val dimText: Color,
    val dimAccent: Color,
)

val DarkColors = AppColors(
    bg = Color(0xFF080E14),          // very dark navy tint
    text = Color.White,
    dimText = Color.White.copy(alpha = 0.55f),
    dimAccent = Signal.copy(alpha = 0.65f),
)

val LightColors = AppColors(
    bg = Color(0xFFF0F6FF),          // pale sky tint
    text = Navy,
    dimText = Navy.copy(alpha = 0.55f),
    dimAccent = Ocean.copy(alpha = 0.80f),
)
