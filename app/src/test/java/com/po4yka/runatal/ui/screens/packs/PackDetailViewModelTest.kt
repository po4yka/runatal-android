package com.po4yka.runatal.ui.screens.packs

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.po4yka.runatal.data.repository.QuotePackRepository
import com.po4yka.runatal.domain.model.QuotePack
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
        coEvery { quotePackRepository.toggleLibrary(any()) } returns testPack.copy(isInLibrary = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `route pack id loads the selected pack on initialization`() = runTest {
        val selectedPack = testPack.copy(id = 32L)
        coEvery { quotePackRepository.getPackById(32L) } returns selectedPack

        val viewModel = PackDetailViewModel(quotePackRepository, packId = 32L)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isEqualTo(PackDetailUiState.Success(selectedPack))
        coVerify(exactly = 1) { quotePackRepository.getPackById(32L) }
    }

    @Test
    fun `invalid route pack id exposes an error without loading`() = runTest {
        val viewModel = PackDetailViewModel(quotePackRepository, packId = 0L)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isEqualTo(PackDetailUiState.Error("Pack not found"))
        coVerify(exactly = 0) { quotePackRepository.getPackById(any()) }
    }

    @Test
    fun `toggleLibrary emits snackbar event with library action when pack is added`() = runTest {
        val viewModel = PackDetailViewModel(
            quotePackRepository = quotePackRepository,
            packId = 7L
        )
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.toggleLibrary()
            advanceUntilIdle()

            assertThat(awaitItem()).isEqualTo(
                PackDetailEvent.ShowMessage(
                    message = "12 pack quotes available in library",
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
    fun `toggle returns committed state even when current screen membership is stale`() = runTest {
        coEvery { quotePackRepository.toggleLibrary(7L) } returns testPack.copy(isInLibrary = false)
        val viewModel = PackDetailViewModel(quotePackRepository, packId = 7L)
        advanceUntilIdle()
        viewModel.toggleLibrary()
        advanceUntilIdle()
        assertThat((viewModel.uiState.value as PackDetailUiState.Success).pack.isInLibrary).isFalse()
        coVerify { quotePackRepository.toggleLibrary(7L) }
    }

    @Test
    fun `toggleLibrary emits message event when update fails`() = runTest {
        coEvery { quotePackRepository.toggleLibrary(any()) } throws IOException("disk")
        val viewModel = PackDetailViewModel(
            quotePackRepository = quotePackRepository,
            packId = 7L
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
