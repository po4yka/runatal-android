package com.runicquotes.android.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Dark color scheme using monochromatic palette.
 * Material 3 Expressive with strictly black & white colors.
 */
private val DarkColorScheme = darkColorScheme(
    primary = White,
    onPrimary = Black,
    primaryContainer = Gray800,
    onPrimaryContainer = Gray100,

    secondary = Gray300,
    onSecondary = Black,
    secondaryContainer = Gray700,
    onSecondaryContainer = Gray200,

    tertiary = Gray400,
    onTertiary = Black,
    tertiaryContainer = Gray700,
    onTertiaryContainer = Gray200,

    error = Gray300,
    onError = Black,
    errorContainer = Gray800,
    onErrorContainer = Gray200,

    background = Black,
    onBackground = White,

    surface = Gray900,
    onSurface = White,
    surfaceVariant = Gray800,
    onSurfaceVariant = Gray300,

    outline = Gray600,
    outlineVariant = Gray800,

    scrim = BlackAlpha80,

    inverseSurface = Gray100,
    inverseOnSurface = Gray900,
    inversePrimary = Gray700
)

/**
 * Light color scheme using monochromatic palette.
 * Material 3 Expressive with strictly black & white colors.
 */
private val LightColorScheme = lightColorScheme(
    primary = Black,
    onPrimary = White,
    primaryContainer = Gray200,
    onPrimaryContainer = Gray900,

    secondary = Gray700,
    onSecondary = White,
    secondaryContainer = Gray300,
    onSecondaryContainer = Gray900,

    tertiary = Gray600,
    onTertiary = White,
    tertiaryContainer = Gray300,
    onTertiaryContainer = Gray900,

    error = Gray700,
    onError = White,
    errorContainer = Gray200,
    onErrorContainer = Gray900,

    background = White,
    onBackground = Black,

    surface = Gray50,
    onSurface = Black,
    surfaceVariant = Gray200,
    onSurfaceVariant = Gray700,

    outline = Gray400,
    outlineVariant = Gray300,

    scrim = BlackAlpha50,

    inverseSurface = Gray900,
    inverseOnSurface = Gray100,
    inversePrimary = Gray300
)

/**
 * Runic Quotes theme using Material 3 Expressive design language
 * with a strictly monochromatic (black & white) color scheme.
 *
 * @param darkTheme Whether to use dark theme
 * @param content The composable content to apply the theme to
 */
@Composable
fun RunicQuotesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = RunicTypography,
        content = content
    )
}
