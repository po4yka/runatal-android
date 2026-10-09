package com.po4yka.runatal.domain.transliteration

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
    fun `transliterate t to Cirth T`() {
        assertThat(transliterator.transliterate("t")).isEqualTo("\uE087")
    }

    @Test
    fun `transliterate d to Cirth D`() {
        assertThat(transliterator.transliterate("d")).isEqualTo("\uE088")
    }

    @Test
    fun `transliterate þ to Cirth TH`() {
        assertThat(transliterator.transliterate("þ")).isEqualTo("\uE089")
    }

    @Test
    fun `transliterate k to Cirth K`() {
        assertThat(transliterator.transliterate("k")).isEqualTo("\uE091")
    }

    @Test
    fun `transliterate g to Cirth G`() {
        assertThat(transliterator.transliterate("g")).isEqualTo("\uE092")
    }

    @Test
    fun `transliterate h to Cirth H`() {
        assertThat(transliterator.transliterate("h")).isEqualTo("\uE0B9")
    }

    @Test
    fun `transliterate s to Cirth S`() {
        assertThat(transliterator.transliterate("s")).isEqualTo("\uE0A1")
    }

    @Test
    fun `transliterate z to Cirth Z`() {
        assertThat(transliterator.transliterate("z")).isEqualTo("\uE0A3")
    }

    @Test
    fun `transliterate r to Cirth R`() {
        assertThat(transliterator.transliterate("r")).isEqualTo("\uE09C")
    }

    @Test
    fun `transliterate l to Cirth L`() {
        assertThat(transliterator.transliterate("l")).isEqualTo("\uE09E")
    }

    @Test
    fun `transliterate m to Cirth M`() {
        assertThat(transliterator.transliterate("m")).isEqualTo("\uE085")
    }

    @Test
    fun `transliterate n to Cirth N`() {
        assertThat(transliterator.transliterate("n")).isEqualTo("\uE08B")
    }

    @Test
    fun `transliterate w to Cirth W`() {
        assertThat(transliterator.transliterate("w")).isEqualTo("\uE0AC")
    }

    @Test
    fun `transliterate j to Cirth J`() {
        assertThat(transliterator.transliterate("j")).isEqualTo("\uE08D")
    }

    @Test
    fun `transliterate y to Cirth Y`() {
        assertThat(transliterator.transliterate("y")).isEqualTo("\uE0E1")
    }

    @Test
    fun `transliterate i to Cirth I`() {
        assertThat(transliterator.transliterate("i")).isEqualTo("\uE0A7")
    }

    @Test
    fun `transliterate e to Cirth E`() {
        assertThat(transliterator.transliterate("e")).isEqualTo("\uE0AF")
    }

    @Test
    fun `transliterate a to Cirth A`() {
        assertThat(transliterator.transliterate("a")).isEqualTo("\uE0B1")
    }

    @Test
    fun `transliterate o to Cirth O`() {
        assertThat(transliterator.transliterate("o")).isEqualTo("\uE0B3")
    }

    @Test
    fun `transliterate u to Cirth U`() {
        assertThat(transliterator.transliterate("u")).isEqualTo("\uE0AA")
    }

    // ==================== Approximation Mappings ====================

    @Test
    fun `transliterate c to k (Cirth K)`() {
        assertThat(transliterator.transliterate("c")).isEqualTo("\uE091")
    }

    @Test
    fun `transliterate q to kw (Cirth KW)`() {
        assertThat(transliterator.transliterate("q")).isEqualTo("\uE096")
    }

    @Test
    fun `transliterate x preserves k and s`() {
        assertThat(transliterator.transliterate("x")).isEqualTo("\uE091\uE0A1")
    }

    // ==================== Digraph Tests ====================

    @Test
    fun `transliterate th digraph to Cirth TH`() {
        val result = transliterator.transliterate("the")
        // "th" -> \uE089, "e" -> \uE0AF
        assertThat(result).isEqualTo("\uE089\uE0AF")
    }

    @Test
    fun `transliterate ch digraph to Cirth CH`() {
        val result = transliterator.transliterate("chain")
        // "ch" -> \uE08C, "a" -> \uE0B1, "i" -> \uE0A7, "n" -> \uE08B
        assertThat(result).isEqualTo("\uE08C\uE0B1\uE0A7\uE08B")
    }

    @Test
    fun `transliterate sh digraph to Cirth SH`() {
        val result = transliterator.transliterate("ship")
        // "sh" -> \uE08E, "i" -> \uE0A7, "p" -> \uE080
        assertThat(result).isEqualTo("\uE08E\uE0A7\uE080")
    }

    @Test
    fun `transliterate ng digraph to Cirth ENG`() {
        val result = transliterator.transliterate("ring")
        // "r" -> \uE09C, "i" -> \uE0A7, "ng" -> \uE095
        assertThat(result).isEqualTo("\uE09C\uE0A7\uE095")
    }

    @Test
    fun `digraphs have priority over individual chars`() {
        val result = transliterator.transliterate("ashing")
        // "a" -> \uE0B1, "sh" -> \uE08E, "i" -> \uE0A7, "ng" -> \uE095
        assertThat(result).isEqualTo("\uE0B1\uE08E\uE0A7\uE095")
    }

    // ==================== Middle-earth Words ====================

    @Test
    fun `transliterate moria`() {
        val result = transliterator.transliterate("moria")
        // m -> \uE085, o -> \uE0B3, r -> \uE09C, i -> \uE0A7, a -> \uE0B1
        assertThat(result).isEqualTo("\uE085\uE0B3\uE09C\uE0A7\uE0B1")
    }

    @Test
    fun `transliterate gandalf`() {
        val result = transliterator.transliterate("gandalf")
        // g -> \uE092, a -> \uE0B1, n -> \uE08B, d -> \uE088, a -> \uE0B1, l -> \uE09E, f -> \uE082
        assertThat(result).isEqualTo("\uE092\uE0B1\uE08B\uE088\uE0B1\uE09E\uE082")
    }

    @Test
    fun `transliterate erebor`() {
        val result = transliterator.transliterate("erebor")
        // e -> \uE0AF, r -> \uE09C, e -> \uE0AF, b -> \uE081, o -> \uE0B3, r -> \uE09C
        assertThat(result).isEqualTo("\uE0AF\uE09C\uE0AF\uE081\uE0B3\uE09C")
    }

    @Test
    fun `transliterate khazad-dum with hyphen`() {
        val result = transliterator.transliterate("khazad-dum")
        // k -> \uE091, h -> \uE0B9, a -> \uE0B1, z -> \uE0A3, a -> \uE0B1, d -> \uE088,
        // "-", d -> \uE088, u -> \uE0AA, m -> \uE085
        assertThat(result).isEqualTo("\uE093\uE0B1\uE0A3\uE0B1\uE088-\uE088\uE0AA\uE085")
    }

    // ==================== Word Tests ====================

    @Test
    fun `transliterate simple word - rune`() {
        val result = transliterator.transliterate("rune")
        // r -> \uE09C, u -> \uE0AA, n -> \uE08B, e -> \uE0AF
        assertThat(result).isEqualTo("\uE09C\uE0AA\uE08B\uE0AF")
    }

    @Test
    fun `transliterate word with multiple digraphs`() {
        val result = transliterator.transliterate("thing")
        // "th" -> \uE089, "i" -> \uE0A7, "ng" -> \uE095
        assertThat(result).isEqualTo("\uE089\uE0A7\uE095")
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
    fun `c and k share K while q represents KW`() {
        val c = transliterator.transliterate("c")
        val k = transliterator.transliterate("k")
        val q = transliterator.transliterate("q")
        assertThat(c).isEqualTo(k)
        assertThat(q).isEqualTo("\uE096")
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
