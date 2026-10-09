package com.po4yka.runatal.domain.transliteration

import com.po4yka.runatal.domain.translation.YoungerFutharkVariant
import javax.inject.Inject

/**
 * Modern Latin transliteration using the sixteen long-branch Younger Futhark runes.
 * This direct spelling approximation does not translate the input into Old Norse.
 */
class YoungerFutharkTransliterator @Inject constructor() : RunicTransliterator {

    override val scriptName: String = "Younger Futhark"

    override fun transliterate(text: String): String = transliterateWithVariant(text, YoungerFutharkVariant.LONG_BRANCH)

    /** Uses the chosen sixteen-sign repertoire for direct Latin spelling. */
    fun transliterateWithVariant(text: String, variant: YoungerFutharkVariant): String =
        YoungerFutharkAlphabet.renderLatinSpelling(text, variant)
}
