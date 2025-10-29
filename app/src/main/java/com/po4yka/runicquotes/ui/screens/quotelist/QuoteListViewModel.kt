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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

/**
 * ViewModel for browsing all quotes with filtering options.
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
        loadQuotes()
    }

    /**
     * Loads quotes based on the current filter.
     */
    private fun loadQuotes() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                // Combine all flows
                combine(
                    quoteRepository.getAllQuotesFlow(),
                    quoteRepository.getUserQuotesFlow(),
                    quoteRepository.getFavoritesFlow(),
                    userPreferencesManager.userPreferencesFlow,
                    _uiState
                ) { allQuotes, userQuotes, favorites, prefs, state ->
                    val filteredQuotes = when (state.currentFilter) {
                        QuoteFilter.ALL -> allQuotes
                        QuoteFilter.USER_CREATED -> userQuotes
                        QuoteFilter.FAVORITES -> favorites
                        QuoteFilter.SYSTEM -> allQuotes.filter { !it.isUserCreated }
                    }

                    state.copy(
                        quotes = filteredQuotes,
                        selectedScript = prefs.selectedScript,
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
     * Changes the current filter.
     */
    fun setFilter(filter: QuoteFilter) {
        _uiState.update { it.copy(currentFilter = filter) }
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
}

/**
 * UI state for quote list screen.
 */
data class QuoteListUiState(
    val quotes: List<Quote> = emptyList(),
    val currentFilter: QuoteFilter = QuoteFilter.ALL,
    val selectedScript: RunicScript = RunicScript.ELDER_FUTHARK,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Available filters for quote list.
 */
enum class QuoteFilter(val displayName: String) {
    ALL("All Quotes"),
    FAVORITES("Favorites"),
    USER_CREATED("My Quotes"),
    SYSTEM("System Quotes")
}
