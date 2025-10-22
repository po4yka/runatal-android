package com.po4yka.runicquotes.domain.transliteration

import com.po4yka.runicquotes.domain.model.RunicScript

/**
 * Factory for creating runic transliterators based on the script type.
 */
object TransliterationFactory {

    /**
     * Creates a transliterator for the specified runic script.
     *
     * @param script The runic script to create a transliterator for
     * @return A RunicTransliterator instance for the specified script
     */
    fun create(script: RunicScript): RunicTransliterator {
        return when (script) {
            RunicScript.ELDER_FUTHARK -> ElderFutharkTransliterator()
            RunicScript.YOUNGER_FUTHARK -> YoungerFutharkTransliterator()
            RunicScript.CIRTH -> CirthTransliterator()
        }
    }

    /**
     * Transliterates text to the specified runic script.
     * This is a convenience method that creates a transliterator and uses it.
     *
     * @param text The Latin text to transliterate
     * @param script The target runic script
     * @return The transliterated text
     */
    fun transliterate(text: String, script: RunicScript): String {
        return create(script).transliterate(text)
    }
}
