package com.po4yka.runatal.data.repository

import com.google.common.truth.Truth.assertThat
import com.po4yka.runatal.data.local.dao.QuotePackDao
import com.po4yka.runatal.data.local.entity.QuotePackEntity
import com.po4yka.runatal.data.seed.QuotePackSeedData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class QuotePackRepositoryImplTest {
    private val dao = mockk<QuotePackDao>()
    private val repository = QuotePackRepositoryImpl(dao)
    private val entity = QuotePackEntity(1L, "Hávamál Selections", "Description", "ᚺ", 4, false)

    @Test
    fun `seeding delegates real canonical content once and retries after failure`() = runTest {
        coEvery { dao.seedCanonicalPacks(any(), any()) } throws IllegalStateException("disk") andThen Unit
        try {
            repository.seedIfNeeded()
            error("Expected storage failure")
        } catch (expected: IllegalStateException) {
            assertThat(expected.message).isEqualTo("disk")
        }
        repository.seedIfNeeded()
        repository.seedIfNeeded()
        coVerify(exactly = 2) {
            dao.seedCanonicalPacks(QuotePackSeedData.getInitialPacks(), match { content ->
                content.size == 6 && content.values.all { it.size == 4 }
            })
        }
    }

    @Test
    fun `flow and lookup operations expose committed database metadata`() = runTest {
        every { dao.getAllFlow() } returns flowOf(listOf(entity))
        every { dao.getLibraryPacksFlow() } returns flowOf(listOf(entity.copy(isInLibrary = true)))
        every { dao.search("Hávamál") } returns flowOf(listOf(entity))
        coEvery { dao.getById(1L) } returns entity
        coEvery { dao.getById(99L) } returns null
        assertThat(repository.getAllPacksFlow().first().single().quoteCount).isEqualTo(4)
        assertThat(repository.getLibraryPacksFlow().first().single().isInLibrary).isTrue()
        assertThat(repository.searchPacks("Hávamál").first().single().name).isEqualTo(entity.name)
        assertThat(repository.getPackById(1L)?.id).isEqualTo(1L)
        assertThat(repository.getPackById(99L)).isNull()
    }

    @Test
    fun `library commands supply real content and return committed membership`() = runTest {
        coEvery { dao.toggleLibrary(1L, any()) } returns entity.copy(isInLibrary = true)
        coEvery { dao.setLibraryMembership(1L, false, any()) } returns entity
        assertThat(repository.toggleLibrary(1L).isInLibrary).isTrue()
        assertThat(repository.setLibraryMembership(1L, false).isInLibrary).isFalse()
        coVerify { dao.toggleLibrary(1L, QuotePackSeedData.getPackQuotes(1L)) }
        coVerify { dao.setLibraryMembership(1L, false, QuotePackSeedData.getPackQuotes(1L)) }
    }
}
