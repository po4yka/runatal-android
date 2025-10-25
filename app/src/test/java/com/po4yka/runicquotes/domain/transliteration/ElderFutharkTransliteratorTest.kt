package com.po4yka.runicquotes.domain.transliteration

import org.junit.Assert.assertEquals
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
        assertEquals("Elder Futhark", transliterator.scriptName)
    }

    // ==================== Individual Rune Mappings ====================

    @Test
    fun `transliterate f to FEHU`() {
        assertEquals("\u16A0", transliterator.transliterate("f"))
    }

    @Test
    fun `transliterate u to URUZ`() {
        assertEquals("\u16A2", transliterator.transliterate("u"))
    }

    @Test
    fun `transliterate v to URUZ (v-u equivalence)`() {
        assertEquals("\u16A2", transliterator.transliterate("v"))
    }

    @Test
    fun `transliterate þ to THURISAZ`() {
        assertEquals("\u16A6", transliterator.transliterate("þ"))
    }

    @Test
    fun `transliterate a to ANSUZ`() {
        assertEquals("\u16A8", transliterator.transliterate("a"))
    }

    @Test
    fun `transliterate r to RAIDO`() {
        assertEquals("\u16B1", transliterator.transliterate("r"))
    }

    @Test
    fun `transliterate k to KAUNA`() {
        assertEquals("\u16B2", transliterator.transliterate("k"))
    }

    @Test
    fun `transliterate c to KAUNA (c-k equivalence)`() {
        assertEquals("\u16B2", transliterator.transliterate("c"))
    }

    @Test
    fun `transliterate g to GEBO`() {
        assertEquals("\u16B7", transliterator.transliterate("g"))
    }

    @Test
    fun `transliterate w to WUNJO`() {
        assertEquals("\u16B9", transliterator.transliterate("w"))
    }

    @Test
    fun `transliterate h to HAGLAZ`() {
        assertEquals("\u16BB", transliterator.transliterate("h"))
    }

    @Test
    fun `transliterate n to NAUDIZ`() {
        assertEquals("\u16BE", transliterator.transliterate("n"))
    }

    @Test
    fun `transliterate i to ISAZ`() {
        assertEquals("\u16C1", transliterator.transliterate("i"))
    }

    @Test
    fun `transliterate j to JERAN`() {
        assertEquals("\u16C3", transliterator.transliterate("j"))
    }

    @Test
    fun `transliterate y to JERAN (y-j equivalence)`() {
        assertEquals("\u16C3", transliterator.transliterate("y"))
    }

    @Test
    fun `transliterate p to PERTH`() {
        assertEquals("\u16C8", transliterator.transliterate("p"))
    }

    @Test
    fun `transliterate z to ALGIZ`() {
        assertEquals("\u16C9", transliterator.transliterate("z"))
    }

    @Test
    fun `transliterate s to SOWILO`() {
        assertEquals("\u16CA", transliterator.transliterate("s"))
    }

    @Test
    fun `transliterate t to TIWAZ`() {
        assertEquals("\u16CF", transliterator.transliterate("t"))
    }

    @Test
    fun `transliterate b to BERKANAN`() {
        assertEquals("\u16D2", transliterator.transliterate("b"))
    }

    @Test
    fun `transliterate e to EHWAZ`() {
        assertEquals("\u16D6", transliterator.transliterate("e"))
    }

    @Test
    fun `transliterate m to MANNAZ`() {
        assertEquals("\u16D7", transliterator.transliterate("m"))
    }

    @Test
    fun `transliterate l to LAGUZ`() {
        assertEquals("\u16DA", transliterator.transliterate("l"))
    }

    @Test
    fun `transliterate o to OTHALAN`() {
        assertEquals("\u16DF", transliterator.transliterate("o"))
    }

    @Test
    fun `transliterate d to DAGAZ`() {
        assertEquals("\u16DE", transliterator.transliterate("d"))
    }

    // ==================== Digraph Mappings ====================

    @Test
    fun `transliterate th digraph to THURISAZ`() {
        val result = transliterator.transliterate("the")
        // "th" -> THURISAZ (U+16A6), "e" -> EHWAZ (U+16D6)
        assertEquals("\u16A6\u16D6", result)
    }

    @Test
    fun `transliterate ng digraph to INGWAZ`() {
        val result = transliterator.transliterate("king")
        // "k" -> KAUNA (U+16B2), "i" -> ISAZ (U+16C1), "ng" -> INGWAZ (U+16DC)
        assertEquals("\u16B2\u16C1\u16DC", result)
    }

    @Test
    fun `digraphs take precedence over individual characters`() {
        val result = transliterator.transliterate("thing")
        // "th" -> THURISAZ (U+16A6), "i" -> ISAZ (U+16C1), "ng" -> INGWAZ (U+16DC)
        assertEquals("\u16A6\u16C1\u16DC", result)
    }

    // ==================== Word and Phrase Tests ====================

    @Test
    fun `transliterate simple word - rune`() {
        val result = transliterator.transliterate("rune")
        // r -> U+16B1, u -> U+16A2, n -> U+16BE, e -> U+16D6
        assertEquals("\u16B1\u16A2\u16BE\u16D6", result)
    }

    @Test
    fun `transliterate word with digraph - strength`() {
        val result = transliterator.transliterate("strength")
        // s -> U+16CA, t -> U+16CF, r -> U+16B1, e -> U+16D6, "ng" -> U+16DC, "th" -> U+16A6
        assertEquals("\u16CA\u16CF\u16B1\u16D6\u16DC\u16A6", result)
    }

    @Test
    fun `transliterate phrase with spaces`() {
        val result = transliterator.transliterate("be brave")
        // b -> U+16D2, e -> U+16D6, " ", b -> U+16D2, r -> U+16B1, a -> U+16A8, v -> U+16A2, e -> U+16D6
        assertEquals("\u16D2\u16D6 \u16D2\u16B1\u16A8\u16A2\u16D6", result)
    }

    // ==================== Case Handling ====================

    @Test
    fun `uppercase letters are converted to lowercase then transliterated`() {
        val upper = transliterator.transliterate("RUNE")
        val lower = transliterator.transliterate("rune")
        assertEquals(lower, upper)
    }

    @Test
    fun `mixed case is normalized`() {
        val result = transliterator.transliterate("RuNe")
        assertEquals("\u16B1\u16A2\u16BE\u16D6", result)
    }

    // ==================== Punctuation Preservation ====================

    @Test
    fun `preserve spaces`() {
        val result = transliterator.transliterate("a b c")
        assertEquals("\u16A8 \u16D2 \u16B2", result)
    }

    @Test
    fun `preserve period`() {
        val result = transliterator.transliterate("end.")
        assertEquals("\u16D6\u16BE\u16DE.", result)
    }

    @Test
    fun `preserve comma`() {
        val result = transliterator.transliterate("one, two")
        assertEquals("\u16DF\u16BE\u16D6, \u16CF\u16B9\u16DF", result)
    }

    @Test
    fun `preserve exclamation mark`() {
        val result = transliterator.transliterate("stop!")
        assertEquals("\u16CA\u16CF\u16DF\u16C8!", result)
    }

    @Test
    fun `preserve question mark`() {
        val result = transliterator.transliterate("why?")
        assertEquals("\u16B9\u16BB\u16C3?", result)
    }

    @Test
    fun `preserve apostrophe`() {
        val result = transliterator.transliterate("don't")
        assertEquals("\u16DE\u16DF\u16BE'\u16CF", result)
    }

    @Test
    fun `preserve quotes`() {
        val result = transliterator.transliterate("\"word\"")
        assertEquals("\"\u16B9\u16DF\u16B1\u16DE\"", result)
    }

    @Test
    fun `preserve hyphen`() {
        val result = transliterator.transliterate("well-done")
        assertEquals("\u16B9\u16D6\u16DA\u16DA-\u16DE\u16DF\u16BE\u16D6", result)
    }

    @Test
    fun `preserve colon`() {
        val result = transliterator.transliterate("note: here")
        assertEquals("\u16BE\u16DF\u16CF\u16D6: \u16BB\u16D6\u16B1\u16D6", result)
    }

    @Test
    fun `preserve semicolon`() {
        val result = transliterator.transliterate("one; two")
        assertEquals("\u16DF\u16BE\u16D6; \u16CF\u16B9\u16DF", result)
    }

    // ==================== Edge Cases ====================

    @Test
    fun `empty string returns empty string`() {
        assertEquals("", transliterator.transliterate(""))
    }

    @Test
    fun `single space returns single space`() {
        assertEquals(" ", transliterator.transliterate(" "))
    }

    @Test
    fun `multiple spaces are preserved`() {
        assertEquals("   ", transliterator.transliterate("   "))
    }

    @Test
    fun `unmapped characters are preserved`() {
        // Numbers and special symbols not in the map
        val result = transliterator.transliterate("123@#$%")
        assertEquals("123@#$%", result)
    }

    @Test
    fun `mixed letters and numbers`() {
        val result = transliterator.transliterate("test123")
        // t -> U+16CF, e -> U+16D6, s -> U+16CA, t -> U+16CF, 1, 2, 3
        assertEquals("\u16CF\u16D6\u16CA\u16CF123", result)
    }

    @Test
    fun `unicode emoji characters are preserved`() {
        val result = transliterator.transliterate("rune 🔥")
        assertEquals("\u16B1\u16A2\u16BE\u16D6 🔥", result)
    }

    // ==================== Real-World Quote Tests ====================

    @Test
    fun `transliterate inspirational quote`() {
        val result = transliterator.transliterate("be brave")
        assertEquals("\u16D2\u16D6 \u16D2\u16B1\u16A8\u16A2\u16D6", result)
    }

    @Test
    fun `transliterate quote with punctuation`() {
        val result = transliterator.transliterate("know thyself.")
        // k -> U+16B2, n -> U+16BE, o -> U+16DF, w -> U+16B9, " ",
        // "th" -> U+16A6, y -> U+16C3, s -> U+16CA, e -> U+16D6, l -> U+16DA, f -> U+16A0, "."
        assertEquals("\u16B2\u16BE\u16DF\u16B9 \u16A6\u16C3\u16CA\u16D6\u16DA\u16A0.", result)
    }

    @Test
    fun `transliterate Norse-inspired phrase`() {
        val result = transliterator.transliterate("the king")
        // "th" -> U+16A6, e -> U+16D6, " ", k -> U+16B2, i -> U+16C1, "ng" -> U+16DC
        assertEquals("\u16A6\u16D6 \u16B2\u16C1\u16DC", result)
    }

    // ==================== Character Equivalence Tests ====================

    @Test
    fun `v and u map to same rune`() {
        val vResult = transliterator.transliterate("v")
        val uResult = transliterator.transliterate("u")
        assertEquals(uResult, vResult)
    }

    @Test
    fun `c and k map to same rune`() {
        val cResult = transliterator.transliterate("c")
        val kResult = transliterator.transliterate("k")
        assertEquals(kResult, cResult)
    }

    @Test
    fun `y and j map to same rune`() {
        val yResult = transliterator.transliterate("y")
        val jResult = transliterator.transliterate("j")
        assertEquals(jResult, yResult)
    }

    // ==================== Stress Tests ====================

    @Test
    fun `long text performance`() {
        val longText = "the quick brown fox jumps over the lazy dog ".repeat(100)
        val result = transliterator.transliterate(longText)
        assert(result.isNotEmpty())
        // Just verify it doesn't crash and produces output
    }

    @Test
    fun `all Latin alphabet characters`() {
        val alphabet = "abcdefghijklmnopqrstuvwxyz"
        val result = transliterator.transliterate(alphabet)
        assert(result.isNotEmpty())
        // Verify all characters are transliterated
        assert(result.none { it.isLowerCase() && it.isLetter() })
    }

    @Test
    fun `complete sentence with all features`() {
        val sentence = "The brave king's strength, wasn't it?"
        val result = transliterator.transliterate(sentence)
        // Verify it contains runes and punctuation
        assert(result.contains(","))
        assert(result.contains("'"))
        assert(result.contains("?"))
        assert(result.contains(" "))
    }
}
