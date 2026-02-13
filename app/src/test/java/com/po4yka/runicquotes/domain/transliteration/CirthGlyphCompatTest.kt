package com.po4yka.runicquotes.domain.transliteration

import org.junit.Assert.assertEquals
import org.junit.Test

class CirthGlyphCompatTest {

    @Test
    fun `normalize legacy PUA glyphs to visible runes`() {
        val legacy = "\uE088\uE0B4\uE0CB\uE09C \uE0B8\uE0CA\uE0A8\uE0A8"
        val normalized = CirthGlyphCompat.normalizeLegacyPuaGlyphs(legacy)

        assertEquals("\u16CF\u16BE\u16DF\u16CB \u16B9\u16A8\u16DA\u16DA", normalized)
    }

    @Test
    fun `leave plain unicode runes unchanged`() {
        val runes = "\u16A0\u16A2\u16A6\u16A8\u16B1\u16B2"
        val normalized = CirthGlyphCompat.normalizeLegacyPuaGlyphs(runes)

        assertEquals(runes, normalized)
    }

    @Test
    fun `preserve punctuation and spaces`() {
        val legacyWithPunctuation = "\uE0B4\uE0CB\uE088, \uE0CA\uE0A8\uE0A8!"
        val normalized = CirthGlyphCompat.normalizeLegacyPuaGlyphs(legacyWithPunctuation)

        assertEquals("\u16BE\u16DF\u16CF, \u16A8\u16DA\u16DA!", normalized)
    }
}
