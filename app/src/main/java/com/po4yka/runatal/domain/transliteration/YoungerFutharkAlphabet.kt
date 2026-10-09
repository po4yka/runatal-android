package com.po4yka.runatal.domain.transliteration

import com.po4yka.runatal.domain.translation.YoungerFutharkVariant
import java.util.Locale

/** The sixteen Younger Futhark signs, shared by direct and historical rendering. */
internal object YoungerFutharkAlphabet {

    private val longBranch = mapOf(
        'f' to 'ᚠ', 'u' to 'ᚢ', 'þ' to 'ᚦ', 'ą' to 'ᚬ',
        'r' to 'ᚱ', 'k' to 'ᚴ', 'h' to 'ᚼ', 'n' to 'ᚾ',
        'i' to 'ᛁ', 'a' to 'ᛅ', 's' to 'ᛋ', 't' to 'ᛏ',
        'b' to 'ᛒ', 'm' to 'ᛘ', 'l' to 'ᛚ', 'ʀ' to 'ᛦ'
    )
    private val shortTwig = longBranch.keys.zip("ᚠᚢᚦᚭᚱᚴᚽᚿᛁᛆᛌᛐᛓᛙᛚᛧ".toList()).toMap()
    private val aliases = mapOf(
        'o' to 'u', 'v' to 'u', 'w' to 'u',
        'e' to 'i', 'j' to 'i', 'y' to 'i',
        'c' to 'k', 'g' to 'k', 'q' to 'k',
        'p' to 'b', 'd' to 't', 'ð' to 'þ', 'z' to 's'
    )

    /** Renders Latin spellings using the selected sixteen-sign repertoire. */
    fun render(text: String, variant: YoungerFutharkVariant): String {
        val mapping = when (variant) {
            YoungerFutharkVariant.LONG_BRANCH -> longBranch
            YoungerFutharkVariant.SHORT_TWIG -> shortTwig
        }
        return buildString {
            text.lowercase(Locale.ROOT).forEach { character ->
                if (character == 'x') {
                    append(mapping.getValue('k'))
                    append(mapping.getValue('s'))
                } else {
                    append(mapping[aliases[character] ?: character] ?: character)
                }
            }
        }
    }
}
