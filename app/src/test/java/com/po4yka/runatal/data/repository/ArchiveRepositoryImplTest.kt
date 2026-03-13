package com.po4yka.runatal.data.repository

import com.google.common.truth.Truth.assertThat
import com.po4yka.runatal.data.local.dao.ArchivedQuoteDao
import com.po4yka.runatal.data.local.entity.ArchivedQuoteEntity
import com.po4yka.runatal.domain.model.ArchivedQuote
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class ArchiveRepositoryImplTest {

    private lateinit var archivedQuoteDao: ArchivedQuoteDao
    private lateinit var repository: ArchiveRepositoryImpl

    @Before
    fun setUp() {
        archivedQuoteDao = mockk()
        repository = ArchiveRepositoryImpl(archivedQuoteDao)
    }

    @Test
    fun `getAllArchivedFlow maps entities to domain models`() = runTest {
        val entity = ArchivedQuoteEntity(
            id = 1L,
            originalQuoteId = 11L,
            textLatin = "Archive me",
            author = "Author",
            archivedAt = 1234L
        )
        every { archivedQuoteDao.getAllFlow() } returns flowOf(listOf(entity))

        val result = repository.getAllArchivedFlow().first()

        assertThat(result).containsExactly(
            ArchivedQuote(
                id = 1L,
                originalQuoteId = 11L,
                textLatin = "Archive me",
                author = "Author",
                archivedAt = 1234L
            )
        )
    }

    @Test
    fun `getDeletedFlow preserves deleted flag`() = runTest {
        val entity = ArchivedQuoteEntity(
            id = 3L,
            originalQuoteId = 33L,
            textLatin = "Deleted",
            author = "Archive",
            archivedAt = 999L,
            isDeleted = true
        )
        every { archivedQuoteDao.getDeletedFlow() } returns flowOf(listOf(entity))

        val result = repository.getDeletedFlow().first().single()

        assertThat(result.isDeleted).isTrue()
        assertThat(result.originalQuoteId).isEqualTo(33L)
    }

    @Test
    fun `getActiveArchivedFlow maps active entities`() = runTest {
        every { archivedQuoteDao.getActiveFlow() } returns flowOf(
            listOf(
                ArchivedQuoteEntity(
                    id = 2L,
                    originalQuoteId = 22L,
                    textLatin = "Visible",
                    author = "Keeper",
                    archivedAt = 4567L
                )
            )
        )

        val result = repository.getActiveArchivedFlow().first().single()

        assertThat(result.textLatin).isEqualTo("Visible")
        assertThat(result.isDeleted).isFalse()
    }

    @Test
    fun `archiveQuote maps domain model to entity`() = runTest {
        val quote = ArchivedQuote(
            id = 5L,
            originalQuoteId = 55L,
            textLatin = "Stored",
            author = "Keeper",
            archivedAt = 4000L,
            isDeleted = true
        )
        coEvery { archivedQuoteDao.archive(any()) } returns 99L

        val result = repository.archiveQuote(quote)

        assertThat(result).isEqualTo(99L)
        coVerify {
            archivedQuoteDao.archive(
                ArchivedQuoteEntity(
                    id = 5L,
                    originalQuoteId = 55L,
                    textLatin = "Stored",
                    author = "Keeper",
                    archivedAt = 4000L,
                    isDeleted = true
                )
            )
        }
    }

    @Test
    fun `restoreQuote delegates to dao`() = runTest {
        coEvery { archivedQuoteDao.restore(7L) } returns Unit

        repository.restoreQuote(7L)

        coVerify(exactly = 1) { archivedQuoteDao.restore(7L) }
    }

    @Test
    fun `softDeleteQuote delegates to dao`() = runTest {
        coEvery { archivedQuoteDao.softDelete(8L) } returns Unit

        repository.softDeleteQuote(8L)

        coVerify(exactly = 1) { archivedQuoteDao.softDelete(8L) }
    }

    @Test
    fun `emptyTrash delegates to dao`() = runTest {
        coEvery { archivedQuoteDao.emptyTrash() } returns Unit

        repository.emptyTrash()

        coVerify(exactly = 1) { archivedQuoteDao.emptyTrash() }
    }
}
