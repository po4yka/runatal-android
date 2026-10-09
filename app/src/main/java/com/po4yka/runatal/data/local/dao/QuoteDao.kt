package com.po4yka.runatal.data.local.dao

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Update
import androidx.room3.Transaction
import com.po4yka.runatal.data.local.entity.QuoteEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Quote operations.
 */
@Dao
interface QuoteDao {

    /** Seeds only missing canonical identities without replacing existing quote rows. */
    @Transaction
    suspend fun seedCanonicalQuotes(quotes: List<QuoteEntity>) {
        require(quotes.all { it.id == 0L && !it.isUserCreated && it.canonicalKey != null })
        val existingKeys = getCanonicalKeys().toSet()
        val missingQuotes = quotes.filter { it.canonicalKey !in existingKeys }
        if (missingQuotes.isNotEmpty()) {
            insertCanonicalQuotes(missingQuotes)
        }
    }

    /** Gets persisted canonical identities, independently of user rows. */
    @Query("SELECT canonicalKey FROM quotes WHERE canonicalKey IS NOT NULL")
    suspend fun getCanonicalKeys(): List<String>

    /** Inserts canonical rows without overwriting quotes on any identity conflict. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCanonicalQuotes(quotes: List<QuoteEntity>)

    /**
     * Get a random quote from the database.
     */
    @Query("SELECT * FROM quotes ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandom(): QuoteEntity?

    /**
     * Get all quotes from the database.
     */
    @Query("SELECT * FROM quotes ORDER BY createdAt DESC")
    suspend fun getAll(): List<QuoteEntity>

    /**
     * Get all quotes as a Flow for reactive updates.
     */
    @Query("SELECT * FROM quotes ORDER BY createdAt DESC")
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
     * Get all user-created quotes.
     */
    @Query("SELECT * FROM quotes WHERE isUserCreated = 1 ORDER BY createdAt DESC")
    fun getUserQuotesFlow(): Flow<List<QuoteEntity>>

    /**
     * Get all favorite quotes.
     */
    @Query("SELECT * FROM quotes WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavoritesFlow(): Flow<List<QuoteEntity>>

    /**
     * Get all favorite quotes.
     */
    @Query("SELECT * FROM quotes WHERE isFavorite = 1 ORDER BY createdAt DESC")
    suspend fun getFavorites(): List<QuoteEntity>

    /**
     * Toggle favorite status for a quote.
     */
    @Query("UPDATE quotes SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: Long, isFavorite: Boolean)

    /**
     * Insert multiple quotes, replacing on conflict.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(quotes: List<QuoteEntity>)

    /**
     * Insert a single quote, returning the row ID.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(quote: QuoteEntity): Long

    /**
     * Update an existing quote.
     */
    @Update
    suspend fun update(quote: QuoteEntity)

    /**
     * Delete a specific quote.
     */
    @Delete
    suspend fun delete(quote: QuoteEntity)

    /**
     * Delete all quotes from the database.
     */
    @Query("DELETE FROM quotes")
    suspend fun deleteAll()

    /**
     * Delete a user quote by ID (only if it's user-created).
     */
    @Query("DELETE FROM quotes WHERE id = :id AND isUserCreated = 1")
    suspend fun deleteUserQuote(id: Long)
}
