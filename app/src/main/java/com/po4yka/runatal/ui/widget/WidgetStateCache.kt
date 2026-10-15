package com.po4yka.runatal.ui.widget

import com.po4yka.runatal.data.preferences.UserPreferences
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap

/**
 * Cache for widget state keyed per widget instance.
 * State is invalidated when date, preferences, or widget size changes.
 */
object WidgetStateCache {

    private data class CacheEntry(
        val date: LocalDate,
        val widgetWidth: Int,
        val widgetHeight: Int,
        val renderEnvironment: String,
        val selectedScript: String,
        val selectedFont: String,
        val displayMode: String,
        val updateMode: String,
        val themeMode: String,
        val themePack: String,
        val highContrastEnabled: Boolean,
        val dynamicColorEnabled: Boolean,
        val state: WidgetState
    )

    private val cache = ConcurrentHashMap<String, CacheEntry>()

    /** Returns the cached [WidgetState] if it is still valid, or null. */
    fun get(
        widgetKey: String,
        currentDate: LocalDate,
        preferences: UserPreferences,
        widgetWidth: Int,
        widgetHeight: Int,
        renderEnvironment: String,
        expectedContent: WidgetQuoteContent
    ): WidgetState? {
        val entry = cache[widgetKey] ?: return null
        val isValid = expectedContent.matches(entry.state) &&
            matchesDimensions(entry, currentDate, widgetWidth, widgetHeight, renderEnvironment) &&
            matchesPreferences(entry, preferences)
        return if (isValid) entry.state else null
    }

    private fun matchesDimensions(
        entry: CacheEntry, date: LocalDate, width: Int, height: Int, environment: String
    ): Boolean = entry.date == date && entry.widgetWidth == width && entry.widgetHeight == height &&
        entry.renderEnvironment == environment

    private fun matchesPreferences(entry: CacheEntry, preferences: UserPreferences): Boolean =
        entry.selectedScript == preferences.selectedScript.name && entry.selectedFont == preferences.selectedFont &&
            entry.displayMode == preferences.widgetDisplayMode && entry.updateMode == preferences.widgetUpdateMode &&
            entry.themeMode == preferences.themeMode && entry.themePack == preferences.themePack &&
            entry.highContrastEnabled == preferences.highContrastEnabled &&
            entry.dynamicColorEnabled == preferences.dynamicColorEnabled

    /** Stores [state] in the cache for the given widget and parameters. */
    fun put(
        widgetKey: String,
        date: LocalDate,
        preferences: UserPreferences,
        widgetWidth: Int,
        widgetHeight: Int,
        renderEnvironment: String,
        state: WidgetState
    ) {
        cache[widgetKey] = CacheEntry(
            date = date,
            widgetWidth = widgetWidth,
            widgetHeight = widgetHeight,
            renderEnvironment = renderEnvironment,
            selectedScript = preferences.selectedScript.name,
            selectedFont = preferences.selectedFont,
            displayMode = preferences.widgetDisplayMode,
            updateMode = preferences.widgetUpdateMode,
            themeMode = preferences.themeMode,
            themePack = preferences.themePack,
            highContrastEnabled = preferences.highContrastEnabled,
            dynamicColorEnabled = preferences.dynamicColorEnabled,
            state = state
        )
    }

    /** Returns only a candidate identity; the loader must re-read its current quote content. */
    fun quoteId(widgetKey: String, date: LocalDate): Long? = cache[widgetKey]
        ?.takeIf { it.date == date }?.state?.quoteId?.takeIf { it > 0 }

    /** Removes the cached state for a single widget. */
    fun clear(widgetKey: String) {
        cache.remove(widgetKey)
    }

    /** Clears all cached widget states. */
    fun clear() {
        cache.clear()
    }
}
