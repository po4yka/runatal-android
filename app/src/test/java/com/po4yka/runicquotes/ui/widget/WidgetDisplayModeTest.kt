package com.po4yka.runicquotes.ui.widget

import com.google.common.truth.Truth.assertThat
import com.po4yka.runicquotes.data.preferences.WidgetDisplayMode
import org.junit.Test

class WidgetDisplayModeTest {

    @Test
    fun `fromPersistedValue returns matching mode`() {
        assertThat(WidgetDisplayMode.fromPersistedValue("rune_only"))
            .isEqualTo(WidgetDisplayMode.RUNE_ONLY)
        assertThat(WidgetDisplayMode.fromPersistedValue("daily_random_tap"))
            .isEqualTo(WidgetDisplayMode.DAILY_RANDOM_TAP)
    }

    @Test
    fun `fromPersistedValue falls back to rune latin`() {
        assertThat(WidgetDisplayMode.fromPersistedValue("unexpected"))
            .isEqualTo(WidgetDisplayMode.RUNE_LATIN)
    }
}
