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
@Suppress("TooManyFunctions")
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
            widgetDisplayMode = preferences[WIDGET_DISPLAY_MODE] ?: "rune_latin",
            quoteListFilter = preferences[QUOTE_LIST_FILTER] ?: "all",
            quoteSearchQuery = preferences[QUOTE_SEARCH_QUERY] ?: "",
            quoteAuthorFilter = preferences[QUOTE_AUTHOR_FILTER] ?: "",
            quoteLengthFilter = preferences[QUOTE_LENGTH_FILTER] ?: "any",
            quoteCollectionFilter = preferences[QUOTE_COLLECTION_FILTER] ?: "all",
            lastQuoteDate = preferences[LAST_QUOTE_DATE] ?: 0L,
            lastDailyQuoteId = preferences[LAST_DAILY_QUOTE_ID] ?: 0L,
            themeMode = preferences[THEME_MODE] ?: "system",
            themePack = preferences[THEME_PACK] ?: "stone",
            showTransliteration = preferences[SHOW_TRANSLITERATION] ?: true,
            fontSize = preferences[FONT_SIZE] ?: 1.0f,
            largeRunesEnabled = preferences[LARGE_RUNES_ENABLED] ?: false,
            highContrastEnabled = preferences[HIGH_CONTRAST_ENABLED] ?: false,
            reducedMotionEnabled = preferences[REDUCED_MOTION_ENABLED] ?: false,
            hasCompletedOnboarding = preferences[HAS_COMPLETED_ONBOARDING] ?: false
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
     * Updates the widget display mode.
     */
    suspend fun updateWidgetDisplayMode(mode: String) {
        dataStore.edit { preferences ->
            preferences[WIDGET_DISPLAY_MODE] = mode
        }
    }

    /**
     * Updates quote list base filter.
     */
    suspend fun updateQuoteListFilter(filter: String) {
        dataStore.edit { preferences ->
            preferences[QUOTE_LIST_FILTER] = filter
        }
    }

    /**
     * Updates quote search query.
     */
    suspend fun updateQuoteSearchQuery(query: String) {
        dataStore.edit { preferences ->
            preferences[QUOTE_SEARCH_QUERY] = query
        }
    }

    /**
     * Updates quote author filter.
     */
    suspend fun updateQuoteAuthorFilter(author: String) {
        dataStore.edit { preferences ->
            preferences[QUOTE_AUTHOR_FILTER] = author
        }
    }

    /**
     * Updates quote length filter.
     */
    suspend fun updateQuoteLengthFilter(lengthFilter: String) {
        dataStore.edit { preferences ->
            preferences[QUOTE_LENGTH_FILTER] = lengthFilter
        }
    }

    /**
     * Updates quote collection filter.
     */
    suspend fun updateQuoteCollectionFilter(collectionFilter: String) {
        dataStore.edit { preferences ->
            preferences[QUOTE_COLLECTION_FILTER] = collectionFilter
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
     * Updates the visual theme pack.
     */
    suspend fun updateThemePack(themePack: String) {
        dataStore.edit { preferences ->
            preferences[THEME_PACK] = themePack
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
     * Updates large runes accessibility preset.
     */
    suspend fun updateLargeRunesEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[LARGE_RUNES_ENABLED] = enabled
        }
    }

    /**
     * Updates high contrast accessibility preset.
     */
    suspend fun updateHighContrastEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[HIGH_CONTRAST_ENABLED] = enabled
        }
    }

    /**
     * Updates reduced motion accessibility preset.
     */
    suspend fun updateReducedMotionEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[REDUCED_MOTION_ENABLED] = enabled
        }
    }

    /**
     * Updates onboarding completion flag.
     */
    suspend fun updateHasCompletedOnboarding(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[HAS_COMPLETED_ONBOARDING] = completed
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
        private val WIDGET_DISPLAY_MODE = stringPreferencesKey("widget_display_mode")
        private val QUOTE_LIST_FILTER = stringPreferencesKey("quote_list_filter")
        private val QUOTE_SEARCH_QUERY = stringPreferencesKey("quote_search_query")
        private val QUOTE_AUTHOR_FILTER = stringPreferencesKey("quote_author_filter")
        private val QUOTE_LENGTH_FILTER = stringPreferencesKey("quote_length_filter")
        private val QUOTE_COLLECTION_FILTER = stringPreferencesKey("quote_collection_filter")
        private val LAST_QUOTE_DATE = longPreferencesKey("last_quote_date")
        private val LAST_DAILY_QUOTE_ID = longPreferencesKey("last_daily_quote_id")
        private val THEME_MODE = stringPreferencesKey("theme_mode")
        private val THEME_PACK = stringPreferencesKey("theme_pack")
        private val SHOW_TRANSLITERATION = booleanPreferencesKey("show_transliteration")
        private val FONT_SIZE = floatPreferencesKey("font_size")
        private val LARGE_RUNES_ENABLED = booleanPreferencesKey("large_runes_enabled")
        private val HIGH_CONTRAST_ENABLED = booleanPreferencesKey("high_contrast_enabled")
        private val REDUCED_MOTION_ENABLED = booleanPreferencesKey("reduced_motion_enabled")
        private val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
    }
}
