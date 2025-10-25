package com.po4yka.runicquotes.domain.transliteration

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Comprehensive unit tests for CirthTransliterator.
 * Tests Tolkien's Angerthas (Cirth) script with PUA codepoints.
 *
 * Coverage goals: >95%
 */
class CirthTransliteratorTest {

    private lateinit var transliterator: CirthTransliterator

    @Before
    fun setUp() {
        transliterator = CirthTransliterator()
    }

    @Test
    fun `script name is Cirth (Angerthas)`() {
        assertEquals("Cirth (Angerthas)", transliterator.scriptName)
    }

    // ==================== Individual Cirth Mappings ====================

    @Test
    fun `transliterate p to Cirth 1`() {
        assertEquals("\uE080", transliterator.transliterate("p"))
    }

    @Test
    fun `transliterate b to Cirth 2`() {
        assertEquals("\uE081", transliterator.transliterate("b"))
    }

    @Test
    fun `transliterate f to Cirth 3`() {
        assertEquals("\uE082", transliterator.transliterate("f"))
    }

    @Test
    fun `transliterate v to Cirth 4`() {
        assertEquals("\uE083", transliterator.transliterate("v"))
    }

    @Test
    fun `transliterate t to Cirth 9`() {
        assertEquals("\uE088", transliterator.transliterate("t"))
    }

    @Test
    fun `transliterate d to Cirth 10`() {
        assertEquals("\uE089", transliterator.transliterate("d"))
    }

    @Test
    fun `transliterate þ to Cirth 11`() {
        assertEquals("\uE08A", transliterator.transliterate("þ"))
    }

    @Test
    fun `transliterate k to Cirth 17`() {
        assertEquals("\uE090", transliterator.transliterate("k"))
    }

    @Test
    fun `transliterate g to Cirth 18`() {
        assertEquals("\uE091", transliterator.transliterate("g"))
    }

    @Test
    fun `transliterate h to Cirth 19`() {
        assertEquals("\uE092", transliterator.transliterate("h"))
    }

    @Test
    fun `transliterate s to Cirth 29`() {
        assertEquals("\uE09C", transliterator.transliterate("s"))
    }

    @Test
    fun `transliterate z to Cirth 30`() {
        assertEquals("\uE09D", transliterator.transliterate("z"))
    }

    @Test
    fun `transliterate r to Cirth 33`() {
        assertEquals("\uE0A0", transliterator.transliterate("r"))
    }

    @Test
    fun `transliterate l to Cirth 41`() {
        assertEquals("\uE0A8", transliterator.transliterate("l"))
    }

    @Test
    fun `transliterate m to Cirth 49`() {
        assertEquals("\uE0B0", transliterator.transliterate("m"))
    }

    @Test
    fun `transliterate n to Cirth 53`() {
        assertEquals("\uE0B4", transliterator.transliterate("n"))
    }

    @Test
    fun `transliterate w to Cirth 57`() {
        assertEquals("\uE0B8", transliterator.transliterate("w"))
    }

    @Test
    fun `transliterate j to Cirth 61`() {
        assertEquals("\uE0BC", transliterator.transliterate("j"))
    }

    @Test
    fun `transliterate y to Cirth 62`() {
        assertEquals("\uE0BD", transliterator.transliterate("y"))
    }

    @Test
    fun `transliterate i to Cirth 73`() {
        assertEquals("\uE0C8", transliterator.transliterate("i"))
    }

    @Test
    fun `transliterate e to Cirth 74`() {
        assertEquals("\uE0C9", transliterator.transliterate("e"))
    }

    @Test
    fun `transliterate a to Cirth 75`() {
        assertEquals("\uE0CA", transliterator.transliterate("a"))
    }

    @Test
    fun `transliterate o to Cirth 76`() {
        assertEquals("\uE0CB", transliterator.transliterate("o"))
    }

    @Test
    fun `transliterate u to Cirth 77`() {
        assertEquals("\uE0CC", transliterator.transliterate("u"))
    }

    // ==================== Approximation Mappings ====================

    @Test
    fun `transliterate c to k (Cirth 17)`() {
        assertEquals("\uE090", transliterator.transliterate("c"))
    }

    @Test
    fun `transliterate q to k (Cirth 17)`() {
        assertEquals("\uE090", transliterator.transliterate("q"))
    }

    @Test
    fun `transliterate x to s approximation`() {
        assertEquals("\uE09C", transliterator.transliterate("x"))
    }

    // ==================== Digraph Tests ====================

    @Test
    fun `transliterate th digraph to Cirth 11`() {
        val result = transliterator.transliterate("the")
        // "th" -> \uE08A, "e" -> \uE0C9
        assertEquals("\uE08A\uE0C9", result)
    }

    @Test
    fun `transliterate ch digraph to Cirth 20`() {
        val result = transliterator.transliterate("chain")
        // "ch" -> \uE093, "a" -> \uE0CA, "i" -> \uE0C8, "n" -> \uE0B4
        assertEquals("\uE093\uE0CA\uE0C8\uE0B4", result)
    }

    @Test
    fun `transliterate sh digraph to Cirth 31`() {
        val result = transliterator.transliterate("ship")
        // "sh" -> \uE09E, "i" -> \uE0C8, "p" -> \uE080
        assertEquals("\uE09E\uE0C8\uE080", result)
    }

    @Test
    fun `transliterate ng digraph to Cirth 54`() {
        val result = transliterator.transliterate("ring")
        // "r" -> \uE0A0, "i" -> \uE0C8, "ng" -> \uE0B5
        assertEquals("\uE0A0\uE0C8\uE0B5", result)
    }

    @Test
    fun `digraphs have priority over individual chars`() {
        val result = transliterator.transliterate("ashing")
        // "a" -> \uE0CA, "sh" -> \uE09E, "i" -> \uE0C8, "ng" -> \uE0B5
        assertEquals("\uE0CA\uE09E\uE0C8\uE0B5", result)
    }

    // ==================== Middle-earth Words ====================

    @Test
    fun `transliterate moria`() {
        val result = transliterator.transliterate("moria")
        // m -> \uE0B0, o -> \uE0CB, r -> \uE0A0, i -> \uE0C8, a -> \uE0CA
        assertEquals("\uE0B0\uE0CB\uE0A0\uE0C8\uE0CA", result)
    }

    @Test
    fun `transliterate gandalf`() {
        val result = transliterator.transliterate("gandalf")
        // g -> \uE091, a -> \uE0CA, n -> \uE0B4, d -> \uE089, a -> \uE0CA, l -> \uE0A8, f -> \uE082
        assertEquals("\uE091\uE0CA\uE0B4\uE089\uE0CA\uE0A8\uE082", result)
    }

    @Test
    fun `transliterate erebor`() {
        val result = transliterator.transliterate("erebor")
        // e -> \uE0C9, r -> \uE0A0, e -> \uE0C9, b -> \uE081, o -> \uE0CB, r -> \uE0A0
        assertEquals("\uE0C9\uE0A0\uE0C9\uE081\uE0CB\uE0A0", result)
    }

    @Test
    fun `transliterate khazad-dum with hyphen`() {
        val result = transliterator.transliterate("khazad-dum")
        // k -> \uE090, h -> \uE092, a -> \uE0CA, z -> \uE09D, a -> \uE0CA, d -> \uE089,
        // "-", d -> \uE089, u -> \uE0CC, m -> \uE0B0
        assertEquals("\uE090\uE092\uE0CA\uE09D\uE0CA\uE089-\uE089\uE0CC\uE0B0", result)
    }

    // ==================== Word Tests ====================

    @Test
    fun `transliterate simple word - rune`() {
        val result = transliterator.transliterate("rune")
        // r -> \uE0A0, u -> \uE0CC, n -> \uE0B4, e -> \uE0C9
        assertEquals("\uE0A0\uE0CC\uE0B4\uE0C9", result)
    }

    @Test
    fun `transliterate word with multiple digraphs`() {
        val result = transliterator.transliterate("thing")
        // "th" -> \uE08A, "i" -> \uE0C8, "ng" -> \uE0B5
        assertEquals("\uE08A\uE0C8\uE0B5", result)
    }

    // ==================== Punctuation Preservation ====================

    @Test
    fun `preserve spaces`() {
        val result = transliterator.transliterate("one two")
        assert(result.contains(" "))
    }

    @Test
    fun `preserve periods`() {
        val result = transliterator.transliterate("end.")
        assert(result.endsWith("."))
    }

    @Test
    fun `preserve commas`() {
        val result = transliterator.transliterate("a, b")
        assert(result.contains(","))
    }

    @Test
    fun `preserve exclamation marks`() {
        val result = transliterator.transliterate("no!")
        assert(result.endsWith("!"))
    }

    @Test
    fun `preserve question marks`() {
        val result = transliterator.transliterate("why?")
        assert(result.endsWith("?"))
    }

    @Test
    fun `preserve apostrophes`() {
        val result = transliterator.transliterate("don't")
        assert(result.contains("'"))
    }

    @Test
    fun `preserve quotes`() {
        val result = transliterator.transliterate("\"word\"")
        assert(result.startsWith("\""))
        assert(result.endsWith("\""))
    }

    @Test
    fun `preserve hyphens`() {
        val result = transliterator.transliterate("half-elf")
        assert(result.contains("-"))
    }

    @Test
    fun `preserve colons`() {
        val result = transliterator.transliterate("note: here")
        assert(result.contains(":"))
    }

    @Test
    fun `preserve semicolons`() {
        val result = transliterator.transliterate("a; b")
        assert(result.contains(";"))
    }

    // ==================== Case Handling ====================

    @Test
    fun `uppercase converted to lowercase`() {
        val upper = transliterator.transliterate("RUNE")
        val lower = transliterator.transliterate("rune")
        assertEquals(lower, upper)
    }

    @Test
    fun `mixed case normalized`() {
        val result = transliterator.transliterate("MoRiA")
        val expected = transliterator.transliterate("moria")
        assertEquals(expected, result)
    }

    // ==================== Edge Cases ====================

    @Test
    fun `empty string returns empty`() {
        assertEquals("", transliterator.transliterate(""))
    }

    @Test
    fun `single space returns single space`() {
        assertEquals(" ", transliterator.transliterate(" "))
    }

    @Test
    fun `multiple spaces preserved`() {
        assertEquals("   ", transliterator.transliterate("   "))
    }

    @Test
    fun `unmapped characters pass through`() {
        val result = transliterator.transliterate("test123")
        assert(result.contains("1"))
        assert(result.contains("2"))
        assert(result.contains("3"))
    }

    @Test
    fun `numbers and letters mixed`() {
        val result = transliterator.transliterate("gate42")
        assert(result.contains("4"))
        assert(result.contains("2"))
    }

    // ==================== Character Equivalence ====================

    @Test
    fun `c and k and q map to same Cirth`() {
        val c = transliterator.transliterate("c")
        val k = transliterator.transliterate("k")
        val q = transliterator.transliterate("q")
        assertEquals(k, c)
        assertEquals(k, q)
    }

    // ==================== Complete Phrases ====================

    @Test
    fun `transliterate Tolkien quote`() {
        val result = transliterator.transliterate("not all who wander are lost")
        assert(result.isNotEmpty())
        assert(result.contains(" "))
    }

    @Test
    fun `transliterate with multiple punctuation types`() {
        val result = transliterator.transliterate("\"to be, or not?\"")
        assert(result.contains("\""))
        assert(result.contains(","))
        assert(result.contains("?"))
    }

    // ==================== Stress Tests ====================

    @Test
    fun `long text performance`() {
        val longText = "the path through moria ".repeat(100)
        val result = transliterator.transliterate(longText)
        assert(result.isNotEmpty())
    }

    @Test
    fun `complete alphabet`() {
        val alphabet = "abcdefghijklmnopqrstuvwxyz"
        val result = transliterator.transliterate(alphabet)
        assert(result.isNotEmpty())
        // All Latin letters should be transliterated to PUA
        assert(result.none { it.isLowerCase() && it.isLetter() })
    }

    @Test
    fun `all digraphs in one phrase`() {
        val result = transliterator.transliterate("the champion shines strongly")
        // Contains th, ch, sh, ng digraphs
        assert(result.isNotEmpty())
    }

    // ==================== PUA Codepoint Verification ====================

    @Test
    fun `verify PUA range for codepoints`() {
        val result = transliterator.transliterate("test")
        // All Cirth runes should be in PUA range (U+E000-U+F8FF)
        result.filter { it != ' ' && !it.isDigit() && it != '.' }
            .forEach { char ->
                val codepoint = char.code
                assert(codepoint in 0xE000..0xF8FF) {
                    "Character $char (U+${codepoint.toString(16)}) not in PUA range"
                }
            }
    }
}
