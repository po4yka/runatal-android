package com.po4yka.runatal.ui.screens.archive

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.po4yka.runatal.data.repository.ArchiveRepository
import com.po4yka.runatal.domain.model.ArchivedQuote
import com.po4yka.runatal.domain.model.QuoteLifecycleChange
import com.po4yka.runatal.domain.model.QuoteLifecycleState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
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

@OptIn(ExperimentalCoroutinesApi::class)
class ArchiveViewModelTest {
    private lateinit var repository: ArchiveRepository
    private lateinit var viewModel: ArchiveViewModel
    private val dispatcher = StandardTestDispatcher()
    private val archived = listOf(ArchivedQuote(1L, "First", "User", 42L), ArchivedQuote(2L, "Second", "User", 43L))
    private val hidden = ArchivedQuote(3L, "Hidden", "User", 44L, QuoteLifecycleState.HIDDEN)
    private val trashed = ArchivedQuote(4L, "Trash", "User", 45L, QuoteLifecycleState.TRASH)
    private val retained = MutableStateFlow(archived + hidden + trashed)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = mockk()
        every { repository.getRetainedQuotesFlow() } returns retained
        coEvery { repository.restoreQuotes(any()) } answers {
            firstArg<List<ArchivedQuote>>().map { receipt(it) }
        }
        coEvery { repository.undoChanges(any()) } returns Unit
        coEvery { repository.moveToTrash(any()) } answers {
            val quote = firstArg<ArchivedQuote>()
            receipt(quote).copy(state = QuoteLifecycleState.TRASH)
        }
        coEvery { repository.emptyTrash() } returns Unit
        viewModel = ArchiveViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `one authoritative snapshot populates archived hidden and trash tabs`() = runTest {
        assertThat(viewModel.uiState.value.archivedQuotes).containsExactlyElementsIn(archived)
        assertThat(viewModel.uiState.value.hiddenQuotes).containsExactly(hidden)
        assertThat(viewModel.uiState.value.deletedQuotes).containsExactly(trashed)
        viewModel.selectTab(ArchiveTab.HIDDEN)
        assertThat(viewModel.uiState.value.quotesForSelectedTab).containsExactly(hidden)
        retained.value = listOf(trashed)
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.hiddenQuotes).isEmpty()
        assertThat(viewModel.uiState.value.deletedQuotes).containsExactly(trashed)
    }

    @Test
    fun `restore all uses one atomic batch and carries its exact undo receipts`() = runTest {
        viewModel.snackbarEvent.test {
            viewModel.restoreAllArchivedQuotes()
            advanceUntilIdle()
            coVerify(exactly = 1) { repository.restoreQuotes(archived) }
            assertThat(awaitItem()).isEqualTo(ArchiveSnackbarEvent.RestoredBatch(archived.map { receipt(it) }))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `hidden restore all is a positive batch operation`() = runTest {
        viewModel.selectTab(ArchiveTab.HIDDEN)
        viewModel.restoreAllArchivedQuotes()
        advanceUntilIdle()
        coVerify(exactly = 1) { repository.restoreQuotes(listOf(hidden)) }
    }

    @Test
    fun `undo restore submits exact mutation tokens in one operation`() = runTest {
        val changes = archived.map { receipt(it) }
        viewModel.undoRestore(changes)
        advanceUntilIdle()
        coVerify(exactly = 1) { repository.undoChanges(changes) }
    }

    @Test
    fun `single trash restore produces a persistent undo receipt`() = runTest {
        viewModel.snackbarEvent.test {
            viewModel.restoreQuote(trashed)
            advanceUntilIdle()
            coVerify { repository.restoreQuotes(listOf(trashed)) }
            assertThat(awaitItem()).isEqualTo(ArchiveSnackbarEvent.RestoredQuote(listOf(receipt(trashed))))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `move to trash and explicit purge use separate commands`() = runTest {
        viewModel.softDeleteQuote(hidden)
        viewModel.emptyTrash()
        advanceUntilIdle()
        coVerify { repository.moveToTrash(hidden) }
        coVerify { repository.emptyTrash() }
    }

    @Test
    fun `failed batch restore reports failure without successful restore event`() = runTest {
        coEvery { repository.restoreQuotes(any()) } throws IOException("disk full")
        viewModel.snackbarEvent.test {
            viewModel.restoreAllArchivedQuotes()
            advanceUntilIdle()
            assertThat(awaitItem()).isEqualTo(ArchiveSnackbarEvent.ShowMessage("Failed to restore quotes: disk full"))
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `retry clears flow failure and restores a single snapshot observation`() = runTest {
        every { repository.getRetainedQuotesFlow() } returnsMany listOf(flow { throw IOException("offline") }, retained)
        val failing = ArchiveViewModel(repository)
        advanceUntilIdle()
        assertThat(failing.uiState.value.errorMessage).isEqualTo("Failed to load archive: offline")
        failing.retry()
        advanceUntilIdle()
        assertThat(failing.uiState.value.errorMessage).isNull()
        assertThat(failing.uiState.value.hiddenQuotes).containsExactly(hidden)
    }

    private fun receipt(quote: ArchivedQuote) = QuoteLifecycleChange(
        quote.id, quote.lifecycleState, QuoteLifecycleState.ACTIVE, quote.archivedAt, "restore-${quote.id}"
    )
}
