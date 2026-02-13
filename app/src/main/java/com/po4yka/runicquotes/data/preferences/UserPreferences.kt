package com.po4yka.runicquotes.data.preferences

import com.po4yka.runicquotes.domain.model.RunicScript

/**
 * Data class representing user preferences.
 */
data class UserPreferences(
    /**
     * The currently selected runic script.
     */
    val selectedScript: RunicScript = RunicScript.DEFAULT,

    /**
     * The currently selected font (noto, babelstone, or cirth).
     */
    val selectedFont: String = "noto",

    /**
     * Widget update mode (daily, manual, etc.).
     */
    val widgetUpdateMode: String = "daily",

    /**
     * Widget content mode (rune_only, rune_latin, daily_random_tap).
     */
    val widgetDisplayMode: String = "rune_latin",

    /**
     * Persisted quote list base filter (all, favorites, user_created, system).
     */
    val quoteListFilter: String = "all",

    /**
     * Persisted quote search query.
     */
    val quoteSearchQuery: String = "",

    /**
     * Persisted quote author filter; empty means all authors.
     */
    val quoteAuthorFilter: String = "",

    /**
     * Persisted quote length filter (any, short, medium, long).
     */
    val quoteLengthFilter: String = "any",

    /**
     * Persisted quote collection filter (all, motivation, stoic, tolkien).
     */
    val quoteCollectionFilter: String = "all",

    /**
     * The last date a quote was displayed (in epoch days).
     */
    val lastQuoteDate: Long = 0L,

    /**
     * The ID of the last daily quote displayed.
     */
    val lastDailyQuoteId: Long = 0L,

    /**
     * Theme mode (light, dark, system).
     */
    val themeMode: String = "system",

    /**
     * Whether dynamic color is enabled (Android 12+).
     */
    val dynamicColorEnabled: Boolean = false,

    /**
     * Visual theme pack (stone, parchment, night_ink).
     */
    val themePack: String = "stone",

    /**
     * Whether to show transliteration alongside runic text.
     */
    val showTransliteration: Boolean = true,

    /**
     * Font size multiplier for runic text.
     */
    val fontSize: Float = 1.0f,

    /**
     * Accessibility preset: larger runes across primary reading surfaces.
     */
    val largeRunesEnabled: Boolean = false,

    /**
     * Accessibility preset: high contrast colors for stronger legibility.
     */
    val highContrastEnabled: Boolean = false,

    /**
     * Accessibility preset: reduce non-essential motion.
     */
    val reducedMotionEnabled: Boolean = false,

    /**
     * Whether onboarding has been completed.
     */
    val hasCompletedOnboarding: Boolean = false
)
