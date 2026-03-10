package com.po4yka.runicquotes.ui.screens.packs

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.po4yka.runicquotes.data.repository.QuotePackRepository
import com.po4yka.runicquotes.domain.model.QuotePack
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

/**
 * ViewModel for pack detail screen with library toggle support.
 */
@HiltViewModel
class PackDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val quotePackRepository: QuotePackRepository
) : ViewModel() {

    private var packId: Long = savedStateHandle.get<Long>("packId") ?: 0L
    private var loadedPackId: Long? = null

    private val _uiState = MutableStateFlow<PackDetailUiState>(PackDetailUiState.Loading)
    val uiState: StateFlow<PackDetailUiState> = _uiState.asStateFlow()

    /** @suppress */
    companion object {
        private const val TAG = "PackDetailViewModel"
    }

    init {
        initializePackIfNeeded(packId)
    }

    /**
     * Initializes the pack data if the given [packId] has not been loaded yet.
     * Called from the screen composable to pass the route parameter.
     */
    fun initializePackIfNeeded(packId: Long) {
        if (packId == 0L || loadedPackId == packId) return
        this.packId = packId
        loadedPackId = packId
        loadPack()
    }

    private fun loadPack() {
        viewModelScope.launch {
            _uiState.value = PackDetailUiState.Loading
            try {
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
            } catch (e: IOException) {
                Log.e(TAG, "IO error toggling library status", e)
            } catch (e: IllegalStateException) {
                Log.e(TAG, "Invalid state toggling library status", e)
            }
        }
    }

    /**
     * Retries loading the pack after an error.
     */
    fun retry() {
        loadPack()
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
