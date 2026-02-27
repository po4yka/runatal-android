package com.po4yka.runicquotes.domain.transliteration

import com.google.common.truth.Truth.assertThat
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
        assertThat(transliterator.scriptName).isEqualTo("Younger Futhark")
    }

    // ==================== Core Rune Mappings ====================

    @Test
    fun `transliterate f to FE`() {
        assertThat(transliterator.transliterate("f")).isEqualTo("\u16A0")
    }

    @Test
    fun `transliterate u to UR`() {
        assertThat(transliterator.transliterate("u")).isEqualTo("\u16A2")
    }

    @Test
    fun `transliterate v to UR`() {
        assertThat(transliterator.transliterate("v")).isEqualTo("\u16A2")
    }

    @Test
    fun `transliterate þ to THURS`() {
        assertThat(transliterator.transliterate("þ")).isEqualTo("\u16A6")
    }

    @Test
    fun `transliterate a to AS`() {
        assertThat(transliterator.transliterate("a")).isEqualTo("\u16A8")
    }

    @Test
    fun `transliterate r to REID`() {
        assertThat(transliterator.transliterate("r")).isEqualTo("\u16B1")
    }

    @Test
    fun `transliterate k to KAUN`() {
        assertThat(transliterator.transliterate("k")).isEqualTo("\u16B2")
    }

    @Test
    fun `transliterate c to KAUN`() {
        assertThat(transliterator.transliterate("c")).isEqualTo("\u16B2")
    }

    @Test
    fun `transliterate g to KAUN`() {
        // Younger Futhark merged g and k
        assertThat(transliterator.transliterate("g")).isEqualTo("\u16B2")
    }

    @Test
    fun `transliterate h to HAGALL`() {
        assertThat(transliterator.transliterate("h")).isEqualTo("\u16BB")
    }

    @Test
    fun `transliterate n to NAUD`() {
        assertThat(transliterator.transliterate("n")).isEqualTo("\u16BE")
    }

    @Test
    fun `transliterate i to IS`() {
        assertThat(transliterator.transliterate("i")).isEqualTo("\u16C1")
    }

    @Test
    fun `transliterate j to IS`() {
        // Younger Futhark merged j and i
        assertThat(transliterator.transliterate("j")).isEqualTo("\u16C1")
    }

    @Test
    fun `transliterate y to IS`() {
        // Younger Futhark merged y and i
        assertThat(transliterator.transliterate("y")).isEqualTo("\u16C1")
    }

    @Test
    fun `transliterate s to SOL`() {
        assertThat(transliterator.transliterate("s")).isEqualTo("\u16CA")
    }

    @Test
    fun `transliterate z to SOL`() {
        // z approximated as s
        assertThat(transliterator.transliterate("z")).isEqualTo("\u16CA")
    }

    @Test
    fun `transliterate t to TYR`() {
        assertThat(transliterator.transliterate("t")).isEqualTo("\u16CF")
    }

    @Test
    fun `transliterate b to BJARKAN`() {
        assertThat(transliterator.transliterate("b")).isEqualTo("\u16D2")
    }

    @Test
    fun `transliterate m to MADR`() {
        assertThat(transliterator.transliterate("m")).isEqualTo("\u16D7")
    }

    @Test
    fun `transliterate l to LOGR`() {
        assertThat(transliterator.transliterate("l")).isEqualTo("\u16DA")
    }

    @Test
    fun `transliterate d to THURS (approximation)`() {
        // d approximated as th in Younger Futhark
        assertThat(transliterator.transliterate("d")).isEqualTo("\u16A6")
    }

    @Test
    fun `transliterate w to UR`() {
        // w merged with u
        assertThat(transliterator.transliterate("w")).isEqualTo("\u16A2")
    }

    // ==================== Digraph Tests ====================

    @Test
    fun `transliterate th digraph to THURS`() {
        val result = transliterator.transliterate("the")
        assertThat(result).isEqualTo("\u16A6\u16D6")
    }

    @Test
    fun `transliterate ng digraph to NAUD`() {
        val result = transliterator.transliterate("king")
        // k -> KAUN, i -> IS, ng -> NAUD
        assertThat(result).isEqualTo("\u16B2\u16C1\u16BE")
    }

    // ==================== Character Merging Tests ====================

    @Test
    fun `g k and c all map to KAUN`() {
        val g = transliterator.transliterate("g")
        val k = transliterator.transliterate("k")
        val c = transliterator.transliterate("c")
        assertThat(g).isEqualTo(k)
        assertThat(c).isEqualTo(k)
    }

    @Test
    fun `i j and y all map to IS`() {
        val i = transliterator.transliterate("i")
        val j = transliterator.transliterate("j")
        val y = transliterator.transliterate("y")
        assertThat(j).isEqualTo(i)
        assertThat(y).isEqualTo(i)
    }

    @Test
    fun `u v and w all map to UR`() {
        val u = transliterator.transliterate("u")
        val v = transliterator.transliterate("v")
        val w = transliterator.transliterate("w")
        assertThat(v).isEqualTo(u)
        assertThat(w).isEqualTo(u)
    }

    @Test
    fun `s and z both map to SOL`() {
        val s = transliterator.transliterate("s")
        val z = transliterator.transliterate("z")
        assertThat(z).isEqualTo(s)
    }

    // ==================== Word Tests ====================

    @Test
    fun `transliterate viking`() {
        val result = transliterator.transliterate("viking")
        // v -> UR, i -> IS, k -> KAUN, i -> IS, ng -> NAUD
        assertThat(result).isEqualTo("\u16A2\u16C1\u16B2\u16C1\u16BE")
    }

    @Test
    fun `transliterate rune`() {
        val result = transliterator.transliterate("rune")
        // r -> REID, u -> UR, n -> NAUD, e -> approximation
        assertThat(result).isEqualTo("\u16B1\u16A2\u16BE\u16D6")
    }

    @Test
    fun `transliterate strength with digraph`() {
        val result = transliterator.transliterate("strength")
        // s -> SOL, t -> TYR, r -> REID, e -> approx, ng -> NAUD, th -> THURS
        assertThat(result).isEqualTo("\u16CA\u16CF\u16B1\u16D6\u16BE\u16A6")
    }

    // ==================== Punctuation Tests ====================

    @Test
    fun `preserve all punctuation`() {
        val result = transliterator.transliterate("hello, world!")
        assertThat(result).contains(",")
        assertThat(result).contains(" ")
        assertThat(result).contains("!")
    }

    @Test
    fun `preserve apostrophe in contractions`() {
        val result = transliterator.transliterate("can't")
        assertThat(result).contains("'")
    }

    // ==================== Case Handling ====================

    @Test
    fun `uppercase is converted to lowercase`() {
        val upper = transliterator.transliterate("RUNE")
        val lower = transliterator.transliterate("rune")
        assertThat(upper).isEqualTo(lower)
    }

    // ==================== Edge Cases ====================

    @Test
    fun `empty string returns empty`() {
        assertThat(transliterator.transliterate("")).isEqualTo("")
    }

    @Test
    fun `spaces are preserved`() {
        assertThat(transliterator.transliterate("   ")).isEqualTo("   ")
    }

    @Test
    fun `unmapped characters pass through`() {
        val result = transliterator.transliterate("test123")
        assertThat(result).contains("1")
        assertThat(result).contains("2")
        assertThat(result).contains("3")
    }

    // ==================== Real-World Viking Phrases ====================

    @Test
    fun `transliterate Viking greeting`() {
        val result = transliterator.transliterate("hail")
        // h -> HAGALL, a -> AS, i -> IS, l -> LOGR
        assertThat(result).isEqualTo("\u16BB\u16A8\u16C1\u16DA")
    }

    @Test
    fun `transliterate Thor`() {
        val result = transliterator.transliterate("thor")
        // "th" -> THURS, o -> approx, r -> REID
        assertThat(result).isEqualTo("\u16A6\u16DF\u16B1")
    }

    @Test
    fun `transliterate Odin`() {
        val result = transliterator.transliterate("odin")
        // o -> approx, d -> THURS, i -> IS, n -> NAUD
        assertThat(result).isEqualTo("\u16DF\u16A6\u16C1\u16BE")
    }

    // ==================== Comparison with Elder Futhark ====================

    @Test
    fun `verify simplified alphabet - fewer distinct runes`() {
        // Younger Futhark has 16 runes vs Elder Futhark's 24
        // Test that multiple letters map to same rune (merging)

        // g and k merged
        assertThat(transliterator.transliterate("g"))
            .isEqualTo(transliterator.transliterate("k"))

        // i, j, y merged
        assertThat(transliterator.transliterate("j"))
            .isEqualTo(transliterator.transliterate("i"))
    }

    // ==================== Stress Tests ====================

    @Test
    fun `long text performance`() {
        val longText = "the viking saga of the north ".repeat(100)
        val result = transliterator.transliterate(longText)
        assertThat(result).isNotEmpty()
    }

    @Test
    fun `complete alphabet`() {
        val alphabet = "abcdefghijklmnopqrstuvwxyz"
        val result = transliterator.transliterate(alphabet)
        assertThat(result).isNotEmpty()
        // All Latin letters should be transliterated
        assertThat(result.none { it.isLowerCase() && it.isLetter() }).isTrue()
    }
}
