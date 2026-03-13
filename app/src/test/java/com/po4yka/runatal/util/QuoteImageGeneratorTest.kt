package com.po4yka.runatal.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QuoteImageGeneratorTest {

    private val generator = QuoteImageGenerator()

    @Test
    fun `generateQuoteImage renders portrait card`() {
        val bitmap = generator.generateQuoteImage(
            runicText = "\uE080 \uE081",
            latinText = "Runes remember.",
            author = "Archivist",
            template = ShareTemplate.CARD,
            appearance = ShareAppearance.DARK
        )

        assertThat(bitmap.width).isEqualTo(1080)
        assertThat(bitmap.height).isEqualTo(1920)
    }

    @Test
    fun `generateQuoteImage renders verse template`() {
        val bitmap = generator.generateQuoteImage(
            runicText = "\u16A0\u16A2\u16B1",
            latinText = "Verse layout",
            author = "Skald",
            template = ShareTemplate.VERSE,
            appearance = ShareAppearance.LIGHT
        )

        assertThat(bitmap.width).isEqualTo(1080)
        assertThat(bitmap.height).isEqualTo(1920)
    }

    @Test
    fun `generateQuoteImage renders landscape template`() {
        val bitmap = generator.generateQuoteImage(
            runicText = "\u16CF\u16B1\u16DE",
            latinText = "Wide layout",
            author = "Navigator",
            template = ShareTemplate.LANDSCAPE,
            appearance = ShareAppearance.DARK
        )

        assertThat(bitmap.width).isEqualTo(1600)
        assertThat(bitmap.height).isEqualTo(900)
    }
}
