package com.po4yka.runatal.data.seed

import com.google.common.truth.Truth.assertThat
import com.po4yka.runatal.domain.model.Quote
import com.po4yka.runatal.domain.model.RunicScript
import com.po4yka.runatal.domain.model.getRunicText
import com.po4yka.runatal.domain.transliteration.CirthTransliterator
import com.po4yka.runatal.domain.transliteration.ElderFutharkTransliterator
import com.po4yka.runatal.domain.transliteration.TransliterationFactory
import com.po4yka.runatal.domain.transliteration.YoungerFutharkTransliterator
import org.junit.Test

class QuoteSeedDataTest {
    private val factory = TransliterationFactory(
        ElderFutharkTransliterator(), YoungerFutharkTransliterator(), CirthTransliterator()
    )

    @Test
    fun `all canonical seed scripts match current factory and word breakdown`() {
        QuoteSeedData.getCanonicalQuotes().forEach { row ->
            val quote = Quote(
                id = row.id, textLatin = row.textLatin, author = row.author,
                runicElder = row.runicElder, runicYounger = row.runicYounger, runicCirth = row.runicCirth
            )
            RunicScript.entries.forEach { script ->
                assertThat(quote.getRunicText(script, factory)).isEqualTo(factory.transliterate(row.textLatin, script))
                assertThat(factory.transliterateWordByWord(row.textLatin, script).fullText)
                    .isEqualTo(quote.getRunicText(script, factory))
            }
        }
    }

    @Test
    fun `canonical generation does not change frozen legacy seed identity or recognition values`() {
        val old = QuoteSeedData.getLegacyQuotes()
        val fresh = QuoteSeedData.getCanonicalQuotes()
        assertThat(QuoteSeedData.getLegacyQuotes()).isEqualTo(old)
        assertThat(fresh.map { it.canonicalKey }).containsExactlyElementsIn(old.map { it.canonicalKey }).inOrder()
        assertThat(fresh.map { it.textLatin }).containsExactlyElementsIn(old.map { it.textLatin }).inOrder()
        assertThat(fresh.map { it.author }).containsExactlyElementsIn(old.map { it.author }).inOrder()
        assertThat(fresh.all { it.id == 0L }).isTrue()
        assertThat(fresh.first().runicElder).isNotEqualTo(old.first().runicElder)
        assertThat(fresh.first().runicElder).endsWith(".")
        assertThat(fresh.first().runicElder).contains("ᛃ")
        assertThat(fresh.first().runicCirth).isNotEqualTo(old.first().runicCirth)
    }
}
