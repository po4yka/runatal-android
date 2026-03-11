package com.po4yka.runicquotes.ui.widget

import android.content.Context
import android.content.res.Configuration
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.toArgb
import com.po4yka.runicquotes.data.preferences.UserPreferences
import com.po4yka.runicquotes.ui.theme.runicColorScheme

internal fun resolveWidgetPalette(
    context: Context,
    preferences: UserPreferences
): WidgetPalette {
    val darkTheme = isDarkTheme(context, preferences)
    return widgetPaletteFromColorScheme(
        runicColorScheme(
            darkTheme = darkTheme,
            themePack = "stone",
            highContrast = preferences.highContrastEnabled,
            dynamicColorEnabled = preferences.dynamicColorEnabled,
            context = context
        )
    )
}

private fun isDarkTheme(context: Context, preferences: UserPreferences): Boolean {
    return when (preferences.themeMode) {
        "dark" -> true
        "light" -> false
        else -> {
            val nightModeFlags = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            nightModeFlags == Configuration.UI_MODE_NIGHT_YES
        }
    }
}

internal fun widgetPaletteFromColorScheme(colorScheme: ColorScheme): WidgetPalette {
    return WidgetPalette(
        background = colorScheme.background.toArgb(),
        surface = colorScheme.surface.toArgb(),
        surfaceMuted = colorScheme.surfaceContainerHighest.toArgb(),
        onBackground = colorScheme.onBackground.toArgb(),
        onSurface = colorScheme.onSurface.toArgb(),
        onSurfaceVariant = colorScheme.onSurfaceVariant.toArgb(),
        outline = colorScheme.outlineVariant.toArgb(),
        primary = colorScheme.primary.toArgb(),
        primaryContainer = colorScheme.primaryContainer.toArgb(),
        onPrimaryContainer = colorScheme.onPrimaryContainer.toArgb(),
        error = colorScheme.error.toArgb(),
        runicText = colorScheme.onSurface.toArgb()
    )
}
