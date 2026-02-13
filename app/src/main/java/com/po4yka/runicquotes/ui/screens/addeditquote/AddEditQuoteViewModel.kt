package com.po4yka.runicquotes.ui.screens.addeditquote

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.po4yka.runicquotes.data.preferences.UserPreferencesManager
import com.po4yka.runicquotes.data.repository.QuoteRepository
import com.po4yka.runicquotes.domain.model.Quote
import com.po4yka.runicquotes.domain.model.RunicScript
import com.po4yka.runicquotes.domain.transliteration.CirthTransliterator
import com.po4yka.runicquotes.domain.transliteration.ElderFutharkTransliterator
import com.po4yka.runicquotes.domain.transliteration.YoungerFutharkTransliterator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

/**
 * ViewModel for adding or editing user-created quotes.
 * Provides live preview of runic transliteration as user types.
 */
@HiltViewModel
class AddEditQuoteViewModel @Inject constructor(
    private val quoteRepository: QuoteRepository,
    private val userPreferencesManager: UserPreferencesManager,
    private val elderFutharkTransliterator: ElderFutharkTransliterator,
    private val youngerFutharkTransliterator: YoungerFutharkTransliterator,
    private val cirthTransliterator: CirthTransliterator,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private var quoteId: Long = savedStateHandle.get<Long>("quoteId") ?: 0L
    private var loadedQuoteId: Long? = null
    private var initialTextLatin: String = ""
    private var initialAuthor: String = ""

    private val _uiState = MutableStateFlow(AddEditQuoteUiState())
    val uiState: StateFlow<AddEditQuoteUiState> = _uiState.asStateFlow()

    companion object {
        private const val TAG = "AddEditQuoteViewModel"
        private const val MAX_QUOTE_LENGTH = 280
        private const val MAX_AUTHOR_LENGTH = 60
        private const val MIN_QUOTE_LENGTH = 10
    }

    init {
        viewModelScope.launch {
            // Load preferences
            userPreferencesManager.userPreferencesFlow.collectLatest { prefs ->
                _uiState.update {
                    it.copy(
                        selectedScript = prefs.selectedScript,
                        selectedFont = prefs.selectedFont
                    )
                }
                recomputeDerivedState()
            }
        }

        initializeQuoteIfNeeded(quoteId)
    }

    /**
     * Loads quote data for editing when a quote ID is provided.
     * Safe to call multiple times; already-loaded IDs are ignored.
     */
    fun initializeQuoteIfNeeded(quoteId: Long) {
        if (quoteId == 0L || loadedQuoteId == quoteId) {
            return
        }

        this.quoteId = quoteId
        loadedQuoteId = quoteId

        viewModelScope.launch {
            val quote = quoteRepository.getQuoteById(quoteId)
            if (quote != null && quote.isUserCreated) {
                initialTextLatin = quote.textLatin
                initialAuthor = quote.author
                _uiState.update {
                    it.copy(
                        textLatin = quote.textLatin,
                        author = quote.author,
                        isEditing = true
                    )
                }
                updateRunicPreviews()
                recomputeDerivedState()
            }
        }
    }

    /**
     * Updates the Latin text and regenerates runic previews.
     */
    fun updateTextLatin(text: String) {
        _uiState.update { it.copy(textLatin = text) }
        updateRunicPreviews()
        recomputeDerivedState()
    }

    /**
     * Updates the author name.
     */
    fun updateAuthor(author: String) {
        _uiState.update { it.copy(author = author) }
        recomputeDerivedState()
    }

    /**
     * Updates the selected script for preview.
     */
    fun updateSelectedScript(script: RunicScript) {
        _uiState.update { it.copy(selectedScript = script) }
    }

    /**
     * Saves the quote to the database.
     */
    fun saveQuote(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val state = _uiState.value

            if (state.textLatin.isBlank() || state.author.isBlank()) {
                _uiState.update { it.copy(errorMessage = "Text and author cannot be empty") }
                return@launch
            }

            if (!state.canSave) {
                _uiState.update {
                    it.copy(errorMessage = "Please resolve field errors before saving")
                }
                return@launch
            }

            _uiState.update { it.copy(isSaving = true, errorMessage = null) }

            try {
                val quote = Quote(
                    id = quoteId,
                    textLatin = state.textLatin.trim(),
                    author = state.author.trim(),
                    runicElder = state.runicElderPreview,
                    runicYounger = state.runicYoungerPreview,
                    runicCirth = state.runicCirthPreview,
                    isUserCreated = true,
                    isFavorite = false,
                    createdAt = System.currentTimeMillis()
                )

                quoteRepository.saveUserQuote(quote)
                initialTextLatin = state.textLatin.trim()
                initialAuthor = state.author.trim()
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        hasUnsavedChanges = false
                    )
                }
                onSuccess()
            } catch (e: IOException) {
                Log.e(TAG, "IO error saving quote", e)
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = "Failed to save quote: ${e.message}"
                    )
                }
            } catch (e: IllegalStateException) {
                Log.e(TAG, "Invalid state saving quote", e)
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = "Invalid state: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Clears any error message.
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * Regenerates runic previews for all scripts based on current text.
     */
    private fun updateRunicPreviews() {
        val text = _uiState.value.textLatin
        _uiState.update {
            it.copy(
                runicElderPreview = elderFutharkTransliterator.transliterate(text),
                runicYoungerPreview = youngerFutharkTransliterator.transliterate(text),
                runicCirthPreview = cirthTransliterator.transliterate(text)
            )
        }
    }

    private fun recomputeDerivedState() {
        val state = _uiState.value
        val quoteTextError = when {
            state.textLatin.isBlank() -> "Quote text is required"
            state.textLatin.length > MAX_QUOTE_LENGTH -> "Keep quote under $MAX_QUOTE_LENGTH characters"
            state.textLatin.trim().length < MIN_QUOTE_LENGTH -> {
                "Use at least $MIN_QUOTE_LENGTH characters for meaningful transliteration"
            }
            else -> null
        }

        val authorError = when {
            state.author.isBlank() -> "Author is required"
            state.author.length > MAX_AUTHOR_LENGTH -> "Keep author under $MAX_AUTHOR_LENGTH characters"
            else -> null
        }

        val confidence = computeTransliterationConfidence(state.textLatin)
        val confidenceHint = when {
            state.textLatin.isBlank() -> "Start typing to compute transliteration confidence."
            confidence >= 90 -> "Excellent transliteration compatibility."
            confidence >= 70 -> "Good coverage. Some characters may map approximately."
            else -> "Low compatibility. Reduce symbols/numbers for cleaner runes."
        }
        val hasUnsavedChanges = if (state.isEditing) {
            state.textLatin.trim() != initialTextLatin.trim() ||
                state.author.trim() != initialAuthor.trim()
        } else {
            state.textLatin.isNotBlank() || state.author.isNotBlank()
        }

        _uiState.update {
            it.copy(
                quoteTextError = quoteTextError,
                authorError = authorError,
                quoteCharCount = state.textLatin.length,
                authorCharCount = state.author.length,
                transliterationConfidence = confidence,
                confidenceHint = confidenceHint,
                hasUnsavedChanges = hasUnsavedChanges,
                canSave = quoteTextError == null &&
                    authorError == null &&
                    hasUnsavedChanges
            )
        }
    }

    private fun computeTransliterationConfidence(text: String): Int {
        val letters = text.filter { it.isLetter() }
        if (letters.isEmpty()) {
            return 100
        }

        val supportedLetters = letters.count { char ->
            val lower = char.lowercaseChar()
            lower in 'a'..'z'
        }

        return (supportedLetters * 100) / letters.length
    }
}

/**
 * UI state for add/edit quote screen.
 */
data class AddEditQuoteUiState(
    val textLatin: String = "",
    val author: String = "",
    val runicElderPreview: String = "",
    val runicYoungerPreview: String = "",
    val runicCirthPreview: String = "",
    val quoteTextError: String? = null,
    val authorError: String? = null,
    val quoteCharCount: Int = 0,
    val authorCharCount: Int = 0,
    val transliterationConfidence: Int = 100,
    val confidenceHint: String = "Start typing to compute transliteration confidence.",
    val selectedScript: RunicScript = RunicScript.ELDER_FUTHARK,
    val selectedFont: String = "noto",
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val hasUnsavedChanges: Boolean = false,
    val canSave: Boolean = false,
    val errorMessage: String? = null
)
