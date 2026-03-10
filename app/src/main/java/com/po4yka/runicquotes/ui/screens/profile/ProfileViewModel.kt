package com.po4yka.runicquotes.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.po4yka.runicquotes.data.repository.QuoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * ViewModel for the profile screen providing user statistics.
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    quoteRepository: QuoteRepository
) : ViewModel() {

    val uiState: StateFlow<ProfileUiState> = combine(
        quoteRepository.getAllQuotesFlow(),
        quoteRepository.getFavoritesFlow(),
        quoteRepository.getUserQuotesFlow()
    ) { allQuotes, favorites, userQuotes ->
        ProfileUiState(
            totalQuotes = allQuotes.size,
            favoriteCount = favorites.size,
            createdCount = userQuotes.size
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
