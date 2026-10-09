package com.po4yka.runatal.ui.screens.references

import com.google.common.truth.Truth.assertThat
import com.po4yka.runatal.data.repository.RuneReferenceRepository
import com.po4yka.runatal.domain.model.RuneReference
import io.mockk.every
import kotlinx.coroutines.flow.flowOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
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
class RuneDetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var runeReferenceRepository: RuneReferenceRepository

    private val testRune = RuneReference(
        id = 3L,
        character = "ᚨ",
        name = "Ansuz",
        pronunciation = "a",
        meaning = "god",
        history = "Associated with divine speech.",
        script = "elder_futhark"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        runeReferenceRepository = mockk()
        every { runeReferenceRepository.observeBookmark(any()) } returns flowOf(false)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `route rune id loads rune on initialization`() = runTest {
        coEvery { runeReferenceRepository.getRuneById(3L) } returns testRune

        val viewModel = RuneDetailViewModel(
            runeReferenceRepository = runeReferenceRepository,
            runeId = 3L
        )
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isEqualTo(RuneDetailUiState.Success(testRune))
        coVerify(exactly = 1) { runeReferenceRepository.getRuneById(3L) }
    }

    @Test
    fun `invalid route rune id exposes an error without loading`() = runTest {
        val viewModel = RuneDetailViewModel(runeReferenceRepository, runeId = 0L)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isEqualTo(RuneDetailUiState.Error("Rune not found"))
        coVerify(exactly = 0) { runeReferenceRepository.getRuneById(any()) }
    }

    @Test
    fun `retry uses route rune id after an error`() = runTest {
        coEvery { runeReferenceRepository.getRuneById(3L) } throws IOException("disk") andThen testRune

        val viewModel = RuneDetailViewModel(
            runeReferenceRepository = runeReferenceRepository,
            runeId = 3L
        )
        advanceUntilIdle()
        assertThat(viewModel.uiState.value).isEqualTo(RuneDetailUiState.Error("Failed to load rune: disk"))

        viewModel.retry()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isEqualTo(RuneDetailUiState.Success(testRune))
    }
}
