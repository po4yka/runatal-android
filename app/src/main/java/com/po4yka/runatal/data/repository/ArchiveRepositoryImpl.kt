package com.po4yka.runatal.data.repository

import com.po4yka.runatal.data.local.dao.ArchivedQuoteDao
import com.po4yka.runatal.domain.model.ArchivedQuote
import com.po4yka.runatal.domain.model.QuoteLifecycleChange
import com.po4yka.runatal.domain.model.QuoteLifecycleState
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Reads retained authoritative quote rows and changes only their lifecycle columns. */
@Singleton
internal class ArchiveRepositoryImpl @Inject constructor(
    private val archivedQuoteDao: ArchivedQuoteDao
) : ArchiveRepository {
    override fun getRetainedQuotesFlow(): Flow<List<ArchivedQuote>> =
        archivedQuoteDao.getRetainedFlow().map { rows ->
            rows.map { row ->
                ArchivedQuote(row.id, row.textLatin, row.author, row.lifecycleChangedAt,
                    QuoteLifecycleState.valueOf(row.lifecycleState))
            }
        }

    override suspend fun restoreQuotes(quotes: List<ArchivedQuote>): List<QuoteLifecycleChange> = storageWrite {
        archivedQuoteDao.restoreBatch(
            quotes.map { it.id to it.lifecycleState }, UUID.randomUUID().toString(), System.currentTimeMillis()
        )
    }

    override suspend fun undoChanges(changes: List<QuoteLifecycleChange>) {
        storageWrite { archivedQuoteDao.undoBatch(changes) }
    }

    override suspend fun moveToTrash(quote: ArchivedQuote): QuoteLifecycleChange {
        require(
            quote.lifecycleState == QuoteLifecycleState.ARCHIVED || quote.lifecycleState == QuoteLifecycleState.HIDDEN
        )
        return storageWrite {
            archivedQuoteDao.transition(
                quote.id, quote.lifecycleState, QuoteLifecycleState.TRASH,
                UUID.randomUUID().toString(), System.currentTimeMillis()
            )
        }
    }

    override suspend fun emptyTrash() {
        storageWrite { archivedQuoteDao.emptyTrash(System.currentTimeMillis()) }
    }
}
