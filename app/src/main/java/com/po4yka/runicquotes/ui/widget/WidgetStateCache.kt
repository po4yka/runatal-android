package com.po4yka.runicquotes.ui.widget

import com.po4yka.runicquotes.data.preferences.UserPreferences
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

    fun get(
        widgetKey: String,
        currentDate: LocalDate,
        preferences: UserPreferences,
        widgetWidth: Int,
        widgetHeight: Int
    ): WidgetState? {
        val entry = cache[widgetKey] ?: return null
        val isValid = entry.date == currentDate &&
            entry.widgetWidth == widgetWidth &&
            entry.widgetHeight == widgetHeight &&
            entry.selectedScript == preferences.selectedScript.name &&
            entry.selectedFont == preferences.selectedFont &&
            entry.displayMode == preferences.widgetDisplayMode &&
            entry.updateMode == preferences.widgetUpdateMode &&
            entry.themeMode == preferences.themeMode &&
            entry.themePack == preferences.themePack &&
            entry.highContrastEnabled == preferences.highContrastEnabled &&
            entry.dynamicColorEnabled == preferences.dynamicColorEnabled
        return if (isValid) entry.state else null
    }

    fun put(
        widgetKey: String,
        date: LocalDate,
        preferences: UserPreferences,
        widgetWidth: Int,
        widgetHeight: Int,
        state: WidgetState
    ) {
        cache[widgetKey] = CacheEntry(
            date = date,
            widgetWidth = widgetWidth,
            widgetHeight = widgetHeight,
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

    fun clear(widgetKey: String) {
        cache.remove(widgetKey)
    }

    fun clear() {
        cache.clear()
    }
}
