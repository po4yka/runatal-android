package com.po4yka.runatal.domain.usecase.translation

import com.google.common.truth.Truth.assertThat
import com.po4yka.runatal.domain.model.Quote
import com.po4yka.runatal.domain.model.RunicScript
import com.po4yka.runatal.domain.repository.QuoteRepository
import com.po4yka.runatal.domain.repository.TranslationRepository
import com.po4yka.runatal.domain.translation.HistoricalStage
import com.po4yka.runatal.domain.translation.HistoricalTranslationService
import com.po4yka.runatal.domain.translation.TranslationDerivationKind
import com.po4yka.runatal.domain.translation.TranslationFidelity
import com.po4yka.runatal.domain.translation.TranslationMode
import com.po4yka.runatal.domain.translation.TranslationResolutionStatus
import com.po4yka.runatal.domain.translation.TranslationResult
import com.po4yka.runatal.domain.translation.YoungerFutharkVariant
import com.po4yka.runatal.domain.transliteration.CirthTransliterator
import com.po4yka.runatal.domain.transliteration.ElderFutharkTransliterator
import com.po4yka.runatal.domain.transliteration.TransliterationFactory
import com.po4yka.runatal.domain.transliteration.YoungerFutharkTransliterator
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class SaveTranslationToLibraryUseCaseTest {

    private val quotes = mockk<QuoteRepository>()
    private val translations = mockk<TranslationRepository>(relaxed = true)
    private val historicalService = mockk<HistoricalTranslationService>()
    private val factory = TransliterationFactory(
        ElderFutharkTransliterator(), YoungerFutharkTransliterator(), CirthTransliterator()
    )
    private val savedQuote = slot<Quote>()
    private val useCase = SaveTranslationToLibraryUseCase(
        quotes, translations, BuildTransliterationBundleUseCase(factory),
        BuildHistoricalTranslationBundleUseCase(historicalService)
    )

    @Before
    fun setUp() {
        coEvery { quotes.saveUserQuote(capture(savedQuote)) } returns 17L
        every { historicalService.translate(any(), any(), any(), any()) } answers {
            TranslationResult(
                sourceText = firstArg(), script = secondArg(), fidelity = thirdArg(),
                derivationKind = TranslationDerivationKind.TOKEN_COMPOSED,
                historicalStage = HistoricalStage.OLD_NORSE,
                normalizedForm = "nótt", diplomaticForm = "nutt", glyphOutput = "ᚾᚢᛏᛏ",
                resolutionStatus = TranslationResolutionStatus.RECONSTRUCTED,
                confidence = 0.9f, engineVersion = "historical-test", datasetVersion = "dataset-test"
            )
        }
    }

    @Test
    fun `translate saves canonical direct renderings and stores historical results exclusively in cache`() = runTest {
        val cached = slot<List<TranslationResult>>()
        coEvery { translations.cacheTranslations(17L, capture(cached), false) } returns Unit

        useCase(request(TranslationMode.TRANSLATE))

        assertDirectQuote()
        assertThat(savedQuote.captured.runicYounger).isEqualTo("ᚾᛁᚴᚼᛏ")
        assertThat(cached.captured).hasSize(3)
        assertThat(cached.captured.map { it.glyphOutput }).containsExactly("ᚾᚢᛏᛏ", "ᚾᚢᛏᛏ", "ᚾᚢᛏᛏ")
    }

    @Test
    fun `transliterate saves canonical renderings without writing historical records`() = runTest {
        useCase(request(TranslationMode.TRANSLITERATE))

        assertDirectQuote()
        verify(exactly = 0) { historicalService.translate(any(), any(), any(), any()) }
        coVerify(exactly = 0) { translations.cacheTranslations(any(), any(), any()) }
    }

    @Test
    fun `historical cache failure leaves saved quote fields as canonical direct text`() = runTest {
        coEvery { translations.cacheTranslations(any(), any(), any()) } throws IOException("cache write failed")

        val failure = runCatching { useCase(request(TranslationMode.TRANSLATE)) }.exceptionOrNull()

        assertThat(failure).isInstanceOf(IOException::class.java)
        assertDirectQuote()
    }

    private fun assertDirectQuote() {
        assertThat(savedQuote.captured.textLatin).isEqualTo("night")
        assertThat(savedQuote.captured.isUserCreated).isTrue()
        assertThat(savedQuote.captured.runicElder).isEqualTo(factory.transliterate("night", RunicScript.ELDER_FUTHARK))
        assertThat(savedQuote.captured.runicYounger)
            .isEqualTo(factory.transliterate("night", RunicScript.YOUNGER_FUTHARK))
        assertThat(savedQuote.captured.runicCirth).isEqualTo(factory.transliterate("night", RunicScript.CIRTH))
    }

    private fun request(mode: TranslationMode) = SaveTranslationRequest(
        inputText = " night ", translationMode = mode, fidelity = TranslationFidelity.STRICT,
        youngerVariant = YoungerFutharkVariant.SHORT_TWIG
    )
}
