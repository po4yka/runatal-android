package com.po4yka.runicquotes.ui.screens.references

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.po4yka.runicquotes.data.repository.RuneReferenceRepository
import com.po4yka.runicquotes.domain.model.RuneReference
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
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
        viewModelScope.launch {
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
                    _uiState.update {
                        it.copy(runes = runes, isLoading = false, errorMessage = null)
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
     * Retries loading runes after an error.
     */
    fun retry() {
        _uiState.update { it.copy(errorMessage = null) }
        loadRunes()
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
    val selectedTab: ScriptTab = ScriptTab.ELDER,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
