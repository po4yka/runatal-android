package com.po4yka.runicquotes.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AppIconVariantTest {

    @Test
    fun `from persisted value returns matching variant`() {
        assertThat(AppIconVariant.fromPersistedValue("pine_green")).isEqualTo(AppIconVariant.PINE_GREEN)
    }

    @Test
    fun `from persisted value falls back to storm slate`() {
        assertThat(AppIconVariant.fromPersistedValue("missing")).isEqualTo(AppIconVariant.STORM_SLATE)
    }
}
