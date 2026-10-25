package com.tubers.blacklight.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tubers.blacklight.domain.logic.ColorMath

/** Fixed design tokens taken from the UV Glow design file. */
object UvColors {
    val Ground = Color(0xFF07050C)
    val NavBackground = Color(0xFF0A0710)
    val Surface = Color(0xFF110D18)
    val SurfaceRaised = Color(0xFF17121F)
    val SurfaceHigh = Color(0xFF1A1424)
    val Field = Color(0xFF0E0B14)
    val Border = Color(0xFF2A2236)
    val BorderSoft = Color(0xFF221B2D)
    val Divider = Color(0xFF1E1828)
    val Track = Color(0xFF3A3048)
    val Text = Color(0xFFF4F0FA)
    val TextSoft = Color(0xFFD9D1E6)
    val TextBody = Color(0xFFCFC6DC)
    val TextSecondary = Color(0xFFA79DB8)
    val TextMuted = Color(0xFF8E84A0)
    val Placeholder = Color(0xFF7D7390)
    val Danger = Color(0xFFF87171)
    val OnDanger = Color(0xFF1A0606)
    val LockBg = Color(0xFF2A1F38)
    val LockText = Color(0xFFE2C9FF)
}

object UvSpacing {
    val xs = 4.dp
    val s = 8.dp
    val m = 12.dp
    val l = 16.dp
    val gutter = 20.dp
    val xl = 24.dp
}

object UvRadius {
    val s = 10.dp
    val m = 14.dp
    val l = 16.dp
    val xl = 18.dp
}

/** Current accent, derived from the selected light color. Changes as the user picks colors. */
@Immutable
data class Accent(
    val color: Color,
    /** Text/icon color on accent-filled buttons. */
    val onColor: Color,
    /** Accent tuned for text on the dark background. */
    val text: Color,
    val ground: Color,
) {
    companion object {
        fun from(colorArgb: Int, groundArgb: Int) = Accent(
            color = Color(colorArgb),
            onColor = Color(ColorMath.onAccent(colorArgb)),
            text = Color(ColorMath.accentText(colorArgb)),
            ground = Color(groundArgb),
        )
    }
}

val LocalAccent = staticCompositionLocalOf { Accent.from(0xFFA855F7.toInt(), 0xFF07050C.toInt()) }

/** Monospace face used for values and tags (the design uses JetBrains Mono). */
val MonoFamily: FontFamily = FontFamily.Monospace

object UvType {
    val pageTitle = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.8).sp)
    val lede = TextStyle(fontSize = 13.sp, color = UvColors.TextSecondary, lineHeight = 18.sp)
    val label = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.4.sp, color = UvColors.TextSecondary)
    val body = TextStyle(fontSize = 14.sp, lineHeight = 20.sp)
    val bodyStrong = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, lineHeight = 18.sp)
    val small = TextStyle(fontSize = 11.sp, color = UvColors.TextSecondary, lineHeight = 15.sp)
    val mono = TextStyle(fontFamily = MonoFamily, fontSize = 12.sp, fontWeight = FontWeight.Medium)
}

private val AppTypography = Typography(
    bodyLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodyMedium = TextStyle(fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
)

@Composable
fun UvGlowTheme(accent: Accent, content: @Composable () -> Unit) {
    val scheme = darkColorScheme(
        primary = accent.color,
        onPrimary = accent.onColor,
        secondary = accent.color,
        background = accent.ground,
        onBackground = UvColors.Text,
        surface = UvColors.Surface,
        onSurface = UvColors.Text,
        surfaceVariant = UvColors.SurfaceRaised,
        onSurfaceVariant = UvColors.TextSecondary,
        surfaceContainerHigh = UvColors.SurfaceRaised,
        outline = UvColors.Border,
        error = UvColors.Danger,
        inverseSurface = UvColors.Text,
        inverseOnSurface = Color(0xFF140E1C),
    )
    MaterialTheme(colorScheme = scheme, typography = AppTypography) {
        CompositionLocalProvider(LocalAccent provides accent, content = content)
    }
}
