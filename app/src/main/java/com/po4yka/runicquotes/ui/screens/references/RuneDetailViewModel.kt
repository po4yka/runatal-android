package com.po4yka.runicquotes.ui.screens.references

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.po4yka.runicquotes.data.repository.RuneReferenceRepository
import com.po4yka.runicquotes.domain.model.RuneReference
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

/**
 * ViewModel for displaying detailed information about a single rune.
 */
@HiltViewModel
class RuneDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val runeReferenceRepository: RuneReferenceRepository
) : ViewModel() {

    private var runeId: Long = savedStateHandle.get<Long>("runeId") ?: 0L
    private var loadedRuneId: Long? = null

    private val _uiState = MutableStateFlow<RuneDetailUiState>(RuneDetailUiState.Loading)
    val uiState: StateFlow<RuneDetailUiState> = _uiState.asStateFlow()

    /** @suppress */
    companion object {
        private const val TAG = "RuneDetailViewModel"
    }

    init {
        initializeRuneIfNeeded(runeId)
    }

    /**
     * Initializes the rune data if the given [runeId] has not been loaded yet.
     * Called from the screen composable to pass the route parameter.
     */
    fun initializeRuneIfNeeded(runeId: Long) {
        if (runeId == 0L || loadedRuneId == runeId) return
        this.runeId = runeId
        loadedRuneId = runeId
        loadRune()
    }

    private fun loadRune() {
        viewModelScope.launch {
            _uiState.value = RuneDetailUiState.Loading
            try {
                val rune = runeReferenceRepository.getRuneById(runeId)
                if (rune != null) {
                    _uiState.value = RuneDetailUiState.Success(rune)
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

    /**
     * Retries loading the rune after an error.
     */
    fun retry() {
        loadRune()
    }
}

/**
 * UI state for the rune detail screen.
 */
sealed interface RuneDetailUiState {
    /** Rune data is being loaded. */
    data object Loading : RuneDetailUiState

    /** Rune loaded successfully. */
    data class Success(val rune: RuneReference) : RuneDetailUiState

    /** An error occurred while loading the rune. */
    data class Error(val message: String) : RuneDetailUiState
}
