package com.po4yka.runicquotes.ui.screens.quote

import app.cash.turbine.test
import com.po4yka.runicquotes.data.preferences.UserPreferences
import com.po4yka.runicquotes.data.preferences.UserPreferencesManager
import com.po4yka.runicquotes.data.repository.QuoteRepository
import com.po4yka.runicquotes.domain.model.Quote
import com.po4yka.runicquotes.domain.model.RunicScript
import com.po4yka.runicquotes.util.QuoteShareManager
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    private lateinit var viewModel: QuoteViewModel

    private val testDispatcher = StandardTestDispatcher()

    private val testQuote = Quote(
        id = 1,
        textLatin = "Test quote",
        author = "Test Author",
        runicElder = "ᛏᛖᛋᛏ",
        runicYounger = "ᛏᛖᛋᛏ",
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

        // Set up preferences flow
        preferencesFlow = MutableStateFlow(defaultPreferences)
        every { userPreferencesManager.userPreferencesFlow } returns preferencesFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ==================== Initialization Tests ====================

    @Test
    fun `viewModel initializes with Loading state`() = runTest {
        // Given: Repository will return a quote
        coEvery { quoteRepository.quoteOfTheDay(any()) } returns testQuote

        // When: ViewModel is created
        viewModel = QuoteViewModel(quoteRepository, userPreferencesManager, quoteShareManager)

        // Then: Initial state is Loading
        viewModel.uiState.test {
            val initialState = awaitItem()
            assertTrue(initialState is QuoteUiState.Loading)

            // Advance to allow initialization to complete
            advanceUntilIdle()

            // Should transition to Success
            val successState = awaitItem()
            assertTrue(successState is QuoteUiState.Success)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `viewModel loads quote of the day on initialization`() = runTest {
        // Given: Repository returns a quote
        coEvery { quoteRepository.quoteOfTheDay(RunicScript.ELDER_FUTHARK) } returns testQuote

        // When: ViewModel is created
        viewModel = QuoteViewModel(quoteRepository, userPreferencesManager, quoteShareManager)
        advanceUntilIdle()

        // Then: Quote is loaded
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is QuoteUiState.Success)
            assertEquals(testQuote, (state as QuoteUiState.Success).quote)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ==================== Success State Tests ====================

    @Test
    fun `successful quote load emits Success state with correct data`() = runTest {
        // Given: Repository returns a quote
        coEvery { quoteRepository.quoteOfTheDay(any()) } returns testQuote

        // When: ViewModel loads quote
        viewModel = QuoteViewModel(quoteRepository, userPreferencesManager, quoteShareManager)
        advanceUntilIdle()

        // Then: Success state contains all expected data
        viewModel.uiState.test {
            val state = awaitItem() as QuoteUiState.Success

            assertEquals(testQuote.id, state.quote.id)
            assertEquals(testQuote.textLatin, state.quote.textLatin)
            assertEquals(testQuote.author, state.quote.author)
            assertEquals("ᛏᛖᛋᛏ", state.runicText)
            assertEquals(RunicScript.ELDER_FUTHARK, state.selectedScript)
            assertEquals("noto", state.selectedFont)
            assertTrue(state.showTransliteration)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Success state uses domain extension for runic text`() = runTest {
        // Given: Quote with Elder Futhark text
        val quote = testQuote.copy(runicElder = "ᚱᚢᚾᛖ")
        coEvery { quoteRepository.quoteOfTheDay(any()) } returns quote

        // When: ViewModel loads quote
        viewModel = QuoteViewModel(quoteRepository, userPreferencesManager, quoteShareManager)
        advanceUntilIdle()

        // Then: Runic text from domain extension is used
        viewModel.uiState.test {
            val state = awaitItem() as QuoteUiState.Success
            assertEquals("ᚱᚢᚾᛖ", state.runicText)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ==================== Empty State Tests ====================

    @Test
    fun `repository returning null emits Empty state`() = runTest {
        // Given: Repository returns null
        coEvery { quoteRepository.quoteOfTheDay(any()) } returns null

        // When: ViewModel loads quote
        viewModel = QuoteViewModel(quoteRepository, userPreferencesManager, quoteShareManager)
        advanceUntilIdle()

        // Then: Empty state is emitted
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is QuoteUiState.Empty)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ==================== Error State Tests ====================

    @Test
    fun `repository throwing exception emits Error state`() = runTest {
        // Given: Repository throws IOException
        coEvery { quoteRepository.quoteOfTheDay(any()) } throws IOException("Database error")

        // When: ViewModel loads quote
        viewModel = QuoteViewModel(quoteRepository, userPreferencesManager, quoteShareManager)
        advanceUntilIdle()

        // Then: Error state with message is emitted
        viewModel.uiState.test {
            val state = awaitItem() as QuoteUiState.Error
            assertEquals("Database error", state.message)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `error state handles exception without message`() = runTest {
        // Given: IOException without message
        coEvery { quoteRepository.quoteOfTheDay(any()) } throws IOException()

        // When: ViewModel loads quote
        viewModel = QuoteViewModel(quoteRepository, userPreferencesManager, quoteShareManager)
        advanceUntilIdle()

        // Then: Generic error message is used
        viewModel.uiState.test {
            val state = awaitItem() as QuoteUiState.Error
            assertEquals("Failed to load quote", state.message)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ==================== Preferences Change Tests ====================

    @Test
    fun `preferences change updates script without loading flash`() = runTest {
        // Given: ViewModel initialized with one quote
        coEvery { quoteRepository.quoteOfTheDay(RunicScript.ELDER_FUTHARK) } returns testQuote

        viewModel = QuoteViewModel(quoteRepository, userPreferencesManager, quoteShareManager)
        advanceUntilIdle()

        // When: Preferences change to different script
        viewModel.uiState.test {
            skipItems(1) // Skip initial success state

            preferencesFlow.value = defaultPreferences.copy(selectedScript = RunicScript.YOUNGER_FUTHARK)
            advanceUntilIdle()

            // Then: State updates immediately without loading
            val successState = awaitItem() as QuoteUiState.Success
            assertEquals(RunicScript.YOUNGER_FUTHARK, successState.selectedScript)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `font preference change is reflected in state`() = runTest {
        // Given: ViewModel initialized
        coEvery { quoteRepository.quoteOfTheDay(any()) } returns testQuote

        viewModel = QuoteViewModel(quoteRepository, userPreferencesManager, quoteShareManager)
        advanceUntilIdle()

        // When: Font preference changes
        viewModel.uiState.test {
            skipItems(1) // Skip initial state

            preferencesFlow.value = defaultPreferences.copy(selectedFont = "babelstone")
            advanceUntilIdle()

            val state = awaitItem() as QuoteUiState.Success
            assertEquals("babelstone", state.selectedFont)

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ==================== Random Quote Tests ====================

    @Test
    fun `getRandomQuote loads random quote successfully`() = runTest {
        // Given: ViewModel initialized with daily quote
        coEvery { quoteRepository.quoteOfTheDay(any()) } returns testQuote
        val randomQuote = testQuote.copy(id = 99, textLatin = "Random quote")
        coEvery { quoteRepository.randomQuote(any()) } returns randomQuote

        viewModel = QuoteViewModel(quoteRepository, userPreferencesManager, quoteShareManager)
        advanceUntilIdle()

        // When: Getting random quote
        viewModel.uiState.test {
            skipItems(1) // Skip initial state

            viewModel.getRandomQuote()
            advanceUntilIdle()

            // Then: Loading emitted
            val loadingState = awaitItem()
            assertTrue(loadingState is QuoteUiState.Loading)

            // Then: Random quote loaded
            val successState = awaitItem() as QuoteUiState.Success
            assertEquals(99L, successState.quote.id)
            assertEquals("Random quote", successState.quote.textLatin)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getRandomQuote handles null result`() = runTest {
        // Given: Repository returns null for random quote
        coEvery { quoteRepository.quoteOfTheDay(any()) } returns testQuote
        coEvery { quoteRepository.randomQuote(any()) } returns null

        viewModel = QuoteViewModel(quoteRepository, userPreferencesManager, quoteShareManager)
        advanceUntilIdle()

        // When: Getting random quote
        viewModel.uiState.test {
            skipItems(1)

            viewModel.getRandomQuote()
            advanceUntilIdle()

            skipItems(1) // Skip Loading
            val state = awaitItem()
            assertTrue(state is QuoteUiState.Empty)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getRandomQuote handles errors`() = runTest {
        // Given: Repository throws IOException
        coEvery { quoteRepository.quoteOfTheDay(any()) } returns testQuote
        coEvery { quoteRepository.randomQuote(any()) } throws IOException("Random error")

        viewModel = QuoteViewModel(quoteRepository, userPreferencesManager, quoteShareManager)
        advanceUntilIdle()

        // When: Getting random quote
        viewModel.uiState.test {
            skipItems(1)

            viewModel.getRandomQuote()
            advanceUntilIdle()

            skipItems(1) // Skip Loading
            val state = awaitItem() as QuoteUiState.Error
            assertEquals("Random error", state.message)

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ==================== Refresh Quote Tests ====================

    @Test
    fun `refreshQuote reloads quote of the day`() = runTest {
        // Given: ViewModel initialized
        coEvery { quoteRepository.quoteOfTheDay(any()) } returns testQuote

        viewModel = QuoteViewModel(quoteRepository, userPreferencesManager, quoteShareManager)
        advanceUntilIdle()

        // When: Refreshing quote
        viewModel.uiState.test {
            skipItems(1)

            viewModel.refreshQuote()
            advanceUntilIdle()

            // Then: Loading and Success states emitted
            val loadingState = awaitItem()
            assertTrue(loadingState is QuoteUiState.Loading)

            val successState = awaitItem()
            assertTrue(successState is QuoteUiState.Success)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `refreshQuote calls repository`() = runTest {
        // Given: ViewModel initialized
        coEvery { quoteRepository.quoteOfTheDay(any()) } returns testQuote

        viewModel = QuoteViewModel(quoteRepository, userPreferencesManager, quoteShareManager)
        advanceUntilIdle()

        // When: Refreshing quote
        viewModel.refreshQuote()
        advanceUntilIdle()

        // Then: Repository is called at least twice (init + refresh)
        coVerify(atLeast = 2) { quoteRepository.quoteOfTheDay(any()) }
    }

    // ==================== Script-Specific Tests ====================

    @Test
    fun `different scripts use correct repository parameter`() = runTest {
        // Given: Younger Futhark preference
        preferencesFlow.value = defaultPreferences.copy(selectedScript = RunicScript.YOUNGER_FUTHARK)
        coEvery { quoteRepository.quoteOfTheDay(RunicScript.YOUNGER_FUTHARK) } returns testQuote

        // When: ViewModel loads quote
        viewModel = QuoteViewModel(quoteRepository, userPreferencesManager, quoteShareManager)
        advanceUntilIdle()

        // Then: Repository called with correct script
        coVerify { quoteRepository.quoteOfTheDay(RunicScript.YOUNGER_FUTHARK) }
    }

    @Test
    fun `Cirth script loads correctly`() = runTest {
        // Given: Cirth preference
        val cirthQuote = testQuote.copy(runicCirth = "\uE0C9\uE0C8\uE0A0\uE088")
        preferencesFlow.value = defaultPreferences.copy(selectedScript = RunicScript.CIRTH)
        coEvery { quoteRepository.quoteOfTheDay(RunicScript.CIRTH) } returns cirthQuote

        // When: ViewModel loads quote
        viewModel = QuoteViewModel(quoteRepository, userPreferencesManager, quoteShareManager)
        advanceUntilIdle()

        // Then: Cirth text is used
        viewModel.uiState.test {
            val state = awaitItem() as QuoteUiState.Success
            assertEquals(RunicScript.CIRTH, state.selectedScript)
            assertEquals("\uE0C9\uE0C8\uE0A0\uE088", state.runicText)
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
        coEvery { quoteRepository.quoteOfTheDay(any()) } returns quoteWithNulls

        // When: ViewModel loads quote
        viewModel = QuoteViewModel(quoteRepository, userPreferencesManager, quoteShareManager)
        advanceUntilIdle()

        // Then: Runic text is generated from domain transliteration
        viewModel.uiState.test {
            val state = awaitItem() as QuoteUiState.Success
            assertTrue(state.runicText.isNotEmpty()) // Transliterated text
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `multiple rapid preference changes are handled correctly`() = runTest {
        // Given: ViewModel initialized
        coEvery { quoteRepository.quoteOfTheDay(any()) } returns testQuote

        viewModel = QuoteViewModel(quoteRepository, userPreferencesManager, quoteShareManager)
        advanceUntilIdle()

        // When: Multiple rapid preference changes
        viewModel.uiState.test {
            // Initial state
            val initial = awaitItem()
            assertTrue(initial is QuoteUiState.Success)

            // First change
            preferencesFlow.value = defaultPreferences.copy(selectedScript = RunicScript.YOUNGER_FUTHARK)
            advanceUntilIdle()
            val state1 = awaitItem() as QuoteUiState.Success
            assertEquals(RunicScript.YOUNGER_FUTHARK, state1.selectedScript)

            // Second change
            preferencesFlow.value = defaultPreferences.copy(selectedScript = RunicScript.CIRTH)
            advanceUntilIdle()
            val state2 = awaitItem() as QuoteUiState.Success
            assertEquals(RunicScript.CIRTH, state2.selectedScript)

            // Third change
            preferencesFlow.value = defaultPreferences.copy(selectedScript = RunicScript.ELDER_FUTHARK)
            advanceUntilIdle()
            val finalState = awaitItem() as QuoteUiState.Success
            assertEquals(RunicScript.ELDER_FUTHARK, finalState.selectedScript)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `state transitions follow correct order from loading to success`() = runTest {
        // Given: Repository will return a quote after delay
        coEvery { quoteRepository.quoteOfTheDay(any()) } returns testQuote

        // When: ViewModel is created
        viewModel = QuoteViewModel(quoteRepository, userPreferencesManager, quoteShareManager)

        // Then: States transition in expected order
        viewModel.uiState.test {
            val loadingState = awaitItem()
            assertTrue("First state should be Loading", loadingState is QuoteUiState.Loading)

            advanceUntilIdle()

            val successState = awaitItem()
            assertTrue("Second state should be Success", successState is QuoteUiState.Success)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `state transitions from success to loading to success on refresh`() = runTest {
        // Given: ViewModel initialized with quote
        coEvery { quoteRepository.quoteOfTheDay(any()) } returns testQuote

        viewModel = QuoteViewModel(quoteRepository, userPreferencesManager, quoteShareManager)
        advanceUntilIdle()

        // When: Refreshing
        viewModel.uiState.test {
            val initialSuccess = awaitItem()
            assertTrue("Should start in Success", initialSuccess is QuoteUiState.Success)

            viewModel.refreshQuote()
            advanceUntilIdle()

            val loading = awaitItem()
            assertTrue("Should transition to Loading", loading is QuoteUiState.Loading)

            val finalSuccess = awaitItem()
            assertTrue("Should return to Success", finalSuccess is QuoteUiState.Success)

            cancelAndIgnoreRemainingEvents()
        }
    }
}
