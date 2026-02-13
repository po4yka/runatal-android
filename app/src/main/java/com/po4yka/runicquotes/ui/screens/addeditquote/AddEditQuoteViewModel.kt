package com.po4yka.runicquotes.ui.screens.addeditquote

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.po4yka.runicquotes.data.preferences.UserPreferencesManager
import com.po4yka.runicquotes.data.repository.QuoteRepository
import com.po4yka.runicquotes.domain.model.Quote
import com.po4yka.runicquotes.domain.model.RunicScript
import com.po4yka.runicquotes.domain.model.getRunicText
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

    private val _uiState = MutableStateFlow(AddEditQuoteUiState())
    val uiState: StateFlow<AddEditQuoteUiState> = _uiState.asStateFlow()

    companion object {
        private const val TAG = "AddEditQuoteViewModel"
    }

    init {
        viewModelScope.launch {
            // Load preferences
            userPreferencesManager.userPreferencesFlow.collectLatest { prefs ->
                _uiState.update { it.copy(selectedScript = prefs.selectedScript) }
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
                _uiState.update {
                    it.copy(
                        textLatin = quote.textLatin,
                        author = quote.author,
                        isEditing = true
                    )
                }
                updateRunicPreviews()
            }
        }
    }

    /**
     * Updates the Latin text and regenerates runic previews.
     */
    fun updateTextLatin(text: String) {
        _uiState.update { it.copy(textLatin = text) }
        updateRunicPreviews()
    }

    /**
     * Updates the author name.
     */
    fun updateAuthor(author: String) {
        _uiState.update { it.copy(author = author) }
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
                _uiState.update { it.copy(isSaving = false) }
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
    val selectedScript: RunicScript = RunicScript.ELDER_FUTHARK,
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)
