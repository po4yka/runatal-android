package com.po4yka.runicquotes.data.repository

import com.po4yka.runicquotes.domain.model.ArchivedQuote
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for archive operations.
 * Returns domain models (ArchivedQuote) instead of data entities.
 */
interface ArchiveRepository {

    /**
     * Gets all archived quotes as a reactive Flow.
     */
    fun getAllArchivedFlow(): Flow<List<ArchivedQuote>>

    /**
     * Gets only active (non-deleted) archived quotes.
     */
    fun getActiveArchivedFlow(): Flow<List<ArchivedQuote>>

    /**
     * Gets only soft-deleted (trashed) quotes.
     */
    fun getDeletedFlow(): Flow<List<ArchivedQuote>>

    /**
     * Archives a quote.
     * @return The ID of the archived entry.
     */
    suspend fun archiveQuote(quote: ArchivedQuote): Long

    /**
     * Restores an archived quote by removing it from the archive.
     */
    suspend fun restoreQuote(id: Long)

    /**
     * Soft-deletes an archived quote (moves to trash).
     */
    suspend fun softDeleteQuote(id: Long)

    /**
     * Permanently deletes all trashed quotes.
     */
    suspend fun emptyTrash()
}
