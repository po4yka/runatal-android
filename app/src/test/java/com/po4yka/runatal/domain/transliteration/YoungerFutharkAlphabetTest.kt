package com.po4yka.runatal.domain.transliteration

import com.google.common.truth.Truth.assertThat
import com.po4yka.runatal.data.seed.QuoteSeedData
import com.po4yka.runatal.data.seed.RuneReferenceSeedData
import com.po4yka.runatal.domain.translation.YoungerFutharkRenderer
import com.po4yka.runatal.domain.translation.YoungerFutharkVariant
import org.junit.Test

class YoungerFutharkAlphabetTest {

    // Unicode chart oracle: https://www.unicode.org/charts/PDF/U16A0.pdf, in conventional futhark order.
    private val latinSigns = "fuþąrkhniastbmlʀ"
    private val longBranch = "ᚠᚢᚦᚬᚱᚴᚼᚾᛁᛅᛋᛏᛒᛘᛚᛦ"
    private val shortTwig = "ᚠᚢᚦᚭᚱᚴᚽᚿᛁᛆᛌᛐᛓᛙᛚᛧ"

    @Test
    fun `historical renderer uses exactly sixteen branch-specific rune signs`() {
        val renderer = YoungerFutharkRenderer()
        assertThat(renderer.render(latinSigns, YoungerFutharkVariant.LONG_BRANCH)).isEqualTo(longBranch)
        assertThat(renderer.render(latinSigns, YoungerFutharkVariant.SHORT_TWIG)).isEqualTo(shortTwig)
        assertThat(longBranch.toSet()).hasSize(16)
        assertThat(shortTwig.toSet()).hasSize(16)
    }

    @Test
    fun `direct renderer and chart use the identical canonical long branch repertoire`() {
        val transliterator = YoungerFutharkTransliterator()
        assertThat(transliterator.transliterate(latinSigns)).isEqualTo(longBranch)
        assertThat(RuneReferenceSeedData.getYoungerFutharkRunes().map { it.character })
            .containsExactlyElementsIn(longBranch.map { it.toString() }).inOrder()
    }

    @Test
    fun `all modern Latin letters are represented within the chosen Younger repertoire`() {
        val renderer = YoungerFutharkRenderer()
        val latin = "abcdefghijklmnopqrstuvwxyzðąʀ"
        assertThat(YoungerFutharkTransliterator().transliterate(latin).toSet())
            .containsAtLeastElementsIn("ᛒᛁᚢᛏᚴᛋ".toSet())
        YoungerFutharkVariant.entries.forEach { variant ->
            val allowed = if (variant == YoungerFutharkVariant.LONG_BRANCH) longBranch else shortTwig
            assertThat(renderer.render(latin, variant).all { it in allowed }).isTrue()
        }
        assertThat(YoungerFutharkTransliterator().transliterate(latin).all { it in longBranch }).isTrue()
        assertThat(YoungerFutharkTransliterator().transliterate("aepdomx"))
            .isEqualTo("ᛅᛁᛒᛏᚢᛘᚴᛋ")
    }

    @Test
    fun `punctuation numbers and unsupported symbols pass through without changing branch`() {
        val source = "123!? 😀"
        YoungerFutharkVariant.entries.forEach { variant ->
            assertThat(YoungerFutharkAlphabet.render(source, variant)).isEqualTo(source)
        }
    }

    @Test
    fun `canonical quote Younger strings come from the same current converter as previews`() {
        val transliterator = YoungerFutharkTransliterator()
        QuoteSeedData.getCanonicalQuotes().forEach { quote ->
            assertThat(quote.runicYounger).isEqualTo(transliterator.transliterate(quote.textLatin))
            assertThat(quote.runicYounger.orEmpty().filter { it.code in 0x16A0..0x16FF }.all { it in longBranch })
                .isTrue()
        }
    }
}
