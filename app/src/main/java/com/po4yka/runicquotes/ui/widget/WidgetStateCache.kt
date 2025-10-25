package com.po4yka.runicquotes.ui.widget

import com.po4yka.runicquotes.data.preferences.UserPreferences
import java.time.LocalDate

/**
 * Cache for widget state to avoid redundant database queries.
 * State is invalidated when the date changes or preferences change.
 */
object WidgetStateCache {

    private var cachedState: WidgetState? = null
    private var cacheDate: LocalDate? = null
    private var cachedScript: String? = null
    private var cachedFont: String? = null

    /**
     * Retrieves cached widget state if valid for current date and preferences.
     *
     * @param currentDate Today's date
     * @param preferences Current user preferences
     * @return Cached WidgetState or null if cache is invalid
     */
    fun get(currentDate: LocalDate, preferences: UserPreferences): WidgetState? {
        // Check if cache is valid
        val isValid = cacheDate == currentDate &&
                cachedScript == preferences.selectedScript.name &&
                cachedFont == preferences.selectedFont

        return if (isValid) cachedState else null
    }

    /**
     * Stores widget state in cache.
     *
     * @param date Current date
     * @param preferences Current user preferences
     * @param state Widget state to cache
     */
    fun put(date: LocalDate, preferences: UserPreferences, state: WidgetState) {
        cacheDate = date
        cachedScript = preferences.selectedScript.name
        cachedFont = preferences.selectedFont
        cachedState = state
    }

    /**
     * Clears the cache, forcing a fresh load on next request.
     */
    fun clear() {
        cachedState = null
        cacheDate = null
        cachedScript = null
        cachedFont = null
    }

    /**
     * Checks if cache is valid for given date and preferences.
     *
     * @param currentDate Date to check
     * @param preferences Preferences to check
     * @return True if cache is valid
     */
    fun isValid(currentDate: LocalDate, preferences: UserPreferences): Boolean {
        return cacheDate == currentDate &&
                cachedScript == preferences.selectedScript.name &&
                cachedFont == preferences.selectedFont
    }
}
