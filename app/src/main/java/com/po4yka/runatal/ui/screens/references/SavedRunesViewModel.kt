package com.po4yka.runatal.ui.screens.references

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.po4yka.runatal.data.repository.RuneReferenceRepository
import com.po4yka.runatal.domain.model.RuneReference
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Live saved rune references. */
@HiltViewModel
internal class SavedRunesViewModel @Inject constructor(repository: RuneReferenceRepository) : ViewModel() {
    val uiState: StateFlow<SavedRunesUiState> = repository.getBookmarkedRunesFlow()
        .map { SavedRunesUiState.Ready(it) as SavedRunesUiState }
        .catch { emit(SavedRunesUiState.Error("Unable to load saved runes")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SavedRunesUiState.Loading)
}

internal sealed interface SavedRunesUiState {
    data object Loading : SavedRunesUiState
    data class Ready(val runes: List<RuneReference>) : SavedRunesUiState
    data class Error(val message: String) : SavedRunesUiState
}
