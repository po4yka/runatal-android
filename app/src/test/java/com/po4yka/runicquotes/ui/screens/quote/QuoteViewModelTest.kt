package com.po4yka.runicquotes.ui.screens.quote

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.po4yka.runicquotes.data.preferences.UserPreferences
import com.po4yka.runicquotes.data.preferences.UserPreferencesManager
import com.po4yka.runicquotes.data.repository.QuoteRepository
import com.po4yka.runicquotes.data.repository.TranslationRepository
import com.po4yka.runicquotes.domain.model.Quote
import com.po4yka.runicquotes.domain.model.RunicScript
import com.po4yka.runicquotes.domain.transliteration.CirthTransliterator
import com.po4yka.runicquotes.domain.transliteration.ElderFutharkTransliterator
import com.po4yka.runicquotes.domain.transliteration.TransliterationFactory
import com.po4yka.runicquotes.domain.transliteration.YoungerFutharkTransliterator
import com.po4yka.runicquotes.domain.translation.HistoricalStage
import com.po4yka.runicquotes.domain.translation.TranslationFidelity
import com.po4yka.runicquotes.domain.translation.TranslationProvenanceEntry
import com.po4yka.runicquotes.domain.translation.TranslationResolutionStatus
import com.po4yka.runicquotes.domain.translation.TranslationResult
import com.po4yka.runicquotes.domain.translation.TranslationTokenBreakdown
import com.po4yka.runicquotes.util.QuoteShareManager
import com.po4yka.runicquotes.util.ShareAppearance
import com.po4yka.runicquotes.util.ShareTemplate
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
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
import java.io.IOException

/**
 * Comprehensive unit tests for QuoteViewModel.
 * Uses Turbine for testing StateFlow emissions and MockK for mocking dependencies.
 *
 * Coverage goals: >85%
 */
@OptIn(ExperimentalCoroutinesApi::class)
class QuoteViewModelTest {

    private lateinit var quoteRepository: QuoteRepository
    private lateinit var userPreferencesManager: UserPreferencesManager
    private lateinit var quoteShareManager: QuoteShareManager
    private lateinit var transliterationFactory: TransliterationFactory
    private lateinit var viewModel: QuoteViewModel

    private val testDispatcher = StandardTestDispatcher()

    private val testQuote = Quote(
        id = 1,
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

        // Create mocks
        quoteRepository = mockk()
        userPreferencesManager = mockk()
        quoteShareManager = mockk(relaxed = true)

        // Create real TransliterationFactory with real transliterators
        transliterationFactory = TransliterationFactory(
            ElderFutharkTransliterator(),
            YoungerFutharkTransliterator(),
            CirthTransliterator()
        )

        // Set up preferences flow
        preferencesFlow = MutableStateFlow(defaultPreferences)
        every { userPreferencesManager.userPreferencesFlow } returns preferencesFlow

        // Mock getAllQuotes used by loadRecentQuotes()
        coEvery { quoteRepository.getAllQuotes() } returns listOf(testQuote)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ==================== Initialization Tests ====================

    @Test
    fun `viewModel initializes with Loading state`() = runTest {
        // Given: Repository will return a quote
        coEvery { quoteRepository.quoteOfTheDay() } returns testQuote

        // When: ViewModel is created
        viewModel = QuoteViewModel(quoteRepository, userPreferencesManager, quoteShareManager, transliterationFactory)

        // Then: Initial state is Loading
        viewModel.uiState.test {
            val initialState = awaitItem()
            assertThat(initialState).isInstanceOf(QuoteUiState.Loading::class.java)

            // Advance to allow initialization to complete
            advanceUntilIdle()

            // Should transition to Success
            val successState = awaitItem()
            assertThat(successState).isInstanceOf(QuoteUiState.Success::class.java)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `viewModel loads quote of the day on initialization`() = runTest {
        // Given: Repository returns a quote
        coEvery { quoteRepository.quoteOfTheDay() } returns testQuote

        // When: ViewModel is created
        viewModel = QuoteViewModel(quoteRepository, userPreferencesManager, quoteShareManager, transliterationFactory)
        advanceUntilIdle()

        // Then: Quote is loaded
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state).isInstanceOf(QuoteUiState.Success::class.java)
            assertThat((state as QuoteUiState.Success).quote).isEqualTo(testQuote)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ==================== Success State Tests ====================

    @Test
    fun `successful quote load emits Success state with correct data`() = runTest {
        // Given: Repository returns a quote
        coEvery { quoteRepository.quoteOfTheDay() } returns testQuote

        // When: ViewModel loads quote
        viewModel = QuoteViewModel(quoteRepository, userPreferencesManager, quoteShareManager, transliterationFactory)
        advanceUntilIdle()

        // Then: Success state contains all expected data
        viewModel.uiState.test {
            val state = awaitItem() as QuoteUiState.Success

            assertThat(state.quote.id).isEqualTo(testQuote.id)
            assertThat(state.quote.textLatin).isEqualTo(testQuote.textLatin)
            assertThat(state.quote.author).isEqualTo(testQuote.author)
            assertThat(state.runicText).isEqualTo("\u16CF\u16D6\u16CA\u16CF")
            assertThat(state.selectedScript).isEqualTo(RunicScript.ELDER_FUTHARK)
            assertThat(state.selectedFont).isEqualTo("noto")
            assertThat(state.showTransliteration).isTrue()
            assertThat(state.wordByWordEnabled).isFalse()

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Success state uses domain extension for runic text`() = runTest {
        // Given: Quote with Elder Futhark text
        val quote = testQuote.copy(runicElder = "\u16B1\u16A2\u16BE\u16D6")
        coEvery { quoteRepository.quoteOfTheDay() } returns quote

        // When: ViewModel loads quote
        viewModel = QuoteViewModel(quoteRepository, userPreferencesManager, quoteShareManager, transliterationFactory)
        advanceUntilIdle()

        // Then: Runic text from domain extension is used
        viewModel.uiState.test {
            val state = awaitItem() as QuoteUiState.Success
            assertThat(state.runicText).isEqualTo("\u16B1\u16A2\u16BE\u16D6")
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ==================== Empty State Tests ====================

    @Test
    fun `repository returning null emits Empty state`() = runTest {
        // Given: Repository returns null
        coEvery { quoteRepository.quoteOfTheDay() } returns null

        // When: ViewModel loads quote
        viewModel = QuoteViewModel(quoteRepository, userPreferencesManager, quoteShareManager, transliterationFactory)
        advanceUntilIdle()

        // Then: Empty state is emitted
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state).isInstanceOf(QuoteUiState.Empty::class.java)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ==================== Error State Tests ====================

    @Test
    fun `repository throwing exception emits Error state`() = runTest {
        // Given: Repository throws IOException
        coEvery { quoteRepository.quoteOfTheDay() } throws IOException("Database error")

        // When: ViewModel loads quote
        viewModel = QuoteViewModel(quoteRepository, userPreferencesManager, quoteShareManager, transliterationFactory)
        advanceUntilIdle()

        // Then: Error state with message is emitted
        viewModel.uiState.test {
            val state = awaitItem() as QuoteUiState.Error
            assertThat(state.message).isEqualTo("Database error")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `error state handles exception without message`() = runTest {
        // Given: IOException without message
        coEvery { quoteRepository.quoteOfTheDay() } throws IOException()

        // When: ViewModel loads quote
        viewModel = QuoteViewModel(quoteRepository, userPreferencesManager, quoteShareManager, transliterationFactory)
        advanceUntilIdle()

        // Then: Generic error message is used
        viewModel.uiState.test {
            val state = awaitItem() as QuoteUiState.Error
            assertThat(state.message).isEqualTo("Failed to load quote")
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ==================== Preferences Change Tests ====================

    @Test
    fun `preferences change updates script without loading flash`() = runTest {
        // Given: ViewModel initialized with one quote
        coEvery { quoteRepository.quoteOfTheDay() } returns testQuote

        viewModel = QuoteViewModel(quoteRepository, userPreferencesManager, quoteShareManager, transliterationFactory)
        advanceUntilIdle()

        // When: Preferences change to different script
        viewModel.uiState.test {
            skipItems(1) // Skip initial success state

            preferencesFlow.value = defaultPreferences.copy(selectedScript = RunicScript.YOUNGER_FUTHARK)
            advanceUntilIdle()

            // Then: State updates immediately without loading
            val successState = awaitItem() as QuoteUiState.Success
            assertThat(successState.selectedScript).isEqualTo(RunicScript.YOUNGER_FUTHARK)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `font preference change is reflected in state`() = runTest {
        // Given: ViewModel initialized
        coEvery { quoteRepository.quoteOfTheDay() } returns testQuote

        viewModel = QuoteViewModel(quoteRepository, userPreferencesManager, quoteShareManager, transliterationFactory)
        advanceUntilIdle()

        // When: Font preference changes
        viewModel.uiState.test {
            skipItems(1) // Skip initial state

            preferencesFlow.value = defaultPreferences.copy(selectedFont = "babelstone")
            advanceUntilIdle()

            val state = awaitItem() as QuoteUiState.Success
            assertThat(state.selectedFont).isEqualTo("babelstone")

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `toggleWordByWordMode applies local override over persisted default`() = runTest {
        coEvery { quoteRepository.quoteOfTheDay() } returns testQuote

        viewModel = QuoteViewModel(quoteRepository, userPreferencesManager, quoteShareManager, transliterationFactory)
        advanceUntilIdle()

        viewModel.uiState.test {
            val initialState = awaitItem() as QuoteUiState.Success
            assertThat(initialState.wordByWordEnabled).isFalse()

            viewModel.toggleWordByWordMode()
            advanceUntilIdle()

            val locallyOverridden = awaitItem() as QuoteUiState.Success
            assertThat(locallyOverridden.wordByWordEnabled).isTrue()

            preferencesFlow.value = defaultPreferences.copy(
                selectedScript = RunicScript.YOUNGER_FUTHARK,
                wordByWordTransliterationEnabled = false
            )
            advanceUntilIdle()

            val afterPreferenceChange = awaitItem() as QuoteUiState.Success
            assertThat(afterPreferenceChange.selectedScript).isEqualTo(RunicScript.YOUNGER_FUTHARK)
            assertThat(afterPreferenceChange.wordByWordEnabled).isTrue()

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `script changes recompute word breakdown`() = runTest {
        val quote = testQuote.copy(textLatin = "thing strong")
        coEvery { quoteRepository.quoteOfTheDay() } returns quote

        viewModel = QuoteViewModel(quoteRepository, userPreferencesManager, quoteShareManager, transliterationFactory)
        advanceUntilIdle()

        viewModel.uiState.test {
            val elderState = awaitItem() as QuoteUiState.Success
            assertThat(elderState.wordBreakdown).containsExactly(
                com.po4yka.runicquotes.domain.transliteration.WordTransliterationPair(
                    sourceToken = "thing",
                    runicToken = transliterationFactory.transliterate("thing", RunicScript.ELDER_FUTHARK)
                ),
                com.po4yka.runicquotes.domain.transliteration.WordTransliterationPair(
                    sourceToken = "strong",
                    runicToken = transliterationFactory.transliterate("strong", RunicScript.ELDER_FUTHARK)
                )
            ).inOrder()

            preferencesFlow.value = defaultPreferences.copy(selectedScript = RunicScript.CIRTH)
            advanceUntilIdle()

            val cirthState = awaitItem() as QuoteUiState.Success
            assertThat(cirthState.wordBreakdown).containsExactly(
                com.po4yka.runicquotes.domain.transliteration.WordTransliterationPair(
                    sourceToken = "thing",
                    runicToken = transliterationFactory.transliterate("thing", RunicScript.CIRTH)
                ),
                com.po4yka.runicquotes.domain.transliteration.WordTransliterationPair(
                    sourceToken = "strong",
                    runicToken = transliterationFactory.transliterate("strong", RunicScript.CIRTH)
                )
            ).inOrder()
            assertThat(cirthState.wordBreakdown).isNotEqualTo(elderState.wordBreakdown)

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ==================== Random Quote Tests ====================

    @Test
    fun `getRandomQuote loads random quote successfully`() = runTest {
        // Given: ViewModel initialized with daily quote
        coEvery { quoteRepository.quoteOfTheDay() } returns testQuote
        val randomQuote = testQuote.copy(id = 99, textLatin = "Random quote")
        coEvery { quoteRepository.randomQuote() } returns randomQuote

        viewModel = QuoteViewModel(quoteRepository, userPreferencesManager, quoteShareManager, transliterationFactory)
        advanceUntilIdle()

        // When: Getting random quote
        viewModel.uiState.test {
            skipItems(1) // Skip initial state

            viewModel.getRandomQuote()
            advanceUntilIdle()

            // Then: Loading emitted
            val loadingState = awaitItem()
            assertThat(loadingState).isInstanceOf(QuoteUiState.Loading::class.java)

            // Then: Random quote loaded
            val successState = awaitItem() as QuoteUiState.Success
            assertThat(successState.quote.id).isEqualTo(99L)
            assertThat(successState.quote.textLatin).isEqualTo("Random quote")

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getRandomQuote handles null result`() = runTest {
        // Given: Repository returns null for random quote
        coEvery { quoteRepository.quoteOfTheDay() } returns testQuote
        coEvery { quoteRepository.randomQuote() } returns null

        viewModel = QuoteViewModel(quoteRepository, userPreferencesManager, quoteShareManager, transliterationFactory)
        advanceUntilIdle()

        // When: Getting random quote
        viewModel.uiState.test {
            skipItems(1)

            viewModel.getRandomQuote()
            advanceUntilIdle()

            skipItems(1) // Skip Loading
            val state = awaitItem()
            assertThat(state).isInstanceOf(QuoteUiState.Empty::class.java)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getRandomQuote handles errors`() = runTest {
        // Given: Repository throws IOException
        coEvery { quoteRepository.quoteOfTheDay() } returns testQuote
        coEvery { quoteRepository.randomQuote() } throws IOException("Random error")

        viewModel = QuoteViewModel(quoteRepository, userPreferencesManager, quoteShareManager, transliterationFactory)
        advanceUntilIdle()

        // When: Getting random quote
        viewModel.uiState.test {
            skipItems(1)

            viewModel.getRandomQuote()
            advanceUntilIdle()

            skipItems(1) // Skip Loading
            val state = awaitItem() as QuoteUiState.Error
            assertThat(state.message).isEqualTo("Random error")

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ==================== Refresh Quote Tests ====================

    @Test
    fun `refreshQuote reloads quote of the day`() = runTest {
        // Given: ViewModel initialized
        coEvery { quoteRepository.quoteOfTheDay() } returns testQuote

        viewModel = QuoteViewModel(quoteRepository, userPreferencesManager, quoteShareManager, transliterationFactory)
        advanceUntilIdle()

        // When: Refreshing quote
        viewModel.uiState.test {
            skipItems(1)

            viewModel.refreshQuote()
            advanceUntilIdle()

            // Then: Loading and Success states emitted
            val loadingState = awaitItem()
            assertThat(loadingState).isInstanceOf(QuoteUiState.Loading::class.java)

            val successState = awaitItem()
            assertThat(successState).isInstanceOf(QuoteUiState.Success::class.java)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `refreshQuote calls repository`() = runTest {
        // Given: ViewModel initialized
        coEvery { quoteRepository.quoteOfTheDay() } returns testQuote

        viewModel = QuoteViewModel(quoteRepository, userPreferencesManager, quoteShareManager, transliterationFactory)
        advanceUntilIdle()

        // When: Refreshing quote
        viewModel.refreshQuote()
        advanceUntilIdle()

        // Then: Repository is called at least twice (init + refresh)
        coVerify(atLeast = 2) { quoteRepository.quoteOfTheDay() }
    }

    // ==================== Script-Specific Tests ====================

    @Test
    fun `different scripts render correct runic text`() = runTest {
        // Given: Younger Futhark preference
        preferencesFlow.value = defaultPreferences.copy(selectedScript = RunicScript.YOUNGER_FUTHARK)
        coEvery { quoteRepository.quoteOfTheDay() } returns testQuote

        // When: ViewModel loads quote
        viewModel = QuoteViewModel(quoteRepository, userPreferencesManager, quoteShareManager, transliterationFactory)
        advanceUntilIdle()

        // Then: Repository called and correct script applied
        coVerify { quoteRepository.quoteOfTheDay() }
        viewModel.uiState.test {
            val state = awaitItem() as QuoteUiState.Success
            assertThat(state.selectedScript).isEqualTo(RunicScript.YOUNGER_FUTHARK)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Cirth script loads correctly`() = runTest {
        // Given: Cirth preference
        val cirthQuote = testQuote.copy(runicCirth = "\uE0C9\uE0C8\uE0A0\uE088")
        preferencesFlow.value = defaultPreferences.copy(selectedScript = RunicScript.CIRTH)
        coEvery { quoteRepository.quoteOfTheDay() } returns cirthQuote

        // When: ViewModel loads quote
        viewModel = QuoteViewModel(quoteRepository, userPreferencesManager, quoteShareManager, transliterationFactory)
        advanceUntilIdle()

        // Then: Cirth text is used
        viewModel.uiState.test {
            val state = awaitItem() as QuoteUiState.Success
            assertThat(state.selectedScript).isEqualTo(RunicScript.CIRTH)
            assertThat(state.runicText).isEqualTo("\uE0C9\uE0C8\uE0A0\uE088")
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ==================== Edge Cases ====================

    @Test
    fun `handles quote with null runic fields`() = runTest {
        // Given: Quote with null runic fields
        val quoteWithNulls = testQuote.copy(
            runicElder = null,
            runicYounger = null,
            runicCirth = null
        )
        coEvery { quoteRepository.quoteOfTheDay() } returns quoteWithNulls

        // When: ViewModel loads quote
        viewModel = QuoteViewModel(quoteRepository, userPreferencesManager, quoteShareManager, transliterationFactory)
        advanceUntilIdle()

        // Then: Runic text is generated from domain transliteration
        viewModel.uiState.test {
            val state = awaitItem() as QuoteUiState.Success
            assertThat(state.runicText).isNotEmpty() // Transliterated text
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `multiple rapid preference changes are handled correctly`() = runTest {
        // Given: ViewModel initialized
        coEvery { quoteRepository.quoteOfTheDay() } returns testQuote

        viewModel = QuoteViewModel(quoteRepository, userPreferencesManager, quoteShareManager, transliterationFactory)
        advanceUntilIdle()

        // When: Multiple rapid preference changes
        viewModel.uiState.test {
            // Initial state
            val initial = awaitItem()
            assertThat(initial).isInstanceOf(QuoteUiState.Success::class.java)

            // First change
            preferencesFlow.value = defaultPreferences.copy(selectedScript = RunicScript.YOUNGER_FUTHARK)
            advanceUntilIdle()
            val state1 = awaitItem() as QuoteUiState.Success
            assertThat(state1.selectedScript).isEqualTo(RunicScript.YOUNGER_FUTHARK)

            // Second change
            preferencesFlow.value = defaultPreferences.copy(selectedScript = RunicScript.CIRTH)
            advanceUntilIdle()
            val state2 = awaitItem() as QuoteUiState.Success
            assertThat(state2.selectedScript).isEqualTo(RunicScript.CIRTH)

            // Third change
            preferencesFlow.value = defaultPreferences.copy(selectedScript = RunicScript.ELDER_FUTHARK)
            advanceUntilIdle()
            val finalState = awaitItem() as QuoteUiState.Success
            assertThat(finalState.selectedScript).isEqualTo(RunicScript.ELDER_FUTHARK)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `state transitions follow correct order from loading to success`() = runTest {
        // Given: Repository will return a quote after delay
        coEvery { quoteRepository.quoteOfTheDay() } returns testQuote

        // When: ViewModel is created
        viewModel = QuoteViewModel(quoteRepository, userPreferencesManager, quoteShareManager, transliterationFactory)

        // Then: States transition in expected order
        viewModel.uiState.test {
            val loadingState = awaitItem()
            assertThat(loadingState).isInstanceOf(QuoteUiState.Loading::class.java)

            advanceUntilIdle()

            val successState = awaitItem()
            assertThat(successState).isInstanceOf(QuoteUiState.Success::class.java)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `state transitions from success to loading to success on refresh`() = runTest {
        // Given: ViewModel initialized with quote
        coEvery { quoteRepository.quoteOfTheDay() } returns testQuote

        viewModel = QuoteViewModel(quoteRepository, userPreferencesManager, quoteShareManager, transliterationFactory)
        advanceUntilIdle()

        // When: Refreshing
        viewModel.uiState.test {
            val initialSuccess = awaitItem()
            assertThat(initialSuccess).isInstanceOf(QuoteUiState.Success::class.java)

            viewModel.refreshQuote()
            advanceUntilIdle()

            val loading = awaitItem()
            assertThat(loading).isInstanceOf(QuoteUiState.Loading::class.java)

            val finalSuccess = awaitItem()
            assertThat(finalSuccess).isInstanceOf(QuoteUiState.Success::class.java)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `toggleFavorite updates current quote and delegates to repository`() = runTest {
        coEvery { quoteRepository.quoteOfTheDay() } returns testQuote
        coEvery { quoteRepository.toggleFavorite(any(), any()) } returns Unit

        viewModel = QuoteViewModel(quoteRepository, userPreferencesManager, quoteShareManager, transliterationFactory)
        advanceUntilIdle()

        viewModel.toggleFavorite()
        advanceUntilIdle()

        coVerify { quoteRepository.toggleFavorite(testQuote.id, true) }
        val state = viewModel.uiState.value as QuoteUiState.Success
        assertThat(state.quote.isFavorite).isTrue()
    }

    @Test
    fun `share and copy actions delegate to share manager`() = runTest {
        coEvery { quoteRepository.quoteOfTheDay() } returns testQuote
        coEvery { quoteShareManager.shareQuoteAsImage(any(), any(), any(), any(), any()) } returns true

        viewModel = QuoteViewModel(quoteRepository, userPreferencesManager, quoteShareManager, transliterationFactory)
        advanceUntilIdle()

        viewModel.shareQuoteAsImage()
        advanceUntilIdle()
        viewModel.shareQuoteText()
        viewModel.copyQuoteText()

        coVerify {
            quoteShareManager.shareQuoteAsImage(
                runicText = "\u16CF\u16D6\u16CA\u16CF",
                latinText = testQuote.textLatin,
                author = testQuote.author,
                template = ShareTemplate.CARD,
                appearance = ShareAppearance.DARK
            )
        }
        io.mockk.verify { quoteShareManager.shareQuoteText(testQuote.textLatin, testQuote.author) }
        io.mockk.verify { quoteShareManager.copyQuoteToClipboard(testQuote.textLatin, testQuote.author) }
    }

    @Test
    fun `deleteQuote removes current quote and loads next daily quote`() = runTest {
        val nextQuote = testQuote.copy(id = 2L, textLatin = "Next quote")
        coEvery { quoteRepository.quoteOfTheDay() } returns testQuote andThen nextQuote
        coEvery { quoteRepository.deleteUserQuote(testQuote.id) } returns Unit

        viewModel = QuoteViewModel(quoteRepository, userPreferencesManager, quoteShareManager, transliterationFactory)
        advanceUntilIdle()

        viewModel.deleteQuote()
        advanceUntilIdle()

        coVerify { quoteRepository.deleteUserQuote(testQuote.id) }
        assertThat((viewModel.uiState.value as QuoteUiState.Success).quote).isEqualTo(nextQuote)
    }

    @Test
    fun `toggleTransliterationVisibility updates persisted preference`() = runTest {
        coEvery { quoteRepository.quoteOfTheDay() } returns testQuote
        coEvery { userPreferencesManager.updateShowTransliteration(false) } returns Unit

        viewModel = QuoteViewModel(quoteRepository, userPreferencesManager, quoteShareManager, transliterationFactory)
        advanceUntilIdle()

        viewModel.toggleTransliterationVisibility()
        advanceUntilIdle()

        coVerify { userPreferencesManager.updateShowTransliteration(false) }
    }

    @Test
    fun `latest available translation is preferred over stored transliteration`() = runTest {
        val translationRepository = mockk<TranslationRepository>()
        val translatedQuote = testQuote.copy(runicElder = "stale")
        val cachedTranslation = TranslationResult(
            sourceText = translatedQuote.textLatin,
            script = RunicScript.ELDER_FUTHARK,
            fidelity = TranslationFidelity.READABLE,
            historicalStage = HistoricalStage.OLD_NORSE,
            normalizedForm = "test",
            diplomaticForm = "test",
            glyphOutput = "cached elder",
            resolutionStatus = TranslationResolutionStatus.RECONSTRUCTED,
            confidence = 0.88f,
            notes = listOf("cached"),
            unresolvedTokens = emptyList(),
            provenance = listOf(
                TranslationProvenanceEntry(
                    sourceId = "cache",
                    label = "Cache",
                    role = "test",
                    license = "none",
                    detail = "preferred"
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
        coEvery { quoteRepository.quoteOfTheDay() } returns translatedQuote
        coEvery { translationRepository.getLatestAvailableTranslation(1L, RunicScript.ELDER_FUTHARK) } returns
            cachedTranslation
        coEvery { translationRepository.getLatestAvailableTranslation(1L, RunicScript.YOUNGER_FUTHARK) } returns null
        coEvery { translationRepository.getLatestAvailableTranslation(1L, RunicScript.CIRTH) } returns null

        viewModel = QuoteViewModel(
            quoteRepository,
            userPreferencesManager,
            quoteShareManager,
            transliterationFactory,
            translationRepository
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value as QuoteUiState.Success
        assertThat(state.runicText).isEqualTo("cached elder")
        assertThat(state.wordBreakdown).containsExactly(
            com.po4yka.runicquotes.domain.transliteration.WordTransliterationPair(
                sourceToken = "Test",
                runicToken = "cached"
            )
        )
    }
}
