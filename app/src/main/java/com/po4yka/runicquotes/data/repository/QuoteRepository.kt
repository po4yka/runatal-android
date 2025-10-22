package com.po4yka.runicquotes.data.repository

import com.po4yka.runicquotes.data.local.entity.QuoteEntity
import com.po4yka.runicquotes.domain.model.RunicScript
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for quote operations.
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
    suspend fun quoteOfTheDay(script: RunicScript): QuoteEntity?

    /**
     * Gets a random quote for the specified script.
     */
    suspend fun randomQuote(script: RunicScript): QuoteEntity?

    /**
     * Gets all quotes as a Flow for reactive updates.
     */
    fun getAllQuotesFlow(): Flow<List<QuoteEntity>>

    /**
     * Gets all quotes.
     */
    suspend fun getAllQuotes(): List<QuoteEntity>

    /**
     * Gets the total count of quotes in the database.
     */
    suspend fun getQuoteCount(): Int
}
