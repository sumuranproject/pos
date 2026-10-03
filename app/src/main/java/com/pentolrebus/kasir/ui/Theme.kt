package com.pentolrebus.kasir.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// SakuKasir blue brand. The mockup's structure is retained, but its purple palette is not.
private val Blue = Color(0xFF2563EB)
private val BlueDark = Color(0xFF60A5FA)

private val LightColors = lightColorScheme(
    primary = Blue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8F0FF),
    onPrimaryContainer = Color(0xFF123A88),
    background = Color(0xFFF6F8FC),
    surface = Color.White,
    surfaceVariant = Color(0xFFEEF2F7),
    onSurfaceVariant = Color(0xFF667085),
    outline = Color(0xFFD9E0EA),
    secondary = Color(0xFF667085),
    error = Color(0xFFD64545),
    errorContainer = Color(0xFFFFE9E9),
    onErrorContainer = Color(0xFF8B1E1E)
)

private val DarkColors = darkColorScheme(
    primary = BlueDark,
    onPrimary = Color(0xFF071A3A),
    primaryContainer = Color(0xFF16366F),
    onPrimaryContainer = Color(0xFFEAF2FF),
    background = Color(0xFF0D1117),
    surface = Color(0xFF151B24),
    surfaceVariant = Color(0xFF1E2733),
    onSurfaceVariant = Color(0xFFA7B1C2),
    outline = Color(0xFF344052),
    secondary = Color(0xFFA7B1C2),
    error = Color(0xFFFF7777),
    errorContainer = Color(0xFF4A2020),
    onErrorContainer = Color(0xFFFFDADA)
)

@Composable
fun KasirTheme(
    dark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = Typography(),
        content = content
    )
}
