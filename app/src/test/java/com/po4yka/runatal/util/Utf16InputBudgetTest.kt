package com.po4yka.runatal.util

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class Utf16InputBudgetTest {
    @Test
    fun `paired supplementary character at a cut is excluded whole while an exact fit is retained`() {
        assertThat(("a".repeat(279) + "😀tail").takeUtf16Budget(280)).isEqualTo("a".repeat(279))
        assertThat(("a".repeat(278) + "😀tail").takeUtf16Budget(280)).isEqualTo("a".repeat(278) + "😀")
        assertThat(("a".repeat(59) + "𐐷tail").takeUtf16Budget(60)).isEqualTo("a".repeat(59))
        assertThat(("a".repeat(58) + "𐐷tail").takeUtf16Budget(60)).isEqualTo("a".repeat(58) + "𐐷")
    }

    @Test
    fun `bmp and ordinary budgets retain the exact prefix without removing valid characters`() {
        assertThat(("a".repeat(279) + "ᚠtail").takeUtf16Budget(280)).isEqualTo("a".repeat(279) + "ᚠ")
        assertThat("abc".takeUtf16Budget(2)).isEqualTo("ab")
        assertThat("😀".takeUtf16Budget(1)).isEmpty()
        assertThat("😀".takeUtf16Budget(2)).isEqualTo("😀")
        assertThat("😀".takeUtf16Budget(0)).isEmpty()
        assertThat("".takeUtf16Budget(0)).isEmpty()
        assertThat("short".takeUtf16Budget(280)).isEqualTo("short")
    }

    @Test
    fun `negative budget is invalid and adjacent supplementary characters do not leak a high surrogate`() {
        assertThrows(IllegalArgumentException::class.java) { "text".takeUtf16Budget(-1) }
        val result = "😀😀tail".takeUtf16Budget(3)
        assertThat(result).isEqualTo("😀")
        assertThat(Character.isHighSurrogate(result.last())).isFalse()
    }
}
