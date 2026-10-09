package com.po4yka.runatal.ui.screens.archive

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.po4yka.runatal.data.repository.ArchiveRepository
import com.po4yka.runatal.domain.model.ArchivedQuote
import com.po4yka.runatal.domain.model.QuoteLifecycleChange
import com.po4yka.runatal.domain.model.QuoteLifecycleState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
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
    private var observationJob: Job? = null

    /** @suppress */
    companion object {
        private const val TAG = "ArchiveViewModel"
    }

    init {
        loadArchive()
    }

    private fun loadArchive() {
        observationJob?.cancel()
        observationJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            archiveRepository.getRetainedQuotesFlow().catch { e ->
                Log.e(TAG, "Error loading archive", e)
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Failed to load archive: ${e.message}")
                }
            }.collect { quotes ->
                _uiState.update { state ->
                    state.copy(
                        archivedQuotes = quotes.filter { it.lifecycleState == QuoteLifecycleState.ARCHIVED },
                        hiddenQuotes = quotes.filter { it.lifecycleState == QuoteLifecycleState.HIDDEN },
                        deletedQuotes = quotes.filter { it.lifecycleState == QuoteLifecycleState.TRASH },
                        isLoading = false, errorMessage = null
                    )
                }
            }
        }
    }

    /**
     * Switches between Archived and Deleted tabs.
     */
    fun selectTab(tab: ArchiveTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    /** Restores one retained quote without rewriting any content or derived metadata. */
    fun restoreQuote(quote: ArchivedQuote) = mutate("restore quote") {
        val changes = archiveRepository.restoreQuotes(listOf(quote))
        _snackbarEvent.send(ArchiveSnackbarEvent.RestoredQuote(changes))
    }

    /** Undoes only the exact persisted restore mutations, as one batch. */
    fun undoRestore(changes: List<QuoteLifecycleChange>) = mutate("undo restore") {
        archiveRepository.undoChanges(changes)
    }

    /** Restores the currently shown archived or hidden batch atomically. */
    fun restoreAllArchivedQuotes() = mutate("restore quotes") {
        val quotes = _uiState.value.quotesForSelectedTab
        if (quotes.isNotEmpty()) {
            val changes = archiveRepository.restoreQuotes(quotes)
            _snackbarEvent.send(ArchiveSnackbarEvent.RestoredBatch(changes))
        }
    }

    /** Moves a retained archived or hidden quote to trash without losing its metadata. */
    fun softDeleteQuote(quote: ArchivedQuote) = mutate("move quote to trash") {
        archiveRepository.moveToTrash(quote)
    }

    /** Permanently removes only trash rows, in one transaction. */
    fun emptyTrash() = mutate("empty trash") {
        archiveRepository.emptyTrash()
    }

    private fun mutate(action: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (exception: IOException) {
                showMutationFailure(action, exception)
            } catch (exception: IllegalStateException) {
                showMutationFailure(action, exception)
            }
        }
    }

    private suspend fun showMutationFailure(action: String, exception: Exception) {
        Log.e(TAG, "Failed to $action", exception)
        _snackbarEvent.send(ArchiveSnackbarEvent.ShowMessage("Failed to $action: ${exception.message}"))
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
    /** Snackbar event for restoring a single quote, carrying the persisted undo receipt. */
    data class RestoredQuote(val changes: List<QuoteLifecycleChange>) : ArchiveSnackbarEvent

    /** Snackbar event for an atomic restored batch. */
    data class RestoredBatch(val changes: List<QuoteLifecycleChange>) : ArchiveSnackbarEvent

    /** Reports persistence failures without claiming that an action succeeded. */
    data class ShowMessage(val message: String) : ArchiveSnackbarEvent
}
