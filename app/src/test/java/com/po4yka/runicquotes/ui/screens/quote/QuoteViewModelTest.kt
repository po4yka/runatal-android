package com.po4yka.runicquotes.ui.screens.quote

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.po4yka.runicquotes.data.preferences.UserPreferences
import com.po4yka.runicquotes.data.preferences.UserPreferencesManager
import com.po4yka.runicquotes.domain.repository.NoOpTranslationRepository
import com.po4yka.runicquotes.domain.repository.QuoteRepository
import com.po4yka.runicquotes.domain.repository.TranslationRepository
import com.po4yka.runicquotes.domain.model.Quote
import com.po4yka.runicquotes.domain.model.RunicScript
import com.po4yka.runicquotes.domain.transliteration.CirthTransliterator
import com.po4yka.runicquotes.domain.transliteration.ElderFutharkTransliterator
import com.po4yka.runicquotes.domain.transliteration.TransliterationFactory
import com.po4yka.runicquotes.domain.transliteration.YoungerFutharkTransliterator
import com.po4yka.runicquotes.domain.translation.HistoricalStage
import com.po4yka.runicquotes.domain.translation.TranslationDerivationKind
import com.po4yka.runicquotes.domain.translation.TranslationFidelity
import com.po4yka.runicquotes.domain.translation.TranslationProvenanceEntry
import com.po4yka.runicquotes.domain.translation.TranslationResolutionStatus
import com.po4yka.runicquotes.domain.translation.TranslationResult
import com.po4yka.runicquotes.domain.translation.TranslationTokenBreakdown
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class QuoteViewModelTest {

    private lateinit var quoteRepository: QuoteRepository
    private lateinit var userPreferencesManager: UserPreferencesManager
    private lateinit var transliterationFactory: TransliterationFactory
    private lateinit var translationRepository: TranslationRepository
    private lateinit var viewModel: QuoteViewModel

    private val testDispatcher = StandardTestDispatcher()

    private val testQuote = Quote(
        id = 1L,
        textLatin = "Test quote",
        author = "Test Author",
        runicElder = "\u16CF\u16D6\u16CA\u16CF",
        runicYounger = "\u16CF\u16D6\u16CA\u16CF",
        runicCirth = "\uE088\uE0C9\uE09C\uE088"
    )

    private val defaultPreferences = UserPreferences(
        selectedScript = RunicScript.ELDER_FUTHARK,
        selectedFont = "noto",
        showTransliteration = true
    )

    private lateinit var preferencesFlow: MutableStateFlow<UserPreferences>

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        quoteRepository = mockk()
        userPreferencesManager = mockk()
        translationRepository = NoOpTranslationRepository
        transliterationFactory = TransliterationFactory(
            elderFutharkTransliterator = ElderFutharkTransliterator(),
            youngerFutharkTransliterator = YoungerFutharkTransliterator(),
            cirthTransliterator = CirthTransliterator()
        )

        preferencesFlow = MutableStateFlow(defaultPreferences)
        every { userPreferencesManager.userPreferencesFlow } returns preferencesFlow
        coEvery { userPreferencesManager.updateSelectedScript(any()) } coAnswers {
            preferencesFlow.value = preferencesFlow.value.copy(selectedScript = firstArg())
        }
        coEvery { userPreferencesManager.updateShowTransliteration(any()) } returns Unit
        coEvery { quoteRepository.getAllQuotes() } returns listOf(testQuote)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `viewModel loads quote of the day on initialization`() = runTest {
        coEvery { quoteRepository.quoteOfTheDay() } returns testQuote

        viewModel = createViewModel()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isInstanceOf(QuoteUiState.Success::class.java)
        val state = viewModel.uiState.value as QuoteUiState.Success
        assertThat(state.quote).isEqualTo(testQuote)
        assertThat(state.runicText).isEqualTo(testQuote.runicElder)
    }

    @Test
    fun `repository returning null emits Empty state`() = runTest {
        coEvery { quoteRepository.quoteOfTheDay() } returns null

        viewModel = createViewModel()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isEqualTo(QuoteUiState.Empty)
    }

    @Test
    fun `repository exception emits Error state`() = runTest {
        coEvery { quoteRepository.quoteOfTheDay() } throws IOException("disk")

        viewModel = createViewModel()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isEqualTo(QuoteUiState.Error("disk"))
    }

    @Test
    fun `updateSelectedScript persists preference and rerenders`() = runTest {
        coEvery { quoteRepository.quoteOfTheDay() } returns testQuote
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.updateSelectedScript(RunicScript.CIRTH)
        advanceUntilIdle()

        val state = viewModel.uiState.value as QuoteUiState.Success
        assertThat(state.selectedScript).isEqualTo(RunicScript.CIRTH)
        coVerify { userPreferencesManager.updateSelectedScript(RunicScript.CIRTH) }
    }

    @Test
    fun `toggleWordByWordMode keeps local override when preferences change`() = runTest {
        coEvery { quoteRepository.quoteOfTheDay() } returns testQuote
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.toggleWordByWordMode()
        advanceUntilIdle()
        preferencesFlow.value = preferencesFlow.value.copy(
            selectedScript = RunicScript.YOUNGER_FUTHARK,
            showTransliteration = false
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value as QuoteUiState.Success
        assertThat(state.wordByWordEnabled).isTrue()
        assertThat(state.selectedScript).isEqualTo(RunicScript.YOUNGER_FUTHARK)
    }

    @Test
    fun `toggleTransliterationVisibility updates persisted preference`() = runTest {
        coEvery { quoteRepository.quoteOfTheDay() } returns testQuote
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.toggleTransliterationVisibility()
        advanceUntilIdle()

        coVerify { userPreferencesManager.updateShowTransliteration(false) }
    }

    @Test
    fun `deleteQuote removes current quote and loads next daily quote`() = runTest {
        val nextQuote = testQuote.copy(id = 2L, textLatin = "Next quote")
        coEvery { quoteRepository.quoteOfTheDay() } returns testQuote andThen nextQuote
        coEvery { quoteRepository.deleteUserQuote(testQuote.id) } returns Unit

        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.deleteQuote()
        advanceUntilIdle()

        coVerify { quoteRepository.deleteUserQuote(testQuote.id) }
        assertThat((viewModel.uiState.value as QuoteUiState.Success).quote).isEqualTo(nextQuote)
    }

    @Test
    fun `latest available translation is preferred over stored transliteration`() = runTest {
        val translationRepository = mockk<TranslationRepository>()
        val cachedTranslation = TranslationResult(
            sourceText = testQuote.textLatin,
            script = RunicScript.ELDER_FUTHARK,
            fidelity = TranslationFidelity.READABLE,
            derivationKind = TranslationDerivationKind.TOKEN_COMPOSED,
            historicalStage = HistoricalStage.OLD_NORSE,
            normalizedForm = "test",
            diplomaticForm = "test",
            glyphOutput = "cached elder",
            resolutionStatus = TranslationResolutionStatus.RECONSTRUCTED,
            confidence = 0.88f,
            notes = listOf("cached"),
            provenance = listOf(
                TranslationProvenanceEntry(
                    sourceId = "cache",
                    label = "Cache",
                    role = "test",
                    license = "none"
                )
            ),
            tokenBreakdown = listOf(
                TranslationTokenBreakdown(
                    sourceToken = "Test",
                    normalizedToken = "test",
                    diplomaticToken = "cached",
                    glyphToken = "cached"
                )
            ),
            engineVersion = "engine",
            datasetVersion = "dataset"
        )
        coEvery { quoteRepository.quoteOfTheDay() } returns testQuote
        coEvery { translationRepository.getLatestAvailableTranslation(1L, RunicScript.ELDER_FUTHARK) } returns
            cachedTranslation
        coEvery { translationRepository.getLatestAvailableTranslation(1L, RunicScript.YOUNGER_FUTHARK) } returns null
        coEvery { translationRepository.getLatestAvailableTranslation(1L, RunicScript.CIRTH) } returns null

        viewModel = createViewModel(translationRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value as QuoteUiState.Success
        assertThat(state.runicText).isEqualTo("cached elder")
        assertThat(state.wordBreakdown.single().runicToken).isEqualTo("cached")
    }

    @Test
    fun `ui state emits loading before success`() = runTest {
        coEvery { quoteRepository.quoteOfTheDay() } returns testQuote
        viewModel = createViewModel()

        viewModel.uiState.test {
            assertThat(awaitItem()).isEqualTo(QuoteUiState.Loading)
            advanceUntilIdle()
            assertThat(awaitItem()).isInstanceOf(QuoteUiState.Success::class.java)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun createViewModel(
        translationRepository: TranslationRepository = NoOpTranslationRepository
    ): QuoteViewModel {
        return QuoteViewModel(
            quoteRepository = quoteRepository,
            userPreferencesManager = userPreferencesManager,
            transliterationFactory = transliterationFactory,
            translationRepository = translationRepository
        )
    }
}
