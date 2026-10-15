package com.po4yka.runatal.ui.screens.references

import androidx.lifecycle.viewModelScope
import com.google.common.truth.Truth.assertThat
import com.po4yka.runatal.data.repository.RuneReferenceRepository
import com.po4yka.runatal.domain.model.RuneReference
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/** Initialization and retry exercise the complete seed-then-observe boundary rather than just the downstream flow. */
@OptIn(ExperimentalCoroutinesApi::class)
class ReferencesViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: RuneReferenceRepository
    private val viewModels = mutableListOf<ReferencesViewModel>()
    private val elder = RuneReference(1L, "ᚠ", "Fehu", "f", "Wealth", "History", "elder_futhark")
    private val cirth = elder.copy(id = 2L, name = "Cirth P", script = "cirth")

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = mockk()
        coEvery { repository.seedIfNeeded() } returns Unit
        every { repository.getRunesByScriptFlow("elder_futhark") } returns flowOf(listOf(elder))
        every { repository.getRunesByScriptFlow("cirth") } returns flowOf(listOf(cirth))
    }

    @After
    fun tearDown() {
        viewModels.forEach { it.viewModelScope.cancel() }
        dispatcher.scheduler.runCurrent()
        Dispatchers.resetMain()
    }

    private fun createViewModel(): ReferencesViewModel =
        ReferencesViewModel(repository).also { viewModels += it }

    @Test
    fun `initial seed IO failure is visible and retry seeds again before showing the real collection`() = runTest {
        coEvery { repository.seedIfNeeded() } throws IOException("disk full") andThen Unit
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.isLoading).isFalse()
        assertThat(viewModel.uiState.value.errorMessage).isEqualTo("Failed to load runes: disk full")
        assertThat(viewModel.uiState.value.runes).isEmpty()
        verify(exactly = 0) { repository.getRunesByScriptFlow(any()) }
        viewModel.retry()
        assertThat(viewModel.uiState.value.isLoading).isTrue()
        advanceUntilIdle()
        coVerify(exactly = 2) { repository.seedIfNeeded() }
        assertThat(viewModel.uiState.value.errorMessage).isNull()
        assertThat(viewModel.uiState.value.runes).containsExactly(elder)
        assertThat(viewModel.uiState.value.totalRuneCount).isEqualTo(1)
    }

    @Test
    fun `synchronous flow construction failure is included in the same recoverable boundary`() = runTest {
        every { repository.getRunesByScriptFlow("elder_futhark") } throws IllegalStateException("not ready") andThen
            flowOf(listOf(elder))
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.errorMessage).isEqualTo("Failed to load runes: not ready")
        viewModel.retry()
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.errorMessage).isNull()
        assertThat(viewModel.uiState.value.runes).containsExactly(elder)
    }

    @Test
    fun `tab change cancels pending initialization and cannot publish the previous tab late`() = runTest {
        val gate = CompletableDeferred<Unit>()
        var calls = 0
        var cancelled = false
        coEvery { repository.seedIfNeeded() } coAnswers {
            calls += 1
            if (calls == 1) {
                try {
                    gate.await()
                } finally {
                    cancelled = true
                }
            }
        }
        val viewModel = createViewModel()
        runCurrent()
        viewModel.selectTab(ScriptTab.CIRTH)
        advanceUntilIdle()
        assertThat(cancelled).isTrue()
        assertThat(viewModel.uiState.value.selectedTab).isEqualTo(ScriptTab.CIRTH)
        assertThat(viewModel.uiState.value.runes).containsExactly(cirth)
        assertThat(viewModel.uiState.value.errorMessage).isNull()
        verify(exactly = 0) { repository.getRunesByScriptFlow("elder_futhark") }
    }

    @Test
    fun `repeated retry replaces the active collection and preserves search filtering`() = runTest {
        val rows = MutableStateFlow(listOf(elder))
        var active = 0
        var maximum = 0
        every { repository.getRunesByScriptFlow("elder_futhark") } returns flow {
            active += 1
            maximum = maxOf(maximum, active)
            try {
                emitAll(rows)
                awaitCancellation()
            } finally {
                active -= 1
            }
        }
        val viewModel = createViewModel()
        runCurrent()
        viewModel.updateSearchQuery("wealth")
        repeat(3) {
            viewModel.retry()
            runCurrent()
        }
        assertThat(maximum).isEqualTo(1)
        assertThat(active).isEqualTo(1)
        assertThat(viewModel.uiState.value.runes).containsExactly(elder)
        rows.value = listOf(elder.copy(meaning = "Changed"))
        runCurrent()
        assertThat(viewModel.uiState.value.runes).isEmpty()
        assertThat(viewModel.uiState.value.totalRuneCount).isEqualTo(1)
        coVerify(exactly = 4) { repository.seedIfNeeded() }
        // Terminate this explicitly owned test collection before resetting Main.
        viewModel.viewModelScope.cancel()
        runCurrent()
    }
}
