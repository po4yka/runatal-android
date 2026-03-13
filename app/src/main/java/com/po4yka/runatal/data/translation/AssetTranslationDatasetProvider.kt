package com.po4yka.runatal.data.translation

import android.content.Context
import com.po4yka.runatal.domain.translation.EreborOrthographyStore
import com.po4yka.runatal.domain.translation.EreborTablesData
import com.po4yka.runatal.domain.translation.FallbackTemplatesData
import com.po4yka.runatal.domain.translation.GrammarRulesData
import com.po4yka.runatal.domain.translation.HistoricalLexiconStore
import com.po4yka.runatal.domain.translation.HistoricalPhraseTemplateEntry
import com.po4yka.runatal.domain.translation.NameAdaptationsData
import com.po4yka.runatal.domain.translation.OldNorseLexiconEntry
import com.po4yka.runatal.domain.translation.ParadigmTablesData
import com.po4yka.runatal.domain.translation.ProtoNorseLexiconEntry
import com.po4yka.runatal.domain.translation.TranslationDatasetManifest
import com.po4yka.runatal.domain.translation.TranslationGoldExampleEntry
import com.po4yka.runatal.domain.translation.RunicCorpusReferenceEntry
import com.po4yka.runatal.domain.translation.RunicCorpusStore
import com.po4yka.runatal.domain.translation.TranslationSourceManifest
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Loads the offline translation datasets shipped with the app.
 */
@Singleton
@Suppress("TooManyFunctions")
internal class AssetTranslationDatasetProvider @Inject constructor(
    @param:ApplicationContext private val context: Context
) : HistoricalLexiconStore, RunicCorpusStore, EreborOrthographyStore {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    private val datasetManifestCache by lazy {
        readObject(
            path = "translation/dataset_manifest.json",
            parser = { content -> json.decodeFromString(TranslationDatasetManifest.serializer(), content) }
        )
    }
    private val oldNorseLexiconCache by lazy {
        readList(
            path = "translation/old_norse_lexicon.json",
            serializer = ListSerializer(OldNorseLexiconEntry.serializer())
        )
    }
    private val protoNorseLexiconCache by lazy {
        readList(
            path = "translation/proto_norse_lexicon.json",
            serializer = ListSerializer(ProtoNorseLexiconEntry.serializer())
        )
    }
    private val paradigmTablesCache by lazy {
        readObject(
            path = "translation/paradigm_tables.json",
            parser = { content -> json.decodeFromString(ParadigmTablesData.serializer(), content) }
        )
    }
    private val ereborTablesCache by lazy {
        readObject(
            path = "translation/erebor_tables.json",
            parser = { content -> json.decodeFromString(EreborTablesData.serializer(), content) }
        )
    }
    private val grammarRulesCache by lazy {
        readObject(
            path = "translation/grammar_rules.json",
            parser = { content -> json.decodeFromString(GrammarRulesData.serializer(), content) }
        )
    }
    private val nameAdaptationsCache by lazy {
        readObject(
            path = "translation/name_adaptations.json",
            parser = { content -> json.decodeFromString(NameAdaptationsData.serializer(), content) }
        )
    }
    private val fallbackTemplatesCache by lazy {
        readObject(
            path = "translation/fallback_templates.json",
            parser = { content -> json.decodeFromString(FallbackTemplatesData.serializer(), content) }
        )
    }
    private val sourceManifestCache by lazy {
        readObject(
            path = "translation/source_manifest.json",
            parser = { content -> json.decodeFromString(TranslationSourceManifest.serializer(), content) }
        )
    }
    private val youngerPhraseTemplatesCache by lazy {
        readList(
            path = "translation/younger_phrase_templates.json",
            serializer = ListSerializer(HistoricalPhraseTemplateEntry.serializer())
        )
    }
    private val elderAttestedFormsCache by lazy {
        readList(
            path = "translation/elder_attested_forms.json",
            serializer = ListSerializer(HistoricalPhraseTemplateEntry.serializer())
        )
    }
    private val runicCorpusRefsCache by lazy {
        readList(
            path = "translation/runic_corpus_refs.json",
            serializer = ListSerializer(RunicCorpusReferenceEntry.serializer())
        )
    }
    private val goldExamplesCache by lazy {
        readList(
            path = "translation/gold_examples.json",
            serializer = ListSerializer(TranslationGoldExampleEntry.serializer())
        )
    }

    override fun datasetManifest(): TranslationDatasetManifest = datasetManifestCache

    override fun oldNorseLexicon(): List<OldNorseLexiconEntry> = oldNorseLexiconCache

    override fun protoNorseLexicon(): List<ProtoNorseLexiconEntry> = protoNorseLexiconCache

    override fun paradigmTables(): ParadigmTablesData = paradigmTablesCache

    override fun ereborTables(): EreborTablesData = ereborTablesCache

    override fun grammarRules(): GrammarRulesData = grammarRulesCache

    override fun nameAdaptations(): NameAdaptationsData = nameAdaptationsCache

    override fun fallbackTemplates(): FallbackTemplatesData = fallbackTemplatesCache

    override fun sourceManifest(): TranslationSourceManifest = sourceManifestCache

    override fun youngerPhraseTemplates(): List<HistoricalPhraseTemplateEntry> = youngerPhraseTemplatesCache

    override fun elderAttestedForms(): List<HistoricalPhraseTemplateEntry> = elderAttestedFormsCache

    override fun runicCorpusReferences(): List<RunicCorpusReferenceEntry> = runicCorpusRefsCache

    override fun goldExamples(): List<TranslationGoldExampleEntry> = goldExamplesCache

    /**
     * Forces eager loading of the bundled translation datasets to reduce first-use latency.
     */
    fun warmUp() {
        datasetManifestCache
        oldNorseLexiconCache
        protoNorseLexiconCache
        paradigmTablesCache
        ereborTablesCache
        grammarRulesCache
        nameAdaptationsCache
        fallbackTemplatesCache
        sourceManifestCache
        youngerPhraseTemplatesCache
        elderAttestedFormsCache
        runicCorpusRefsCache
        goldExamplesCache
    }

    private fun <T> readList(path: String, serializer: kotlinx.serialization.KSerializer<List<T>>): List<T> {
        return readObject(path) { content ->
            json.decodeFromString(serializer, content)
        }
    }

    private fun <T> readObject(path: String, parser: (String) -> T): T {
        val content = context.assets.open(path).bufferedReader().use { it.readText() }
        return parser(content)
    }
}
