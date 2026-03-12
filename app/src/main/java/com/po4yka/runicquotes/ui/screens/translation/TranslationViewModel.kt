package com.po4yka.runicquotes.ui.screens.translation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.po4yka.runicquotes.BuildConfig
import com.po4yka.runicquotes.data.preferences.UserPreferencesManager
import com.po4yka.runicquotes.data.repository.QuoteRepository
import com.po4yka.runicquotes.data.repository.TranslationRepository
import com.po4yka.runicquotes.di.DefaultDispatcher
import com.po4yka.runicquotes.domain.model.RunicScript
import com.po4yka.runicquotes.domain.model.displayName
import com.po4yka.runicquotes.domain.transliteration.TransliterationFactory
import com.po4yka.runicquotes.domain.usecase.translation.BuildHistoricalTranslationBundleUseCase
import com.po4yka.runicquotes.domain.usecase.translation.BuildTranslationPresentationUseCase
import com.po4yka.runicquotes.domain.usecase.translation.BuildTransliterationBundleUseCase
import com.po4yka.runicquotes.domain.usecase.translation.SaveTranslationRequest
import com.po4yka.runicquotes.domain.usecase.translation.SaveTranslationToLibraryUseCase
import com.po4yka.runicquotes.domain.usecase.translation.TranslationInputSnapshot
import com.po4yka.runicquotes.domain.usecase.translation.TranslationPreferencesSnapshot
import com.po4yka.runicquotes.domain.usecase.translation.TranslationPresentation
import com.po4yka.runicquotes.domain.translation.TranslationFidelity
import com.po4yka.runicquotes.domain.translation.TranslationMode
import com.po4yka.runicquotes.domain.translation.TranslationProvenanceEntry
import com.po4yka.runicquotes.domain.translation.TranslationResolutionStatus
import com.po4yka.runicquotes.domain.translation.TranslationTokenBreakdown
import com.po4yka.runicquotes.domain.translation.YoungerFutharkVariant
import com.po4yka.runicquotes.domain.transliteration.WordTransliterationPair
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

/**
 * ViewModel for the translation screen, including saving transliterations to the library.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
internal class TranslationViewModel @Inject constructor(
    private val userPreferencesManager: UserPreferencesManager,
    private val buildTranslationPresentationUseCase: BuildTranslationPresentationUseCase,
    private val saveTranslationToLibraryUseCase: SaveTranslationToLibraryUseCase,
    @param:DefaultDispatcher private val translationDispatcher: CoroutineDispatcher
) : ViewModel() {

    internal constructor(
        transliterationFactory: TransliterationFactory,
        historicalTranslationService: com.po4yka.runicquotes.domain.translation.HistoricalTranslationService,
        quoteRepository: QuoteRepository,
        translationRepository: TranslationRepository,
        userPreferencesManager: UserPreferencesManager,
        translationDispatcher: CoroutineDispatcher = Dispatchers.Default
    ) : this(
        userPreferencesManager = userPreferencesManager,
        buildTranslationPresentationUseCase = BuildTranslationPresentationUseCase(
            buildTransliterationBundleUseCase = BuildTransliterationBundleUseCase(transliterationFactory),
            buildHistoricalTranslationBundleUseCase = BuildHistoricalTranslationBundleUseCase(
                historicalTranslationService
            )
        ),
        saveTranslationToLibraryUseCase = SaveTranslationToLibraryUseCase(
            quoteRepository = quoteRepository,
            translationRepository = translationRepository,
            buildTransliterationBundleUseCase = BuildTransliterationBundleUseCase(transliterationFactory),
            buildHistoricalTranslationBundleUseCase = BuildHistoricalTranslationBundleUseCase(
                historicalTranslationService
            )
        ),
        translationDispatcher = translationDispatcher
    )

    private val _inputText = MutableStateFlow("")
    private val _selectedScript = MutableStateFlow(RunicScript.DEFAULT)
    private val _selectedFont = MutableStateFlow("noto")
    private val _translationMode = MutableStateFlow(TranslationMode.DEFAULT)
    private val _selectedFidelity = MutableStateFlow(TranslationFidelity.DEFAULT)
    private val _selectedYoungerVariant = MutableStateFlow(YoungerFutharkVariant.DEFAULT)
    private val _isSaving = MutableStateFlow(false)
    private val _persistedWordByWordEnabled = MutableStateFlow(false)
    private val _localWordByWordOverride = MutableStateFlow<Boolean?>(null)
    private val translateFeatureEnabled = BuildConfig.ENABLE_EXPERIMENTAL_TRANSLATE
    private val _events = Channel<TranslationEvent>(Channel.BUFFERED)

    /** @suppress */
    companion object {
        private const val TAG = "TranslationViewModel"
        private const val MAX_INPUT_LENGTH = 280
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

    val events = _events.receiveAsFlow()

    private val preferencesSnapshot = combine(
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
        TranslationPreferencesSnapshot(
            selectedScript = basics.selectedScript,
            selectedFont = basics.selectedFont,
            persistedWordByWordEnabled = basics.persistedWordByWordEnabled,
            translationMode = modePreferences.translationMode,
            fidelity = modePreferences.fidelity,
            youngerVariant = modePreferences.youngerVariant
        )
    }

    private val inputSnapshot = combine(
        _inputText,
        _isSaving,
        _localWordByWordOverride
    ) { inputText, isSaving, localWordByWordOverride ->
        TranslationInputSnapshot(
            inputText = inputText,
            isSaving = isSaving,
            localWordByWordOverride = localWordByWordOverride
        )
    }

    val uiState: StateFlow<TranslationUiState> = combine(
        preferencesSnapshot,
        inputSnapshot
    ) { preferences, inputState ->
        TranslationRenderRequest(
            preferences = preferences,
            input = inputState
        )
    }.mapLatest { request ->
        withContext(translationDispatcher) {
            buildTranslationPresentationUseCase(
                preferences = request.preferences,
                input = request.input,
                translateFeatureEnabled = translateFeatureEnabled
            ).toUiState()
        }
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
                val result = withContext(translationDispatcher) {
                    saveTranslationToLibraryUseCase(
                        SaveTranslationRequest(
                            inputText = state.inputText,
                            translationMode = state.translationMode,
                            fidelity = state.selectedFidelity,
                            youngerVariant = state.selectedYoungerVariant
                        )
                    )
                }
                _events.send(TranslationEvent.ShowMessage(result.message))
            } catch (exception: IOException) {
                Log.e(TAG, "IO error saving translation", exception)
                _events.send(TranslationEvent.ShowMessage("Failed to save translation"))
            } catch (exception: IllegalStateException) {
                Log.e(TAG, "Invalid state saving translation", exception)
                _events.send(TranslationEvent.ShowMessage("Invalid translation state"))
            } finally {
                _isSaving.value = false
            }
        }
    }
}

/** One-off UI events emitted by the translation screen. */
sealed interface TranslationEvent {
    /** Shows transient feedback to the user. */
    data class ShowMessage(val message: String) : TranslationEvent
}

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

private data class TranslationRenderRequest(
    val preferences: TranslationPreferencesSnapshot,
    val input: TranslationInputSnapshot
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
    val translationTrackLabel: String = "",
    val derivationKindLabel: String = "",
    val unavailableExplanation: String? = null
) {
    /** Display name for the currently selected script. */
    val scriptDisplayName: String get() = selectedScript.displayName
}

private fun TranslationPresentation.toUiState(): TranslationUiState {
    return TranslationUiState(
        inputText = inputText,
        transliteratedText = transliteratedText,
        selectedScript = selectedScript,
        translationMode = translationMode,
        selectedFidelity = selectedFidelity,
        selectedYoungerVariant = selectedYoungerVariant,
        selectedFont = selectedFont,
        translateFeatureEnabled = translateFeatureEnabled,
        outputGlyphCount = outputGlyphCount,
        inputCharacterCount = inputCharacterCount,
        canSave = canSave,
        isSaving = isSaving,
        errorMessage = errorMessage,
        wordByWordEnabled = wordByWordEnabled,
        wordBreakdown = wordBreakdown,
        normalizedForm = normalizedForm,
        diplomaticForm = diplomaticForm,
        confidence = confidence,
        notes = notes,
        tokenBreakdown = tokenBreakdown,
        resolutionStatus = resolutionStatus,
        unresolvedTokens = unresolvedTokens,
        provenance = provenance,
        fallbackSuggestion = fallbackSuggestion,
        translationTrackLabel = translationTrackLabel,
        derivationKindLabel = derivationKindLabel,
        unavailableExplanation = unavailableExplanation
    )
}
