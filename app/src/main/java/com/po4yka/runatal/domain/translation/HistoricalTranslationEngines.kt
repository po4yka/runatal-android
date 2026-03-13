package com.po4yka.runatal.domain.translation

import com.po4yka.runatal.domain.model.RunicScript
import com.po4yka.runatal.domain.transliteration.CirthTransliterator
import com.po4yka.runatal.domain.transliteration.ElderFutharkTransliterator
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Translation engine for English to Old Norse to Younger Futhark.
 */
internal class YoungerFutharkTranslationEngine @Inject constructor(
    lexiconStore: HistoricalLexiconStore,
    runicCorpusStore: RunicCorpusStore
) : TranslationEngine {

    override val script: RunicScript = RunicScript.YOUNGER_FUTHARK
    override val engineVersion: String = "yf-translation-v3"

    private val parser = EnglishSyntaxParser()
    private val sourceCatalog = HistoricalSourceCatalog(
        sourceManifest = lexiconStore.sourceManifest(),
        corpusReferences = runicCorpusStore.runicCorpusReferences()
    )
    private val goldExampleResolver = TranslationGoldExampleResolver(runicCorpusStore)
    private val phraseTemplateResolver = RunicPhraseTemplateResolver(runicCorpusStore, sourceCatalog)
    private val lexiconLookup = HistoricalLexiconLookup(lexiconStore, sourceCatalog)
    private val morphologyStage = OldNorseMorphologyStage(lexiconLookup)
    private val phonologyStage = YoungerFutharkPhonologyStage()
    private val renderer = YoungerFutharkRenderer()
    private val evidenceSynthesizer = TranslationEvidenceSynthesizer(lexiconLookup.datasetVersion())
    override val datasetVersion: String = lexiconLookup.datasetVersion()

    override fun translate(request: TranslationRequest): TranslationResult {
        goldExampleResolver.resolve(request, engineVersion)?.let { return it }
        phraseTemplateResolver.resolveYounger(request, renderer)?.let { return it.copy(engineVersion = engineVersion) }

        val parsed = parser.parse(request.sourceText)
        val grammarRules = lexiconLookup.grammarRules()
        val resolutions = parsed.tokens.mapNotNull { token ->
            when {
                token.type == ParsedEnglishTokenType.PUNCTUATION -> token.asPunctuationResolution()
                token.normalized in grammarRules.removableWords -> null
                else -> resolveToken(token, request)
            }
        }

        return evidenceSynthesizer.buildResult(
            request = request,
            resolutions = resolutions,
            evidenceRequest = TranslationEvidenceRequest(
                script = script,
                derivationKind = TranslationDerivationKind.TOKEN_COMPOSED,
                historicalStage = HistoricalStage.OLD_NORSE,
                engineVersion = engineVersion,
                requestedVariant = request.youngerVariant.name,
                baseConfidence = when (request.fidelity) {
                    TranslationFidelity.STRICT -> 0.9f
                    TranslationFidelity.READABLE -> 0.82f
                    TranslationFidelity.DECORATIVE -> 0.7f
                },
                fallbackStatus = TranslationResolutionStatus.RECONSTRUCTED,
                defaultNote = "Generated using the offline Old Norse translation pipeline."
            )
        )
    }

    private fun resolveToken(
        token: ParsedEnglishToken,
        request: TranslationRequest
    ): TranslationTokenResolution {
        val provenance = mutableListOf<TranslationProvenanceEntry>()
        val notes = mutableListOf<String>()
        var resolutionStatus = TranslationResolutionStatus.RECONSTRUCTED

        val normalized = when {
            token.normalized in lexiconLookup.grammarRules().pronounMap -> {
                provenance += lexiconLookup.provenanceFor(
                    sourceId = "internal_heuristics",
                    detail = "Pronoun mapping"
                )
                lexiconLookup.grammarRules().pronounMap.getValue(token.normalized)
            }

            token.normalized in lexiconLookup.grammarRules().prepositionMap -> {
                provenance += lexiconLookup.provenanceFor(
                    sourceId = "internal_heuristics",
                    detail = "Preposition mapping"
                )
                lexiconLookup.grammarRules().prepositionMap.getValue(token.normalized)
            }

            lexiconLookup.resolveName(token.normalized) != null -> {
                provenance += lexiconLookup.provenanceFor(
                    sourceId = "internal_heuristics",
                    detail = "Curated name adaptation"
                )
                lexiconLookup.resolveName(token.normalized).orEmpty()
            }

            lexiconLookup.oldNorseFor(token.normalized, request.fidelity) != null -> {
                val entry = lexiconLookup.oldNorseFor(token.normalized, request.fidelity)!!
                provenance += lexiconLookup.provenanceFor(entry)
                val morphology = morphologyStage.inflect(entry, token)
                notes += morphology.notes
                morphology.form
            }

            request.fidelity != TranslationFidelity.STRICT &&
                lexiconLookup.fallbackParaphrase(token.normalized) != null -> {
                resolutionStatus = TranslationResolutionStatus.APPROXIMATED
                notes += "Used descriptive paraphrase for '${token.raw}'."
                provenance += lexiconLookup.provenanceFor(
                    sourceId = "internal_heuristics",
                    detail = "Readable-mode paraphrase"
                )
                lexiconLookup.fallbackParaphrase(token.normalized).orEmpty()
            }

            request.fidelity != TranslationFidelity.STRICT -> {
                resolutionStatus = TranslationResolutionStatus.APPROXIMATED
                notes += if (request.fidelity == TranslationFidelity.DECORATIVE) {
                    "Decorative mode preserved '${token.raw}' phonetically."
                } else {
                    "Readable mode preserved '${token.raw}' phonetically."
                }
                provenance += lexiconLookup.provenanceFor(
                    sourceId = "internal_heuristics",
                    detail = "Phonological preservation fallback"
                )
                token.normalized
            }

            else -> {
                return TranslationTokenResolution(
                    sourceToken = token.raw,
                    normalizedToken = "",
                    diplomaticToken = "",
                    glyphToken = "",
                    resolutionStatus = TranslationResolutionStatus.UNAVAILABLE,
                    notes = listOf("Missing Old Norse lemma for '${token.raw}'."),
                    unresolvedToken = token.raw
                )
            }
        }

        val phonology = phonologyStage.rewrite(normalized)
        notes += phonology.notes
        val diplomatic = phonology.form
        val glyph = renderer.render(diplomatic, request.youngerVariant)

        return TranslationTokenResolution(
            sourceToken = token.raw,
            normalizedToken = normalized,
            diplomaticToken = diplomatic,
            glyphToken = glyph,
            resolutionStatus = resolutionStatus,
            notes = notes.distinct(),
            provenance = provenance.distinctBy { listOf(it.sourceId, it.referenceId, it.detail) }
        )
    }
}

/**
 * Translation engine for English to Proto-Norse to Elder Futhark.
 */
internal class ElderFutharkTranslationEngine @Inject constructor(
    lexiconStore: HistoricalLexiconStore,
    runicCorpusStore: RunicCorpusStore,
    elderTransliterator: ElderFutharkTransliterator
) : TranslationEngine {

    override val script: RunicScript = RunicScript.ELDER_FUTHARK
    override val engineVersion: String = "ef-translation-v3"

    private val parser = EnglishSyntaxParser()
    private val sourceCatalog = HistoricalSourceCatalog(
        sourceManifest = lexiconStore.sourceManifest(),
        corpusReferences = runicCorpusStore.runicCorpusReferences()
    )
    private val goldExampleResolver = TranslationGoldExampleResolver(runicCorpusStore)
    private val phraseTemplateResolver = RunicPhraseTemplateResolver(runicCorpusStore, sourceCatalog)
    private val lexiconLookup = HistoricalLexiconLookup(lexiconStore, sourceCatalog)
    private val lexicalStage = ProtoNorseLexicalStage(lexiconLookup)
    private val renderer = ElderRuneRenderer(elderTransliterator)
    private val evidenceSynthesizer = TranslationEvidenceSynthesizer(lexiconLookup.datasetVersion())
    override val datasetVersion: String = lexiconLookup.datasetVersion()

    override fun translate(request: TranslationRequest): TranslationResult {
        goldExampleResolver.resolve(request, engineVersion)?.let { return it }
        phraseTemplateResolver.resolveElder(request, renderer)?.let { return it.copy(engineVersion = engineVersion) }

        return if (request.fidelity == TranslationFidelity.STRICT) {
            strictUnavailableResult(request)
        } else {
            composeReadableResult(request)
        }
    }

    private fun composeReadableResult(request: TranslationRequest): TranslationResult {
        val parsed = parser.parse(request.sourceText)
        val resolutions = parsed.tokens.map { token ->
            when {
                token.type == ParsedEnglishTokenType.PUNCTUATION -> token.asPunctuationResolution()
                else -> {
                    val output = lexicalStage.reconstruct(token, request.fidelity)
                    if (output.unresolvedToken != null) {
                        TranslationTokenResolution(
                            sourceToken = token.raw,
                            normalizedToken = "",
                            diplomaticToken = "",
                            glyphToken = "",
                            resolutionStatus = TranslationResolutionStatus.UNAVAILABLE,
                            notes = output.notes,
                            unresolvedToken = output.unresolvedToken
                        )
                    } else {
                        val normalized = output.form.orEmpty()
                        TranslationTokenResolution(
                            sourceToken = token.raw,
                            normalizedToken = normalized,
                            diplomaticToken = normalized,
                            glyphToken = renderer.render(normalized),
                            resolutionStatus = output.resolutionStatus,
                            notes = output.notes,
                            provenance = output.provenance
                        )
                    }
                }
            }
        }

        return evidenceSynthesizer.buildResult(
            request = request,
            resolutions = resolutions,
            evidenceRequest = TranslationEvidenceRequest(
                script = script,
                derivationKind = TranslationDerivationKind.TOKEN_COMPOSED,
                historicalStage = HistoricalStage.PROTO_NORSE,
                engineVersion = engineVersion,
                baseConfidence = when (request.fidelity) {
                    TranslationFidelity.READABLE -> 0.74f
                    TranslationFidelity.DECORATIVE -> 0.6f
                    TranslationFidelity.STRICT -> 0.84f
                },
                fallbackStatus = TranslationResolutionStatus.RECONSTRUCTED,
                defaultNote = "Generated using the offline Proto-Norse translation pipeline."
            )
        )
    }

    private fun strictUnavailableResult(request: TranslationRequest): TranslationResult {
        return TranslationResult(
            sourceText = request.sourceText,
            script = script,
            fidelity = request.fidelity,
            derivationKind = TranslationDerivationKind.PHRASE_TEMPLATE,
            historicalStage = HistoricalStage.PROTO_NORSE,
            normalizedForm = "",
            diplomaticForm = "",
            glyphOutput = "",
            resolutionStatus = TranslationResolutionStatus.UNAVAILABLE,
            confidence = 0f,
            notes = listOf("Missing attested or reconstructed Elder Futhark pattern for this phrase."),
            unresolvedTokens = listOf(request.sourceText.trim()).filter { it.isNotBlank() },
            provenance = emptyList(),
            tokenBreakdown = emptyList(),
            engineVersion = engineVersion,
            datasetVersion = datasetVersion
        )
    }
}

/**
 * Translation engine for English to Erebor Cirth transcription.
 */
internal class EreborCirthTranslationEngine @Inject constructor(
    runicCorpusStore: RunicCorpusStore,
    ereborStore: EreborOrthographyStore,
    cirthTransliterator: CirthTransliterator
) : TranslationEngine {

    override val script: RunicScript = RunicScript.CIRTH
    override val engineVersion: String = "cirth-translation-v3"

    private val parser = EnglishSyntaxParser()
    private val sourceCatalog = HistoricalSourceCatalog(
        sourceManifest = ereborStore.sourceManifest(),
        corpusReferences = runicCorpusStore.runicCorpusReferences()
    )
    private val goldExampleResolver = TranslationGoldExampleResolver(runicCorpusStore)
    private val tokenizer = CirthOrthographyStage(
        ereborStore = ereborStore,
        sourceCatalog = sourceCatalog,
        transliterator = cirthTransliterator
    )
    private val evidenceSynthesizer = TranslationEvidenceSynthesizer(ereborStore.datasetManifest().version)
    override val datasetVersion: String = ereborStore.datasetManifest().version

    override fun translate(request: TranslationRequest): TranslationResult {
        goldExampleResolver.resolve(request, engineVersion)?.let { return it }
        tokenizer.resolvePhrase(request)?.let { result ->
            return result.copy(
                engineVersion = engineVersion,
                datasetVersion = datasetVersion
            )
        }

        val parsed = parser.parse(request.sourceText)
        val resolutions = parsed.tokens.map { token ->
            when {
                token.type == ParsedEnglishTokenType.PUNCTUATION -> token.asPunctuationResolution()
                else -> {
                    val output = tokenizer.renderToken(token.normalized, request.fidelity)
                    if (output.unresolvedToken != null) {
                        TranslationTokenResolution(
                            sourceToken = token.raw,
                            normalizedToken = "",
                            diplomaticToken = "",
                            glyphToken = "",
                            resolutionStatus = TranslationResolutionStatus.UNAVAILABLE,
                            notes = output.notes,
                            unresolvedToken = output.unresolvedToken
                        )
                    } else {
                        TranslationTokenResolution(
                            sourceToken = token.raw,
                            normalizedToken = token.normalized,
                            diplomaticToken = output.diplomatic.orEmpty(),
                            glyphToken = output.glyphs.orEmpty(),
                            resolutionStatus = output.resolutionStatus,
                            notes = output.notes,
                            provenance = output.provenance
                        )
                    }
                }
            }
        }

        return evidenceSynthesizer.buildResult(
            request = request,
            resolutions = resolutions,
            evidenceRequest = TranslationEvidenceRequest(
                script = script,
                derivationKind = TranslationDerivationKind.SEQUENCE_TRANSCRIPTION,
                historicalStage = HistoricalStage.EREBOR_ENGLISH,
                engineVersion = engineVersion,
                baseConfidence = when (request.fidelity) {
                    TranslationFidelity.STRICT -> 0.72f
                    TranslationFidelity.READABLE -> 0.6f
                    TranslationFidelity.DECORATIVE -> 0.54f
                },
                fallbackStatus = TranslationResolutionStatus.RECONSTRUCTED,
                defaultNote = "Generated using the offline Erebor transcription pipeline."
            )
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

private fun ParsedEnglishToken.asPunctuationResolution(): TranslationTokenResolution {
    return TranslationTokenResolution(
        sourceToken = raw,
        normalizedToken = raw,
        diplomaticToken = raw,
        glyphToken = raw,
        resolutionStatus = TranslationResolutionStatus.RECONSTRUCTED
    )
}
