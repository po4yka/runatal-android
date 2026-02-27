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
 * @param transliterationFactory Factory for on-the-fly transliteration when pre-computed text is absent
 * @return The runic text in the specified script
 */
fun Quote.getRunicText(script: RunicScript, transliterationFactory: TransliterationFactory): String {
    val precomputedText = when (script) {
        RunicScript.ELDER_FUTHARK -> runicElder
        RunicScript.YOUNGER_FUTHARK -> runicYounger
        RunicScript.CIRTH -> runicCirth
    }

    return precomputedText ?: transliterationFactory.transliterate(textLatin, script)
}
