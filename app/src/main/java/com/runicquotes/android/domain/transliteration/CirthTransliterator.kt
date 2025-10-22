package com.runicquotes.android.domain.transliteration

/**
 * Transliterator for Cirth (Angerthas) script.
 * This is Tolkien's fictional runic script from Middle-earth.
 * Uses Private Use Area (PUA) codepoints: U+E080+
 *
 * Note: This is a basic implementation. Full Cirth support requires
 * specialized fonts and more complex mappings.
 */
class CirthTransliterator : RunicTransliterator {

    override val scriptName: String = "Cirth (Angerthas)"

    /**
     * Basic mapping of Latin characters to Cirth runes.
     * This is a simplified mapping for demonstration.
     * Full implementation would require proper Angerthas Moria or Daeron mappings.
     */
    private val runicMap = mapOf(
        'p' to '\uE080', // Cirth 1
        'b' to '\uE081', // Cirth 2
        'f' to '\uE082', // Cirth 3
        'v' to '\uE083', // Cirth 4
        't' to '\uE088', // Cirth 9
        'd' to '\uE089', // Cirth 10
        'þ' to '\uE08A', // Cirth 11 (thorn)
        'k' to '\uE090', // Cirth 17
        'g' to '\uE091', // Cirth 18
        'h' to '\uE092', // Cirth 19
        's' to '\uE09C', // Cirth 29
        'z' to '\uE09D', // Cirth 30
        'r' to '\uE0A0', // Cirth 33
        'l' to '\uE0A8', // Cirth 41
        'm' to '\uE0B0', // Cirth 49
        'n' to '\uE0B4', // Cirth 53
        'w' to '\uE0B8', // Cirth 57
        'j' to '\uE0BC', // Cirth 61
        'y' to '\uE0BD', // Cirth 62
        'i' to '\uE0C8', // Cirth 73
        'e' to '\uE0C9', // Cirth 74
        'a' to '\uE0CA', // Cirth 75
        'o' to '\uE0CB', // Cirth 76
        'u' to '\uE0CC', // Cirth 77
        'c' to '\uE090', // Cirth 17 (c -> k)
        'q' to '\uE090', // Cirth 17 (q -> k)
        'x' to '\uE09C', // Cirth 29 (x -> s approximation)
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
        "th" to "\uE08A", // Cirth 11
        "ch" to "\uE093", // Cirth 20
        "sh" to "\uE09E", // Cirth 31
        "ng" to "\uE0B5"  // Cirth 54
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
