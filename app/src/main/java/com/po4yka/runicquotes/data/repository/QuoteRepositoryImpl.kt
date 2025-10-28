package com.po4yka.runicquotes.data.repository

import com.po4yka.runicquotes.data.local.dao.QuoteDao
import com.po4yka.runicquotes.data.local.entity.QuoteEntity
import com.po4yka.runicquotes.domain.model.Quote
import com.po4yka.runicquotes.domain.model.RunicScript
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of QuoteRepository.
 * Maps data layer entities to domain models to maintain separation of concerns.
 */
@Singleton
class QuoteRepositoryImpl @Inject constructor(
    private val quoteDao: QuoteDao
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
        val dayOfYear = LocalDate.now().dayOfYear
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
     * These are placeholder quotes that will be replaced with real content later.
     */
    private fun getInitialQuotes(): List<QuoteEntity> {
        return listOf(
            QuoteEntity(
                id = 1,
                textLatin = "The only way to do great work is to love what you do.",
                author = "Steve Jobs",
                runicElder = "ᚦᛖ ᛟᚾᛚᚤ ᚹᚨᚤ ᛏᛟ ᛞᛟ ᚷᚱᛖᚨᛏ ᚹᛟᚱᚲ ᛁᛋ ᛏᛟ ᛚᛟᚢᛖ ᚹᚺᚨᛏ ᚤᛟᚢ ᛞᛟ",
                runicYounger = null,
                runicCirth = null
            ),
            QuoteEntity(
                id = 2,
                textLatin = "Not all those who wander are lost.",
                author = "J.R.R. Tolkien",
                runicElder = "ᚾᛟᛏ ᚨᛚᛚ ᚦᛟᛋᛖ ᚹᚺᛟ ᚹᚨᚾᛞᛖᚱ ᚨᚱᛖ ᛚᛟᛋᛏ",
                runicYounger = null,
                runicCirth = null
            ),
            QuoteEntity(
                id = 3,
                textLatin = "In the middle of difficulty lies opportunity.",
                author = "Albert Einstein",
                runicElder = "ᛁᚾ ᚦᛖ ᛗᛁᛞᛞᛚᛖ ᛟᚠ ᛞᛁᚠᚠᛁᚲᚢᛚᛏᚤ ᛚᛁᛖᛋ ᛟᛈᛈᛟᚱᛏᚢᚾᛁᛏᚤ",
                runicYounger = null,
                runicCirth = null
            ),
            QuoteEntity(
                id = 4,
                textLatin = "Be yourself; everyone else is already taken.",
                author = "Oscar Wilde",
                runicElder = "ᛒᛖ ᚤᛟᚢᚱᛋᛖᛚᚠ ᛖᚢᛖᚱᚤᛟᚾᛖ ᛖᛚᛋᛖ ᛁᛋ ᚨᛚᚱᛖᚨᛞᚤ ᛏᚨᚲᛖᚾ",
                runicYounger = null,
                runicCirth = null
            ),
            QuoteEntity(
                id = 5,
                textLatin = "The journey of a thousand miles begins with one step.",
                author = "Lao Tzu",
                runicElder = "ᚦᛖ ᛃᛟᚢᚱᚾᛖᚤ ᛟᚠ ᚨ ᚦᛟᚢᛋᚨᚾᛞ ᛗᛁᛚᛖᛋ ᛒᛖᚷᛁᚾᛋ ᚹᛁᚦ ᛟᚾᛖ ᛋᛏᛖᛈ",
                runicYounger = null,
                runicCirth = null
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
