package com.po4yka.runicquotes.ui.screens.quote

import com.po4yka.runicquotes.data.local.entity.QuoteEntity
import com.po4yka.runicquotes.domain.model.RunicScript

/**
 * UI state for the Quote screen.
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
        val quote: QuoteEntity,
        val runicText: String,
        val selectedScript: RunicScript,
        val selectedFont: String,
        val showTransliteration: Boolean
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
