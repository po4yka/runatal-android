package com.po4yka.runicquotes.ui.screens.quote

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.po4yka.runicquotes.data.preferences.UserPreferencesManager
import com.po4yka.runicquotes.data.repository.QuoteRepository
import com.po4yka.runicquotes.domain.model.RunicScript
import com.po4yka.runicquotes.domain.transliteration.TransliterationFactory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Quote screen.
 * Manages quote data, script selection, and transliteration.
 */
@HiltViewModel
class QuoteViewModel @Inject constructor(
    private val quoteRepository: QuoteRepository,
    private val userPreferencesManager: UserPreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<QuoteUiState>(QuoteUiState.Loading)
    val uiState: StateFlow<QuoteUiState> = _uiState.asStateFlow()

    init {
        // Seed database and load initial quote
        viewModelScope.launch {
            quoteRepository.seedIfNeeded()
            loadQuoteOfTheDay()
        }

        // Observe preferences changes
        viewModelScope.launch {
            userPreferencesManager.userPreferencesFlow.collectLatest { preferences ->
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
            val preferences = userPreferencesManager.userPreferencesFlow.replayCache.firstOrNull()
                ?: return

            val quote = quoteRepository.quoteOfTheDay(preferences.selectedScript)

            if (quote != null) {
                val runicText = getRunicText(quote, preferences.selectedScript)

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
                val preferences = userPreferencesManager.userPreferencesFlow.replayCache.firstOrNull()
                    ?: return@launch

                val quote = quoteRepository.randomQuote(preferences.selectedScript)

                if (quote != null) {
                    val runicText = getRunicText(quote, preferences.selectedScript)

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
     * Gets the runic text for a quote based on the selected script.
     * If pre-computed runic text exists, uses it; otherwise transliterates on the fly.
     */
    private fun getRunicText(quote: com.po4yka.runicquotes.data.local.entity.QuoteEntity, script: RunicScript): String {
        // Try to get pre-computed runic text
        val precomputedText = when (script) {
            RunicScript.ELDER_FUTHARK -> quote.runicElder
            RunicScript.YOUNGER_FUTHARK -> quote.runicYounger
            RunicScript.CIRTH -> quote.runicCirth
        }

        // If pre-computed text exists, use it; otherwise transliterate on the fly
        return precomputedText ?: TransliterationFactory.transliterate(quote.textLatin, script)
    }
}
