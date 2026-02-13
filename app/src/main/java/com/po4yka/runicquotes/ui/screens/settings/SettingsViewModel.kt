package com.po4yka.runicquotes.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.po4yka.runicquotes.data.preferences.UserPreferences
import com.po4yka.runicquotes.data.preferences.UserPreferencesManager
import com.po4yka.runicquotes.domain.model.RunicScript
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Settings screen.
 * Manages user preferences.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesManager: UserPreferencesManager
) : ViewModel() {

    /**
     * Current user preferences as StateFlow.
     */
    val userPreferences: StateFlow<UserPreferences> =
        userPreferencesManager.userPreferencesFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences()
        )

    /**
     * Updates the selected runic script.
     */
    fun updateSelectedScript(script: RunicScript) {
        viewModelScope.launch {
            userPreferencesManager.updateSelectedScript(script)
        }
    }

    /**
     * Updates the selected font.
     */
    fun updateSelectedFont(font: String) {
        viewModelScope.launch {
            userPreferencesManager.updateSelectedFont(font)
        }
    }

    /**
     * Updates the theme mode.
     */
    fun updateThemeMode(mode: String) {
        viewModelScope.launch {
            userPreferencesManager.updateThemeMode(mode)
        }
    }

    /**
     * Updates the visual theme pack.
     */
    fun updateThemePack(themePack: String) {
        viewModelScope.launch {
            userPreferencesManager.updateThemePack(themePack)
        }
    }

    /**
     * Updates whether to show transliteration.
     */
    fun updateShowTransliteration(show: Boolean) {
        viewModelScope.launch {
            userPreferencesManager.updateShowTransliteration(show)
        }
    }

    /**
     * Updates the font size multiplier.
     */
    fun updateFontSize(size: Float) {
        viewModelScope.launch {
            userPreferencesManager.updateFontSize(size)
        }
    }
}
