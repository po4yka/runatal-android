@file:Suppress("MagicNumber")

package com.po4yka.runicquotes.domain.translation

import com.po4yka.runicquotes.domain.model.RunicScript
import com.po4yka.runicquotes.domain.transliteration.CirthTransliterator
import com.po4yka.runicquotes.domain.transliteration.ElderFutharkTransliterator
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Translation engine for English to Old Norse to Younger Futhark.
 */
internal class YoungerFutharkTranslationEngine @Inject constructor(
    datasetProvider: TranslationDatasetProvider
) : TranslationEngine {

    override val script: RunicScript = RunicScript.YOUNGER_FUTHARK
    override val engineVersion: String = "yf-translation-v1"

    private val parser = EnglishSyntaxParser()
    private val overrideResolver = TranslationOverrideResolver(datasetProvider)
    private val lexiconLookup = HistoricalLexiconLookup(datasetProvider)
    private val inflector = OldNorseInflector(datasetProvider)
    private val phonologyRewriter = YoungerFutharkPhonologyRewriter()
    private val renderer = YoungerFutharkRenderer()

    override fun translate(request: TranslationRequest): TranslationResult {
        overrideResolver.resolve(request)?.let { return it.copy(engineVersion = engineVersion) }

        val parsed = parser.parse(request.sourceText)
        val normalizedTokens = mutableListOf<String>()
        val diplomaticTokens = mutableListOf<String>()
        val glyphTokens = mutableListOf<String>()
        val notes = mutableListOf<String>()
        var confidence = 0.92f
        val grammarRules = lexiconLookup.grammarRules()

        parsed.tokens.forEach { token ->
            if (token.type == ParsedEnglishTokenType.PUNCTUATION) {
                normalizedTokens += token.raw
                diplomaticTokens += token.raw
                glyphTokens += token.raw
                return@forEach
            }

            if (token.normalized in grammarRules.removableWords) {
                confidence -= 0.02f
                return@forEach
            }

            val normalizedToken = when {
                lexiconLookup.resolveName(token.normalized) != null -> {
                    confidence -= 0.03f
                    lexiconLookup.resolveName(token.normalized).orEmpty()
                }

                lexiconLookup.oldNorseFor(token.normalized) != null -> {
                    inflector.inflect(lexiconLookup.oldNorseFor(token.normalized)!!, token)
                }

                lexiconLookup.fallbackParaphrase(token.normalized) != null -> {
                    confidence -= 0.12f
                    notes += "Used descriptive paraphrase for '${token.raw}'."
                    lexiconLookup.fallbackParaphrase(token.normalized).orEmpty()
                }

                else -> {
                    confidence -= 0.16f
                    notes += "Preserved '${token.raw}' phonetically in Old Norse output."
                    token.normalized
                }
            }

            val diplomaticToken = phonologyRewriter.rewrite(normalizedToken)
            val glyphToken = renderer.render(diplomaticToken, request.youngerVariant)
            normalizedTokens += normalizedToken
            diplomaticTokens += diplomaticToken
            glyphTokens += glyphToken
        }

        val normalizedForm = stitchTokens(normalizedTokens)
        val diplomaticForm = stitchTokens(diplomaticTokens)
        val glyphOutput = stitchTokens(glyphTokens)

        return TranslationResult(
            sourceText = request.sourceText,
            script = script,
            fidelity = request.fidelity,
            historicalStage = HistoricalStage.OLD_NORSE,
            normalizedForm = normalizedForm,
            diplomaticForm = diplomaticForm,
            glyphOutput = glyphOutput,
            variant = request.youngerVariant.name,
            confidence = confidence.coerceIn(0.18f, 0.99f),
            notes = notes.distinct().ifEmpty {
                listOf("Generated using an offline Old Norse best-effort pipeline.")
            },
            tokenBreakdown = buildTokenBreakdown(parsed, normalizedTokens, diplomaticTokens, glyphTokens),
            engineVersion = engineVersion
        )
    }
}

/**
 * Translation engine for English to Proto-Norse to Elder Futhark.
 */
internal class ElderFutharkTranslationEngine @Inject constructor(
    datasetProvider: TranslationDatasetProvider,
    elderTransliterator: ElderFutharkTransliterator
) : TranslationEngine {

    override val script: RunicScript = RunicScript.ELDER_FUTHARK
    override val engineVersion: String = "ef-translation-v1"

    private val parser = EnglishSyntaxParser()
    private val overrideResolver = TranslationOverrideResolver(datasetProvider)
    private val lexiconLookup = HistoricalLexiconLookup(datasetProvider)
    private val reconstructor = ProtoNorseReconstructor(lexiconLookup)
    private val renderer = ElderRuneRenderer(elderTransliterator)

    override fun translate(request: TranslationRequest): TranslationResult {
        overrideResolver.resolve(request)?.let { return it.copy(engineVersion = engineVersion) }

        val parsed = parser.parse(request.sourceText)
        val normalizedTokens = mutableListOf<String>()
        val diplomaticTokens = mutableListOf<String>()
        val glyphTokens = mutableListOf<String>()
        val notes = mutableListOf<String>()
        var confidence = 0.88f

        parsed.tokens.forEach { token ->
            if (token.type == ParsedEnglishTokenType.PUNCTUATION) {
                normalizedTokens += token.raw
                diplomaticTokens += token.raw
                glyphTokens += token.raw
                return@forEach
            }

            val (protoForm, note) = reconstructor.reconstruct(token)
            if (note != null) {
                confidence -= 0.14f
                notes += note
            }
            val diplomatic = protoForm
            normalizedTokens += protoForm
            diplomaticTokens += diplomatic
            glyphTokens += renderer.render(diplomatic)
        }

        return TranslationResult(
            sourceText = request.sourceText,
            script = script,
            fidelity = request.fidelity,
            historicalStage = HistoricalStage.PROTO_NORSE,
            normalizedForm = stitchTokens(normalizedTokens),
            diplomaticForm = stitchTokens(diplomaticTokens),
            glyphOutput = stitchTokens(glyphTokens),
            confidence = confidence.coerceIn(0.12f, 0.96f),
            notes = notes.distinct().ifEmpty {
                listOf("Generated using an offline Proto-Norse reconstruction pipeline.")
            },
            tokenBreakdown = buildTokenBreakdown(parsed, normalizedTokens, diplomaticTokens, glyphTokens),
            engineVersion = engineVersion
        )
    }
}

/**
 * Translation engine for English to Erebor Cirth transcription.
 */
internal class EreborCirthTranslationEngine @Inject constructor(
    datasetProvider: TranslationDatasetProvider,
    cirthTransliterator: CirthTransliterator
) : TranslationEngine {

    override val script: RunicScript = RunicScript.CIRTH
    override val engineVersion: String = "cirth-translation-v1"

    private val parser = EnglishSyntaxParser()
    private val overrideResolver = TranslationOverrideResolver(datasetProvider)
    private val tokenizer = CirthClusterTokenizer(
        lexiconLookup = HistoricalLexiconLookup(datasetProvider),
        transliterator = cirthTransliterator
    )

    override fun translate(request: TranslationRequest): TranslationResult {
        overrideResolver.resolve(request)?.let { return it.copy(engineVersion = engineVersion) }

        val parsed = parser.parse(request.sourceText)
        val normalizedTokens = mutableListOf<String>()
        val diplomaticTokens = mutableListOf<String>()
        val glyphTokens = mutableListOf<String>()
        val notes = mutableListOf<String>()
        var confidence = 0.9f

        parsed.tokens.forEach { token ->
            if (token.type == ParsedEnglishTokenType.PUNCTUATION) {
                normalizedTokens += token.raw
                diplomaticTokens += token.raw
                glyphTokens += token.raw
                return@forEach
            }

            val normalized = token.normalized
            val (diplomatic, glyphs) = tokenizer.renderToken(normalized)
            if (diplomatic.contains('·')) {
                notes += "Applied Erebor cluster tokenization for '${token.raw}'."
            } else {
                confidence -= 0.04f
            }
            normalizedTokens += normalized
            diplomaticTokens += diplomatic
            glyphTokens += glyphs
        }

        return TranslationResult(
            sourceText = request.sourceText,
            script = script,
            fidelity = request.fidelity,
            historicalStage = HistoricalStage.EREBOR_ENGLISH,
            normalizedForm = stitchTokens(normalizedTokens),
            diplomaticForm = stitchTokens(diplomaticTokens),
            glyphOutput = stitchTokens(glyphTokens),
            confidence = confidence.coerceIn(0.24f, 0.98f),
            notes = notes.distinct().ifEmpty {
                listOf("Generated using an offline Erebor Cirth transcription pipeline.")
            },
            tokenBreakdown = buildTokenBreakdown(parsed, normalizedTokens, diplomaticTokens, glyphTokens),
            engineVersion = engineVersion
        )
    }
}

@Singleton
internal class TranslationEngineFactory @Inject constructor(
    private val elderEngine: ElderFutharkTranslationEngine,
    private val youngerEngine: YoungerFutharkTranslationEngine,
    private val cirthEngine: EreborCirthTranslationEngine
) {
    fun create(script: RunicScript): TranslationEngine = when (script) {
        RunicScript.ELDER_FUTHARK -> elderEngine
        RunicScript.YOUNGER_FUTHARK -> youngerEngine
        RunicScript.CIRTH -> cirthEngine
    }
}

@Singleton
internal class HistoricalTranslationService @Inject constructor(
    private val translationEngineFactory: TranslationEngineFactory
) {
    fun translate(
        text: String,
        script: RunicScript,
        fidelity: TranslationFidelity = TranslationFidelity.DEFAULT,
        youngerVariant: YoungerFutharkVariant = YoungerFutharkVariant.DEFAULT
    ): TranslationResult {
        val request = TranslationRequest(
            sourceText = text,
            script = script,
            fidelity = fidelity,
            youngerVariant = youngerVariant
        )
        return translationEngineFactory.create(script).translate(request)
    }
}

private fun stitchTokens(tokens: List<String>): String {
    return buildString {
        tokens.forEachIndexed { index, token ->
            if (index > 0 && token !in PunctuationTokens && previousCharacterNeedsSpace(lastOrNull())) {
                append(' ')
            }
            append(token)
        }
    }.trim()
}

private fun previousCharacterNeedsSpace(previous: Char?): Boolean {
    return previous != null && previous !in setOf(' ', '\n', '-', '·')
}

private fun buildTokenBreakdown(
    parsed: ParsedEnglishText,
    normalizedTokens: List<String>,
    diplomaticTokens: List<String>,
    glyphTokens: List<String>
): List<TranslationTokenBreakdown> {
    val wordTokens = parsed.tokens.filter { it.type == ParsedEnglishTokenType.WORD }
    return wordTokens.mapIndexed { index, token ->
        TranslationTokenBreakdown(
            sourceToken = token.raw,
            normalizedToken = normalizedTokens.getOrElse(index) { token.normalized },
            diplomaticToken = diplomaticTokens.getOrElse(index) { token.normalized },
            glyphToken = glyphTokens.getOrElse(index) { token.normalized }
        )
    }
}

private val PunctuationTokens = setOf(".", ",", "!", "?", ";", ":")
