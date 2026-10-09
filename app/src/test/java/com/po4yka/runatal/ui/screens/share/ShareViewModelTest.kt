package com.po4yka.runatal.ui.screens.share

import com.google.common.truth.Truth.assertThat
import com.po4yka.runatal.domain.repository.NoOpTranslationRepository
import com.po4yka.runatal.domain.repository.QuoteRepository
import com.po4yka.runatal.domain.repository.TranslationRepository
import com.po4yka.runatal.domain.model.Quote
import com.po4yka.runatal.domain.model.QuoteShareContent
import com.po4yka.runatal.data.preferences.UserPreferences
import com.po4yka.runatal.data.preferences.UserPreferencesManager
import com.po4yka.runatal.domain.usecase.quote.BuildQuotePresentationUseCase
import com.po4yka.runatal.domain.transliteration.TransliterationFactory
import com.po4yka.runatal.domain.transliteration.ElderFutharkTransliterator
import com.po4yka.runatal.domain.transliteration.YoungerFutharkTransliterator
import com.po4yka.runatal.domain.transliteration.CirthTransliterator
import io.mockk.every
import kotlinx.coroutines.flow.flowOf
import com.po4yka.runatal.domain.model.RunicScript
import com.po4yka.runatal.domain.translation.HistoricalStage
import com.po4yka.runatal.domain.translation.TranslationDerivationKind
import com.po4yka.runatal.domain.translation.TranslationFidelity
import com.po4yka.runatal.domain.translation.TranslationResolutionStatus
import com.po4yka.runatal.domain.translation.TranslationResult
import com.po4yka.runatal.util.ShareAppearance
import com.po4yka.runatal.util.ShareTemplate
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
        quoteId: Long = 0L,
        translationRepository: TranslationRepository = NoOpTranslationRepository,
        script: RunicScript = RunicScript.DEFAULT
    ): ShareViewModel {
        val preferences = mockk<UserPreferencesManager>()
        every { preferences.userPreferencesFlow } returns flowOf(UserPreferences(selectedScript = script))
        return ShareViewModel(
            quoteRepository = quoteRepository,
            quoteId = quoteId,
            buildQuotePresentationUseCase = BuildQuotePresentationUseCase(
                TransliterationFactory(ElderFutharkTransliterator(), YoungerFutharkTransliterator(), CirthTransliterator()),
                translationRepository
            ),
            userPreferencesManager = preferences
        )
    }

    @Test
    fun `route quote id loads quote on initialization`() = runTest {
        coEvery { quoteRepository.getQuoteById(7L) } returns testQuote
        val viewModel = createViewModel(quoteId = 7L)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isEqualTo(
            ShareUiState.Success(QuoteShareContent(testQuote, RunicScript.DEFAULT, "noto", testQuote.runicElder.orEmpty()))
        )
    }

    @Test
    fun `share prepares the selected script even when other cached fields exist`() = runTest {
        coEvery { quoteRepository.getQuoteById(7L) } returns testQuote
        val viewModel = createViewModel(quoteId = 7L, script = RunicScript.CIRTH)

        advanceUntilIdle()

        val content = (viewModel.uiState.value as ShareUiState.Success).content
        assertThat(content.script).isEqualTo(RunicScript.CIRTH)
        assertThat(content.scriptLabel).isEqualTo("Cirth")
        assertThat(content.runicText).isEqualTo(testQuote.runicCirth)
        assertThat(content.font).isEqualTo("noto")
    }

    @Test
    fun `zero route quote id exposes an error and avoids repository work`() = runTest {
        val viewModel = createViewModel()

        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isEqualTo(ShareUiState.Error("Quote not found"))
        coVerify(exactly = 0) { quoteRepository.getQuoteById(any()) }
    }

    @Test
    fun `route quote id is loaded exactly once on initialization`() = runTest {
        coEvery { quoteRepository.getQuoteById(7L) } returns testQuote
        createViewModel(quoteId = 7L)
        advanceUntilIdle()

        coVerify(exactly = 1) { quoteRepository.getQuoteById(7L) }
    }

    @Test
    fun `loadQuote surfaces repository errors`() = runTest {
        coEvery { quoteRepository.getQuoteById(7L) } throws IOException("disk")
        val viewModel = createViewModel(quoteId = 7L)
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

        val viewModel = createViewModel(
            quoteId = 7L,
            translationRepository = translationRepository
        )
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isEqualTo(
            ShareUiState.Success(QuoteShareContent(testQuote, RunicScript.DEFAULT, "noto", "cached elder"))
        )
    }

    @Test
    fun `retry reloads quote after an error`() = runTest {
        coEvery { quoteRepository.getQuoteById(7L) } throws IOException("disk") andThen testQuote
        val viewModel = createViewModel(quoteId = 7L)
        advanceUntilIdle()
        assertThat(viewModel.uiState.value).isEqualTo(ShareUiState.Error("Failed to load quote: disk"))

        viewModel.retry()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isEqualTo(
            ShareUiState.Success(QuoteShareContent(testQuote, RunicScript.DEFAULT, "noto", testQuote.runicElder.orEmpty()))
        )
    }
}
