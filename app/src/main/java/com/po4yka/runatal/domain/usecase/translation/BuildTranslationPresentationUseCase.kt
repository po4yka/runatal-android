package com.po4yka.runatal.domain.usecase.translation

import com.po4yka.runatal.domain.model.RunicScript
import com.po4yka.runatal.domain.model.displayName
import com.po4yka.runatal.domain.translation.TranslationDerivationKind
import com.po4yka.runatal.domain.translation.TranslationFidelity
import com.po4yka.runatal.domain.translation.TranslationMode
import com.po4yka.runatal.domain.translation.TranslationProvenanceEntry
import com.po4yka.runatal.domain.translation.TranslationResolutionStatus
import com.po4yka.runatal.domain.translation.TranslationResult
import com.po4yka.runatal.domain.translation.TranslationTokenBreakdown
import com.po4yka.runatal.domain.translation.YoungerFutharkVariant
import com.po4yka.runatal.domain.transliteration.WordTransliterationPair
import javax.inject.Inject

internal data class TranslationPreferencesSnapshot(
    val selectedScript: RunicScript,
    val selectedFont: String,
    val persistedWordByWordEnabled: Boolean,
    val translationMode: TranslationMode,
    val fidelity: TranslationFidelity,
    val youngerVariant: YoungerFutharkVariant
)

internal data class TranslationInputSnapshot(
    val inputText: String,
    val isSaving: Boolean,
    val localWordByWordOverride: Boolean?
)

internal data class TranslationPresentation(
    val inputText: String,
    val transliteratedText: String,
    val selectedScript: RunicScript,
    val translationMode: TranslationMode,
    val selectedFidelity: TranslationFidelity,
    val selectedYoungerVariant: YoungerFutharkVariant,
    val selectedFont: String,
    val translateFeatureEnabled: Boolean,
    val outputGlyphCount: Int,
    val inputCharacterCount: Int,
    val canSave: Boolean,
    val isSaving: Boolean,
    val errorMessage: String?,
    val wordByWordEnabled: Boolean,
    val wordBreakdown: List<WordTransliterationPair>,
    val normalizedForm: String,
    val diplomaticForm: String,
    val confidence: Float?,
    val notes: List<String>,
    val tokenBreakdown: List<TranslationTokenBreakdown>,
    val resolutionStatus: TranslationResolutionStatus?,
    val unresolvedTokens: List<String>,
    val provenance: List<TranslationProvenanceEntry>,
    val fallbackSuggestion: String?,
    val translationTrackLabel: String,
    val derivationKindLabel: String,
    val unavailableExplanation: String?
)

internal class BuildTranslationPresentationUseCase @Inject constructor(
    private val buildTransliterationBundleUseCase: BuildTransliterationBundleUseCase,
    private val buildHistoricalTranslationBundleUseCase: BuildHistoricalTranslationBundleUseCase
) {

    suspend operator fun invoke(
        preferences: TranslationPreferencesSnapshot,
        input: TranslationInputSnapshot,
        translateFeatureEnabled: Boolean
    ): TranslationPresentation {
        val selectedScript = setOf(preferences.selectedScript)
        val transliterationBundle = buildTransliterationBundleUseCase(
            inputText = input.inputText,
            scripts = selectedScript
        )
        val effectiveMode = if (translateFeatureEnabled) {
            preferences.translationMode
        } else {
            TranslationMode.TRANSLITERATE
        }
        val isHistoricalTranslation = effectiveMode == TranslationMode.TRANSLATE
        val translationBundle = if (translateFeatureEnabled && isHistoricalTranslation) {
            buildHistoricalTranslationBundleUseCase(
                inputText = input.inputText,
                fidelity = preferences.fidelity,
                youngerVariant = preferences.youngerVariant,
                scripts = selectedScript
            )
        } else {
            HistoricalTranslationBundle()
        }

        val selectedOutput = if (isHistoricalTranslation) {
            translationBundle.outputFor(preferences.selectedScript)
        } else {
            transliterationBundle.outputFor(preferences.selectedScript)
        }
        val selectedBreakdown = if (isHistoricalTranslation) {
            translationBundle.resultFor(preferences.selectedScript)?.tokenBreakdown.orEmpty()
        } else {
            transliterationBundle.breakdownFor(preferences.selectedScript).wordPairs.toTranslationBreakdown()
        }
        val wordByWordEnabled = input.localWordByWordOverride ?: preferences.persistedWordByWordEnabled
        val selectedTranslationResult = translationBundle.resultFor(preferences.selectedScript)
        val isTranslationAvailable =
            selectedTranslationResult?.resolutionStatus != TranslationResolutionStatus.UNAVAILABLE
        val errorMessage = if (isHistoricalTranslation) {
            translationBundle.errorMessage
        } else {
            transliterationBundle.errorMessage
        }
        val canSaveTranslation = !isHistoricalTranslation ||
            (selectedTranslationResult != null && isTranslationAvailable && selectedOutput.isNotBlank())

        return TranslationPresentation(
            inputText = input.inputText,
            transliteratedText = selectedOutput,
            selectedScript = preferences.selectedScript,
            translationMode = effectiveMode,
            selectedFidelity = preferences.fidelity,
            selectedYoungerVariant = preferences.youngerVariant,
            selectedFont = preferences.selectedFont,
            translateFeatureEnabled = translateFeatureEnabled,
            outputGlyphCount = selectedOutput.glyphCount(),
            inputCharacterCount = input.inputText.length,
            canSave = input.inputText.trim().isNotEmpty() &&
                !input.isSaving &&
                errorMessage == null &&
                canSaveTranslation,
            isSaving = input.isSaving,
            errorMessage = errorMessage,
            wordByWordEnabled = wordByWordEnabled,
            wordBreakdown = selectedBreakdown.toWordPairs(),
            normalizedForm = selectedTranslationResult?.normalizedForm.orEmpty(),
            diplomaticForm = selectedTranslationResult?.diplomaticForm.orEmpty(),
            confidence = selectedTranslationResult?.confidence,
            notes = selectedTranslationResult?.notes.orEmpty(),
            tokenBreakdown = selectedBreakdown,
            resolutionStatus = selectedTranslationResult?.resolutionStatus,
            unresolvedTokens = selectedTranslationResult?.unresolvedTokens.orEmpty(),
            provenance = selectedTranslationResult?.provenance.orEmpty(),
            fallbackSuggestion = selectedTranslationResult.fallbackSuggestion(preferences.fidelity),
            translationTrackLabel = selectedTranslationResult.translationTrackLabel(preferences.selectedScript),
            derivationKindLabel = selectedTranslationResult.derivationKindLabel(),
            unavailableExplanation = selectedTranslationResult.unavailableExplanation(preferences.selectedScript)
        )
    }
}

private fun TranslationResult?.fallbackSuggestion(fidelity: TranslationFidelity): String? {
    if (this == null || resolutionStatus != TranslationResolutionStatus.UNAVAILABLE) {
        return null
    }
    return when (fidelity) {
        TranslationFidelity.STRICT -> "Try Readable or Decorative for a best-effort result."
        TranslationFidelity.READABLE -> "Try Decorative for a looser approximation."
        TranslationFidelity.DECORATIVE -> null
    }
}

private fun TranslationResult?.translationTrackLabel(script: RunicScript): String {
    if (script == RunicScript.CIRTH) {
        return "Erebor transcription"
    }
    return script.displayName
}

private fun TranslationResult?.derivationKindLabel(): String {
    return when (this?.derivationKind) {
        TranslationDerivationKind.GOLD_EXAMPLE -> "Gold example"
        TranslationDerivationKind.PHRASE_TEMPLATE -> "Phrase template"
        TranslationDerivationKind.TOKEN_COMPOSED -> "Token composed"
        TranslationDerivationKind.SEQUENCE_TRANSCRIPTION -> "Sequence transcription"
        null -> ""
    }
}

private fun TranslationResult?.unavailableExplanation(script: RunicScript): String? {
    if (this == null || resolutionStatus != TranslationResolutionStatus.UNAVAILABLE) {
        return null
    }

    return when {
        script == RunicScript.CIRTH -> "Unsupported Erebor sequence or phrase mapping for strict mode."
        notes.any { it.contains("attested or reconstructed Elder Futhark pattern", ignoreCase = true) } ->
            "Missing attested or reconstructed Elder Futhark pattern."

        unresolvedTokens.isNotEmpty() -> "Missing lemma coverage for: ${unresolvedTokens.joinToString(", ")}."
        else -> notes.firstOrNull() ?: "No defensible strict result is available."
    }
}
