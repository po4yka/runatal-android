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
    val fontSize: Float = 1.0f
)
