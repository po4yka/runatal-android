package com.po4yka.runatal.domain.translation

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TranslationPipelineStagesTest {

    private val sourceManifest = TranslationSourceManifest(
        sources = listOf(
            TranslationSourceEntry(
                id = "zoega",
                name = "Zoega",
                role = "Lexicon reference",
                license = "Public domain",
                url = "https://example.com/zoega"
            ),
            TranslationSourceEntry(
                id = "internal_heuristics",
                name = "Runatal heuristics",
                role = "Offline fallback logic",
                license = "Project-owned",
                url = "https://example.com/internal"
            )
        )
    )

    @Test
    fun `labels and token stitching use user facing wording`() {
        assertThat(TranslationFidelity.READABLE.label).isEqualTo("Readable")
        assertThat(YoungerFutharkVariant.SHORT_TWIG.label).isEqualTo("Short-twig")
        assertThat(TranslationResolutionStatus.APPROXIMATED.label).isEqualTo("Approximation")
        assertThat(stitchTokens(listOf("wolf", ",", "night", "!"))).isEqualTo("wolf, night!")
    }

    @Test
    fun `lexicon lookup resolves synonyms names and strict eligibility`() {
        val lookup = HistoricalLexiconLookup(
            lexiconStore = lexiconStore(
                oldNorse = listOf(
                    OldNorseLexiconEntry(
                        id = "on_hunt",
                        english = "hunt",
                        lemma = "veiða",
                        partOfSpeech = "verb",
                        strictEligible = false,
                        sourceId = "zoega",
                        citations = listOf("veiða")
                    )
                ),
                protoNorse = listOf(
                    ProtoNorseLexiconEntry(
                        id = "pn_wulfaz",
                        english = "wolf",
                        form = "wulfaz",
                        partOfSpeech = "noun",
                        strictEligible = true,
                        sourceId = "zoega",
                        citations = listOf("wulfaz")
                    )
                ),
                names = mapOf("odin" to "óðinn"),
                synonyms = mapOf("hunts" to "hunt"),
                paraphrases = mapOf("signal" to "beacon")
            ),
            sourceCatalog = HistoricalSourceCatalog(sourceManifest)
        )

        assertThat(lookup.oldNorseFor("hunts", TranslationFidelity.STRICT)).isNull()
        assertThat(lookup.oldNorseFor("hunts", TranslationFidelity.READABLE)?.lemma).isEqualTo("veiða")
        assertThat(lookup.protoNorseFor("wolf", TranslationFidelity.STRICT)?.form).isEqualTo("wulfaz")
        assertThat(lookup.resolveName("odin")).isEqualTo("óðinn")
        assertThat(lookup.fallbackParaphrase("signal")).isEqualTo("beacon")
    }

    @Test
    fun `younger phonology stage applies reductions and simplifications`() {
        val output = YoungerFutharkPhonologyStage().rewrite("eodgllnn")

        assertThat(output.form).isEqualTo("iutkln")
        assertThat(output.notes).contains("Applied front-vowel reduction group.")
        assertThat(output.notes).contains("Applied rounded-vowel reduction group.")
        assertThat(output.notes).contains("Applied voicing-neutralization group.")
        assertThat(output.notes).contains("Applied devoicing group.")
        assertThat(output.notes).contains("Applied geminate-simplification group.")
    }

    @Test
    fun `proto norse lexical stage distinguishes strict readable paraphrase and preservation`() {
        val stage = ProtoNorseLexicalStage(
            HistoricalLexiconLookup(
                lexiconStore = lexiconStore(
                    paraphrases = mapOf("signal" to "beacon")
                ),
                sourceCatalog = HistoricalSourceCatalog(sourceManifest)
            )
        )

        val strict = stage.reconstruct(
            ParsedEnglishToken("signal", "signal", ParsedEnglishTokenType.WORD, 0, 6),
            TranslationFidelity.STRICT
        )
        val readableParaphrase = stage.reconstruct(
            ParsedEnglishToken("signal", "signal", ParsedEnglishTokenType.WORD, 0, 6),
            TranslationFidelity.READABLE
        )
        val readablePreservation = stage.reconstruct(
            ParsedEnglishToken("radar", "radar", ParsedEnglishTokenType.WORD, 0, 5),
            TranslationFidelity.READABLE
        )

        assertThat(strict.unresolvedToken).isEqualTo("signal")
        assertThat(strict.notes.single()).contains("Missing attested or reconstructed Elder Futhark pattern")

        assertThat(readableParaphrase.form).isEqualTo("beacon")
        assertThat(readableParaphrase.resolutionStatus).isEqualTo(TranslationResolutionStatus.APPROXIMATED)
        assertThat(readableParaphrase.notes.single()).contains("descriptive paraphrase")

        assertThat(readablePreservation.form).isEqualTo("radar")
        assertThat(readablePreservation.resolutionStatus).isEqualTo(TranslationResolutionStatus.APPROXIMATED)
        assertThat(readablePreservation.notes.single()).contains("phonological preservation")
    }

    @Test
    fun `diphthongs reduce before individual vowels and acute vowel classes never leak Latin`() {
        val stage = YoungerFutharkPhonologyStage()
        assertThat(stage.rewrite("veiði").form).isEqualTo("uiþi")
        assertThat(stage.rewrite("ei ey á é í ó ú ý").form).isEqualTo("i u a i i u u u")
        assertThat(stage.rewrite("úlfr").form).isEqualTo("ulfr")
    }

    private fun lexiconStore(
        oldNorse: List<OldNorseLexiconEntry> = emptyList(),
        protoNorse: List<ProtoNorseLexiconEntry> = emptyList(),
        nounParadigms: Map<String, NounParadigm> = emptyMap(),
        verbParadigms: Map<String, VerbParadigm> = emptyMap(),
        names: Map<String, String> = emptyMap(),
        synonyms: Map<String, String> = emptyMap(),
        paraphrases: Map<String, String> = emptyMap()
    ): HistoricalLexiconStore {
        return object : HistoricalLexiconStore {
            override fun datasetManifest(): TranslationDatasetManifest = TranslationDatasetManifest(
                version = "test",
                generatedAt = "2026-03-12",
                generatedBy = "tests"
            )

            override fun sourceManifest(): TranslationSourceManifest = sourceManifest

            override fun oldNorseLexicon(): List<OldNorseLexiconEntry> = oldNorse

            override fun protoNorseLexicon(): List<ProtoNorseLexiconEntry> = protoNorse

            override fun paradigmTables(): ParadigmTablesData = ParadigmTablesData(
                nounParadigms = nounParadigms,
                verbParadigms = verbParadigms
            )

            override fun grammarRules(): GrammarRulesData = GrammarRulesData()

            override fun nameAdaptations(): NameAdaptationsData = NameAdaptationsData(names = names)

            override fun fallbackTemplates(): FallbackTemplatesData = FallbackTemplatesData(
                synonyms = synonyms,
                paraphrases = paraphrases
            )
        }
    }
}
