package com.darkmode.scrollgridstudio.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkFlatScheme = darkColorScheme(
    primary = Color(0xFF4ADE80),
    onPrimary = Color(0xFF061B0E),
    secondary = Color(0xFF38BDF8),
    onSecondary = Color(0xFF061822),
    background = Color(0xFF0C0F14),
    onBackground = Color(0xFFF3F4F6),
    surface = Color(0xFF131820),
    onSurface = Color(0xFFF3F4F6),
    surfaceVariant = Color(0xFF1C232E),
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF2B3544)
)

@Composable
fun ScrollGridTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkFlatScheme,
        content = content
    )
}
