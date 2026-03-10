package com.po4yka.runicquotes.ui.screens.quotelist

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.po4yka.runicquotes.data.preferences.UserPreferences
import com.po4yka.runicquotes.data.preferences.UserPreferencesManager
import com.po4yka.runicquotes.data.repository.QuoteRepository
import com.po4yka.runicquotes.domain.model.Quote
import com.po4yka.runicquotes.domain.model.RunicScript
import com.po4yka.runicquotes.domain.transliteration.TransliterationFactory
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
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class QuoteListViewModelTest {

    private lateinit var quoteRepository: QuoteRepository
    private lateinit var userPreferencesManager: UserPreferencesManager
    private lateinit var transliterationFactory: TransliterationFactory
    private lateinit var viewModel: QuoteListViewModel

    private val testDispatcher = StandardTestDispatcher()

    private val testQuotes = listOf(
        Quote(
            id = 1,
            textLatin = "Great work begins with one step.",
            author = "Steve Jobs",
            runicElder = "\u16CF\u16D6\u16CA\u16CF",
            runicYounger = "\u16CF\u16D6\u16CA\u16CF",
            runicCirth = "\uE088\uE0C9\uE09C\uE088",
            isUserCreated = false,
            isFavorite = false
        ),
        Quote(
            id = 2,
            textLatin = "Not all those who wander are lost.",
            author = "J.R.R. Tolkien",
            runicElder = "\u16A6\u16D6\u16CA\u16CF",
            runicYounger = "\u16A6\u16D6\u16CA\u16CF",
            runicCirth = "\uE088\uE0B4\uE0C9\uE09C\uE088",
            isUserCreated = true,
            isFavorite = true
        ),
        Quote(
            id = 3,
            textLatin = "In the middle of difficulty lies opportunity.",
            author = "Lao Tzu",
            runicElder = "\u16B9\u16DF\u16B1\u16DE",
            runicYounger = "\u16B9\u16DF\u16B1\u16DE",
            runicCirth = "\uE0B8\uE0CB\uE0A0\uE089",
            isUserCreated = false,
            isFavorite = true
        )
    )

    private val userQuotes = testQuotes.filter { it.isUserCreated }
    private val favoriteQuotes = testQuotes.filter { it.isFavorite }

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

        quoteRepository = mockk()
        userPreferencesManager = mockk()
        transliterationFactory = mockk()

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
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = QuoteListViewModel(
        quoteRepository, userPreferencesManager, transliterationFactory
    )

    @Test
    fun `viewModel initializes with loading state`() = runTest {
        viewModel = createViewModel()

        viewModel.uiState.test {
            val initialState = awaitItem()
            assertThat(initialState.isLoading).isFalse()
            assertThat(initialState.quotes).isEqualTo(emptyList<Quote>())

            advanceUntilIdle()

            val loadingState = awaitItem()
            assertThat(loadingState.isLoading).isTrue()

            val loadedState = awaitItem()
            assertThat(loadedState.isLoading).isFalse()
            assertThat(loadedState.quotes).hasSize(testQuotes.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `viewModel loads quotes on initialization`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.quotes).hasSize(testQuotes.size)
            assertThat(state.currentFilter).isEqualTo(QuoteFilter.ALL)
            assertThat(state.isLoading).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setFilter to ALL shows all quotes`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.setFilter(QuoteFilter.ALL)
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.currentFilter).isEqualTo(QuoteFilter.ALL)
            assertThat(state.quotes).hasSize(testQuotes.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setFilter to USER_CREATED shows only user quotes`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.setFilter(QuoteFilter.USER_CREATED)
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.currentFilter).isEqualTo(QuoteFilter.USER_CREATED)
            assertThat(state.quotes).hasSize(userQuotes.size)
            assertThat(state.quotes.all { it.isUserCreated }).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setFilter to FAVORITES shows only favorite quotes`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.setFilter(QuoteFilter.FAVORITES)
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.currentFilter).isEqualTo(QuoteFilter.FAVORITES)
            assertThat(state.quotes).hasSize(favoriteQuotes.size)
            assertThat(state.quotes.all { it.isFavorite }).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `toggleFavorite calls repository with correct parameters`() = runTest {
        coEvery { quoteRepository.toggleFavorite(any(), any()) } returns Unit
        viewModel = createViewModel()
        advanceUntilIdle()

        val quote = testQuotes[0]
        viewModel.toggleFavorite(quote)
        advanceUntilIdle()

        coVerify { quoteRepository.toggleFavorite(quote.id, !quote.isFavorite) }
    }

    @Test
    fun `toggleFavorite handles IOException`() = runTest {
        coEvery { quoteRepository.toggleFavorite(any(), any()) } throws IOException("Network error")
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.toggleFavorite(testQuotes[0])
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.errorMessage).isNotNull()
            assertThat(state.errorMessage).contains("Failed to update favorite")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `deleteQuote calls repository with quote id`() = runTest {
        coEvery { quoteRepository.deleteUserQuote(any()) } returns Unit
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.deleteQuote(2L)
        advanceUntilIdle()

        coVerify { quoteRepository.deleteUserQuote(2L) }
    }

    @Test
    fun `deleteQuote handles IOException`() = runTest {
        coEvery { quoteRepository.deleteUserQuote(any()) } throws IOException("Database error")
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.deleteQuote(1L)
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.errorMessage).isNotNull()
            assertThat(state.errorMessage).contains("Failed to delete quote")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `clearError removes error message`() = runTest {
        coEvery { quoteRepository.toggleFavorite(any(), any()) } throws IOException("Error")
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.toggleFavorite(testQuotes[0])
        advanceUntilIdle()

        viewModel.uiState.test {
            val stateWithError = awaitItem()
            assertThat(stateWithError.errorMessage).isNotNull()

            viewModel.clearError()
            advanceUntilIdle()

            val stateWithoutError = awaitItem()
            assertThat(stateWithoutError.errorMessage).isNull()

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `preferences change updates selectedScript in state`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.uiState.test {
            val initial = awaitItem()
            assertThat(initial.selectedScript).isEqualTo(RunicScript.ELDER_FUTHARK)

            preferencesFlow.value = defaultPreferences.copy(selectedScript = RunicScript.YOUNGER_FUTHARK)
            advanceUntilIdle()

            val updated = awaitItem()
            assertThat(updated.selectedScript).isEqualTo(RunicScript.YOUNGER_FUTHARK)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `empty quotes list is handled correctly`() = runTest {
        every { quoteRepository.getAllQuotesFlow() } returns flowOf(emptyList())
        every { quoteRepository.getUserQuotesFlow() } returns flowOf(emptyList())
        every { quoteRepository.getFavoritesFlow() } returns flowOf(emptyList())

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.quotes).isEmpty()
            assertThat(state.isLoading).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }
}
