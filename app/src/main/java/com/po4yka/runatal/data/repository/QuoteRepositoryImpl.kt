package com.po4yka.runatal.data.repository

import com.po4yka.runatal.data.local.dao.QuoteDao
import com.po4yka.runatal.data.local.entity.QuoteEntity
import com.po4yka.runatal.data.seed.QuoteSeedData
import com.po4yka.runatal.domain.model.Quote
import com.po4yka.runatal.domain.repository.QuoteRepository
import com.po4yka.runatal.util.TimeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of QuoteRepository.
 * Maps data layer entities to domain models to maintain separation of concerns.
 */
@Singleton
internal class QuoteRepositoryImpl @Inject constructor(
    private val quoteDao: QuoteDao,
    private val timeProvider: TimeProvider
) : QuoteRepository {

    override suspend fun seedIfNeeded() {
        quoteDao.seedCanonicalQuotes(QuoteSeedData.getCanonicalQuotes())
    }

    override suspend fun quoteOfTheDay(): Quote? {
        // Ensure database is seeded
        seedIfNeeded()

        // Get a consistent quote for today based on the day of year
        val dayOfYear = timeProvider.getCurrentDayOfYear()
        val allQuotes = quoteDao.getAll()

        if (allQuotes.isEmpty()) return null

        // Use modulo to get a consistent quote for the day
        val index = dayOfYear % allQuotes.size
        return allQuotes[index].toDomain()
    }

    override suspend fun randomQuote(): Quote? {
        seedIfNeeded()
        return quoteDao.getRandom()?.toDomain()
    }

    override fun getAllQuotesFlow(): Flow<List<Quote>> {
        return quoteDao.getAllAsFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getAllQuotes(): List<Quote> {
        return quoteDao.getAll().map { it.toDomain() }
    }

    override suspend fun getQuoteCount(): Int {
        return quoteDao.getCount()
    }

    override fun getUserQuotesFlow(): Flow<List<Quote>> {
        return quoteDao.getUserQuotesFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getFavoritesFlow(): Flow<List<Quote>> {
        return quoteDao.getFavoritesFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getFavorites(): List<Quote> {
        return quoteDao.getFavorites().map { it.toDomain() }
    }

    override suspend fun toggleFavorite(quoteId: Long, isFavorite: Boolean) {
        quoteDao.updateFavoriteStatus(quoteId, isFavorite)
    }

    override suspend fun saveUserQuote(quote: Quote): Long {
        require(quote.id == 0L) { "New user quotes require a database-assigned identity." }
        return storageWrite { quoteDao.insert(quote.toEntity().copy(isUserCreated = true)) }
    }

    override suspend fun updateUserQuoteContent(
        quote: Quote,
        expectedTextLatin: String,
        expectedAuthor: String
    ): Quote {
        require(quote.id > 0L) { "Editing requires an existing quote identity." }
        val updated = checkNotNull(storageWrite {
            quoteDao.updateUserContent(quote.toEntity(), expectedTextLatin, expectedAuthor)
        }) { "The quote was deleted or its content changed. Reload it before saving." }
        return updated.toDomain()
    }

    override suspend fun restoreUserQuote(quote: Quote): Long {
        val entity = quote.toEntity().copy(
            id = quote.id,
            isUserCreated = true
        )
        return quoteDao.insert(entity)
    }

    override suspend fun deleteUserQuote(quoteId: Long) {
        quoteDao.deleteUserQuote(quoteId)
    }

    override suspend fun getQuoteById(id: Long): Quote? {
        return quoteDao.getById(id)?.toDomain()
    }

    /**
     * Maps a data layer entity to a domain model.
     * This maintains clean architecture by keeping domain models
     * independent from data layer implementation details.
     */
    private fun QuoteEntity.toDomain() = Quote(
        id = id,
        textLatin = textLatin,
        author = author,
        runicElder = runicElder,
        runicYounger = runicYounger,
        runicCirth = runicCirth,
        isUserCreated = isUserCreated,
        isFavorite = isFavorite,
        createdAt = createdAt
    )

    /**
     * Maps a domain model to a data layer entity.
     */
    private fun Quote.toEntity() = QuoteEntity(
        id = id,
        textLatin = textLatin,
        author = author,
        runicElder = runicElder,
        runicYounger = runicYounger,
        runicCirth = runicCirth,
        isUserCreated = isUserCreated,
        isFavorite = isFavorite,
        createdAt = createdAt
    )
}
