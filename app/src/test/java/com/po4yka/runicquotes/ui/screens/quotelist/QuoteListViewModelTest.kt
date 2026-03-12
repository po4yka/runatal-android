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
class QuoteListViewModelTest {

    private lateinit var quoteRepository: QuoteRepository
    private lateinit var userPreferencesManager: UserPreferencesManager
    private lateinit var transliterationFactory: TransliterationFactory
    private lateinit var viewModel: QuoteListViewModel

    private val testDispatcher = StandardTestDispatcher()

    private val testQuotes = listOf(
        Quote(
            id = 1L,
            textLatin = "Great work begins with one step.",
            author = "Steve Jobs",
            runicElder = "",
            runicYounger = "",
            runicCirth = "",
            isUserCreated = false,
            isFavorite = false
        ),
        Quote(
            id = 2L,
            textLatin = "Not all those who wander are lost.",
            author = "J.R.R. Tolkien",
            runicElder = "",
            runicYounger = "",
            runicCirth = "",
            isUserCreated = true,
            isFavorite = true
        ),
        Quote(
            id = 3L,
            textLatin = "In the middle of difficulty lies opportunity.",
            author = "Lao Tzu",
            runicElder = "",
            runicYounger = "",
            runicCirth = "",
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
        transliterationFactory = mockk(relaxed = true)

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
        coEvery { quoteRepository.toggleFavorite(any(), any()) } returns Unit
        coEvery { quoteRepository.deleteUserQuote(any()) } returns Unit
        coEvery { quoteRepository.saveUserQuote(any()) } returns 99L
        coEvery { quoteRepository.restoreUserQuote(any()) } returns 99L
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `viewModel initializes with loading then emits loaded state`() = runTest {
        viewModel = createViewModel()

        viewModel.uiState.test {
            assertThat(awaitItem().isLoading).isTrue()
            advanceUntilIdle()
            val loadedState = awaitItem()
            assertThat(loadedState.isLoading).isFalse()
            assertThat(loadedState.quotes).hasSize(testQuotes.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setFilter to USER_CREATED shows only user quotes`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.setFilter(QuoteFilter.USER_CREATED)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.currentFilter).isEqualTo(QuoteFilter.USER_CREATED)
        assertThat(viewModel.uiState.value.quotes).containsExactlyElementsIn(userQuotes)
        coVerify { userPreferencesManager.updateQuoteListFilter(QuoteFilter.USER_CREATED.persistedValue) }
    }

    @Test
    fun `setFilter to FAVORITES shows only favorite quotes`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.setFilter(QuoteFilter.FAVORITES)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.currentFilter).isEqualTo(QuoteFilter.FAVORITES)
        assertThat(viewModel.uiState.value.quotes).containsExactlyElementsIn(favoriteQuotes)
    }

    @Test
    fun `updateSearchQuery filters quotes and persists search`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.updateSearchQuery("wander")
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.searchQuery).isEqualTo("wander")
        assertThat(viewModel.uiState.value.quotes).containsExactly(testQuotes[1])
        coVerify { userPreferencesManager.updateQuoteSearchQuery("wander") }
    }

    @Test
    fun `toggleFavorite calls repository with inverted favorite state`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.toggleFavorite(testQuotes.first())
        advanceUntilIdle()

        coVerify { quoteRepository.toggleFavorite(testQuotes.first().id, true) }
    }

    @Test
    fun `toggleFavorite surfaces io errors`() = runTest {
        coEvery { quoteRepository.toggleFavorite(any(), any()) } throws IOException("disk")
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.toggleFavorite(testQuotes.first())
            advanceUntilIdle()

            assertThat(awaitItem()).isEqualTo(
                QuoteListEvent.ShowMessage("Failed to update favorite: disk")
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `deleteQuote deletes then emits undo event`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.deleteQuote(testQuotes[1])
            advanceUntilIdle()

            coVerify { quoteRepository.deleteUserQuote(2L) }
            assertThat(awaitItem()).isEqualTo(QuoteListEvent.QuoteDeleted(testQuotes[1]))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `restoreDeletedQuote re-inserts a user-created copy`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.restoreDeletedQuote(testQuotes.first())
        advanceUntilIdle()

        coVerify {
            quoteRepository.restoreUserQuote(
                match { restored -> restored.id == 1L && restored.isUserCreated }
            )
        }
    }

    private fun createViewModel(): QuoteListViewModel {
        return QuoteListViewModel(
            quoteRepository = quoteRepository,
            userPreferencesManager = userPreferencesManager,
            transliterationFactory = transliterationFactory
        )
    }
}
