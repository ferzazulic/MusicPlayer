package com.ferzazulic.carmusic.ui.theme

import android.graphics.Typeface
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.ferzazulic.carmusic.settings.AnimationLevel

/** Theme after the user's accent override and animated colour transitions are applied. */
data class ResolvedTheme(
    val base: CarTheme,
    val accent: Color,
    val bgTop: Color,
    val bgBottom: Color,
    val surface: Color,
    val surfaceHigh: Color,
    val outline: Color,
    val text: Color,
    val textMuted: Color,
    val accentSecondary: Color,
    val onAccent: Color,
    val animations: AnimationLevel,
) {
    val corner get() = base.corner
    fun label(s: String) = if (base.uppercaseLabels) s.uppercase() else s
    val animate get() = animations != AnimationLevel.OFF
    val rich get() = animations == AnimationLevel.FULL
    fun duration(ms: Int) = when (animations) {
        AnimationLevel.FULL -> ms
        AnimationLevel.REDUCED -> ms / 2
        AnimationLevel.OFF -> 0
    }
}

val LocalCarTheme = staticCompositionLocalOf<ResolvedTheme> { error("No theme") }

@Composable
fun CarMusicTheme(theme: CarTheme, customAccent: Color?, animations: AnimationLevel, content: @Composable () -> Unit) {
    val spec = tween<Color>(if (animations == AnimationLevel.OFF) 0 else 700)
    val accent by animateColorAsState(customAccent ?: theme.accent, spec, label = "accent")
    val bgTop by animateColorAsState(theme.bgTop, spec, label = "bgTop")
    val bgBottom by animateColorAsState(theme.bgBottom, spec, label = "bgBottom")
    val surface by animateColorAsState(theme.surface, spec, label = "surface")
    val surfaceHigh by animateColorAsState(theme.surfaceHigh, spec, label = "surfaceHigh")
    val outline by animateColorAsState(theme.outline, spec, label = "outline")
    val text by animateColorAsState(theme.text, spec, label = "text")
    val muted by animateColorAsState(theme.textMuted, spec, label = "muted")
    val secondary by animateColorAsState(theme.accentSecondary, spec, label = "secondary")
    val onAccent = if (accent.luminance() > 0.55f) Color.Black else Color.White

    val resolved = ResolvedTheme(theme, accent, bgTop, bgBottom, surface, surfaceHigh, outline, text, muted, secondary, onAccent, animations)

    val family = remember(theme.font) {
        when (theme.font) {
            FontStyle.CONDENSED -> FontFamily(Typeface.create("sans-serif-condensed", Typeface.NORMAL))
            FontStyle.LIGHT -> FontFamily.SansSerif
            FontStyle.DEFAULT -> FontFamily.Default
        }
    }
    val titleWeight = if (theme.font == FontStyle.LIGHT) FontWeight.Light else FontWeight.SemiBold
    val typography = remember(family, titleWeight) {
        Typography(
            displaySmall = TextStyle(fontFamily = family, fontWeight = titleWeight, fontSize = 40.sp, lineHeight = 46.sp),
            headlineMedium = TextStyle(fontFamily = family, fontWeight = titleWeight, fontSize = 30.sp, lineHeight = 36.sp),
            headlineSmall = TextStyle(fontFamily = family, fontWeight = titleWeight, fontSize = 24.sp, lineHeight = 30.sp),
            titleLarge = TextStyle(fontFamily = family, fontWeight = FontWeight.Medium, fontSize = 21.sp, lineHeight = 27.sp),
            titleMedium = TextStyle(fontFamily = family, fontWeight = FontWeight.Medium, fontSize = 18.sp, lineHeight = 24.sp),
            bodyLarge = TextStyle(fontFamily = family, fontSize = 18.sp, lineHeight = 24.sp),
            bodyMedium = TextStyle(fontFamily = family, fontSize = 16.sp, lineHeight = 22.sp),
            labelLarge = TextStyle(fontFamily = family, fontWeight = FontWeight.Medium, fontSize = 16.sp, letterSpacing = 0.6.sp),
            labelMedium = TextStyle(fontFamily = family, fontWeight = FontWeight.Medium, fontSize = 14.sp, letterSpacing = 0.8.sp),
        )
    }
    val scheme = darkColorScheme(
        primary = accent, onPrimary = onAccent, secondary = secondary,
        background = bgBottom, onBackground = text,
        surface = surface, onSurface = text, surfaceVariant = surfaceHigh, onSurfaceVariant = muted,
        outline = outline, surfaceContainer = surface, surfaceContainerHigh = surfaceHigh,
        surfaceContainerHighest = surfaceHigh, secondaryContainer = surfaceHigh, onSecondaryContainer = text,
    )
    CompositionLocalProvider(LocalCarTheme provides resolved) {
        MaterialTheme(colorScheme = scheme, typography = typography, content = content)
    }
}
