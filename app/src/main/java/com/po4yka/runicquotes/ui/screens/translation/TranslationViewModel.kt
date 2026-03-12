package com.po4yka.runicquotes.ui.screens.translation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.po4yka.runicquotes.BuildConfig
import com.po4yka.runicquotes.data.preferences.UserPreferencesManager
import com.po4yka.runicquotes.data.repository.QuoteRepository
import com.po4yka.runicquotes.data.repository.TranslationRepository
import com.po4yka.runicquotes.domain.model.Quote
import com.po4yka.runicquotes.domain.model.RunicScript
import com.po4yka.runicquotes.domain.model.displayName
import com.po4yka.runicquotes.domain.transliteration.TransliterationBreakdown
import com.po4yka.runicquotes.domain.transliteration.TransliterationFactory
import com.po4yka.runicquotes.domain.translation.HistoricalTranslationService
import com.po4yka.runicquotes.domain.translation.TranslationFidelity
import com.po4yka.runicquotes.domain.translation.TranslationMode
import com.po4yka.runicquotes.domain.translation.TranslationProvenanceEntry
import com.po4yka.runicquotes.domain.translation.TranslationResolutionStatus
import com.po4yka.runicquotes.domain.translation.TranslationResult
import com.po4yka.runicquotes.domain.translation.TranslationTokenBreakdown
import com.po4yka.runicquotes.domain.translation.YoungerFutharkVariant
import com.po4yka.runicquotes.domain.transliteration.WordTransliterationPair
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

/**
 * ViewModel for the translation screen, including saving transliterations to the library.
 */
@HiltViewModel
internal class TranslationViewModel @Inject constructor(
    private val transliterationFactory: TransliterationFactory,
    private val historicalTranslationService: HistoricalTranslationService,
    private val quoteRepository: QuoteRepository,
    private val translationRepository: TranslationRepository,
    private val userPreferencesManager: UserPreferencesManager
) : ViewModel() {

    private val _inputText = MutableStateFlow("")
    private val _selectedScript = MutableStateFlow(RunicScript.DEFAULT)
    private val _selectedFont = MutableStateFlow("noto")
    private val _translationMode = MutableStateFlow(TranslationMode.DEFAULT)
    private val _selectedFidelity = MutableStateFlow(TranslationFidelity.DEFAULT)
    private val _selectedYoungerVariant = MutableStateFlow(YoungerFutharkVariant.DEFAULT)
    private val _isSaving = MutableStateFlow(false)
    private val _feedbackMessage = MutableStateFlow<String?>(null)
    private val _persistedWordByWordEnabled = MutableStateFlow(false)
    private val _localWordByWordOverride = MutableStateFlow<Boolean?>(null)
    private val translateFeatureEnabled = BuildConfig.ENABLE_EXPERIMENTAL_TRANSLATE

    /** @suppress */
    companion object {
        private const val TAG = "TranslationViewModel"
        private const val MAX_INPUT_LENGTH = 280
        private const val DEFAULT_TRANSLATION_AUTHOR = "Runatal"
    }

    init {
        viewModelScope.launch {
            userPreferencesManager.userPreferencesFlow.collectLatest { preferences ->
                _selectedScript.value = preferences.selectedScript
                _selectedFont.value = preferences.selectedFont
                _persistedWordByWordEnabled.value = preferences.wordByWordTransliterationEnabled
            }
        }
    }

    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    val uiState: StateFlow<TranslationUiState> = combine(
        combine(
            combine(
                _selectedScript,
                _selectedFont,
                _persistedWordByWordEnabled
            ) { selectedScript, selectedFont, persistedWordByWordEnabled ->
                TranslationPreferenceBasics(
                    selectedScript = selectedScript,
                    selectedFont = selectedFont,
                    persistedWordByWordEnabled = persistedWordByWordEnabled
                )
            },
            combine(
                _translationMode,
                _selectedFidelity,
                _selectedYoungerVariant
            ) { mode, fidelity, youngerVariant ->
                TranslationModePreferences(
                    translationMode = mode,
                    fidelity = fidelity,
                    youngerVariant = youngerVariant
                )
            }
        ) { basics, modePreferences ->
            TranslationPreferencesState(
                selectedScript = basics.selectedScript,
                selectedFont = basics.selectedFont,
                persistedWordByWordEnabled = basics.persistedWordByWordEnabled,
                translationMode = modePreferences.translationMode,
                fidelity = modePreferences.fidelity,
                youngerVariant = modePreferences.youngerVariant
            )
        },
        combine(
            _inputText,
            _isSaving,
            _localWordByWordOverride
        ) { inputText, isSaving, localWordByWordOverride ->
            TranslationInputState(
                inputText = inputText,
                isSaving = isSaving,
                localWordByWordOverride = localWordByWordOverride
            )
        }
    ) { preferences, inputState ->
        val transliterationBundle = transliterateBundle(inputState.inputText)
        val effectiveMode = if (translateFeatureEnabled) {
            preferences.translationMode
        } else {
            TranslationMode.TRANSLITERATE
        }
        val isHistoricalTranslation = effectiveMode == TranslationMode.TRANSLATE
        val translationBundle = if (translateFeatureEnabled && isHistoricalTranslation) {
            translateBundle(
                inputText = inputState.inputText,
                fidelity = preferences.fidelity,
                youngerVariant = preferences.youngerVariant
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
        val wordByWordEnabled =
            inputState.localWordByWordOverride ?: preferences.persistedWordByWordEnabled
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

        TranslationUiState(
            inputText = inputState.inputText,
            transliteratedText = selectedOutput,
            selectedScript = preferences.selectedScript,
            translationMode = effectiveMode,
            selectedFidelity = preferences.fidelity,
            selectedYoungerVariant = preferences.youngerVariant,
            selectedFont = preferences.selectedFont,
            outputGlyphCount = selectedOutput.glyphCount(),
            inputCharacterCount = inputState.inputText.length,
            canSave = inputState.inputText.trim().isNotEmpty() &&
                !inputState.isSaving &&
                errorMessage == null &&
                canSaveTranslation,
            isSaving = inputState.isSaving,
            errorMessage = errorMessage,
            wordByWordEnabled = wordByWordEnabled,
            wordBreakdown = selectedBreakdown.toWordPairs(),
            normalizedForm = selectedTranslationResult?.normalizedForm.orEmpty(),
            diplomaticForm = selectedTranslationResult?.diplomaticForm.orEmpty(),
            translateFeatureEnabled = translateFeatureEnabled,
            confidence = selectedTranslationResult?.confidence,
            notes = selectedTranslationResult?.notes.orEmpty(),
            tokenBreakdown = selectedBreakdown,
            resolutionStatus = selectedTranslationResult?.resolutionStatus,
            unresolvedTokens = selectedTranslationResult?.unresolvedTokens.orEmpty(),
            provenance = selectedTranslationResult?.provenance.orEmpty(),
            fallbackSuggestion = selectedTranslationResult?.fallbackSuggestion(preferences.fidelity),
            translationTrackLabel = selectedTranslationResult.translationTrackLabel(preferences.selectedScript)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = TranslationUiState(translateFeatureEnabled = translateFeatureEnabled)
    )

    /**
     * Updates the source text with the Figma-defined 280 character cap.
     */
    fun updateInputText(text: String) {
        _inputText.value = text.take(MAX_INPUT_LENGTH)
    }

    /**
     * Switches between legacy transliteration and structured translation.
     */
    fun selectMode(mode: TranslationMode) {
        _translationMode.value = if (translateFeatureEnabled) {
            mode
        } else {
            TranslationMode.TRANSLITERATE
        }
    }

    /**
     * Selects the translation fidelity level.
     */
    fun selectFidelity(fidelity: TranslationFidelity) {
        _selectedFidelity.value = fidelity
    }

    /**
     * Selects the Younger Futhark rendering variant for historical translation mode.
     */
    fun selectYoungerVariant(variant: YoungerFutharkVariant) {
        _selectedYoungerVariant.value = variant
    }

    /**
     * Selects the active output script and persists it as the preferred script.
     */
    fun selectScript(script: RunicScript) {
        _selectedScript.value = script
        viewModelScope.launch {
            userPreferencesManager.updateSelectedScript(script)
        }
    }

    /**
     * Rotates to the next script. Used by the compact Figma quick action.
     */
    fun cycleScript() {
        val scripts = RunicScript.entries
        val nextIndex = (scripts.indexOf(_selectedScript.value) + 1) % scripts.size
        selectScript(scripts[nextIndex])
    }

    /**
     * Clears the entered source text.
     */
    fun clearInput() {
        _inputText.value = ""
    }

    /**
     * Toggles the local word-by-word transliteration presentation for the current screen session.
     */
    fun toggleWordByWordMode() {
        _localWordByWordOverride.value =
            !(_localWordByWordOverride.value ?: _persistedWordByWordEnabled.value)
    }

    /**
     * Saves the current transliteration bundle as a user-created quote.
     */
    fun saveToLibrary() {
        val state = uiState.value
        if (!state.canSave) {
            return
        }

        viewModelScope.launch {
            _isSaving.value = true
            try {
                val input = state.inputText.trim()
                val transliterationBundle = transliterateBundle(input)
                val historicalBundle = translateBundle(
                    inputText = input,
                    fidelity = state.selectedFidelity,
                    youngerVariant = state.selectedYoungerVariant
                )
                val quote = Quote(
                    id = 0L,
                    textLatin = input,
                    author = DEFAULT_TRANSLATION_AUTHOR,
                    runicElder = if (state.translationMode == TranslationMode.TRANSLATE) {
                        historicalBundle.outputFor(RunicScript.ELDER_FUTHARK)
                    } else {
                        transliterationBundle.outputFor(RunicScript.ELDER_FUTHARK)
                    },
                    runicYounger = if (state.translationMode == TranslationMode.TRANSLATE) {
                        historicalBundle.outputFor(RunicScript.YOUNGER_FUTHARK)
                    } else {
                        transliterationBundle.outputFor(RunicScript.YOUNGER_FUTHARK)
                    },
                    runicCirth = if (state.translationMode == TranslationMode.TRANSLATE) {
                        historicalBundle.outputFor(RunicScript.CIRTH)
                    } else {
                        transliterationBundle.outputFor(RunicScript.CIRTH)
                    },
                    isUserCreated = true,
                    isFavorite = false,
                    createdAt = System.currentTimeMillis()
                )

                val quoteId = quoteRepository.saveUserQuote(quote)
                if (state.translationMode == TranslationMode.TRANSLATE) {
                    translationRepository.cacheTranslations(
                        quoteId = quoteId,
                        results = historicalBundle.results(),
                        isBackfilled = false
                    )
                    _feedbackMessage.value = "Saved translation to library"
                } else {
                    _feedbackMessage.value = "Saved to library"
                }
            } catch (exception: IOException) {
                Log.e(TAG, "IO error saving translation", exception)
                _feedbackMessage.value = "Failed to save translation"
            } catch (exception: IllegalStateException) {
                Log.e(TAG, "Invalid state saving translation", exception)
                _feedbackMessage.value = "Invalid translation state"
            } finally {
                _isSaving.value = false
            }
        }
    }

    /**
     * Clears the transient snackbar feedback message after it is shown.
     */
    fun clearFeedback() {
        _feedbackMessage.value = null
    }

    @Suppress("TooGenericExceptionCaught")
    private fun transliterateBundle(inputText: String): TransliterationBundle {
        if (inputText.isBlank()) {
            return TransliterationBundle()
        }

        return try {
            TransliterationBundle(
                elder = transliterationFactory.transliterate(inputText, RunicScript.ELDER_FUTHARK),
                younger = transliterationFactory.transliterate(inputText, RunicScript.YOUNGER_FUTHARK),
                cirth = transliterationFactory.transliterate(inputText, RunicScript.CIRTH),
                elderBreakdown = transliterationFactory.transliterateWordByWord(
                    inputText,
                    RunicScript.ELDER_FUTHARK
                ),
                youngerBreakdown = transliterationFactory.transliterateWordByWord(
                    inputText,
                    RunicScript.YOUNGER_FUTHARK
                ),
                cirthBreakdown = transliterationFactory.transliterateWordByWord(
                    inputText,
                    RunicScript.CIRTH
                )
            )
        } catch (exception: Exception) {
            TransliterationBundle(errorMessage = exception.message ?: "Transliteration failed")
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun translateBundle(
        inputText: String,
        fidelity: TranslationFidelity,
        youngerVariant: YoungerFutharkVariant
    ): HistoricalTranslationBundle {
        if (inputText.isBlank()) {
            return HistoricalTranslationBundle()
        }

        return try {
            HistoricalTranslationBundle(
                elder = historicalTranslationService.translate(
                    text = inputText,
                    script = RunicScript.ELDER_FUTHARK,
                    fidelity = fidelity,
                    youngerVariant = youngerVariant
                ),
                younger = historicalTranslationService.translate(
                    text = inputText,
                    script = RunicScript.YOUNGER_FUTHARK,
                    fidelity = fidelity,
                    youngerVariant = youngerVariant
                ),
                cirth = historicalTranslationService.translate(
                    text = inputText,
                    script = RunicScript.CIRTH,
                    fidelity = fidelity,
                    youngerVariant = youngerVariant
                )
            )
        } catch (exception: Exception) {
            HistoricalTranslationBundle(errorMessage = exception.message ?: "Historical translation failed")
        }
    }
}

private data class TransliterationBundle(
    val elder: String = "",
    val younger: String = "",
    val cirth: String = "",
    val elderBreakdown: TransliterationBreakdown = TransliterationBreakdown(),
    val youngerBreakdown: TransliterationBreakdown = TransliterationBreakdown(),
    val cirthBreakdown: TransliterationBreakdown = TransliterationBreakdown(),
    val errorMessage: String? = null
) {
    fun outputFor(script: RunicScript): String = when (script) {
        RunicScript.ELDER_FUTHARK -> elder
        RunicScript.YOUNGER_FUTHARK -> younger
        RunicScript.CIRTH -> cirth
    }

    fun breakdownFor(script: RunicScript): TransliterationBreakdown = when (script) {
        RunicScript.ELDER_FUTHARK -> elderBreakdown
        RunicScript.YOUNGER_FUTHARK -> youngerBreakdown
        RunicScript.CIRTH -> cirthBreakdown
    }
}

private data class TranslationPreferencesState(
    val selectedScript: RunicScript,
    val selectedFont: String,
    val persistedWordByWordEnabled: Boolean,
    val translationMode: TranslationMode,
    val fidelity: TranslationFidelity,
    val youngerVariant: YoungerFutharkVariant
)

private data class TranslationPreferenceBasics(
    val selectedScript: RunicScript,
    val selectedFont: String,
    val persistedWordByWordEnabled: Boolean
)

private data class TranslationModePreferences(
    val translationMode: TranslationMode,
    val fidelity: TranslationFidelity,
    val youngerVariant: YoungerFutharkVariant
)

private data class TranslationInputState(
    val inputText: String,
    val isSaving: Boolean,
    val localWordByWordOverride: Boolean?
)

/**
 * UI state for the translation screen.
 */
internal data class TranslationUiState(
    val inputText: String = "",
    val transliteratedText: String = "",
    val selectedScript: RunicScript = RunicScript.DEFAULT,
    val translationMode: TranslationMode = TranslationMode.DEFAULT,
    val selectedFidelity: TranslationFidelity = TranslationFidelity.DEFAULT,
    val selectedYoungerVariant: YoungerFutharkVariant = YoungerFutharkVariant.DEFAULT,
    val selectedFont: String = "noto",
    val translateFeatureEnabled: Boolean = true,
    val outputGlyphCount: Int = 0,
    val inputCharacterCount: Int = 0,
    val canSave: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val wordByWordEnabled: Boolean = false,
    val wordBreakdown: List<WordTransliterationPair> = emptyList(),
    val normalizedForm: String = "",
    val diplomaticForm: String = "",
    val confidence: Float? = null,
    val notes: List<String> = emptyList(),
    val tokenBreakdown: List<TranslationTokenBreakdown> = emptyList(),
    val resolutionStatus: TranslationResolutionStatus? = null,
    val unresolvedTokens: List<String> = emptyList(),
    val provenance: List<TranslationProvenanceEntry> = emptyList(),
    val fallbackSuggestion: String? = null,
    val translationTrackLabel: String = ""
) {
    /** Display name for the currently selected script. */
    val scriptDisplayName: String get() = selectedScript.displayName
}

private fun String.glyphCount(): Int = count { character -> !character.isWhitespace() }

private data class HistoricalTranslationBundle(
    val elder: TranslationResult? = null,
    val younger: TranslationResult? = null,
    val cirth: TranslationResult? = null,
    val errorMessage: String? = null
) {
    fun outputFor(script: RunicScript): String = resultFor(script)?.glyphOutput.orEmpty()

    fun resultFor(script: RunicScript): TranslationResult? = when (script) {
        RunicScript.ELDER_FUTHARK -> elder
        RunicScript.YOUNGER_FUTHARK -> younger
        RunicScript.CIRTH -> cirth
    }

    fun results(): List<TranslationResult> = listOfNotNull(elder, younger, cirth)
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

private fun List<WordTransliterationPair>.toTranslationBreakdown(): List<TranslationTokenBreakdown> {
    return map { pair ->
        TranslationTokenBreakdown(
            sourceToken = pair.sourceToken,
            normalizedToken = pair.sourceToken,
            diplomaticToken = pair.runicToken,
            glyphToken = pair.runicToken
        )
    }
}

private fun List<TranslationTokenBreakdown>.toWordPairs(): List<WordTransliterationPair> {
    return map { token ->
        WordTransliterationPair(
            sourceToken = token.sourceToken,
            runicToken = token.glyphToken
        )
    }
}
