package com.po4yka.runatal.ui.screens.references

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.po4yka.runatal.data.repository.RuneReferenceRepository
import com.po4yka.runatal.domain.model.RuneReference
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for browsing rune references grouped by script type.
 */
@HiltViewModel
class ReferencesViewModel @Inject constructor(
    private val runeReferenceRepository: RuneReferenceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReferencesUiState())
    val uiState: StateFlow<ReferencesUiState> = _uiState.asStateFlow()
    private var currentRunes: List<RuneReference> = emptyList()
    private var loadJob: Job? = null

    /** @suppress */
    companion object {
        private const val TAG = "ReferencesViewModel"
    }

    init {
        viewModelScope.launch {
            runeReferenceRepository.seedIfNeeded()
            loadRunes()
        }
    }

    private fun loadRunes() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val script = _uiState.value.selectedTab.scriptKey
            runeReferenceRepository.getRunesByScriptFlow(script)
                .catch { e ->
                    Log.e(TAG, "Error loading runes", e)
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = "Failed to load runes: ${e.message}")
                    }
                }
                .collect { runes ->
                    currentRunes = runes
                    _uiState.update {
                        it.copy(
                            runes = filterRunes(runes, it.searchQuery),
                            totalRuneCount = runes.size,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
        }
    }

    /**
     * Switches between Elder Futhark, Younger Futhark, and Cirth tabs.
     */
    fun selectTab(tab: ScriptTab) {
        _uiState.update { it.copy(selectedTab = tab) }
        loadRunes()
    }

    /**
     * Expands or collapses the inline search field.
     * Closing search also clears the active query.
     */
    fun toggleSearch() {
        val searchVisible = !_uiState.value.isSearchVisible
        _uiState.update {
            it.copy(
                isSearchVisible = searchVisible,
                searchQuery = if (searchVisible) it.searchQuery else ""
            )
        }
        if (!searchVisible) {
            applySearch()
        }
    }

    /**
     * Filters the current script set by rune name, sound, meaning, or glyph.
     */
    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applySearch()
    }

    /**
     * Retries loading runes after an error.
     */
    fun retry() {
        _uiState.update { it.copy(errorMessage = null) }
        loadRunes()
    }

    private fun applySearch() {
        _uiState.update {
            it.copy(runes = filterRunes(currentRunes, it.searchQuery))
        }
    }

    private fun filterRunes(runes: List<RuneReference>, query: String): List<RuneReference> {
        val normalizedQuery = query.trim().lowercase()
        if (normalizedQuery.isBlank()) {
            return runes
        }

        return runes.filter { rune ->
            rune.name.lowercase().contains(normalizedQuery) ||
                rune.pronunciation.lowercase().contains(normalizedQuery) ||
                rune.meaning.lowercase().contains(normalizedQuery) ||
                rune.character.contains(normalizedQuery)
        }
    }
}

/**
 * Script tab options for the references screen.
 */
enum class ScriptTab(val displayName: String, val scriptKey: String) {
    ELDER("Elder", "elder_futhark"),
    YOUNGER("Younger", "younger_futhark"),
    CIRTH("Cirth", "cirth")
}

/**
 * UI state for the references screen.
 */
data class ReferencesUiState(
    val runes: List<RuneReference> = emptyList(),
    val totalRuneCount: Int = 0,
    val selectedTab: ScriptTab = ScriptTab.ELDER,
    val isLoading: Boolean = false,
    val isSearchVisible: Boolean = false,
    val searchQuery: String = "",
    val errorMessage: String? = null
)
