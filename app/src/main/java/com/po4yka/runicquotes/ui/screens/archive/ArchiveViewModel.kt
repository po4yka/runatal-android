package com.po4yka.runicquotes.ui.screens.archive

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.po4yka.runicquotes.data.repository.ArchiveRepository
import com.po4yka.runicquotes.domain.model.ArchivedQuote
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

/**
 * ViewModel for the archive screen with archived and deleted quote tabs.
 */
@HiltViewModel
class ArchiveViewModel @Inject constructor(
    private val archiveRepository: ArchiveRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArchiveUiState())
    val uiState: StateFlow<ArchiveUiState> = _uiState.asStateFlow()

    private val _snackbarEvent = Channel<ArchiveSnackbarEvent>(Channel.BUFFERED)
    val snackbarEvent = _snackbarEvent.receiveAsFlow()

    /** @suppress */
    companion object {
        private const val TAG = "ArchiveViewModel"
    }

    init {
        loadArchive()
    }

    private fun loadArchive() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            combine(
                archiveRepository.getActiveArchivedFlow(),
                archiveRepository.getDeletedFlow()
            ) { archived, deleted ->
                ArchiveUiState(
                    archivedQuotes = archived,
                    hiddenQuotes = emptyList(),
                    deletedQuotes = deleted,
                    selectedTab = _uiState.value.selectedTab,
                    isLoading = false
                )
            }.catch { e ->
                Log.e(TAG, "Error loading archive", e)
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Failed to load archive: ${e.message}")
                }
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    /**
     * Switches between Archived and Deleted tabs.
     */
    fun selectTab(tab: ArchiveTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    /**
     * Restores an archived quote back to the library.
     */
    fun restoreQuote(quote: ArchivedQuote) {
        viewModelScope.launch {
            try {
                archiveRepository.restoreQuote(quote.id)
                _snackbarEvent.send(ArchiveSnackbarEvent.RestoredQuote(listOf(quote)))
            } catch (e: IOException) {
                Log.e(TAG, "IO error restoring quote", e)
            } catch (e: IllegalStateException) {
                Log.e(TAG, "Invalid state restoring quote", e)
            }
        }
    }

    /**
     * Re-archives a quote (undo restore).
     */
    fun undoRestore(quotes: List<ArchivedQuote>) {
        viewModelScope.launch {
            try {
                quotes.forEach { quote ->
                    archiveRepository.archiveQuote(quote)
                }
            } catch (e: IOException) {
                Log.e(TAG, "IO error undoing restore", e)
            } catch (e: IllegalStateException) {
                Log.e(TAG, "Invalid state undoing restore", e)
            }
        }
    }

    /**
     * Restores all currently archived quotes back to the library.
     */
    fun restoreAllArchivedQuotes() {
        viewModelScope.launch {
            val quotesToRestore = _uiState.value.archivedQuotes
            if (quotesToRestore.isEmpty()) return@launch

            try {
                quotesToRestore.forEach { quote ->
                    archiveRepository.restoreQuote(quote.id)
                }
                _snackbarEvent.send(ArchiveSnackbarEvent.RestoredBatch(quotesToRestore))
            } catch (e: IOException) {
                Log.e(TAG, "IO error restoring all quotes", e)
            } catch (e: IllegalStateException) {
                Log.e(TAG, "Invalid state restoring all quotes", e)
            }
        }
    }

    /**
     * Soft-deletes an archived quote (moves to trash).
     */
    fun softDeleteQuote(id: Long) {
        viewModelScope.launch {
            try {
                archiveRepository.softDeleteQuote(id)
            } catch (e: IOException) {
                Log.e(TAG, "IO error deleting quote", e)
            } catch (e: IllegalStateException) {
                Log.e(TAG, "Invalid state deleting quote", e)
            }
        }
    }

    /**
     * Permanently removes all trashed quotes.
     */
    fun emptyTrash() {
        viewModelScope.launch {
            try {
                archiveRepository.emptyTrash()
            } catch (e: IOException) {
                Log.e(TAG, "IO error emptying trash", e)
            } catch (e: IllegalStateException) {
                Log.e(TAG, "Invalid state emptying trash", e)
            }
        }
    }

    /**
     * Retries loading after an error.
     */
    fun retry() {
        _uiState.update { it.copy(errorMessage = null) }
        loadArchive()
    }
}

/**
 * Archive tab options.
 */
enum class ArchiveTab { ARCHIVED, HIDDEN, DELETED }

/**
 * UI state for the archive screen.
 */
data class ArchiveUiState(
    val archivedQuotes: List<ArchivedQuote> = emptyList(),
    val hiddenQuotes: List<ArchivedQuote> = emptyList(),
    val deletedQuotes: List<ArchivedQuote> = emptyList(),
    val selectedTab: ArchiveTab = ArchiveTab.ARCHIVED,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

val ArchiveUiState.quotesForSelectedTab: List<ArchivedQuote>
    get() = when (selectedTab) {
        ArchiveTab.ARCHIVED -> archivedQuotes
        ArchiveTab.HIDDEN -> hiddenQuotes
        ArchiveTab.DELETED -> deletedQuotes
    }

/**
 * Event for showing a snackbar with undo action after restoring a quote.
 */
sealed interface ArchiveSnackbarEvent {
    val quotes: List<ArchivedQuote>

    /** Snackbar event for restoring a single quote. */
    data class RestoredQuote(override val quotes: List<ArchivedQuote>) : ArchiveSnackbarEvent

    /** Snackbar event for restoring multiple quotes in one action. */
    data class RestoredBatch(override val quotes: List<ArchivedQuote>) : ArchiveSnackbarEvent
}
