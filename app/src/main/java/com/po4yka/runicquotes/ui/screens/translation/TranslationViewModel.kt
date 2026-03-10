package com.po4yka.runicquotes.ui.screens.translation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.po4yka.runicquotes.data.repository.RuneReferenceRepository
import com.po4yka.runicquotes.domain.model.RuneReference
import com.po4yka.runicquotes.domain.model.RunicScript
import com.po4yka.runicquotes.domain.model.displayName
import com.po4yka.runicquotes.domain.transliteration.TransliterationFactory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the translation screen providing real-time transliteration.
 */
@HiltViewModel
class TranslationViewModel @Inject constructor(
    private val transliterationFactory: TransliterationFactory,
    private val runeReferenceRepository: RuneReferenceRepository
) : ViewModel() {

    private val _inputText = MutableStateFlow("")
    private val _selectedScript = MutableStateFlow(RunicScript.DEFAULT)

    init {
        viewModelScope.launch { runeReferenceRepository.seedIfNeeded() }
    }

    val uiState: StateFlow<TranslationUiState> = combine(
        _inputText,
        _selectedScript,
        runeReferenceRepository.getRunesByScriptFlow(RunicScript.ELDER_FUTHARK.toDbKey()),
        runeReferenceRepository.getRunesByScriptFlow(RunicScript.YOUNGER_FUTHARK.toDbKey()),
        runeReferenceRepository.getRunesByScriptFlow(RunicScript.CIRTH.toDbKey())
    ) { input, script, elder, younger, cirth ->
        val transliterated = if (input.isBlank()) "" else {
            transliterationFactory.transliterate(input, script)
        }
        val runes = when (script) {
            RunicScript.ELDER_FUTHARK -> elder
            RunicScript.YOUNGER_FUTHARK -> younger
            RunicScript.CIRTH -> cirth
        }
        TranslationUiState(
            inputText = input,
            transliteratedText = transliterated,
            selectedScript = script,
            runeCharacters = runes
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), TranslationUiState())

    /**
     * Updates the input text and triggers transliteration.
     */
    fun updateInputText(text: String) {
        _inputText.value = text
    }

    /**
     * Selects a runic script for transliteration.
     */
    fun selectScript(script: RunicScript) {
        _selectedScript.value = script
    }

    /**
     * Clears the input text field.
     */
    fun clearInput() {
        _inputText.value = ""
    }
}

private fun RunicScript.toDbKey(): String = when (this) {
    RunicScript.ELDER_FUTHARK -> "elder_futhark"
    RunicScript.YOUNGER_FUTHARK -> "younger_futhark"
    RunicScript.CIRTH -> "cirth"
}

/**
 * UI state for the translation screen.
 */
data class TranslationUiState(
    val inputText: String = "",
    val transliteratedText: String = "",
    val selectedScript: RunicScript = RunicScript.DEFAULT,
    val runeCharacters: List<RuneReference> = emptyList()
) {
    /** Display name for the currently selected script. */
    val scriptDisplayName: String get() = selectedScript.displayName
}
