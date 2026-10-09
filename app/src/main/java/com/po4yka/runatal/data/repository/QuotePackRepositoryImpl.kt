package com.po4yka.runatal.data.repository

import com.po4yka.runatal.data.local.dao.QuotePackDao
import com.po4yka.runatal.data.local.entity.QuotePackEntity
import com.po4yka.runatal.data.seed.QuotePackSeedData
import com.po4yka.runatal.domain.model.QuotePack
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of QuotePackRepository.
 * Maps data layer entities to domain models.
 */
@Singleton
class QuotePackRepositoryImpl @Inject constructor(
    private val quotePackDao: QuotePackDao
) : QuotePackRepository {

    private var isSeeded = false

    override suspend fun seedIfNeeded() {
        if (isSeeded) {
            return
        }

        val canonicalPacks = QuotePackSeedData.getInitialPacks()
        quotePackDao.seedCanonicalPacks(
            canonicalPacks,
            canonicalPacks.associate { it.id to QuotePackSeedData.getPackQuotes(it.id) }
        )
        isSeeded = true
    }

    override fun getAllPacksFlow(): Flow<List<QuotePack>> {
        return quotePackDao.getAllFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getPackById(id: Long): QuotePack? {
        return quotePackDao.getById(id)?.toDomain()
    }

    override fun getLibraryPacksFlow(): Flow<List<QuotePack>> {
        return quotePackDao.getLibraryPacksFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun searchPacks(query: String): Flow<List<QuotePack>> {
        return quotePackDao.search(query).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun toggleLibrary(packId: Long): QuotePack =
        quotePackDao.toggleLibrary(packId, QuotePackSeedData.getPackQuotes(packId)).toDomain()

    override suspend fun setLibraryMembership(packId: Long, isInLibrary: Boolean): QuotePack =
        quotePackDao.setLibraryMembership(packId, isInLibrary, QuotePackSeedData.getPackQuotes(packId)).toDomain()

    private fun QuotePackEntity.toDomain() = QuotePack(
        id = id,
        name = name,
        description = description,
        coverRune = coverRune,
        quoteCount = quoteCount,
        isInLibrary = isInLibrary
    )
}
