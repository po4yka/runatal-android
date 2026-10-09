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
import com.po4yka.runatal.di.DefaultDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** ViewModel for the Library screen with tab filtering. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
internal class QuoteListViewModel @Inject constructor(
    private val quoteRepository: QuoteRepository,
    private val userPreferencesManager: UserPreferencesManager,
    resolveQuoteRenderingUseCase: ResolveQuoteRenderingUseCase,
    @param:DefaultDispatcher private val preparationDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val preparation = QuoteListPreparation(resolveQuoteRenderingUseCase)

    private val _uiState = MutableStateFlow(QuoteListUiState(isLoading = true))
    val uiState: StateFlow<QuoteListUiState> = _uiState.asStateFlow()
    private val currentFilter = MutableStateFlow(QuoteFilter.ALL)
    private val searchQuery = MutableStateFlow("")
    private val pendingSearchWrite = MutableStateFlow<SearchWrite?>(null)
    private var lastPersistedSearch: SearchWrite? = null
    private var filterWasChanged = false
    private val _events = Channel<QuoteListEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    /** @suppress */
    companion object {
        private const val TAG = "QuoteListViewModel"
    }

    init {
        viewModelScope.launch { persistSearchUpdates() }
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
                    sourceState to filterState
                }.mapLatest { (source, filter) ->
                    withContext(preparationDispatcher) {
                        preparation.prepare(source.allQuotes, source.preferences.selectedScript,
                            source.preferences.selectedFont, filter.currentFilter, filter.searchQuery)
                    }
                }.collect { newState ->
                    if (newState.currentFilter == currentFilter.value && newState.searchQuery == searchQuery.value) {
                        _uiState.value = newState
                    }
                }
            } catch (e: CancellationException) {
                throw e
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
        filterWasChanged = true
        currentFilter.value = filter
        _uiState.update { it.copy(currentFilter = filter) }
        viewModelScope.launch {
            userPreferencesManager.updateQuoteListFilter(filter.persistedValue)
        }
    }

    /** Applies search immediately; one writer coalesces pending edits and flushes on navigation. */
    fun updateSearchQuery(query: String) {
        searchQuery.value = query
        _uiState.update { it.copy(searchQuery = query) }
        pendingSearchWrite.value = SearchWrite(query, userPreferencesManager.reserveQuoteSearchWrite())
    }

    private suspend fun persistSearchUpdates() {
        try {
            pendingSearchWrite.filterNotNull().collect { persistSearch(it) }
        } finally {
            withContext(NonCancellable) {
                pendingSearchWrite.value?.let { if (it != lastPersistedSearch) persistSearch(it) }
            }
        }
    }

    private suspend fun persistSearch(write: SearchWrite) {
        try {
            // Finish the in-flight durable edit; cancellation then flushes only the newest pending value.
            withContext(NonCancellable) {
                userPreferencesManager.updateQuoteSearchQuery(write.query, write.generation)
                lastPersistedSearch = write
            }
        } catch (exception: IOException) {
            Log.e(TAG, "Failed to persist Library search", exception)
            val message = "Couldn't save the search. Your current results remain available."
            _events.trySend(QuoteListEvent.ShowMessage(message))
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

    private data class SearchWrite(val query: String, val generation: Long)

    private suspend fun restorePersistedFilters() {
        val prefs = userPreferencesManager.userPreferencesFlow.first()
        if (!filterWasChanged) currentFilter.value = QuoteFilter.fromPersistedValue(prefs.quoteListFilter)
        if (pendingSearchWrite.value == null) searchQuery.value = prefs.quoteSearchQuery
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
