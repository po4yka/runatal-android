package com.po4yka.runicquotes.domain.translation

import com.google.common.truth.Truth.assertThat
import com.po4yka.runicquotes.domain.model.RunicScript
import com.po4yka.runicquotes.domain.transliteration.CirthTransliterator
import com.po4yka.runicquotes.domain.transliteration.ElderFutharkTransliterator
import org.junit.Test

class HistoricalTranslationServiceTest {

    private val dataset = FakeCuratedTranslationStore()
    private val service = HistoricalTranslationService(
        TranslationEngineFactory(
            elderEngine = ElderFutharkTranslationEngine(
                lexiconStore = dataset,
                runicCorpusStore = dataset,
                elderTransliterator = ElderFutharkTransliterator()
            ),
            youngerEngine = YoungerFutharkTranslationEngine(
                lexiconStore = dataset,
                runicCorpusStore = dataset
            ),
            cirthEngine = EreborCirthTranslationEngine(
                runicCorpusStore = dataset,
                ereborStore = dataset,
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

        assertThat(result.derivationKind).isEqualTo(TranslationDerivationKind.GOLD_EXAMPLE)
        assertThat(result.historicalStage).isEqualTo(HistoricalStage.OLD_NORSE)
        assertThat(result.normalizedForm).isEqualTo("úlfr veiðir um nótt")
        assertThat(result.glyphOutput).contains("ᚢᛚᚠᚱ")
        assertThat(result.provenance.map { it.referenceId }).contains("yf_ref_wolf_night")
        assertThat(result.datasetVersion).isEqualTo("test-dataset-v2")
    }

    @Test
    fun `younger template is preferred over token composition when phrase is curated`() {
        val result = service.translate(
            text = "The king walks under the mountain",
            script = RunicScript.YOUNGER_FUTHARK,
            fidelity = TranslationFidelity.STRICT
        )

        assertThat(result.derivationKind).isEqualTo(TranslationDerivationKind.PHRASE_TEMPLATE)
        assertThat(result.normalizedForm).isEqualTo("konungr gengr undir fjall")
        assertThat(result.diplomaticForm).isEqualTo("kununkr kinkr untir fial")
        assertThat(result.provenance.single().referenceId).isEqualTo("yf_ref_king_mountain")
    }

    @Test
    fun `strict elder translation uses curated attested short formula when available`() {
        val result = service.translate(
            text = "The king",
            script = RunicScript.ELDER_FUTHARK,
            fidelity = TranslationFidelity.STRICT
        )

        assertThat(result.derivationKind).isEqualTo(TranslationDerivationKind.PHRASE_TEMPLATE)
        assertThat(result.resolutionStatus).isEqualTo(TranslationResolutionStatus.ATTESTED)
        assertThat(result.normalizedForm).isEqualTo("kuningaz")
        assertThat(result.glyphOutput).isEqualTo("ᚲᚢᚾᛁᛜᚨᛉ")
        assertThat(result.provenance.single().referenceId).isEqualTo("ef_ref_kuningaz")
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
        assertThat(result.notes.single()).contains("Missing attested or reconstructed Elder Futhark pattern")
    }

    @Test
    fun `readable elder translation falls back with approximation note`() {
        val result = service.translate(
            text = "signal",
            script = RunicScript.ELDER_FUTHARK,
            fidelity = TranslationFidelity.READABLE
        )

        assertThat(result.derivationKind).isEqualTo(TranslationDerivationKind.TOKEN_COMPOSED)
        assertThat(result.resolutionStatus).isEqualTo(TranslationResolutionStatus.APPROXIMATED)
        assertThat(result.notes.joinToString()).contains("phonological preservation")
        assertThat(result.glyphOutput).isNotEmpty()
    }

    @Test
    fun `cirth phrase mapping is preferred over sequence transcription when curated`() {
        val result = service.translate(
            text = "Under the mountain",
            script = RunicScript.CIRTH,
            fidelity = TranslationFidelity.STRICT
        )

        assertThat(result.derivationKind).isEqualTo(TranslationDerivationKind.PHRASE_TEMPLATE)
        assertThat(result.diplomaticForm).isEqualTo("u·n·d·e·r th·e m·ou·n·t·ai·n")
        assertThat(result.provenance.single().referenceId).isEqualTo("cirth_ref_under_mountain")
    }

    @Test
    fun `cirth sequence transcription records expanded orthography handling`() {
        val result = service.translate(
            text = "night",
            script = RunicScript.CIRTH,
            fidelity = TranslationFidelity.STRICT
        )

        assertThat(result.derivationKind).isEqualTo(TranslationDerivationKind.SEQUENCE_TRANSCRIPTION)
        assertThat(result.diplomaticForm).contains("gh")
        assertThat(result.notes.joinToString()).contains("sequence-table transcription")
        assertThat(result.provenance.single().sourceId).isEqualTo("tolkien_appendix_e")
    }
}

private class FakeCuratedTranslationStore :
    HistoricalLexiconStore,
    RunicCorpusStore,
    EreborOrthographyStore {

    override fun datasetManifest(): TranslationDatasetManifest = TranslationDatasetManifest(
        version = "test-dataset-v2",
        generatedAt = "2026-03-12",
        generatedBy = "tests"
    )

    override fun sourceManifest(): TranslationSourceManifest = TranslationSourceManifest(
        sources = listOf(
            TranslationSourceEntry(
                id = "onp",
                name = "ONP",
                role = "Old Norse headword and normalization reference",
                license = "Reference only",
                url = "https://example.com/onp"
            ),
            TranslationSourceEntry(
                id = "zoega",
                name = "Zoega",
                role = "Lexicon support for curated lemma forms",
                license = "Public domain",
                url = "https://example.com/zoega"
            ),
            TranslationSourceEntry(
                id = "srd",
                name = "SRD",
                role = "Gold examples and runic comparison corpus",
                license = "Reference only",
                url = "https://example.com/srd"
            ),
            TranslationSourceEntry(
                id = "runor",
                name = "Runor",
                role = "Runic inscription discovery and verification reference",
                license = "Reference only",
                url = "https://example.com/runor"
            ),
            TranslationSourceEntry(
                id = "tolkien_appendix_e",
                name = "Appendix E",
                role = "Cirth transcription reference",
                license = "Reference only",
                url = "https://example.com/appendix-e"
            ),
            TranslationSourceEntry(
                id = "internal_heuristics",
                name = "Runatal heuristics",
                role = "Offline fallback logic and generated educational notes",
                license = "Project-owned",
                url = "https://example.com/internal"
            )
        )
    )

    override fun oldNorseLexicon(): List<OldNorseLexiconEntry> = listOf(
        OldNorseLexiconEntry(
            id = "on_ulfR",
            english = "wolf",
            partOfSpeech = "noun",
            lemma = "úlfr",
            paradigmId = "strong_masc_a",
            sourceId = "zoega",
            citations = listOf("úlfr")
        ),
        OldNorseLexiconEntry(
            id = "on_veida",
            english = "hunt",
            partOfSpeech = "verb",
            lemma = "veiða",
            paradigmId = "verb_regular_a",
            present3sg = "veiðir",
            past3sg = "veiddi",
            sourceId = "zoega",
            citations = listOf("veiða")
        ),
        OldNorseLexiconEntry(
            id = "on_nott",
            english = "night",
            partOfSpeech = "noun",
            lemma = "nótt",
            paradigmId = "strong_fem_t",
            sourceId = "zoega",
            citations = listOf("nótt")
        ),
        OldNorseLexiconEntry(
            id = "on_konungr",
            english = "king",
            partOfSpeech = "noun",
            lemma = "konungr",
            paradigmId = "strong_masc_r",
            sourceId = "zoega",
            citations = listOf("konungr")
        ),
        OldNorseLexiconEntry(
            id = "on_fjall",
            english = "mountain",
            partOfSpeech = "noun",
            lemma = "fjall",
            paradigmId = "neuter_regular",
            sourceId = "zoega",
            citations = listOf("fjall")
        ),
        OldNorseLexiconEntry(
            id = "on_ganga",
            english = "walk",
            partOfSpeech = "verb",
            lemma = "ganga",
            paradigmId = "verb_regular_a",
            present3sg = "gengr",
            past3sg = "gekk",
            sourceId = "zoega",
            citations = listOf("ganga")
        ),
        OldNorseLexiconEntry(
            id = "on_undir",
            english = "under",
            partOfSpeech = "preposition",
            lemma = "undir",
            dativePhrase = "undir",
            sourceId = "zoega",
            citations = listOf("undir")
        )
    )

    override fun protoNorseLexicon(): List<ProtoNorseLexiconEntry> = listOf(
        ProtoNorseLexiconEntry(
            id = "pn_wulfaz",
            english = "wolf",
            form = "wulfaz",
            partOfSpeech = "noun",
            strictEligible = true,
            sourceId = "srd",
            citations = listOf("wulfaz")
        ),
        ProtoNorseLexiconEntry(
            id = "pn_kuningaz",
            english = "king",
            form = "kuningaz",
            partOfSpeech = "noun",
            strictEligible = true,
            sourceId = "srd",
            citations = listOf("kuningaz")
        ),
        ProtoNorseLexiconEntry(
            id = "pn_waithi",
            english = "hunt",
            form = "waiþiþi",
            partOfSpeech = "verb",
            strictEligible = false,
            sourceId = "internal_heuristics",
            citations = listOf("Approximation")
        )
    )

    override fun paradigmTables(): ParadigmTablesData = ParadigmTablesData(
        nounParadigms = mapOf(
            "strong_masc_a" to NounParadigm(pluralSuffix = "ar"),
            "strong_masc_r" to NounParadigm(pluralSuffix = "ar"),
            "strong_fem_t" to NounParadigm(pluralSuffix = "ir"),
            "neuter_regular" to NounParadigm()
        ),
        verbParadigms = mapOf(
            "verb_regular_a" to VerbParadigm(
                thirdPersonPresentSuffix = "r",
                thirdPersonPastSuffix = "ði"
            )
        )
    )

    override fun grammarRules(): GrammarRulesData = GrammarRulesData(
        removableWords = listOf("the"),
        prepositionMap = mapOf(
            "at" to "um",
            "under" to "undir"
        )
    )

    override fun nameAdaptations(): NameAdaptationsData = NameAdaptationsData()

    override fun fallbackTemplates(): FallbackTemplatesData = FallbackTemplatesData(
        synonyms = mapOf(
            "hunts" to "hunt",
            "walks" to "walk"
        )
    )

    override fun youngerPhraseTemplates(): List<HistoricalPhraseTemplateEntry> = listOf(
        HistoricalPhraseTemplateEntry(
            id = "yf_tpl_king_walks_under_mountain",
            script = RunicScript.YOUNGER_FUTHARK.name,
            fidelity = TranslationFidelity.STRICT.name,
            derivationKind = TranslationDerivationKind.PHRASE_TEMPLATE.name,
            historicalStage = HistoricalStage.OLD_NORSE.name,
            sourceText = "The king walks under the mountain",
            normalizedForm = "konungr gengr undir fjall",
            diplomaticForm = "kununkr kinkr untir fial",
            notes = listOf("Matched a curated Younger Futhark phrase template."),
            referenceIds = listOf("yf_ref_king_mountain"),
            tokenBreakdown = listOf(
                HistoricalTemplateTokenEntry(
                    sourceToken = "king",
                    normalizedToken = "konungr",
                    diplomaticToken = "kununkr",
                    referenceIds = listOf("yf_ref_king_mountain")
                ),
                HistoricalTemplateTokenEntry(
                    sourceToken = "walks",
                    normalizedToken = "gengr",
                    diplomaticToken = "kinkr",
                    referenceIds = listOf("yf_ref_king_mountain")
                ),
                HistoricalTemplateTokenEntry(
                    sourceToken = "under",
                    normalizedToken = "undir",
                    diplomaticToken = "untir",
                    referenceIds = listOf("yf_ref_king_mountain")
                ),
                HistoricalTemplateTokenEntry(
                    sourceToken = "mountain",
                    normalizedToken = "fjall",
                    diplomaticToken = "fial",
                    referenceIds = listOf("yf_ref_king_mountain")
                )
            )
        )
    )

    override fun elderAttestedForms(): List<HistoricalPhraseTemplateEntry> = listOf(
        HistoricalPhraseTemplateEntry(
            id = "ef_tpl_the_king",
            script = RunicScript.ELDER_FUTHARK.name,
            fidelity = TranslationFidelity.STRICT.name,
            derivationKind = TranslationDerivationKind.PHRASE_TEMPLATE.name,
            historicalStage = HistoricalStage.PROTO_NORSE.name,
            sourceText = "The king",
            normalizedForm = "kuningaz",
            diplomaticForm = "kuningaz",
            resolutionStatus = TranslationResolutionStatus.ATTESTED.name,
            notes = listOf("Matched a curated short Elder Futhark formula for 'the king'."),
            referenceIds = listOf("ef_ref_kuningaz"),
            tokenBreakdown = listOf(
                HistoricalTemplateTokenEntry(
                    sourceToken = "king",
                    normalizedToken = "kuningaz",
                    diplomaticToken = "kuningaz",
                    resolutionStatus = TranslationResolutionStatus.ATTESTED.name,
                    referenceIds = listOf("ef_ref_kuningaz")
                )
            )
        )
    )

    override fun runicCorpusReferences(): List<RunicCorpusReferenceEntry> = listOf(
        RunicCorpusReferenceEntry(
            id = "yf_ref_wolf_night",
            sourceId = "runor",
            label = "Runor-aligned Younger exemplar",
            detail = "Curated Younger exemplar for the wolf-night phrase."
        ),
        RunicCorpusReferenceEntry(
            id = "yf_ref_king_mountain",
            sourceId = "runor",
            label = "Runor-style king-under-mountain template",
            detail = "Curated Younger Futhark phrase template for the king-under-mountain formula."
        ),
        RunicCorpusReferenceEntry(
            id = "ef_ref_kuningaz",
            sourceId = "srd",
            label = "Kuningaz attested-form reference",
            detail = "Curated short Elder Futhark formula for 'the king'."
        ),
        RunicCorpusReferenceEntry(
            id = "cirth_ref_under_mountain",
            sourceId = "tolkien_appendix_e",
            label = "Appendix E under-mountain example",
            detail = "Curated Erebor transcription example for 'Under the mountain'."
        ),
        RunicCorpusReferenceEntry(
            id = "cirth_ref_wolf_night",
            sourceId = "tolkien_appendix_e",
            label = "Appendix E wolf-night example",
            detail = "Curated Erebor transcription example for the wolf-night phrase."
        )
    )

    override fun goldExamples(): List<TranslationGoldExampleEntry> = listOf(
        TranslationGoldExampleEntry(
            id = "gold_wolf_hunts_night",
            sourceText = "The wolf hunts at night",
            results = listOf(
                TranslationGoldExampleResult(
                    script = RunicScript.YOUNGER_FUTHARK.name,
                    fidelity = TranslationFidelity.STRICT.name,
                    derivationKind = TranslationDerivationKind.GOLD_EXAMPLE.name,
                    historicalStage = HistoricalStage.OLD_NORSE.name,
                    normalizedForm = "úlfr veiðir um nótt",
                    diplomaticForm = "ulfr uiþir um nutt",
                    glyphOutput = "ᚢᛚᚠᚱ ᚢᛁᚦᛁᚱ ᚢᛘ ᚾᚢᛏᛏ",
                    requestedVariant = YoungerFutharkVariant.LONG_BRANCH.name,
                    resolutionStatus = TranslationResolutionStatus.RECONSTRUCTED.name,
                    confidence = 0.97f,
                    notes = listOf("Matched a curated Old Norse example aligned with the specification."),
                    provenance = listOf(
                        TranslationProvenanceEntry(
                            sourceId = "runor",
                            referenceId = "yf_ref_wolf_night",
                            label = "Runor-aligned Younger exemplar",
                            role = "Runic inscription discovery and verification reference",
                            license = "Reference only"
                        )
                    )
                ),
                TranslationGoldExampleResult(
                    script = RunicScript.ELDER_FUTHARK.name,
                    fidelity = TranslationFidelity.STRICT.name,
                    derivationKind = TranslationDerivationKind.GOLD_EXAMPLE.name,
                    historicalStage = HistoricalStage.PROTO_NORSE.name,
                    normalizedForm = "wulfaz waiþiþi nahts",
                    diplomaticForm = "wulfaz waiþiþi nahts",
                    glyphOutput = "ᚹᚢᛚᚠᚨᛉ ᚹᚨᛁᚦᛁᚦᛁ ᚾᚨᚻᛏᛊ",
                    resolutionStatus = TranslationResolutionStatus.RECONSTRUCTED.name,
                    confidence = 0.9f,
                    notes = listOf("Matched a curated Proto-Norse reconstruction."),
                    provenance = listOf(
                        TranslationProvenanceEntry(
                            sourceId = "srd",
                            referenceId = "ef_ref_kuningaz",
                            label = "SRD-style Elder exemplar",
                            role = "Gold examples and runic comparison corpus",
                            license = "Reference only"
                        )
                    )
                ),
                TranslationGoldExampleResult(
                    script = RunicScript.CIRTH.name,
                    fidelity = TranslationFidelity.STRICT.name,
                    derivationKind = TranslationDerivationKind.GOLD_EXAMPLE.name,
                    historicalStage = HistoricalStage.EREBOR_ENGLISH.name,
                    normalizedForm = "the wolf hunts at night",
                    diplomaticForm = "th·e w·o·l·f h·u·n·t·s a·t n·i·gh·t",
                    glyphOutput = "    ",
                    resolutionStatus = TranslationResolutionStatus.APPROXIMATED.name,
                    confidence = 0.78f,
                    notes = listOf("Curated Erebor transcription example using Appendix E-style tables."),
                    provenance = listOf(
                        TranslationProvenanceEntry(
                            sourceId = "tolkien_appendix_e",
                            referenceId = "cirth_ref_wolf_night",
                            label = "Appendix E wolf-night example",
                            role = "Cirth transcription reference",
                            license = "Reference only"
                        )
                    )
                )
            )
        )
    )

    override fun ereborTables(): EreborTablesData = EreborTablesData(
        phraseMappings = listOf(
            EreborPhraseMappingEntry(
                id = "cirth_tpl_under_mountain",
                sourceText = "Under the mountain",
                diplomaticForm = "u·n·d·e·r th·e m·ou·n·t·ai·n",
                glyphOutput = "  ",
                notes = listOf("Matched a curated Erebor phrase mapping."),
                referenceIds = listOf("cirth_ref_under_mountain")
            )
        ),
        sequences = mapOf(
            "th" to "\uE08A",
            "ng" to "\uE0B5",
            "gh" to "\uE0BB",
            "ai" to "\uE0CA\uE0C8",
            "ou" to "\uE0CB\uE0CC"
        ),
        singleCharacters = mapOf(
            "u" to "\uE0CC",
            "n" to "\uE0B4",
            "d" to "\uE089",
            "e" to "\uE0C9",
            "r" to "\uE0A0",
            "t" to "\uE088",
            "m" to "\uE0B0",
            "a" to "\uE0CA",
            "i" to "\uE0C8",
            "g" to "\uE091",
            "h" to "\uE092",
            "o" to "\uE0CB",
            "w" to "\uE0B8",
            "l" to "\uE0A8",
            "f" to "\uE082",
            "s" to "\uE09C",
            " " to " "
        ),
        longVowels = mapOf(
            "ee" to "\uE0C9\uE0C9",
            "oo" to "\uE0CB\uE0CB"
        ),
        longConsonants = mapOf(
            "ll" to "\uE0BE",
            "tt" to "\uE088\uE088"
        ),
        wordSeparator = "·"
    )
}
