package com.po4yka.runicquotes.domain.translation

import com.po4yka.runicquotes.domain.model.RunicScript
import com.po4yka.runicquotes.domain.transliteration.CirthTransliterator
import com.po4yka.runicquotes.domain.transliteration.ElderFutharkTransliterator

internal class TranslationOverrideResolver(
    private val datasetProvider: TranslationDatasetProvider
) {
    fun resolve(request: TranslationRequest): TranslationResult? {
        val override = datasetProvider.backfillOverrides().firstOrNull {
            it.sourceText.equals(request.sourceText.trim(), ignoreCase = true)
        } ?: return null

        val result = override.results.firstOrNull {
            it.script == request.script.name &&
                it.fidelity == request.fidelity.name &&
                (request.script != RunicScript.YOUNGER_FUTHARK ||
                    it.variant == null ||
                    it.variant == request.youngerVariant.name)
        } ?: return null

        return TranslationResult(
            sourceText = request.sourceText,
            script = request.script,
            fidelity = request.fidelity,
            historicalStage = HistoricalStage.valueOf(result.historicalStage),
            normalizedForm = result.normalizedForm,
            diplomaticForm = result.diplomaticForm,
            glyphOutput = result.glyphOutput,
            variant = result.variant,
            confidence = result.confidence,
            notes = result.notes,
            tokenBreakdown = result.tokenBreakdown,
            engineVersion = "asset-override-v1"
        )
    }
}

internal class HistoricalLexiconLookup(
    private val datasetProvider: TranslationDatasetProvider
) {
    private val oldNorseEntries by lazy {
        datasetProvider.oldNorseLexicon().associateBy { it.english }
    }
    private val protoNorseEntries by lazy {
        datasetProvider.protoNorseLexicon().associateBy { it.english }
    }

    fun oldNorseFor(token: String): OldNorseLexiconEntry? {
        val normalized = resolveSynonym(token)
        return oldNorseEntries[normalized]
    }

    fun protoNorseFor(token: String): ProtoNorseLexiconEntry? {
        val normalized = resolveSynonym(token)
        return protoNorseEntries[normalized]
    }

    fun resolveName(token: String): String? = datasetProvider.nameAdaptations().names[token]

    fun fallbackParaphrase(token: String): String? =
        datasetProvider.fallbackRules().paraphrases[token]

    fun grammarRules(): GrammarRulesData = datasetProvider.grammarRules()

    fun cirthClusters(): CirthClustersData = datasetProvider.cirthClusters()

    private fun resolveSynonym(token: String): String =
        datasetProvider.fallbackRules().synonyms[token] ?: token
}

internal class OldNorseInflector(
    private val datasetProvider: TranslationDatasetProvider
) {
    fun inflect(entry: OldNorseLexiconEntry, token: ParsedEnglishToken): String {
        return when (entry.partOfSpeech) {
            "verb" -> when {
                token.normalized.endsWith("ed") && entry.past3sg != null -> entry.past3sg
                token.normalized.endsWith("s") && entry.present3sg != null -> entry.present3sg
                entry.present3sg != null && token.normalized == entry.english -> entry.present3sg
                else -> entry.lemma
            }

            "preposition" -> entry.dativePhrase ?: entry.lemma
            "noun" -> applyNounPattern(entry)
            else -> entry.lemma
        }
    }

    private fun applyNounPattern(entry: OldNorseLexiconEntry): String {
        if (entry.declensionClass == null) {
            return entry.lemma
        }
        val suffix = datasetProvider.inflectionTables()
            .strongMasculineSuffixes[entry.declensionClass] ?: return entry.lemma
        return if (entry.lemma.endsWith(suffix)) entry.lemma else entry.lemma + suffix
    }
}

internal class YoungerFutharkPhonologyRewriter {
    fun rewrite(text: String): String {
        return buildString(text.length) {
            text.lowercase().forEach { char ->
                append(
                    when (char) {
                        'ð' -> 'þ'
                        'e', 'é', 'æ' -> 'i'
                        'o', 'ó', 'ǫ', 'ø', 'y' -> 'u'
                        'g', 'q', 'c' -> 'k'
                        'd' -> 't'
                        else -> char
                    }
                )
            }
        }
    }
}

internal class YoungerFutharkRenderer {
    private val longBranchMap = mapOf(
        'f' to 'ᚠ',
        'u' to 'ᚢ',
        'v' to 'ᚢ',
        'w' to 'ᚢ',
        'þ' to 'ᚦ',
        'a' to 'ᛅ',
        'ą' to 'ᚬ',
        'r' to 'ᚱ',
        'ʀ' to 'ᛦ',
        'k' to 'ᚴ',
        'g' to 'ᚴ',
        'h' to 'ᚼ',
        'n' to 'ᚾ',
        'i' to 'ᛁ',
        'j' to 'ᛁ',
        's' to 'ᛋ',
        't' to 'ᛏ',
        'd' to 'ᛏ',
        'b' to 'ᛒ',
        'p' to 'ᛒ',
        'm' to 'ᛘ',
        'l' to 'ᛚ',
        ' ' to ' '
    )

    private val shortTwigMap = mapOf(
        'f' to 'ᚠ',
        'u' to 'ᚢ',
        'v' to 'ᚢ',
        'w' to 'ᚢ',
        'þ' to 'ᚦ',
        'a' to 'ᛆ',
        'ą' to 'ᚭ',
        'r' to 'ᚱ',
        'ʀ' to 'ᛧ',
        'k' to 'ᚴ',
        'g' to 'ᚴ',
        'h' to 'ᚽ',
        'n' to 'ᚿ',
        'i' to 'ᛁ',
        'j' to 'ᛁ',
        's' to 'ᛌ',
        't' to 'ᛐ',
        'd' to 'ᛐ',
        'b' to 'ᛓ',
        'p' to 'ᛓ',
        'm' to 'ᛙ',
        'l' to 'ᛚ',
        ' ' to ' '
    )

    fun render(text: String, variant: YoungerFutharkVariant): String {
        val mapping = when (variant) {
            YoungerFutharkVariant.LONG_BRANCH -> longBranchMap
            YoungerFutharkVariant.SHORT_TWIG -> shortTwigMap
        }

        return text.map { char ->
            mapping[char] ?: char
        }.joinToString("")
    }
}

internal class ProtoNorseReconstructor(
    private val lexiconLookup: HistoricalLexiconLookup
) {
    fun reconstruct(token: ParsedEnglishToken): Pair<String, String?> {
        val lexicon = lexiconLookup.protoNorseFor(token.normalized)
        return if (lexicon != null) {
            lexicon.form to null
        } else {
            token.normalized to "Used phonological preservation for '${token.raw}'."
        }
    }
}

internal class CirthClusterTokenizer(
    private val lexiconLookup: HistoricalLexiconLookup,
    private val transliterator: CirthTransliterator
) {
    fun renderToken(token: String): Pair<String, String> {
        val clusters = lexiconLookup.cirthClusters().clusters
        var remaining = token.lowercase()
        val glyphs = StringBuilder()
        val diplomaticTokens = mutableListOf<String>()

        while (remaining.isNotEmpty()) {
            val cluster = clusters.keys
                .sortedByDescending { it.length }
                .firstOrNull { remaining.startsWith(it) }
            if (cluster != null) {
                glyphs.append(clusters[cluster])
                diplomaticTokens += cluster
                remaining = remaining.removePrefix(cluster)
            } else {
                val char = remaining.first().toString()
                glyphs.append(transliterator.transliterate(char))
                diplomaticTokens += char
                remaining = remaining.drop(1)
            }
        }

        return diplomaticTokens.joinToString("·") to glyphs.toString()
    }
}

internal class ElderRuneRenderer(
    private val transliterator: ElderFutharkTransliterator
) {
    fun render(text: String): String = transliterator.transliterate(text)
}
