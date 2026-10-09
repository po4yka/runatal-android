package com.po4yka.runatal.domain.translation

import com.po4yka.runatal.domain.model.RunicScript
import com.po4yka.runatal.domain.transliteration.CirthTransliterator
import com.po4yka.runatal.domain.transliteration.ElderFutharkTransliterator
import com.po4yka.runatal.domain.transliteration.YoungerFutharkAlphabet

internal class TranslationGoldExampleResolver(
    private val runicCorpusStore: RunicCorpusStore
) {
    private val elderAttestation = ElderAttestationPolicy(runicCorpusStore)

    fun resolve(
        request: TranslationRequest,
        engineVersion: String
    ): TranslationResult? {
        val example = runicCorpusStore.goldExamples().firstOrNull {
            it.sourceText.normalizePhraseKey() == request.sourceText.normalizePhraseKey()
        } ?: return null

        val result = example.results.firstOrNull {
            it.script == request.script.name &&
                it.fidelity == request.fidelity.name &&
                (request.script != RunicScript.YOUNGER_FUTHARK ||
                    it.requestedVariant == null ||
                    it.requestedVariant == request.youngerVariant.name) && isEligible(request, it)
        } ?: return null

        return TranslationResult(
            sourceText = request.sourceText,
            script = request.script,
            fidelity = request.fidelity,
            derivationKind = TranslationDerivationKind.valueOf(result.derivationKind),
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
            engineVersion = engineVersion,
            datasetVersion = runicCorpusStore.datasetManifest().version
        )
    }

    private fun isEligible(request: TranslationRequest, result: TranslationGoldExampleResult): Boolean {
        if (request.script != RunicScript.ELDER_FUTHARK || request.fidelity != TranslationFidelity.STRICT) return true
        return elderAttestation.allows(
            result.normalizedForm, result.diplomaticForm, result.resolutionStatus,
            result.historicalStage, result.provenance.mapNotNull { it.referenceId }
        ) && result.glyphOutput == elderAttestation.render(result.diplomaticForm)
    }
}

internal class HistoricalSourceCatalog(
    sourceManifest: TranslationSourceManifest,
    corpusReferences: List<RunicCorpusReferenceEntry> = emptyList()
) {
    private val sourceEntries = sourceManifest.sources.associateBy { it.id }
    private val runicCorpusReferences = corpusReferences.associateBy { it.id }

    fun provenanceFor(
        sourceId: String,
        referenceId: String? = null,
        detail: String? = null
    ): TranslationProvenanceEntry {
        val source = sourceEntries[sourceId] ?: sourceEntries.getValue(INTERNAL_HEURISTICS_SOURCE_ID)
        val reference = referenceId?.let(runicCorpusReferences::get)
        return TranslationProvenanceEntry(
            sourceId = source.id,
            referenceId = referenceId,
            label = reference?.label ?: source.name,
            role = source.role,
            license = source.license,
            detail = detail ?: reference?.detail,
            url = reference?.url ?: source.url
        )
    }

    private companion object {
        const val INTERNAL_HEURISTICS_SOURCE_ID = "internal_heuristics"
    }
}

internal class HistoricalLexiconLookup(
    private val lexiconStore: HistoricalLexiconStore,
    private val sourceCatalog: HistoricalSourceCatalog
) {
    private val oldNorseEntries by lazy {
        lexiconStore.oldNorseLexicon().associateBy { it.english.lowercase() }
    }
    private val protoNorseEntries by lazy {
        lexiconStore.protoNorseLexicon().associateBy { it.english.lowercase() }
    }

    fun datasetVersion(): String = lexiconStore.datasetManifest().version

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

    fun resolveName(token: String): String? = lexiconStore.nameAdaptations().names[token]

    fun fallbackParaphrase(token: String): String? = lexiconStore.fallbackTemplates().paraphrases[token]

    fun grammarRules(): GrammarRulesData = lexiconStore.grammarRules()

    fun paradigmTables(): ParadigmTablesData = lexiconStore.paradigmTables()

    fun provenanceFor(entry: OldNorseLexiconEntry): TranslationProvenanceEntry {
        return sourceCatalog.provenanceFor(
            sourceId = entry.sourceId,
            referenceId = entry.id,
            detail = entry.citations.joinToString().takeIf { it.isNotBlank() }
        )
    }

    fun provenanceFor(entry: ProtoNorseLexiconEntry): TranslationProvenanceEntry {
        return sourceCatalog.provenanceFor(
            sourceId = entry.sourceId,
            referenceId = entry.id,
            detail = entry.citations.joinToString().takeIf { it.isNotBlank() }
        )
    }

    fun provenanceFor(
        sourceId: String,
        referenceId: String? = null,
        detail: String? = null
    ): TranslationProvenanceEntry = sourceCatalog.provenanceFor(sourceId, referenceId, detail)

    private fun resolveSynonym(token: String): String =
        lexiconStore.fallbackTemplates().synonyms[token] ?: token
}

internal class RunicPhraseTemplateResolver(
    private val runicCorpusStore: RunicCorpusStore,
    private val sourceCatalog: HistoricalSourceCatalog
) {
    fun resolveElder(
        request: TranslationRequest,
        renderer: ElderRuneRenderer
    ): TranslationResult? {
        val template = findTemplate(request, runicCorpusStore.elderAttestedForms()) ?: return null
        val policy = ElderAttestationPolicy(runicCorpusStore)
        if (request.fidelity == TranslationFidelity.STRICT && !policy.allows(
                template.normalizedForm, template.diplomaticForm, template.resolutionStatus,
                template.historicalStage, template.referenceIds
            )) return null
        return template.toTranslationResult(
            request = request,
            script = RunicScript.ELDER_FUTHARK,
            datasetVersion = runicCorpusStore.datasetManifest().version,
            engineVersion = "ef-template-v3"
        ) { token ->
            if (template.resolutionStatus == TranslationResolutionStatus.ATTESTED.name) policy.render(token)
            else renderer.render(token)
        }
    }

    private fun findTemplate(
        request: TranslationRequest,
        templates: List<HistoricalPhraseTemplateEntry>
    ): HistoricalPhraseTemplateEntry? {
        val candidates = templates.filter {
            it.script == request.script.name &&
                it.sourceText.normalizePhraseKey() == request.sourceText.normalizePhraseKey()
        }
        return candidates.firstOrNull { it.fidelity == request.fidelity.name }
            ?: candidates.firstOrNull { it.fidelity == TranslationFidelity.STRICT.name }
    }

    private fun HistoricalPhraseTemplateEntry.toTranslationResult(
        request: TranslationRequest,
        script: RunicScript,
        datasetVersion: String,
        engineVersion: String,
        glyphRenderer: (String) -> String
    ): TranslationResult {
        val provenance = referenceIds.map { referenceId ->
            sourceCatalog.provenanceFor(
                sourceId = runicCorpusStore.runicCorpusReferences()
                    .firstOrNull { it.id == referenceId }
                    ?.sourceId
                    ?: "internal_heuristics",
                referenceId = referenceId
            )
        }
        val breakdown = tokenBreakdown.map { token ->
            val tokenProvenance = token.referenceIds.map { referenceId ->
                sourceCatalog.provenanceFor(
                    sourceId = runicCorpusStore.runicCorpusReferences()
                        .firstOrNull { it.id == referenceId }
                        ?.sourceId
                        ?: "internal_heuristics",
                    referenceId = referenceId
                )
            }
            TranslationTokenBreakdown(
                sourceToken = token.sourceToken,
                normalizedToken = token.normalizedToken,
                diplomaticToken = token.diplomaticToken,
                glyphToken = glyphRenderer(token.diplomaticToken),
                resolutionStatus = TranslationResolutionStatus.valueOf(token.resolutionStatus),
                provenance = tokenProvenance
            )
        }
        return TranslationResult(
            sourceText = request.sourceText,
            script = script,
            fidelity = request.fidelity,
            derivationKind = TranslationDerivationKind.valueOf(derivationKind),
            historicalStage = HistoricalStage.valueOf(historicalStage),
            normalizedForm = normalizedForm,
            diplomaticForm = diplomaticForm,
            glyphOutput = glyphRenderer(diplomaticForm),
            requestedVariant = if (script == RunicScript.YOUNGER_FUTHARK) request.youngerVariant.name else null,
            resolutionStatus = TranslationResolutionStatus.valueOf(resolutionStatus),
            confidence = confidenceFor(TranslationResolutionStatus.valueOf(resolutionStatus)),
            notes = notes,
            unresolvedTokens = emptyList(),
            provenance = provenance,
            tokenBreakdown = breakdown,
            engineVersion = engineVersion,
            datasetVersion = datasetVersion
        )
    }
}

internal data class TranslationTokenResolution(
    val sourceToken: String,
    val normalizedToken: String,
    val diplomaticToken: String,
    val glyphToken: String,
    val resolutionStatus: TranslationResolutionStatus,
    val notes: List<String> = emptyList(),
    val unresolvedToken: String? = null,
    val provenance: List<TranslationProvenanceEntry> = emptyList(),
    val isPunctuation: Boolean = false
)

internal data class PhonologyStageOutput(
    val form: String,
    val notes: List<String>
)

internal class YoungerFutharkPhonologyStage {
    fun rewrite(text: String): PhonologyStageOutput {
        var current = text.lowercase()
        val notes = mutableListOf<String>()

        current = applyDiphthongHandling(current, notes)
        current = applyFrontVowelReduction(current, notes)
        current = applyRoundedVowelReduction(current, notes)
        current = applyVoicingAndDevoicing(current, notes)
        current = applyGeminateSimplification(current, notes)

        return PhonologyStageOutput(
            form = current,
            notes = notes.distinct()
        )
    }

    private fun applyFrontVowelReduction(
        value: String,
        notes: MutableList<String>
    ): String {
        var current = value
        current = applyRegexRule(
            value = current,
            regex = Regex("[eéæí]"),
            replacement = "i",
            notes = notes,
            note = "Applied front-vowel reduction group."
        )
        current = applyLiteralRule(
            value = current,
            target = "ja",
            replacement = "ia",
            notes = notes,
            note = "Normalized glide-plus-vowel spelling for Younger Futhark."
        )
        current = applyLiteralRule(current, "á", "a", notes, "Reduced acute a to its runic vowel class.")
        return current
    }

    private fun applyRoundedVowelReduction(
        value: String,
        notes: MutableList<String>
    ): String {
        return applyRegexRule(
            value = value,
            regex = Regex("[oóǫøyýú]"),
            replacement = "u",
            notes = notes,
            note = "Applied rounded-vowel reduction group."
        )
    }

    private fun applyDiphthongHandling(
        value: String,
        notes: MutableList<String>
    ): String {
        var current = value
        current = applyLiteralRule(current, "ei", "i", notes, "Applied diphthong handling group.")
        current = applyLiteralRule(current, "ey", "y", notes, "Applied diphthong handling group.")
        return current
    }

    private fun applyVoicingAndDevoicing(
        value: String,
        notes: MutableList<String>
    ): String {
        var current = value
        current = applyLiteralRule(current, "g", "k", notes, "Applied voicing-neutralization group.")
        current = applyLiteralRule(current, "d", "t", notes, "Applied devoicing group.")
        current = applyLiteralRule(current, "ð", "þ", notes, "Normalized eth to thorn.")
        current = applyLiteralRule(current, "v", "u", notes, "Reduced v to its runic vowel class.")
        return current
    }

    private fun applyGeminateSimplification(
        value: String,
        notes: MutableList<String>
    ): String {
        var current = value
        current = applyLiteralRule(current, "ll", "l", notes, "Applied geminate-simplification group.")
        current = applyLiteralRule(current, "nn", "n", notes, "Applied geminate-simplification group.")
        current = applyLiteralRule(current, "mm", "m", notes, "Applied geminate-simplification group.")
        current = applyLiteralRule(current, "rr", "r", notes, "Applied geminate-simplification group.")
        return current
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
    fun render(text: String, variant: YoungerFutharkVariant): String = YoungerFutharkAlphabet.render(text, variant)
}

internal data class ProtoNorseStageOutput(
    val historicalStage: HistoricalStage,
    val form: String? = null,
    val notes: List<String> = emptyList(),
    val resolutionStatus: TranslationResolutionStatus = TranslationResolutionStatus.RECONSTRUCTED,
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
        return when {
            lexiconEntry != null -> ProtoNorseStageOutput(
                historicalStage = HistoricalStage.PROTO_NORSE,
                form = lexiconEntry.form,
                resolutionStatus = if (lexiconEntry.strictEligible) {
                    TranslationResolutionStatus.RECONSTRUCTED
                } else {
                    TranslationResolutionStatus.APPROXIMATED
                },
                provenance = listOf(lexiconLookup.provenanceFor(lexiconEntry))
            )

            fidelity == TranslationFidelity.STRICT -> ProtoNorseStageOutput(
                historicalStage = HistoricalStage.PROTO_NORSE,
                unresolvedToken = token.raw,
                notes = listOf("Missing attested or reconstructed Elder Futhark pattern for '${token.raw}'.")
            )

            else -> ProtoNorseStageOutput(
                historicalStage = HistoricalStage.PRESERVED_SOURCE,
                form = token.normalized,
                notes = listOf("Preserved source spelling for '${token.raw}'; no Proto-Norse lemma is available."),
                resolutionStatus = TranslationResolutionStatus.APPROXIMATED,
                provenance = listOf(
                    lexiconLookup.provenanceFor(
                        sourceId = "internal_heuristics",
                        detail = "Source preservation without a Proto-Norse reconstruction"
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
    val resolutionStatus: TranslationResolutionStatus = TranslationResolutionStatus.RECONSTRUCTED,
    val unresolvedToken: String? = null,
    val provenance: List<TranslationProvenanceEntry> = emptyList()
)

internal class CirthOrthographyStage(
    private val ereborStore: EreborOrthographyStore,
    private val sourceCatalog: HistoricalSourceCatalog,
    private val transliterator: CirthTransliterator
) {
    fun resolvePhrase(request: TranslationRequest): TranslationResult? {
        val mapping = ereborStore.ereborTables().phraseMappings.firstOrNull {
            it.sourceText.normalizePhraseKey() == request.sourceText.normalizePhraseKey()
        } ?: return null

        return TranslationResult(
            sourceText = request.sourceText,
            script = RunicScript.CIRTH,
            fidelity = request.fidelity,
            derivationKind = TranslationDerivationKind.PHRASE_TEMPLATE,
            historicalStage = HistoricalStage.EREBOR_ENGLISH,
            normalizedForm = request.sourceText.lowercase(),
            diplomaticForm = mapping.diplomaticForm,
            glyphOutput = mapping.glyphOutput,
            resolutionStatus = TranslationResolutionStatus.valueOf(mapping.resolutionStatus),
            confidence = confidenceFor(TranslationResolutionStatus.valueOf(mapping.resolutionStatus)),
            notes = mapping.notes,
            unresolvedTokens = emptyList(),
            provenance = mapping.referenceIds.map { referenceId ->
                sourceCatalog.provenanceFor(
                    sourceId = ereborStore.sourceManifest().sources
                        .firstOrNull { source -> source.id == "tolkien_appendix_e" }
                        ?.id
                        ?: "internal_heuristics",
                    referenceId = referenceId
                )
            },
            tokenBreakdown = emptyList(),
            engineVersion = "cirth-phrase-v3",
            datasetVersion = ereborStore.datasetManifest().version
        )
    }

    fun renderToken(
        token: String,
        fidelity: TranslationFidelity
    ): CirthOrthographyOutput {
        val tables = ereborStore.ereborTables()
        val sequenceMappings = linkedMapOf<String, String>().apply {
            putAll(tables.longConsonants)
            putAll(tables.longVowels)
            putAll(tables.sequences)
        }
        val allSequences = sequenceMappings.keys.sortedByDescending { it.length }
        val diplomaticTokens = mutableListOf<String>()
        val glyphs = StringBuilder()
        var remaining = token.lowercase()
        var appliedCanonicalRule = false
        var approximated = false
        var unresolvedToken: String? = null

        while (remaining.isNotEmpty()) {
            val sequence = allSequences.firstOrNull { remaining.startsWith(it) }
            val singleCharacter = remaining.first().toString()
            val nextGlyph = when {
                sequence != null -> {
                    appliedCanonicalRule = true
                    diplomaticTokens += sequence
                    remaining = remaining.removePrefix(sequence)
                    sequenceMappings.getValue(sequence)
                }

                tables.singleCharacters[singleCharacter] != null -> {
                    diplomaticTokens += singleCharacter
                    remaining = remaining.drop(1)
                    tables.singleCharacters.getValue(singleCharacter)
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
                notes = listOf("Unsupported Erebor sequence in '$token'.")
            )
        }

        return CirthOrthographyOutput(
            diplomatic = diplomaticTokens.joinToString(tables.wordSeparator),
            glyphs = glyphs.toString(),
            notes = listOfNotNull(
                if (appliedCanonicalRule) "Applied Erebor sequence-table transcription." else null,
                if (approximated) "Used readable-mode character fallback for an unsupported Erebor sequence." else null
            ),
            resolutionStatus = if (approximated) {
                TranslationResolutionStatus.APPROXIMATED
            } else {
                TranslationResolutionStatus.RECONSTRUCTED
            },
            provenance = listOf(
                sourceCatalog.provenanceFor(
                    sourceId = "tolkien_appendix_e",
                    detail = "Erebor orthography table"
                )
            )
        )
    }
}

internal class ElderRuneRenderer(
    private val transliterator: ElderFutharkTransliterator
) {
    fun render(text: String): String {
        val spelling = text.map { character ->
            when (character) {
                'ą', 'ā' -> 'a'
                'ę', 'ē' -> 'e'
                'į', 'ī' -> 'i'
                'ǫ', 'ǭ', 'ō' -> 'o'
                'ų', 'ū' -> 'u'
                else -> character
            }
        }.joinToString("")
        return transliterator.transliterate(spelling)
    }
}

internal data class TranslationEvidenceRequest(
    val script: RunicScript,
    val derivationKind: TranslationDerivationKind,
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
        val hasContent = resolutions.any { !it.isPunctuation && it.sourceToken.hasVisibleContent() }
        val unresolvedTokens = (resolutions.mapNotNull { it.unresolvedToken } +
            if (hasContent) emptyList() else resolutions.filter { !it.isPunctuation }.map { it.sourceToken }).distinct()
        val provenance = resolutions.flatMap { it.provenance }.distinctBy {
            listOf(it.sourceId, it.referenceId, it.role, it.detail, it.label)
        }
        val notes = (resolutions.flatMap { it.notes } +
            if (hasContent) emptyList() else listOf("No visible translatable source content was found.")).distinct()
        val resolutionStatus = when {
            !hasContent || unresolvedTokens.isNotEmpty() -> TranslationResolutionStatus.UNAVAILABLE
            resolutions.any { it.resolutionStatus == TranslationResolutionStatus.APPROXIMATED } ->
                TranslationResolutionStatus.APPROXIMATED
            else -> evidenceRequest.fallbackStatus
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
            derivationKind = evidenceRequest.derivationKind,
            historicalStage = evidenceRequest.historicalStage,
            normalizedForm = normalizedForm,
            diplomaticForm = diplomaticForm,
            glyphOutput = glyphOutput,
            requestedVariant = evidenceRequest.requestedVariant,
            resolutionStatus = resolutionStatus,
            confidence = confidenceFor(resolutionStatus, evidenceRequest.baseConfidence),
            notes = if (notes.isNotEmpty()) notes else listOf(evidenceRequest.defaultNote),
            unresolvedTokens = unresolvedTokens,
            provenance = provenance,
            tokenBreakdown = buildTokenBreakdown(availableResolutions),
            engineVersion = evidenceRequest.engineVersion,
            datasetVersion = datasetVersion
        )
    }
}

private fun confidenceFor(
    resolutionStatus: TranslationResolutionStatus,
    baseConfidence: Float = 0.9f
): Float {
    return when (resolutionStatus) {
        TranslationResolutionStatus.ATTESTED -> ATTESTED_CONFIDENCE
        TranslationResolutionStatus.RECONSTRUCTED -> baseConfidence
        TranslationResolutionStatus.APPROXIMATED -> {
            (baseConfidence - APPROXIMATION_PENALTY).coerceAtLeast(MIN_APPROXIMATION_CONFIDENCE)
        }
        TranslationResolutionStatus.UNAVAILABLE -> 0f
    }
}

private fun String.normalizePhraseKey(): String {
    return lowercase()
        .trim()
        .replace(Regex("\\s+"), " ")
}

internal fun stitchTokens(tokens: List<String>): String {
    return buildString {
        tokens.filter { it.isNotEmpty() }.forEachIndexed { index, token ->
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
            glyphToken = resolution.glyphToken,
            resolutionStatus = resolution.resolutionStatus,
            provenance = resolution.provenance
        )
    }
}

internal val PUNCTUATION_TOKENS = setOf(".", ",", "!", "?", ";", ":")

private const val ATTESTED_CONFIDENCE = 0.98f
private const val APPROXIMATION_PENALTY = 0.18f
private const val MIN_APPROXIMATION_CONFIDENCE = 0.3f

private fun String.hasVisibleContent(): Boolean = codePoints().anyMatch { codePoint ->
    Character.getType(codePoint) !in invisibleSourceTypes
}

private val invisibleSourceTypes = setOf(
    Character.CONTROL.toInt(),
    Character.FORMAT.toInt(),
    Character.NON_SPACING_MARK.toInt(),
    Character.COMBINING_SPACING_MARK.toInt(),
    Character.ENCLOSING_MARK.toInt(),
    Character.SPACE_SEPARATOR.toInt(),
    Character.LINE_SEPARATOR.toInt(),
    Character.PARAGRAPH_SEPARATOR.toInt()
)
