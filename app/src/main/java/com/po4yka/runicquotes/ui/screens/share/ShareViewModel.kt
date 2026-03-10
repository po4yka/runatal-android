package com.po4yka.runicquotes.ui.screens.share

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.po4yka.runicquotes.data.repository.QuoteRepository
import com.po4yka.runicquotes.domain.model.Quote
import com.po4yka.runicquotes.util.QuoteShareManager
import com.po4yka.runicquotes.util.ShareAppearance
import com.po4yka.runicquotes.util.ShareTemplate
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
class ShareViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val quoteRepository: QuoteRepository,
    private val quoteShareManager: QuoteShareManager
) : ViewModel() {

    private var quoteId: Long = savedStateHandle.get<Long>("quoteId") ?: 0L

    private val _uiState = MutableStateFlow<ShareUiState>(ShareUiState.Loading)
    val uiState: StateFlow<ShareUiState> = _uiState.asStateFlow()

    private val _selectedTemplate = MutableStateFlow(ShareTemplate.CARD)
    val selectedTemplate: StateFlow<ShareTemplate> = _selectedTemplate.asStateFlow()

    private val _selectedAppearance = MutableStateFlow(ShareAppearance.DARK)
    val selectedAppearance: StateFlow<ShareAppearance> = _selectedAppearance.asStateFlow()

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    /** @suppress */
    companion object {
        private const val TAG = "ShareViewModel"
    }

    init {
        if (quoteId != 0L) {
            loadQuote()
        }
    }

    /** Initializes the ViewModel with a quote ID if not already loaded. */
    fun initializeQuoteIfNeeded(id: Long) {
        if (id != 0L && id != quoteId) {
            quoteId = id
            loadQuote()
        } else if (id != 0L && _uiState.value is ShareUiState.Loading) {
            loadQuote()
        }
    }

    private fun loadQuote() {
        viewModelScope.launch {
            _uiState.value = ShareUiState.Loading
            try {
                val quote = quoteRepository.getQuoteById(quoteId)
                if (quote != null) {
                    _uiState.value = ShareUiState.Success(quote)
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

    /** Shares the quote as plain text. */
    fun shareAsText() {
        val quote = (uiState.value as? ShareUiState.Success)?.quote ?: return
        quoteShareManager.shareQuoteText(quote.textLatin, quote.author)
    }

    /** Shares the quote as a styled image. */
    fun shareAsImage() {
        val quote = (uiState.value as? ShareUiState.Success)?.quote ?: return
        val runicText = quote.runicElder ?: quote.textLatin
        viewModelScope.launch {
            quoteShareManager.shareQuoteAsImage(
                runicText = runicText,
                latinText = quote.textLatin,
                author = quote.author,
                template = _selectedTemplate.value,
                appearance = _selectedAppearance.value
            )
        }
    }

    /** Copies quote text and author to the clipboard. */
    fun copyQuote() {
        val quote = (uiState.value as? ShareUiState.Success)?.quote ?: return
        quoteShareManager.copyQuoteToClipboard(quote.textLatin, quote.author)
        _feedbackMessage.value = "Quote copied"
    }

    /** Clears transient feedback after it has been shown. */
    fun clearFeedback() {
        _feedbackMessage.value = null
    }

    /** Retries loading the quote after an error. */
    fun retry() {
        loadQuote()
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
