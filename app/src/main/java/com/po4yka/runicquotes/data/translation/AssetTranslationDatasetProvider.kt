package com.po4yka.runicquotes.data.translation

import android.content.Context
import com.po4yka.runicquotes.domain.translation.CirthClustersData
import com.po4yka.runicquotes.domain.translation.FallbackRulesData
import com.po4yka.runicquotes.domain.translation.GrammarRulesData
import com.po4yka.runicquotes.domain.translation.InflectionTablesData
import com.po4yka.runicquotes.domain.translation.NameAdaptationsData
import com.po4yka.runicquotes.domain.translation.OldNorseLexiconEntry
import com.po4yka.runicquotes.domain.translation.ProtoNorseLexiconEntry
import com.po4yka.runicquotes.domain.translation.TranslationDatasetProvider
import com.po4yka.runicquotes.domain.translation.TranslationOverrideEntry
import com.po4yka.runicquotes.domain.translation.TranslationSourceManifest
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer

/**
 * Loads the offline translation datasets shipped with the app.
 */
@Singleton
internal class AssetTranslationDatasetProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : TranslationDatasetProvider {

    private val json = Json {
        ignoreUnknownKeys = true
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
    private val inflectionTablesCache by lazy {
        readObject(
            path = "translation/inflection_tables.json",
            parser = { content -> json.decodeFromString(InflectionTablesData.serializer(), content) }
        )
    }
    private val cirthClustersCache by lazy {
        readObject(
            path = "translation/cirth_clusters.json",
            parser = { content -> json.decodeFromString(CirthClustersData.serializer(), content) }
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
    private val fallbackRulesCache by lazy {
        readObject(
            path = "translation/fallback_rules.json",
            parser = { content -> json.decodeFromString(FallbackRulesData.serializer(), content) }
        )
    }
    private val sourceManifestCache by lazy {
        readObject(
            path = "translation/sources_manifest.json",
            parser = { content -> json.decodeFromString(TranslationSourceManifest.serializer(), content) }
        )
    }
    private val overridesCache by lazy {
        readList(
            path = "translation/seed_backfill_overrides.json",
            serializer = ListSerializer(TranslationOverrideEntry.serializer())
        )
    }

    override fun oldNorseLexicon(): List<OldNorseLexiconEntry> = oldNorseLexiconCache

    override fun protoNorseLexicon(): List<ProtoNorseLexiconEntry> = protoNorseLexiconCache

    override fun inflectionTables(): InflectionTablesData = inflectionTablesCache

    override fun cirthClusters(): CirthClustersData = cirthClustersCache

    override fun grammarRules(): GrammarRulesData = grammarRulesCache

    override fun nameAdaptations(): NameAdaptationsData = nameAdaptationsCache

    override fun fallbackRules(): FallbackRulesData = fallbackRulesCache

    override fun sourceManifest(): TranslationSourceManifest = sourceManifestCache

    override fun backfillOverrides(): List<TranslationOverrideEntry> = overridesCache

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
