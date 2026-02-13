package com.po4yka.runicquotes.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.po4yka.runicquotes.data.preferences.UserPreferences
import com.po4yka.runicquotes.data.preferences.UserPreferencesManager
import com.po4yka.runicquotes.domain.model.RunicScript
import com.po4yka.runicquotes.ui.widget.WidgetDisplayMode
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
     * Updates whether dynamic color is enabled.
     */
    fun updateDynamicColorEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesManager.updateDynamicColorEnabled(enabled)
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

    /**
     * Updates widget content mode.
     */
    fun updateWidgetDisplayMode(mode: WidgetDisplayMode) {
        viewModelScope.launch {
            userPreferencesManager.updateWidgetDisplayMode(mode.persistedValue)
        }
    }

    /**
     * Toggles large runes accessibility preset.
     */
    fun updateLargeRunesEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesManager.updateLargeRunesEnabled(enabled)
        }
    }

    /**
     * Toggles high contrast accessibility preset.
     */
    fun updateHighContrastEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesManager.updateHighContrastEnabled(enabled)
        }
    }

    /**
     * Toggles reduced motion accessibility preset.
     */
    fun updateReducedMotionEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesManager.updateReducedMotionEnabled(enabled)
        }
    }

    /**
     * Marks onboarding as completed.
     */
    fun completeOnboarding() {
        viewModelScope.launch {
            userPreferencesManager.updateHasCompletedOnboarding(true)
        }
    }
}
