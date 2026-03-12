package com.po4yka.runicquotes.ui.screens.addeditquote

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.po4yka.runicquotes.data.preferences.UserPreferencesManager
import com.po4yka.runicquotes.data.repository.NoOpTranslationRepository
import com.po4yka.runicquotes.data.repository.QuoteRepository
import com.po4yka.runicquotes.data.repository.TranslationRepository
import com.po4yka.runicquotes.domain.model.Quote
import com.po4yka.runicquotes.domain.model.RunicScript
import com.po4yka.runicquotes.domain.model.getRunicText
import com.po4yka.runicquotes.domain.transliteration.TransliterationFactory
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
internal class AddEditQuoteViewModel @Inject constructor(
    private val quoteRepository: QuoteRepository,
    private val userPreferencesManager: UserPreferencesManager,
    private val transliterationFactory: TransliterationFactory,
    savedStateHandle: SavedStateHandle,
    private val translationRepository: TranslationRepository = NoOpTranslationRepository
) : ViewModel() {

    private var quoteId: Long = savedStateHandle.get<Long>("quoteId") ?: 0L
    private var loadedQuoteId: Long? = null
    private var loadedQuote: Quote? = null
    private var initialTextLatin: String = ""
    private var initialAuthor: String = ""
    private var hasAttemptedSave: Boolean = false

    private val _uiState = MutableStateFlow(AddEditQuoteUiState())
    val uiState: StateFlow<AddEditQuoteUiState> = _uiState.asStateFlow()

    /** Constants for validation limits. */
    companion object {
        private const val TAG = "AddEditQuoteViewModel"
        private const val MAX_QUOTE_LENGTH = 280
        private const val MAX_AUTHOR_LENGTH = 60
        private const val MIN_QUOTE_LENGTH = 3
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
                loadedQuote = quote
                initialTextLatin = quote.textLatin
                initialAuthor = quote.author
                _uiState.update {
                    it.copy(
                        textLatin = quote.textLatin,
                        author = quote.author,
                        runicElderPreview = quote.getRunicText(RunicScript.ELDER_FUTHARK, transliterationFactory),
                        runicYoungerPreview = quote.getRunicText(RunicScript.YOUNGER_FUTHARK, transliterationFactory),
                        runicCirthPreview = quote.getRunicText(RunicScript.CIRTH, transliterationFactory),
                        createdAtMillis = quote.createdAt,
                        isEditing = true
                    )
                }
                recomputeDerivedState()
            }
        }
    }

    /**
     * Updates the Latin text and regenerates runic previews.
     */
    fun updateTextLatin(text: String) {
        _uiState.update { it.copy(textLatin = text) }
        updateRunicPreviews(text)
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
     * Saves the quote to the database and shows confirmation.
     */
    fun saveQuote(onEditSaved: () -> Unit = {}) {
        viewModelScope.launch {
            val state = _uiState.value
            hasAttemptedSave = true
            recomputeDerivedState()

            if (!_uiState.value.canSave) {
                return@launch
            }

            _uiState.update { it.copy(isSaving = true, errorMessage = null) }

            try {
                val trimmedText = state.textLatin.trim()
                val trimmedAuthor = state.author.trim()
                val didTextChange = state.isEditing && trimmedText != initialTextLatin.trim()
                val createdAt = if (state.isEditing && state.createdAtMillis != 0L) {
                    state.createdAtMillis
                } else {
                    System.currentTimeMillis()
                }
                val quote = Quote(
                    id = quoteId,
                    textLatin = trimmedText,
                    author = trimmedAuthor,
                    runicElder = state.runicElderPreview,
                    runicYounger = state.runicYoungerPreview,
                    runicCirth = state.runicCirthPreview,
                    isUserCreated = true,
                    isFavorite = loadedQuote?.isFavorite ?: false,
                    createdAt = createdAt
                )

                val savedQuoteId = quoteRepository.saveUserQuote(quote)
                if (didTextChange) {
                    runCatching {
                        translationRepository.deleteTranslationsForQuote(savedQuoteId)
                    }.onFailure { throwable ->
                        Log.w(
                            TAG,
                            "Saved edited quote but failed to invalidate translation cache for id=$savedQuoteId",
                            throwable
                        )
                    }
                }
                quoteId = savedQuoteId
                loadedQuoteId = savedQuoteId
                loadedQuote = quote.copy(id = savedQuoteId)
                initialTextLatin = trimmedText
                initialAuthor = trimmedAuthor
                if (state.isEditing) {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            hasUnsavedChanges = false,
                            createdAtMillis = createdAt
                        )
                    }
                    onEditSaved()
                } else {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            hasUnsavedChanges = false,
                            createdAtMillis = createdAt,
                            showConfirmation = true
                        )
                    }
                }
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
     * Deletes the current quote being edited.
     */
    fun deleteQuote(onSuccess: () -> Unit) {
        if (quoteId == 0L) return
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true) }
            try {
                quoteRepository.deleteUserQuote(quoteId)
                onSuccess()
            } catch (e: IOException) {
                Log.e(TAG, "IO error deleting quote", e)
                _uiState.update {
                    it.copy(
                        isDeleting = false,
                        errorMessage = "Failed to delete quote: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Resets the form for creating another quote after confirmation.
     */
    fun resetForNewQuote() {
        quoteId = 0L
        loadedQuoteId = null
        loadedQuote = null
        initialTextLatin = ""
        initialAuthor = ""
        hasAttemptedSave = false
        _uiState.update {
            AddEditQuoteUiState(
                selectedScript = it.selectedScript,
                selectedFont = it.selectedFont
            )
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
    private fun updateRunicPreviews(text: String = _uiState.value.textLatin) {
        val preservedQuote = loadedQuote?.takeIf { it.textLatin == text }
        _uiState.update {
            it.copy(
                runicElderPreview = preservedQuote?.getRunicText(RunicScript.ELDER_FUTHARK, transliterationFactory)
                    ?: transliterationFactory.transliterate(text, RunicScript.ELDER_FUTHARK),
                runicYoungerPreview = preservedQuote?.getRunicText(RunicScript.YOUNGER_FUTHARK, transliterationFactory)
                    ?: transliterationFactory.transliterate(text, RunicScript.YOUNGER_FUTHARK),
                runicCirthPreview = preservedQuote?.getRunicText(RunicScript.CIRTH, transliterationFactory)
                    ?: transliterationFactory.transliterate(text, RunicScript.CIRTH)
            )
        }
    }

    private fun recomputeDerivedState() {
        val state = _uiState.value
        val rawQuoteTextError = when {
            state.textLatin.trim().length < MIN_QUOTE_LENGTH -> {
                "Quote must be at least $MIN_QUOTE_LENGTH characters"
            }
            state.textLatin.length > MAX_QUOTE_LENGTH -> "Keep quote under $MAX_QUOTE_LENGTH characters"
            else -> null
        }

        val rawAuthorError = when {
            state.author.isBlank() -> "Author is required"
            state.author.length > MAX_AUTHOR_LENGTH -> "Keep author under $MAX_AUTHOR_LENGTH characters"
            else -> null
        }
        val showQuoteTextError = hasAttemptedSave || state.textLatin.isNotBlank()
        val showAuthorError = hasAttemptedSave || state.author.isNotBlank()
        val quoteTextError = rawQuoteTextError?.takeIf { showQuoteTextError }
        val authorError = rawAuthorError?.takeIf { showAuthorError }

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
                hasUnsavedChanges = hasUnsavedChanges,
                canSave = rawQuoteTextError == null &&
                    rawAuthorError == null &&
                    hasUnsavedChanges
            )
        }
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
    val selectedScript: RunicScript = RunicScript.ELDER_FUTHARK,
    val selectedFont: String = "noto",
    val createdAtMillis: Long = 0L,
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val hasUnsavedChanges: Boolean = false,
    val canSave: Boolean = false,
    val showConfirmation: Boolean = false,
    val errorMessage: String? = null
)
