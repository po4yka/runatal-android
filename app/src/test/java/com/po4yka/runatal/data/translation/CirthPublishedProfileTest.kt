package com.po4yka.runatal.data.translation

import com.google.common.truth.Truth.assertThat
import com.po4yka.runatal.domain.model.RunicScript
import com.po4yka.runatal.domain.translation.ElderFutharkTranslationEngine
import com.po4yka.runatal.domain.translation.EreborCirthTranslationEngine
import com.po4yka.runatal.domain.translation.HistoricalStage
import com.po4yka.runatal.domain.translation.TranslationFidelity
import com.po4yka.runatal.domain.translation.TranslationRequest
import com.po4yka.runatal.domain.translation.TranslationResolutionStatus
import com.po4yka.runatal.domain.transliteration.CirthAlphabet
import com.po4yka.runatal.domain.transliteration.CirthTransliterator
import com.po4yka.runatal.domain.transliteration.ElderFutharkTransliterator
import com.po4yka.runatal.data.seed.RuneReferenceSeedData
import org.junit.Test

/** Independent oracles transcribed from the publisher's Cirth demo, rather than the runtime map. */
class CirthPublishedProfileTest {
    private val dataset = bundledTranslationTestData()
    private val engine = EreborCirthTranslationEngine(dataset, dataset, CirthTransliterator())
    private val title = "The Lord of the Rings translated from the Red Book"

    @Test
    fun `strict reproduces the cited corrected title page including separator and boundary glyphs`() {
        val result = engine.translate(TranslationRequest(title, RunicScript.CIRTH, TranslationFidelity.STRICT))
        val expected = "\uE0E9\uE08A\uE0BA\uE0E7\uE09E\uE0B3\uE08B\uE088\uE0E7" +
            "\uE0B3\uE083\uE0E7\uE08A\uE0BA\uE0E7\uE08B\uE0A7\uE0A3\uE0A2\uE0E7 " +
            "\uE087\uE08B\uE0B1\uE095\uE0A2\uE09E\uE0B1\uE087\uE0BB\uE088\uE0E7" +
            "\uE082\uE08B\uE0B3\uE085\uE0E7\uE08A\uE0BA\uE0E7\uE08B\uE0AF\uE088\uE0E7" +
            "\uE081\uE0B4\uE091\uE0EA"
        assertThat(result.glyphOutput).isEqualTo(expected)
        assertThat(result.resolutionStatus).isEqualTo(TranslationResolutionStatus.RECONSTRUCTED)
        assertThat(result.historicalStage).isEqualTo(HistoricalStage.EREBOR_ENGLISH)
        assertThat(result.provenance.single().url).isEqualTo("https://www.kreativekorp.com/software/fonts/fairfaxhd/")
        val caseVariant = engine.translate(TranslationRequest(title.uppercase(), RunicScript.CIRTH))
        assertThat(caseVariant.glyphOutput).isEqualTo(expected)
    }

    @Test
    fun `published words retain positive capability without extrapolating unseen English spelling`() {
        val result = engine.translate(TranslationRequest("lord book", RunicScript.CIRTH))
        assertThat(result.glyphOutput).isEqualTo("\uE09E\uE0B3\uE08B\uE088 \uE081\uE0B4\uE091")
        assertThat(result.resolutionStatus).isEqualTo(TranslationResolutionStatus.RECONSTRUCTED)
        val unsupported = engine.translate(TranslationRequest("wolf", RunicScript.CIRTH))
        assertThat(unsupported.resolutionStatus).isEqualTo(TranslationResolutionStatus.UNAVAILABLE)
        assertThat(unsupported.glyphOutput).isEmpty()
        val readable = engine.translate(TranslationRequest("wolf", RunicScript.CIRTH, TranslationFidelity.READABLE))
        assertThat(readable.glyphOutput).isEqualTo("\uE0AC\uE0B3\uE09E\uE082")
        assertThat(readable.resolutionStatus).isEqualTo(TranslationResolutionStatus.APPROXIMATED)
    }

    @Test
    fun `verified phrases cannot silently discard trailing symbols or punctuation`() {
        val elder = ElderFutharkTranslationEngine(dataset, dataset, ElderFutharkTransliterator())
        listOf("🙂", "!").forEach { suffix ->
            val strictCirth = engine.translate(TranslationRequest(title + suffix, RunicScript.CIRTH))
            if (suffix == "🙂") {
                assertThat(strictCirth.resolutionStatus).isEqualTo(TranslationResolutionStatus.UNAVAILABLE)
                assertThat(strictCirth.glyphOutput).isEmpty()
            } else {
                assertThat(strictCirth.glyphOutput).endsWith(suffix)
            }
            val readable = engine.translate(TranslationRequest(title + suffix, RunicScript.CIRTH,
                TranslationFidelity.READABLE))
            assertThat(readable.glyphOutput).endsWith(suffix)
            val strictElder = elder.translate(TranslationRequest("Hlewagastiz" + suffix, RunicScript.ELDER_FUTHARK))
            assertThat(strictElder.resolutionStatus).isEqualTo(TranslationResolutionStatus.UNAVAILABLE)
            val readableElder = elder.translate(TranslationRequest("Hlewagastiz" + suffix, RunicScript.ELDER_FUTHARK,
                TranslationFidelity.READABLE))
            assertThat(readableElder.glyphOutput).endsWith(suffix)
        }
    }

    @Test
    fun `reference chart and transliterator share genuine UCSUR glyph identities`() {
        val references = RuneReferenceSeedData.getCirthRunes()
        assertThat(references.map { it.character }).containsAtLeastElementsIn(
            (CirthAlphabet.letters.values + CirthAlphabet.sequences.values).distinct()
        )
        assertThat(references.map { it.character }.distinct()).hasSize(references.size)
        assertThat(CirthTransliterator().transliterate("t d m n i ch sh gh qu x"))
            .isEqualTo("\uE087 \uE088 \uE085 \uE08B \uE0A7 \uE08C \uE08E \uE094 \uE096 \uE091\uE0A1")
    }
}
