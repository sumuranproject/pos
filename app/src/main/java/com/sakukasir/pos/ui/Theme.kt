package com.sakukasir.pos.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.sakukasir.pos.R

private val LightColors = lightColorScheme(
    primary = Color(0xFF0F5132), onPrimary = Color.White,
    primaryContainer = Color(0xFFE7F0EA), onPrimaryContainer = Color(0xFF0A3D26),
    secondary = Color(0xFF7C3AED), onSecondary = Color.White,
    error = Color(0xFFDC2626), background = Color(0xFFFAFAF7),
    surface = Color(0xFFFFFFFF), surfaceVariant = Color(0xFFF1F0EA),
    outline = Color(0xFFE5E3DA), onSurface = Color(0xFF1A1A1A),
    onSurfaceVariant = Color(0xFF6B6B63)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF22A06B), onPrimary = Color.White,
    primaryContainer = Color(0xFF0F2A1E), onPrimaryContainer = Color(0xFFB8E8D0),
    secondary = Color(0xFFA78BFA), onSecondary = Color(0xFF24104E),
    error = Color(0xFFF87171), background = Color(0xFF0F0F0D),
    surface = Color(0xFF17171A), surfaceVariant = Color(0xFF1A1A17),
    outline = Color(0xFF2A2A26), onSurface = Color(0xFFF5F5F0),
    onSurfaceVariant = Color(0xFFA0A09A)
)

// Bundled locally in res/font. No runtime font download is required.
val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
)

private fun inter(base: TextStyle) = base.copy(fontFamily = Inter)

val SkTypography = Typography(
    displayLarge = inter(Typography().displayLarge),
    displayMedium = inter(Typography().displayMedium),
    displaySmall = inter(Typography().displaySmall),
    headlineLarge = inter(Typography().headlineLarge),
    headlineMedium = inter(Typography().headlineMedium),
    headlineSmall = inter(Typography().headlineSmall),
    titleLarge = inter(Typography().titleLarge),
    titleMedium = inter(Typography().titleMedium),
    titleSmall = inter(Typography().titleSmall),
    bodyLarge = inter(Typography().bodyLarge),
    bodyMedium = inter(Typography().bodyMedium),
    bodySmall = inter(Typography().bodySmall),
    labelLarge = inter(Typography().labelLarge),
    labelMedium = inter(Typography().labelMedium),
    labelSmall = inter(Typography().labelSmall),
)

/** Use this style for TRX IDs, receipt/thermal preview and audit timestamps. */
fun trxMonoStyle(weight: FontWeight = FontWeight.Normal, size: androidx.compose.ui.unit.TextUnit = 14.sp) =
    TextStyle(fontFamily = JetBrainsMono, fontWeight = weight, fontSize = size)

@Composable
fun SakuKasirTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = SkTypography,
        content = content
    )
}
