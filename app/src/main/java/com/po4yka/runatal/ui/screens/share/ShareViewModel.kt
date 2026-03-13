package com.po4yka.runatal.ui.screens.share

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.po4yka.runatal.domain.repository.NoOpTranslationRepository
import com.po4yka.runatal.domain.model.Quote
import com.po4yka.runatal.domain.model.RunicScript
import com.po4yka.runatal.domain.repository.QuoteRepository
import com.po4yka.runatal.domain.repository.TranslationRepository
import com.po4yka.runatal.util.ShareAppearance
import com.po4yka.runatal.util.ShareTemplate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

/**
 * ViewModel for the share quote screen.
 */
@HiltViewModel
internal class ShareViewModel @Inject constructor(
    private val quoteRepository: QuoteRepository,
    savedStateHandle: SavedStateHandle,
    private val translationRepository: TranslationRepository = NoOpTranslationRepository
) : ViewModel() {

    private val quoteId: Long = savedStateHandle.get<Long>(QUOTE_ID_KEY) ?: 0L

    private val _uiState = MutableStateFlow<ShareUiState>(ShareUiState.Loading)
    val uiState: StateFlow<ShareUiState> = _uiState.asStateFlow()

    private val _selectedTemplate = MutableStateFlow(ShareTemplate.CARD)
    val selectedTemplate: StateFlow<ShareTemplate> = _selectedTemplate.asStateFlow()

    private val _selectedAppearance = MutableStateFlow(ShareAppearance.DARK)
    val selectedAppearance: StateFlow<ShareAppearance> = _selectedAppearance.asStateFlow()

    /** @suppress */
    companion object {
        private const val TAG = "ShareViewModel"
        private const val QUOTE_ID_KEY = "quoteId"
    }

    init {
        if (quoteId != 0L) {
            loadQuote()
        }
    }

    private fun loadQuote() {
        viewModelScope.launch {
            _uiState.value = ShareUiState.Loading
            try {
                val quote = quoteRepository.getQuoteById(quoteId)
                if (quote != null) {
                    _uiState.value = ShareUiState.Success(resolveShareQuote(quote))
                } else {
                    _uiState.value = ShareUiState.Error("Quote not found")
                }
            } catch (e: IOException) {
                Log.e(TAG, "IO error loading quote", e)
                _uiState.value = ShareUiState.Error("Failed to load quote: ${e.message}")
            } catch (e: IllegalStateException) {
                Log.e(TAG, "Invalid state loading quote", e)
                _uiState.value = ShareUiState.Error("Invalid state: ${e.message}")
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

    private suspend fun resolveShareQuote(quote: Quote): Quote {
        val elderTranslation = translationRepository.getLatestAvailableTranslation(
            quoteId = quote.id,
            script = RunicScript.ELDER_FUTHARK
        )
        val youngerTranslation = translationRepository.getLatestAvailableTranslation(
            quoteId = quote.id,
            script = RunicScript.YOUNGER_FUTHARK
        )
        val cirthTranslation = translationRepository.getLatestAvailableTranslation(
            quoteId = quote.id,
            script = RunicScript.CIRTH
        )

        if (elderTranslation == null && youngerTranslation == null && cirthTranslation == null) {
            return quote
        }

        return quote.copy(
            runicElder = elderTranslation?.glyphOutput ?: quote.runicElder,
            runicYounger = youngerTranslation?.glyphOutput ?: quote.runicYounger,
            runicCirth = cirthTranslation?.glyphOutput ?: quote.runicCirth
        )
    }
}

/**
 * UI state for the share screen.
 */
sealed interface ShareUiState {
    /** Quote data is being loaded. */
    data object Loading : ShareUiState

    /** Quote loaded successfully. */
    data class Success(val quote: Quote) : ShareUiState

    /** An error occurred while loading the quote. */
    data class Error(val message: String) : ShareUiState
}
