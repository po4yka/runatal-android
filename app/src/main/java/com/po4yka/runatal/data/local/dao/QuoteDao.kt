package com.po4yka.runatal.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Update
import androidx.room3.Transaction
import com.po4yka.runatal.data.local.entity.QuoteEntity
import com.po4yka.runatal.data.local.entity.TranslationRecordEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Quote operations.
 */
@Dao
internal interface QuoteDao {

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
    @Query(
        "SELECT canonicalKey FROM quotes WHERE canonicalKey IS NOT NULL " +
            "UNION SELECT canonicalKey FROM canonical_quote_tombstones"
    )
    suspend fun getCanonicalKeys(): List<String>

    /** Inserts canonical rows without overwriting quotes on any identity conflict. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCanonicalQuotes(quotes: List<QuoteEntity>)

    /**
     * Get a random quote from the database.
     */
    @Query("SELECT * FROM quotes WHERE lifecycleState = 'ACTIVE' ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandom(): QuoteEntity?

    /**
     * Get all quotes from the database.
     */
    @Query("SELECT * FROM quotes WHERE lifecycleState = 'ACTIVE' ORDER BY createdAt DESC, id DESC")
    suspend fun getAll(): List<QuoteEntity>

    /** Stable candidate ordering without loading quote text or glyph fields. */
    @Query("SELECT id FROM quotes WHERE lifecycleState = 'ACTIVE' ORDER BY id")
    suspend fun getQuoteIdentities(): List<Long>

    /**
     * Get all quotes as a Flow for reactive updates.
     */
    @Query("SELECT * FROM quotes WHERE lifecycleState = 'ACTIVE' ORDER BY createdAt DESC, id DESC")
    fun getAllAsFlow(): Flow<List<QuoteEntity>>

    /**
     * Get a quote by its ID.
     */
    @Query("SELECT * FROM quotes WHERE id = :id AND lifecycleState = 'ACTIVE'")
    suspend fun getById(id: Long): QuoteEntity?

    /**
     * Get the total count of quotes in the database.
     */
    @Query("SELECT COUNT(*) FROM quotes WHERE lifecycleState = 'ACTIVE'")
    suspend fun getCount(): Int

    /**
     * Get all user-created quotes.
     */
    @Query("SELECT * FROM quotes WHERE isUserCreated = 1 AND lifecycleState = 'ACTIVE' ORDER BY createdAt DESC")
    fun getUserQuotesFlow(): Flow<List<QuoteEntity>>

    /**
     * Get all favorite quotes.
     */
    @Query("SELECT * FROM quotes WHERE isFavorite = 1 AND lifecycleState = 'ACTIVE' ORDER BY createdAt DESC")
    fun getFavoritesFlow(): Flow<List<QuoteEntity>>

    /**
     * Get all favorite quotes.
     */
    @Query("SELECT * FROM quotes WHERE isFavorite = 1 AND lifecycleState = 'ACTIVE' ORDER BY createdAt DESC")
    suspend fun getFavorites(): List<QuoteEntity>

    /**
     * Toggle favorite status for a quote.
     */
    @Query("UPDATE quotes SET isFavorite = :isFavorite WHERE id = :id AND lifecycleState = 'ACTIVE'")
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

    /** Commits a new library quote and all its prepared historical records as one aggregate. */
    @Transaction
    suspend fun insertUserQuoteWithTranslations(
        quote: QuoteEntity,
        records: List<TranslationRecordEntity>
    ): Long {
        require(quote.id == 0L && quote.isUserCreated)
        require(records.isNotEmpty() && records.all { it.id == 0L && it.sourceText == quote.textLatin })
        val quoteId = insert(quote)
        insertAggregateTranslations(records.map { it.copy(quoteId = quoteId) })
        return quoteId
    }

    /** New aggregate identities have no existing cache rows; duplicate selections abort the aggregate. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAggregateTranslations(records: List<TranslationRecordEntity>)

    /** Applies a user-content change and its cache invalidation as one transaction. */
    @Transaction
    suspend fun updateUserContent(
        quote: QuoteEntity,
        expectedTextLatin: String,
        expectedAuthor: String
    ): QuoteEntity? {
        val current = getById(quote.id) ?: return null
        val sourceMatches = current.textLatin == expectedTextLatin && current.author == expectedAuthor
        return if (current.isUserCreated && sourceMatches) {
            val updated = updateContentColumns(
                id = quote.id, textLatin = quote.textLatin, author = quote.author,
                runicElder = quote.runicElder, runicYounger = quote.runicYounger, runicCirth = quote.runicCirth
            )
            check(updated == 1) { "The quote content could not be updated." }
            if (expectedTextLatin != quote.textLatin) {
                invalidateContentTranslations(quote.id)
                invalidateBackfillCompletion(quote.id)
            }
            getById(quote.id)
        } else {
            null
        }
    }

    /** Updates only content if the source snapshot is still current and user-owned. */
    @Query(
        "UPDATE quotes SET textLatin = :textLatin, author = :author, " +
            "runicElder = :runicElder, runicYounger = :runicYounger, runicCirth = :runicCirth " +
            "WHERE id = :id AND isUserCreated = 1 AND lifecycleState = 'ACTIVE'"
    )
    suspend fun updateContentColumns(
        id: Long,
        textLatin: String,
        author: String,
        runicElder: String?,
        runicYounger: String?,
        runicCirth: String?
    ): Int

    /** Invalidates derived rows within the same content-write transaction. */
    @Query("DELETE FROM translation_records WHERE quoteId = :quoteId")
    suspend fun invalidateContentTranslations(quoteId: Long)

    /** Marks a changed source dirty even if the user edits A to B and back to A. */
    @Query("DELETE FROM translation_backfill_completions WHERE quoteId = :quoteId")
    suspend fun invalidateBackfillCompletion(quoteId: Long)

    /** Emits every committed quote invalidation, even when identity query values are unchanged. */
    @Query("SELECT id FROM quotes WHERE lifecycleState = 'ACTIVE' ORDER BY id")
    fun observeQuoteIdentities(): Flow<List<Long>>

    /**
     * Update an existing quote.
     */
    @Update
    suspend fun update(quote: QuoteEntity)
}
