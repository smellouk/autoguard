package com.smellouk.autoguard.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset

/** All design tokens for one theme. */
@Immutable
data class AutoGuardColors(
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val line: Color,
    val line2: Color,
    val text: Color,
    val textDim: Color,
    val textFaint: Color,
    val green: Color,   // per-theme accent text/dots/track
    val blue: Color,
    val amber: Color,
    val red: Color,
    val onGreen: Color,
    val onAmber: Color,
    val isLight: Boolean,
) {
    /** Diagonal (~150°) green gradient for CTAs, hero shield, active segment — vivid in both themes. */
    val greenGradient: Brush
        get() = Brush.linearGradient(
            colors = listOf(GreenGradStart, GreenGradEnd),
            start = Offset.Zero,
            end = Offset.Infinite, // corner-to-corner diagonal, not a flat vertical sweep
        )

    fun wash(accent: Accent): Color = when (accent) {
        Accent.GREEN -> GreenVivid.copy(alpha = 0.13f)
        Accent.BLUE -> BlueVivid.copy(alpha = 0.12f)
        Accent.AMBER -> AmberVivid.copy(alpha = 0.12f)
        Accent.RED -> RedVivid.copy(alpha = 0.12f)
    }

    fun washBorder(accent: Accent): Color = when (accent) {
        Accent.GREEN -> GreenVivid.copy(alpha = 0.28f)
        Accent.BLUE -> BlueVivid.copy(alpha = 0.28f)
        Accent.AMBER -> AmberVivid.copy(alpha = 0.28f)
        Accent.RED -> RedVivid.copy(alpha = 0.28f)
    }

    fun accentText(accent: Accent): Color = when (accent) {
        Accent.GREEN -> green
        Accent.BLUE -> blue
        Accent.AMBER -> amber
        Accent.RED -> red
    }
}

enum class Accent { GREEN, BLUE, AMBER, RED }

/* Vivid accents are constant across themes (used for washes, gradient, glows). */
val GreenVivid = Color(0xFF35E3A4)
val BlueVivid = Color(0xFF74B6FF)
val AmberVivid = Color(0xFFFFC24B)
val RedVivid = Color(0xFFFF5D6B)
val GreenGradStart = Color(0xFF3FF0B0)
val GreenGradEnd = Color(0xFF16A06F)

private val DarkColors = AutoGuardColors(
    bg = Color(0xFF0A0D0C),
    surface = Color(0xFF141917),
    surface2 = Color(0xFF1D2421),
    line = Color(0x14FFFFFF),       // white .08
    line2 = Color(0x26FFFFFF),      // white .15
    text = Color(0xFFEDF2EF),
    textDim = Color(0xFF8C9692),
    textFaint = Color(0xFF5D6864),
    green = GreenVivid,
    blue = BlueVivid,
    amber = AmberVivid,
    red = RedVivid,
    onGreen = Color(0xFF07140F),
    onAmber = Color(0xFF1C1502),
    isLight = false,
)

private val LightColors = AutoGuardColors(
    bg = Color(0xFFEEF1EF),
    surface = Color(0xFFFFFFFF),
    surface2 = Color(0xFFE6EAE7),
    line = Color(0x14000000),       // black .08
    line2 = Color(0x24000000),      // black .14
    text = Color(0xFF14211C),
    textDim = Color(0xFF5A6661),
    textFaint = Color(0xFF93A09B),
    green = Color(0xFF0B8E56),
    blue = Color(0xFF2C6CD4),
    amber = Color(0xFFB0770A),
    red = Color(0xFFD63A47),
    onGreen = Color(0xFF07140F),
    onAmber = Color(0xFF1C1502),
    isLight = true,
)

val LocalAGColors = staticCompositionLocalOf { DarkColors }

/** Convenient accessor: `AG.colors`. */
object AG {
    val colors: AutoGuardColors
        @Composable get() = LocalAGColors.current
}

@Composable
fun AutoGuardTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors

    // A minimal Material scheme so ripples / Switch defaults pick sane colours.
    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = colors.green, onPrimary = colors.onGreen,
            background = colors.bg, onBackground = colors.text,
            surface = colors.surface, onSurface = colors.text,
        )
    } else {
        lightColorScheme(
            primary = colors.green, onPrimary = colors.onGreen,
            background = colors.bg, onBackground = colors.text,
            surface = colors.surface, onSurface = colors.text,
        )
    }

    CompositionLocalProvider(LocalAGColors provides colors) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
