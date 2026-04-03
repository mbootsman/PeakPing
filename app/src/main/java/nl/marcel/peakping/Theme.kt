package nl.marcel.peakping

import androidx.compose.ui.graphics.Color

enum class ThemeMode { SYSTEM, DARK, LIGHT }

enum class UnitSystem { METRIC, IMPERIAL }

val AccentGreen = Color(0xFF3A8C66)

data class AppColors(
    val bg: Color,
    val text: Color,
    val dimText: Color,
    val dimAccent: Color,
)

val DarkColors = AppColors(
    bg = Color(0xFF0D0D0D),
    text = Color.White,
    dimText = Color.White.copy(alpha = 0.55f),
    dimAccent = AccentGreen.copy(alpha = 0.65f),
)

val LightColors = AppColors(
    bg = Color(0xFFF5F5F5),
    text = Color(0xFF1A1A1A),
    dimText = Color(0xFF1A1A1A).copy(alpha = 0.55f),
    dimAccent = AccentGreen.copy(alpha = 0.80f),
)
