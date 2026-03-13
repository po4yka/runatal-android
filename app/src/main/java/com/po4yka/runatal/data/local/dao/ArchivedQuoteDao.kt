package com.po4yka.runatal.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.po4yka.runatal.data.local.entity.ArchivedQuoteEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for ArchivedQuote operations.
 */
@Dao
interface ArchivedQuoteDao {

    /**
     * Get all archived quotes as a reactive Flow.
     */
    @Query("SELECT * FROM archived_quotes ORDER BY archivedAt DESC")
    fun getAllFlow(): Flow<List<ArchivedQuoteEntity>>

    /**
     * Get only active (non-deleted) archived quotes.
     */
    @Query("SELECT * FROM archived_quotes WHERE isDeleted = 0 ORDER BY archivedAt DESC")
    fun getActiveFlow(): Flow<List<ArchivedQuoteEntity>>

    /**
     * Get only soft-deleted (trashed) quotes.
     */
    @Query("SELECT * FROM archived_quotes WHERE isDeleted = 1 ORDER BY archivedAt DESC")
    fun getDeletedFlow(): Flow<List<ArchivedQuoteEntity>>

    /**
     * Archive a quote by inserting it.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun archive(entity: ArchivedQuoteEntity): Long

    /**
     * Restore an archived quote by removing it from the archive table.
     */
    @Query("DELETE FROM archived_quotes WHERE id = :id")
    suspend fun restore(id: Long)

    /**
     * Soft-delete an archived quote (move to trash).
     */
    @Query("UPDATE archived_quotes SET isDeleted = 1 WHERE id = :id")
    suspend fun softDelete(id: Long)

    /**
     * Permanently delete all trashed quotes.
     */
    @Query("DELETE FROM archived_quotes WHERE isDeleted = 1")
    suspend fun emptyTrash()
}
