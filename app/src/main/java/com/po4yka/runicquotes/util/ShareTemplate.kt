package com.po4yka.runicquotes.util

/**
 * Share image style presets.
 */
enum class ShareTemplate(val displayName: String) {
    CARD("Card"),
    VERSE("Verse"),
    LANDSCAPE("Landscape")
}

/**
 * Visual tone for share previews and generated images.
 */
enum class ShareAppearance(val displayName: String) {
    LIGHT("Light"),
    DARK("Dark")
}
