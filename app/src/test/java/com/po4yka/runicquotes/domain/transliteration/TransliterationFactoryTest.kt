package com.po4yka.runicquotes.domain.transliteration

import com.google.common.truth.Truth.assertThat
import com.po4yka.runicquotes.domain.model.RunicScript
import org.junit.Before
import org.junit.Test

class TransliterationFactoryTest {

    private lateinit var factory: TransliterationFactory

    @Before
    fun setUp() {
        factory = TransliterationFactory(
            elderFutharkTransliterator = ElderFutharkTransliterator(),
            youngerFutharkTransliterator = YoungerFutharkTransliterator(),
            cirthTransliterator = CirthTransliterator()
        )
    }

    @Test
    fun `transliterateWordByWord returns empty breakdown for empty input`() {
        val breakdown = factory.transliterateWordByWord("", RunicScript.ELDER_FUTHARK)

        assertThat(breakdown.fullText).isEmpty()
        assertThat(breakdown.wordPairs).isEmpty()
    }

    @Test
    fun `transliterateWordByWord preserves whitespace only full output and emits no pairs`() {
        val breakdown = factory.transliterateWordByWord("   ", RunicScript.ELDER_FUTHARK)

        assertThat(breakdown.fullText).isEqualTo("   ")
        assertThat(breakdown.wordPairs).isEmpty()
    }

    @Test
    fun `transliterateWordByWord keeps punctuation attached to tokens`() {
        val breakdown = factory.transliterateWordByWord("Well, friend!", RunicScript.ELDER_FUTHARK)

        assertThat(breakdown.wordPairs).containsExactly(
            WordTransliterationPair(
                sourceToken = "Well,",
                runicToken = factory.transliterate("Well,", RunicScript.ELDER_FUTHARK)
            ),
            WordTransliterationPair(
                sourceToken = "friend!",
                runicToken = factory.transliterate("friend!", RunicScript.ELDER_FUTHARK)
            )
        ).inOrder()
    }

    @Test
    fun `transliterateWordByWord keeps apostrophes and hyphenated words intact`() {
        val breakdown = factory.transliterateWordByWord("stone-born one's", RunicScript.YOUNGER_FUTHARK)

        assertThat(breakdown.wordPairs).containsExactly(
            WordTransliterationPair(
                sourceToken = "stone-born",
                runicToken = factory.transliterate("stone-born", RunicScript.YOUNGER_FUTHARK)
            ),
            WordTransliterationPair(
                sourceToken = "one's",
                runicToken = factory.transliterate("one's", RunicScript.YOUNGER_FUTHARK)
            )
        ).inOrder()
    }

    @Test
    fun `transliterateWordByWord preserves digraph handling and matches full transliteration`() {
        val input = "Shifting things change"

        val breakdown = factory.transliterateWordByWord(input, RunicScript.CIRTH)

        assertThat(breakdown.wordPairs).containsExactly(
            WordTransliterationPair(
                sourceToken = "Shifting",
                runicToken = factory.transliterate("Shifting", RunicScript.CIRTH)
            ),
            WordTransliterationPair(
                sourceToken = "things",
                runicToken = factory.transliterate("things", RunicScript.CIRTH)
            ),
            WordTransliterationPair(
                sourceToken = "change",
                runicToken = factory.transliterate("change", RunicScript.CIRTH)
            )
        ).inOrder()
        assertThat(breakdown.fullText).isEqualTo(factory.transliterate(input, RunicScript.CIRTH))
    }
}
