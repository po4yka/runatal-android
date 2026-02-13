package com.po4yka.runicquotes.ui.widget

/**
 * Widget content variants.
 */
enum class WidgetDisplayMode(
    val persistedValue: String,
    val displayName: String,
    val subtitle: String
) {
    RUNE_ONLY(
        persistedValue = "rune_only",
        displayName = "Rune Only",
        subtitle = "Large runes without Latin text"
    ),
    RUNE_LATIN(
        persistedValue = "rune_latin",
        displayName = "Rune + Latin",
        subtitle = "Runes with translation and author"
    ),
    DAILY_RANDOM_TAP(
        persistedValue = "daily_random_tap",
        displayName = "Daily + Tap Random",
        subtitle = "Show daily quote and tap widget for random"
    );

    companion object {
        fun fromPersistedValue(value: String): WidgetDisplayMode {
            return entries.firstOrNull { it.persistedValue == value } ?: RUNE_LATIN
        }
    }
}
