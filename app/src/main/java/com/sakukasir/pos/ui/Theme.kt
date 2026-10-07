package com.sakukasir.pos.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.sakukasir.pos.R

@Immutable
class SkColors(
    val primary: Color, val primaryHover: Color, val primarySoft: Color,
    val cash: Color, val cashSoft: Color, val qris: Color, val qrisSoft: Color,
    val alert: Color, val alertSoft: Color, val warn: Color, val warnSoft: Color,
    val surface: Color, val surfaceAlt: Color, val card: Color,
    val border: Color, val borderStrong: Color,
    val text: Color, val textMuted: Color, val textFaint: Color, val dark: Boolean
)

// Values copied 1:1 from the web reference (style.css :root and [data-theme="dark"]).
val LightSk = SkColors(
    primary = Color(0xFF0F5132), primaryHover = Color(0xFF0A3D26), primarySoft = Color(0xFFE7F0EA),
    cash = Color(0xFF16A34A), cashSoft = Color(0xFFDCFCE7), qris = Color(0xFF7C3AED), qrisSoft = Color(0xFFEDE9FE),
    alert = Color(0xFFDC2626), alertSoft = Color(0xFFFEE2E2), warn = Color(0xFFD97706), warnSoft = Color(0xFFFEF3C7),
    surface = Color(0xFFFAFAF7), surfaceAlt = Color(0xFFF1F0EA), card = Color(0xFFFFFFFF),
    border = Color(0xFFE5E3DA), borderStrong = Color(0xFFD6D4C9),
    text = Color(0xFF1A1A1A), textMuted = Color(0xFF6B6B63), textFaint = Color(0xFF9A9A90), dark = false
)
val DarkSk = SkColors(
    primary = Color(0xFF22A06B), primaryHover = Color(0xFF2BB97C), primarySoft = Color(0xFF0F2A1E),
    cash = Color(0xFF34D399), cashSoft = Color(0xFF064E3B), qris = Color(0xFFA78BFA), qrisSoft = Color(0xFF3B2A63),
    alert = Color(0xFFF87171), alertSoft = Color(0xFF4C1D1D), warn = Color(0xFFFBBF24), warnSoft = Color(0xFF4A3410),
    surface = Color(0xFF0F0F0D), surfaceAlt = Color(0xFF1A1A17), card = Color(0xFF17171A),
    border = Color(0xFF2A2A26), borderStrong = Color(0xFF3A3A34),
    text = Color(0xFFF5F5F0), textMuted = Color(0xFFA0A09A), textFaint = Color(0xFF6E6E68), dark = true
)

val LocalSk = staticCompositionLocalOf { LightSk }

object Sk {
    val c: SkColors
        @Composable @ReadOnlyComposable get() = LocalSk.current
}

private fun materialScheme(c: SkColors) = if (c.dark) darkColorScheme(
    primary = c.primary, onPrimary = Color.White, primaryContainer = c.primarySoft, onPrimaryContainer = c.text,
    secondary = c.qris, error = c.alert, background = c.surface, onBackground = c.text,
    surface = c.card, onSurface = c.text, surfaceVariant = c.surfaceAlt, onSurfaceVariant = c.textMuted,
    outline = c.border, outlineVariant = c.border, surfaceContainer = c.card, surfaceContainerHigh = c.card,
    surfaceContainerHighest = c.card, surfaceContainerLow = c.card, surfaceContainerLowest = c.card
) else lightColorScheme(
    primary = c.primary, onPrimary = Color.White, primaryContainer = c.primarySoft, onPrimaryContainer = c.text,
    secondary = c.qris, error = c.alert, background = c.surface, onBackground = c.text,
    surface = c.card, onSurface = c.text, surfaceVariant = c.surfaceAlt, onSurfaceVariant = c.textMuted,
    outline = c.border, outlineVariant = c.border, surfaceContainer = c.card, surfaceContainerHigh = c.card,
    surfaceContainerHighest = c.card, surfaceContainerLow = c.card, surfaceContainerLowest = c.card
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
    val sk = if (dark) DarkSk else LightSk
    CompositionLocalProvider(LocalSk provides sk) {
        MaterialTheme(colorScheme = materialScheme(sk), typography = SkTypography, content = content)
    }
}
