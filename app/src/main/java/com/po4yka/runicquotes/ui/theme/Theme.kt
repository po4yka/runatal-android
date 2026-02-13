package com.po4yka.runicquotes.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Stone pack: cool mineral tones.
 */
private val StoneLightColorScheme = lightColorScheme(
    primary = Color(0xFF2F3842),
    onPrimary = Color(0xFFF5F7FA),
    primaryContainer = Color(0xFFDCE3EA),
    onPrimaryContainer = Color(0xFF1B2127),
    secondary = Color(0xFF4A5A6A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDCE5EF),
    onSecondaryContainer = Color(0xFF1A2430),
    tertiary = Color(0xFF5A6167),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFDEE3E7),
    onTertiaryContainer = Color(0xFF191E22),
    background = Color(0xFFF3F5F7),
    onBackground = Color(0xFF171C21),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF171C21),
    surfaceVariant = Color(0xFFDFE5EA),
    onSurfaceVariant = Color(0xFF41484F),
    outline = Color(0xFF717880),
    outlineVariant = Color(0xFFC1C7CD),
    inverseSurface = Color(0xFF2C3137),
    inverseOnSurface = Color(0xFFEFF1F4),
    inversePrimary = Color(0xFFB8C8D9),
    scrim = Color(0x99000000)
)

private val StoneDarkColorScheme = darkColorScheme(
    primary = Color(0xFFB8C8D9),
    onPrimary = Color(0xFF203243),
    primaryContainer = Color(0xFF364757),
    onPrimaryContainer = Color(0xFFD6E4F2),
    secondary = Color(0xFFB4C9DE),
    onSecondary = Color(0xFF203041),
    secondaryContainer = Color(0xFF354759),
    onSecondaryContainer = Color(0xFFD1E5FC),
    tertiary = Color(0xFFC1C7CE),
    onTertiary = Color(0xFF2A3137),
    tertiaryContainer = Color(0xFF434A50),
    onTertiaryContainer = Color(0xFFDDE3E9),
    background = Color(0xFF101419),
    onBackground = Color(0xFFE3E8EE),
    surface = Color(0xFF171C21),
    onSurface = Color(0xFFE3E8EE),
    surfaceVariant = Color(0xFF40474E),
    onSurfaceVariant = Color(0xFFC0C7CF),
    outline = Color(0xFF8A9299),
    outlineVariant = Color(0xFF40474E),
    inverseSurface = Color(0xFFE3E8EE),
    inverseOnSurface = Color(0xFF2C3137),
    inversePrimary = Color(0xFF4E6071),
    scrim = Color(0xCC000000)
)

/**
 * Parchment pack: warm ink-on-paper tones.
 */
private val ParchmentLightColorScheme = lightColorScheme(
    primary = Color(0xFF6B4E2F),
    onPrimary = Color(0xFFFFF8F1),
    primaryContainer = Color(0xFFF5DFC6),
    onPrimaryContainer = Color(0xFF2A1B0B),
    secondary = Color(0xFF79563A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF0DFC8),
    onSecondaryContainer = Color(0xFF2B1C0D),
    tertiary = Color(0xFF7B6A45),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF2E5BF),
    onTertiaryContainer = Color(0xFF261F07),
    background = Color(0xFFFAF2E4),
    onBackground = Color(0xFF2A2116),
    surface = Color(0xFFFFFAF2),
    onSurface = Color(0xFF2A2116),
    surfaceVariant = Color(0xFFEBDDC8),
    onSurfaceVariant = Color(0xFF534434),
    outline = Color(0xFF86725D),
    outlineVariant = Color(0xFFD2C2AC),
    inverseSurface = Color(0xFF2F261B),
    inverseOnSurface = Color(0xFFF8EFE2),
    inversePrimary = Color(0xFFD8B892),
    scrim = Color(0x99000000)
)

private val ParchmentDarkColorScheme = darkColorScheme(
    primary = Color(0xFFD8B892),
    onPrimary = Color(0xFF3B2813),
    primaryContainer = Color(0xFF523B24),
    onPrimaryContainer = Color(0xFFF5DFC6),
    secondary = Color(0xFFDFBE99),
    onSecondary = Color(0xFF3A2916),
    secondaryContainer = Color(0xFF523C28),
    onSecondaryContainer = Color(0xFFF9DFC0),
    tertiary = Color(0xFFE0CB9E),
    onTertiary = Color(0xFF3C2F11),
    tertiaryContainer = Color(0xFF554221),
    onTertiaryContainer = Color(0xFFF8E4B8),
    background = Color(0xFF1F1810),
    onBackground = Color(0xFFF2E6D4),
    surface = Color(0xFF282016),
    onSurface = Color(0xFFF2E6D4),
    surfaceVariant = Color(0xFF534434),
    onSurfaceVariant = Color(0xFFD6C4AD),
    outline = Color(0xFFA08B74),
    outlineVariant = Color(0xFF534434),
    inverseSurface = Color(0xFFF2E6D4),
    inverseOnSurface = Color(0xFF352B1F),
    inversePrimary = Color(0xFF6B4E2F),
    scrim = Color(0xCC000000)
)

/**
 * Night Ink pack: deep blue-black with crisp highlights.
 */
private val NightInkLightColorScheme = lightColorScheme(
    primary = Color(0xFF103349),
    onPrimary = Color(0xFFEAF8FF),
    primaryContainer = Color(0xFFCBE9FA),
    onPrimaryContainer = Color(0xFF001F30),
    secondary = Color(0xFF2D4A63),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD5E5F6),
    onSecondaryContainer = Color(0xFF12283D),
    tertiary = Color(0xFF3A546E),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD6E8FA),
    onTertiaryContainer = Color(0xFF1A2C3F),
    background = Color(0xFFF5F9FF),
    onBackground = Color(0xFF0E1A24),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0E1A24),
    surfaceVariant = Color(0xFFDCE5F0),
    onSurfaceVariant = Color(0xFF3E4A56),
    outline = Color(0xFF6F7B87),
    outlineVariant = Color(0xFFBFC9D3),
    inverseSurface = Color(0xFF1D2933),
    inverseOnSurface = Color(0xFFECF2F8),
    inversePrimary = Color(0xFF93C8E5),
    scrim = Color(0x99000000)
)

private val NightInkDarkColorScheme = darkColorScheme(
    primary = Color(0xFF93C8E5),
    onPrimary = Color(0xFF00344D),
    primaryContainer = Color(0xFF1B4A64),
    onPrimaryContainer = Color(0xFFCBE9FA),
    secondary = Color(0xFFA8C8E3),
    onSecondary = Color(0xFF123249),
    secondaryContainer = Color(0xFF294960),
    onSecondaryContainer = Color(0xFFD5E5F6),
    tertiary = Color(0xFFB3CBE5),
    onTertiary = Color(0xFF1D3449),
    tertiaryContainer = Color(0xFF334C62),
    onTertiaryContainer = Color(0xFFD6E8FA),
    background = Color(0xFF090F15),
    onBackground = Color(0xFFDDE6EF),
    surface = Color(0xFF121B24),
    onSurface = Color(0xFFDDE6EF),
    surfaceVariant = Color(0xFF3E4A56),
    onSurfaceVariant = Color(0xFFBEC9D5),
    outline = Color(0xFF8894A0),
    outlineVariant = Color(0xFF3E4A56),
    inverseSurface = Color(0xFFDDE6EF),
    inverseOnSurface = Color(0xFF24303B),
    inversePrimary = Color(0xFF1E5777),
    scrim = Color(0xCC000000)
)

private val HighContrastLightColorScheme = lightColorScheme(
    primary = Color.Black,
    onPrimary = Color.White,
    primaryContainer = Color.White,
    onPrimaryContainer = Color.Black,
    secondary = Color.Black,
    onSecondary = Color.White,
    secondaryContainer = Color.White,
    onSecondaryContainer = Color.Black,
    tertiary = Color.Black,
    onTertiary = Color.White,
    tertiaryContainer = Color.White,
    onTertiaryContainer = Color.Black,
    background = Color.White,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFF2F2F2),
    onSurfaceVariant = Color.Black,
    outline = Color.Black,
    outlineVariant = Color(0xFF444444),
    inverseSurface = Color.Black,
    inverseOnSurface = Color.White,
    inversePrimary = Color.White,
    scrim = Color(0xCC000000)
)

private val HighContrastDarkColorScheme = darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    primaryContainer = Color.Black,
    onPrimaryContainer = Color.White,
    secondary = Color.White,
    onSecondary = Color.Black,
    secondaryContainer = Color.Black,
    onSecondaryContainer = Color.White,
    tertiary = Color.White,
    onTertiary = Color.Black,
    tertiaryContainer = Color.Black,
    onTertiaryContainer = Color.White,
    background = Color.Black,
    onBackground = Color.White,
    surface = Color.Black,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF1C1C1C),
    onSurfaceVariant = Color.White,
    outline = Color.White,
    outlineVariant = Color(0xFFAAAAAA),
    inverseSurface = Color.White,
    inverseOnSurface = Color.Black,
    inversePrimary = Color.Black,
    scrim = Color(0xCC000000)
)

val LocalRunicFontScale = staticCompositionLocalOf { 1.0f }
val LocalReduceMotion = staticCompositionLocalOf { false }

private fun resolveColorScheme(
    darkTheme: Boolean,
    themePack: String,
    highContrast: Boolean
): ColorScheme {
    if (highContrast) {
        return if (darkTheme) HighContrastDarkColorScheme else HighContrastLightColorScheme
    }

    return when (themePack) {
        "parchment" -> if (darkTheme) ParchmentDarkColorScheme else ParchmentLightColorScheme
        "night_ink" -> if (darkTheme) NightInkDarkColorScheme else NightInkLightColorScheme
        else -> if (darkTheme) StoneDarkColorScheme else StoneLightColorScheme
    }
}

/**
 * Runic Quotes theme using pack-based palettes and typography.
 *
 * @param darkTheme Whether to use dark theme
 * @param themePack Visual palette and typography pack
 * @param runicFontScale Global scale multiplier for runic glyphs
 * @param highContrast Enable high-contrast palette override
 * @param reducedMotion Disable non-essential motion
 * @param content The composable content to apply the theme to
 */
@Composable
fun RunicQuotesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themePack: String = "stone",
    runicFontScale: Float = 1.0f,
    highContrast: Boolean = false,
    reducedMotion: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = resolveColorScheme(
        darkTheme = darkTheme,
        themePack = themePack,
        highContrast = highContrast
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Enable edge-to-edge display
            WindowCompat.setDecorFitsSystemWindows(window, false)
            // Set status bar appearance
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalRunicFontScale provides runicFontScale,
        LocalReduceMotion provides reducedMotion
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typographyForThemePack(themePack),
            content = content
        )
    }
}
