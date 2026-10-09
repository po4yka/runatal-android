package com.po4yka.runatal.ui.screens.references

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.po4yka.runatal.data.repository.RuneReferenceRepository
import com.po4yka.runatal.domain.model.RuneReference
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import java.io.IOException

/**
 * ViewModel for displaying detailed information about a single rune.
 */
@HiltViewModel(assistedFactory = RuneDetailViewModel.Factory::class)
class RuneDetailViewModel @AssistedInject constructor(
    private val runeReferenceRepository: RuneReferenceRepository,
    @Assisted private val runeId: Long
) : ViewModel() {

    private var loadingJob: Job? = null
    private val messages = Channel<String>(Channel.BUFFERED)
    val bookmarkMessages = messages.receiveAsFlow()

    private val _uiState = MutableStateFlow<RuneDetailUiState>(RuneDetailUiState.Loading)
    val uiState: StateFlow<RuneDetailUiState> = _uiState.asStateFlow()

    /** Creates an entry-scoped ViewModel with the typed navigation argument. */
    @AssistedFactory
    interface Factory {
        /** Creates the ViewModel for the selected navigation entry. */
        fun create(runeId: Long): RuneDetailViewModel
    }

    /** @suppress */
    companion object {
        private const val TAG = "RuneDetailViewModel"
    }

    init {
        if (runeId > 0L) {
            loadRune()
        } else {
            _uiState.value = RuneDetailUiState.Error("Rune not found")
        }
    }

    private fun loadRune() {
        loadingJob?.cancel()
        loadingJob = viewModelScope.launch {
            _uiState.value = RuneDetailUiState.Loading
            try {
                val rune = runeReferenceRepository.getRuneById(runeId)
                if (rune != null) {
                    runeReferenceRepository.observeBookmark(rune.id).collect { bookmarked ->
                        _uiState.value = RuneDetailUiState.Success(rune, bookmarked)
                    }
                } else {
                    _uiState.value = RuneDetailUiState.Error("Rune not found")
                }
            } catch (e: IOException) {
                Log.e(TAG, "IO error loading rune", e)
                _uiState.value = RuneDetailUiState.Error("Failed to load rune: ${e.message}")
            } catch (e: IllegalStateException) {
                Log.e(TAG, "Invalid state loading rune", e)
                _uiState.value = RuneDetailUiState.Error("Invalid state: ${e.message}")
            }
        }
    }

    /** Atomically toggles the current rune bookmark. */
    fun toggleBookmark() {
        if (_uiState.value !is RuneDetailUiState.Success) return
        viewModelScope.launch {
            try {
                runeReferenceRepository.toggleBookmark(runeId)
            } catch (exception: IOException) {
                Log.e(TAG, "Could not update rune bookmark", exception)
                messages.send("Could not update saved rune")
            } catch (exception: IllegalStateException) {
                Log.e(TAG, "Rune bookmark state is invalid", exception)
                messages.send("Rune is no longer available")
            }
        }
    }

    /**
     * Retries loading the rune after an error.
     */
    fun retry() {
        if (runeId != 0L) {
            loadRune()
        }
    }
}

/**
 * UI state for the rune detail screen.
 */
sealed interface RuneDetailUiState {
    /** Rune data is being loaded. */
    data object Loading : RuneDetailUiState

    /** Rune loaded successfully. */
    data class Success(val rune: RuneReference, val isBookmarked: Boolean = false) : RuneDetailUiState

    /** An error occurred while loading the rune. */
    data class Error(val message: String) : RuneDetailUiState
}
