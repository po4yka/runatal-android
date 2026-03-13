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
    fun `old norse morphology inflects nouns verbs and prepositions`() {
        val lookup = HistoricalLexiconLookup(
            lexiconStore = lexiconStore(
                nounParadigms = mapOf("strong_masc_r" to NounParadigm(pluralSuffix = "ar")),
                verbParadigms = mapOf(
                    "verb_regular_a" to VerbParadigm(
                        thirdPersonPresentSuffix = "r",
                        thirdPersonPastSuffix = "ði"
                    )
                )
            ),
            sourceCatalog = HistoricalSourceCatalog(sourceManifest)
        )
        val stage = OldNorseMorphologyStage(lookup)

        val pluralNoun = stage.inflect(
            entry = OldNorseLexiconEntry(
                id = "on_king",
                english = "king",
                lemma = "konungr",
                partOfSpeech = "noun",
                paradigmId = "strong_masc_r",
                sourceId = "zoega",
                citations = listOf("konungr")
            ),
            token = ParsedEnglishToken("kings", "kings", ParsedEnglishTokenType.WORD)
        )
        val pastVerb = stage.inflect(
            entry = OldNorseLexiconEntry(
                id = "on_walk",
                english = "walk",
                lemma = "ganga",
                partOfSpeech = "verb",
                paradigmId = "verb_regular_a",
                sourceId = "zoega",
                citations = listOf("ganga")
            ),
            token = ParsedEnglishToken("walked", "walked", ParsedEnglishTokenType.WORD)
        )
        val preposition = stage.inflect(
            entry = OldNorseLexiconEntry(
                id = "on_under",
                english = "under",
                lemma = "undir",
                partOfSpeech = "preposition",
                dativePhrase = "undir",
                sourceId = "zoega",
                citations = listOf("undir")
            ),
            token = ParsedEnglishToken("under", "under", ParsedEnglishTokenType.WORD)
        )

        assertThat(pluralNoun.form).isEqualTo("konungar")
        assertThat(pluralNoun.notes.single()).contains("noun paradigm")
        assertThat(pastVerb.form).isEqualTo("gangði")
        assertThat(pastVerb.notes.single()).contains("verb paradigm")
        assertThat(preposition.form).isEqualTo("undir")
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
            ParsedEnglishToken("signal", "signal", ParsedEnglishTokenType.WORD),
            TranslationFidelity.STRICT
        )
        val readableParaphrase = stage.reconstruct(
            ParsedEnglishToken("signal", "signal", ParsedEnglishTokenType.WORD),
            TranslationFidelity.READABLE
        )
        val readablePreservation = stage.reconstruct(
            ParsedEnglishToken("radar", "radar", ParsedEnglishTokenType.WORD),
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
