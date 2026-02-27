package com.po4yka.runicquotes.domain.transliteration

import com.google.common.truth.Truth.assertThat
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
        assertThat(transliterator.scriptName).isEqualTo("Cirth (Angerthas)")
    }

    // ==================== Individual Cirth Mappings ====================

    @Test
    fun `transliterate p to Cirth 1`() {
        assertThat(transliterator.transliterate("p")).isEqualTo("\uE080")
    }

    @Test
    fun `transliterate b to Cirth 2`() {
        assertThat(transliterator.transliterate("b")).isEqualTo("\uE081")
    }

    @Test
    fun `transliterate f to Cirth 3`() {
        assertThat(transliterator.transliterate("f")).isEqualTo("\uE082")
    }

    @Test
    fun `transliterate v to Cirth 4`() {
        assertThat(transliterator.transliterate("v")).isEqualTo("\uE083")
    }

    @Test
    fun `transliterate t to Cirth 9`() {
        assertThat(transliterator.transliterate("t")).isEqualTo("\uE088")
    }

    @Test
    fun `transliterate d to Cirth 10`() {
        assertThat(transliterator.transliterate("d")).isEqualTo("\uE089")
    }

    @Test
    fun `transliterate þ to Cirth 11`() {
        assertThat(transliterator.transliterate("þ")).isEqualTo("\uE08A")
    }

    @Test
    fun `transliterate k to Cirth 17`() {
        assertThat(transliterator.transliterate("k")).isEqualTo("\uE090")
    }

    @Test
    fun `transliterate g to Cirth 18`() {
        assertThat(transliterator.transliterate("g")).isEqualTo("\uE091")
    }

    @Test
    fun `transliterate h to Cirth 19`() {
        assertThat(transliterator.transliterate("h")).isEqualTo("\uE092")
    }

    @Test
    fun `transliterate s to Cirth 29`() {
        assertThat(transliterator.transliterate("s")).isEqualTo("\uE09C")
    }

    @Test
    fun `transliterate z to Cirth 30`() {
        assertThat(transliterator.transliterate("z")).isEqualTo("\uE09D")
    }

    @Test
    fun `transliterate r to Cirth 33`() {
        assertThat(transliterator.transliterate("r")).isEqualTo("\uE0A0")
    }

    @Test
    fun `transliterate l to Cirth 41`() {
        assertThat(transliterator.transliterate("l")).isEqualTo("\uE0A8")
    }

    @Test
    fun `transliterate m to Cirth 49`() {
        assertThat(transliterator.transliterate("m")).isEqualTo("\uE0B0")
    }

    @Test
    fun `transliterate n to Cirth 53`() {
        assertThat(transliterator.transliterate("n")).isEqualTo("\uE0B4")
    }

    @Test
    fun `transliterate w to Cirth 57`() {
        assertThat(transliterator.transliterate("w")).isEqualTo("\uE0B8")
    }

    @Test
    fun `transliterate j to Cirth 61`() {
        assertThat(transliterator.transliterate("j")).isEqualTo("\uE0BC")
    }

    @Test
    fun `transliterate y to Cirth 62`() {
        assertThat(transliterator.transliterate("y")).isEqualTo("\uE0BD")
    }

    @Test
    fun `transliterate i to Cirth 73`() {
        assertThat(transliterator.transliterate("i")).isEqualTo("\uE0C8")
    }

    @Test
    fun `transliterate e to Cirth 74`() {
        assertThat(transliterator.transliterate("e")).isEqualTo("\uE0C9")
    }

    @Test
    fun `transliterate a to Cirth 75`() {
        assertThat(transliterator.transliterate("a")).isEqualTo("\uE0CA")
    }

    @Test
    fun `transliterate o to Cirth 76`() {
        assertThat(transliterator.transliterate("o")).isEqualTo("\uE0CB")
    }

    @Test
    fun `transliterate u to Cirth 77`() {
        assertThat(transliterator.transliterate("u")).isEqualTo("\uE0CC")
    }

    // ==================== Approximation Mappings ====================

    @Test
    fun `transliterate c to k (Cirth 17)`() {
        assertThat(transliterator.transliterate("c")).isEqualTo("\uE090")
    }

    @Test
    fun `transliterate q to k (Cirth 17)`() {
        assertThat(transliterator.transliterate("q")).isEqualTo("\uE090")
    }

    @Test
    fun `transliterate x to s approximation`() {
        assertThat(transliterator.transliterate("x")).isEqualTo("\uE09C")
    }

    // ==================== Digraph Tests ====================

    @Test
    fun `transliterate th digraph to Cirth 11`() {
        val result = transliterator.transliterate("the")
        // "th" -> \uE08A, "e" -> \uE0C9
        assertThat(result).isEqualTo("\uE08A\uE0C9")
    }

    @Test
    fun `transliterate ch digraph to Cirth 20`() {
        val result = transliterator.transliterate("chain")
        // "ch" -> \uE093, "a" -> \uE0CA, "i" -> \uE0C8, "n" -> \uE0B4
        assertThat(result).isEqualTo("\uE093\uE0CA\uE0C8\uE0B4")
    }

    @Test
    fun `transliterate sh digraph to Cirth 31`() {
        val result = transliterator.transliterate("ship")
        // "sh" -> \uE09E, "i" -> \uE0C8, "p" -> \uE080
        assertThat(result).isEqualTo("\uE09E\uE0C8\uE080")
    }

    @Test
    fun `transliterate ng digraph to Cirth 54`() {
        val result = transliterator.transliterate("ring")
        // "r" -> \uE0A0, "i" -> \uE0C8, "ng" -> \uE0B5
        assertThat(result).isEqualTo("\uE0A0\uE0C8\uE0B5")
    }

    @Test
    fun `digraphs have priority over individual chars`() {
        val result = transliterator.transliterate("ashing")
        // "a" -> \uE0CA, "sh" -> \uE09E, "i" -> \uE0C8, "ng" -> \uE0B5
        assertThat(result).isEqualTo("\uE0CA\uE09E\uE0C8\uE0B5")
    }

    // ==================== Middle-earth Words ====================

    @Test
    fun `transliterate moria`() {
        val result = transliterator.transliterate("moria")
        // m -> \uE0B0, o -> \uE0CB, r -> \uE0A0, i -> \uE0C8, a -> \uE0CA
        assertThat(result).isEqualTo("\uE0B0\uE0CB\uE0A0\uE0C8\uE0CA")
    }

    @Test
    fun `transliterate gandalf`() {
        val result = transliterator.transliterate("gandalf")
        // g -> \uE091, a -> \uE0CA, n -> \uE0B4, d -> \uE089, a -> \uE0CA, l -> \uE0A8, f -> \uE082
        assertThat(result).isEqualTo("\uE091\uE0CA\uE0B4\uE089\uE0CA\uE0A8\uE082")
    }

    @Test
    fun `transliterate erebor`() {
        val result = transliterator.transliterate("erebor")
        // e -> \uE0C9, r -> \uE0A0, e -> \uE0C9, b -> \uE081, o -> \uE0CB, r -> \uE0A0
        assertThat(result).isEqualTo("\uE0C9\uE0A0\uE0C9\uE081\uE0CB\uE0A0")
    }

    @Test
    fun `transliterate khazad-dum with hyphen`() {
        val result = transliterator.transliterate("khazad-dum")
        // k -> \uE090, h -> \uE092, a -> \uE0CA, z -> \uE09D, a -> \uE0CA, d -> \uE089,
        // "-", d -> \uE089, u -> \uE0CC, m -> \uE0B0
        assertThat(result).isEqualTo("\uE090\uE092\uE0CA\uE09D\uE0CA\uE089-\uE089\uE0CC\uE0B0")
    }

    // ==================== Word Tests ====================

    @Test
    fun `transliterate simple word - rune`() {
        val result = transliterator.transliterate("rune")
        // r -> \uE0A0, u -> \uE0CC, n -> \uE0B4, e -> \uE0C9
        assertThat(result).isEqualTo("\uE0A0\uE0CC\uE0B4\uE0C9")
    }

    @Test
    fun `transliterate word with multiple digraphs`() {
        val result = transliterator.transliterate("thing")
        // "th" -> \uE08A, "i" -> \uE0C8, "ng" -> \uE0B5
        assertThat(result).isEqualTo("\uE08A\uE0C8\uE0B5")
    }

    // ==================== Punctuation Preservation ====================

    @Test
    fun `preserve spaces`() {
        val result = transliterator.transliterate("one two")
        assertThat(result).contains(" ")
    }

    @Test
    fun `preserve periods`() {
        val result = transliterator.transliterate("end.")
        assertThat(result).endsWith(".")
    }

    @Test
    fun `preserve commas`() {
        val result = transliterator.transliterate("a, b")
        assertThat(result).contains(",")
    }

    @Test
    fun `preserve exclamation marks`() {
        val result = transliterator.transliterate("no!")
        assertThat(result).endsWith("!")
    }

    @Test
    fun `preserve question marks`() {
        val result = transliterator.transliterate("why?")
        assertThat(result).endsWith("?")
    }

    @Test
    fun `preserve apostrophes`() {
        val result = transliterator.transliterate("don't")
        assertThat(result).contains("'")
    }

    @Test
    fun `preserve quotes`() {
        val result = transliterator.transliterate("\"word\"")
        assertThat(result).startsWith("\"")
        assertThat(result).endsWith("\"")
    }

    @Test
    fun `preserve hyphens`() {
        val result = transliterator.transliterate("half-elf")
        assertThat(result).contains("-")
    }

    @Test
    fun `preserve colons`() {
        val result = transliterator.transliterate("note: here")
        assertThat(result).contains(":")
    }

    @Test
    fun `preserve semicolons`() {
        val result = transliterator.transliterate("a; b")
        assertThat(result).contains(";")
    }

    // ==================== Case Handling ====================

    @Test
    fun `uppercase converted to lowercase`() {
        val upper = transliterator.transliterate("RUNE")
        val lower = transliterator.transliterate("rune")
        assertThat(upper).isEqualTo(lower)
    }

    @Test
    fun `mixed case normalized`() {
        val result = transliterator.transliterate("MoRiA")
        val expected = transliterator.transliterate("moria")
        assertThat(result).isEqualTo(expected)
    }

    // ==================== Edge Cases ====================

    @Test
    fun `empty string returns empty`() {
        assertThat(transliterator.transliterate("")).isEqualTo("")
    }

    @Test
    fun `single space returns single space`() {
        assertThat(transliterator.transliterate(" ")).isEqualTo(" ")
    }

    @Test
    fun `multiple spaces preserved`() {
        assertThat(transliterator.transliterate("   ")).isEqualTo("   ")
    }

    @Test
    fun `unmapped characters pass through`() {
        val result = transliterator.transliterate("test123")
        assertThat(result).contains("1")
        assertThat(result).contains("2")
        assertThat(result).contains("3")
    }

    @Test
    fun `numbers and letters mixed`() {
        val result = transliterator.transliterate("gate42")
        assertThat(result).contains("4")
        assertThat(result).contains("2")
    }

    // ==================== Character Equivalence ====================

    @Test
    fun `c and k and q map to same Cirth`() {
        val c = transliterator.transliterate("c")
        val k = transliterator.transliterate("k")
        val q = transliterator.transliterate("q")
        assertThat(c).isEqualTo(k)
        assertThat(q).isEqualTo(k)
    }

    // ==================== Complete Phrases ====================

    @Test
    fun `transliterate Tolkien quote`() {
        val result = transliterator.transliterate("not all who wander are lost")
        assertThat(result).isNotEmpty()
        assertThat(result).contains(" ")
    }

    @Test
    fun `transliterate with multiple punctuation types`() {
        val result = transliterator.transliterate("\"to be, or not?\"")
        assertThat(result).contains("\"")
        assertThat(result).contains(",")
        assertThat(result).contains("?")
    }

    // ==================== Stress Tests ====================

    @Test
    fun `long text performance`() {
        val longText = "the path through moria ".repeat(100)
        val result = transliterator.transliterate(longText)
        assertThat(result).isNotEmpty()
    }

    @Test
    fun `complete alphabet`() {
        val alphabet = "abcdefghijklmnopqrstuvwxyz"
        val result = transliterator.transliterate(alphabet)
        assertThat(result).isNotEmpty()
        // All Latin letters should be transliterated to PUA
        assertThat(result.none { it.isLowerCase() && it.isLetter() }).isTrue()
    }

    @Test
    fun `all digraphs in one phrase`() {
        val result = transliterator.transliterate("the champion shines strongly")
        // Contains th, ch, sh, ng digraphs
        assertThat(result).isNotEmpty()
    }

    // ==================== PUA Codepoint Verification ====================

    @Test
    fun `verify PUA range for codepoints`() {
        val result = transliterator.transliterate("test")
        // All Cirth runes should be in PUA range (U+E000-U+F8FF)
        result.filter { it != ' ' && !it.isDigit() && it != '.' }
            .forEach { char ->
                val codepoint = char.code
                assertThat(codepoint in 0xE000..0xF8FF).isTrue()
            }
    }
}
