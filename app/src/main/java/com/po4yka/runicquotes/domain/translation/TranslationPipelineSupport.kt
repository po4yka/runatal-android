package com.po4yka.runicquotes.domain.translation

import com.po4yka.runicquotes.domain.model.RunicScript
import com.po4yka.runicquotes.domain.transliteration.CirthTransliterator
import com.po4yka.runicquotes.domain.transliteration.ElderFutharkTransliterator

internal class TranslationGoldExampleResolver(
    private val datasetProvider: TranslationDatasetProvider
) {
    fun resolve(request: TranslationRequest): TranslationResult? {
        val example = datasetProvider.goldExamples().firstOrNull {
            it.sourceText.equals(request.sourceText.trim(), ignoreCase = true)
        } ?: return null

        val result = example.results.firstOrNull {
            it.script == request.script.name &&
                it.fidelity == request.fidelity.name &&
                (request.script != RunicScript.YOUNGER_FUTHARK ||
                    it.requestedVariant == null ||
                    it.requestedVariant == request.youngerVariant.name)
        } ?: return null

        return TranslationResult(
            sourceText = request.sourceText,
            script = request.script,
            fidelity = request.fidelity,
            historicalStage = HistoricalStage.valueOf(result.historicalStage),
            normalizedForm = result.normalizedForm,
            diplomaticForm = result.diplomaticForm,
            glyphOutput = result.glyphOutput,
            requestedVariant = when (request.script) {
                RunicScript.YOUNGER_FUTHARK -> request.youngerVariant.name
                else -> result.requestedVariant
            },
            resolutionStatus = TranslationResolutionStatus.valueOf(result.resolutionStatus),
            confidence = result.confidence,
            notes = result.notes,
            unresolvedTokens = result.unresolvedTokens,
            provenance = result.provenance,
            tokenBreakdown = result.tokenBreakdown,
            engineVersion = "gold-example-v2",
            datasetVersion = datasetProvider.datasetManifest().version
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
    private val sourceEntries by lazy {
        datasetProvider.sourceManifest().sources.associateBy { it.id }
    }

    fun datasetVersion(): String = datasetProvider.datasetManifest().version

    fun oldNorseFor(token: String, fidelity: TranslationFidelity): OldNorseLexiconEntry? {
        val normalized = resolveSynonym(token)
        val candidate = oldNorseEntries[normalized] ?: return null
        return if (fidelity == TranslationFidelity.STRICT && !candidate.strictEligible) null else candidate
    }

    fun protoNorseFor(token: String, fidelity: TranslationFidelity): ProtoNorseLexiconEntry? {
        val normalized = resolveSynonym(token)
        val candidate = protoNorseEntries[normalized] ?: return null
        return if (fidelity == TranslationFidelity.STRICT && !candidate.strictEligible) null else candidate
    }

    fun resolveName(token: String): String? = datasetProvider.nameAdaptations().names[token]

    fun fallbackParaphrase(token: String): String? =
        datasetProvider.fallbackRules().paraphrases[token]

    fun grammarRules(): GrammarRulesData = datasetProvider.grammarRules()

    fun cirthOrthography(): CirthOrthographyData = datasetProvider.cirthOrthography()

    fun provenanceFor(sourceId: String, detail: String? = null): TranslationProvenanceEntry {
        val source = sourceEntries[sourceId] ?: sourceEntries.getValue(INTERNAL_HEURISTICS_SOURCE_ID)
        return TranslationProvenanceEntry(
            sourceId = source.id,
            label = source.name,
            role = source.role,
            license = source.license,
            detail = detail,
            url = source.url
        )
    }

    private fun resolveSynonym(token: String): String =
        datasetProvider.fallbackRules().synonyms[token] ?: token

    private companion object {
        const val INTERNAL_HEURISTICS_SOURCE_ID = "internal_heuristics"
    }
}

internal data class TranslationTokenResolution(
    val sourceToken: String,
    val normalizedToken: String,
    val diplomaticToken: String,
    val glyphToken: String,
    val notes: List<String> = emptyList(),
    val unresolvedToken: String? = null,
    val approximated: Boolean = false,
    val provenance: List<TranslationProvenanceEntry> = emptyList()
)

internal data class MorphologyHints(
    val isPlural: Boolean,
    val isPast: Boolean,
    val isThirdPersonSingular: Boolean
)

internal data class MorphologyStageOutput(
    val form: String,
    val notes: List<String> = emptyList()
)

internal class OldNorseMorphologyStage(
    private val datasetProvider: TranslationDatasetProvider
) {
    fun inflect(entry: OldNorseLexiconEntry, token: ParsedEnglishToken): MorphologyStageOutput {
        val hints = token.toMorphologyHints()
        return when (entry.partOfSpeech) {
            "verb" -> MorphologyStageOutput(
                form = inflectVerb(entry, hints),
                notes = listOfNotNull(
                    entry.paradigmId?.let { "Applied verb paradigm $it." }
                )
            )

            "noun" -> MorphologyStageOutput(
                form = inflectNoun(entry, hints),
                notes = listOfNotNull(
                    entry.paradigmId?.let { "Applied noun paradigm $it." }
                )
            )

            "preposition" -> MorphologyStageOutput(entry.dativePhrase ?: entry.lemma)
            else -> MorphologyStageOutput(entry.lemma)
        }
    }

    private fun inflectVerb(entry: OldNorseLexiconEntry, hints: MorphologyHints): String {
        val paradigm = entry.paradigmId?.let { datasetProvider.inflectionTables().verbParadigms[it] }
        val pastForm = when {
            !hints.isPast -> null
            entry.past3sg != null -> entry.past3sg
            paradigm != null -> entry.lemma.removeSuffix("a") + paradigm.thirdPersonPastSuffix
            else -> null
        }
        val presentForm = when {
            !hints.isThirdPersonSingular -> null
            entry.present3sg != null -> entry.present3sg
            paradigm != null -> entry.lemma.removeSuffix("a") + paradigm.thirdPersonPresentSuffix
            else -> null
        }

        return pastForm ?: presentForm ?: entry.lemma
    }

    private fun inflectNoun(entry: OldNorseLexiconEntry, hints: MorphologyHints): String {
        val paradigm = entry.paradigmId?.let { datasetProvider.inflectionTables().nounParadigms[it] }
        val inflectedForm = when {
            !hints.isPlural -> null
            entry.pluralForm != null -> entry.pluralForm
            paradigm == null || paradigm.pluralSuffix.isBlank() -> null
            entry.lemma.endsWith("r") -> entry.lemma.dropLast(1) + paradigm.pluralSuffix
            else -> entry.lemma + paradigm.pluralSuffix
        }

        return inflectedForm ?: entry.lemma
    }
}

internal data class PhonologyStageOutput(
    val form: String,
    val notes: List<String>
)

internal class YoungerFutharkPhonologyStage {
    fun rewrite(text: String): PhonologyStageOutput {
        var current = text.lowercase()
        val notes = mutableListOf<String>()

        current = applyRegexRule(current, Regex("[éæ]"), "i", notes, "Reduced front vowels to i.")
        current = applyRegexRule(current, Regex("[oóǫøy]"), "u", notes, "Reduced rounded vowels to u.")
        current = applyLiteralRule(current, "ei", "i", notes, "Simplified diphthong ei.")
        current = applyLiteralRule(current, "g", "k", notes, "Applied voicing neutralization g -> k.")
        current = applyLiteralRule(current, "d", "t", notes, "Applied devoicing d -> t.")
        current = applyLiteralRule(current, "ð", "þ", notes, "Normalized ð to þ.")
        current = applyLiteralRule(current, "ll", "l", notes, "Simplified doubled consonant ll.")
        current = applyLiteralRule(current, "nn", "n", notes, "Simplified doubled consonant nn.")
        current = applyLiteralRule(current, "mm", "m", notes, "Simplified doubled consonant mm.")
        current = applyLiteralRule(current, "rr", "r", notes, "Simplified doubled consonant rr.")

        return PhonologyStageOutput(
            form = current,
            notes = notes.distinct()
        )
    }

    private fun applyRegexRule(
        value: String,
        regex: Regex,
        replacement: String,
        notes: MutableList<String>,
        note: String
    ): String {
        val replaced = value.replace(regex, replacement)
        if (replaced != value) {
            notes += note
        }
        return replaced
    }

    private fun applyLiteralRule(
        value: String,
        target: String,
        replacement: String,
        notes: MutableList<String>,
        note: String
    ): String {
        val replaced = value.replace(target, replacement)
        if (replaced != value) {
            notes += note
        }
        return replaced
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

internal data class ProtoNorseStageOutput(
    val form: String? = null,
    val notes: List<String> = emptyList(),
    val approximated: Boolean = false,
    val unresolvedToken: String? = null,
    val provenance: List<TranslationProvenanceEntry> = emptyList()
)

internal class ProtoNorseLexicalStage(
    private val lexiconLookup: HistoricalLexiconLookup
) {
    fun reconstruct(
        token: ParsedEnglishToken,
        fidelity: TranslationFidelity
    ): ProtoNorseStageOutput {
        val lexiconEntry = lexiconLookup.protoNorseFor(token.normalized, fidelity)
        val paraphrase = lexiconLookup.fallbackParaphrase(token.normalized)

        return when {
            lexiconEntry != null -> ProtoNorseStageOutput(
                form = lexiconEntry.form,
                provenance = listOf(
                    lexiconLookup.provenanceFor(
                        sourceId = lexiconEntry.sourceId,
                        detail = lexiconEntry.citations.joinToString().takeIf { it.isNotBlank() }
                    )
                )
            )

            fidelity == TranslationFidelity.STRICT -> ProtoNorseStageOutput(
                unresolvedToken = token.raw,
                notes = listOf("No strict Proto-Norse support for '${token.raw}'.")
            )

            paraphrase != null -> ProtoNorseStageOutput(
                form = paraphrase.lowercase(),
                notes = listOf("Used descriptive paraphrase for '${token.raw}'."),
                approximated = true,
                provenance = listOf(
                    lexiconLookup.provenanceFor(
                        sourceId = "internal_heuristics",
                        detail = "Readable-mode paraphrase"
                    )
                )
            )

            else -> ProtoNorseStageOutput(
                form = token.normalized,
                notes = listOf("Used phonological preservation for '${token.raw}'."),
                approximated = true,
                provenance = listOf(
                    lexiconLookup.provenanceFor(
                        sourceId = "internal_heuristics",
                        detail = "Proto-Norse preservation fallback"
                    )
                )
            )
        }
    }
}

internal data class CirthOrthographyOutput(
    val diplomatic: String? = null,
    val glyphs: String? = null,
    val notes: List<String> = emptyList(),
    val approximated: Boolean = false,
    val unresolvedToken: String? = null,
    val provenance: List<TranslationProvenanceEntry> = emptyList()
)

internal class CirthOrthographyStage(
    private val lexiconLookup: HistoricalLexiconLookup,
    private val transliterator: CirthTransliterator
) {
    fun renderToken(
        token: String,
        fidelity: TranslationFidelity
    ): CirthOrthographyOutput {
        val orthography = lexiconLookup.cirthOrthography()
        val allSequences = orthography.sequences.keys.sortedByDescending { it.length }
        val diplomaticTokens = mutableListOf<String>()
        val glyphs = StringBuilder()
        var remaining = token.lowercase()
        var matchedSequence = false
        var approximated = false
        var unresolvedToken: String? = null

        while (remaining.isNotEmpty()) {
            val sequence = allSequences.firstOrNull { remaining.startsWith(it) }
            val singleCharacter = remaining.first().toString()
            val nextGlyph = when {
                sequence != null -> {
                    matchedSequence = true
                    diplomaticTokens += sequence
                    remaining = remaining.removePrefix(sequence)
                    orthography.sequences.getValue(sequence)
                }

                orthography.singleCharacters[singleCharacter] != null -> {
                    diplomaticTokens += singleCharacter
                    remaining = remaining.drop(1)
                    orthography.singleCharacters.getValue(singleCharacter)
                }

                fidelity == TranslationFidelity.STRICT -> {
                    unresolvedToken = token
                    remaining = ""
                    null
                }

                else -> {
                    diplomaticTokens += singleCharacter
                    remaining = remaining.drop(1)
                    approximated = true
                    transliterator.transliterate(singleCharacter)
                }
            }

            nextGlyph?.let(glyphs::append)
        }

        if (unresolvedToken != null) {
            return CirthOrthographyOutput(
                unresolvedToken = unresolvedToken,
                notes = listOf("No strict Erebor transcription for '$token'.")
            )
        }

        return CirthOrthographyOutput(
            diplomatic = diplomaticTokens.joinToString("·"),
            glyphs = glyphs.toString(),
            notes = listOfNotNull(
                if (matchedSequence) "Applied Erebor cluster and diphthong rules." else null,
                if (approximated) "Used character fallback for unsupported Erebor sequence." else null
            ),
            approximated = approximated,
            provenance = listOf(
                lexiconLookup.provenanceFor(
                    sourceId = "tolkien_appendix_e",
                    detail = "Appendix E-inspired Erebor orthography"
                )
            )
        )
    }
}

internal class ElderRuneRenderer(
    private val transliterator: ElderFutharkTransliterator
) {
    fun render(text: String): String = transliterator.transliterate(text)
}

internal data class TranslationEvidenceRequest(
    val script: RunicScript,
    val historicalStage: HistoricalStage,
    val engineVersion: String,
    val requestedVariant: String? = null,
    val baseConfidence: Float,
    val fallbackStatus: TranslationResolutionStatus,
    val defaultNote: String
)

internal class TranslationEvidenceSynthesizer(
    private val datasetVersion: String
) {
    fun buildResult(
        request: TranslationRequest,
        resolutions: List<TranslationTokenResolution>,
        evidenceRequest: TranslationEvidenceRequest
    ): TranslationResult {
        val unresolvedTokens = resolutions.mapNotNull { it.unresolvedToken }.distinct()
        val provenance = resolutions.flatMap { it.provenance }.distinctBy {
            listOf(it.sourceId, it.role, it.detail, it.label)
        }
        val notes = resolutions.flatMap { it.notes }.distinct()
        val resolutionStatus = when {
            unresolvedTokens.isNotEmpty() -> TranslationResolutionStatus.UNAVAILABLE
            resolutions.any { it.approximated } -> TranslationResolutionStatus.APPROXIMATED
            else -> evidenceRequest.fallbackStatus
        }

        val confidence = when (resolutionStatus) {
            TranslationResolutionStatus.ATTESTED -> 0.98f
            TranslationResolutionStatus.RECONSTRUCTED -> evidenceRequest.baseConfidence
            TranslationResolutionStatus.APPROXIMATED -> (evidenceRequest.baseConfidence - 0.2f).coerceAtLeast(0.32f)
            TranslationResolutionStatus.UNAVAILABLE -> 0f
        }

        val availableResolutions = resolutions.filter { it.unresolvedToken == null }
        val normalizedForm = if (resolutionStatus == TranslationResolutionStatus.UNAVAILABLE) {
            ""
        } else {
            stitchTokens(availableResolutions.map { it.normalizedToken })
        }
        val diplomaticForm = if (resolutionStatus == TranslationResolutionStatus.UNAVAILABLE) {
            ""
        } else {
            stitchTokens(availableResolutions.map { it.diplomaticToken })
        }
        val glyphOutput = if (resolutionStatus == TranslationResolutionStatus.UNAVAILABLE) {
            ""
        } else {
            stitchTokens(availableResolutions.map { it.glyphToken })
        }

        return TranslationResult(
            sourceText = request.sourceText,
            script = evidenceRequest.script,
            fidelity = request.fidelity,
            historicalStage = evidenceRequest.historicalStage,
            normalizedForm = normalizedForm,
            diplomaticForm = diplomaticForm,
            glyphOutput = glyphOutput,
            requestedVariant = evidenceRequest.requestedVariant,
            resolutionStatus = resolutionStatus,
            confidence = confidence,
            notes = if (notes.isNotEmpty()) notes else listOf(evidenceRequest.defaultNote),
            unresolvedTokens = unresolvedTokens,
            provenance = provenance,
            tokenBreakdown = buildTokenBreakdown(availableResolutions),
            engineVersion = evidenceRequest.engineVersion,
            datasetVersion = datasetVersion
        )
    }
}

private fun ParsedEnglishToken.toMorphologyHints(): MorphologyHints {
    return MorphologyHints(
        isPlural = normalized.endsWith("s") && !normalized.endsWith("'s"),
        isPast = normalized.endsWith("ed"),
        isThirdPersonSingular = normalized.endsWith("s") && !normalized.endsWith("ss")
    )
}

internal fun stitchTokens(tokens: List<String>): String {
    return buildString {
        tokens.forEachIndexed { index, token ->
            if (index > 0 && token !in PUNCTUATION_TOKENS && previousCharacterNeedsSpace(lastOrNull())) {
                append(' ')
            }
            append(token)
        }
    }.trim()
}

private fun previousCharacterNeedsSpace(previous: Char?): Boolean {
    return previous != null && previous !in setOf(' ', '\n', '-', '·')
}

internal fun buildTokenBreakdown(
    resolutions: List<TranslationTokenResolution>
): List<TranslationTokenBreakdown> {
    return resolutions.map { resolution ->
        TranslationTokenBreakdown(
            sourceToken = resolution.sourceToken,
            normalizedToken = resolution.normalizedToken,
            diplomaticToken = resolution.diplomaticToken,
            glyphToken = resolution.glyphToken
        )
    }
}

internal val PUNCTUATION_TOKENS = setOf(".", ",", "!", "?", ";", ":")
