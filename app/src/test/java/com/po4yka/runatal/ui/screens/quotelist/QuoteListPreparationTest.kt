package com.po4yka.runatal.ui.screens.quotelist

import com.google.common.truth.Truth.assertThat
import com.po4yka.runatal.domain.model.Quote
import com.po4yka.runatal.domain.model.ResolvedQuoteRendering
import com.po4yka.runatal.domain.model.RunicScript
import com.po4yka.runatal.domain.translation.TranslationMode
import com.po4yka.runatal.domain.usecase.quote.ResolveQuoteRenderingUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class QuoteListPreparationTest {
    private val resolve = mockk<ResolveQuoteRenderingUseCase>()
    private val preparation = QuoteListPreparation(resolve)
    private val quotes = (0 until 1000).map { index ->
        Quote(index + 1L, "Rune $index", "Author ${index % 5}", null, null, null,
            isUserCreated = index % 2 == 0, isFavorite = index % 3 == 0)
    }

    @Test
    fun `a thousand quote library reuses resolution across search tabs and fonts`() = runTest {
        coEvery { resolve(any(), any()) } coAnswers {
            ResolvedQuoteRendering("ᚠ", emptyList(), TranslationMode.TRANSLITERATE, "Resolved ${firstArg<Quote>().id}")
        }
        prepare()
        listOf("Rune", "Rune 9", "Author 4", "", "Rune").forEach { query ->
            val state = prepare(query = query, font = "babelstone")
            val expected = quotes.filter {
                query.isEmpty() || it.textLatin.lowercase().contains(query.lowercase()) ||
                    it.author.lowercase().contains(query.lowercase())
            }
            assertThat(state.quotes).containsExactlyElementsIn(expected).inOrder()
            assertThat(state.quoteItems.map { it.quote }).containsExactlyElementsIn(expected).inOrder()
        }
        assertThat(prepare(filter = QuoteFilter.FAVORITES).quotes).containsExactlyElementsIn(
            quotes.filter { it.isFavorite }).inOrder()
        assertThat(prepare(filter = QuoteFilter.USER_CREATED).quotes).containsExactlyElementsIn(
            quotes.filter { it.isUserCreated }).inOrder()
        coVerify(exactly = 1000) { resolve(any(), RunicScript.ELDER_FUTHARK) }
        println("Library resolution measurement: 1000 calls for 1000 quotes across 8 views; search does no new resolution")
    }

    @Test
    fun `editing removal and script changes invalidate only the relevant retained results`() = runTest {
        coEvery { resolve(any(), any()) } coAnswers {
            val quote = firstArg<Quote>()
            ResolvedQuoteRendering(quote.textLatin, emptyList(), TranslationMode.TRANSLITERATE, "Current")
        }
        prepare()
        val updated = quotes.toMutableList().apply { this[0] = first().copy(textLatin = "Changed") }
        val state = prepare(updated)
        assertThat(state.quoteItems.first().runicPreviewText).isEqualTo("Changed")
        coVerify(exactly = 1001) { resolve(any(), RunicScript.ELDER_FUTHARK) }
        prepare(updated.drop(1))
        coVerify(exactly = 1001) { resolve(any(), RunicScript.ELDER_FUTHARK) }
        prepare(updated.drop(1), script = RunicScript.CIRTH)
        coVerify(exactly = 999) { resolve(any(), RunicScript.CIRTH) }
    }

    @Test
    fun `an initial narrow search resolves only matches and a failed resolution can be retried`() = runTest {
        coEvery { resolve(any(), any()) } throws java.io.IOException("storage")
        val failure = runCatching { prepare(query = "Rune 999") }.exceptionOrNull()
        assertThat(failure).isInstanceOf(java.io.IOException::class.java)
        coEvery { resolve(any(), any()) } returns
            ResolvedQuoteRendering("ᚠ", emptyList(), TranslationMode.TRANSLITERATE, "Current")
        assertThat(prepare(query = "Rune 999").quotes).containsExactly(quotes.last())
        coVerify(exactly = 2) { resolve(any(), any()) }
    }

    private suspend fun prepare(
        source: List<Quote> = quotes, script: RunicScript = RunicScript.ELDER_FUTHARK,
        filter: QuoteFilter = QuoteFilter.ALL, query: String = "", font: String = "noto"
    ) = preparation.prepare(source, script, font, filter, query)
}
