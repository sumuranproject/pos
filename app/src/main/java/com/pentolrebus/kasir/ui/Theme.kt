package com.pentolrebus.kasir.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val P = Color(0xFF1F6FEB)
private val PL = Color(0xFFE6F0FF)
private val PG = Color(0xFFE8EEF7)
private val BG = Color(0xFFF4F7FC)
private val C = Color(0xFFFFFFFF)
private val T = Color(0xFF1C1B22)
private val M = Color(0xFF66708A)
private val B = Color(0xFFDFE6F2)
private val G = Color(0xFF1E9E5A)
private val R = Color(0xFFD64545)

private val LightColors = lightColorScheme(
    primary = P, onPrimary = Color.White, primaryContainer = PL, onPrimaryContainer = P,
    background = BG, onBackground = T, surface = C, onSurface = T,
    surfaceVariant = PL, onSurfaceVariant = M, outline = B, outlineVariant = B,
    secondary = M, onSecondary = Color.White, error = R, onError = Color.White
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF5B9DFF), onPrimary = Color(0xFF06101F), primaryContainer = Color(0xFF1B2C4A), onPrimaryContainer = Color(0xFFEEF2FA),
    background = Color(0xFF111722), onBackground = Color(0xFFEEF2FA), surface = Color(0xFF1B2332), onSurface = Color(0xFFEEF2FA),
    surfaceVariant = Color(0xFF1B2C4A), onSurfaceVariant = Color(0xFF9AA7BF), outline = Color(0xFF2A3550), outlineVariant = Color(0xFF2A3550),
    secondary = Color(0xFF9AA7BF), onSecondary = Color(0xFF111722), error = Color(0xFFF06A6A), onError = Color(0xFF06101F)
)

private const val HTML_DPI_SCALE = 1.10f

// saku-kasir-dpi.html: html{font-size:110%}.
// Compose uses dp/sp rather than CSS px/rem, so the 110% reference is
// applied to typography while dp remains density-independent.
private val SakuTypography = Typography(
    displayLarge = TextStyle(fontSize = (32f * HTML_DPI_SCALE).sp, lineHeight = (38f * HTML_DPI_SCALE).sp, fontWeight = FontWeight.ExtraBold),
    headlineLarge = TextStyle(fontSize = (24f * HTML_DPI_SCALE).sp, lineHeight = (30f * HTML_DPI_SCALE).sp, fontWeight = FontWeight.ExtraBold),
    headlineSmall = TextStyle(fontSize = (20f * HTML_DPI_SCALE).sp, lineHeight = (25f * HTML_DPI_SCALE).sp, fontWeight = FontWeight.ExtraBold),
    titleLarge = TextStyle(fontSize = (18f * HTML_DPI_SCALE).sp, lineHeight = (23f * HTML_DPI_SCALE).sp, fontWeight = FontWeight.ExtraBold),
    titleMedium = TextStyle(fontSize = (16f * HTML_DPI_SCALE).sp, lineHeight = (21f * HTML_DPI_SCALE).sp, fontWeight = FontWeight.Bold),
    titleSmall = TextStyle(fontSize = (14f * HTML_DPI_SCALE).sp, lineHeight = (18f * HTML_DPI_SCALE).sp, fontWeight = FontWeight.Bold),
    bodyLarge = TextStyle(fontSize = (16f * HTML_DPI_SCALE).sp, lineHeight = (21f * HTML_DPI_SCALE).sp, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontSize = (14f * HTML_DPI_SCALE).sp, lineHeight = (19f * HTML_DPI_SCALE).sp, fontWeight = FontWeight.Normal),
    bodySmall = TextStyle(fontSize = (13f * HTML_DPI_SCALE).sp, lineHeight = (17f * HTML_DPI_SCALE).sp, fontWeight = FontWeight.Normal),
    labelLarge = TextStyle(fontSize = (14f * HTML_DPI_SCALE).sp, lineHeight = (18f * HTML_DPI_SCALE).sp, fontWeight = FontWeight.Bold),
    labelMedium = TextStyle(fontSize = (13f * HTML_DPI_SCALE).sp, lineHeight = (16f * HTML_DPI_SCALE).sp, fontWeight = FontWeight.SemiBold),
    labelSmall = TextStyle(fontSize = (12f * HTML_DPI_SCALE).sp, lineHeight = (15f * HTML_DPI_SCALE).sp, fontWeight = FontWeight.SemiBold)
)

@Composable
fun KasirTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, typography = SakuTypography, content = content)
}
