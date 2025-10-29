package com.po4yka.runicquotes.ui.screens.quote

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.po4yka.runicquotes.data.preferences.UserPreferencesManager
import com.po4yka.runicquotes.data.repository.QuoteRepository
import com.po4yka.runicquotes.domain.model.getRunicText
import com.po4yka.runicquotes.util.QuoteShareManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Quote screen.
 * Manages UI state and coordinates between repository and preferences.
 * Business logic is delegated to domain layer.
 */
@HiltViewModel
class QuoteViewModel @Inject constructor(
    private val quoteRepository: QuoteRepository,
    private val userPreferencesManager: UserPreferencesManager,
    private val quoteShareManager: QuoteShareManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<QuoteUiState>(QuoteUiState.Loading)
    val uiState: StateFlow<QuoteUiState> = _uiState.asStateFlow()

    init {
        // Load initial quote
        viewModelScope.launch {
            loadQuoteOfTheDay()
        }

        // Observe preferences changes
        viewModelScope.launch {
            userPreferencesManager.userPreferencesFlow.collectLatest {
                // Reload quote when preferences change
                loadQuoteOfTheDay()
            }
        }
    }

    /**
     * Loads the quote of the day based on current preferences.
     */
    private suspend fun loadQuoteOfTheDay() {
        _uiState.update { QuoteUiState.Loading }

        try {
            val preferences = userPreferencesManager.userPreferencesFlow.first()

            val quote = quoteRepository.quoteOfTheDay(preferences.selectedScript)

            if (quote != null) {
                // Use domain extension function for business logic
                val runicText = quote.getRunicText(preferences.selectedScript)

                _uiState.update {
                    QuoteUiState.Success(
                        quote = quote,
                        runicText = runicText,
                        selectedScript = preferences.selectedScript,
                        selectedFont = preferences.selectedFont,
                        showTransliteration = preferences.showTransliteration
                    )
                }
            } else {
                _uiState.update { QuoteUiState.Empty }
            }
        } catch (e: Exception) {
            _uiState.update { QuoteUiState.Error(e.message ?: "Unknown error") }
        }
    }

    /**
     * Gets a random quote instead of the daily quote.
     */
    fun getRandomQuote() {
        viewModelScope.launch {
            _uiState.update { QuoteUiState.Loading }

            try {
                val preferences = userPreferencesManager.userPreferencesFlow.first()

                val quote = quoteRepository.randomQuote(preferences.selectedScript)

                if (quote != null) {
                    // Use domain extension function for business logic
                    val runicText = quote.getRunicText(preferences.selectedScript)

                    _uiState.update {
                        QuoteUiState.Success(
                            quote = quote,
                            runicText = runicText,
                            selectedScript = preferences.selectedScript,
                            selectedFont = preferences.selectedFont,
                            showTransliteration = preferences.showTransliteration
                        )
                    }
                } else {
                    _uiState.update { QuoteUiState.Empty }
                }
            } catch (e: Exception) {
                _uiState.update { QuoteUiState.Error(e.message ?: "Unknown error") }
            }
        }
    }

    /**
     * Refreshes the current quote (reloads quote of the day).
     */
    fun refreshQuote() {
        viewModelScope.launch {
            loadQuoteOfTheDay()
        }
    }

    /**
     * Toggles the favorite status of the current quote.
     */
    fun toggleFavorite() {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is QuoteUiState.Success) {
                try {
                    quoteRepository.toggleFavorite(
                        currentState.quote.id,
                        !currentState.quote.isFavorite
                    )
                    // Reload to reflect the change
                    loadQuoteOfTheDay()
                } catch (e: Exception) {
                    // Silently fail, could add error state if needed
                }
            }
        }
    }

    /**
     * Shares the current quote as an image.
     */
    fun shareQuoteAsImage() {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is QuoteUiState.Success) {
                quoteShareManager.shareQuoteAsImage(
                    runicText = currentState.runicText,
                    latinText = currentState.quote.textLatin,
                    author = currentState.quote.author
                )
            }
        }
    }

    /**
     * Shares the current quote as text.
     */
    fun shareQuoteText() {
        val currentState = _uiState.value
        if (currentState is QuoteUiState.Success) {
            quoteShareManager.shareQuoteText(
                latinText = currentState.quote.textLatin,
                author = currentState.quote.author
            )
        }
    }
}
