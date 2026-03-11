package com.po4yka.runicquotes.ui.screens.share

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.po4yka.runicquotes.data.repository.QuoteRepository
import com.po4yka.runicquotes.domain.model.Quote
import com.po4yka.runicquotes.util.QuoteShareManager
import com.po4yka.runicquotes.util.ShareAppearance
import com.po4yka.runicquotes.util.ShareTemplate
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
    private lateinit var quoteShareManager: QuoteShareManager

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
        quoteShareManager = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(
        quoteId: Long = 7L
    ): ShareViewModel {
        return ShareViewModel(
            savedStateHandle = SavedStateHandle(mapOf("quoteId" to quoteId)),
            quoteRepository = quoteRepository,
            quoteShareManager = quoteShareManager
        )
    }

    @Test
    fun `init loads quote from saved state handle`() = runTest {
        coEvery { quoteRepository.getQuoteById(7L) } returns testQuote

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isEqualTo(ShareUiState.Success(testQuote))
    }

    @Test
    fun `initializeQuoteIfNeeded loads quote when state was created without id`() = runTest {
        coEvery { quoteRepository.getQuoteById(8L) } returns testQuote.copy(id = 8L)
        val viewModel = createViewModel(quoteId = 0L)

        viewModel.initializeQuoteIfNeeded(8L)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isEqualTo(ShareUiState.Success(testQuote.copy(id = 8L)))
    }

    @Test
    fun `initializeQuoteIfNeeded reloads current id while still loading`() = runTest {
        coEvery { quoteRepository.getQuoteById(0L) } returns null
        coEvery { quoteRepository.getQuoteById(9L) } returns testQuote.copy(id = 9L)
        val viewModel = createViewModel(quoteId = 0L)

        viewModel.initializeQuoteIfNeeded(9L)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isEqualTo(ShareUiState.Success(testQuote.copy(id = 9L)))
    }

    @Test
    fun `loadQuote emits not found when repository returns null`() = runTest {
        coEvery { quoteRepository.getQuoteById(7L) } returns null

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isEqualTo(ShareUiState.Error("Quote not found"))
    }

    @Test
    fun `loadQuote surfaces io errors`() = runTest {
        coEvery { quoteRepository.getQuoteById(7L) } throws IOException("disk")

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isEqualTo(ShareUiState.Error("Failed to load quote: disk"))
    }

    @Test
    fun `loadQuote surfaces illegal state errors`() = runTest {
        coEvery { quoteRepository.getQuoteById(7L) } throws IllegalStateException("bad state")

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isEqualTo(ShareUiState.Error("Invalid state: bad state"))
    }

    @Test
    fun `selectTemplate and selectAppearance update state flows`() = runTest {
        coEvery { quoteRepository.getQuoteById(7L) } returns testQuote
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.selectTemplate(ShareTemplate.LANDSCAPE)
        viewModel.selectAppearance(ShareAppearance.LIGHT)

        assertThat(viewModel.selectedTemplate.value).isEqualTo(ShareTemplate.LANDSCAPE)
        assertThat(viewModel.selectedAppearance.value).isEqualTo(ShareAppearance.LIGHT)
    }

    @Test
    fun `shareAsText delegates to share manager when quote is loaded`() = runTest {
        coEvery { quoteRepository.getQuoteById(7L) } returns testQuote
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.shareAsText()

        verify { quoteShareManager.shareQuoteText(testQuote.textLatin, testQuote.author) }
    }

    @Test
    fun `shareAsImage uses selected style and elder fallback`() = runTest {
        coEvery { quoteRepository.getQuoteById(7L) } returns testQuote.copy(runicElder = null)
        coEvery {
            quoteShareManager.shareQuoteAsImage(any(), any(), any(), any(), any())
        } returns true
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.selectTemplate(ShareTemplate.VERSE)
        viewModel.selectAppearance(ShareAppearance.LIGHT)

        viewModel.shareAsImage()
        advanceUntilIdle()

        coVerify {
            quoteShareManager.shareQuoteAsImage(
                runicText = testQuote.textLatin,
                latinText = testQuote.textLatin,
                author = testQuote.author,
                template = ShareTemplate.VERSE,
                appearance = ShareAppearance.LIGHT
            )
        }
    }

    @Test
    fun `copyQuote emits feedback and clearFeedback resets it`() = runTest {
        coEvery { quoteRepository.getQuoteById(7L) } returns testQuote
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.feedbackMessage.test {
            assertThat(awaitItem()).isNull()

            viewModel.copyQuote()
            assertThat(awaitItem()).isEqualTo("Quote copied")

            viewModel.clearFeedback()
            assertThat(awaitItem()).isNull()

            cancelAndIgnoreRemainingEvents()
        }

        verify { quoteShareManager.copyQuoteToClipboard(testQuote.textLatin, testQuote.author) }
    }

    @Test
    fun `share methods are no-ops while not in success state`() = runTest {
        coEvery { quoteRepository.getQuoteById(7L) } returns null
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.shareAsText()
        viewModel.shareAsImage()
        viewModel.copyQuote()
        advanceUntilIdle()

        verify(exactly = 0) { quoteShareManager.shareQuoteText(any(), any()) }
        verify(exactly = 0) { quoteShareManager.copyQuoteToClipboard(any(), any()) }
        coVerify(exactly = 0) {
            quoteShareManager.shareQuoteAsImage(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `retry reloads quote after an error`() = runTest {
        coEvery { quoteRepository.getQuoteById(7L) } throws IOException("disk") andThen testQuote
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isEqualTo(ShareUiState.Error("Failed to load quote: disk"))

        viewModel.retry()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isEqualTo(ShareUiState.Success(testQuote))
    }
}
