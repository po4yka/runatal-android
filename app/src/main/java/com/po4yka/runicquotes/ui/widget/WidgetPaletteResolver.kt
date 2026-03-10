package com.po4yka.runicquotes.ui.widget

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import com.po4yka.runicquotes.data.preferences.UserPreferences

internal fun resolveWidgetPalette(
    context: Context,
    preferences: UserPreferences
): WidgetPalette {
    val darkTheme = isDarkTheme(context, preferences)

    if (preferences.highContrastEnabled) {
        return highContrastPalette(darkTheme)
    }

    val foundationPalette = foundationPalette(darkTheme)

    if (preferences.dynamicColorEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        return harmonizeWithDynamic(context, foundationPalette, darkTheme)
    }

    return foundationPalette
}

private fun highContrastPalette(darkTheme: Boolean): WidgetPalette {
    return if (darkTheme) {
        WidgetPalette(
            background = Color.BLACK,
            surface = Color.BLACK,
            onBackground = Color.WHITE,
            onSurface = Color.WHITE,
            primary = Color.WHITE,
            primaryContainer = Color.BLACK,
            onPrimaryContainer = Color.WHITE,
            error = Color.parseColor("#FF6B6B"),
            runicText = Color.WHITE
        )
    } else {
        WidgetPalette(
            background = Color.WHITE,
            surface = Color.WHITE,
            onBackground = Color.BLACK,
            onSurface = Color.BLACK,
            primary = Color.BLACK,
            primaryContainer = Color.WHITE,
            onPrimaryContainer = Color.BLACK,
            error = Color.parseColor("#B00020"),
            runicText = Color.BLACK
        )
    }
}

private fun foundationPalette(darkTheme: Boolean): WidgetPalette {
    return if (darkTheme) {
        WidgetPalette(
            background = Color.parseColor("#0E1113"),
            surface = Color.parseColor("#14171A"),
            onBackground = Color.parseColor("#E0E2E5"),
            onSurface = Color.parseColor("#E0E2E5"),
            primary = Color.parseColor("#C1C7D0"),
            primaryContainer = Color.parseColor("#50575F"),
            onPrimaryContainer = Color.parseColor("#DEE4EC"),
            error = Color.parseColor("#F0B8BF"),
            runicText = Color.parseColor("#E7EAEE")
        )
    } else {
        WidgetPalette(
            background = Color.parseColor("#F4F6F8"),
            surface = Color.parseColor("#EBEDF0"),
            onBackground = Color.parseColor("#191C1E"),
            onSurface = Color.parseColor("#191C1E"),
            primary = Color.parseColor("#68707A"),
            primaryContainer = Color.parseColor("#D8DEE6"),
            onPrimaryContainer = Color.parseColor("#1E2328"),
            error = Color.parseColor("#96404A"),
            runicText = Color.parseColor("#1E2328")
        )
    }
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

@RequiresApi(Build.VERSION_CODES.S)
private fun harmonizeWithDynamic(
    context: Context,
    foundationPalette: WidgetPalette,
    darkTheme: Boolean
): WidgetPalette {
    val dynamicScheme = if (darkTheme) {
        dynamicDarkColorScheme(context)
    } else {
        dynamicLightColorScheme(context)
    }

    fun blend(dynamic: Int, foundation: Int): Int = ColorUtils.blendARGB(dynamic, foundation, 0.18f)

    return foundationPalette.copy(
        primary = blend(dynamicScheme.primary.toArgb(), foundationPalette.primary),
        primaryContainer = blend(dynamicScheme.primaryContainer.toArgb(), foundationPalette.primaryContainer),
        onPrimaryContainer = blend(
            dynamicScheme.onPrimaryContainer.toArgb(),
            foundationPalette.onPrimaryContainer
        ),
        error = blend(dynamicScheme.error.toArgb(), foundationPalette.error),
        runicText = blend(dynamicScheme.primary.toArgb(), foundationPalette.runicText)
    )
}
