package com.darkmode.scrollgridstudio.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
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
    paper = Color(0xFFF5F6F8),
    ink = Color(0xFF14171D),
    mutedInk = Color(0xFF646B78),
    blue = Color(0xFF3B6EF6),
    bluePale = Color(0xFFE6EEFF),
    coral = Color(0xFFD97254),
    cardPaper = Color(0xFFFFFFFF),
    hairline = Color(0xFFE2E5EC),
    canvas = Color(0xFFEEF0F4),
    panel = Color(0xFFFFFFFF),
    elevated = Color(0xFFE9EDF5),
    border = Color(0xFFDCE0E8),
    accent = Color(0xFF3B6EF6),
    danger = Color(0xFFDC3F3F),
    textPrimary = Color(0xFF14171D),
    textSecondary = Color(0xFF646B78)
)

private val DarkPalette = ScrollGridPalette(
    paper = Color(0xFF0E1014),
    ink = Color(0xFFF2F4F8),
    mutedInk = Color(0xFF8C93A1),
    blue = Color(0xFF5B8DEF),
    bluePale = Color(0xFF1B253A),
    coral = Color(0xFFE07A5F),
    cardPaper = Color(0xFF161920),
    hairline = Color(0xFF262B36),
    canvas = Color(0xFF0B0D11),
    panel = Color(0xFF14171E),
    elevated = Color(0xFF1F2430),
    border = Color(0xFF282E3A),
    accent = Color(0xFF5B8DEF),
    danger = Color(0xFFEF5350),
    textPrimary = Color(0xFFF2F4F8),
    textSecondary = Color(0xFF8C93A1)
)

val LocalScrollGridPalette = staticCompositionLocalOf { DarkPalette }

@Composable
fun ScrollGridTheme(
    preference: ThemePreference = ThemePreference.DARK,
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
            primary = palette.accent,
            onPrimary = Color(0xFF0A0D14),
            secondary = palette.coral,
            onSecondary = Color(0xFF1E120E),
            background = palette.canvas,
            onBackground = palette.textPrimary,
            surface = palette.panel,
            onSurface = palette.textPrimary,
            surfaceVariant = palette.elevated,
            onSurfaceVariant = palette.textSecondary,
            outline = palette.border
        )
    } else {
        lightColorScheme(
            primary = palette.accent,
            onPrimary = Color.White,
            secondary = palette.coral,
            onSecondary = Color.White,
            background = palette.canvas,
            onBackground = palette.textPrimary,
            surface = palette.panel,
            onSurface = palette.textPrimary,
            surfaceVariant = palette.elevated,
            onSurfaceVariant = palette.textSecondary,
            outline = palette.border
        )
    }
    CompositionLocalProvider(LocalScrollGridPalette provides palette) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
