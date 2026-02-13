package com.po4yka.runicquotes.ui.screens.quotelist

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.po4yka.runicquotes.data.preferences.UserPreferencesManager
import com.po4yka.runicquotes.data.repository.QuoteRepository
import com.po4yka.runicquotes.domain.model.Quote
import com.po4yka.runicquotes.domain.model.RunicScript
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.Locale
import javax.inject.Inject

/**
 * ViewModel for browsing all quotes with smart filtering.
 */
@HiltViewModel
class QuoteListViewModel @Inject constructor(
    private val quoteRepository: QuoteRepository,
    private val userPreferencesManager: UserPreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuoteListUiState())
    val uiState: StateFlow<QuoteListUiState> = _uiState.asStateFlow()

    companion object {
        private const val TAG = "QuoteListViewModel"
    }

    init {
        viewModelScope.launch {
            restorePersistedFilters()
            loadQuotes()
        }
    }

    /**
     * Loads quotes and applies smart filtering.
     */
    private fun loadQuotes() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                combine(
                    quoteRepository.getAllQuotesFlow(),
                    quoteRepository.getUserQuotesFlow(),
                    quoteRepository.getFavoritesFlow(),
                    userPreferencesManager.userPreferencesFlow,
                    _uiState
                ) { allQuotes, userQuotes, favorites, prefs, state ->
                    val baseQuotes = when (state.currentFilter) {
                        QuoteFilter.ALL -> allQuotes
                        QuoteFilter.USER_CREATED -> userQuotes
                        QuoteFilter.FAVORITES -> favorites
                        QuoteFilter.SYSTEM -> allQuotes.filter { !it.isUserCreated }
                    }

                    val quoteSearch = state.searchQuery.trim().lowercase(Locale.getDefault())
                    val filteredBySearch = if (quoteSearch.isEmpty()) {
                        baseQuotes
                    } else {
                        baseQuotes.filter { quote ->
                            quote.textLatin.lowercase(Locale.getDefault()).contains(quoteSearch) ||
                                quote.author.lowercase(Locale.getDefault()).contains(quoteSearch)
                        }
                    }

                    val filteredByAuthor = if (state.selectedAuthor == null) {
                        filteredBySearch
                    } else {
                        filteredBySearch.filter { it.author == state.selectedAuthor }
                    }

                    val filteredQuotes = when (state.lengthFilter) {
                        QuoteLengthFilter.ANY -> filteredByAuthor
                        QuoteLengthFilter.SHORT -> filteredByAuthor.filter { it.textLatin.length <= 80 }
                        QuoteLengthFilter.MEDIUM -> filteredByAuthor.filter {
                            it.textLatin.length in 81..160
                        }
                        QuoteLengthFilter.LONG -> filteredByAuthor.filter { it.textLatin.length > 160 }
                    }

                    state.copy(
                        quotes = filteredQuotes,
                        availableAuthors = allQuotes.map { it.author }.distinct().sorted(),
                        selectedScript = prefs.selectedScript,
                        selectedFont = prefs.selectedFont,
                        isLoading = false
                    )
                }.collect { newState ->
                    _uiState.value = newState
                }
            } catch (e: IOException) {
                Log.e(TAG, "IO error loading quotes", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to load quotes: ${e.message}"
                    )
                }
            } catch (e: IllegalStateException) {
                Log.e(TAG, "Invalid state loading quotes", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Invalid state: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Changes the current base filter and persists it.
     */
    fun setFilter(filter: QuoteFilter) {
        _uiState.update { it.copy(currentFilter = filter) }
        viewModelScope.launch {
            userPreferencesManager.updateQuoteListFilter(filter.persistedValue)
        }
    }

    /**
     * Updates quote search query and persists it.
     */
    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        viewModelScope.launch {
            userPreferencesManager.updateQuoteSearchQuery(query)
        }
    }

    /**
     * Updates author filter and persists it.
     */
    fun updateAuthorFilter(author: String?) {
        _uiState.update { it.copy(selectedAuthor = author) }
        viewModelScope.launch {
            userPreferencesManager.updateQuoteAuthorFilter(author ?: "")
        }
    }

    /**
     * Updates quote length filter and persists it.
     */
    fun updateLengthFilter(lengthFilter: QuoteLengthFilter) {
        _uiState.update { it.copy(lengthFilter = lengthFilter) }
        viewModelScope.launch {
            userPreferencesManager.updateQuoteLengthFilter(lengthFilter.persistedValue)
        }
    }

    /**
     * Clears all smart filters and persists defaults.
     */
    fun clearSmartFilters() {
        _uiState.update {
            it.copy(
                searchQuery = "",
                selectedAuthor = null,
                lengthFilter = QuoteLengthFilter.ANY
            )
        }
        viewModelScope.launch {
            userPreferencesManager.updateQuoteSearchQuery("")
            userPreferencesManager.updateQuoteAuthorFilter("")
            userPreferencesManager.updateQuoteLengthFilter(QuoteLengthFilter.ANY.persistedValue)
        }
    }

    /**
     * Toggles the favorite status of a quote.
     */
    fun toggleFavorite(quote: Quote) {
        viewModelScope.launch {
            try {
                quoteRepository.toggleFavorite(quote.id, !quote.isFavorite)
            } catch (e: IOException) {
                Log.e(TAG, "IO error toggling favorite", e)
                _uiState.update {
                    it.copy(errorMessage = "Failed to update favorite: ${e.message}")
                }
            } catch (e: IllegalStateException) {
                Log.e(TAG, "Invalid state toggling favorite", e)
                _uiState.update {
                    it.copy(errorMessage = "Invalid state: ${e.message}")
                }
            }
        }
    }

    /**
     * Deletes a user-created quote.
     */
    fun deleteQuote(quoteId: Long) {
        viewModelScope.launch {
            try {
                quoteRepository.deleteUserQuote(quoteId)
            } catch (e: IOException) {
                Log.e(TAG, "IO error deleting quote", e)
                _uiState.update {
                    it.copy(errorMessage = "Failed to delete quote: ${e.message}")
                }
            } catch (e: IllegalStateException) {
                Log.e(TAG, "Invalid state deleting quote", e)
                _uiState.update {
                    it.copy(errorMessage = "Invalid state: ${e.message}")
                }
            }
        }
    }

    /**
     * Clears any error message.
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * Whether any smart filter is active.
     */
    fun hasSmartFilters(state: QuoteListUiState): Boolean {
        return state.searchQuery.isNotBlank() ||
            state.selectedAuthor != null ||
            state.lengthFilter != QuoteLengthFilter.ANY
    }

    private suspend fun restorePersistedFilters() {
        val prefs = userPreferencesManager.userPreferencesFlow.first()
        _uiState.update {
            it.copy(
                currentFilter = QuoteFilter.fromPersistedValue(prefs.quoteListFilter),
                searchQuery = prefs.quoteSearchQuery,
                selectedAuthor = prefs.quoteAuthorFilter.ifBlank { null },
                lengthFilter = QuoteLengthFilter.fromPersistedValue(prefs.quoteLengthFilter)
            )
        }
    }
}

/**
 * UI state for quote list screen.
 */
data class QuoteListUiState(
    val quotes: List<Quote> = emptyList(),
    val currentFilter: QuoteFilter = QuoteFilter.ALL,
    val searchQuery: String = "",
    val selectedAuthor: String? = null,
    val lengthFilter: QuoteLengthFilter = QuoteLengthFilter.ANY,
    val availableAuthors: List<String> = emptyList(),
    val selectedScript: RunicScript = RunicScript.ELDER_FUTHARK,
    val selectedFont: String = "noto",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Available base filters for quote list.
 */
enum class QuoteFilter(
    val displayName: String,
    val persistedValue: String
) {
    ALL("All Quotes", "all"),
    FAVORITES("Favorites", "favorites"),
    USER_CREATED("My Quotes", "user_created"),
    SYSTEM("System Quotes", "system");

    companion object {
        fun fromPersistedValue(value: String): QuoteFilter {
            return entries.firstOrNull { it.persistedValue == value } ?: ALL
        }
    }
}

/**
 * Length-based smart filters.
 */
enum class QuoteLengthFilter(
    val displayName: String,
    val persistedValue: String
) {
    ANY("Any Length", "any"),
    SHORT("Short", "short"),
    MEDIUM("Medium", "medium"),
    LONG("Long", "long");

    companion object {
        fun fromPersistedValue(value: String): QuoteLengthFilter {
            return entries.firstOrNull { it.persistedValue == value } ?: ANY
        }
    }
}
