package com.po4yka.runatal.data.repository

import com.po4yka.runatal.data.local.dao.QuoteDao
import com.po4yka.runatal.data.local.dao.ArchivedQuoteDao
import com.po4yka.runatal.data.local.entity.QuoteEntity
import com.po4yka.runatal.data.preferences.UserPreferencesManager
import com.po4yka.runatal.data.seed.QuoteSeedData
import com.po4yka.runatal.domain.model.Quote
import com.po4yka.runatal.domain.model.QuoteLifecycleChange
import com.po4yka.runatal.domain.model.QuoteLifecycleState
import com.po4yka.runatal.domain.repository.QuoteRepository
import com.po4yka.runatal.util.TimeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.po4yka.runatal.domain.translation.TranslationMode
import com.po4yka.runatal.domain.translation.TranslationFidelity
import com.po4yka.runatal.domain.translation.YoungerFutharkVariant
import javax.inject.Inject
import javax.inject.Singleton
import java.util.UUID

/**
 * Implementation of QuoteRepository.
 * Maps data layer entities to domain models to maintain separation of concerns.
 */
@Singleton
internal class QuoteRepositoryImpl @Inject constructor(
    private val quoteDao: QuoteDao,
    private val timeProvider: TimeProvider,
    private val userPreferencesManager: UserPreferencesManager,
    private val archivedQuoteDao: ArchivedQuoteDao
) : QuoteRepository {

    override fun observeQuoteChanges(): Flow<Unit> = quoteDao.observeQuoteIdentities().map { Unit }

    override suspend fun seedIfNeeded() {
        quoteDao.seedCanonicalQuotes(QuoteSeedData.getCanonicalQuotes())
    }

    override suspend fun quoteOfTheDay(): Quote? {
        seedIfNeeded()
        val epochDay = timeProvider.getCurrentDate().toEpochDay()
        while (true) {
            val selectedId = userPreferencesManager.selectDailyQuote(epochDay) { quoteDao.getQuoteIdentities() }
                ?: return null
            val selected = quoteDao.getById(selectedId)
            if (selected != null) return selected.toDomain()
        }
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
        require(quote.id == 0L && quote.lifecycleState == QuoteLifecycleState.ACTIVE) {
            "New user quotes require an active, database-assigned identity."
        }
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

    override suspend fun deleteUserQuote(quoteId: Long): QuoteLifecycleChange = storageWrite {
        archivedQuoteDao.transition(
            quoteId, QuoteLifecycleState.ACTIVE, QuoteLifecycleState.TRASH,
            UUID.randomUUID().toString(), System.currentTimeMillis(), onlyUserCreated = true
        )
    }

    override suspend fun archiveQuote(quoteId: Long): QuoteLifecycleChange =
        moveActive(quoteId, QuoteLifecycleState.ARCHIVED)

    override suspend fun hideQuote(quoteId: Long): QuoteLifecycleChange =
        moveActive(quoteId, QuoteLifecycleState.HIDDEN)

    override suspend fun undoLifecycleChange(change: QuoteLifecycleChange) {
        storageWrite { archivedQuoteDao.undoBatch(listOf(change)) }
    }

    private suspend fun moveActive(quoteId: Long, state: QuoteLifecycleState): QuoteLifecycleChange = storageWrite {
        archivedQuoteDao.transition(
            quoteId, QuoteLifecycleState.ACTIVE, state, UUID.randomUUID().toString(), System.currentTimeMillis()
        )
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
        createdAt = createdAt,
        renderingMode = TranslationMode.valueOf(renderingMode),
        renderingFidelity = TranslationFidelity.valueOf(renderingFidelity),
        renderingYoungerVariant = YoungerFutharkVariant.valueOf(renderingYoungerVariant),
        lifecycleState = QuoteLifecycleState.valueOf(lifecycleState)
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
        createdAt = createdAt,
        renderingMode = renderingMode.name,
        renderingFidelity = renderingFidelity.name,
        renderingYoungerVariant = renderingYoungerVariant.name,
        lifecycleState = lifecycleState.name
    )
}
