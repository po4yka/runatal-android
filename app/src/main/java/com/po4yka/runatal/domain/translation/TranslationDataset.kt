package com.po4yka.runatal.domain.translation

import kotlinx.serialization.Serializable

/**
 * Old Norse and Proto-Norse lexical sources used by the historical translation engines.
 */
internal interface HistoricalLexiconStore {
    fun datasetManifest(): TranslationDatasetManifest
    fun sourceManifest(): TranslationSourceManifest
    fun oldNorseLexicon(): List<OldNorseLexiconEntry>
    fun protoNorseLexicon(): List<ProtoNorseLexiconEntry>
    fun paradigmTables(): ParadigmTablesData
    fun grammarRules(): GrammarRulesData
    fun nameAdaptations(): NameAdaptationsData
    fun fallbackTemplates(): FallbackTemplatesData
}

/**
 * Attested or curated phrase-level records used for Futhark selection precedence.
 */
internal interface RunicCorpusStore {
    fun datasetManifest(): TranslationDatasetManifest
    fun sourceManifest(): TranslationSourceManifest
    fun youngerPhraseTemplates(): List<HistoricalPhraseTemplateEntry>
    fun elderAttestedForms(): List<HistoricalPhraseTemplateEntry>
    fun runicCorpusReferences(): List<RunicCorpusReferenceEntry>
    fun goldExamples(): List<TranslationGoldExampleEntry>
}

/**
 * Tolkien-specific orthography tables and curated examples.
 */
internal interface EreborOrthographyStore {
    fun datasetManifest(): TranslationDatasetManifest
    fun sourceManifest(): TranslationSourceManifest
    fun ereborTables(): EreborTablesData
}

@Serializable
internal data class TranslationDatasetManifest(
    val version: String,
    val generatedAt: String,
    val generatedBy: String,
    val notes: List<String> = emptyList()
)

@Serializable
internal data class OldNorseLexiconEntry(
    val id: String,
    val english: String,
    val partOfSpeech: String,
    val lemma: String,
    val paradigmId: String? = null,
    val present3sg: String? = null,
    val past3sg: String? = null,
    val pluralForm: String? = null,
    val dativePhrase: String? = null,
    val strictEligible: Boolean = true,
    val sourceId: String,
    val citations: List<String> = emptyList(),
    val gender: GrammaticalGender? = null,
    val englishPluralForms: List<String> = emptyList(),
    val nounForms: Map<String, String> = emptyMap(),
    val adjectiveForms: Map<String, String> = emptyMap(),
    val presentForms: Map<String, String> = emptyMap(),
    val pastForms: Map<String, String> = emptyMap(),
    val englishVerbForms: Map<String, EnglishFiniteVerbForm> = emptyMap(),
    val objectCase: GrammaticalCase? = null,
    val inflectionSourceId: String? = null,
    val inflectionCitations: List<String> = emptyList()
)

@Serializable
internal data class EnglishFiniteVerbForm(val tense: GrammaticalTense, val agreements: List<String>)

@Serializable
internal enum class GrammaticalGender { MASCULINE, FEMININE, NEUTER }

@Serializable
internal enum class GrammaticalCase { NOMINATIVE, ACCUSATIVE, GENITIVE, DATIVE }

@Serializable
internal enum class GrammaticalTense { PRESENT, PAST }

@Serializable
internal enum class GrammaticalNumber { SINGULAR, PLURAL }

@Serializable
internal data class GovernedPreposition(
    val lemma: String,
    val grammaticalCase: GrammaticalCase,
    val sourceId: String,
    val citations: List<String>,
    val allowedHeadwords: List<String> = emptyList(),
    val notes: List<String> = emptyList()
)

@Serializable
internal data class ProtoNorseLexiconEntry(
    val id: String,
    val english: String,
    val form: String,
    val partOfSpeech: String,
    val strictEligible: Boolean = false,
    val sourceId: String,
    val citations: List<String> = emptyList()
)

@Serializable
internal data class ParadigmTablesData(
    val nounParadigms: Map<String, NounParadigm> = emptyMap(),
    val verbParadigms: Map<String, VerbParadigm> = emptyMap()
)

@Serializable
internal data class NounParadigm(
    val nominativeSingularSuffix: String = "",
    val pluralSuffix: String = ""
)

@Serializable
internal data class VerbParadigm(
    val thirdPersonPresentSuffix: String = "",
    val thirdPersonPastSuffix: String = ""
)

@Serializable
internal data class EreborTablesData(
    val phraseMappings: List<EreborPhraseMappingEntry> = emptyList(),
    val publishedWords: Map<String, String> = emptyMap(),
    val profileSourceId: String = "internal_heuristics",
    val profileReferenceId: String? = null,
    val wordSeparator: String = "·"
)

@Serializable
internal data class EreborPhraseMappingEntry(
    val id: String,
    val sourceText: String,
    val diplomaticForm: String,
    val glyphOutput: String,
    val resolutionStatus: String = TranslationResolutionStatus.ATTESTED.name,
    val notes: List<String> = emptyList(),
    val referenceIds: List<String> = emptyList()
)

@Serializable
internal data class GrammarRulesData(
    val removableWords: List<String> = emptyList(),
    val prepositionMap: Map<String, String> = emptyMap(),
    val interrogatives: List<String> = emptyList(),
    val pronounMap: Map<String, String> = emptyMap(),
    val governedPrepositions: Map<String, GovernedPreposition> = emptyMap()
)

@Serializable
internal data class NameAdaptationsData(
    val names: Map<String, String> = emptyMap()
)

@Serializable
internal data class FallbackTemplatesData(
    val synonyms: Map<String, String> = emptyMap(),
    val paraphrases: Map<String, String> = emptyMap()
)

@Serializable
internal data class TranslationSourceManifest(
    val sources: List<TranslationSourceEntry> = emptyList()
)

@Serializable
internal data class TranslationSourceEntry(
    val id: String,
    val name: String,
    val role: String,
    val license: String,
    val url: String
)

@Serializable
internal data class RunicCorpusReferenceEntry(
    val id: String,
    val sourceId: String,
    val label: String,
    val detail: String,
    val url: String? = null,
    val attestation: InscriptionAttestation? = null
)

@Serializable
internal data class InscriptionAttestation(
    val locator: String,
    val diplomaticText: String,
    val historicalStage: String,
    val namedForms: List<String> = emptyList()
)

@Serializable
internal data class HistoricalPhraseTemplateEntry(
    val id: String,
    val script: String,
    val fidelity: String,
    val derivationKind: String,
    val historicalStage: String,
    val sourceText: String,
    val normalizedForm: String,
    val diplomaticForm: String,
    val resolutionStatus: String = TranslationResolutionStatus.RECONSTRUCTED.name,
    val notes: List<String> = emptyList(),
    val referenceIds: List<String> = emptyList(),
    val tokenBreakdown: List<HistoricalTemplateTokenEntry> = emptyList()
)

@Serializable
internal data class HistoricalTemplateTokenEntry(
    val sourceToken: String,
    val normalizedToken: String,
    val diplomaticToken: String,
    val resolutionStatus: String = TranslationResolutionStatus.RECONSTRUCTED.name,
    val referenceIds: List<String> = emptyList()
)

@Serializable
internal data class TranslationGoldExampleEntry(
    val id: String,
    val sourceText: String,
    val results: List<TranslationGoldExampleResult>
)

@Serializable
internal data class TranslationGoldExampleResult(
    val script: String,
    val fidelity: String,
    val derivationKind: String = TranslationDerivationKind.GOLD_EXAMPLE.name,
    val historicalStage: String,
    val normalizedForm: String,
    val diplomaticForm: String,
    val glyphOutput: String,
    val requestedVariant: String? = null,
    val resolutionStatus: String = TranslationResolutionStatus.ATTESTED.name,
    val confidence: Float = 1f,
    val notes: List<String> = emptyList(),
    val unresolvedTokens: List<String> = emptyList(),
    val provenance: List<TranslationProvenanceEntry> = emptyList(),
    val tokenBreakdown: List<TranslationTokenBreakdown> = emptyList()
)
