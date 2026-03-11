package com.po4yka.runicquotes.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ShareTemplateTest {

    @Test
    fun `share templates expose stable display names`() {
        assertThat(ShareTemplate.CARD.displayName).isEqualTo("Card")
        assertThat(ShareTemplate.VERSE.displayName).isEqualTo("Verse")
        assertThat(ShareTemplate.LANDSCAPE.displayName).isEqualTo("Landscape")
    }

    @Test
    fun `share appearances expose stable display names`() {
        assertThat(ShareAppearance.LIGHT.displayName).isEqualTo("Light")
        assertThat(ShareAppearance.DARK.displayName).isEqualTo("Dark")
    }
}
