package com.po4yka.runatal.data.translation

import com.google.common.truth.Truth.assertThat
import com.po4yka.runatal.domain.model.RunicScript
import com.po4yka.runatal.domain.translation.ElderFutharkTranslationEngine
import com.po4yka.runatal.domain.translation.HistoricalStage
import com.po4yka.runatal.domain.translation.TranslationFidelity
import com.po4yka.runatal.domain.translation.TranslationRequest
import com.po4yka.runatal.domain.translation.TranslationResolutionStatus
import com.po4yka.runatal.domain.transliteration.ElderFutharkTransliterator
import org.junit.Test

class ElderTargetStageAssetsTest {
    private val data = bundledTranslationTestData()
    private val engine = ElderFutharkTranslationEngine(data, data, ElderFutharkTransliterator())

    @Test
    fun `shared Old Norse paraphrases never contaminate the Elder target language`() {
        listOf(TranslationFidelity.READABLE, TranslationFidelity.DECORATIVE).forEach { fidelity ->
            listOf("computer", "opportunity", "yourself", "everyone").forEach { source ->
                val result = translate(source, fidelity)
                assertThat(result.historicalStage).isEqualTo(HistoricalStage.PRESERVED_SOURCE)
                assertThat(result.normalizedForm).isEqualTo(source)
                assertThat(result.resolutionStatus).isEqualTo(TranslationResolutionStatus.APPROXIMATED)
                assertThat(result.notes.joinToString()).contains("no Proto-Norse lemma")
                assertThat(result.glyphOutput).isEqualTo(ElderFutharkTransliterator().transliterate(source))
            }
        }
    }

    @Test
    fun `curated Proto-Norse forms retain marked vowels in language layer without Latin glyph leakage`() {
        val examples = mapOf("mountain" to ("bergą" to "ᛒᛖᚱᚷᚨ"), "work" to ("werką" to "ᚹᛖᚱᚲᚨ"))
        examples.forEach { (source, expected) ->
            val result = translate(source)
            assertThat(result.historicalStage).isEqualTo(HistoricalStage.PROTO_NORSE)
            assertThat(result.normalizedForm).isEqualTo(expected.first)
            assertThat(result.glyphOutput).isEqualTo(expected.second)
            assertThat(result.glyphOutput.any { it.code !in 0x16A0..0x16FF }).isFalse()
            assertThat(result.resolutionStatus).isEqualTo(TranslationResolutionStatus.APPROXIMATED)
        }
    }

    @Test
    fun `mixed lexical reconstruction and preserved source has an explicit mixed-stage contract`() {
        val result = translate("wolf computer")
        assertThat(result.normalizedForm).isEqualTo("wulfaz computer")
        assertThat(result.historicalStage).isEqualTo(HistoricalStage.MIXED_PROTO_NORSE_SOURCE)
        assertThat(result.resolutionStatus).isEqualTo(TranslationResolutionStatus.APPROXIMATED)
        assertThat(result.normalizedForm).doesNotContain("reiknandi")
        assertThat(result.normalizedForm).doesNotContain("vél")
    }

    @Test
    fun `unsupported Unicode source remains explicit preservation instead of false Proto-Norse`() {
        val result = translate("Привет café 12.5")
        assertThat(result.historicalStage).isEqualTo(HistoricalStage.PRESERVED_SOURCE)
        assertThat(result.resolutionStatus).isEqualTo(TranslationResolutionStatus.APPROXIMATED)
        assertThat(result.glyphOutput).contains("Привет")
        assertThat(result.glyphOutput).contains("café")
        assertThat(result.glyphOutput).contains("12.5")
    }

    @Test
    fun `strict unknown words remain unavailable and never authorize a source fallback`() {
        val result = translate("computer", TranslationFidelity.STRICT)
        assertThat(result.resolutionStatus).isEqualTo(TranslationResolutionStatus.UNAVAILABLE)
        assertThat(result.glyphOutput).isEmpty()
        assertThat(result.historicalStage).isEqualTo(HistoricalStage.PROTO_NORSE)
    }

    private fun translate(text: String, fidelity: TranslationFidelity = TranslationFidelity.READABLE) =
        engine.translate(TranslationRequest(text, RunicScript.ELDER_FUTHARK, fidelity))
}
