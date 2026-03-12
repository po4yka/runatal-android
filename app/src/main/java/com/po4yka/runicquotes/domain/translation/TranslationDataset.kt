package com.po4yka.runicquotes.domain.translation

import kotlinx.serialization.Serializable

/**
 * Offline dataset contract used by the historical translation engines.
 */
@Suppress("ComplexInterface")
internal interface TranslationDatasetProvider {
    fun datasetManifest(): TranslationDatasetManifest
    fun oldNorseLexicon(): List<OldNorseLexiconEntry>
    fun protoNorseLexicon(): List<ProtoNorseLexiconEntry>
    fun inflectionTables(): InflectionTablesData
    fun cirthOrthography(): CirthOrthographyData
    fun grammarRules(): GrammarRulesData
    fun nameAdaptations(): NameAdaptationsData
    fun fallbackRules(): FallbackRulesData
    fun sourceManifest(): TranslationSourceManifest
    fun goldExamples(): List<TranslationGoldExampleEntry>
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
    val citations: List<String> = emptyList()
)

@Serializable
internal data class ProtoNorseLexiconEntry(
    val english: String,
    val form: String,
    val partOfSpeech: String,
    val strictEligible: Boolean = false,
    val sourceId: String,
    val citations: List<String> = emptyList()
)

@Serializable
internal data class InflectionTablesData(
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
internal data class CirthOrthographyData(
    val sequences: Map<String, String> = emptyMap(),
    val singleCharacters: Map<String, String> = emptyMap()
)

@Serializable
internal data class GrammarRulesData(
    val removableWords: List<String> = emptyList(),
    val prepositionMap: Map<String, String> = emptyMap(),
    val interrogatives: List<String> = emptyList(),
    val pronounMap: Map<String, String> = emptyMap()
)

@Serializable
internal data class NameAdaptationsData(
    val names: Map<String, String> = emptyMap()
)

@Serializable
internal data class FallbackRulesData(
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
internal data class TranslationGoldExampleEntry(
    val sourceText: String,
    val results: List<TranslationGoldExampleResult>
)

@Serializable
internal data class TranslationGoldExampleResult(
    val script: String,
    val fidelity: String,
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
