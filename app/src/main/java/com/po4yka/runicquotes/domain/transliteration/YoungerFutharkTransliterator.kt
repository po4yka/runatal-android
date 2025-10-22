package com.po4yka.runicquotes.domain.transliteration

/**
 * Transliterator for Younger Futhark runic script.
 * Younger Futhark is a simplified version used during the Viking Age (~800-1100 AD).
 * Unicode range: U+16A0–U+16EA (subset of Runic block)
 */
class YoungerFutharkTransliterator : RunicTransliterator {

    override val scriptName: String = "Younger Futhark"

    /**
     * Mapping of Latin characters to Younger Futhark runes.
     * Younger Futhark has only 16 runes compared to Elder Futhark's 24.
     */
    private val runicMap = mapOf(
        'f' to '\u16A0', // ᚠ FE
        'u' to '\u16A2', // ᚢ UR
        'v' to '\u16A2', // ᚢ UR (v -> u)
        'þ' to '\u16A6', // ᚦ THURS
        'a' to '\u16A8', // ᚨ AS/OSS
        'r' to '\u16B1', // ᚱ REID
        'k' to '\u16B2', // ᚲ KAUN
        'c' to '\u16B2', // ᚲ KAUN (c -> k)
        'g' to '\u16B2', // ᚲ KAUN (g -> k in Younger Futhark)
        'h' to '\u16BB', // ᚻ HAGALL
        'n' to '\u16BE', // ᚾ NAUD
        'i' to '\u16C1', // ᛁ IS
        'j' to '\u16C1', // ᛁ IS (j -> i)
        'y' to '\u16C1', // ᛁ IS (y -> i)
        'p' to '\u16C8', // ᛈ (approximation, not traditional YF)
        'z' to '\u16CA', // ᛊ SOL (z -> s)
        's' to '\u16CA', // ᛊ SOL
        't' to '\u16CF', // ᛏ TYR
        'b' to '\u16D2', // ᛒ BJARKAN
        'e' to '\u16D6', // ᛖ (approximation)
        'm' to '\u16D7', // ᛗ MADR
        'l' to '\u16DA', // ᛚ LOGR
        'o' to '\u16DF', // ᛟ (approximation)
        'd' to '\u16A6', // ᚦ THURS (d approximated as th)
        'w' to '\u16A2', // ᚢ UR (w -> u)
        'x' to '\u16B2', // ᚲ KAUN
        'q' to '\u16B2', // ᚲ KAUN
        ' ' to ' ',      // Preserve spaces
        '.' to '.',      // Preserve periods
        ',' to ',',      // Preserve commas
        '!' to '!',      // Preserve exclamations
        '?' to '?',      // Preserve questions
        '\'' to '\'',    // Preserve apostrophes
        '"' to '"',      // Preserve quotes
        '-' to '-',      // Preserve hyphens
        ':' to ':',      // Preserve colons
        ';' to ';'       // Preserve semicolons
    )

    /**
     * Digraph mappings for special character combinations.
     */
    private val digraphMap = mapOf(
        "th" to "\u16A6", // ᚦ THURS
        "ng" to "\u16BE"  // ᚾ NAUD (approximation)
    )

    override fun transliterate(text: String): String {
        var result = text.lowercase()

        // Replace digraphs first
        digraphMap.forEach { (latin, rune) ->
            result = result.replace(latin, rune)
        }

        // Replace individual characters
        return result.map { char ->
            runicMap[char] ?: char
        }.joinToString("")
    }
}
