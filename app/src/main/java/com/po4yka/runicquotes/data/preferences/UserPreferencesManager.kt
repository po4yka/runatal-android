package com.po4yka.runicquotes.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.po4yka.runicquotes.domain.model.RunicScript
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for user preferences using Jetpack DataStore.
 */
@Singleton
class UserPreferencesManager @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    /**
     * Flow of user preferences that emits whenever preferences change.
     */
    val userPreferencesFlow: Flow<UserPreferences> = dataStore.data.map { preferences ->
        UserPreferences(
            selectedScript = preferences[SELECTED_SCRIPT]
                ?.let { scriptName -> RunicScript.entries.firstOrNull { it.name == scriptName } }
                ?: RunicScript.DEFAULT,
            selectedFont = preferences[SELECTED_FONT] ?: "noto",
            widgetUpdateMode = preferences[WIDGET_UPDATE_MODE] ?: "daily",
            lastQuoteDate = preferences[LAST_QUOTE_DATE] ?: 0L,
            lastDailyQuoteId = preferences[LAST_DAILY_QUOTE_ID] ?: 0L,
            themeMode = preferences[THEME_MODE] ?: "system",
            showTransliteration = preferences[SHOW_TRANSLITERATION] ?: true,
            fontSize = preferences[FONT_SIZE] ?: 1.0f
        )
    }

    /**
     * Updates the selected runic script.
     */
    suspend fun updateSelectedScript(script: RunicScript) {
        dataStore.edit { preferences ->
            preferences[SELECTED_SCRIPT] = script.name
        }
    }

    /**
     * Updates the selected font.
     */
    suspend fun updateSelectedFont(font: String) {
        dataStore.edit { preferences ->
            preferences[SELECTED_FONT] = font
        }
    }

    /**
     * Updates the widget update mode.
     */
    suspend fun updateWidgetUpdateMode(mode: String) {
        dataStore.edit { preferences ->
            preferences[WIDGET_UPDATE_MODE] = mode
        }
    }

    /**
     * Updates the last quote date.
     */
    suspend fun updateLastQuoteDate(date: Long) {
        dataStore.edit { preferences ->
            preferences[LAST_QUOTE_DATE] = date
        }
    }

    /**
     * Updates the last daily quote ID.
     */
    suspend fun updateLastDailyQuoteId(id: Long) {
        dataStore.edit { preferences ->
            preferences[LAST_DAILY_QUOTE_ID] = id
        }
    }

    /**
     * Updates the theme mode.
     */
    suspend fun updateThemeMode(mode: String) {
        dataStore.edit { preferences ->
            preferences[THEME_MODE] = mode
        }
    }

    /**
     * Updates whether to show transliteration.
     */
    suspend fun updateShowTransliteration(show: Boolean) {
        dataStore.edit { preferences ->
            preferences[SHOW_TRANSLITERATION] = show
        }
    }

    /**
     * Updates the font size multiplier.
     */
    suspend fun updateFontSize(size: Float) {
        dataStore.edit { preferences ->
            preferences[FONT_SIZE] = size
        }
    }

    /**
     * Clears all preferences.
     */
    suspend fun clearPreferences() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    companion object {
        private val SELECTED_SCRIPT = stringPreferencesKey("selected_script")
        private val SELECTED_FONT = stringPreferencesKey("selected_font")
        private val WIDGET_UPDATE_MODE = stringPreferencesKey("widget_update_mode")
        private val LAST_QUOTE_DATE = longPreferencesKey("last_quote_date")
        private val LAST_DAILY_QUOTE_ID = longPreferencesKey("last_daily_quote_id")
        private val THEME_MODE = stringPreferencesKey("theme_mode")
        private val SHOW_TRANSLITERATION = booleanPreferencesKey("show_transliteration")
        private val FONT_SIZE = floatPreferencesKey("font_size")
    }
}
