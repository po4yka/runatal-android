package com.po4yka.runatal.util

import com.po4yka.runatal.domain.model.Quote
import com.po4yka.runatal.domain.model.QuoteShareContent
import com.po4yka.runatal.domain.model.RunicScript
import org.robolectric.RuntimeEnvironment
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QuoteImageGeneratorTest {

    private val generator = QuoteImageGenerator(RuntimeEnvironment.getApplication())

    @Test
    fun `generateQuoteImage renders portrait card`() {
        val bitmap = generator.generateQuoteImage(
            content = QuoteShareContent(
                Quote(1L, "Runes remember.", "Archivist", null, null, null),
                RunicScript.CIRTH, "noto", "\uE080 \uE081"
            ),
            template = ShareTemplate.CARD,
            appearance = ShareAppearance.DARK
        )

        assertThat(bitmap.width).isEqualTo(1080)
        assertThat(bitmap.height).isEqualTo(1920)
    }

    @Test
    fun `generateQuoteImage renders verse template`() {
        val bitmap = generator.generateQuoteImage(
            content = QuoteShareContent(
                Quote(1L, "Verse layout", "Skald", null, null, null),
                RunicScript.ELDER_FUTHARK, "noto", "\u16A0\u16A2\u16B1"
            ),
            template = ShareTemplate.VERSE,
            appearance = ShareAppearance.LIGHT
        )

        assertThat(bitmap.width).isEqualTo(1080)
        assertThat(bitmap.height).isEqualTo(1920)
    }

    @Test
    fun `generateQuoteImage renders landscape template`() {
        val bitmap = generator.generateQuoteImage(
            content = QuoteShareContent(
                Quote(1L, "Wide layout", "Navigator", null, null, null),
                RunicScript.YOUNGER_FUTHARK, "noto", "\u16CF\u16B1\u16DE"
            ),
            template = ShareTemplate.LANDSCAPE,
            appearance = ShareAppearance.DARK
        )

        assertThat(bitmap.width).isEqualTo(1600)
        assertThat(bitmap.height).isEqualTo(900)
    }
}
