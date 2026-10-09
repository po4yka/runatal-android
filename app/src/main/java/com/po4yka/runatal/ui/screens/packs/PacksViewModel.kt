package com.po4yka.runatal.ui.screens.packs

import android.util.Log
import android.database.SQLException
import java.io.IOException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.po4yka.runatal.data.repository.QuotePackRepository
import com.po4yka.runatal.domain.model.QuotePackContentCatalog
import com.po4yka.runatal.domain.model.QuotePack
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.Normalizer
import javax.inject.Inject

/**
 * ViewModel for browsing curated quote packs with search and library management.
 */
@HiltViewModel
class PacksViewModel @Inject constructor(
    private val quotePackRepository: QuotePackRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PacksUiState())
    val uiState: StateFlow<PacksUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    private val _events = Channel<PacksEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    /** @suppress */
    companion object {
        private const val TAG = "PacksViewModel"
    }

    private var loadJob: Job? = null

    init {
        loadPacks()
    }

    private fun loadPacks() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                quotePackRepository.seedIfNeeded()
                combine(
                    quotePackRepository.getAllPacksFlow(),
                    _searchQuery
                ) { allPacks, query ->
                    val filtered = if (query.isBlank()) {
                        allPacks
                    } else {
                        val q = normalize(query)
                        allPacks.filter { pack ->
                            val source = QuotePackContentCatalog.sourceLabel(pack)
                            normalize(pack.name).contains(q) ||
                                normalize(pack.description).contains(q) ||
                                normalize(source).contains(q)
                        }
                    }

                    PacksUiState(
                        packs = filtered,
                        totalCount = allPacks.size,
                        libraryCount = allPacks.count { it.isInLibrary },
                        searchQuery = query,
                        isLoading = false
                    )
                }.collect { newState ->
                    _uiState.value = newState
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                showLoadError(e)
            } catch (e: IllegalStateException) {
                showLoadError(e)
            } catch (e: SQLException) {
                showLoadError(e)
            }
        }
    }

    /**
     * Updates the search query for filtering packs.
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Toggles library membership for a pack.
     */
    fun toggleLibrary(pack: QuotePack) {
        viewModelScope.launch {
            try {
                quotePackRepository.toggleLibrary(pack.id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                showUpdateError(e)
            } catch (e: IllegalStateException) {
                showUpdateError(e)
            } catch (e: SQLException) {
                showUpdateError(e)
            }
        }
    }

    /**
     * Retries loading packs after an error.
     */
    fun retry() {
        _uiState.update { it.copy(errorMessage = null) }
        loadPacks()
    }

    private fun showLoadError(error: Exception) {
        Log.e(TAG, "Error loading packs", error)
        _uiState.update {
            it.copy(isLoading = false, errorMessage = "Failed to load packs: ${error.message}")
        }
    }

    private suspend fun showUpdateError(error: Exception) {
        Log.e(TAG, "Error toggling library status", error)
        _events.send(PacksEvent.ShowMessage("Failed to update pack: ${error.message}"))
    }

    private fun normalize(value: String): String {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
            .lowercase()
    }
}

/**
 * UI state for the packs browsing screen.
 */
data class PacksUiState(
    val packs: List<QuotePack> = emptyList(),
    val totalCount: Int = 0,
    val libraryCount: Int = 0,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

/** One-off UI events emitted by the packs screen. */
sealed interface PacksEvent {
    /** Shows transient feedback to the user. */
    data class ShowMessage(val message: String) : PacksEvent
}
