package com.po4yka.runicquotes.data.repository

import com.po4yka.runicquotes.data.local.dao.QuoteDao
import com.po4yka.runicquotes.data.local.entity.QuoteEntity
import com.po4yka.runicquotes.domain.model.Quote
import com.po4yka.runicquotes.domain.model.RunicScript
import com.po4yka.runicquotes.util.TimeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of QuoteRepository.
 * Maps data layer entities to domain models to maintain separation of concerns.
 */
@Singleton
class QuoteRepositoryImpl @Inject constructor(
    private val quoteDao: QuoteDao,
    private val timeProvider: TimeProvider
) : QuoteRepository {

    private var isSeeded = false

    override suspend fun seedIfNeeded() {
        if (isSeeded || quoteDao.getCount() > 0) {
            return
        }

        // Seed with initial quotes
        val initialQuotes = getInitialQuotes()
        quoteDao.insertAll(initialQuotes)
        isSeeded = true
    }

    override suspend fun quoteOfTheDay(script: RunicScript): Quote? {
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

    override suspend fun randomQuote(script: RunicScript): Quote? {
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
        val entity = quote.toEntity().copy(isUserCreated = true)
        return if (quote.id == 0L) {
            quoteDao.insert(entity)
        } else {
            quoteDao.update(entity)
            quote.id
        }
    }

    override suspend fun deleteUserQuote(quoteId: Long) {
        quoteDao.deleteUserQuote(quoteId)
    }

    override suspend fun getQuoteById(id: Long): Quote? {
        return quoteDao.getById(id)?.toDomain()
    }

    /**
     * Returns a list of initial quotes to seed the database with.
     * All runic transliterations are pre-computed for optimal performance.
     */
    private fun getInitialQuotes(): List<QuoteEntity> {
        return listOf(
            QuoteEntity(
                id = 1,
                textLatin = "The only way to do great work is to love what you do.",
                author = "Steve Jobs",
                runicElder = "ᚦᛖ ᛟᚾᛚᚤ ᚹᚨᚤ ᛏᛟ ᛞᛟ ᚷᚱᛖᚨᛏ ᚹᛟᚱᚲ ᛁᛋ ᛏᛟ ᛚᛟᚢᛖ ᚹᚺᚨᛏ ᚤᛟᚢ ᛞᛟ",
                runicYounger = "ᚦᛖ ᛟᚾᛚᛁ ᚹᚨᛁ ᛏᛟ ᚦᛟ ᚲᚱᛖᚨᛏ ᚹᛟᚱᚲ ᛁᛋ ᛏᛟ ᛚᛟᚢᛖ ᚹᚻᚨᛏ ᛁᛟᚢ ᚦᛟ.",
                runicCirth = "\uE088\uE0B4\uE0C9 \uE0CB\uE0B4\uE0A8\uE0C8 \uE0B8\uE0CA\uE0C8 \uE088\uE0CB \uE089\uE0CB \uE091\uE0A0\uE0C9\uE0CA\uE088 \uE0B8\uE0CB\uE0A0\uE090 \uE0C8\uE09C \uE088\uE0CB \uE0A8\uE0CB\uE0CC\uE0C9 \uE0B8\uE0B4\uE0CA\uE088 \uE0C8\uE0CB\uE0CC \uE089\uE0CB."
            ),
            QuoteEntity(
                id = 2,
                textLatin = "Not all those who wander are lost.",
                author = "J.R.R. Tolkien",
                runicElder = "ᚾᛟᛏ ᚨᛚᛚ ᚦᛟᛋᛖ ᚹᚺᛟ ᚹᚨᚾᛞᛖᚱ ᚨᚱᛖ ᛚᛟᛋᛏ",
                runicYounger = "ᚾᛟᛏ ᚨᛚᛚ ᚦᛟᛋᛖ ᚹᚻᛟ ᚹᚨᚾᚦᛖᚱ ᚨᚱᛖ ᛚᛟᛋᛏ.",
                runicCirth = "\uE0B4\uE0CB\uE088 \uE0CA\uE0A8\uE0A8 \uE088\uE0B4\uE0CB\uE09C\uE0C9 \uE0B8\uE0B4\uE0CB \uE0B8\uE0CA\uE0B4\uE089\uE0C9\uE0A0 \uE0CA\uE0A0\uE0C9 \uE0A8\uE0CB\uE09C\uE088."
            ),
            QuoteEntity(
                id = 3,
                textLatin = "In the middle of difficulty lies opportunity.",
                author = "Albert Einstein",
                runicElder = "ᛁᚾ ᚦᛖ ᛗᛁᛞᛞᛚᛖ ᛟᚠ ᛞᛁᚠᚠᛁᚲᚢᛚᛏᚤ ᛚᛁᛖᛋ ᛟᛈᛈᛟᚱᛏᚢᚾᛁᛏᚤ",
                runicYounger = "ᛁᚾ ᚦᛖ ᛗᛁᚦᚦᛚᛖ ᛟᚠ ᚦᛁᚠᚠᛁᚲᚢᛚᛏᛁ ᛚᛁᛖᛋ ᛟᛈᛈᛟᚱᛏᚢᚾᛁᛏᛁ.",
                runicCirth = "\uE0C8\uE0B4 \uE088\uE0B4\uE0C9 \uE0B0\uE0C8\uE089\uE089\uE0A8\uE0C9 \uE0CB\uE082 \uE089\uE0C8\uE082\uE082\uE0C8\uE090\uE0CC\uE0A8\uE088\uE0C8 \uE0A8\uE0C8\uE0C9\uE09C \uE0CB\uE080\uE080\uE0CB\uE0A0\uE088\uE0CC\uE0B4\uE0C8\uE088\uE0C8."
            ),
            QuoteEntity(
                id = 4,
                textLatin = "Be yourself; everyone else is already taken.",
                author = "Oscar Wilde",
                runicElder = "ᛒᛖ ᚤᛟᚢᚱᛋᛖᛚᚠ ᛖᚢᛖᚱᚤᛟᚾᛖ ᛖᛚᛋᛖ ᛁᛋ ᚨᛚᚱᛖᚨᛞᚤ ᛏᚨᚲᛖᚾ",
                runicYounger = "ᛒᛖ ᛁᛟᚢᚱᛋᛖᛚᚠ; ᛖᚢᛖᚱᛁᛟᚾᛖ ᛖᛚᛋᛖ ᛁᛋ ᚨᛚᚱᛖᚨᚦᛁ ᛏᚨᚲᛖᚾ.",
                runicCirth = "\uE081\uE0C9 \uE0C8\uE0CB\uE0CC\uE0A0\uE09C\uE0C9\uE0A8\uE082; \uE0C9\uE0CC\uE0C9\uE0A0\uE0C8\uE0CB\uE0B4\uE0C9 \uE0C9\uE0A8\uE09C\uE0C9 \uE0C8\uE09C \uE0CA\uE0A8\uE0A0\uE0C9\uE0CA\uE089\uE0C8 \uE088\uE0CA\uE090\uE0C9\uE0B4."
            ),
            QuoteEntity(
                id = 5,
                textLatin = "The journey of a thousand miles begins with one step.",
                author = "Lao Tzu",
                runicElder = "ᚦᛖ ᛃᛟᚢᚱᚾᛖᚤ ᛟᚠ ᚨ ᚦᛟᚢᛋᚨᚾᛞ ᛗᛁᛚᛖᛋ ᛒᛖᚷᛁᚾᛋ ᚹᛁᚦ ᛟᚾᛖ ᛋᛏᛖᛈ",
                runicYounger = "ᚦᛖ ᛁᛟᚢᚱᚾᛖᛁ ᛟᚠ ᚨ ᚦᛟᚢᛋᚨᚾᚦ ᛗᛁᛚᛖᛋ ᛒᛖᚲᛁᚾᛋ ᚹᛁᚦ ᛟᚾᛖ ᛋᛏᛖᛈ.",
                runicCirth = "\uE088\uE0B4\uE0C9 \uE0BC\uE0CB\uE0CC\uE0A0\uE0B4\uE0C9\uE0C8 \uE0CB\uE082 \uE0CA \uE088\uE0B4\uE0CB\uE0CC\uE09C\uE0CA\uE0B4\uE089 \uE0B0\uE0C8\uE0A8\uE0C9\uE09C \uE081\uE0C9\uE091\uE0C8\uE0B4\uE09C \uE0B8\uE0C8\uE088\uE0B4 \uE0CB\uE0B4\uE0C9 \uE09C\uE088\uE0C9\uE080."
            )
        )
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
