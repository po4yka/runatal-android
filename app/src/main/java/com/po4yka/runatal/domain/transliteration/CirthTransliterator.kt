package com.po4yka.runatal.domain.transliteration

import javax.inject.Inject

/** Educational Latin spelling approximation using genuine UCSUR Cirth glyphs. */
class CirthTransliterator @Inject constructor() : RunicTransliterator {
    override val scriptName: String = "Cirth (Angerthas)"

    override fun transliterate(text: String): String = CirthAlphabet.transliterate(text)
}
