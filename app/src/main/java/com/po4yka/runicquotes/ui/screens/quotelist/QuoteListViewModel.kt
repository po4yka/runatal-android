package com.po4yka.runicquotes.ui.screens.quotelist

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.po4yka.runicquotes.data.preferences.UserPreferencesManager
import com.po4yka.runicquotes.data.repository.QuoteRepository
import com.po4yka.runicquotes.domain.model.Quote
import com.po4yka.runicquotes.domain.model.RunicScript
import com.po4yka.runicquotes.domain.transliteration.TransliterationFactory
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

/** ViewModel for the Library screen with tab filtering. */
@HiltViewModel
class QuoteListViewModel @Inject constructor(
    private val quoteRepository: QuoteRepository,
    private val userPreferencesManager: UserPreferencesManager,
    val transliterationFactory: TransliterationFactory
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuoteListUiState())
    val uiState: StateFlow<QuoteListUiState> = _uiState.asStateFlow()

    /** @suppress */
    companion object {
        private const val TAG = "QuoteListViewModel"
    }

    init {
        viewModelScope.launch {
            restorePersistedFilters()
            loadQuotes()
        }
    }

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
                    }

                    val quoteSearch = state.searchQuery.trim().lowercase(Locale.getDefault())
                    val filteredQuotes = if (quoteSearch.isEmpty()) {
                        baseQuotes
                    } else {
                        baseQuotes.filter { quote ->
                            quote.textLatin.lowercase(Locale.getDefault()).contains(quoteSearch) ||
                                quote.author.lowercase(Locale.getDefault()).contains(quoteSearch)
                        }
                    }

                    state.copy(
                        quotes = filteredQuotes,
                        selectedScript = prefs.selectedScript,
                        selectedFont = prefs.selectedFont,
                        isLoading = false,
                        filterCounts = mapOf(
                            QuoteFilter.ALL to allQuotes.size,
                            QuoteFilter.FAVORITES to favorites.size,
                            QuoteFilter.USER_CREATED to userQuotes.size
                        )
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

    /** Changes the active tab filter and persists it. */
    fun setFilter(filter: QuoteFilter) {
        _uiState.update { it.copy(currentFilter = filter) }
        viewModelScope.launch {
            userPreferencesManager.updateQuoteListFilter(filter.persistedValue)
        }
    }

    /** Updates search query and persists it. */
    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        viewModelScope.launch {
            userPreferencesManager.updateQuoteSearchQuery(query)
        }
    }

    /** Toggles the favorite status of a quote. */
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

    /** Deletes a user-created quote. */
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

    /** Restores a previously deleted quote. */
    fun restoreDeletedQuote(quote: Quote) {
        viewModelScope.launch {
            try {
                quoteRepository.saveUserQuote(quote.copy(isUserCreated = true))
            } catch (e: IOException) {
                Log.e(TAG, "IO error restoring quote", e)
                _uiState.update {
                    it.copy(errorMessage = "Failed to restore quote: ${e.message}")
                }
            } catch (e: IllegalStateException) {
                Log.e(TAG, "Invalid state restoring quote", e)
                _uiState.update {
                    it.copy(errorMessage = "Invalid state: ${e.message}")
                }
            }
        }
    }

    /** Clears any error message. */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private suspend fun restorePersistedFilters() {
        val prefs = userPreferencesManager.userPreferencesFlow.first()
        _uiState.update {
            it.copy(
                currentFilter = QuoteFilter.fromPersistedValue(prefs.quoteListFilter),
                searchQuery = prefs.quoteSearchQuery
            )
        }
    }
}

/** UI state for the Library screen. */
data class QuoteListUiState(
    val quotes: List<Quote> = emptyList(),
    val currentFilter: QuoteFilter = QuoteFilter.ALL,
    val searchQuery: String = "",
    val selectedScript: RunicScript = RunicScript.ELDER_FUTHARK,
    val selectedFont: String = "noto",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val filterCounts: Map<QuoteFilter, Int> = emptyMap()
)

/** Tab filters for the Library screen: All, Favorites, Custom. */
enum class QuoteFilter(
    val displayName: String,
    val persistedValue: String
) {
    ALL("All", "all"),
    FAVORITES("Favorites", "favorites"),
    USER_CREATED("Custom", "user_created");

    /** @suppress */
    companion object {
        /** Restores filter from persisted string value. */
        fun fromPersistedValue(value: String): QuoteFilter {
            return entries.firstOrNull { it.persistedValue == value } ?: ALL
        }
    }
}
