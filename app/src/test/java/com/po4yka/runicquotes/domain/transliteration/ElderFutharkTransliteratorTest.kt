package com.po4yka.runicquotes.domain.transliteration

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

/**
 * Comprehensive unit tests for ElderFutharkTransliterator.
 * Tests all rune mappings, digraphs, edge cases, and special characters.
 *
 * Coverage goals: >95%
 */
class ElderFutharkTransliteratorTest {

    private lateinit var transliterator: ElderFutharkTransliterator

    @Before
    fun setUp() {
        transliterator = ElderFutharkTransliterator()
    }

    @Test
    fun `script name is Elder Futhark`() {
        assertThat(transliterator.scriptName).isEqualTo("Elder Futhark")
    }

    // ==================== Individual Rune Mappings ====================

    @Test
    fun `transliterate f to FEHU`() {
        assertThat(transliterator.transliterate("f")).isEqualTo("\u16A0")
    }

    @Test
    fun `transliterate u to URUZ`() {
        assertThat(transliterator.transliterate("u")).isEqualTo("\u16A2")
    }

    @Test
    fun `transliterate v to URUZ (v-u equivalence)`() {
        assertThat(transliterator.transliterate("v")).isEqualTo("\u16A2")
    }

    @Test
    fun `transliterate þ to THURISAZ`() {
        assertThat(transliterator.transliterate("þ")).isEqualTo("\u16A6")
    }

    @Test
    fun `transliterate a to ANSUZ`() {
        assertThat(transliterator.transliterate("a")).isEqualTo("\u16A8")
    }

    @Test
    fun `transliterate r to RAIDO`() {
        assertThat(transliterator.transliterate("r")).isEqualTo("\u16B1")
    }

    @Test
    fun `transliterate k to KAUNA`() {
        assertThat(transliterator.transliterate("k")).isEqualTo("\u16B2")
    }

    @Test
    fun `transliterate c to KAUNA (c-k equivalence)`() {
        assertThat(transliterator.transliterate("c")).isEqualTo("\u16B2")
    }

    @Test
    fun `transliterate g to GEBO`() {
        assertThat(transliterator.transliterate("g")).isEqualTo("\u16B7")
    }

    @Test
    fun `transliterate w to WUNJO`() {
        assertThat(transliterator.transliterate("w")).isEqualTo("\u16B9")
    }

    @Test
    fun `transliterate h to HAGLAZ`() {
        assertThat(transliterator.transliterate("h")).isEqualTo("\u16BB")
    }

    @Test
    fun `transliterate n to NAUDIZ`() {
        assertThat(transliterator.transliterate("n")).isEqualTo("\u16BE")
    }

    @Test
    fun `transliterate i to ISAZ`() {
        assertThat(transliterator.transliterate("i")).isEqualTo("\u16C1")
    }

    @Test
    fun `transliterate j to JERAN`() {
        assertThat(transliterator.transliterate("j")).isEqualTo("\u16C3")
    }

    @Test
    fun `transliterate y to JERAN (y-j equivalence)`() {
        assertThat(transliterator.transliterate("y")).isEqualTo("\u16C3")
    }

    @Test
    fun `transliterate p to PERTH`() {
        assertThat(transliterator.transliterate("p")).isEqualTo("\u16C8")
    }

    @Test
    fun `transliterate z to ALGIZ`() {
        assertThat(transliterator.transliterate("z")).isEqualTo("\u16C9")
    }

    @Test
    fun `transliterate s to SOWILO`() {
        assertThat(transliterator.transliterate("s")).isEqualTo("\u16CA")
    }

    @Test
    fun `transliterate t to TIWAZ`() {
        assertThat(transliterator.transliterate("t")).isEqualTo("\u16CF")
    }

    @Test
    fun `transliterate b to BERKANAN`() {
        assertThat(transliterator.transliterate("b")).isEqualTo("\u16D2")
    }

    @Test
    fun `transliterate e to EHWAZ`() {
        assertThat(transliterator.transliterate("e")).isEqualTo("\u16D6")
    }

    @Test
    fun `transliterate m to MANNAZ`() {
        assertThat(transliterator.transliterate("m")).isEqualTo("\u16D7")
    }

    @Test
    fun `transliterate l to LAGUZ`() {
        assertThat(transliterator.transliterate("l")).isEqualTo("\u16DA")
    }

    @Test
    fun `transliterate o to OTHALAN`() {
        assertThat(transliterator.transliterate("o")).isEqualTo("\u16DF")
    }

    @Test
    fun `transliterate d to DAGAZ`() {
        assertThat(transliterator.transliterate("d")).isEqualTo("\u16DE")
    }

    // ==================== Digraph Mappings ====================

    @Test
    fun `transliterate th digraph to THURISAZ`() {
        val result = transliterator.transliterate("the")
        // "th" -> THURISAZ (U+16A6), "e" -> EHWAZ (U+16D6)
        assertThat(result).isEqualTo("\u16A6\u16D6")
    }

    @Test
    fun `transliterate ng digraph to INGWAZ`() {
        val result = transliterator.transliterate("king")
        // "k" -> KAUNA (U+16B2), "i" -> ISAZ (U+16C1), "ng" -> INGWAZ (U+16DC)
        assertThat(result).isEqualTo("\u16B2\u16C1\u16DC")
    }

    @Test
    fun `digraphs take precedence over individual characters`() {
        val result = transliterator.transliterate("thing")
        // "th" -> THURISAZ (U+16A6), "i" -> ISAZ (U+16C1), "ng" -> INGWAZ (U+16DC)
        assertThat(result).isEqualTo("\u16A6\u16C1\u16DC")
    }

    // ==================== Word and Phrase Tests ====================

    @Test
    fun `transliterate simple word - rune`() {
        val result = transliterator.transliterate("rune")
        // r -> U+16B1, u -> U+16A2, n -> U+16BE, e -> U+16D6
        assertThat(result).isEqualTo("\u16B1\u16A2\u16BE\u16D6")
    }

    @Test
    fun `transliterate word with digraph - strength`() {
        val result = transliterator.transliterate("strength")
        // s -> U+16CA, t -> U+16CF, r -> U+16B1, e -> U+16D6, "ng" -> U+16DC, "th" -> U+16A6
        assertThat(result).isEqualTo("\u16CA\u16CF\u16B1\u16D6\u16DC\u16A6")
    }

    @Test
    fun `transliterate phrase with spaces`() {
        val result = transliterator.transliterate("be brave")
        // b -> U+16D2, e -> U+16D6, " ", b -> U+16D2, r -> U+16B1, a -> U+16A8, v -> U+16A2, e -> U+16D6
        assertThat(result).isEqualTo("\u16D2\u16D6 \u16D2\u16B1\u16A8\u16A2\u16D6")
    }

    // ==================== Case Handling ====================

    @Test
    fun `uppercase letters are converted to lowercase then transliterated`() {
        val upper = transliterator.transliterate("RUNE")
        val lower = transliterator.transliterate("rune")
        assertThat(upper).isEqualTo(lower)
    }

    @Test
    fun `mixed case is normalized`() {
        val result = transliterator.transliterate("RuNe")
        assertThat(result).isEqualTo("\u16B1\u16A2\u16BE\u16D6")
    }

    // ==================== Punctuation Preservation ====================

    @Test
    fun `preserve spaces`() {
        val result = transliterator.transliterate("a b c")
        assertThat(result).isEqualTo("\u16A8 \u16D2 \u16B2")
    }

    @Test
    fun `preserve period`() {
        val result = transliterator.transliterate("end.")
        assertThat(result).isEqualTo("\u16D6\u16BE\u16DE.")
    }

    @Test
    fun `preserve comma`() {
        val result = transliterator.transliterate("one, two")
        assertThat(result).isEqualTo("\u16DF\u16BE\u16D6, \u16CF\u16B9\u16DF")
    }

    @Test
    fun `preserve exclamation mark`() {
        val result = transliterator.transliterate("stop!")
        assertThat(result).isEqualTo("\u16CA\u16CF\u16DF\u16C8!")
    }

    @Test
    fun `preserve question mark`() {
        val result = transliterator.transliterate("why?")
        assertThat(result).isEqualTo("\u16B9\u16BB\u16C3?")
    }

    @Test
    fun `preserve apostrophe`() {
        val result = transliterator.transliterate("don't")
        assertThat(result).isEqualTo("\u16DE\u16DF\u16BE'\u16CF")
    }

    @Test
    fun `preserve quotes`() {
        val result = transliterator.transliterate("\"word\"")
        assertThat(result).isEqualTo("\"\u16B9\u16DF\u16B1\u16DE\"")
    }

    @Test
    fun `preserve hyphen`() {
        val result = transliterator.transliterate("well-done")
        assertThat(result).isEqualTo("\u16B9\u16D6\u16DA\u16DA-\u16DE\u16DF\u16BE\u16D6")
    }

    @Test
    fun `preserve colon`() {
        val result = transliterator.transliterate("note: here")
        assertThat(result).isEqualTo("\u16BE\u16DF\u16CF\u16D6: \u16BB\u16D6\u16B1\u16D6")
    }

    @Test
    fun `preserve semicolon`() {
        val result = transliterator.transliterate("one; two")
        assertThat(result).isEqualTo("\u16DF\u16BE\u16D6; \u16CF\u16B9\u16DF")
    }

    // ==================== Edge Cases ====================

    @Test
    fun `empty string returns empty string`() {
        assertThat(transliterator.transliterate("")).isEqualTo("")
    }

    @Test
    fun `single space returns single space`() {
        assertThat(transliterator.transliterate(" ")).isEqualTo(" ")
    }

    @Test
    fun `multiple spaces are preserved`() {
        assertThat(transliterator.transliterate("   ")).isEqualTo("   ")
    }

    @Test
    fun `unmapped characters are preserved`() {
        // Numbers and special symbols not in the map
        val result = transliterator.transliterate("123@#$%")
        assertThat(result).isEqualTo("123@#$%")
    }

    @Test
    fun `mixed letters and numbers`() {
        val result = transliterator.transliterate("test123")
        // t -> U+16CF, e -> U+16D6, s -> U+16CA, t -> U+16CF, 1, 2, 3
        assertThat(result).isEqualTo("\u16CF\u16D6\u16CA\u16CF123")
    }

    @Test
    fun `unicode emoji characters are preserved`() {
        val result = transliterator.transliterate("rune \uD83D\uDD25")
        assertThat(result).isEqualTo("\u16B1\u16A2\u16BE\u16D6 \uD83D\uDD25")
    }

    // ==================== Real-World Quote Tests ====================

    @Test
    fun `transliterate inspirational quote`() {
        val result = transliterator.transliterate("be brave")
        assertThat(result).isEqualTo("\u16D2\u16D6 \u16D2\u16B1\u16A8\u16A2\u16D6")
    }

    @Test
    fun `transliterate quote with punctuation`() {
        val result = transliterator.transliterate("know thyself.")
        // k -> U+16B2, n -> U+16BE, o -> U+16DF, w -> U+16B9, " ",
        // "th" -> U+16A6, y -> U+16C3, s -> U+16CA, e -> U+16D6, l -> U+16DA, f -> U+16A0, "."
        assertThat(result).isEqualTo("\u16B2\u16BE\u16DF\u16B9 \u16A6\u16C3\u16CA\u16D6\u16DA\u16A0.")
    }

    @Test
    fun `transliterate Norse-inspired phrase`() {
        val result = transliterator.transliterate("the king")
        // "th" -> U+16A6, e -> U+16D6, " ", k -> U+16B2, i -> U+16C1, "ng" -> U+16DC
        assertThat(result).isEqualTo("\u16A6\u16D6 \u16B2\u16C1\u16DC")
    }

    // ==================== Character Equivalence Tests ====================

    @Test
    fun `v and u map to same rune`() {
        val vResult = transliterator.transliterate("v")
        val uResult = transliterator.transliterate("u")
        assertThat(vResult).isEqualTo(uResult)
    }

    @Test
    fun `c and k map to same rune`() {
        val cResult = transliterator.transliterate("c")
        val kResult = transliterator.transliterate("k")
        assertThat(cResult).isEqualTo(kResult)
    }

    @Test
    fun `y and j map to same rune`() {
        val yResult = transliterator.transliterate("y")
        val jResult = transliterator.transliterate("j")
        assertThat(yResult).isEqualTo(jResult)
    }

    // ==================== Stress Tests ====================

    @Test
    fun `long text performance`() {
        val longText = "the quick brown fox jumps over the lazy dog ".repeat(100)
        val result = transliterator.transliterate(longText)
        assertThat(result).isNotEmpty()
        // Just verify it doesn't crash and produces output
    }

    @Test
    fun `all Latin alphabet characters`() {
        val alphabet = "abcdefghijklmnopqrstuvwxyz"
        val result = transliterator.transliterate(alphabet)
        assertThat(result).isNotEmpty()
        // Verify all characters are transliterated
        assertThat(result.none { it.isLowerCase() && it.isLetter() }).isTrue()
    }

    @Test
    fun `complete sentence with all features`() {
        val sentence = "The brave king's strength, wasn't it?"
        val result = transliterator.transliterate(sentence)
        // Verify it contains runes and punctuation
        assertThat(result).contains(",")
        assertThat(result).contains("'")
        assertThat(result).contains("?")
        assertThat(result).contains(" ")
    }
}
