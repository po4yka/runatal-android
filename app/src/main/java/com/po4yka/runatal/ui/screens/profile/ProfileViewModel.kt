package com.po4yka.runatal.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.po4yka.runatal.domain.repository.ReadingHistoryRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.combine
import com.po4yka.runatal.domain.repository.QuoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * ViewModel for the profile screen providing user statistics.
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    quoteRepository: QuoteRepository,
    readingHistoryRepository: ReadingHistoryRepository
) : ViewModel() {

    val uiState: StateFlow<ProfileUiState> = combine(
        quoteRepository.getAllQuotesFlow(), readingHistoryRepository.stats()
    ) { allQuotes, activity ->
        ProfileUiState(
            totalQuotes = allQuotes.size,
            favoriteCount = allQuotes.count { it.isFavorite },
            createdCount = allQuotes.count { it.isUserCreated },
            streakDays = activity.streakDays,
            firstReadDate = activity.firstReadDate
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
    val streakDays: Int = 0,
    val firstReadDate: LocalDate? = null
)
