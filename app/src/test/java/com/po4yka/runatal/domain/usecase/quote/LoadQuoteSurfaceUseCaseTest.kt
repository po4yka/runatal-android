package com.po4yka.runatal.domain.usecase.quote

import com.google.common.truth.Truth.assertThat
import com.po4yka.runatal.domain.model.Quote
import com.po4yka.runatal.domain.model.RunicScript
import com.po4yka.runatal.domain.repository.NoOpTranslationRepository
import com.po4yka.runatal.domain.repository.QuoteRepository
import com.po4yka.runatal.domain.repository.ReadingHistoryRepository
import com.po4yka.runatal.domain.repository.TranslationRepository
import com.po4yka.runatal.domain.translation.TranslationMode
import com.po4yka.runatal.domain.transliteration.CirthTransliterator
import com.po4yka.runatal.domain.transliteration.ElderFutharkTransliterator
import com.po4yka.runatal.domain.transliteration.TransliterationFactory
import com.po4yka.runatal.domain.transliteration.YoungerFutharkTransliterator
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class LoadQuoteSurfaceUseCaseTest {
    private val quotes = mockk<QuoteRepository>()
    private val history = mockk<ReadingHistoryRepository>(relaxed = true)
    private val quote = Quote(1L, "wolf", "Reader", null, null, null)
    private val factory = TransliterationFactory(
        ElderFutharkTransliterator(), YoungerFutharkTransliterator(), CirthTransliterator()
    )

    @Test
    fun `failed rendering does not become a reading or increase a streak`() = runTest {
        coEvery { quotes.quoteOfTheDay() } returns quote.copy(renderingMode = TranslationMode.TRANSLATE)
        every { history.readings() } returns flowOf(emptyList())
        val translations = mockk<TranslationRepository>()
        coEvery { translations.getCachedTranslation(any(), any(), any(), any(), any()) } throws IOException("disk")
        val load = loader(translations)
        val failure = runCatching { load(QuoteSurfaceSource.DAILY, RunicScript.ELDER_FUTHARK) }.exceptionOrNull()
        assertThat(failure).isInstanceOf(IOException::class.java)
        coVerify(exactly = 0) { history.recordRead(any(), any()) }
    }

    @Test
    fun `successfully prepared content is recorded using its actual quote and script`() = runTest {
        coEvery { quotes.quoteOfTheDay() } returns quote
        every { history.readings() } returns flowOf(emptyList())
        val surface = loader(NoOpTranslationRepository)(QuoteSurfaceSource.DAILY, RunicScript.ELDER_FUTHARK)
        assertThat(surface?.presentation?.runicText).isEqualTo("ᚹᛟᛚᚠ")
        coVerify(exactly = 1) { history.recordRead(1L, RunicScript.ELDER_FUTHARK) }
    }

    private fun loader(translations: TranslationRepository) = LoadQuoteSurfaceUseCase(
        quotes, BuildQuotePresentationUseCase(ResolveQuoteRenderingUseCase(factory, translations)), history
    )
}
