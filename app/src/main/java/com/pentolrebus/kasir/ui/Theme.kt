package com.pentolrebus.kasir.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LogoBlue = Color(0xFF1F6FEB)
private val LogoBlueDark = Color(0xFF5B9DFF)

private val LightColors = lightColorScheme(
    primary = LogoBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD9EDFF),
    onPrimaryContainer = Color(0xFF00345F),
    background = Color(0xFFF4F7FC),
    surface = Color.White,
    surfaceVariant = Color(0xFFE8F1F8),
    onSurfaceVariant = Color(0xFF52606D),
    secondary = Color(0xFF42627A),
    error = Color(0xFFD64545)
)

private val DarkColors = darkColorScheme(
    primary = LogoBlueDark,
    onPrimary = Color(0xFF003A62),
    primaryContainer = Color(0xFF075C99),
    onPrimaryContainer = Color(0xFFD9EDFF),
    background = Color(0xFF111722),
    surface = Color(0xFF102333),
    surfaceVariant = Color(0xFF1B3448),
    onSurfaceVariant = Color(0xFFB9CAD8),
    secondary = Color(0xFF9FC2DA),
    error = Color(0xFFF06A6A)
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
