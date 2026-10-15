package com.po4yka.runatal.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.po4yka.runatal.domain.model.QuoteReading
import com.po4yka.runatal.domain.model.ReadingStats
import com.po4yka.runatal.domain.repository.ReadingHistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** Presents persisted reading activity without inventing reading events. */
@HiltViewModel
internal class ReadingHistoryViewModel @Inject constructor(repository: ReadingHistoryRepository) : ViewModel() {
    val uiState: StateFlow<ReadingHistoryUiState> = combine(repository.readings(), repository.stats()) {
        readings, stats -> ReadingHistoryUiState.Ready(readings, stats) as ReadingHistoryUiState
    }.catch { emit(ReadingHistoryUiState.Error("Unable to load reading history")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReadingHistoryUiState.Loading)
}

internal sealed interface ReadingHistoryUiState {
    data object Loading : ReadingHistoryUiState
    data class Ready(val readings: List<QuoteReading>, val stats: ReadingStats) : ReadingHistoryUiState
    data class Error(val message: String) : ReadingHistoryUiState
}
