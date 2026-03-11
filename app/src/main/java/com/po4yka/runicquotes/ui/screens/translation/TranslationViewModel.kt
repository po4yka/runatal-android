package com.po4yka.runicquotes.ui.screens.translation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.po4yka.runicquotes.data.preferences.UserPreferencesManager
import com.po4yka.runicquotes.data.repository.QuoteRepository
import com.po4yka.runicquotes.domain.model.Quote
import com.po4yka.runicquotes.domain.model.RunicScript
import com.po4yka.runicquotes.domain.model.displayName
import com.po4yka.runicquotes.domain.transliteration.TransliterationBreakdown
import com.po4yka.runicquotes.domain.transliteration.TransliterationFactory
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
class TranslationViewModel @Inject constructor(
    private val transliterationFactory: TransliterationFactory,
    private val quoteRepository: QuoteRepository,
    private val userPreferencesManager: UserPreferencesManager
) : ViewModel() {

    private val _inputText = MutableStateFlow("")
    private val _selectedScript = MutableStateFlow(RunicScript.DEFAULT)
    private val _selectedFont = MutableStateFlow("noto")
    private val _isSaving = MutableStateFlow(false)
    private val _feedbackMessage = MutableStateFlow<String?>(null)
    private val _persistedWordByWordEnabled = MutableStateFlow(false)
    private val _localWordByWordOverride = MutableStateFlow<Boolean?>(null)

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
            _selectedScript,
            _selectedFont,
            _persistedWordByWordEnabled
        ) { selectedScript, selectedFont, persistedWordByWordEnabled ->
            TranslationPreferencesState(
                selectedScript = selectedScript,
                selectedFont = selectedFont,
                persistedWordByWordEnabled = persistedWordByWordEnabled
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
        val bundle = transliterateBundle(inputState.inputText)
        val selectedOutput = bundle.outputFor(preferences.selectedScript)
        val selectedBreakdown = bundle.breakdownFor(preferences.selectedScript)
        val wordByWordEnabled =
            inputState.localWordByWordOverride ?: preferences.persistedWordByWordEnabled

        TranslationUiState(
            inputText = inputState.inputText,
            transliteratedText = selectedOutput,
            selectedScript = preferences.selectedScript,
            selectedFont = preferences.selectedFont,
            outputGlyphCount = selectedOutput.glyphCount(),
            inputCharacterCount = inputState.inputText.length,
            canSave = inputState.inputText.trim().isNotEmpty() &&
                !inputState.isSaving &&
                bundle.errorMessage == null,
            isSaving = inputState.isSaving,
            errorMessage = bundle.errorMessage,
            wordByWordEnabled = wordByWordEnabled,
            wordBreakdown = selectedBreakdown.wordPairs
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = TranslationUiState()
    )

    /**
     * Updates the source text with the Figma-defined 280 character cap.
     */
    fun updateInputText(text: String) {
        _inputText.value = text.take(MAX_INPUT_LENGTH)
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
                val bundle = transliterateBundle(input)
                val quote = Quote(
                    id = 0L,
                    textLatin = input,
                    author = DEFAULT_TRANSLATION_AUTHOR,
                    runicElder = bundle.outputFor(RunicScript.ELDER_FUTHARK),
                    runicYounger = bundle.outputFor(RunicScript.YOUNGER_FUTHARK),
                    runicCirth = bundle.outputFor(RunicScript.CIRTH),
                    isUserCreated = true,
                    isFavorite = false,
                    createdAt = System.currentTimeMillis()
                )

                quoteRepository.saveUserQuote(quote)
                _feedbackMessage.value = "Saved to library"
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
    val persistedWordByWordEnabled: Boolean
)

private data class TranslationInputState(
    val inputText: String,
    val isSaving: Boolean,
    val localWordByWordOverride: Boolean?
)

/**
 * UI state for the translation screen.
 */
data class TranslationUiState(
    val inputText: String = "",
    val transliteratedText: String = "",
    val selectedScript: RunicScript = RunicScript.DEFAULT,
    val selectedFont: String = "noto",
    val outputGlyphCount: Int = 0,
    val inputCharacterCount: Int = 0,
    val canSave: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val wordByWordEnabled: Boolean = false,
    val wordBreakdown: List<WordTransliterationPair> = emptyList()
) {
    /** Display name for the currently selected script. */
    val scriptDisplayName: String get() = selectedScript.displayName
}

private fun String.glyphCount(): Int = count { character -> !character.isWhitespace() }
