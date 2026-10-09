package com.po4yka.runatal.data.translation

import com.google.common.truth.Truth.assertThat
import com.po4yka.runatal.domain.model.RunicScript
import com.po4yka.runatal.domain.translation.HistoricalLexiconStore
import com.po4yka.runatal.domain.translation.TranslationDerivationKind
import com.po4yka.runatal.domain.translation.TranslationRequest
import com.po4yka.runatal.domain.translation.TranslationResolutionStatus
import com.po4yka.runatal.domain.translation.YoungerFutharkTranslationEngine
import org.junit.Test

/** Pins cited normalized forms independently, then checks comparison data and generic runtime against those oracles. */
class YoungerComparisonAssetsTest {
    private val data = bundledTranslationTestData()

    @Test
    fun `wolf comparison retains the declared definite subject and reduced rendering profile`() {
        // Barnes NION I §3.1.9 pp.56–58: the chosen suffixed masculine nominative singular article.
        // https://vsnr.org/wp-content/uploads/2021/11/NION-1.pdf
        val example = data.goldExamples().single { it.id == "gold_wolf_hunts_night" }
        val comparison = example.results.single { it.script == RunicScript.YOUNGER_FUTHARK.name }
        val expectedNormalized = "úlfrinn veiðir um nótt"
        val expectedDiplomatic = "ulfrin uiþir um nutt"
        val expectedGlyphs = "ᚢᛚᚠᚱᛁᚾ ᚢᛁᚦᛁᚱ ᚢᛘ ᚾᚢᛏᛏ"
        assertThat(comparison.normalizedForm).isEqualTo(expectedNormalized)
        assertThat(comparison.diplomaticForm).isEqualTo(expectedDiplomatic)
        assertThat(comparison.glyphOutput).isEqualTo(expectedGlyphs)
        assertThat(comparison.tokenBreakdown.map { it.sourceToken }.joinToString(" ")).isEqualTo(example.sourceText)
        assertThat(comparison.tokenBreakdown.single { it.sourceToken == "wolf" }.normalizedToken).isEqualTo("úlfrinn")
        assertThat(comparison.provenance.any { it.sourceId == "barnes_nion" }).isTrue()
        val runtime = YoungerFutharkTranslationEngine(data, data).translate(request(example.sourceText))
        assertThat(runtime.normalizedForm).isEqualTo(expectedNormalized)
        assertThat(runtime.diplomaticForm).isEqualTo(expectedDiplomatic)
        assertThat(runtime.glyphOutput).isEqualTo(expectedGlyphs)
        assertThat(runtime.derivationKind).isEqualTo(TranslationDerivationKind.TOKEN_COMPOSED)
    }

    @Test
    fun `mountain comparison uses definite nominative subject and locative definite dative complement`() {
        // Barnes pp.56–58 supplies article inflection; Zoega s.v. undir I.1 supplies selected locative dative.
        // Directional accusative (undir II.1) is a separate interpretation and is not assumed here.
        val comparison = data.youngerPhraseTemplates().single { it.id == "yf_tpl_king_walks_under_mountain" }
        val expectedNormalized = "konungrinn gengr undir fjallinu"
        val expectedDiplomatic = "kununkrin kinkr untir fialinu"
        assertThat(comparison.normalizedForm).isEqualTo(expectedNormalized)
        assertThat(comparison.diplomaticForm).isEqualTo(expectedDiplomatic)
        assertThat(comparison.tokenBreakdown.map { it.sourceToken }.joinToString(" "))
            .isEqualTo(comparison.sourceText)
        assertThat(comparison.tokenBreakdown.single { it.sourceToken == "king" }.normalizedToken)
            .isEqualTo("konungrinn")
        assertThat(comparison.tokenBreakdown.single { it.sourceToken == "mountain" }.normalizedToken)
            .isEqualTo("fjallinu")
        val runtime = YoungerFutharkTranslationEngine(data, data).translate(request(comparison.sourceText))
        assertThat(runtime.normalizedForm).isEqualTo(expectedNormalized)
        assertThat(runtime.diplomaticForm).isEqualTo(expectedDiplomatic)
        assertThat(runtime.resolutionStatus).isEqualTo(TranslationResolutionStatus.RECONSTRUCTED)
        assertThat(runtime.derivationKind).isEqualTo(TranslationDerivationKind.TOKEN_COMPOSED)
        assertThat(runtime.notes.joinToString()).contains("under as spatial location")
    }

    @Test
    fun `a matching corrected gold record cannot bypass a missing cited definite form`() {
        val altered = object : HistoricalLexiconStore by data {
            override fun oldNorseLexicon() = data.oldNorseLexicon().map { entry ->
                if (entry.english == "wolf") {
                    entry.copy(nounForms = entry.nounForms - "DEFINITE_NOMINATIVE_SINGULAR")
                } else entry
            }
        }
        val engine = YoungerFutharkTranslationEngine(altered, data)
        val result = engine.translate(request("The wolf hunts at night"))
        assertThat(result.resolutionStatus).isEqualTo(TranslationResolutionStatus.UNAVAILABLE)
        assertThat(result.glyphOutput).isEmpty()
        assertThat(result.derivationKind).isEqualTo(TranslationDerivationKind.TOKEN_COMPOSED)
    }

    @Test
    fun `a matching corrected phrase comparison cannot bypass missing locative government`() {
        val altered = object : HistoricalLexiconStore by data {
            override fun grammarRules() = data.grammarRules().copy(
                governedPrepositions = data.grammarRules().governedPrepositions - "under"
            )
        }
        val engine = YoungerFutharkTranslationEngine(altered, data)
        val result = engine.translate(request("The king walks under the mountain"))
        assertThat(result.resolutionStatus).isEqualTo(TranslationResolutionStatus.UNAVAILABLE)
        assertThat(result.glyphOutput).isEmpty()
        assertThat(result.derivationKind).isEqualTo(TranslationDerivationKind.TOKEN_COMPOSED)
    }

    private fun request(source: String) = TranslationRequest(source, RunicScript.YOUNGER_FUTHARK)
}
