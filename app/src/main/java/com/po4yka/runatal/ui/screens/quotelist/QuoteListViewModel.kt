package com.po4yka.runatal.ui.screens.quotelist

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.po4yka.runatal.data.preferences.UserPreferencesManager
import com.po4yka.runatal.data.preferences.UserPreferences
import com.po4yka.runatal.domain.model.Quote
import com.po4yka.runatal.domain.model.QuoteLifecycleChange
import com.po4yka.runatal.domain.model.RunicScript
import com.po4yka.runatal.domain.model.ResolvedQuoteRendering
import com.po4yka.runatal.domain.usecase.quote.ResolveQuoteRenderingUseCase
import com.po4yka.runatal.domain.repository.QuoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.Locale
import javax.inject.Inject

/** ViewModel for the Library screen with tab filtering. */
@HiltViewModel
internal class QuoteListViewModel @Inject constructor(
    private val quoteRepository: QuoteRepository,
    private val userPreferencesManager: UserPreferencesManager,
    private val resolveQuoteRenderingUseCase: ResolveQuoteRenderingUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuoteListUiState(isLoading = true))
    val uiState: StateFlow<QuoteListUiState> = _uiState.asStateFlow()
    private val currentFilter = MutableStateFlow(QuoteFilter.ALL)
    private val searchQuery = MutableStateFlow("")
    private val _events = Channel<QuoteListEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    /** @suppress */
    companion object {
        private const val TAG = "QuoteListViewModel"
    }

    init {
        viewModelScope.launch {
            restorePersistedFilters()
            observeQuotes()
        }
    }

    private fun observeQuotes() {
        viewModelScope.launch {
            try {
                combine(
                    combine(
                        quoteRepository.getAllQuotesFlow(),
                        userPreferencesManager.userPreferencesFlow
                    ) { allQuotes, prefs ->
                        QuoteListSourceState(
                            allQuotes = allQuotes,
                            preferences = prefs
                        )
                    },
                    combine(currentFilter, searchQuery) { selectedFilter, query ->
                        QuoteListFilterState(
                            currentFilter = selectedFilter,
                            searchQuery = query
                        )
                    }
                ) { sourceState, filterState ->
                    val favoriteQuotes = sourceState.allQuotes.filter { it.isFavorite }
                    val userQuotes = sourceState.allQuotes.filter { it.isUserCreated }
                    val baseQuotes = when (filterState.currentFilter) {
                        QuoteFilter.ALL -> sourceState.allQuotes
                        QuoteFilter.USER_CREATED -> userQuotes
                        QuoteFilter.FAVORITES -> favoriteQuotes
                    }

                    val quoteSearch = filterState.searchQuery.trim().lowercase(Locale.getDefault())
                    val filteredQuotes = if (quoteSearch.isEmpty()) {
                        baseQuotes
                    } else {
                        baseQuotes.filter { quote ->
                            quote.textLatin.lowercase(Locale.getDefault()).contains(quoteSearch) ||
                                quote.author.lowercase(Locale.getDefault()).contains(quoteSearch)
                        }
                    }
                    val quoteItems = filteredQuotes.map { quote ->
                        val rendering = resolveQuoteRenderingUseCase(quote, sourceState.preferences.selectedScript)
                        QuoteListItemUiModel(quote, rendering.glyphOutput, rendering)
                    }

                    QuoteListUiState(
                        quotes = filteredQuotes,
                        quoteItems = quoteItems,
                        currentFilter = filterState.currentFilter,
                        searchQuery = filterState.searchQuery,
                        selectedScript = sourceState.preferences.selectedScript,
                        selectedFont = sourceState.preferences.selectedFont,
                        isLoading = false,
                        filterCounts = mapOf(
                            QuoteFilter.ALL to sourceState.allQuotes.size,
                            QuoteFilter.FAVORITES to favoriteQuotes.size,
                            QuoteFilter.USER_CREATED to userQuotes.size
                        )
                    )
                }.collect { newState ->
                    _uiState.value = newState
                }
            } catch (e: IOException) {
                Log.e(TAG, "IO error loading quotes", e)
                _uiState.update { it.copy(isLoading = false) }
                _events.send(QuoteListEvent.ShowMessage("Failed to load quotes: ${e.message}"))
            } catch (e: IllegalStateException) {
                Log.e(TAG, "Invalid state loading quotes", e)
                _uiState.update { it.copy(isLoading = false) }
                _events.send(QuoteListEvent.ShowMessage("Invalid state: ${e.message}"))
            }
        }
    }

    /** Changes the active tab filter and persists it. */
    fun setFilter(filter: QuoteFilter) {
        currentFilter.value = filter
        viewModelScope.launch {
            userPreferencesManager.updateQuoteListFilter(filter.persistedValue)
        }
    }

    /** Updates search query and persists it. */
    fun updateSearchQuery(query: String) {
        searchQuery.value = query
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
                _events.send(QuoteListEvent.ShowMessage("Failed to update favorite: ${e.message}"))
            } catch (e: IllegalStateException) {
                Log.e(TAG, "Invalid state toggling favorite", e)
                _events.send(QuoteListEvent.ShowMessage("Invalid state: ${e.message}"))
            }
        }
    }

    /** Moves a custom quote to retained trash and provides an exact undo receipt. */
    fun deleteQuote(quote: Quote) = moveQuote(quote, quoteRepository::deleteUserQuote)

    /** Archives an active quote until it is restored. */
    fun archiveQuote(quote: Quote) = moveQuote(quote, quoteRepository::archiveQuote)

    /** Hides an active quote until it is restored from the Hidden tab. */
    fun hideQuote(quote: Quote) = moveQuote(quote, quoteRepository::hideQuote)

    private fun moveQuote(quote: Quote, command: suspend (Long) -> QuoteLifecycleChange) {
        viewModelScope.launch {
            try {
                val change = command(quote.id)
                _events.send(QuoteListEvent.QuoteMoved(change))
            } catch (exception: IOException) {
                showLifecycleFailure(exception)
            } catch (exception: IllegalStateException) {
                showLifecycleFailure(exception)
            }
        }
    }

    /** Undoes a state mutation without reinserting a stale quote snapshot. */
    fun undoQuoteMove(change: QuoteLifecycleChange) {
        viewModelScope.launch {
            try {
                quoteRepository.undoLifecycleChange(change)
            } catch (exception: IOException) {
                showLifecycleFailure(exception)
            } catch (exception: IllegalStateException) {
                showLifecycleFailure(exception)
            }
        }
    }

    private suspend fun showLifecycleFailure(exception: Exception) {
        Log.e(TAG, "Quote lifecycle action failed", exception)
        _events.send(QuoteListEvent.ShowMessage("Failed to update quote: ${exception.message}"))
    }

    private suspend fun restorePersistedFilters() {
        val prefs = userPreferencesManager.userPreferencesFlow.first()
        currentFilter.value = QuoteFilter.fromPersistedValue(prefs.quoteListFilter)
        searchQuery.value = prefs.quoteSearchQuery
    }
}

private data class QuoteListSourceState(
    val allQuotes: List<Quote>,
    val preferences: UserPreferences
)

private data class QuoteListFilterState(
    val currentFilter: QuoteFilter,
    val searchQuery: String
)

/** UI state for the Library screen. */
data class QuoteListUiState(
    val quotes: List<Quote> = emptyList(),
    val quoteItems: List<QuoteListItemUiModel> = emptyList(),
    val currentFilter: QuoteFilter = QuoteFilter.ALL,
    val searchQuery: String = "",
    val selectedScript: RunicScript = RunicScript.ELDER_FUTHARK,
    val selectedFont: String = "noto",
    val isLoading: Boolean = false,
    val filterCounts: Map<QuoteFilter, Int> = emptyMap()
)

/** Presentation model for quotes rendered in the library list and actions sheet. */
data class QuoteListItemUiModel(
    val quote: Quote,
    val runicPreviewText: String,
    val rendering: ResolvedQuoteRendering
)

/** One-off UI events emitted by the library screen. */
sealed interface QuoteListEvent {
    /** Shows transient feedback to the user. */
    data class ShowMessage(val message: String) : QuoteListEvent

    /** Provides the exact receipt for a successful, reversible lifecycle action. */
    data class QuoteMoved(val change: QuoteLifecycleChange) : QuoteListEvent
}

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
