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

    val packPalette = packPalette(
        themePack = preferences.themePack,
        darkTheme = darkTheme
    )

    if (preferences.dynamicColorEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        return harmonizeWithDynamic(context, packPalette, darkTheme)
    }

    return packPalette
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

private fun packPalette(themePack: String, darkTheme: Boolean): WidgetPalette {
    return when (themePack) {
        "parchment" -> parchmentPalette(darkTheme)
        "night_ink" -> nightInkPalette(darkTheme)
        else -> stonePalette(darkTheme)
    }
}

private fun parchmentPalette(darkTheme: Boolean): WidgetPalette {
    return if (darkTheme) {
        WidgetPalette(
            background = Color.parseColor("#1F1810"),
            surface = Color.parseColor("#282016"),
            onBackground = Color.parseColor("#F2E6D4"),
            onSurface = Color.parseColor("#F2E6D4"),
            primary = Color.parseColor("#D8B892"),
            primaryContainer = Color.parseColor("#523B24"),
            onPrimaryContainer = Color.parseColor("#F5DFC6"),
            error = Color.parseColor("#FFB4AB"),
            runicText = Color.parseColor("#FFF3DF")
        )
    } else {
        WidgetPalette(
            background = Color.parseColor("#FAF2E4"),
            surface = Color.parseColor("#FFF8EE"),
            onBackground = Color.parseColor("#2A2116"),
            onSurface = Color.parseColor("#2A2116"),
            primary = Color.parseColor("#6B4E2F"),
            primaryContainer = Color.parseColor("#F5DFC6"),
            onPrimaryContainer = Color.parseColor("#2A1B0B"),
            error = Color.parseColor("#B3261E"),
            runicText = Color.parseColor("#3A2817")
        )
    }
}

private fun nightInkPalette(darkTheme: Boolean): WidgetPalette {
    return if (darkTheme) {
        WidgetPalette(
            background = Color.parseColor("#090F15"),
            surface = Color.parseColor("#121B24"),
            onBackground = Color.parseColor("#DDE6EF"),
            onSurface = Color.parseColor("#DDE6EF"),
            primary = Color.parseColor("#93C8E5"),
            primaryContainer = Color.parseColor("#1B4A64"),
            onPrimaryContainer = Color.parseColor("#CBE9FA"),
            error = Color.parseColor("#FFB4AB"),
            runicText = Color.parseColor("#ECF7FF")
        )
    } else {
        WidgetPalette(
            background = Color.parseColor("#F5F9FF"),
            surface = Color.parseColor("#FFFFFF"),
            onBackground = Color.parseColor("#0E1A24"),
            onSurface = Color.parseColor("#0E1A24"),
            primary = Color.parseColor("#103349"),
            primaryContainer = Color.parseColor("#CBE9FA"),
            onPrimaryContainer = Color.parseColor("#001F30"),
            error = Color.parseColor("#B3261E"),
            runicText = Color.parseColor("#12344A")
        )
    }
}

private fun stonePalette(darkTheme: Boolean): WidgetPalette {
    return if (darkTheme) {
        WidgetPalette(
            background = Color.parseColor("#101419"),
            surface = Color.parseColor("#171C21"),
            onBackground = Color.parseColor("#E3E8EE"),
            onSurface = Color.parseColor("#E3E8EE"),
            primary = Color.parseColor("#B8C8D9"),
            primaryContainer = Color.parseColor("#364757"),
            onPrimaryContainer = Color.parseColor("#D6E4F2"),
            error = Color.parseColor("#FFB4AB"),
            runicText = Color.parseColor("#F5F7FA")
        )
    } else {
        WidgetPalette(
            background = Color.parseColor("#F3F5F7"),
            surface = Color.parseColor("#FFFFFF"),
            onBackground = Color.parseColor("#171C21"),
            onSurface = Color.parseColor("#171C21"),
            primary = Color.parseColor("#2F3842"),
            primaryContainer = Color.parseColor("#DCE3EA"),
            onPrimaryContainer = Color.parseColor("#1B2127"),
            error = Color.parseColor("#B3261E"),
            runicText = Color.parseColor("#25303A")
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
    packPalette: WidgetPalette,
    darkTheme: Boolean
): WidgetPalette {
    val dynamicScheme = if (darkTheme) {
        dynamicDarkColorScheme(context)
    } else {
        dynamicLightColorScheme(context)
    }

    fun blend(dynamic: Int, pack: Int): Int = ColorUtils.blendARGB(dynamic, pack, 0.28f)

    return packPalette.copy(
        background = blend(dynamicScheme.background.toArgb(), packPalette.background),
        surface = blend(dynamicScheme.surface.toArgb(), packPalette.surface),
        onBackground = blend(dynamicScheme.onBackground.toArgb(), packPalette.onBackground),
        onSurface = blend(dynamicScheme.onSurface.toArgb(), packPalette.onSurface),
        primary = blend(dynamicScheme.primary.toArgb(), packPalette.primary),
        primaryContainer = blend(dynamicScheme.primaryContainer.toArgb(), packPalette.primaryContainer),
        onPrimaryContainer = blend(
            dynamicScheme.onPrimaryContainer.toArgb(),
            packPalette.onPrimaryContainer
        ),
        error = blend(dynamicScheme.error.toArgb(), packPalette.error),
        runicText = blend(dynamicScheme.onSurface.toArgb(), packPalette.runicText)
    )
}
