package com.po4yka.runicquotes.ui.screens.quote

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.po4yka.runicquotes.data.preferences.UserPreferences
import com.po4yka.runicquotes.data.preferences.UserPreferencesManager
import com.po4yka.runicquotes.data.repository.QuoteRepository
import com.po4yka.runicquotes.domain.model.Quote
import com.po4yka.runicquotes.domain.model.RunicScript
import com.po4yka.runicquotes.domain.model.getRunicText
import com.po4yka.runicquotes.domain.transliteration.TransliterationFactory
import com.po4yka.runicquotes.util.QuoteShareManager
import com.po4yka.runicquotes.util.ShareAppearance
import com.po4yka.runicquotes.util.ShareTemplate
import android.util.Log
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
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
    private val quoteShareManager: QuoteShareManager,
    private val transliterationFactory: TransliterationFactory
) : ViewModel() {

    private val _uiState = MutableStateFlow<QuoteUiState>(QuoteUiState.Loading)
    val uiState: StateFlow<QuoteUiState> = _uiState.asStateFlow()
    private val localWordByWordOverride = MutableStateFlow<Boolean?>(null)
    private var currentQuote: Quote? = null
    private var recentQuotes: List<RecentQuoteItem> = emptyList()
    private var latestPreferences: UserPreferences = UserPreferences()

    init {
        // Observe preferences changes and keep quote rendering in sync.
        viewModelScope.launch {
            combine(
                userPreferencesManager.userPreferencesFlow,
                localWordByWordOverride
            ) { preferences, wordByWordOverride ->
                preferences to wordByWordOverride
            }.collectLatest { (preferences, wordByWordOverride) ->
                latestPreferences = preferences
                if (currentQuote == null) {
                    loadQuoteOfTheDay(preferences = preferences, showLoading = true)
                } else {
                    applyPreferencesToCurrentQuote(
                        preferences = preferences,
                        wordByWordOverride = wordByWordOverride
                    )
                }
            }
        }
    }

    /**
     * Loads the quote of the day based on current preferences.
     */
    private suspend fun loadQuoteOfTheDay(
        preferences: UserPreferences? = null,
        showLoading: Boolean = true
    ) {
        if (showLoading) {
            _uiState.update { QuoteUiState.Loading }
        }

        try {
            val resolvedPreferences = preferences ?: userPreferencesManager.userPreferencesFlow.first()

            val quote = quoteRepository.quoteOfTheDay()

            if (quote != null) {
                currentQuote = quote
                loadRecentQuotes(resolvedPreferences, excludeId = quote.id)
                emitSuccessState(quote, resolvedPreferences)
            } else {
                currentQuote = null
                _uiState.update { QuoteUiState.Empty }
            }
        } catch (e: IOException) {
            _uiState.update { QuoteUiState.Error(e.message ?: "Failed to load quote") }
        } catch (e: IllegalStateException) {
            _uiState.update { QuoteUiState.Error(e.message ?: "Invalid state") }
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

                val quote = quoteRepository.randomQuote()

                if (quote != null) {
                    currentQuote = quote
                    loadRecentQuotes(preferences, excludeId = quote.id)
                    emitSuccessState(quote, preferences)
                } else {
                    currentQuote = null
                    _uiState.update { QuoteUiState.Empty }
                }
            } catch (e: IOException) {
                _uiState.update { QuoteUiState.Error(e.message ?: "Failed to load random quote") }
            } catch (e: IllegalStateException) {
                _uiState.update { QuoteUiState.Error(e.message ?: "Invalid state") }
            }
        }
    }

    /**
     * Refreshes the current quote (reloads quote of the day).
     */
    fun refreshQuote() {
        viewModelScope.launch {
            loadQuoteOfTheDay(showLoading = true)
        }
    }

    /**
     * Toggles the favorite status of the current quote.
     */
    @Suppress("SwallowedException") // Intentionally silent fail for non-critical favorite toggle
    fun toggleFavorite() {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is QuoteUiState.Success) {
                try {
                    val nextFavoriteState = !currentState.quote.isFavorite
                    quoteRepository.toggleFavorite(
                        currentState.quote.id,
                        nextFavoriteState
                    )
                    currentQuote = currentState.quote.copy(isFavorite = nextFavoriteState)
                    applyPreferencesToCurrentQuote(userPreferencesManager.userPreferencesFlow.first())
                } catch (_: IOException) {
                    // Error toggling favorite, silently fail - non-critical operation
                } catch (_: IllegalStateException) {
                    // Invalid state, silently fail - non-critical operation
                }
            }
        }
    }

    /**
     * Shares the current quote as an image.
     */
    fun shareQuoteAsImage(
        template: ShareTemplate = ShareTemplate.CARD,
        appearance: ShareAppearance = ShareAppearance.DARK
    ) {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is QuoteUiState.Success) {
                quoteShareManager.shareQuoteAsImage(
                    runicText = currentState.runicText,
                    latinText = currentState.quote.textLatin,
                    author = currentState.quote.author,
                    template = template,
                    appearance = appearance
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

    /**
     * Copies current quote text to the system clipboard.
     */
    fun copyQuoteText() {
        val currentState = _uiState.value
        if (currentState is QuoteUiState.Success) {
            quoteShareManager.copyQuoteToClipboard(
                latinText = currentState.quote.textLatin,
                author = currentState.quote.author
            )
        }
    }

    /**
     * Deletes the current quote and loads a new one.
     */
    fun deleteQuote() {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is QuoteUiState.Success) {
                try {
                    quoteRepository.deleteUserQuote(currentState.quote.id)
                    currentQuote = null
                    loadQuoteOfTheDay(showLoading = true)
                } catch (e: IOException) {
                    Log.e(TAG, "IO error deleting quote", e)
                } catch (e: IllegalStateException) {
                    Log.e(TAG, "Invalid state deleting quote", e)
                }
            }
        }
    }

    /**
     * Updates selected script and keeps preview responsive.
     */
    fun updateSelectedScript(script: RunicScript) {
        viewModelScope.launch {
            userPreferencesManager.updateSelectedScript(script)
        }
    }

    /** Toggles whether the Latin transliteration is visible on quote surfaces. */
    fun toggleTransliterationVisibility() {
        viewModelScope.launch {
            val currentPreferences = userPreferencesManager.userPreferencesFlow.first()
            userPreferencesManager.updateShowTransliteration(!currentPreferences.showTransliteration)
        }
    }

    /**
     * Toggles the local word-by-word transliteration presentation for the current screen session.
     */
    fun toggleWordByWordMode() {
        localWordByWordOverride.update { currentOverride ->
            !(currentOverride ?: latestPreferences.wordByWordTransliterationEnabled)
        }
    }

    private fun applyPreferencesToCurrentQuote(
        preferences: UserPreferences,
        wordByWordOverride: Boolean? = localWordByWordOverride.value
    ) {
        val quote = currentQuote ?: return
        emitSuccessState(quote, preferences, wordByWordOverride)
    }

    private suspend fun loadRecentQuotes(preferences: UserPreferences, excludeId: Long) {
        val allQuotes = quoteRepository.getAllQuotes()
        recentQuotes = allQuotes
            .filter { it.id != excludeId }
            .take(RECENT_QUOTES_LIMIT)
            .map { quote ->
                RecentQuoteItem(
                    quote = quote,
                    runicText = quote.getRunicText(preferences.selectedScript, transliterationFactory)
                )
            }
    }

    private fun emitSuccessState(
        quote: Quote,
        preferences: UserPreferences,
        wordByWordOverride: Boolean? = localWordByWordOverride.value
    ) {
        val runicText = quote.getRunicText(preferences.selectedScript, transliterationFactory)
        val wordBreakdown = transliterationFactory.transliterateWordByWord(
            text = quote.textLatin,
            script = preferences.selectedScript
        ).wordPairs
        val wordByWordEnabled = wordByWordOverride ?: preferences.wordByWordTransliterationEnabled
        // Re-render recent quotes runic text when script changes
        val updatedRecent = recentQuotes.map { item ->
            item.copy(
                runicText = item.quote.getRunicText(
                    preferences.selectedScript,
                    transliterationFactory
                )
            )
        }
        recentQuotes = updatedRecent
        _uiState.update {
            QuoteUiState.Success(
                quote = quote,
                runicText = runicText,
                selectedScript = preferences.selectedScript,
                selectedFont = preferences.selectedFont,
                showTransliteration = preferences.showTransliteration,
                wordByWordEnabled = wordByWordEnabled,
                wordBreakdown = wordBreakdown,
                recentQuotes = updatedRecent
            )
        }
    }

    private companion object {
        const val TAG = "QuoteViewModel"
        const val RECENT_QUOTES_LIMIT = 3
    }
}
