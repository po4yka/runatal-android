package com.po4yka.runatal.data.repository

import com.po4yka.runatal.data.local.dao.QuotePackDao
import com.po4yka.runatal.data.local.entity.QuotePackEntity
import com.po4yka.runatal.data.seed.QuotePackSeedData
import com.po4yka.runatal.domain.model.QuotePack
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.Normalizer
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
        val existingPacks = quotePackDao.getAll()

        val syncedPacks = canonicalPacks.map { canonical ->
            val existing = existingPacks.firstOrNull { existing ->
                existing.id == canonical.id ||
                    existing.coverRune == canonical.coverRune ||
                    normalizeName(existing.name) == normalizeName(canonical.name)
            }

            canonical.copy(isInLibrary = existing?.isInLibrary ?: canonical.isInLibrary)
        }

        quotePackDao.insertAll(syncedPacks)
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

    override suspend fun insertPack(pack: QuotePack): Long {
        return quotePackDao.insert(pack.toEntity())
    }

    override suspend fun insertAllPacks(packs: List<QuotePack>) {
        quotePackDao.insertAll(packs.map { it.toEntity() })
    }

    override suspend fun updatePack(pack: QuotePack) {
        quotePackDao.update(pack.toEntity())
    }

    override suspend fun deletePack(pack: QuotePack) {
        quotePackDao.delete(pack.toEntity())
    }

    private fun QuotePackEntity.toDomain() = QuotePack(
        id = id,
        name = name,
        description = description,
        coverRune = coverRune,
        quoteCount = quoteCount,
        isInLibrary = isInLibrary
    )

    private fun QuotePack.toEntity() = QuotePackEntity(
        id = id,
        name = name,
        description = description,
        coverRune = coverRune,
        quoteCount = quoteCount,
        isInLibrary = isInLibrary
    )

    private fun normalizeName(value: String): String {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
            .replace("[^a-zA-Z0-9]+".toRegex(), "")
            .lowercase()
    }
}
