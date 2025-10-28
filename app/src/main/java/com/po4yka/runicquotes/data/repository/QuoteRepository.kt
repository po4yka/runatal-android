package com.po4yka.runicquotes.data.repository

import com.po4yka.runicquotes.domain.model.Quote
import com.po4yka.runicquotes.domain.model.RunicScript
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

    /**
     * Gets the quote of the day for the specified script.
     * This returns a consistent quote for the current day.
     */
    suspend fun quoteOfTheDay(script: RunicScript): Quote?

    /**
     * Gets a random quote for the specified script.
     */
    suspend fun randomQuote(script: RunicScript): Quote?

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
     * Inserts or updates a user-created quote.
     * @return The ID of the inserted/updated quote.
     */
    suspend fun saveUserQuote(quote: Quote): Long

    /**
     * Deletes a user-created quote.
     */
    suspend fun deleteUserQuote(quoteId: Long)

    /**
     * Gets a quote by ID.
     */
    suspend fun getQuoteById(id: Long): Quote?
}
