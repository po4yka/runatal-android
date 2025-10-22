package com.po4yka.runicquotes.domain.transliteration

/**
 * Interface for transliterating Latin text to runic scripts.
 */
interface RunicTransliterator {

    /**
     * Transliterates the given Latin text to the runic script.
     *
     * @param text The Latin text to transliterate
     * @return The transliterated runic text
     */
    fun transliterate(text: String): String

    /**
     * Returns the name of the runic script this transliterator produces.
     */
    val scriptName: String
}
