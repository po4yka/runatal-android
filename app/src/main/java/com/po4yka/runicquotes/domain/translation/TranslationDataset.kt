package com.po4yka.runicquotes.domain.translation

import kotlinx.serialization.Serializable

/**
 * Offline dataset contract used by the historical translation engines.
 */
internal interface TranslationDatasetProvider {
    fun oldNorseLexicon(): List<OldNorseLexiconEntry>
    fun protoNorseLexicon(): List<ProtoNorseLexiconEntry>
    fun inflectionTables(): InflectionTablesData
    fun cirthClusters(): CirthClustersData
    fun grammarRules(): GrammarRulesData
    fun nameAdaptations(): NameAdaptationsData
    fun fallbackRules(): FallbackRulesData
    fun sourceManifest(): TranslationSourceManifest
    fun backfillOverrides(): List<TranslationOverrideEntry>
}

@Serializable
internal data class OldNorseLexiconEntry(
    val english: String,
    val partOfSpeech: String,
    val lemma: String,
    val declensionClass: String? = null,
    val present3sg: String? = null,
    val past3sg: String? = null,
    val dativePhrase: String? = null,
    val source: String? = null
)

@Serializable
internal data class ProtoNorseLexiconEntry(
    val english: String,
    val form: String,
    val partOfSpeech: String,
    val source: String? = null
)

@Serializable
internal data class InflectionTablesData(
    val strongMasculineSuffixes: Map<String, String> = emptyMap(),
    val weakVerbSuffixes: Map<String, String> = emptyMap()
)

@Serializable
internal data class CirthClustersData(
    val clusters: Map<String, String> = emptyMap()
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
    val name: String,
    val role: String,
    val license: String
)

@Serializable
internal data class TranslationOverrideEntry(
    val sourceText: String,
    val results: List<TranslationOverrideResult>
)

@Serializable
internal data class TranslationOverrideResult(
    val script: String,
    val fidelity: String,
    val historicalStage: String,
    val normalizedForm: String,
    val diplomaticForm: String,
    val glyphOutput: String,
    val variant: String? = null,
    val confidence: Float = 1f,
    val notes: List<String> = emptyList(),
    val tokenBreakdown: List<TranslationTokenBreakdown> = emptyList()
)
