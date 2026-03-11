package com.po4yka.runicquotes.ui.widget

import com.google.common.truth.Truth.assertThat
import com.po4yka.runicquotes.ui.theme.foundationRunicColorScheme
import org.junit.Test

class WidgetStateTest {

    @Test
    fun `widget state defaults align with medium rune latin presentation`() {
        val state = WidgetState()

        assertThat(state.runicText).isEmpty()
        assertThat(state.sizeClass).isEqualTo(WidgetSizeClass.MEDIUM)
        assertThat(state.displayMode).isEqualTo(WidgetDisplayMode.RUNE_LATIN)
        assertThat(state.isLoading).isFalse()
        assertThat(state.error).isNull()
    }

    @Test
    fun `default palette matches dark foundation scheme`() {
        val palette = WidgetPalette.default()

        assertThat(palette).isEqualTo(
            widgetPaletteFromColorScheme(foundationRunicColorScheme(darkTheme = true))
        )
    }
}
