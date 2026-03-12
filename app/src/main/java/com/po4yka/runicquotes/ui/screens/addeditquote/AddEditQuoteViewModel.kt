package com.po4yka.runicquotes.ui.screens.addeditquote

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.po4yka.runicquotes.data.preferences.UserPreferencesManager
import com.po4yka.runicquotes.domain.repository.NoOpTranslationRepository
import com.po4yka.runicquotes.domain.repository.QuoteRepository
import com.po4yka.runicquotes.domain.repository.TranslationRepository
import com.po4yka.runicquotes.domain.model.Quote
import com.po4yka.runicquotes.domain.model.RunicScript
import com.po4yka.runicquotes.domain.transliteration.TransliterationFactory
import com.po4yka.runicquotes.domain.usecase.addeditquote.AddEditQuoteEditorInteractors
import com.po4yka.runicquotes.domain.usecase.addeditquote.BuildQuotePreviewsUseCase
import com.po4yka.runicquotes.domain.usecase.addeditquote.EvaluateQuoteDraftUseCase
import com.po4yka.runicquotes.domain.usecase.addeditquote.LoadEditableQuoteUseCase
import com.po4yka.runicquotes.domain.usecase.addeditquote.QuotePreviewSet
import com.po4yka.runicquotes.domain.usecase.addeditquote.SaveEditableQuoteRequest
import com.po4yka.runicquotes.domain.usecase.addeditquote.SaveEditableQuoteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
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
    savedStateHandle: SavedStateHandle,
    private val editorInteractors: AddEditQuoteEditorInteractors
) : ViewModel() {

    internal constructor(
        quoteRepository: QuoteRepository,
        userPreferencesManager: UserPreferencesManager,
        transliterationFactory: TransliterationFactory,
        savedStateHandle: SavedStateHandle,
        translationRepository: TranslationRepository = NoOpTranslationRepository
    ) : this(
        quoteRepository = quoteRepository,
        userPreferencesManager = userPreferencesManager,
        savedStateHandle = savedStateHandle,
        editorInteractors = AddEditQuoteEditorInteractors(
            loadEditableQuoteUseCase = LoadEditableQuoteUseCase(
                quoteRepository = quoteRepository,
                buildQuotePreviewsUseCase = BuildQuotePreviewsUseCase(transliterationFactory)
            ),
            buildQuotePreviewsUseCase = BuildQuotePreviewsUseCase(transliterationFactory),
            evaluateQuoteDraftUseCase = EvaluateQuoteDraftUseCase(),
            saveEditableQuoteUseCase = SaveEditableQuoteUseCase(
                quoteRepository = quoteRepository,
                translationRepository = translationRepository
            )
        )
    )

    private var quoteId: Long = savedStateHandle.get<Long>("quoteId") ?: 0L
    private var loadedQuoteId: Long? = null
    private var loadedQuote: Quote? = null
    private var initialTextLatin: String = ""
    private var initialAuthor: String = ""
    private var hasAttemptedSave: Boolean = false

    private val _uiState = MutableStateFlow(AddEditQuoteUiState())
    val uiState: StateFlow<AddEditQuoteUiState> = _uiState.asStateFlow()
    private val _events = Channel<AddEditQuoteEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    /** Constants for validation limits. */
    companion object {
        private const val TAG = "AddEditQuoteViewModel"
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
            val loadedEditableQuote = editorInteractors.loadEditableQuoteUseCase(quoteId)
            if (loadedEditableQuote != null) {
                loadedQuote = loadedEditableQuote.quote
                initialTextLatin = loadedEditableQuote.quote.textLatin
                initialAuthor = loadedEditableQuote.quote.author
                _uiState.update {
                    it.copy(
                        textLatin = loadedEditableQuote.quote.textLatin,
                        author = loadedEditableQuote.quote.author,
                        runicElderPreview = loadedEditableQuote.previews.elder,
                        runicYoungerPreview = loadedEditableQuote.previews.younger,
                        runicCirthPreview = loadedEditableQuote.previews.cirth,
                        createdAtMillis = loadedEditableQuote.quote.createdAt,
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
    fun saveQuote() {
        viewModelScope.launch {
            val state = _uiState.value
            hasAttemptedSave = true
            recomputeDerivedState()

            if (!_uiState.value.canSave) {
                return@launch
            }

            _uiState.update { it.copy(isSaving = true) }

            try {
                val result = editorInteractors.saveEditableQuoteUseCase(
                    SaveEditableQuoteRequest(
                        quoteId = quoteId,
                        textLatin = state.textLatin,
                        author = state.author,
                        previews = QuotePreviewSet(
                            elder = state.runicElderPreview,
                            younger = state.runicYoungerPreview,
                            cirth = state.runicCirthPreview
                        ),
                        existingQuote = loadedQuote,
                        createdAtMillis = state.createdAtMillis,
                        isEditing = state.isEditing,
                        initialTextLatin = initialTextLatin
                    )
                )
                if (result.translationInvalidationError != null) {
                    Log.w(
                        TAG,
                        "Saved edited quote but failed to invalidate translation cache for id=${result.savedQuote.id}",
                        result.translationInvalidationError
                    )
                }
                quoteId = result.savedQuote.id
                loadedQuoteId = result.savedQuote.id
                loadedQuote = result.savedQuote
                initialTextLatin = result.savedQuote.textLatin
                initialAuthor = result.savedQuote.author
                if (state.isEditing) {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            hasUnsavedChanges = false,
                            createdAtMillis = result.savedQuote.createdAt
                        )
                    }
                    _events.send(AddEditQuoteEvent.NavigateBackAfterEdit)
                } else {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            hasUnsavedChanges = false,
                            createdAtMillis = result.savedQuote.createdAt,
                            showConfirmation = true
                        )
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "IO error saving quote", e)
                _uiState.update { it.copy(isSaving = false) }
                _events.send(AddEditQuoteEvent.ShowMessage("Failed to save quote: ${e.message}"))
            } catch (e: IllegalStateException) {
                Log.e(TAG, "Invalid state saving quote", e)
                _uiState.update { it.copy(isSaving = false) }
                _events.send(AddEditQuoteEvent.ShowMessage("Invalid state: ${e.message}"))
            }
        }
    }

    /**
     * Deletes the current quote being edited.
     */
    fun deleteQuote() {
        if (quoteId == 0L) return
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true) }
            try {
                quoteRepository.deleteUserQuote(quoteId)
                _events.send(AddEditQuoteEvent.NavigateBackAfterDelete)
            } catch (e: IOException) {
                Log.e(TAG, "IO error deleting quote", e)
                _uiState.update { it.copy(isDeleting = false) }
                _events.send(AddEditQuoteEvent.ShowMessage("Failed to delete quote: ${e.message}"))
            } catch (e: IllegalStateException) {
                Log.e(TAG, "Invalid state deleting quote", e)
                _uiState.update { it.copy(isDeleting = false) }
                _events.send(AddEditQuoteEvent.ShowMessage("Invalid state: ${e.message}"))
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
     * Regenerates runic previews for all scripts based on current text.
     */
    private fun updateRunicPreviews(text: String = _uiState.value.textLatin) {
        val previews = editorInteractors.buildQuotePreviewsUseCase(
            text = text,
            preservedQuote = loadedQuote
        )
        _uiState.update {
            it.copy(
                runicElderPreview = previews.elder,
                runicYoungerPreview = previews.younger,
                runicCirthPreview = previews.cirth
            )
        }
    }

    private fun recomputeDerivedState() {
        val state = _uiState.value
        val evaluation = editorInteractors.evaluateQuoteDraftUseCase(
            textLatin = state.textLatin,
            author = state.author,
            isEditing = state.isEditing,
            initialTextLatin = initialTextLatin,
            initialAuthor = initialAuthor,
            hasAttemptedSave = hasAttemptedSave
        )

        _uiState.update {
            it.copy(
                quoteTextError = evaluation.quoteTextError,
                authorError = evaluation.authorError,
                quoteCharCount = evaluation.quoteCharCount,
                authorCharCount = evaluation.authorCharCount,
                hasUnsavedChanges = evaluation.hasUnsavedChanges,
                canSave = evaluation.canSave
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
    val showConfirmation: Boolean = false
)

/** One-off navigation events emitted by the add/edit quote screen. */
sealed interface AddEditQuoteEvent {
    /** Shows transient feedback to the user. */
    data class ShowMessage(val message: String) : AddEditQuoteEvent

    /** Navigates back after an existing quote has been saved. */
    data object NavigateBackAfterEdit : AddEditQuoteEvent

    /** Navigates back after the current quote has been deleted. */
    data object NavigateBackAfterDelete : AddEditQuoteEvent
}
