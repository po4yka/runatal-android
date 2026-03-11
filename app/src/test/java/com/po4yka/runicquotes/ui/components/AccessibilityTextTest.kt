package com.po4yka.runicquotes.ui.components

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AccessibilityTextTest {

    @Test
    fun `buildRunicAccessibilityText formats readable summary`() {
        val description = buildRunicAccessibilityText(
            latinText = "\"Stay curious\"",
            author = "Ada Lovelace",
            scriptLabel = "Elder Futhark",
            prefix = "Share preview"
        )

        assertThat(description)
            .isEqualTo("Share preview. Stay curious. By Ada Lovelace. Script: Elder Futhark")
    }

    @Test
    fun `toggle and selection descriptions remain human readable`() {
        assertThat(toggleStateDescription(true)).isEqualTo("On")
        assertThat(toggleStateDescription(false)).isEqualTo("Off")
        assertThat(selectionStateDescription(true)).isEqualTo("Selected")
        assertThat(selectionStateDescription(false)).isEqualTo("Not selected")
    }
}
