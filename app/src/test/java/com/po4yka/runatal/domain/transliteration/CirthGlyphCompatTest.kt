package com.po4yka.runatal.domain.transliteration

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** Exercises the public path from saved Cirth output to font-compatible Unicode text. */
class CirthGlyphCompatTest {

    @Test
    fun `normalize legacy PUA glyphs to visible runes`() {
        val legacy = "\uE088\uE0B4\uE0CB\uE09C \uE0B8\uE0CA\uE0A8\uE0A8"
        val normalized = CirthGlyphCompat.normalizeLegacyPuaGlyphs(legacy)

        assertThat(normalized).isEqualTo("\u16CF\u16BE\u16DF\u16CB \u16B9\u16A8\u16DA\u16DA")
    }

    @Test
    fun `leave plain unicode runes unchanged`() {
        val runes = "\u16A0\u16A2\u16A6\u16A8\u16B1\u16B2"
        val normalized = CirthGlyphCompat.normalizeLegacyPuaGlyphs(runes)

        assertThat(normalized).isEqualTo(runes)
    }

    @Test
    fun `preserve punctuation and spaces`() {
        val legacyWithPunctuation = "\uE0B4\uE0CB\uE088, \uE0CA\uE0A8\uE0A8!"
        val normalized = CirthGlyphCompat.normalizeLegacyPuaGlyphs(legacyWithPunctuation)

        assertThat(normalized).isEqualTo("\u16BE\u16DF\u16CF, \u16A8\u16DA\u16DA!")
    }

    @Test
    fun `direct Cirth alphabet and digraphs render as Unicode runes with every delimiter intact`() {
        val source = "a b c d e f g h i j k l m n o p q r s t u v w x y z | th ch sh ng"
        val legacy = CirthTransliterator().transliterate(source)

        val rendered = CirthGlyphCompat.normalizeLegacyPuaGlyphs(legacy)
        val legacyGlyphs = legacy.filter { it.code in PRIVATE_USE_RANGE }
        val runes = rendered.filter { it != ' ' && it != '|' }

        assertThat(legacyGlyphs.toSet()).hasSize(27)
        assertThat(runes.toSet()).hasSize(27)
        assertThat(runes.length).isEqualTo(legacyGlyphs.length)
        assertThat(runes.all { it.code in RUNIC_RANGE }).isTrue()
        assertThat(rendered.any { it.code in PRIVATE_USE_RANGE }).isFalse()
        assertThat(rendered.filter { it == ' ' || it == '|' })
            .isEqualTo(source.filter { it == ' ' || it == '|' })
        assertThat(CirthGlyphCompat.normalizeLegacyPuaGlyphs(rendered)).isEqualTo(rendered)
    }

    @Test
    fun `mixed saved text preserves Unicode prefix emoji and unknown glyphs around converted letters`() {
        val source = "Latin 😀 \uE0BB \uE089\uE0C9 | \uE0BB end"

        val rendered = CirthGlyphCompat.normalizeLegacyPuaGlyphs(source)

        assertThat(rendered).isEqualTo("Latin 😀 \uE0BB ᛞᛖ | \uE0BB end")
        assertThat(CirthGlyphCompat.normalizeLegacyPuaGlyphs(rendered)).isEqualTo(rendered)
    }

    private companion object {
        val PRIVATE_USE_RANGE = 0xE000..0xF8FF
        val RUNIC_RANGE = 0x16A0..0x16FF
    }
}
