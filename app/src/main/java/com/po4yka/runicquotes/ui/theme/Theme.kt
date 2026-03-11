package com.po4yka.runicquotes.ui.theme

import android.app.Activity
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val FoundationLightColorScheme = lightColorScheme(
    primary = FoundationLightPrimary,
    onPrimary = FoundationLightOnPrimary,
    primaryContainer = FoundationLightPrimaryContainer,
    onPrimaryContainer = FoundationLightOnPrimaryContainer,
    secondary = FoundationLightSecondary,
    onSecondary = FoundationLightOnSecondary,
    secondaryContainer = FoundationLightSecondaryContainer,
    onSecondaryContainer = FoundationLightOnSecondaryContainer,
    tertiary = FoundationLightTertiary,
    onTertiary = FoundationLightOnTertiary,
    tertiaryContainer = FoundationLightTertiaryContainer,
    onTertiaryContainer = FoundationLightOnTertiaryContainer,
    error = FoundationLightError,
    onError = FoundationLightOnError,
    errorContainer = FoundationLightErrorContainer,
    onErrorContainer = FoundationLightOnErrorContainer,
    background = FoundationLightBackground,
    onBackground = FoundationLightOnBackground,
    surface = FoundationLightSurface,
    onSurface = FoundationLightOnSurface,
    surfaceVariant = FoundationLightSurfaceVariant,
    onSurfaceVariant = FoundationLightOnSurfaceVariant,
    outline = FoundationLightOutline,
    outlineVariant = FoundationLightOutlineVariant,
    inverseSurface = FoundationLightInverseSurface,
    inverseOnSurface = FoundationLightInverseOnSurface,
    inversePrimary = FoundationLightInversePrimary,
    surfaceDim = FoundationLightSurfaceDim,
    surfaceBright = FoundationLightSurfaceBright,
    surfaceContainerLowest = FoundationLightSurfaceContainerLowest,
    surfaceContainerLow = FoundationLightSurfaceContainerLow,
    surfaceContainer = FoundationLightSurfaceContainer,
    surfaceContainerHigh = FoundationLightSurfaceContainerHigh,
    surfaceContainerHighest = FoundationLightSurfaceContainerHighest,
    scrim = Color(0x99000000)
)

private val FoundationDarkColorScheme = darkColorScheme(
    primary = FoundationDarkPrimary,
    onPrimary = FoundationDarkOnPrimary,
    primaryContainer = FoundationDarkPrimaryContainer,
    onPrimaryContainer = FoundationDarkOnPrimaryContainer,
    secondary = FoundationDarkSecondary,
    onSecondary = FoundationDarkOnSecondary,
    secondaryContainer = FoundationDarkSecondaryContainer,
    onSecondaryContainer = FoundationDarkOnSecondaryContainer,
    tertiary = FoundationDarkTertiary,
    onTertiary = FoundationDarkOnTertiary,
    tertiaryContainer = FoundationDarkTertiaryContainer,
    onTertiaryContainer = FoundationDarkOnTertiaryContainer,
    error = FoundationDarkError,
    onError = FoundationDarkOnError,
    errorContainer = FoundationDarkErrorContainer,
    onErrorContainer = FoundationDarkOnErrorContainer,
    background = FoundationDarkBackground,
    onBackground = FoundationDarkOnBackground,
    surface = FoundationDarkSurface,
    onSurface = FoundationDarkOnSurface,
    surfaceVariant = FoundationDarkSurfaceVariant,
    onSurfaceVariant = FoundationDarkOnSurfaceVariant,
    outline = FoundationDarkOutline,
    outlineVariant = FoundationDarkOutlineVariant,
    inverseSurface = FoundationDarkInverseSurface,
    inverseOnSurface = FoundationDarkInverseOnSurface,
    inversePrimary = FoundationDarkInversePrimary,
    surfaceDim = FoundationDarkSurfaceDim,
    surfaceBright = FoundationDarkSurfaceBright,
    surfaceContainerLowest = FoundationDarkSurfaceContainerLowest,
    surfaceContainerLow = FoundationDarkSurfaceContainerLow,
    surfaceContainer = FoundationDarkSurfaceContainer,
    surfaceContainerHigh = FoundationDarkSurfaceContainerHigh,
    surfaceContainerHighest = FoundationDarkSurfaceContainerHighest,
    scrim = Color(0xCC000000)
)

private val HighContrastLightColorScheme = lightColorScheme(
    primary = HighContrastBlack,
    onPrimary = HighContrastWhite,
    primaryContainer = HighContrastWhite,
    onPrimaryContainer = HighContrastBlack,
    secondary = HighContrastBlack,
    onSecondary = HighContrastWhite,
    secondaryContainer = HighContrastWhite,
    onSecondaryContainer = HighContrastBlack,
    tertiary = HighContrastBlack,
    onTertiary = HighContrastWhite,
    tertiaryContainer = HighContrastWhite,
    onTertiaryContainer = HighContrastBlack,
    background = HighContrastWhite,
    onBackground = HighContrastBlack,
    surface = HighContrastWhite,
    onSurface = HighContrastBlack,
    surfaceVariant = Color(0xFFF2F2F2),
    onSurfaceVariant = HighContrastBlack,
    outline = HighContrastBlack,
    outlineVariant = Color(0xFF444444),
    inverseSurface = HighContrastBlack,
    inverseOnSurface = HighContrastWhite,
    inversePrimary = HighContrastWhite,
    surfaceDim = Color(0xFFE2E2E2),
    surfaceBright = HighContrastWhite,
    surfaceContainerLowest = HighContrastWhite,
    surfaceContainerLow = Color(0xFFF7F7F7),
    surfaceContainer = Color(0xFFF0F0F0),
    surfaceContainerHigh = Color(0xFFE9E9E9),
    surfaceContainerHighest = Color(0xFFE0E0E0),
    scrim = Color(0xCC000000)
)

private val HighContrastDarkColorScheme = darkColorScheme(
    primary = HighContrastWhite,
    onPrimary = HighContrastBlack,
    primaryContainer = HighContrastBlack,
    onPrimaryContainer = HighContrastWhite,
    secondary = HighContrastWhite,
    onSecondary = HighContrastBlack,
    secondaryContainer = HighContrastBlack,
    onSecondaryContainer = HighContrastWhite,
    tertiary = HighContrastWhite,
    onTertiary = HighContrastBlack,
    tertiaryContainer = HighContrastBlack,
    onTertiaryContainer = HighContrastWhite,
    background = HighContrastBlack,
    onBackground = HighContrastWhite,
    surface = HighContrastBlack,
    onSurface = HighContrastWhite,
    surfaceVariant = Color(0xFF1C1C1C),
    onSurfaceVariant = HighContrastWhite,
    outline = HighContrastWhite,
    outlineVariant = Color(0xFFAAAAAA),
    inverseSurface = HighContrastWhite,
    inverseOnSurface = HighContrastBlack,
    inversePrimary = HighContrastBlack,
    surfaceDim = HighContrastBlack,
    surfaceBright = Color(0xFF353535),
    surfaceContainerLowest = HighContrastBlack,
    surfaceContainerLow = Color(0xFF101010),
    surfaceContainer = Color(0xFF161616),
    surfaceContainerHigh = Color(0xFF1E1E1E),
    surfaceContainerHighest = Color(0xFF272727),
    scrim = Color(0xCC000000)
)

val LocalRunicFontScale = staticCompositionLocalOf { 1.0f }
val LocalReduceMotion = staticCompositionLocalOf { false }

internal fun foundationRunicColorScheme(darkTheme: Boolean): ColorScheme {
    return if (darkTheme) FoundationDarkColorScheme else FoundationLightColorScheme
}

internal fun highContrastRunicColorScheme(darkTheme: Boolean): ColorScheme {
    return if (darkTheme) HighContrastDarkColorScheme else HighContrastLightColorScheme
}

internal fun runicColorScheme(
    darkTheme: Boolean,
    @Suppress("UNUSED_PARAMETER")
    themePack: String,
    highContrast: Boolean,
    dynamicColorEnabled: Boolean,
    context: android.content.Context
): ColorScheme {
    if (highContrast) {
        return highContrastRunicColorScheme(darkTheme)
    }

    val foundationColorScheme = foundationRunicColorScheme(darkTheme)

    if (dynamicColorEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val dynamicColorScheme = dynamicColorSchemeForContext(
            context = context,
            darkTheme = darkTheme
        )
        return harmonizeDynamicWithFoundation(dynamicColorScheme, foundationColorScheme)
    }
    return foundationColorScheme
}

@RequiresApi(Build.VERSION_CODES.S)
private fun dynamicColorSchemeForContext(
    context: android.content.Context,
    darkTheme: Boolean
): ColorScheme {
    return if (darkTheme) {
        dynamicDarkColorScheme(context)
    } else {
        dynamicLightColorScheme(context)
    }
}

private fun harmonizeDynamicWithFoundation(dynamic: ColorScheme, foundation: ColorScheme): ColorScheme {
    fun blend(dynamicColor: Color, packColor: Color): Color {
        return lerp(dynamicColor, packColor, 0.18f)
    }

    return foundation.copy(
        primary = blend(dynamic.primary, foundation.primary),
        onPrimary = blend(dynamic.onPrimary, foundation.onPrimary),
        primaryContainer = blend(dynamic.primaryContainer, foundation.primaryContainer),
        onPrimaryContainer = blend(dynamic.onPrimaryContainer, foundation.onPrimaryContainer),
        secondary = blend(dynamic.secondary, foundation.secondary),
        onSecondary = blend(dynamic.onSecondary, foundation.onSecondary),
        secondaryContainer = blend(dynamic.secondaryContainer, foundation.secondaryContainer),
        onSecondaryContainer = blend(dynamic.onSecondaryContainer, foundation.onSecondaryContainer),
        tertiary = blend(dynamic.tertiary, foundation.tertiary),
        onTertiary = blend(dynamic.onTertiary, foundation.onTertiary),
        tertiaryContainer = blend(dynamic.tertiaryContainer, foundation.tertiaryContainer),
        onTertiaryContainer = blend(dynamic.onTertiaryContainer, foundation.onTertiaryContainer),
        inversePrimary = blend(dynamic.inversePrimary, foundation.inversePrimary)
    )
}

/**
 * Runic Quotes theme using the Runatal foundation palette from Figma.
 *
 * @param darkTheme Whether to use dark theme
 * @param dynamicColorEnabled Whether to gently harmonize accent roles with Material You
 * @param themePack Legacy preference kept for compatibility with existing onboarding/settings data
 * @param runicFontScale Global scale multiplier for runic glyphs
 * @param highContrast Enable high-contrast palette override
 * @param reducedMotion Disable non-essential motion
 * @param content The composable content to apply the theme to
 */
@Composable
fun RunicQuotesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColorEnabled: Boolean = false,
    themePack: String = "stone",
    runicFontScale: Float = 1.0f,
    highContrast: Boolean = false,
    reducedMotion: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = runicColorScheme(
        darkTheme = darkTheme,
        themePack = themePack,
        highContrast = highContrast,
        dynamicColorEnabled = dynamicColorEnabled,
        context = context
    )
    val typography = typographyForThemePack(themePack)
    val expressiveTypography = expressiveTypographyForThemePack(themePack, typography)
    val shapeTokens = runicShapeTokens()
    val materialShapes = runicMaterialShapes(shapeTokens)

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
        LocalReduceMotion provides reducedMotion,
        LocalRunicShapeTokens provides shapeTokens,
        LocalRunicElevationTokens provides runicElevationTokens(),
        LocalRunicMotionTokens provides runicMotionTokens(),
        LocalRunicExpressiveType provides expressiveTypography
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = materialShapes,
            content = content
        )
    }
}
