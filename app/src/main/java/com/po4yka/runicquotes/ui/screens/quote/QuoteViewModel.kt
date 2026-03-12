package com.po4yka.runicquotes.ui.screens.quote

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.po4yka.runicquotes.data.preferences.UserPreferences
import com.po4yka.runicquotes.data.preferences.UserPreferencesManager
import com.po4yka.runicquotes.data.repository.NoOpTranslationRepository
import com.po4yka.runicquotes.data.repository.QuoteRepository
import com.po4yka.runicquotes.data.repository.TranslationRepository
import com.po4yka.runicquotes.domain.model.Quote
import com.po4yka.runicquotes.domain.model.RunicScript
import com.po4yka.runicquotes.domain.transliteration.TransliterationFactory
import com.po4yka.runicquotes.domain.usecase.quote.BuildQuotePresentationUseCase
import com.po4yka.runicquotes.domain.usecase.quote.LoadQuoteSurfaceUseCase
import com.po4yka.runicquotes.domain.usecase.quote.QuotePresentation
import com.po4yka.runicquotes.domain.usecase.quote.QuoteSurfaceSource
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
@Suppress("TooManyFunctions")
internal class QuoteViewModel @Inject constructor(
    private val quoteRepository: QuoteRepository,
    private val userPreferencesManager: UserPreferencesManager,
    private val loadQuoteSurfaceUseCase: LoadQuoteSurfaceUseCase,
    private val buildQuotePresentationUseCase: BuildQuotePresentationUseCase
) : ViewModel() {

    internal constructor(
        quoteRepository: QuoteRepository,
        userPreferencesManager: UserPreferencesManager,
        transliterationFactory: TransliterationFactory,
        translationRepository: TranslationRepository = NoOpTranslationRepository
    ) : this(
        quoteRepository = quoteRepository,
        userPreferencesManager = userPreferencesManager,
        loadQuoteSurfaceUseCase = LoadQuoteSurfaceUseCase(
            quoteRepository = quoteRepository,
            buildQuotePresentationUseCase = BuildQuotePresentationUseCase(
                transliterationFactory = transliterationFactory,
                translationRepository = translationRepository
            )
        ),
        buildQuotePresentationUseCase = BuildQuotePresentationUseCase(
            transliterationFactory = transliterationFactory,
            translationRepository = translationRepository
        )
    )

    private val _uiState = MutableStateFlow<QuoteUiState>(QuoteUiState.Loading)
    val uiState: StateFlow<QuoteUiState> = _uiState.asStateFlow()
    private val localWordByWordOverride = MutableStateFlow<Boolean?>(null)
    private var currentQuote: Quote? = null
    private var recentQuoteCandidates: List<Quote> = emptyList()
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
            val loadedQuoteSurface = loadQuoteSurfaceUseCase(
                source = QuoteSurfaceSource.DAILY,
                selectedScript = resolvedPreferences.selectedScript
            )

            if (loadedQuoteSurface != null) {
                currentQuote = loadedQuoteSurface.quote
                recentQuoteCandidates = loadedQuoteSurface.recentQuoteCandidates
                emitSuccessState(loadedQuoteSurface.presentation, resolvedPreferences)
            } else {
                currentQuote = null
                recentQuoteCandidates = emptyList()
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
                val loadedQuoteSurface = loadQuoteSurfaceUseCase(
                    source = QuoteSurfaceSource.RANDOM,
                    selectedScript = preferences.selectedScript
                )

                if (loadedQuoteSurface != null) {
                    currentQuote = loadedQuoteSurface.quote
                    recentQuoteCandidates = loadedQuoteSurface.recentQuoteCandidates
                    emitSuccessState(loadedQuoteSurface.presentation, preferences)
                } else {
                    currentQuote = null
                    recentQuoteCandidates = emptyList()
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

    private suspend fun applyPreferencesToCurrentQuote(
        preferences: UserPreferences,
        wordByWordOverride: Boolean? = localWordByWordOverride.value
    ) {
        val quote = currentQuote ?: return
        val presentation = buildQuotePresentationUseCase(
            quote = quote,
            selectedScript = preferences.selectedScript,
            recentQuoteCandidates = recentQuoteCandidates
        )
        emitSuccessState(presentation, preferences, wordByWordOverride)
    }

    private suspend fun emitSuccessState(
        presentation: QuotePresentation,
        preferences: UserPreferences,
        wordByWordOverride: Boolean? = localWordByWordOverride.value
    ) {
        val wordByWordEnabled = wordByWordOverride ?: preferences.wordByWordTransliterationEnabled
        val updatedRecent = presentation.recentQuotes.map { item ->
            RecentQuoteItem(
                quote = item.quote,
                runicText = item.runicText
            )
        }
        _uiState.update {
            QuoteUiState.Success(
                quote = presentation.quote,
                runicText = presentation.runicText,
                selectedScript = preferences.selectedScript,
                selectedFont = preferences.selectedFont,
                showTransliteration = preferences.showTransliteration,
                wordByWordEnabled = wordByWordEnabled,
                wordBreakdown = presentation.wordBreakdown,
                recentQuotes = updatedRecent
            )
        }
    }

    private companion object {
        const val TAG = "QuoteViewModel"
    }
}
