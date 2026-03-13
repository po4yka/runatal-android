package com.po4yka.runatal.ui.screens.packs

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.po4yka.runatal.data.repository.QuotePackRepository
import com.po4yka.runatal.domain.model.QuotePack
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PacksViewModelTest {

    private lateinit var quotePackRepository: QuotePackRepository
    private val testDispatcher = StandardTestDispatcher()

    private val testPack = QuotePack(
        id = 7L,
        name = "Rune Wisdom",
        description = "Short curated sayings",
        coverRune = "\u16A0",
        quoteCount = 12,
        isInLibrary = false
    )

    private lateinit var allPacksFlow: MutableStateFlow<List<QuotePack>>
    private lateinit var libraryPacksFlow: MutableStateFlow<List<QuotePack>>

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        quotePackRepository = mockk()

        allPacksFlow = MutableStateFlow(listOf(testPack))
        libraryPacksFlow = MutableStateFlow(emptyList())

        coEvery { quotePackRepository.seedIfNeeded() } returns Unit
        every { quotePackRepository.getAllPacksFlow() } returns allPacksFlow
        every { quotePackRepository.getLibraryPacksFlow() } returns libraryPacksFlow
        coEvery { quotePackRepository.updatePack(any()) } returns Unit
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `toggleLibrary emits message event on io error`() = runTest {
        coEvery { quotePackRepository.updatePack(any()) } throws IOException("disk")
        val viewModel = PacksViewModel(quotePackRepository)
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.toggleLibrary(testPack)
            advanceUntilIdle()

            assertThat(awaitItem()).isEqualTo(PacksEvent.ShowMessage("Failed to update pack: disk"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `load failure stays in persistent ui state`() = runTest {
        every { quotePackRepository.getAllPacksFlow() } returns flow { throw IOException("offline") }
        every { quotePackRepository.getLibraryPacksFlow() } returns flowOf(emptyList())

        val viewModel = PacksViewModel(quotePackRepository)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.errorMessage).isEqualTo("Failed to load packs: offline")
    }

    @Test
    fun `retry clears load error and reloads packs`() = runTest {
        every { quotePackRepository.getAllPacksFlow() } returnsMany listOf(
            flow { throw IOException("offline") },
            allPacksFlow
        )

        val viewModel = PacksViewModel(quotePackRepository)
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.errorMessage).isEqualTo("Failed to load packs: offline")

        viewModel.retry()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.errorMessage).isNull()
        assertThat(viewModel.uiState.value.packs).containsExactly(testPack)
    }
}
