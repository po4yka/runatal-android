package com.po4yka.runatal.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.po4yka.runatal.domain.repository.QuoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * ViewModel for the profile screen providing user statistics.
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    quoteRepository: QuoteRepository
) : ViewModel() {

    val uiState: StateFlow<ProfileUiState> = quoteRepository.getAllQuotesFlow().map { allQuotes ->
        ProfileUiState(
            totalQuotes = allQuotes.size,
            favoriteCount = allQuotes.count { it.isFavorite },
            createdCount = allQuotes.count { it.isUserCreated }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), ProfileUiState())
}

/**
 * UI state for the profile screen.
 */
data class ProfileUiState(
    val totalQuotes: Int = 0,
    val favoriteCount: Int = 0,
    val createdCount: Int = 0,
    val streakDays: Int = 0
)
