package com.po4yka.runicquotes.ui.screens.share

import com.google.common.truth.Truth.assertThat
import com.po4yka.runicquotes.data.repository.NoOpTranslationRepository
import com.po4yka.runicquotes.data.repository.QuoteRepository
import com.po4yka.runicquotes.data.repository.TranslationRepository
import com.po4yka.runicquotes.domain.model.Quote
import com.po4yka.runicquotes.domain.model.RunicScript
import com.po4yka.runicquotes.domain.translation.HistoricalStage
import com.po4yka.runicquotes.domain.translation.TranslationDerivationKind
import com.po4yka.runicquotes.domain.translation.TranslationFidelity
import com.po4yka.runicquotes.domain.translation.TranslationResolutionStatus
import com.po4yka.runicquotes.domain.translation.TranslationResult
import com.po4yka.runicquotes.util.ShareAppearance
import com.po4yka.runicquotes.util.ShareTemplate
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ShareViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var quoteRepository: QuoteRepository

    private val testQuote = Quote(
        id = 7L,
        textLatin = "Wisdom begins in wonder.",
        author = "Socrates",
        runicElder = "\u16B9\u16A0\u16DE",
        runicYounger = "\u16B9\u16A0\u16DE",
        runicCirth = "\uE0B8\uE080\uE089"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        quoteRepository = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(
        translationRepository: TranslationRepository = NoOpTranslationRepository
    ): ShareViewModel {
        return ShareViewModel(
            quoteRepository = quoteRepository,
            translationRepository = translationRepository
        )
    }

    @Test
    fun `initializeQuoteIfNeeded loads quote when given a non-zero id`() = runTest {
        coEvery { quoteRepository.getQuoteById(7L) } returns testQuote
        val viewModel = createViewModel()

        viewModel.initializeQuoteIfNeeded(7L)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isEqualTo(ShareUiState.Success(testQuote))
    }

    @Test
    fun `initializeQuoteIfNeeded ignores zero id`() = runTest {
        val viewModel = createViewModel()

        viewModel.initializeQuoteIfNeeded(0L)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isEqualTo(ShareUiState.Loading)
        coVerify(exactly = 0) { quoteRepository.getQuoteById(any()) }
    }

    @Test
    fun `initializeQuoteIfNeeded does not reload the same successful id`() = runTest {
        coEvery { quoteRepository.getQuoteById(7L) } returns testQuote
        val viewModel = createViewModel()

        viewModel.initializeQuoteIfNeeded(7L)
        advanceUntilIdle()
        viewModel.initializeQuoteIfNeeded(7L)
        advanceUntilIdle()

        coVerify(exactly = 1) { quoteRepository.getQuoteById(7L) }
    }

    @Test
    fun `loadQuote surfaces repository errors`() = runTest {
        coEvery { quoteRepository.getQuoteById(7L) } throws IOException("disk")
        val viewModel = createViewModel()

        viewModel.initializeQuoteIfNeeded(7L)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isEqualTo(ShareUiState.Error("Failed to load quote: disk"))
    }

    @Test
    fun `selectTemplate and selectAppearance update state flows`() = runTest {
        val viewModel = createViewModel()

        viewModel.selectTemplate(ShareTemplate.LANDSCAPE)
        viewModel.selectAppearance(ShareAppearance.LIGHT)

        assertThat(viewModel.selectedTemplate.value).isEqualTo(ShareTemplate.LANDSCAPE)
        assertThat(viewModel.selectedAppearance.value).isEqualTo(ShareAppearance.LIGHT)
    }

    @Test
    fun `latest translations override stored runes on the share surface`() = runTest {
        val translationRepository = mockk<TranslationRepository>()
        val elderTranslation = TranslationResult(
            sourceText = testQuote.textLatin,
            script = RunicScript.ELDER_FUTHARK,
            fidelity = TranslationFidelity.STRICT,
            derivationKind = TranslationDerivationKind.PHRASE_TEMPLATE,
            historicalStage = HistoricalStage.PROTO_NORSE,
            normalizedForm = "wulfaz",
            diplomaticForm = "wulfaz",
            glyphOutput = "cached elder",
            resolutionStatus = TranslationResolutionStatus.ATTESTED,
            confidence = 0.98f,
            engineVersion = "engine",
            datasetVersion = "dataset"
        )
        val cirthTranslation = TranslationResult(
            sourceText = testQuote.textLatin,
            script = RunicScript.CIRTH,
            fidelity = TranslationFidelity.READABLE,
            derivationKind = TranslationDerivationKind.SEQUENCE_TRANSCRIPTION,
            historicalStage = HistoricalStage.EREBOR_ENGLISH,
            normalizedForm = "wisdom begins in wonder",
            diplomaticForm = "w·i·s·d·o·m",
            glyphOutput = "cached cirth",
            resolutionStatus = TranslationResolutionStatus.APPROXIMATED,
            confidence = 0.7f,
            engineVersion = "engine",
            datasetVersion = "dataset"
        )
        coEvery { quoteRepository.getQuoteById(7L) } returns testQuote
        coEvery { translationRepository.getLatestAvailableTranslation(7L, RunicScript.ELDER_FUTHARK) } returns
            elderTranslation
        coEvery { translationRepository.getLatestAvailableTranslation(7L, RunicScript.YOUNGER_FUTHARK) } returns null
        coEvery { translationRepository.getLatestAvailableTranslation(7L, RunicScript.CIRTH) } returns cirthTranslation

        val viewModel = createViewModel(translationRepository = translationRepository)
        viewModel.initializeQuoteIfNeeded(7L)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isEqualTo(
            ShareUiState.Success(
                testQuote.copy(
                    runicElder = "cached elder",
                    runicYounger = testQuote.runicYounger,
                    runicCirth = "cached cirth"
                )
            )
        )
    }

    @Test
    fun `retry reloads quote after an error`() = runTest {
        coEvery { quoteRepository.getQuoteById(7L) } throws IOException("disk") andThen testQuote
        val viewModel = createViewModel()

        viewModel.initializeQuoteIfNeeded(7L)
        advanceUntilIdle()
        assertThat(viewModel.uiState.value).isEqualTo(ShareUiState.Error("Failed to load quote: disk"))

        viewModel.retry()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isEqualTo(ShareUiState.Success(testQuote))
    }
}
