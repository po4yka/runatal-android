package com.po4yka.runatal.data.repository

import com.po4yka.runatal.domain.model.ArchivedQuote
import com.po4yka.runatal.domain.model.QuoteLifecycleChange
import kotlinx.coroutines.flow.Flow

/** Retained quote lifecycle operations, with atomic batch restore, token-based undo and explicit purge. */
interface ArchiveRepository {
    /** One database snapshot supplies all non-active tabs without transient cross-flow duplication. */
    fun getRetainedQuotesFlow(): Flow<List<ArchivedQuote>>

    /** Atomically restores all expected non-active snapshots and returns their undo receipts. */
    suspend fun restoreQuotes(quotes: List<ArchivedQuote>): List<QuoteLifecycleChange>
    /** Atomically undoes a batch while every receipt still matches the latest persisted action. */
    suspend fun undoChanges(changes: List<QuoteLifecycleChange>)
    /** Moves an archived or hidden quote to retained trash. */
    suspend fun moveToTrash(quote: ArchivedQuote): QuoteLifecycleChange
    /** Explicitly purges trash quotes and their dependent rows while remembering canonical removals. */
    suspend fun emptyTrash()
}
