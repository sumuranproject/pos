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
private val T = Color(0xFF1C1B22)
private val M = Color(0xFF66708A)
private val B = Color(0xFFDFE6F2)
private val G = Color(0xFF1E9E5A)
private val R = Color(0xFFD64545)

private val LightColors = lightColorScheme(
    primary = P, onPrimary = Color.White, primaryContainer = PL, onPrimaryContainer = P,
    background = BG, onBackground = T, surface = Color.White, onSurface = T,
    surfaceVariant = PL, onSurfaceVariant = M, outline = B, outlineVariant = B,
    secondary = M, onSecondary = Color.White, error = R, onError = Color.White
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF6FA8FF), onPrimary = Color(0xFF082A57), primaryContainer = Color(0xFF153D70), onPrimaryContainer = Color(0xFFD9E8FF),
    background = Color(0xFF10151F), onBackground = Color(0xFFF1F4F9), surface = Color(0xFF171E29), onSurface = Color(0xFFF1F4F9),
    surfaceVariant = Color(0xFF202B3A), onSurfaceVariant = Color(0xFFAAB6C8), outline = Color(0xFF354255), outlineVariant = Color(0xFF354255),
    secondary = Color(0xFFAAB6C8), onSecondary = Color(0xFF10151F), error = Color(0xFFFF7474), onError = Color(0xFF3B0808)
)

private val SakuTypography = Typography(
    displayLarge = TextStyle(fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.ExtraBold),
    headlineLarge = TextStyle(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.ExtraBold),
    headlineSmall = TextStyle(fontSize = 20.sp, lineHeight = 25.sp, fontWeight = FontWeight.ExtraBold),
    titleLarge = TextStyle(fontSize = 18.sp, lineHeight = 23.sp, fontWeight = FontWeight.ExtraBold),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 21.sp, fontWeight = FontWeight.Bold),
    titleSmall = TextStyle(fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 21.sp, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 19.sp, fontWeight = FontWeight.Normal),
    bodySmall = TextStyle(fontSize = 13.sp, lineHeight = 17.sp, fontWeight = FontWeight.Normal),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold),
    labelMedium = TextStyle(fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold),
    labelSmall = TextStyle(fontSize = 12.sp, lineHeight = 15.sp, fontWeight = FontWeight.SemiBold)
)

@Composable
fun KasirTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, typography = SakuTypography, content = content)
}
