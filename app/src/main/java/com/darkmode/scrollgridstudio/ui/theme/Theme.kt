package com.darkmode.scrollgridstudio.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

enum class ThemePreference(val label: String) {
    SYSTEM("System"), LIGHT("Light"), DARK("Dark");

    companion object {
        fun fromStorage(value: String?): ThemePreference =
            entries.firstOrNull { it.name == value } ?: SYSTEM
    }
}

data class ScrollGridPalette(
    val paper: Color,
    val ink: Color,
    val mutedInk: Color,
    val blue: Color,
    val bluePale: Color,
    val coral: Color,
    val cardPaper: Color,
    val hairline: Color,
    val canvas: Color,
    val panel: Color,
    val elevated: Color,
    val border: Color,
    val accent: Color,
    val danger: Color,
    val textPrimary: Color,
    val textSecondary: Color
)

private val LightPalette = ScrollGridPalette(
    paper = Color(0xFFF2F0E9), ink = Color(0xFF171916), mutedInk = Color(0xFF777A72),
    blue = Color(0xFF3157F6), bluePale = Color(0xFFE0E6FF), coral = Color(0xFFE8795B),
    cardPaper = Color(0xFFFFFEFA), hairline = Color(0xFFE0DED5),
    canvas = Color(0xFFF2F0E9), panel = Color(0xFFFFFEFA), elevated = Color(0xFFE0E6FF),
    border = Color(0xFFE0DED5), accent = Color(0xFF3157F6), danger = Color(0xFFDC4949),
    textPrimary = Color(0xFF171916), textSecondary = Color(0xFF777A72)
)

private val DarkPalette = ScrollGridPalette(
    paper = Color(0xFF121417), ink = Color(0xFFF3F2ED), mutedInk = Color(0xFFAAAFA8),
    blue = Color(0xFF91A5FF), bluePale = Color(0xFF29334F), coral = Color(0xFFFF9A7F),
    cardPaper = Color(0xFF1B1F24), hairline = Color(0xFF343A42),
    canvas = Color(0xFF121417), panel = Color(0xFF1B1F24), elevated = Color(0xFF29334F),
    border = Color(0xFF343A42), accent = Color(0xFF91A5FF), danger = Color(0xFFFF7777),
    textPrimary = Color(0xFFF3F2ED), textSecondary = Color(0xFFAAAFA8)
)

val LocalScrollGridPalette = staticCompositionLocalOf { LightPalette }

@Composable
fun ScrollGridTheme(
    preference: ThemePreference = ThemePreference.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (preference) {
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
    }
    val palette = if (darkTheme) DarkPalette else LightPalette
    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = palette.accent, onPrimary = Color(0xFF10131A),
            secondary = palette.coral, onSecondary = Color(0xFF21130F),
            background = palette.canvas, onBackground = palette.textPrimary,
            surface = palette.panel, onSurface = palette.textPrimary,
            surfaceVariant = palette.elevated, onSurfaceVariant = palette.textSecondary,
            outline = palette.border
        )
    } else {
        lightColorScheme(
            primary = palette.accent, onPrimary = Color.White,
            secondary = palette.coral, onSecondary = Color.White,
            background = palette.canvas, onBackground = palette.textPrimary,
            surface = palette.panel, onSurface = palette.textPrimary,
            surfaceVariant = palette.elevated, onSurfaceVariant = palette.textSecondary,
            outline = palette.border
        )
    }
    CompositionLocalProvider(LocalScrollGridPalette provides palette) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
