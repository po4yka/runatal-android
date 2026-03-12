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
    fun `younger translation uses curated override for canonical example`() {
        val result = service.translate(
            text = "The wolf hunts at night",
            script = RunicScript.YOUNGER_FUTHARK
        )

        assertThat(result.historicalStage).isEqualTo(HistoricalStage.OLD_NORSE)
        assertThat(result.normalizedForm).isEqualTo("úlfr veiðir um nótt")
        assertThat(result.glyphOutput).contains("ᚢᛚᚠᚱ")
        assertThat(result.notes).contains("Matched a curated Old Norse example from the translation specification.")
    }

    @Test
    fun `elder translation falls back with explanatory note for unknown words`() {
        val result = service.translate(
            text = "signal",
            script = RunicScript.ELDER_FUTHARK
        )

        assertThat(result.historicalStage).isEqualTo(HistoricalStage.PROTO_NORSE)
        assertThat(result.notes.first()).contains("phonological preservation")
        assertThat(result.confidence).isLessThan(0.88f)
    }

    @Test
    fun `cirth translation records cluster-aware tokenization`() {
        val result = service.translate(
            text = "thing",
            script = RunicScript.CIRTH
        )

        assertThat(result.diplomaticForm).contains("th")
        assertThat(result.notes.joinToString()).contains("cluster tokenization")
        assertThat(result.tokenBreakdown).isNotEmpty()
    }
}

private class FakeTranslationDatasetProvider : TranslationDatasetProvider {
    override fun oldNorseLexicon(): List<OldNorseLexiconEntry> = listOf(
        OldNorseLexiconEntry(
            english = "wolf",
            partOfSpeech = "noun",
            lemma = "úlfr",
            declensionClass = "strong_masc_a"
        ),
        OldNorseLexiconEntry(
            english = "hunt",
            partOfSpeech = "verb",
            lemma = "veiða",
            present3sg = "veiðir",
            past3sg = "veiddi"
        ),
        OldNorseLexiconEntry(
            english = "night",
            partOfSpeech = "noun",
            lemma = "nótt"
        ),
        OldNorseLexiconEntry(
            english = "at",
            partOfSpeech = "preposition",
            lemma = "um",
            dativePhrase = "um"
        )
    )

    override fun protoNorseLexicon(): List<ProtoNorseLexiconEntry> = listOf(
        ProtoNorseLexiconEntry("wolf", "wulfaz", "noun"),
        ProtoNorseLexiconEntry("hunt", "waiþiþi", "verb"),
        ProtoNorseLexiconEntry("night", "nahts", "noun")
    )

    override fun inflectionTables(): InflectionTablesData = InflectionTablesData(
        strongMasculineSuffixes = mapOf("strong_masc_a" to "")
    )

    override fun cirthClusters(): CirthClustersData = CirthClustersData(
        clusters = mapOf(
            "th" to "\uE08A",
            "ng" to "\uE0B5"
        )
    )

    override fun grammarRules(): GrammarRulesData = GrammarRulesData(
        removableWords = listOf("the")
    )

    override fun nameAdaptations(): NameAdaptationsData = NameAdaptationsData()

    override fun fallbackRules(): FallbackRulesData = FallbackRulesData()

    override fun sourceManifest(): TranslationSourceManifest = TranslationSourceManifest()

    override fun backfillOverrides(): List<TranslationOverrideEntry> = listOf(
        TranslationOverrideEntry(
            sourceText = "The wolf hunts at night",
            results = listOf(
                TranslationOverrideResult(
                    script = RunicScript.YOUNGER_FUTHARK.name,
                    fidelity = TranslationFidelity.STRICT.name,
                    historicalStage = HistoricalStage.OLD_NORSE.name,
                    normalizedForm = "úlfr veiðir um nótt",
                    diplomaticForm = "ulfr uiþir um nutt",
                    glyphOutput = "ᚢᛚᚠᚱ ᚢᛁᚦᛁᚱ ᚢᛘ ᚾᚢᛏᛏ",
                    variant = YoungerFutharkVariant.LONG_BRANCH.name,
                    confidence = 0.97f,
                    notes = listOf("Matched a curated Old Norse example from the translation specification.")
                )
            )
        )
    )
}
