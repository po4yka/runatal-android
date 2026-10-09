package com.po4yka.runatal.ui.screens.share

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.po4yka.runatal.domain.model.Quote
import com.po4yka.runatal.domain.model.QuoteShareContent
import com.po4yka.runatal.data.preferences.UserPreferencesManager
import com.po4yka.runatal.domain.usecase.quote.BuildQuotePresentationUseCase
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.Job
import com.po4yka.runatal.data.preferences.UserPreferences
import com.po4yka.runatal.domain.repository.QuoteRepository
import com.po4yka.runatal.util.ShareAppearance
import com.po4yka.runatal.util.ShareTemplate
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * ViewModel for the share quote screen.
 */
@HiltViewModel(assistedFactory = ShareViewModel.Factory::class)
internal class ShareViewModel @AssistedInject constructor(
    private val quoteRepository: QuoteRepository,
    @Assisted private val quoteId: Long,
    private val buildQuotePresentationUseCase: BuildQuotePresentationUseCase,
    private val userPreferencesManager: UserPreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<ShareUiState>(ShareUiState.Loading)
    val uiState: StateFlow<ShareUiState> = _uiState.asStateFlow()

    private val _selectedTemplate = MutableStateFlow(ShareTemplate.CARD)
    val selectedTemplate: StateFlow<ShareTemplate> = _selectedTemplate.asStateFlow()

    private val _selectedAppearance = MutableStateFlow(ShareAppearance.DARK)
    val selectedAppearance: StateFlow<ShareAppearance> = _selectedAppearance.asStateFlow()

    /** Creates an entry-scoped ViewModel with the typed navigation argument. */
    @AssistedFactory
    interface Factory {
        /** Creates the ViewModel for the selected navigation entry. */
        fun create(quoteId: Long): ShareViewModel
    }

    /** @suppress */
    companion object {
        private const val TAG = "ShareViewModel"
    }

    init {
        if (quoteId > 0L) {
            loadQuote()
        } else {
            _uiState.value = ShareUiState.Error("Quote not found")
        }
    }

    private var loadJob: Job? = null

    private fun loadQuote() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = ShareUiState.Loading
            combine(
                userPreferencesManager.userPreferencesFlow,
                quoteRepository.observeQuoteChanges().onStart { emit(Unit) }
            ) { preferences, _ -> preferences }.collectLatest { preferences ->
                try {
                    val quote = quoteRepository.getQuoteById(quoteId)
                    _uiState.value = if (quote == null) ShareUiState.Error("Quote not found")
                        else ShareUiState.Success(resolveShareQuote(quote, preferences))
                } catch (e: IOException) {
                    Log.e(TAG, "IO error loading quote", e)
                    _uiState.value = ShareUiState.Error("Failed to load quote: ${e.message}")
                } catch (e: IllegalStateException) {
                    Log.e(TAG, "Invalid state loading quote", e)
                    _uiState.value = ShareUiState.Error("Invalid state: ${e.message}")
                }
            }
        }
    }

    /** Selects a share template style. */
    fun selectTemplate(template: ShareTemplate) {
        _selectedTemplate.update { template }
    }

    /** Selects the preview appearance and export theme. */
    fun selectAppearance(appearance: ShareAppearance) {
        _selectedAppearance.update { appearance }
    }

    /** Retries loading the quote after an error. */
    fun retry() {
        if (quoteId != 0L) {
            loadQuote()
        }
    }

    private suspend fun resolveShareQuote(quote: Quote, preferences: UserPreferences): QuoteShareContent {
        val presentation = buildQuotePresentationUseCase(quote, preferences.selectedScript, emptyList())
        return QuoteShareContent(quote, preferences.selectedScript, preferences.selectedFont, presentation.rendering)
    }

}

/**
 * UI state for the share screen.
 */
sealed interface ShareUiState {
    /** Quote data is being loaded. */
    data object Loading : ShareUiState

    /** Quote loaded successfully. */
    data class Success(val content: QuoteShareContent) : ShareUiState

    /** An error occurred while loading the quote. */
    data class Error(val message: String) : ShareUiState
}
