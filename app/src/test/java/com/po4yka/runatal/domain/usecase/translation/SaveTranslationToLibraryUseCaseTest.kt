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
        coEvery { translations.saveUserQuoteWithTranslations(capture(savedQuote), any()) } returns 17L
        every { historicalService.translate(any(), any(), any(), any()) } answers {
            TranslationResult(
                sourceText = firstArg(), script = secondArg(), fidelity = thirdArg(),
                requestedVariant = if (secondArg<RunicScript>() == RunicScript.YOUNGER_FUTHARK) {
                    arg<YoungerFutharkVariant>(3).name
                } else { null },
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
        coEvery { translations.saveUserQuoteWithTranslations(capture(savedQuote), capture(cached)) } returns 17L

        useCase(request(TranslationMode.TRANSLATE))

        assertDirectQuote()
        assertThat(savedQuote.captured.renderingMode).isEqualTo(TranslationMode.TRANSLATE)
        assertThat(savedQuote.captured.renderingFidelity).isEqualTo(TranslationFidelity.STRICT)
        assertThat(savedQuote.captured.renderingYoungerVariant).isEqualTo(YoungerFutharkVariant.SHORT_TWIG)
        assertThat(savedQuote.captured.runicYounger).isEqualTo("ᚿᛁᚴᚽᛐ")
        coVerify(exactly = 0) { quotes.saveUserQuote(any()) }
        coVerify(exactly = 0) { translations.cacheTranslations(any(), any(), any()) }
        assertThat(cached.captured).hasSize(3)
        assertThat(cached.captured.map { it.glyphOutput }).containsExactly("ᚾᚢᛏᛏ", "ᚾᚢᛏᛏ", "ᚾᚢᛏᛏ")
    }

    @Test
    fun `transliterate saves canonical renderings without writing historical records`() = runTest {
        useCase(request(TranslationMode.TRANSLITERATE))

        assertDirectQuote()
        assertThat(savedQuote.captured.renderingMode).isEqualTo(TranslationMode.TRANSLITERATE)
        assertThat(savedQuote.captured.renderingYoungerVariant).isEqualTo(YoungerFutharkVariant.SHORT_TWIG)
        verify(exactly = 0) { historicalService.translate(any(), any(), any(), any()) }
        coVerify(exactly = 0) { translations.saveUserQuoteWithTranslations(any(), any()) }
        coVerify(exactly = 0) { translations.cacheTranslations(any(), any(), any()) }
    }

    @Test
    fun `historical write failure cannot create a quote through a separate nontransactional path`() = runTest {
        coEvery { translations.saveUserQuoteWithTranslations(any(), any()) } throws IOException("write failed")

        val failure = runCatching { useCase(request(TranslationMode.TRANSLATE)) }.exceptionOrNull()

        assertThat(failure).isInstanceOf(IOException::class.java)
        coVerify(exactly = 0) { quotes.saveUserQuote(any()) }
        coVerify(exactly = 0) { translations.cacheTranslations(any(), any(), any()) }
    }

    @Test
    fun `bundle preparation failure is rejected before any persistence`() = runTest {
        every { historicalService.translate(any(), any(), any(), any()) } throws
            IllegalStateException("dataset failure")

        val failure = runCatching { useCase(request(TranslationMode.TRANSLATE)) }.exceptionOrNull()

        assertThat(failure).isInstanceOf(IllegalStateException::class.java)
        assertNoPersistence()
    }

    @Test
    fun `direct rendering preparation error cannot persist empty quote fields`() = runTest {
        val broken = mockk<BuildTransliterationBundleUseCase>()
        coEvery { broken(any(), any(), any()) } returns TransliterationBundle(errorMessage = "rendering failure")
        val brokenSave = SaveTranslationToLibraryUseCase(
            quotes, translations, broken, BuildHistoricalTranslationBundleUseCase(historicalService)
        )

        val failure = runCatching { brokenSave(request(TranslationMode.TRANSLITERATE)) }.exceptionOrNull()

        assertThat(failure).isInstanceOf(IllegalStateException::class.java)
        assertNoPersistence()
    }

    @Test
    fun `selected unavailable output cannot save successes from the other scripts`() = runTest {
        every {
            historicalService.translate(any(), RunicScript.YOUNGER_FUTHARK, any(), any())
        } answers {
            prepared(firstArg(), RunicScript.YOUNGER_FUTHARK, thirdArg(), arg(3)).copy(
                glyphOutput = "", resolutionStatus = TranslationResolutionStatus.UNAVAILABLE,
                confidence = 0f, unresolvedTokens = listOf("night")
            )
        }

        val failure = runCatching { useCase(request(TranslationMode.TRANSLATE)) }.exceptionOrNull()

        assertThat(failure).isInstanceOf(IllegalStateException::class.java)
        assertNoPersistence()
    }

    @Test
    fun `mismatched selected source script fidelity or variant is rejected before writing`() = runTest {
        val malformed: List<(TranslationResult) -> TranslationResult> = listOf(
            { it.copy(sourceText = "another source") },
            { it.copy(script = RunicScript.CIRTH) },
            { it.copy(fidelity = TranslationFidelity.DECORATIVE) },
            { it.copy(requestedVariant = YoungerFutharkVariant.LONG_BRANCH.name) }
        )
        malformed.forEach { alter ->
            every {
                historicalService.translate(any(), RunicScript.YOUNGER_FUTHARK, any(), any())
            } answers { alter(prepared(firstArg(), secondArg(), thirdArg(), arg(3))) }

            val failure = runCatching { useCase(request(TranslationMode.TRANSLATE)) }.exceptionOrNull()

            assertThat(failure).isInstanceOf(IllegalStateException::class.java)
        }
        assertNoPersistence()
    }

    private fun prepared(
        text: String, script: RunicScript, fidelity: TranslationFidelity, variant: YoungerFutharkVariant
    ) = TranslationResult(
        sourceText = text, script = script, fidelity = fidelity,
        requestedVariant = if (script == RunicScript.YOUNGER_FUTHARK) variant.name else null,
        derivationKind = TranslationDerivationKind.TOKEN_COMPOSED, historicalStage = HistoricalStage.OLD_NORSE,
        normalizedForm = "nótt", diplomaticForm = "nutt", glyphOutput = "ᚾᚢᛏᛏ",
        resolutionStatus = TranslationResolutionStatus.RECONSTRUCTED, confidence = 0.9f,
        engineVersion = "historical-test", datasetVersion = "dataset-test"
    )

    private fun assertNoPersistence() {
        coVerify(exactly = 0) { quotes.saveUserQuote(any()) }
        coVerify(exactly = 0) { translations.saveUserQuoteWithTranslations(any(), any()) }
        coVerify(exactly = 0) { translations.cacheTranslations(any(), any(), any()) }
    }

    private fun assertDirectQuote() {
        assertThat(savedQuote.captured.textLatin).isEqualTo("night")
        assertThat(savedQuote.captured.isUserCreated).isTrue()
        assertThat(savedQuote.captured.runicElder).isEqualTo(factory.transliterate("night", RunicScript.ELDER_FUTHARK))
        assertThat(savedQuote.captured.runicYounger)
            .isEqualTo(factory.transliterate("night", RunicScript.YOUNGER_FUTHARK, YoungerFutharkVariant.SHORT_TWIG))
        assertThat(savedQuote.captured.runicCirth).isEqualTo(factory.transliterate("night", RunicScript.CIRTH))
    }

    @Test
    fun `direct one and two character sources remain valid saved quotes`() = runTest {
        listOf("I", "Go").forEach { source ->
            useCase(request(TranslationMode.TRANSLITERATE).copy(inputText = source))
            assertThat(savedQuote.captured.textLatin).isEqualTo(source)
            assertThat(savedQuote.captured.runicElder).isNotEmpty()
        }
    }

    @Test
    fun `blank and overbudget source is rejected before preparing or persisting`() = runTest {
        listOf("", " \t\n", "a".repeat(281)).forEach { source ->
            val failure = runCatching {
                useCase(request(TranslationMode.TRANSLITERATE).copy(inputText = source))
            }.exceptionOrNull()
            assertThat(failure).isInstanceOf(IllegalStateException::class.java)
        }
        assertNoPersistence()
        verify(exactly = 0) { historicalService.translate(any(), any(), any(), any()) }
    }

    private fun request(mode: TranslationMode) = SaveTranslationRequest(
        inputText = " night ", translationMode = mode, selectedScript = RunicScript.YOUNGER_FUTHARK,
        fidelity = TranslationFidelity.STRICT,
        youngerVariant = YoungerFutharkVariant.SHORT_TWIG
    )
}
