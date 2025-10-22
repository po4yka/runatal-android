package com.po4yka.runicquotes.domain.transliteration

/**
 * Transliterator for Elder Futhark runic script.
 * Unicode range: U+16A0–U+16FF
 */
class ElderFutharkTransliterator : RunicTransliterator {

    override val scriptName: String = "Elder Futhark"

    /**
     * Mapping of Latin characters to Elder Futhark runes.
     * Based on the standard Elder Futhark alphabet.
     */
    private val runicMap = mapOf(
        'f' to '\u16A0', // ᚠ FEHU
        'u' to '\u16A2', // ᚢ URUZ
        'v' to '\u16A2', // ᚢ URUZ (v -> u)
        'þ' to '\u16A6', // ᚦ THURISAZ
        'a' to '\u16A8', // ᚨ ANSUZ
        'r' to '\u16B1', // ᚱ RAIDO
        'k' to '\u16B2', // ᚲ KAUNA
        'c' to '\u16B2', // ᚲ KAUNA (c -> k)
        'g' to '\u16B7', // ᚷ GEBO
        'w' to '\u16B9', // ᚹ WUNJO
        'h' to '\u16BB', // ᚻ HAGLAZ
        'n' to '\u16BE', // ᚾ NAUDIZ
        'i' to '\u16C1', // ᛁ ISAZ
        'j' to '\u16C3', // ᛃ JERAN
        'y' to '\u16C3', // ᛃ JERAN (y -> j)
        'ï' to '\u16C7', // ᛇ IWAZ
        'p' to '\u16C8', // ᛈ PERTH
        'z' to '\u16C9', // ᛉ ALGIZ
        's' to '\u16CA', // ᛊ SOWILO
        't' to '\u16CF', // ᛏ TIWAZ
        'b' to '\u16D2', // ᛒ BERKANAN
        'e' to '\u16D6', // ᛖ EHWAZ
        'm' to '\u16D7', // ᛗ MANNAZ
        'l' to '\u16DA', // ᛚ LAGUZ
        'ŋ' to '\u16DC', // ᛜ INGWAZ
        'o' to '\u16DF', // ᛟ OTHALAN
        'd' to '\u16DE', // ᛞ DAGAZ
        'x' to '\u16B2', // ᚲ KAUNA + SOWILO (approximation)
        'q' to '\u16B2', // ᚲ KAUNA + WUNJO (approximation)
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
        "th" to "\u16A6", // ᚦ THURISAZ
        "ng" to "\u16DC"  // ᛜ INGWAZ
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
