package com.po4yka.runicquotes.domain.model

import com.po4yka.runicquotes.domain.transliteration.TransliterationFactory

/**
 * Extension functions for Quote domain model.
 * Contains business logic for quote operations.
 */

/**
 * Gets the runic text for this quote based on the selected script.
 * If pre-computed runic text exists, uses it; otherwise transliterates on the fly.
 *
 * @param script The runic script to get text for
 * @return The runic text in the specified script
 */
fun Quote.getRunicText(script: RunicScript): String {
    // Try to get pre-computed runic text
    val precomputedText = when (script) {
        RunicScript.ELDER_FUTHARK -> runicElder
        RunicScript.YOUNGER_FUTHARK -> runicYounger
        RunicScript.CIRTH -> runicCirth
    }

    // If pre-computed text exists, use it; otherwise transliterate on the fly
    return precomputedText ?: TransliterationFactory.transliterate(textLatin, script)
}
