package com.po4yka.runicquotes.ui.screens.packs

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.po4yka.runicquotes.data.repository.QuotePackRepository
import com.po4yka.runicquotes.domain.model.QuotePack
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
class PackDetailViewModelTest {

    private lateinit var quotePackRepository: QuotePackRepository
    private val testDispatcher = StandardTestDispatcher()

    private val testPack = QuotePack(
        id = 7L,
        name = "Rune Wisdom",
        description = "Short curated sayings",
        coverRune = "\u16A0",
        quoteCount = 12
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        quotePackRepository = mockk()

        coEvery { quotePackRepository.seedIfNeeded() } returns Unit
        coEvery { quotePackRepository.getPackById(7L) } returns testPack
        coEvery { quotePackRepository.updatePack(any()) } returns Unit
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `toggleLibrary emits snackbar event with library action when pack is added`() = runTest {
        val viewModel = PackDetailViewModel(
            quotePackRepository = quotePackRepository,
            savedStateHandle = SavedStateHandle(mapOf("packId" to 7L))
        )
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.toggleLibrary()
            advanceUntilIdle()

            assertThat(awaitItem()).isEqualTo(
                PackDetailEvent.ShowMessage(
                    message = "12 quotes added to library",
                    actionLabel = "View library",
                    action = PackDetailEventAction.VIEW_LIBRARY
                )
            )
            cancelAndIgnoreRemainingEvents()
        }

        val uiState = viewModel.uiState.value as PackDetailUiState.Success
        assertThat(uiState.pack.isInLibrary).isTrue()
    }

    @Test
    fun `toggleLibrary emits message event when update fails`() = runTest {
        coEvery { quotePackRepository.updatePack(any()) } throws IOException("disk")
        val viewModel = PackDetailViewModel(
            quotePackRepository = quotePackRepository,
            savedStateHandle = SavedStateHandle(mapOf("packId" to 7L))
        )
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.toggleLibrary()
            advanceUntilIdle()

            assertThat(awaitItem()).isEqualTo(
                PackDetailEvent.ShowMessage("Failed to update pack: disk")
            )
            cancelAndIgnoreRemainingEvents()
        }
    }
}
