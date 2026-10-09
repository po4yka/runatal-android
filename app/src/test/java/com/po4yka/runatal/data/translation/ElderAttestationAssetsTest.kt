package com.po4yka.runatal.data.translation

import com.google.common.truth.Truth.assertThat
import com.po4yka.runatal.domain.model.RunicScript
import com.po4yka.runatal.domain.translation.ElderAttestationPolicy
import com.po4yka.runatal.domain.translation.ElderFutharkTranslationEngine
import com.po4yka.runatal.domain.translation.HistoricalStage
import com.po4yka.runatal.domain.translation.RunicCorpusStore
import com.po4yka.runatal.domain.translation.TranslationDerivationKind
import com.po4yka.runatal.domain.translation.TranslationFidelity
import com.po4yka.runatal.domain.translation.TranslationGoldExampleEntry
import com.po4yka.runatal.domain.translation.TranslationGoldExampleResult
import com.po4yka.runatal.domain.translation.TranslationProvenanceEntry
import com.po4yka.runatal.domain.translation.TranslationRequest
import com.po4yka.runatal.domain.translation.TranslationResolutionStatus
import com.po4yka.runatal.domain.transliteration.ElderFutharkTransliterator
import org.junit.Test

/** Real asset positives use the National Museum's located Gallehus inscription, not placeholder references. */
class ElderAttestationAssetsTest {
    private val data = bundledTranslationTestData()
    private val engine = ElderFutharkTranslationEngine(data, data, ElderFutharkTransliterator())

    @Test
    fun `documented Gallehus forms remain available with actual object provenance`() {
        val examples = mapOf(
            "Hlewagastiz" to "ᚻᛚᛖᚹᚨᚷᚨᛊᛏᛁᛉ",
            "Holtijaz" to "ᚻᛟᛚᛏᛁᛃᚨᛉ",
            "I Hlewagastiz Holtijaz made the horn" to
                "ᛖᚲᚻᛚᛖᚹᚨᚷᚨᛊᛏᛁᛉ : ᚻᛟᛚᛏᛁᛃᚨᛉ : ᚻᛟᚱᚾᚨ : ᛏᚨᚹᛁᛞᛟ"
        )
        examples.forEach { (source, glyphs) ->
            val result = engine.translate(request(source))
            assertThat(result.resolutionStatus).isEqualTo(TranslationResolutionStatus.ATTESTED)
            assertThat(result.glyphOutput).isEqualTo(glyphs)
            assertThat(result.historicalStage).isEqualTo(HistoricalStage.PROTO_NORSE)
            assertThat(result.provenance.single().sourceId).isEqualTo("natmus_gallehus")
            assertThat(result.provenance.single().referenceId).isEqualTo("ef_ref_gallehus")
            assertThat(result.provenance.single().url).contains("the-golden-horns")
            assertThat(result.provenance.none { it.sourceId == "internal_heuristics" }).isTrue()
        }
    }

    @Test
    fun `unsupported wolf king and regression example cannot claim strict corpus accuracy`() {
        listOf("Wolf", "The king", "The wolf hunts at night", "Hlewagastiz 123").forEach { source ->
            val result = engine.translate(request(source))
            assertThat(result.resolutionStatus).isEqualTo(TranslationResolutionStatus.UNAVAILABLE)
            assertThat(result.confidence).isEqualTo(0f)
            assertThat(result.glyphOutput).isEmpty()
        }
        val readable = engine.translate(request("wolf").copy(fidelity = TranslationFidelity.READABLE))
        assertThat(readable.resolutionStatus).isEqualTo(TranslationResolutionStatus.APPROXIMATED)
    }

    @Test
    fun `a gold row with a real reference still cannot bypass transcription or glyph validation`() {
        val fraudulent = TranslationGoldExampleEntry(
            id = "test_bad_gold", sourceText = "invented",
            results = listOf(
                TranslationGoldExampleResult(
                    script = "ELDER_FUTHARK", fidelity = "STRICT", historicalStage = "PROTO_NORSE",
                    normalizedForm = "hlewagastiz", diplomaticForm = "hlewagastiR", glyphOutput = "wrong",
                    resolutionStatus = "ATTESTED", provenance = listOf(
                        TranslationProvenanceEntry(
                            sourceId = "natmus_gallehus", referenceId = "ef_ref_gallehus",
                            label = "Gallehus", role = "Inscription", license = "Reference only"
                        )
                    )
                )
            )
        )
        val corpus = object : RunicCorpusStore by data {
            override fun goldExamples() = listOf(fraudulent)
        }
        val result = ElderFutharkTranslationEngine(data, corpus, ElderFutharkTransliterator())
            .translate(request("invented"))
        assertThat(result.resolutionStatus).isEqualTo(TranslationResolutionStatus.UNAVAILABLE)
        assertThat(result.derivationKind).isNotEqualTo(TranslationDerivationKind.GOLD_EXAMPLE)
    }

    @Test
    fun `attestation requires a located source named form and matching normalized transcription`() {
        val policy = ElderAttestationPolicy(data)
        assertThat(policy.allows("hlewagastiz", "hlewagastiR", "ATTESTED", "PROTO_NORSE", listOf("ef_ref_gallehus")))
            .isTrue()
        assertThat(policy.allows("wulfaz", "wulfaz", "ATTESTED", "PROTO_NORSE", listOf("ef_ref_gallehus")))
            .isFalse()
        assertThat(
            policy.allows("hlewagastiz", "hlewagastiR", "RECONSTRUCTED", "PROTO_NORSE", listOf("ef_ref_gallehus"))
        )
            .isFalse()
        assertThat(policy.allows("king", "hlewagastiR", "ATTESTED", "PROTO_NORSE", listOf("ef_ref_gallehus")))
            .isFalse()
        assertThat(policy.allows("hlewagastiz", "hlewagastiR", "ATTESTED", "PROTO_NORSE", listOf("missing")))
            .isFalse()
    }
}

private fun request(text: String) = TranslationRequest(text, RunicScript.ELDER_FUTHARK, TranslationFidelity.STRICT)
