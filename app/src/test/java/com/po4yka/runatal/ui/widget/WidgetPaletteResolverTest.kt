package com.po4yka.runatal.ui.widget

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.compose.ui.graphics.toArgb
import com.google.common.truth.Truth.assertThat
import com.po4yka.runatal.data.preferences.UserPreferences
import com.po4yka.runatal.ui.theme.foundationRunicColorScheme
import com.po4yka.runatal.ui.theme.runicColorScheme
import io.mockk.every
import io.mockk.mockk
import org.junit.Test

class WidgetPaletteResolverTest {

    @Test
    fun `widget palette is derived from app color scheme roles`() {
        val colorScheme = foundationRunicColorScheme(darkTheme = true)

        val palette = widgetPaletteFromColorScheme(colorScheme)

        assertThat(palette.background).isEqualTo(colorScheme.background.toArgb())
        assertThat(palette.surface).isEqualTo(colorScheme.surface.toArgb())
        assertThat(palette.surfaceMuted).isEqualTo(colorScheme.surfaceContainerHighest.toArgb())
        assertThat(palette.onSurface).isEqualTo(colorScheme.onSurface.toArgb())
        assertThat(palette.onSurfaceVariant).isEqualTo(colorScheme.onSurfaceVariant.toArgb())
        assertThat(palette.outline).isEqualTo(colorScheme.outlineVariant.toArgb())
        assertThat(palette.runicText).isEqualTo(colorScheme.onSurface.toArgb())
    }

    @Test
    fun `default widget palette matches dark foundation color scheme`() {
        assertThat(WidgetPalette.default())
            .isEqualTo(widgetPaletteFromColorScheme(foundationRunicColorScheme(darkTheme = true)))
    }

    @Test
    fun `resolveWidgetPalette respects explicit light and dark theme preferences`() {
        val context = contextWithNightMode(Configuration.UI_MODE_NIGHT_YES)

        val lightPalette = resolveWidgetPalette(
            context = context,
            preferences = UserPreferences(themeMode = "light")
        )
        val darkPalette = resolveWidgetPalette(
            context = context,
            preferences = UserPreferences(themeMode = "dark")
        )

        assertThat(lightPalette).isEqualTo(
            widgetPaletteFromColorScheme(
                runicColorScheme(
                    darkTheme = false,
                    themePack = "stone",
                    highContrast = false,
                    dynamicColorEnabled = false,
                    context = context
                )
            )
        )
        assertThat(darkPalette).isEqualTo(
            widgetPaletteFromColorScheme(
                runicColorScheme(
                    darkTheme = true,
                    themePack = "stone",
                    highContrast = false,
                    dynamicColorEnabled = false,
                    context = context
                )
            )
        )
    }

    @Test
    fun `resolveWidgetPalette follows system night mode and high contrast preference`() {
        val darkContext = contextWithNightMode(Configuration.UI_MODE_NIGHT_YES)
        val lightContext = contextWithNightMode(Configuration.UI_MODE_NIGHT_NO)

        val darkSystemPalette = resolveWidgetPalette(
            context = darkContext,
            preferences = UserPreferences(themeMode = "system", highContrastEnabled = true)
        )
        val lightSystemPalette = resolveWidgetPalette(
            context = lightContext,
            preferences = UserPreferences(themeMode = "system")
        )

        assertThat(darkSystemPalette).isEqualTo(
            widgetPaletteFromColorScheme(
                runicColorScheme(
                    darkTheme = true,
                    themePack = "stone",
                    highContrast = true,
                    dynamicColorEnabled = false,
                    context = darkContext
                )
            )
        )
        assertThat(lightSystemPalette).isEqualTo(
            widgetPaletteFromColorScheme(
                runicColorScheme(
                    darkTheme = false,
                    themePack = "stone",
                    highContrast = false,
                    dynamicColorEnabled = false,
                    context = lightContext
                )
            )
        )
    }

    private fun contextWithNightMode(nightMask: Int): Context {
        val configuration = Configuration().apply {
            uiMode = nightMask
        }
        val resources = mockk<Resources>()
        every { resources.configuration } returns configuration
        return mockk<Context>().also {
            every { it.resources } returns resources
        }
    }
}
