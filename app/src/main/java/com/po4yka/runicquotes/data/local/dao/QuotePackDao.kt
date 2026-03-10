package com.po4yka.runicquotes.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.po4yka.runicquotes.data.local.entity.QuotePackEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for QuotePack operations.
 */
@Dao
interface QuotePackDao {

    /**
     * Get all quote packs immediately.
     */
    @Query("SELECT * FROM quote_packs ORDER BY id ASC")
    suspend fun getAll(): List<QuotePackEntity>

    /**
     * Get all quote packs as a reactive Flow.
     */
    @Query("SELECT * FROM quote_packs ORDER BY id ASC")
    fun getAllFlow(): Flow<List<QuotePackEntity>>

    /**
     * Get a single quote pack by ID.
     */
    @Query("SELECT * FROM quote_packs WHERE id = :id")
    suspend fun getById(id: Long): QuotePackEntity?

    /**
     * Get packs that the user has added to their library.
     */
    @Query("SELECT * FROM quote_packs WHERE isInLibrary = 1 ORDER BY id ASC")
    fun getLibraryPacksFlow(): Flow<List<QuotePackEntity>>

    /**
     * Search packs by name or description.
     */
    @Query(
        "SELECT * FROM quote_packs WHERE name LIKE '%' || :query || '%' " +
            "OR description LIKE '%' || :query || '%' ORDER BY id ASC"
    )
    fun search(query: String): Flow<List<QuotePackEntity>>

    /**
     * Insert a single pack, replacing on conflict.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pack: QuotePackEntity): Long

    /**
     * Insert multiple packs, replacing on conflict.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(packs: List<QuotePackEntity>)

    /**
     * Update an existing pack.
     */
    @Update
    suspend fun update(pack: QuotePackEntity)

    /**
     * Delete a specific pack.
     */
    @Delete
    suspend fun delete(pack: QuotePackEntity)

    /**
     * Get the total number of packs in the database.
     */
    @Query("SELECT COUNT(*) FROM quote_packs")
    suspend fun getCount(): Int
}
