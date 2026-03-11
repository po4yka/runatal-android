package com.po4yka.runicquotes.ui.screens.archive

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.po4yka.runicquotes.data.repository.ArchiveRepository
import com.po4yka.runicquotes.domain.model.ArchivedQuote
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
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
class ArchiveViewModelTest {

    private lateinit var archiveRepository: ArchiveRepository
    private lateinit var viewModel: ArchiveViewModel

    private val testDispatcher = StandardTestDispatcher()

    private val archivedQuotesFlow = MutableStateFlow(
        listOf(
            ArchivedQuote(
                id = 1L,
                originalQuoteId = 11L,
                textLatin = "The only way to do great work is to love what you do.",
                author = "Steve Jobs",
                archivedAt = 1_708_560_000_000L
            ),
            ArchivedQuote(
                id = 2L,
                originalQuoteId = 12L,
                textLatin = "In the middle of difficulty lies opportunity.",
                author = "Albert Einstein",
                archivedAt = 1_708_473_600_000L
            )
        )
    )

    private val deletedQuotesFlow = MutableStateFlow(
        listOf(
            ArchivedQuote(
                id = 3L,
                originalQuoteId = 13L,
                textLatin = "The mind is everything. What you think you become.",
                author = "Buddha",
                archivedAt = 1_708_214_400_000L,
                isDeleted = true
            )
        )
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        archiveRepository = mockk()

        every { archiveRepository.getAllArchivedFlow() } returns archivedQuotesFlow
        every { archiveRepository.getActiveArchivedFlow() } returns archivedQuotesFlow
        every { archiveRepository.getDeletedFlow() } returns deletedQuotesFlow
        coEvery { archiveRepository.restoreQuote(any()) } returns Unit
        coEvery { archiveRepository.archiveQuote(any()) } returns 1L
        coEvery { archiveRepository.softDeleteQuote(any()) } returns Unit
        coEvery { archiveRepository.emptyTrash() } returns Unit

        viewModel = ArchiveViewModel(archiveRepository)
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `selectTab updates selected archive tab`() = runTest {
        viewModel.selectTab(ArchiveTab.HIDDEN)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.selectedTab).isEqualTo(ArchiveTab.HIDDEN)
        assertThat(viewModel.uiState.value.hiddenQuotes).isEmpty()
    }

    @Test
    fun `restoreAllArchivedQuotes restores each archived quote and emits batch snackbar`() = runTest {
        viewModel.snackbarEvent.test {
            viewModel.restoreAllArchivedQuotes()
            advanceUntilIdle()

            coVerify(exactly = 1) { archiveRepository.restoreQuote(1L) }
            coVerify(exactly = 1) { archiveRepository.restoreQuote(2L) }

            val event = awaitItem()
            assertThat(event).isInstanceOf(ArchiveSnackbarEvent.RestoredBatch::class.java)
            assertThat(event.quotes).hasSize(2)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `undoRestore archives each restored quote again`() = runTest {
        val restoredQuotes = archivedQuotesFlow.value

        viewModel.undoRestore(restoredQuotes)
        advanceUntilIdle()

        coVerify(exactly = restoredQuotes.size) {
            archiveRepository.archiveQuote(any())
        }
    }

    @Test
    fun `restoreQuote emits single quote snackbar`() = runTest {
        viewModel.snackbarEvent.test {
            val quote = archivedQuotesFlow.value.first()

            viewModel.restoreQuote(quote)
            advanceUntilIdle()

            coVerify { archiveRepository.restoreQuote(quote.id) }
            val event = awaitItem()
            assertThat(event).isEqualTo(ArchiveSnackbarEvent.RestoredQuote(listOf(quote)))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `softDeleteQuote and emptyTrash delegate to repository`() = runTest {
        viewModel.softDeleteQuote(2L)
        viewModel.emptyTrash()
        advanceUntilIdle()

        coVerify { archiveRepository.softDeleteQuote(2L) }
        coVerify { archiveRepository.emptyTrash() }
    }

    @Test
    fun `retry clears error and reloads archive after a flow failure`() = runTest {
        every { archiveRepository.getActiveArchivedFlow() } returnsMany listOf(
            flow { throw IOException("offline") },
            archivedQuotesFlow
        )
        every { archiveRepository.getDeletedFlow() } returns deletedQuotesFlow

        val failingViewModel = ArchiveViewModel(archiveRepository)
        advanceUntilIdle()

        assertThat(failingViewModel.uiState.value.errorMessage)
            .isEqualTo("Failed to load archive: offline")

        failingViewModel.retry()
        advanceUntilIdle()

        assertThat(failingViewModel.uiState.value.errorMessage).isNull()
        assertThat(failingViewModel.uiState.value.archivedQuotes).hasSize(2)
    }
}
