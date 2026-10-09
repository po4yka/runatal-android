package com.po4yka.runatal.domain.translation

import com.google.common.truth.Truth.assertThat
import java.util.Locale
import org.junit.Test

class EnglishSyntaxParserTest {

    private val parser = EnglishSyntaxParser()

    @Test
    fun `scanner covers every non-whitespace source code unit without changing raw spans`() {
        val source = "  2\u00A0wolves (cafe\u0301), Привет 😀💡 — wolf2\n$42! "
        val tokens = parser.parse(source).tokens
        val covered = BooleanArray(source.length)
        tokens.forEach { token ->
            assertThat(source.substring(token.startOffset, token.endOffset)).isEqualTo(token.raw)
            for (offset in token.startOffset until token.endOffset) {
                assertThat(covered[offset]).isFalse()
                covered[offset] = true
            }
        }
        source.forEachIndexed { offset, character ->
            assertThat(covered[offset]).isEqualTo(!character.isWhitespace())
        }
        assertThat(tokens.filter { it.type == ParsedEnglishTokenType.UNSUPPORTED }.map { it.raw })
            .containsExactly("2", "cafe\u0301", "Привет", "😀💡", "wolf2", "$42")
    }

    @Test
    fun `numeric literals and mixed alphanumeric identifiers stay indivisible`() {
        val source = "12.5 12:30 2026-10-09 $12.50 runatal42 42runatal user_42"
        val tokens = parser.parse(source).tokens

        assertThat(tokens.map { it.raw }).containsExactly(
            "12.5", "12:30", "2026-10-09", "$12.50", "runatal42", "42runatal", "user_42"
        ).inOrder()
        assertThat(tokens.all { it.type == ParsedEnglishTokenType.UNSUPPORTED }).isTrue()
    }

    @Test
    fun `curly and straight internal apostrophes remain one word with canonical lookup spelling`() {
        val tokens = parser.parse("don't don’t wolf‘s").tokens

        assertThat(tokens.map { it.raw }).containsExactly("don't", "don’t", "wolf‘s").inOrder()
        assertThat(tokens.map { it.normalized }).containsExactly("don't", "don't", "wolf's").inOrder()
        assertThat(tokens.map { it.type }).containsExactly(
            ParsedEnglishTokenType.WORD,
            ParsedEnglishTokenType.WORD,
            ParsedEnglishTokenType.WORD
        )
    }

    @Test
    fun `combining marks normalize as a complete unsupported word`() {
        val token = parser.parse("cafe\u0301").tokens.single()

        assertThat(token.raw).isEqualTo("cafe\u0301")
        assertThat(token.normalized).isEqualTo("café")
        assertThat(token.type).isEqualTo(ParsedEnglishTokenType.UNSUPPORTED)
        assertThat(token.endOffset).isEqualTo(5)
    }

    @Test
    fun `English lookup casing is independent of the device locale`() {
        val originalLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            assertThat(parser.parse("I NIGHT").tokens.map { it.normalized }).containsExactly("i", "night")
        } finally {
            Locale.setDefault(originalLocale)
        }
    }

    @Test
    fun `source spans remain original across leading spaces line breaks and astral symbols`() {
        val source = "\n\tI\u00A0hunt 😀!"
        val tokens = parser.parse(source).tokens

        assertThat(tokens.map { it.startOffset }).containsExactly(2, 4, 9, 11).inOrder()
        assertThat(tokens.map { it.endOffset }).containsExactly(3, 8, 11, 12).inOrder()
        assertThat(parser.parse(" \t\n\u00A0 ").tokens).isEmpty()
    }
}
