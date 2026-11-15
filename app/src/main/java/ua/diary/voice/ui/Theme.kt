package ua.diary.voice.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext

/** Кольорова тема. [light]/[dark] — основний колір для світлого й темного режимів. */
data class AppPalette(val id: String, val name: String, val light: Color, val dark: Color)

val PALETTES = listOf(
    AppPalette("violet", "Фіолетова", Color(0xFF5B4B8A), Color(0xFFC9B8FF)),
    AppPalette("khaki", "Хакі", Color(0xFF4F6B2E), Color(0xFFB5D18C)),
    AppPalette("navy", "Морська", Color(0xFF1F5FA8), Color(0xFFA8C8FF)),
    AppPalette("teal", "Бірюзова", Color(0xFF00696E), Color(0xFF6FD7DD)),
    AppPalette("amber", "Бурштинова", Color(0xFF8A5100), Color(0xFFFFB86B)),
    AppPalette("cherry", "Вишнева", Color(0xFF9C2F4A), Color(0xFFFFB1C2)),
    AppPalette("graphite", "Графіт", Color(0xFF455A64), Color(0xFFB0C4CE)),
)

/** Особливий ідентифікатор: кольори з шпалер телефона (Android 12+). */
const val PALETTE_SYSTEM = "system"

enum class DarkMode(val label: String) { SYSTEM("Як у системі"), LIGHT("Світла"), DARK("Темна") }

val supportsDynamicColor get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

private val Neutral = Color(0xFF777680)

private fun lightScheme(p: Color): ColorScheme {
    val bg = lerp(p, Color.White, 0.96f)
    return lightColorScheme(
        primary = p,
        onPrimary = Color.White,
        primaryContainer = lerp(p, Color.White, 0.78f),
        onPrimaryContainer = lerp(p, Color.Black, 0.6f),
        secondary = lerp(p, Neutral, 0.45f),
        onSecondary = Color.White,
        secondaryContainer = lerp(p, Color.White, 0.86f),
        onSecondaryContainer = lerp(p, Color.Black, 0.65f),
        tertiary = lerp(p, Color(0xFF7D5260), 0.5f),
        background = bg,
        surface = bg,
        surfaceVariant = lerp(p, Color.White, 0.88f),
        onSurfaceVariant = lerp(p, Color(0xFF44464F), 0.8f),
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = lerp(p, Color.White, 0.94f),
        surfaceContainer = lerp(p, Color.White, 0.92f),
        surfaceContainerHigh = lerp(p, Color.White, 0.90f),
        surfaceContainerHighest = lerp(p, Color.White, 0.87f),
        outlineVariant = lerp(p, Color.White, 0.75f),
    )
}

private fun darkScheme(p: Color, base: Color): ColorScheme {
    val bg = lerp(base, Color.Black, 0.88f)
    return darkColorScheme(
        primary = p,
        onPrimary = lerp(p, Color.Black, 0.78f),
        primaryContainer = lerp(base, Color.Black, 0.35f),
        onPrimaryContainer = lerp(p, Color.White, 0.6f),
        secondary = lerp(p, Color(0xFFC6C5D0), 0.45f),
        onSecondary = lerp(p, Color.Black, 0.8f),
        secondaryContainer = lerp(base, Color.Black, 0.6f),
        onSecondaryContainer = lerp(p, Color.White, 0.7f),
        background = bg,
        surface = bg,
        surfaceVariant = lerp(base, Color.Black, 0.7f),
        onSurfaceVariant = lerp(p, Color(0xFFC6C5D0), 0.75f),
        surfaceContainerLowest = lerp(base, Color.Black, 0.93f),
        surfaceContainerLow = lerp(base, Color.Black, 0.86f),
        surfaceContainer = lerp(base, Color.Black, 0.83f),
        surfaceContainerHigh = lerp(base, Color.Black, 0.79f),
        surfaceContainerHighest = lerp(base, Color.Black, 0.75f),
        outlineVariant = lerp(base, Color.Black, 0.55f),
    )
}

@Composable
fun isDark(mode: DarkMode): Boolean = when (mode) {
    DarkMode.SYSTEM -> isSystemInDarkTheme()
    DarkMode.LIGHT -> false
    DarkMode.DARK -> true
}

@Composable
fun DiaryTheme(paletteId: String = "violet", darkMode: DarkMode = DarkMode.SYSTEM, content: @Composable () -> Unit) {
    val dark = isDark(darkMode)
    val ctx = LocalContext.current
    val scheme = if (paletteId == PALETTE_SYSTEM && supportsDynamicColor) {
        if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
    } else {
        val p = PALETTES.firstOrNull { it.id == paletteId } ?: PALETTES.first()
        if (dark) darkScheme(p.dark, p.light) else lightScheme(p.light)
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
