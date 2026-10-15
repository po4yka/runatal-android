package com.po4yka.runatal.data.repository

import com.google.common.truth.Truth.assertThat
import com.po4yka.runatal.data.local.dao.ArchivedQuoteDao
import com.po4yka.runatal.data.local.entity.QuoteEntity
import com.po4yka.runatal.domain.model.ArchivedQuote
import com.po4yka.runatal.domain.model.QuoteLifecycleChange
import com.po4yka.runatal.domain.model.QuoteLifecycleState
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
    private lateinit var dao: ArchivedQuoteDao
    private lateinit var repository: ArchiveRepositoryImpl
    private val archived = ArchivedQuote(1L, "Stored", "Keeper", 42L)
    private val receipt = QuoteLifecycleChange(
        1L, QuoteLifecycleState.ARCHIVED, QuoteLifecycleState.ACTIVE, 42L, "restore"
    )

    @Before
    fun setUp() {
        dao = mockk()
        repository = ArchiveRepositoryImpl(dao)
    }

    @Test
    fun `retained flow uses authoritative identity and exposes all three nonactive states`() = runTest {
        val rows = listOf("ARCHIVED", "HIDDEN", "TRASH").mapIndexed { index, state ->
            QuoteEntity(id = index + 1L, textLatin = state, author = "User", lifecycleState = state,
                lifecycleChangedAt = 42L, isFavorite = true, createdAt = 12L, runicElder = "manual")
        }
        every { dao.getRetainedFlow() } returns flowOf(rows)

        val retained = repository.getRetainedQuotesFlow().first()

        assertThat(retained.map { it.id }).containsExactly(1L, 2L, 3L).inOrder()
        assertThat(retained.map { it.lifecycleState }).containsExactly(
            QuoteLifecycleState.ARCHIVED, QuoteLifecycleState.HIDDEN, QuoteLifecycleState.TRASH
        ).inOrder()
        assertThat(retained.last().isDeleted).isTrue()
        assertThat(retained.first().archivedAt).isEqualTo(42L)
        assertThat(retained.first().author).isEqualTo("User")
    }

    @Test
    fun `restore submits one batch with expected states and returns persistent undo receipts`() = runTest {
        val hidden = archived.copy(id = 2L, lifecycleState = QuoteLifecycleState.HIDDEN)
        coEvery { dao.restoreBatch(any(), any(), any()) } returns listOf(receipt)

        assertThat(repository.restoreQuotes(listOf(archived, hidden))).containsExactly(receipt)
        coVerify(exactly = 1) {
            dao.restoreBatch(listOf(1L to QuoteLifecycleState.ARCHIVED, 2L to QuoteLifecycleState.HIDDEN), any(), any())
        }
    }

    @Test
    fun `undo preserves exact receipt and does not reconstruct quote snapshots`() = runTest {
        coEvery { dao.undoBatch(listOf(receipt)) } returns Unit
        repository.undoChanges(listOf(receipt))
        coVerify(exactly = 1) { dao.undoBatch(listOf(receipt)) }
    }

    @Test
    fun `moving an archived quote to trash retains state guard and explicit purge is one operation`() = runTest {
        val deletion = receipt.copy(state = QuoteLifecycleState.TRASH)
        coEvery {
            dao.transition(1L, QuoteLifecycleState.ARCHIVED, QuoteLifecycleState.TRASH, any(), any(), false)
        } returns deletion
        coEvery { dao.emptyTrash(any()) } returns Unit
        assertThat(repository.moveToTrash(archived)).isEqualTo(deletion)
        repository.emptyTrash()
        coVerify(exactly = 1) { dao.emptyTrash(any()) }
    }

    @Test
    fun `moving an already trashed quote is rejected without a database mutation`() = runTest {
        val failure = runCatching {
            repository.moveToTrash(archived.copy(lifecycleState = QuoteLifecycleState.TRASH))
        }.exceptionOrNull()
        assertThat(failure).isInstanceOf(IllegalArgumentException::class.java)
        coVerify(exactly = 0) { dao.transition(any(), any(), any(), any(), any(), any()) }
    }
}
