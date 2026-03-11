package com.po4yka.runicquotes.data.repository

import com.google.common.truth.Truth.assertThat
import com.po4yka.runicquotes.data.local.dao.QuotePackDao
import com.po4yka.runicquotes.data.local.entity.QuotePackEntity
import com.po4yka.runicquotes.domain.model.QuotePack
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class QuotePackRepositoryImplTest {

    private lateinit var quotePackDao: QuotePackDao
    private lateinit var repository: QuotePackRepositoryImpl

    @Before
    fun setUp() {
        quotePackDao = mockk()
        repository = QuotePackRepositoryImpl(quotePackDao)
    }

    @Test
    fun `seedIfNeeded syncs canonical packs and preserves normalized library membership`() = runTest {
        val inserted = slot<List<QuotePackEntity>>()
        coEvery { quotePackDao.getAll() } returns listOf(
            QuotePackEntity(
                id = 99L,
                name = "Havamal selections",
                description = "Custom",
                coverRune = "X",
                quoteCount = 1,
                isInLibrary = true
            )
        )
        coEvery { quotePackDao.insertAll(capture(inserted)) } returns Unit

        repository.seedIfNeeded()

        val havamal = inserted.captured.first { it.id == 1L }
        assertThat(havamal.name).isEqualTo("Hávamál Selections")
        assertThat(havamal.isInLibrary).isTrue()
        coVerify(exactly = 1) { quotePackDao.insertAll(any()) }
    }

    @Test
    fun `seedIfNeeded runs only once per repository instance`() = runTest {
        coEvery { quotePackDao.getAll() } returns emptyList()
        coEvery { quotePackDao.insertAll(any()) } returns Unit

        repository.seedIfNeeded()
        repository.seedIfNeeded()

        coVerify(exactly = 1) { quotePackDao.getAll() }
        coVerify(exactly = 1) { quotePackDao.insertAll(any()) }
    }

    @Test
    fun `getAllPacksFlow maps entities to domain models`() = runTest {
        every { quotePackDao.getAllFlow() } returns flowOf(
            listOf(
                QuotePackEntity(
                    id = 4L,
                    name = "Seasonal",
                    description = "Turns of the year",
                    coverRune = "\u16CA",
                    quoteCount = 8,
                    isInLibrary = true
                )
            )
        )

        val result = repository.getAllPacksFlow().first().single()

        assertThat(result).isEqualTo(
            QuotePack(
                id = 4L,
                name = "Seasonal",
                description = "Turns of the year",
                coverRune = "\u16CA",
                quoteCount = 8,
                isInLibrary = true
            )
        )
    }

    @Test
    fun `getPackById maps nullable dao response`() = runTest {
        coEvery { quotePackDao.getById(2L) } returns QuotePackEntity(
            id = 2L,
            name = "Elder Voices",
            description = "Lore",
            coverRune = "\u16A0",
            quoteCount = 18,
            isInLibrary = false
        )
        coEvery { quotePackDao.getById(9L) } returns null

        assertThat(repository.getPackById(2L)?.name).isEqualTo("Elder Voices")
        assertThat(repository.getPackById(9L)).isNull()
    }

    @Test
    fun `searchPacks maps search flow results`() = runTest {
        every { quotePackDao.search("wanderer") } returns flowOf(
            listOf(
                QuotePackEntity(
                    id = 3L,
                    name = "Path of the Wanderer",
                    description = "Journeys",
                    coverRune = "\u16B1",
                    quoteCount = 12
                )
            )
        )

        val result = repository.searchPacks("wanderer").first().single()

        assertThat(result.name).isEqualTo("Path of the Wanderer")
        assertThat(result.coverRune).isEqualTo("\u16B1")
    }

    @Test
    fun `getLibraryPacksFlow maps library flow`() = runTest {
        every { quotePackDao.getLibraryPacksFlow() } returns flowOf(
            listOf(
                QuotePackEntity(
                    id = 5L,
                    name = "Seasonal Runes",
                    description = "Calendar",
                    coverRune = "\u16CA",
                    quoteCount = 8,
                    isInLibrary = true
                )
            )
        )

        val result = repository.getLibraryPacksFlow().first().single()

        assertThat(result.isInLibrary).isTrue()
    }

    @Test
    fun `insert and update operations map domain models back to entities`() = runTest {
        val pack = QuotePack(
            id = 12L,
            name = "New Pack",
            description = "Description",
            coverRune = "\u16CF",
            quoteCount = 3,
            isInLibrary = true
        )
        coEvery { quotePackDao.insert(any()) } returns 44L
        coEvery { quotePackDao.update(any()) } returns Unit
        coEvery { quotePackDao.delete(any()) } returns Unit
        coEvery { quotePackDao.insertAll(any()) } returns Unit

        val insertedId = repository.insertPack(pack)
        repository.updatePack(pack)
        repository.deletePack(pack)
        repository.insertAllPacks(listOf(pack))

        assertThat(insertedId).isEqualTo(44L)
        coVerify { quotePackDao.insert(QuotePackEntity(12L, "New Pack", "Description", "\u16CF", 3, true)) }
        coVerify { quotePackDao.update(QuotePackEntity(12L, "New Pack", "Description", "\u16CF", 3, true)) }
        coVerify { quotePackDao.delete(QuotePackEntity(12L, "New Pack", "Description", "\u16CF", 3, true)) }
        coVerify { quotePackDao.insertAll(listOf(QuotePackEntity(12L, "New Pack", "Description", "\u16CF", 3, true))) }
    }
}
