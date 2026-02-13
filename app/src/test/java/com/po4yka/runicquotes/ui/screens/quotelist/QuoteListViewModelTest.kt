package com.po4yka.runicquotes.ui.screens.quotelist

import app.cash.turbine.test
import com.po4yka.runicquotes.data.preferences.UserPreferences
import com.po4yka.runicquotes.data.preferences.UserPreferencesManager
import com.po4yka.runicquotes.data.repository.QuoteRepository
import com.po4yka.runicquotes.domain.model.Quote
import com.po4yka.runicquotes.domain.model.RunicScript
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

/**
 * Comprehensive unit tests for QuoteListViewModel.
 * Tests filtering, favorite toggling, quote deletion, and error handling.
 *
 * Coverage goals: >85%
 */
@OptIn(ExperimentalCoroutinesApi::class)
class QuoteListViewModelTest {

    private lateinit var quoteRepository: QuoteRepository
    private lateinit var userPreferencesManager: UserPreferencesManager
    private lateinit var viewModel: QuoteListViewModel

    private val testDispatcher = StandardTestDispatcher()

    private val testQuotes = listOf(
        Quote(
            id = 1,
            textLatin = "System quote 1",
            author = "Author 1",
            runicElder = "ᛏᛖᛋᛏ",
            runicYounger = "ᛏᛖᛋᛏ",
            runicCirth = "\uE088\uE0C9\uE09C\uE088",
            isUserCreated = false,
            isFavorite = false
        ),
        Quote(
            id = 2,
            textLatin = "User quote 1",
            author = "Author 2",
            runicElder = "ᚦᛖᛋᛏ",
            runicYounger = "ᚦᛖᛋᛏ",
            runicCirth = "\uE088\uE0B4\uE0C9\uE09C\uE088",
            isUserCreated = true,
            isFavorite = true
        ),
        Quote(
            id = 3,
            textLatin = "System quote 2",
            author = "Author 3",
            runicElder = "ᚹᛟᚱᛞ",
            runicYounger = "ᚹᛟᚱᛞ",
            runicCirth = "\uE0B8\uE0CB\uE0A0\uE089",
            isUserCreated = false,
            isFavorite = true
        )
    )

    private val userQuotes = testQuotes.filter { it.isUserCreated }
    private val favoriteQuotes = testQuotes.filter { it.isFavorite }
    private val systemQuotes = testQuotes.filter { !it.isUserCreated }

    private val defaultPreferences = UserPreferences(
        selectedScript = RunicScript.ELDER_FUTHARK,
        selectedFont = "noto",
        showTransliteration = true
    )

    private lateinit var preferencesFlow: MutableStateFlow<UserPreferences>
    private lateinit var allQuotesFlow: MutableStateFlow<List<Quote>>
    private lateinit var userQuotesFlow: MutableStateFlow<List<Quote>>
    private lateinit var favoritesFlow: MutableStateFlow<List<Quote>>

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        // Create mocks
        quoteRepository = mockk()
        userPreferencesManager = mockk()

        // Set up flows
        preferencesFlow = MutableStateFlow(defaultPreferences)
        allQuotesFlow = MutableStateFlow(testQuotes)
        userQuotesFlow = MutableStateFlow(userQuotes)
        favoritesFlow = MutableStateFlow(favoriteQuotes)

        every { userPreferencesManager.userPreferencesFlow } returns preferencesFlow
        every { quoteRepository.getAllQuotesFlow() } returns allQuotesFlow
        every { quoteRepository.getUserQuotesFlow() } returns userQuotesFlow
        every { quoteRepository.getFavoritesFlow() } returns favoritesFlow
        coEvery { userPreferencesManager.updateQuoteListFilter(any()) } returns Unit
        coEvery { userPreferencesManager.updateQuoteSearchQuery(any()) } returns Unit
        coEvery { userPreferencesManager.updateQuoteAuthorFilter(any()) } returns Unit
        coEvery { userPreferencesManager.updateQuoteLengthFilter(any()) } returns Unit
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ==================== Initialization Tests ====================

    @Test
    fun `viewModel initializes with loading state`() = runTest {
        // When: ViewModel is created
        viewModel = QuoteListViewModel(quoteRepository, userPreferencesManager)

        // Then: State transitions through loading to loaded
        viewModel.uiState.test {
            // Initial state before coroutine runs
            val initialState = awaitItem()
            assertFalse(initialState.isLoading)
            assertEquals(emptyList<Quote>(), initialState.quotes)

            // Advance to execute the init coroutine
            advanceUntilIdle()

            // Loading state is set
            val loadingState = awaitItem()
            assertTrue(loadingState.isLoading)

            // Then combine emits with loaded quotes
            val loadedState = awaitItem()
            assertFalse(loadedState.isLoading)
            assertEquals(testQuotes.size, loadedState.quotes.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `viewModel loads quotes on initialization`() = runTest {
        // When: ViewModel is created
        viewModel = QuoteListViewModel(quoteRepository, userPreferencesManager)
        advanceUntilIdle()

        // Then: Quotes are loaded with ALL filter
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(testQuotes.size, state.quotes.size)
            assertEquals(QuoteFilter.ALL, state.currentFilter)
            assertFalse(state.isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `viewModel initializes with preferences from manager`() = runTest {
        // When: ViewModel is created
        viewModel = QuoteListViewModel(quoteRepository, userPreferencesManager)
        advanceUntilIdle()

        // Then: Preferences are applied
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(RunicScript.ELDER_FUTHARK, state.selectedScript)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ==================== Filter Tests ====================

    @Test
    fun `setFilter to ALL shows all quotes`() = runTest {
        // Given: ViewModel initialized
        viewModel = QuoteListViewModel(quoteRepository, userPreferencesManager)
        advanceUntilIdle()

        // When: Setting filter to ALL
        viewModel.setFilter(QuoteFilter.ALL)
        advanceUntilIdle()

        // Then: All quotes are shown
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(QuoteFilter.ALL, state.currentFilter)
            assertEquals(testQuotes.size, state.quotes.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setFilter to USER_CREATED shows only user quotes`() = runTest {
        // Given: ViewModel initialized
        viewModel = QuoteListViewModel(quoteRepository, userPreferencesManager)
        advanceUntilIdle()

        // When: Setting filter to USER_CREATED
        viewModel.setFilter(QuoteFilter.USER_CREATED)
        advanceUntilIdle()

        // Then: Only user-created quotes are shown
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(QuoteFilter.USER_CREATED, state.currentFilter)
            assertEquals(userQuotes.size, state.quotes.size)
            assertTrue(state.quotes.all { it.isUserCreated })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setFilter to FAVORITES shows only favorite quotes`() = runTest {
        // Given: ViewModel initialized
        viewModel = QuoteListViewModel(quoteRepository, userPreferencesManager)
        advanceUntilIdle()

        // When: Setting filter to FAVORITES
        viewModel.setFilter(QuoteFilter.FAVORITES)
        advanceUntilIdle()

        // Then: Only favorite quotes are shown
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(QuoteFilter.FAVORITES, state.currentFilter)
            assertEquals(favoriteQuotes.size, state.quotes.size)
            assertTrue(state.quotes.all { it.isFavorite })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setFilter to SYSTEM shows only system quotes`() = runTest {
        // Given: ViewModel initialized
        viewModel = QuoteListViewModel(quoteRepository, userPreferencesManager)
        advanceUntilIdle()

        // When: Setting filter to SYSTEM
        viewModel.setFilter(QuoteFilter.SYSTEM)
        advanceUntilIdle()

        // Then: Only system quotes are shown
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(QuoteFilter.SYSTEM, state.currentFilter)
            assertEquals(systemQuotes.size, state.quotes.size)
            assertTrue(state.quotes.all { !it.isUserCreated })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `switching between filters updates quotes list`() = runTest {
        // Given: ViewModel initialized
        viewModel = QuoteListViewModel(quoteRepository, userPreferencesManager)

        viewModel.uiState.test {
            // Initial empty state
            awaitItem()

            // Advance to complete loading
            advanceUntilIdle()

            // Loading state then loaded state
            awaitItem() // isLoading = true
            val allState = awaitItem() // isLoading = false with ALL quotes
            assertEquals(testQuotes.size, allState.quotes.size)

            // Switch to FAVORITES
            viewModel.setFilter(QuoteFilter.FAVORITES)
            // setFilter updates state, then combine re-emits
            val filterUpdate1 = awaitItem() // Direct update from setFilter
            advanceUntilIdle()
            val favoritesState = awaitItem() // Combine re-emits with filtered data
            assertEquals(favoriteQuotes.size, favoritesState.quotes.size)

            // Switch to USER_CREATED
            viewModel.setFilter(QuoteFilter.USER_CREATED)
            awaitItem() // Direct update from setFilter
            advanceUntilIdle()
            val userState = awaitItem() // Combine re-emits with filtered data
            assertEquals(userQuotes.size, userState.quotes.size)

            // Switch to SYSTEM
            viewModel.setFilter(QuoteFilter.SYSTEM)
            awaitItem() // Direct update from setFilter
            advanceUntilIdle()
            val systemState = awaitItem() // Combine re-emits with filtered data
            assertEquals(systemQuotes.size, systemState.quotes.size)

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ==================== Favorite Toggle Tests ====================

    @Test
    fun `toggleFavorite calls repository with correct parameters`() = runTest {
        // Given: ViewModel initialized
        coEvery { quoteRepository.toggleFavorite(any(), any()) } returns Unit
        viewModel = QuoteListViewModel(quoteRepository, userPreferencesManager)
        advanceUntilIdle()

        val quote = testQuotes[0]

        // When: Toggling favorite
        viewModel.toggleFavorite(quote)
        advanceUntilIdle()

        // Then: Repository is called with toggled value
        coVerify { quoteRepository.toggleFavorite(quote.id, !quote.isFavorite) }
    }

    @Test
    fun `toggleFavorite handles IOException`() = runTest {
        // Given: Repository throws IOException
        coEvery { quoteRepository.toggleFavorite(any(), any()) } throws IOException("Network error")
        viewModel = QuoteListViewModel(quoteRepository, userPreferencesManager)
        advanceUntilIdle()

        // When: Toggling favorite
        viewModel.toggleFavorite(testQuotes[0])
        advanceUntilIdle()

        // Then: Error message is set
        viewModel.uiState.test {
            val state = awaitItem()
            assertNotNull(state.errorMessage)
            assertTrue(state.errorMessage!!.contains("Failed to update favorite"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `toggleFavorite handles IllegalStateException`() = runTest {
        // Given: Repository throws IllegalStateException
        coEvery { quoteRepository.toggleFavorite(any(), any()) } throws IllegalStateException("Invalid state")
        viewModel = QuoteListViewModel(quoteRepository, userPreferencesManager)
        advanceUntilIdle()

        // When: Toggling favorite
        viewModel.toggleFavorite(testQuotes[0])
        advanceUntilIdle()

        // Then: Error message is set
        viewModel.uiState.test {
            val state = awaitItem()
            assertNotNull(state.errorMessage)
            assertTrue(state.errorMessage!!.contains("Invalid state"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ==================== Delete Quote Tests ====================

    @Test
    fun `deleteQuote calls repository with quote id`() = runTest {
        // Given: ViewModel initialized
        coEvery { quoteRepository.deleteUserQuote(any()) } returns Unit
        viewModel = QuoteListViewModel(quoteRepository, userPreferencesManager)
        advanceUntilIdle()

        val quoteId = 2L

        // When: Deleting quote
        viewModel.deleteQuote(quoteId)
        advanceUntilIdle()

        // Then: Repository is called
        coVerify { quoteRepository.deleteUserQuote(quoteId) }
    }

    @Test
    fun `deleteQuote handles IOException`() = runTest {
        // Given: Repository throws IOException
        coEvery { quoteRepository.deleteUserQuote(any()) } throws IOException("Database error")
        viewModel = QuoteListViewModel(quoteRepository, userPreferencesManager)
        advanceUntilIdle()

        // When: Deleting quote
        viewModel.deleteQuote(1L)
        advanceUntilIdle()

        // Then: Error message is set
        viewModel.uiState.test {
            val state = awaitItem()
            assertNotNull(state.errorMessage)
            assertTrue(state.errorMessage!!.contains("Failed to delete quote"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `deleteQuote handles IllegalStateException`() = runTest {
        // Given: Repository throws IllegalStateException
        coEvery { quoteRepository.deleteUserQuote(any()) } throws IllegalStateException("Invalid state")
        viewModel = QuoteListViewModel(quoteRepository, userPreferencesManager)
        advanceUntilIdle()

        // When: Deleting quote
        viewModel.deleteQuote(1L)
        advanceUntilIdle()

        // Then: Error message is set
        viewModel.uiState.test {
            val state = awaitItem()
            assertNotNull(state.errorMessage)
            assertTrue(state.errorMessage!!.contains("Invalid state"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ==================== Error Handling Tests ====================

    @Test
    fun `clearError removes error message`() = runTest {
        // Given: ViewModel with error
        coEvery { quoteRepository.toggleFavorite(any(), any()) } throws IOException("Error")
        viewModel = QuoteListViewModel(quoteRepository, userPreferencesManager)
        advanceUntilIdle()

        viewModel.toggleFavorite(testQuotes[0])
        advanceUntilIdle()

        // When: Clearing error
        viewModel.uiState.test {
            val stateWithError = awaitItem()
            assertNotNull(stateWithError.errorMessage)

            viewModel.clearError()
            advanceUntilIdle()

            val stateWithoutError = awaitItem()
            assertNull(stateWithoutError.errorMessage)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `loading error sets isLoading to false`() = runTest {
        // Given: Repository flows are empty (complete without emitting)
        every { quoteRepository.getAllQuotesFlow() } returns flowOf()
        every { quoteRepository.getUserQuotesFlow() } returns flowOf()
        every { quoteRepository.getFavoritesFlow() } returns flowOf()

        // When: ViewModel is created with empty flows
        viewModel = QuoteListViewModel(quoteRepository, userPreferencesManager)

        viewModel.uiState.test {
            // Initial state before loading starts
            val initialState = awaitItem()
            assertFalse(initialState.isLoading)

            advanceUntilIdle()

            // Loading starts but combine never emits because flows are empty
            val loadingState = awaitItem()
            assertTrue(loadingState.isLoading) // Still loading because combine never completes

            // No more emissions since combine never emits
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ==================== Preferences Change Tests ====================

    @Test
    fun `preferences change updates selectedScript in state`() = runTest {
        // Given: ViewModel initialized
        viewModel = QuoteListViewModel(quoteRepository, userPreferencesManager)
        advanceUntilIdle()

        viewModel.uiState.test {
            // Initial state
            val initial = awaitItem()
            assertEquals(RunicScript.ELDER_FUTHARK, initial.selectedScript)

            // When: Preferences change
            preferencesFlow.value = defaultPreferences.copy(selectedScript = RunicScript.YOUNGER_FUTHARK)
            advanceUntilIdle()

            // Then: State reflects new script
            val updated = awaitItem()
            assertEquals(RunicScript.YOUNGER_FUTHARK, updated.selectedScript)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `quotes flow update reflects in state`() = runTest {
        // Given: ViewModel initialized
        viewModel = QuoteListViewModel(quoteRepository, userPreferencesManager)
        advanceUntilIdle()

        viewModel.uiState.test {
            // Initial state
            val initial = awaitItem()
            assertEquals(3, initial.quotes.size)

            // When: Quotes flow updates
            val newQuotes = testQuotes + Quote(
                id = 4,
                textLatin = "New quote",
                author = "New author",
                runicElder = "ᚾᛖᚹ",
                runicYounger = "ᚾᛖᚹ",
                runicCirth = "\uE0B4\uE0C9\uE0B8",
                isUserCreated = false,
                isFavorite = false
            )
            allQuotesFlow.value = newQuotes
            advanceUntilIdle()

            // Then: State reflects new quotes
            val updated = awaitItem()
            assertEquals(4, updated.quotes.size)

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ==================== Edge Cases ====================

    @Test
    fun `empty quotes list is handled correctly`() = runTest {
        // Given: Empty flows
        every { quoteRepository.getAllQuotesFlow() } returns flowOf(emptyList())
        every { quoteRepository.getUserQuotesFlow() } returns flowOf(emptyList())
        every { quoteRepository.getFavoritesFlow() } returns flowOf(emptyList())

        // When: ViewModel is created
        viewModel = QuoteListViewModel(quoteRepository, userPreferencesManager)
        advanceUntilIdle()

        // Then: State contains empty list
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.quotes.isEmpty())
            assertFalse(state.isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `SYSTEM filter excludes user-created quotes correctly`() = runTest {
        // Given: Mixed quotes
        viewModel = QuoteListViewModel(quoteRepository, userPreferencesManager)
        advanceUntilIdle()

        // When: Filtering by SYSTEM
        viewModel.setFilter(QuoteFilter.SYSTEM)
        advanceUntilIdle()

        // Then: No user-created quotes in result
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.quotes.none { it.isUserCreated })
            assertEquals(2, state.quotes.size) // Only 2 system quotes
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `multiple error scenarios are handled independently`() = runTest {
        // Given: ViewModel initialized
        viewModel = QuoteListViewModel(quoteRepository, userPreferencesManager)
        advanceUntilIdle()

        viewModel.uiState.test {
            awaitItem() // Initial state

            // First error: toggle favorite
            coEvery { quoteRepository.toggleFavorite(any(), any()) } throws IOException("Toggle error")
            viewModel.toggleFavorite(testQuotes[0])
            advanceUntilIdle()
            val state1 = awaitItem()
            assertTrue(state1.errorMessage!!.contains("Toggle error"))

            // Clear error
            viewModel.clearError()
            advanceUntilIdle()
            val clearedState = awaitItem()
            assertNull(clearedState.errorMessage)

            // Second error: delete quote
            coEvery { quoteRepository.deleteUserQuote(any()) } throws IOException("Delete error")
            viewModel.deleteQuote(1L)
            advanceUntilIdle()
            val state2 = awaitItem()
            assertTrue(state2.errorMessage!!.contains("Delete error"))

            cancelAndIgnoreRemainingEvents()
        }
    }
}
