package com.po4yka.runicquotes.domain.translation

import com.google.common.truth.Truth.assertThat
import com.po4yka.runicquotes.domain.model.RunicScript
import com.po4yka.runicquotes.domain.transliteration.CirthTransliterator
import com.po4yka.runicquotes.domain.transliteration.ElderFutharkTransliterator
import org.junit.Test

class HistoricalTranslationServiceTest {

    private val datasetProvider = FakeTranslationDatasetProvider()
    private val service = HistoricalTranslationService(
        TranslationEngineFactory(
            elderEngine = ElderFutharkTranslationEngine(
                datasetProvider = datasetProvider,
                elderTransliterator = ElderFutharkTransliterator()
            ),
            youngerEngine = YoungerFutharkTranslationEngine(datasetProvider),
            cirthEngine = EreborCirthTranslationEngine(
                datasetProvider = datasetProvider,
                cirthTransliterator = CirthTransliterator()
            )
        )
    )

    @Test
    fun `strict younger translation uses curated gold example with provenance`() {
        val result = service.translate(
            text = "The wolf hunts at night",
            script = RunicScript.YOUNGER_FUTHARK
        )

        assertThat(result.historicalStage).isEqualTo(HistoricalStage.OLD_NORSE)
        assertThat(result.normalizedForm).isEqualTo("úlfr veiðir um nótt")
        assertThat(result.glyphOutput).contains("ᚢᛚᚠᚱ")
        assertThat(result.resolutionStatus).isEqualTo(TranslationResolutionStatus.RECONSTRUCTED)
        assertThat(result.provenance).isNotEmpty()
        assertThat(result.datasetVersion).isEqualTo("test-dataset-v1")
    }

    @Test
    fun `strict elder translation returns unavailable for unsupported words`() {
        val result = service.translate(
            text = "signal",
            script = RunicScript.ELDER_FUTHARK,
            fidelity = TranslationFidelity.STRICT
        )

        assertThat(result.resolutionStatus).isEqualTo(TranslationResolutionStatus.UNAVAILABLE)
        assertThat(result.unresolvedTokens).containsExactly("signal")
        assertThat(result.glyphOutput).isEmpty()
    }

    @Test
    fun `readable elder translation falls back with approximation note`() {
        val result = service.translate(
            text = "signal",
            script = RunicScript.ELDER_FUTHARK,
            fidelity = TranslationFidelity.READABLE
        )

        assertThat(result.resolutionStatus).isEqualTo(TranslationResolutionStatus.APPROXIMATED)
        assertThat(result.notes.joinToString()).contains("phonological preservation")
        assertThat(result.glyphOutput).isNotEmpty()
    }

    @Test
    fun `cirth translation records expanded orthography handling`() {
        val result = service.translate(
            text = "night",
            script = RunicScript.CIRTH,
            fidelity = TranslationFidelity.STRICT
        )

        assertThat(result.diplomaticForm).contains("gh")
        assertThat(result.notes.joinToString()).contains("cluster and diphthong rules")
        assertThat(result.provenance.single().sourceId).isEqualTo("tolkien_appendix_e")
    }
}

private class FakeTranslationDatasetProvider : TranslationDatasetProvider {
    override fun datasetManifest(): TranslationDatasetManifest = TranslationDatasetManifest(
        version = "test-dataset-v1",
        generatedAt = "2026-03-12",
        generatedBy = "tests"
    )

    override fun oldNorseLexicon(): List<OldNorseLexiconEntry> = listOf(
        OldNorseLexiconEntry(
            english = "wolf",
            partOfSpeech = "noun",
            lemma = "úlfr",
            paradigmId = "strong_masc_a",
            sourceId = "zoega"
        ),
        OldNorseLexiconEntry(
            english = "hunt",
            partOfSpeech = "verb",
            lemma = "veiða",
            paradigmId = "verb_regular_a",
            present3sg = "veiðir",
            past3sg = "veiddi",
            sourceId = "zoega"
        ),
        OldNorseLexiconEntry(
            english = "night",
            partOfSpeech = "noun",
            lemma = "nótt",
            paradigmId = "strong_fem_t",
            sourceId = "zoega"
        ),
        OldNorseLexiconEntry(
            english = "at",
            partOfSpeech = "preposition",
            lemma = "um",
            dativePhrase = "um",
            sourceId = "zoega"
        )
    )

    override fun protoNorseLexicon(): List<ProtoNorseLexiconEntry> = listOf(
        ProtoNorseLexiconEntry(
            english = "wolf",
            form = "wulfaz",
            partOfSpeech = "noun",
            strictEligible = true,
            sourceId = "srd"
        )
    )

    override fun inflectionTables(): InflectionTablesData = InflectionTablesData(
        nounParadigms = mapOf(
            "strong_masc_a" to NounParadigm(),
            "strong_fem_t" to NounParadigm()
        ),
        verbParadigms = mapOf(
            "verb_regular_a" to VerbParadigm(
                thirdPersonPresentSuffix = "r",
                thirdPersonPastSuffix = "ði"
            )
        )
    )

    override fun cirthOrthography(): CirthOrthographyData = CirthOrthographyData(
        sequences = mapOf(
            "th" to "\uE08A",
            "ng" to "\uE0B5",
            "gh" to "\uE0BB"
        ),
        singleCharacters = mapOf(
            "n" to "\uE0B4",
            "i" to "\uE0C8",
            "t" to "\uE088",
            "h" to "\uE092",
            " " to " "
        )
    )

    override fun grammarRules(): GrammarRulesData = GrammarRulesData(
        removableWords = listOf("the")
    )

    override fun nameAdaptations(): NameAdaptationsData = NameAdaptationsData()

    override fun fallbackRules(): FallbackRulesData = FallbackRulesData()

    override fun sourceManifest(): TranslationSourceManifest = TranslationSourceManifest(
        sources = listOf(
            TranslationSourceEntry(
                id = "zoega",
                name = "Zoega",
                role = "Old Norse lexicon seed",
                license = "Public domain",
                url = "https://example.com/zoega"
            ),
            TranslationSourceEntry(
                id = "srd",
                name = "SRD",
                role = "Runic comparison corpus",
                license = "Reference only",
                url = "https://example.com/srd"
            ),
            TranslationSourceEntry(
                id = "tolkien_appendix_e",
                name = "Appendix E",
                role = "Cirth orthography",
                license = "Reference only",
                url = "https://example.com/cirth"
            ),
            TranslationSourceEntry(
                id = "internal_heuristics",
                name = "Runatal heuristics",
                role = "Fallback logic",
                license = "Project-owned",
                url = "https://example.com/internal"
            )
        )
    )

    override fun goldExamples(): List<TranslationGoldExampleEntry> = listOf(
        TranslationGoldExampleEntry(
            sourceText = "The wolf hunts at night",
            results = listOf(
                TranslationGoldExampleResult(
                    script = RunicScript.YOUNGER_FUTHARK.name,
                    fidelity = TranslationFidelity.STRICT.name,
                    historicalStage = HistoricalStage.OLD_NORSE.name,
                    normalizedForm = "úlfr veiðir um nótt",
                    diplomaticForm = "ulfr uiþir um nutt",
                    glyphOutput = "ᚢᛚᚠᚱ ᚢᛁᚦᛁᚱ ᚢᛘ ᚾᚢᛏᛏ",
                    requestedVariant = YoungerFutharkVariant.LONG_BRANCH.name,
                    resolutionStatus = TranslationResolutionStatus.RECONSTRUCTED.name,
                    confidence = 0.97f,
                    notes = listOf("Matched a curated Old Norse example aligned with the translation specification."),
                    provenance = listOf(
                        TranslationProvenanceEntry(
                            sourceId = "zoega",
                            label = "Zoega",
                            role = "Old Norse lexicon seed",
                            license = "Public domain"
                        )
                    )
                )
            )
        )
    )
}
