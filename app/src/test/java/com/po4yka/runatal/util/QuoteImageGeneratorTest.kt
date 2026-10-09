package com.po4yka.runatal.util

import com.po4yka.runatal.domain.model.Quote
import com.po4yka.runatal.domain.model.QuoteShareContent
import com.po4yka.runatal.domain.model.RunicScript
import com.po4yka.runatal.domain.transliteration.TransliterationFactory
import com.po4yka.runatal.domain.transliteration.ElderFutharkTransliterator
import com.po4yka.runatal.domain.transliteration.YoungerFutharkTransliterator
import com.po4yka.runatal.domain.transliteration.CirthTransliterator
import android.app.Application
import android.graphics.Bitmap
import java.io.File
import org.robolectric.RuntimeEnvironment
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class QuoteImageGeneratorTest {

    private val generator = QuoteImageGenerator(RuntimeEnvironment.getApplication())

    @Test
    fun `every template preserves long words newlines runes and author inside the image`() {
        val factory = TransliterationFactory(
            ElderFutharkTransliterator(), YoungerFutharkTransliterator(), CirthTransliterator()
        )
        val sources = listOf("Words remember. ".repeat(17).trim(), "x".repeat(280), "First\nSecond\nThird\nFourth")
        RunicScript.entries.forEach { script ->
            sources.forEach { text ->
                val content = QuoteShareContent(
                    Quote(1L, text, "Author".repeat(10), null, null, null),
                    script, "noto", factory.transliterate(text, script)
                )
                ShareTemplate.entries.forEach { template ->
                    val prepared = generator.prepareLayout(content, template, ShareAppearance.LIGHT)
                    assertThat(prepared.blocks.map { it.layout.text.toString() }).containsExactly(
                        "Runatal · ${content.scriptLabel}",
                        content.runicText,
                        "“$text”", "— ${content.author}"
                    )
                    prepared.blocks.forEach { block ->
                        assertThat(block.layout.getLineEnd(block.layout.lineCount - 1))
                            .isEqualTo(block.layout.text.length)
                        assertThat(block.top).isAtLeast(prepared.panel.top)
                        assertThat(block.top + block.layout.height).isAtMost(prepared.panel.bottom)
                        // LineMax excludes trailing wrap spaces, which have no visible ink.
                        repeat(block.layout.lineCount) { line ->
                            com.google.common.truth.Truth.assertWithMessage(
                                "$script $template source=${text.take(20)} line=$line " +
                                    "max=${block.layout.getLineMax(line)} text=" +
                                    block.layout.text.subSequence(
                                        block.layout.getLineStart(line), block.layout.getLineEnd(line)
                                    ))
                                .that(block.layout.getLineMax(line)).isAtMost(block.layout.width.toFloat())
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `native share render produces inspectable images for all templates`() {
        val text = "Wisdom begins in wonder.\nRunes remember the words we carry."
        val content = QuoteShareContent(
            Quote(1L, text, "Socrates / Runatal", null, null, null),
            RunicScript.ELDER_FUTHARK, "babelstone", ElderFutharkTransliterator().transliterate(text)
        )
        val output = File("build/reports/share-render").apply { mkdirs() }
        ShareTemplate.entries.forEach { template ->
            val bitmap = generator.generateQuoteImage(content, template, ShareAppearance.LIGHT)
            File(output, "${template.name.lowercase()}.png").outputStream().use { stream ->
                assertThat(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)).isTrue()
            }
        }
    }

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
