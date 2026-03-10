package com.po4yka.runicquotes.ui.screens.packs

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.po4yka.runicquotes.data.repository.QuotePackRepository
import com.po4yka.runicquotes.domain.model.QuotePack
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.Locale
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

    /** @suppress */
    companion object {
        private const val TAG = "PacksViewModel"
    }

    init {
        viewModelScope.launch {
            quotePackRepository.seedIfNeeded()
            loadPacks()
        }
    }

    private fun loadPacks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            combine(
                quotePackRepository.getAllPacksFlow(),
                quotePackRepository.getLibraryPacksFlow(),
                _searchQuery
            ) { allPacks, libraryPacks, query ->
                val filtered = if (query.isBlank()) {
                    allPacks
                } else {
                    val q = query.trim().lowercase(Locale.getDefault())
                    allPacks.filter { pack ->
                        pack.name.lowercase(Locale.getDefault()).contains(q) ||
                            pack.description.lowercase(Locale.getDefault()).contains(q)
                    }
                }

                PacksUiState(
                    packs = filtered,
                    totalCount = allPacks.size,
                    libraryCount = libraryPacks.size,
                    searchQuery = query,
                    isLoading = false
                )
            }.catch { e ->
                Log.e(TAG, "Error loading packs", e)
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Failed to load packs: ${e.message}")
                }
            }.collect { newState ->
                _uiState.value = newState
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
                quotePackRepository.updatePack(pack.copy(isInLibrary = !pack.isInLibrary))
            } catch (e: IOException) {
                Log.e(TAG, "IO error toggling library status", e)
                _uiState.update {
                    it.copy(errorMessage = "Failed to update pack: ${e.message}")
                }
            } catch (e: IllegalStateException) {
                Log.e(TAG, "Invalid state toggling library status", e)
                _uiState.update {
                    it.copy(errorMessage = "Invalid state: ${e.message}")
                }
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

    /**
     * Clears any displayed error message.
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
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
