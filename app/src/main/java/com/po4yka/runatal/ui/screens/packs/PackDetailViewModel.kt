package com.po4yka.runatal.ui.screens.packs

import android.util.Log
import android.database.SQLException
import java.io.IOException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.po4yka.runatal.data.repository.QuotePackRepository
import com.po4yka.runatal.domain.model.QuotePack
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

/**
 * ViewModel for pack detail screen with library toggle support.
 */
@HiltViewModel(assistedFactory = PackDetailViewModel.Factory::class)
class PackDetailViewModel @AssistedInject constructor(
    private val quotePackRepository: QuotePackRepository,
    @Assisted private val packId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow<PackDetailUiState>(PackDetailUiState.Loading)
    val uiState: StateFlow<PackDetailUiState> = _uiState.asStateFlow()

    private val _events = Channel<PackDetailEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    /** Creates an entry-scoped ViewModel with the typed navigation argument. */
    @AssistedFactory
    interface Factory {
        /** Creates the ViewModel for the selected navigation entry. */
        fun create(packId: Long): PackDetailViewModel
    }

    /** @suppress */
    companion object {
        private const val TAG = "PackDetailViewModel"
    }

    init {
        if (packId > 0L) {
            loadPack()
        } else {
            _uiState.value = PackDetailUiState.Error("Pack not found")
        }
    }

    private fun loadPack() {
        viewModelScope.launch {
            _uiState.value = PackDetailUiState.Loading
            try {
                quotePackRepository.seedIfNeeded()
                val pack = quotePackRepository.getPackById(packId)
                if (pack != null) {
                    _uiState.value = PackDetailUiState.Success(pack)
                } else {
                    _uiState.value = PackDetailUiState.Error("Pack not found")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                showLoadError(e)
            } catch (e: IllegalStateException) {
                showLoadError(e)
            } catch (e: SQLException) {
                showLoadError(e)
            }
        }
    }

    /**
     * Toggles library membership for the current pack.
     */
    fun toggleLibrary() {
        val current = (_uiState.value as? PackDetailUiState.Success)?.pack ?: return
        viewModelScope.launch {
            try {
                val updated = quotePackRepository.toggleLibrary(current.id)
                _uiState.update { PackDetailUiState.Success(updated) }
                _events.send(
                    PackDetailEvent.ShowMessage(
                        message = if (updated.isInLibrary) {
                            "${updated.quoteCount} pack quotes available in library"
                        } else {
                            "${updated.name} removed from library"
                        },
                        actionLabel = if (updated.isInLibrary) "View library" else null,
                        action = if (updated.isInLibrary) PackDetailEventAction.VIEW_LIBRARY else null
                    )
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                showUpdateError(e)
            } catch (e: IllegalStateException) {
                showUpdateError(e)
            } catch (e: SQLException) {
                showUpdateError(e)
            }
        }
    }

    private fun showLoadError(error: Exception) {
        Log.e(TAG, "Error loading pack", error)
        _uiState.value = PackDetailUiState.Error("Failed to load pack: ${error.message}")
    }

    private suspend fun showUpdateError(error: Exception) {
        Log.e(TAG, "Error toggling library status", error)
        _events.send(PackDetailEvent.ShowMessage("Failed to update pack: ${error.message}"))
    }

    /**
     * Retries loading the pack after an error.
     */
    fun retry() {
        if (packId > 0L) {
            loadPack()
        }
    }
}

/**
 * UI state for the pack detail screen.
 */
sealed interface PackDetailUiState {
    /** Pack data is being loaded. */
    data object Loading : PackDetailUiState

    /** Pack loaded successfully. */
    data class Success(val pack: QuotePack) : PackDetailUiState

    /** An error occurred while loading the pack. */
    data class Error(val message: String) : PackDetailUiState
}

/** One-off UI events emitted by the pack detail screen. */
sealed interface PackDetailEvent {
    /** Displays transient pack-related feedback. */
    data class ShowMessage(
        val message: String,
        val actionLabel: String? = null,
        val action: PackDetailEventAction? = null
    ) : PackDetailEvent
}

/** Supported actions for pack-detail transient feedback. */
enum class PackDetailEventAction {
    VIEW_LIBRARY
}
