package com.runicquotes.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.runicquotes.android.data.local.entity.QuoteEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Quote operations.
 */
@Dao
interface QuoteDao {

    /**
     * Get a random quote from the database.
     */
    @Query("SELECT * FROM quotes ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandom(): QuoteEntity?

    /**
     * Get all quotes from the database.
     */
    @Query("SELECT * FROM quotes")
    suspend fun getAll(): List<QuoteEntity>

    /**
     * Get all quotes as a Flow for reactive updates.
     */
    @Query("SELECT * FROM quotes")
    fun getAllAsFlow(): Flow<List<QuoteEntity>>

    /**
     * Get a quote by its ID.
     */
    @Query("SELECT * FROM quotes WHERE id = :id")
    suspend fun getById(id: Long): QuoteEntity?

    /**
     * Get the total count of quotes in the database.
     */
    @Query("SELECT COUNT(*) FROM quotes")
    suspend fun getCount(): Int

    /**
     * Insert multiple quotes, replacing on conflict.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(quotes: List<QuoteEntity>)

    /**
     * Insert a single quote, replacing on conflict.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(quote: QuoteEntity)

    /**
     * Delete all quotes from the database.
     */
    @Query("DELETE FROM quotes")
    suspend fun deleteAll()
}
