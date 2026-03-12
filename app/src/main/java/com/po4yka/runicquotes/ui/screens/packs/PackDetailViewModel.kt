package com.po4yka.runicquotes.ui.screens.packs

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.po4yka.runicquotes.data.repository.QuotePackRepository
import com.po4yka.runicquotes.domain.model.QuotePack
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

/**
 * ViewModel for pack detail screen with library toggle support.
 */
@HiltViewModel
class PackDetailViewModel @Inject constructor(
    private val quotePackRepository: QuotePackRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val packId: Long = savedStateHandle.get<Long>(PACK_ID_KEY) ?: 0L

    private val _uiState = MutableStateFlow<PackDetailUiState>(PackDetailUiState.Loading)
    val uiState: StateFlow<PackDetailUiState> = _uiState.asStateFlow()

    private val _events = Channel<PackDetailEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    /** @suppress */
    companion object {
        private const val TAG = "PackDetailViewModel"
        private const val PACK_ID_KEY = "packId"
    }

    init {
        if (packId != 0L) {
            loadPack()
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
            } catch (e: IOException) {
                Log.e(TAG, "IO error loading pack", e)
                _uiState.value = PackDetailUiState.Error("Failed to load pack: ${e.message}")
            } catch (e: IllegalStateException) {
                Log.e(TAG, "Invalid state loading pack", e)
                _uiState.value = PackDetailUiState.Error("Invalid state: ${e.message}")
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
                val updated = current.copy(isInLibrary = !current.isInLibrary)
                quotePackRepository.updatePack(updated)
                _uiState.update { PackDetailUiState.Success(updated) }
                _events.send(
                    PackDetailEvent.ShowMessage(
                        message = if (updated.isInLibrary) {
                            "${updated.quoteCount} quotes added to library"
                        } else {
                            "${updated.name} removed from library"
                        },
                        actionLabel = if (updated.isInLibrary) "View library" else null,
                        action = if (updated.isInLibrary) PackDetailEventAction.VIEW_LIBRARY else null
                    )
                )
            } catch (e: IOException) {
                Log.e(TAG, "IO error toggling library status", e)
                _events.send(PackDetailEvent.ShowMessage("Failed to update pack: ${e.message}"))
            } catch (e: IllegalStateException) {
                Log.e(TAG, "Invalid state toggling library status", e)
                _events.send(PackDetailEvent.ShowMessage("Invalid state: ${e.message}"))
            }
        }
    }

    /**
     * Retries loading the pack after an error.
     */
    fun retry() {
        if (packId != 0L) {
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
