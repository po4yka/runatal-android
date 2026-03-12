package com.po4yka.runicquotes.ui.screens.references

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.po4yka.runicquotes.data.repository.RuneReferenceRepository
import com.po4yka.runicquotes.domain.model.RuneReference
import io.mockk.coEvery
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
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `saved rune id loads rune on initialization`() = runTest {
        coEvery { runeReferenceRepository.getRuneById(3L) } returns testRune

        val viewModel = RuneDetailViewModel(
            runeReferenceRepository = runeReferenceRepository,
            savedStateHandle = SavedStateHandle(mapOf("runeId" to 3L))
        )
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isEqualTo(RuneDetailUiState.Success(testRune))
    }

    @Test
    fun `retry uses saved rune id after an error`() = runTest {
        coEvery { runeReferenceRepository.getRuneById(3L) } throws IOException("disk") andThen testRune

        val viewModel = RuneDetailViewModel(
            runeReferenceRepository = runeReferenceRepository,
            savedStateHandle = SavedStateHandle(mapOf("runeId" to 3L))
        )
        advanceUntilIdle()
        assertThat(viewModel.uiState.value).isEqualTo(RuneDetailUiState.Error("Failed to load rune: disk"))

        viewModel.retry()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isEqualTo(RuneDetailUiState.Success(testRune))
    }
}
