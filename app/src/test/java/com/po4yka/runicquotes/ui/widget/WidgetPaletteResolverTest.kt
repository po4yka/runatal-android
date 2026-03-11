package com.po4yka.runicquotes.ui.widget

import androidx.compose.ui.graphics.toArgb
import com.google.common.truth.Truth.assertThat
import com.po4yka.runicquotes.ui.theme.foundationRunicColorScheme
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
}
