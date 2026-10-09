package com.po4yka.runatal.domain.transliteration

import com.po4yka.runatal.domain.translation.YoungerFutharkVariant
import java.util.Locale
import javax.inject.Inject

/**
 * Modern Latin transliteration using the sixteen long-branch Younger Futhark runes.
 * This direct spelling approximation does not translate the input into Old Norse.
 */
class YoungerFutharkTransliterator @Inject constructor() : RunicTransliterator {

    override val scriptName: String = "Younger Futhark"

    override fun transliterate(text: String): String {
        val spelling = text.lowercase(Locale.ROOT)
            .replace("th", "þ")
            .replace("ng", "n")
        return YoungerFutharkAlphabet.render(spelling, YoungerFutharkVariant.LONG_BRANCH)
    }
}
