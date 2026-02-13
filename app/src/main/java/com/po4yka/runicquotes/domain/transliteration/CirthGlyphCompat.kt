package com.po4yka.runicquotes.domain.transliteration

/**
 * Compatibility helpers for legacy Cirth text.
 *
 * Earlier builds stored Cirth using Private Use Area (PUA) codepoints (U+E080+),
 * which are not covered by the bundled runic fonts. This mapper converts the
 * legacy glyphs to standard Unicode runes that are supported by app fonts.
 */
object CirthGlyphCompat {
    fun normalizeLegacyPuaGlyphs(text: String): String {
        var builder: StringBuilder? = null

        text.forEachIndexed { index, char ->
            val mapped = mapLegacyPuaChar(char)
            if (mapped != char) {
                if (builder == null) {
                    builder = StringBuilder(text.length)
                    builder?.append(text, 0, index)
                }
                builder?.append(mapped)
            } else {
                builder?.append(char)
            }
        }

        return builder?.toString() ?: text
    }

    private fun mapLegacyPuaChar(char: Char): Char = when (char) {
        '\uE080' -> '\u16C8' // p
        '\uE081' -> '\u16D2' // b
        '\uE082' -> '\u16A0' // f
        '\uE083' -> '\u16A1' // v
        '\uE088' -> '\u16CF' // t
        '\uE089' -> '\u16DE' // d
        '\uE08A' -> '\u16A6' // th
        '\uE090' -> '\u16B2' // k
        '\uE091' -> '\u16B7' // g
        '\uE092' -> '\u16BA' // h
        '\uE093' -> '\u16E3' // ch
        '\uE09C' -> '\u16CB' // s
        '\uE09D' -> '\u16C9' // z
        '\uE09E' -> '\u16CC' // sh
        '\uE0A0' -> '\u16B1' // r
        '\uE0A8' -> '\u16DA' // l
        '\uE0B0' -> '\u16D7' // m
        '\uE0B4' -> '\u16BE' // n
        '\uE0B5' -> '\u16DC' // ng
        '\uE0B8' -> '\u16B9' // w
        '\uE0BC' -> '\u16C3' // j
        '\uE0BD' -> '\u16A4' // y
        '\uE0C8' -> '\u16C1' // i
        '\uE0C9' -> '\u16D6' // e
        '\uE0CA' -> '\u16A8' // a
        '\uE0CB' -> '\u16DF' // o
        '\uE0CC' -> '\u16A2' // u
        else -> char
    }
}
