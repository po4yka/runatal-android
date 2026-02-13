package com.po4yka.runicquotes.domain.transliteration

/**
 * Compatibility helpers for legacy Cirth text.
 *
 * Earlier builds stored Cirth using Private Use Area (PUA) codepoints (U+E080+),
 * which are not covered by the bundled runic fonts. This mapper converts the
 * legacy glyphs to standard Unicode runes that are supported by app fonts.
 */
object CirthGlyphCompat {
    private val legacyPuaToRunic = mapOf(
        '\uE080' to '\u16C8', // p
        '\uE081' to '\u16D2', // b
        '\uE082' to '\u16A0', // f
        '\uE083' to '\u16A1', // v
        '\uE088' to '\u16CF', // t
        '\uE089' to '\u16DE', // d
        '\uE08A' to '\u16A6', // th
        '\uE090' to '\u16B2', // k
        '\uE091' to '\u16B7', // g
        '\uE092' to '\u16BA', // h
        '\uE093' to '\u16E3', // ch
        '\uE09C' to '\u16CB', // s
        '\uE09D' to '\u16C9', // z
        '\uE09E' to '\u16CC', // sh
        '\uE0A0' to '\u16B1', // r
        '\uE0A8' to '\u16DA', // l
        '\uE0B0' to '\u16D7', // m
        '\uE0B4' to '\u16BE', // n
        '\uE0B5' to '\u16DC', // ng
        '\uE0B8' to '\u16B9', // w
        '\uE0BC' to '\u16C3', // j
        '\uE0BD' to '\u16A4', // y
        '\uE0C8' to '\u16C1', // i
        '\uE0C9' to '\u16D6', // e
        '\uE0CA' to '\u16A8', // a
        '\uE0CB' to '\u16DF', // o
        '\uE0CC' to '\u16A2'  // u
    )

    fun normalizeLegacyPuaGlyphs(text: String): String {
        if (text.none { it in '\uE000'..'\uF8FF' }) {
            return text
        }

        return buildString(text.length) {
            text.forEach { char ->
                append(legacyPuaToRunic[char] ?: char)
            }
        }
    }
}
