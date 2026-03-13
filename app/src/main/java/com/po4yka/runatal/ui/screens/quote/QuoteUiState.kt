package com.po4yka.runatal.ui.screens.quote

import com.po4yka.runatal.domain.model.Quote
import com.po4yka.runatal.domain.model.RunicScript
import com.po4yka.runatal.domain.transliteration.WordTransliterationPair

/**
 * UI state for the Quote screen.
 * Uses domain model (Quote) instead of data entity (QuoteEntity)
 * to maintain clean architecture separation.
 */
sealed class QuoteUiState {
    /**
     * Loading state while fetching quote.
     */
    data object Loading : QuoteUiState()

    /**
     * Success state with quote data.
     */
    data class Success(
        val quote: Quote,
        val runicText: String,
        val selectedScript: RunicScript,
        val selectedFont: String,
        val showTransliteration: Boolean,
        val wordByWordEnabled: Boolean,
        val wordBreakdown: List<WordTransliterationPair> = emptyList(),
        val recentQuotes: List<RecentQuoteItem> = emptyList()
    ) : QuoteUiState()

    /**
     * Error state with error message.
     */
    data class Error(val message: String) : QuoteUiState()

    /**
     * Empty state when no quotes are available.
     */
    data object Empty : QuoteUiState()
}

/**
 * A recent quote item with pre-rendered runic text.
 */
data class RecentQuoteItem(
    val quote: Quote,
    val runicText: String
)
