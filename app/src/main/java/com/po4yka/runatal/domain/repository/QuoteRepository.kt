package com.po4yka.runatal.domain.repository

import com.po4yka.runatal.domain.model.Quote
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for quote operations.
 * Returns domain models (Quote) instead of data entities (QuoteEntity)
 * to maintain clean architecture and separation of concerns.
 */
interface QuoteRepository {

    /**
     * Seeds the database with initial quotes if needed.
     */
    suspend fun seedIfNeeded()

    /** Emits after committed quote writes; values must not be deduplicated by consumers. */
    fun observeQuoteChanges(): Flow<Unit>

    /**
     * Gets the quote of the day.
     * This returns a consistent quote for the current day.
     */
    suspend fun quoteOfTheDay(): Quote?

    /**
     * Gets a random quote.
     */
    suspend fun randomQuote(): Quote?

    /**
     * Gets all quotes as a Flow for reactive updates.
     */
    fun getAllQuotesFlow(): Flow<List<Quote>>

    /**
     * Gets all quotes.
     */
    suspend fun getAllQuotes(): List<Quote>

    /**
     * Gets the total count of quotes in the database.
     */
    suspend fun getQuoteCount(): Int

    /**
     * Gets all user-created quotes as a Flow for reactive updates.
     */
    fun getUserQuotesFlow(): Flow<List<Quote>>

    /**
     * Gets all favorite quotes as a Flow for reactive updates.
     */
    fun getFavoritesFlow(): Flow<List<Quote>>

    /**
     * Gets all favorite quotes.
     */
    suspend fun getFavorites(): List<Quote>

    /**
     * Toggles the favorite status of a quote.
     */
    suspend fun toggleFavorite(quoteId: Long, isFavorite: Boolean)

    /**
     * Creates a user quote with a database-assigned identity. The incoming ID must be zero.
     * @return The ID of the created quote.
     */
    suspend fun saveUserQuote(quote: Quote): Long

    /**
     * Updates editor-owned content and invalidates changed-source translations atomically.
     * Preserves favorite status, creation time and stored identity; rejects missing or changed content.
     */
    suspend fun updateUserQuoteContent(
        quote: Quote,
        expectedTextLatin: String,
        expectedAuthor: String
    ): Quote

    /**
     * Re-inserts a deleted user-created quote with its existing identity.
     * @return The restored quote ID.
     */
    suspend fun restoreUserQuote(quote: Quote): Long

    /**
     * Deletes a user-created quote.
     */
    suspend fun deleteUserQuote(quoteId: Long)

    /**
     * Gets a quote by ID.
     */
    suspend fun getQuoteById(id: Long): Quote?
}
