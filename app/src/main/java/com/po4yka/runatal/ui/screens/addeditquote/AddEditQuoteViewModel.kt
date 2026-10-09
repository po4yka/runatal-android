package com.po4yka.runatal.ui.screens.addeditquote

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.po4yka.runatal.data.preferences.UserPreferencesManager
import com.po4yka.runatal.domain.repository.QuoteRepository
import com.po4yka.runatal.domain.model.Quote
import com.po4yka.runatal.domain.model.RunicScript
import com.po4yka.runatal.domain.transliteration.TransliterationFactory
import com.po4yka.runatal.domain.usecase.addeditquote.AddEditQuoteEditorInteractors
import com.po4yka.runatal.domain.usecase.addeditquote.BuildQuotePreviewsUseCase
import com.po4yka.runatal.domain.usecase.addeditquote.EvaluateQuoteDraftUseCase
import com.po4yka.runatal.domain.usecase.addeditquote.LoadEditableQuoteUseCase
import com.po4yka.runatal.domain.usecase.addeditquote.QuotePreviewSet
import com.po4yka.runatal.domain.usecase.addeditquote.SaveEditableQuoteRequest
import com.po4yka.runatal.domain.usecase.addeditquote.SaveEditableQuoteUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
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

/**
 * ViewModel for adding or editing user-created quotes.
 * Provides live preview of runic transliteration as user types.
 */
@HiltViewModel(assistedFactory = AddEditQuoteViewModel.Factory::class)
internal class AddEditQuoteViewModel @AssistedInject constructor(
    private val quoteRepository: QuoteRepository,
    private val userPreferencesManager: UserPreferencesManager,
    @Assisted private var quoteId: Long,
    private val editorInteractors: AddEditQuoteEditorInteractors,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    internal constructor(
        quoteRepository: QuoteRepository,
        userPreferencesManager: UserPreferencesManager,
        transliterationFactory: TransliterationFactory,
        quoteId: Long,
        savedStateHandle: SavedStateHandle = SavedStateHandle()
    ) : this(
        quoteRepository = quoteRepository,
        userPreferencesManager = userPreferencesManager,
        quoteId = quoteId,
        editorInteractors = AddEditQuoteEditorInteractors(
            loadEditableQuoteUseCase = LoadEditableQuoteUseCase(
                quoteRepository = quoteRepository,
                buildQuotePreviewsUseCase = BuildQuotePreviewsUseCase(transliterationFactory)
            ),
            buildQuotePreviewsUseCase = BuildQuotePreviewsUseCase(transliterationFactory),
            evaluateQuoteDraftUseCase = EvaluateQuoteDraftUseCase(),
            saveEditableQuoteUseCase = SaveEditableQuoteUseCase(
                quoteRepository = quoteRepository,
                buildQuotePreviewsUseCase = BuildQuotePreviewsUseCase(transliterationFactory)
            )
        ),
        savedStateHandle = savedStateHandle
    )

    private var loadedQuoteId: Long? = null
    private var loadedQuote: Quote? = null
    private var initialTextLatin: String = ""
    private var initialAuthor: String = ""
    private var hasAttemptedSave: Boolean = false

    private val _uiState = MutableStateFlow(
        AddEditQuoteUiState(
            textLatin = savedStateHandle[DRAFT_TEXT] ?: "",
            author = savedStateHandle[DRAFT_AUTHOR] ?: "",
            showConfirmation = savedStateHandle[SAVED_CONFIRMATION] ?: false
        )
    )
    val uiState: StateFlow<AddEditQuoteUiState> = _uiState.asStateFlow()
    private val _events = Channel<AddEditQuoteEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    /** Creates an entry-scoped editor for a new quote (zero) or the selected quote. */
    @AssistedFactory
    interface Factory {
        /** Creates the ViewModel for the selected navigation entry. */
        fun create(quoteId: Long): AddEditQuoteViewModel
    }

    /** Constants for validation limits. */
    companion object {
        private const val TAG = "AddEditQuoteViewModel"
        private const val DRAFT_TEXT = "editor.text"
        private const val DRAFT_AUTHOR = "editor.author"
        private const val SAVED_QUOTE_ID = "editor.quoteId"
        private const val SAVED_CONFIRMATION = "editor.confirmation"
        private const val INITIAL_TEXT = "editor.initialText"
        private const val INITIAL_AUTHOR = "editor.initialAuthor"
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

        this.quoteId = savedStateHandle[SAVED_QUOTE_ID] ?: quoteId
        updateRunicPreviews(_uiState.value.textLatin)
        recomputeDerivedState()
        initializeQuoteIfNeeded(this.quoteId)
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
        _uiState.update { it.copy(isLoading = true, loadError = null, canSave = false) }

        viewModelScope.launch {
            try {
                val loadedEditableQuote = editorInteractors.loadEditableQuoteUseCase(quoteId)
                if (loadedEditableQuote == null) {
                    _uiState.update { it.copy(loadError = "Quote is missing or cannot be edited") }
                    return@launch
                }
                loadedQuote = loadedEditableQuote.quote
                initialTextLatin = savedStateHandle[INITIAL_TEXT] ?: loadedEditableQuote.quote.textLatin
                initialAuthor = savedStateHandle[INITIAL_AUTHOR] ?: loadedEditableQuote.quote.author
                savedStateHandle[INITIAL_TEXT] = initialTextLatin
                savedStateHandle[INITIAL_AUTHOR] = initialAuthor
                _uiState.update {
                    it.copy(
                        textLatin = savedStateHandle[DRAFT_TEXT] ?: loadedEditableQuote.quote.textLatin,
                        author = savedStateHandle[DRAFT_AUTHOR] ?: loadedEditableQuote.quote.author,
                        runicElderPreview = loadedEditableQuote.previews.elder,
                        runicYoungerPreview = loadedEditableQuote.previews.younger,
                        runicCirthPreview = loadedEditableQuote.previews.cirth,
                        createdAtMillis = loadedEditableQuote.quote.createdAt,
                        isEditing = true
                    )
                }
                savedStateHandle[DRAFT_TEXT] = _uiState.value.textLatin
                savedStateHandle[DRAFT_AUTHOR] = _uiState.value.author
                updateRunicPreviews(_uiState.value.textLatin)
            } catch (exception: IOException) {
                Log.e(TAG, "Failed to load editable quote", exception)
                _uiState.update { it.copy(loadError = "Failed to load quote") }
            } catch (exception: IllegalStateException) {
                Log.e(TAG, "Invalid editable quote state", exception)
                _uiState.update { it.copy(loadError = "Failed to load quote") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
                recomputeDerivedState()
            }
        }
    }

    /** Retries loading an existing quote after an error. */
    fun retryLoading() {
        if (_uiState.value.isLoading || quoteId == 0L) return
        loadedQuoteId = null
        initializeQuoteIfNeeded(quoteId)
    }

    /**
     * Updates the Latin text and regenerates runic previews.
     */
    fun updateTextLatin(text: String) {
        if (!_uiState.value.isEditable) return
        savedStateHandle[DRAFT_TEXT] = text
        _uiState.update { it.copy(textLatin = text) }
        updateRunicPreviews(text)
        recomputeDerivedState()
    }

    /**
     * Updates the author name.
     */
    fun updateAuthor(author: String) {
        if (!_uiState.value.isEditable) return
        savedStateHandle[DRAFT_AUTHOR] = author
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
        if (!_uiState.value.isEditable) return
        hasAttemptedSave = true
        recomputeDerivedState()
        val state = _uiState.value
        if (!state.canSave) return
        _uiState.update { it.copy(isSaving = true, canSave = false) }

        viewModelScope.launch {
            try {
                val result = editorInteractors.saveEditableQuoteUseCase(
                    SaveEditableQuoteRequest(
                        quoteId = quoteId,
                        textLatin = state.textLatin,
                        author = state.author,
                        existingQuote = loadedQuote,
                        createdAtMillis = state.createdAtMillis,
                        isEditing = state.isEditing,
                        initialTextLatin = initialTextLatin,
                        initialAuthor = initialAuthor
                    )
                )
                quoteId = result.savedQuote.id
                savedStateHandle[SAVED_QUOTE_ID] = quoteId
                savedStateHandle[DRAFT_TEXT] = result.savedQuote.textLatin
                savedStateHandle[DRAFT_AUTHOR] = result.savedQuote.author
                savedStateHandle[SAVED_CONFIRMATION] = !state.isEditing
                loadedQuoteId = result.savedQuote.id
                loadedQuote = result.savedQuote
                initialTextLatin = result.savedQuote.textLatin
                initialAuthor = result.savedQuote.author
                savedStateHandle[INITIAL_TEXT] = initialTextLatin
                savedStateHandle[INITIAL_AUTHOR] = initialAuthor
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
                recomputeDerivedState()
                _events.send(AddEditQuoteEvent.ShowMessage("Failed to save quote: ${e.message}"))
            } catch (e: IllegalStateException) {
                Log.e(TAG, "Invalid state saving quote", e)
                _uiState.update { it.copy(isSaving = false) }
                recomputeDerivedState()
                _events.send(AddEditQuoteEvent.ShowMessage("Invalid state: ${e.message}"))
            }
        }
    }

    /**
     * Deletes the current quote being edited.
     */
    fun deleteQuote() {
        if (quoteId == 0L || !_uiState.value.isEditable) return
        _uiState.update { it.copy(isDeleting = true, canSave = false) }
        viewModelScope.launch {
            try {
                quoteRepository.deleteUserQuote(quoteId)
                _events.send(AddEditQuoteEvent.NavigateBackAfterDelete)
            } catch (e: IOException) {
                Log.e(TAG, "IO error deleting quote", e)
                _uiState.update { it.copy(isDeleting = false) }
                recomputeDerivedState()
                _events.send(AddEditQuoteEvent.ShowMessage("Failed to delete quote: ${e.message}"))
            } catch (e: IllegalStateException) {
                Log.e(TAG, "Invalid state deleting quote", e)
                _uiState.update { it.copy(isDeleting = false) }
                recomputeDerivedState()
                _events.send(AddEditQuoteEvent.ShowMessage("Invalid state: ${e.message}"))
            }
        }
    }

    /**
     * Resets the form for creating another quote after confirmation.
     */
    fun resetForNewQuote() {
        savedStateHandle.remove<String>(INITIAL_TEXT)
        savedStateHandle.remove<String>(INITIAL_AUTHOR)
        savedStateHandle.remove<String>(DRAFT_TEXT)
        savedStateHandle.remove<String>(DRAFT_AUTHOR)
        savedStateHandle.remove<Long>(SAVED_QUOTE_ID)
        savedStateHandle.remove<Boolean>(SAVED_CONFIRMATION)
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
                canSave = evaluation.canSave && it.isEditable
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
    val isLoading: Boolean = false,
    val loadError: String? = null
) {
    /** Whether a mutually exclusive save or delete is in flight. */
    val isMutating: Boolean get() = isSaving || isDeleting

    /** Editing requires a successfully loaded quote and no pending mutation. */
    val isEditable: Boolean get() = !isMutating && !isLoading && loadError == null
}

/** One-off navigation events emitted by the add/edit quote screen. */
sealed interface AddEditQuoteEvent {
    /** Shows transient feedback to the user. */
    data class ShowMessage(val message: String) : AddEditQuoteEvent

    /** Navigates back after an existing quote has been saved. */
    data object NavigateBackAfterEdit : AddEditQuoteEvent

    /** Navigates back after the current quote has been deleted. */
    data object NavigateBackAfterDelete : AddEditQuoteEvent
}
