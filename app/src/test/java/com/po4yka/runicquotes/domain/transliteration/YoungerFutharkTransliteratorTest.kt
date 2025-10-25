package com.po4yka.runicquotes.domain.transliteration

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Comprehensive unit tests for YoungerFutharkTransliterator.
 * Tests the 16-rune simplified Viking Age runic alphabet.
 *
 * Coverage goals: >95%
 */
class YoungerFutharkTransliteratorTest {

    private lateinit var transliterator: YoungerFutharkTransliterator

    @Before
    fun setUp() {
        transliterator = YoungerFutharkTransliterator()
    }

    @Test
    fun `script name is Younger Futhark`() {
        assertEquals("Younger Futhark", transliterator.scriptName)
    }

    // ==================== Core Rune Mappings ====================

    @Test
    fun `transliterate f to FE`() {
        assertEquals("\u16A0", transliterator.transliterate("f"))
    }

    @Test
    fun `transliterate u to UR`() {
        assertEquals("\u16A2", transliterator.transliterate("u"))
    }

    @Test
    fun `transliterate v to UR`() {
        assertEquals("\u16A2", transliterator.transliterate("v"))
    }

    @Test
    fun `transliterate þ to THURS`() {
        assertEquals("\u16A6", transliterator.transliterate("þ"))
    }

    @Test
    fun `transliterate a to AS`() {
        assertEquals("\u16A8", transliterator.transliterate("a"))
    }

    @Test
    fun `transliterate r to REID`() {
        assertEquals("\u16B1", transliterator.transliterate("r"))
    }

    @Test
    fun `transliterate k to KAUN`() {
        assertEquals("\u16B2", transliterator.transliterate("k"))
    }

    @Test
    fun `transliterate c to KAUN`() {
        assertEquals("\u16B2", transliterator.transliterate("c"))
    }

    @Test
    fun `transliterate g to KAUN`() {
        // Younger Futhark merged g and k
        assertEquals("\u16B2", transliterator.transliterate("g"))
    }

    @Test
    fun `transliterate h to HAGALL`() {
        assertEquals("\u16BB", transliterator.transliterate("h"))
    }

    @Test
    fun `transliterate n to NAUD`() {
        assertEquals("\u16BE", transliterator.transliterate("n"))
    }

    @Test
    fun `transliterate i to IS`() {
        assertEquals("\u16C1", transliterator.transliterate("i"))
    }

    @Test
    fun `transliterate j to IS`() {
        // Younger Futhark merged j and i
        assertEquals("\u16C1", transliterator.transliterate("j"))
    }

    @Test
    fun `transliterate y to IS`() {
        // Younger Futhark merged y and i
        assertEquals("\u16C1", transliterator.transliterate("y"))
    }

    @Test
    fun `transliterate s to SOL`() {
        assertEquals("\u16CA", transliterator.transliterate("s"))
    }

    @Test
    fun `transliterate z to SOL`() {
        // z approximated as s
        assertEquals("\u16CA", transliterator.transliterate("z"))
    }

    @Test
    fun `transliterate t to TYR`() {
        assertEquals("\u16CF", transliterator.transliterate("t"))
    }

    @Test
    fun `transliterate b to BJARKAN`() {
        assertEquals("\u16D2", transliterator.transliterate("b"))
    }

    @Test
    fun `transliterate m to MADR`() {
        assertEquals("\u16D7", transliterator.transliterate("m"))
    }

    @Test
    fun `transliterate l to LOGR`() {
        assertEquals("\u16DA", transliterator.transliterate("l"))
    }

    @Test
    fun `transliterate d to THURS (approximation)`() {
        // d approximated as th in Younger Futhark
        assertEquals("\u16A6", transliterator.transliterate("d"))
    }

    @Test
    fun `transliterate w to UR`() {
        // w merged with u
        assertEquals("\u16A2", transliterator.transliterate("w"))
    }

    // ==================== Digraph Tests ====================

    @Test
    fun `transliterate th digraph to THURS`() {
        val result = transliterator.transliterate("the")
        assertEquals("\u16A6\u16D6", result)
    }

    @Test
    fun `transliterate ng digraph to NAUD`() {
        val result = transliterator.transliterate("king")
        // k -> KAUN, i -> IS, ng -> NAUD
        assertEquals("\u16B2\u16C1\u16BE", result)
    }

    // ==================== Character Merging Tests ====================

    @Test
    fun `g k and c all map to KAUN`() {
        val g = transliterator.transliterate("g")
        val k = transliterator.transliterate("k")
        val c = transliterator.transliterate("c")
        assertEquals(k, g)
        assertEquals(k, c)
    }

    @Test
    fun `i j and y all map to IS`() {
        val i = transliterator.transliterate("i")
        val j = transliterator.transliterate("j")
        val y = transliterator.transliterate("y")
        assertEquals(i, j)
        assertEquals(i, y)
    }

    @Test
    fun `u v and w all map to UR`() {
        val u = transliterator.transliterate("u")
        val v = transliterator.transliterate("v")
        val w = transliterator.transliterate("w")
        assertEquals(u, v)
        assertEquals(u, w)
    }

    @Test
    fun `s and z both map to SOL`() {
        val s = transliterator.transliterate("s")
        val z = transliterator.transliterate("z")
        assertEquals(s, z)
    }

    // ==================== Word Tests ====================

    @Test
    fun `transliterate viking`() {
        val result = transliterator.transliterate("viking")
        // v -> UR, i -> IS, k -> KAUN, i -> IS, ng -> NAUD
        assertEquals("\u16A2\u16C1\u16B2\u16C1\u16BE", result)
    }

    @Test
    fun `transliterate rune`() {
        val result = transliterator.transliterate("rune")
        // r -> REID, u -> UR, n -> NAUD, e -> approximation
        assertEquals("\u16B1\u16A2\u16BE\u16D6", result)
    }

    @Test
    fun `transliterate strength with digraph`() {
        val result = transliterator.transliterate("strength")
        // s -> SOL, t -> TYR, r -> REID, e -> approx, ng -> NAUD, th -> THURS
        assertEquals("\u16CA\u16CF\u16B1\u16D6\u16BE\u16A6", result)
    }

    // ==================== Punctuation Tests ====================

    @Test
    fun `preserve all punctuation`() {
        val result = transliterator.transliterate("hello, world!")
        assert(result.contains(","))
        assert(result.contains(" "))
        assert(result.contains("!"))
    }

    @Test
    fun `preserve apostrophe in contractions`() {
        val result = transliterator.transliterate("can't")
        assert(result.contains("'"))
    }

    // ==================== Case Handling ====================

    @Test
    fun `uppercase is converted to lowercase`() {
        val upper = transliterator.transliterate("RUNE")
        val lower = transliterator.transliterate("rune")
        assertEquals(lower, upper)
    }

    // ==================== Edge Cases ====================

    @Test
    fun `empty string returns empty`() {
        assertEquals("", transliterator.transliterate(""))
    }

    @Test
    fun `spaces are preserved`() {
        assertEquals("   ", transliterator.transliterate("   "))
    }

    @Test
    fun `unmapped characters pass through`() {
        val result = transliterator.transliterate("test123")
        assert(result.contains("1"))
        assert(result.contains("2"))
        assert(result.contains("3"))
    }

    // ==================== Real-World Viking Phrases ====================

    @Test
    fun `transliterate Viking greeting`() {
        val result = transliterator.transliterate("hail")
        // h -> HAGALL, a -> AS, i -> IS, l -> LOGR
        assertEquals("\u16BB\u16A8\u16C1\u16DA", result)
    }

    @Test
    fun `transliterate Thor`() {
        val result = transliterator.transliterate("thor")
        // "th" -> THURS, o -> approx, r -> REID
        assertEquals("\u16A6\u16DF\u16B1", result)
    }

    @Test
    fun `transliterate Odin`() {
        val result = transliterator.transliterate("odin")
        // o -> approx, d -> THURS, i -> IS, n -> NAUD
        assertEquals("\u16DF\u16A6\u16C1\u16BE", result)
    }

    // ==================== Comparison with Elder Futhark ====================

    @Test
    fun `verify simplified alphabet - fewer distinct runes`() {
        // Younger Futhark has 16 runes vs Elder Futhark's 24
        // Test that multiple letters map to same rune (merging)

        // g and k merged
        assertEquals(
            transliterator.transliterate("k"),
            transliterator.transliterate("g")
        )

        // i, j, y merged
        assertEquals(
            transliterator.transliterate("i"),
            transliterator.transliterate("j")
        )
    }

    // ==================== Stress Tests ====================

    @Test
    fun `long text performance`() {
        val longText = "the viking saga of the north ".repeat(100)
        val result = transliterator.transliterate(longText)
        assert(result.isNotEmpty())
    }

    @Test
    fun `complete alphabet`() {
        val alphabet = "abcdefghijklmnopqrstuvwxyz"
        val result = transliterator.transliterate(alphabet)
        assert(result.isNotEmpty())
        // All Latin letters should be transliterated
        assert(result.none { it.isLowerCase() && it.isLetter() })
    }
}
