package com.po4yka.runatal.data.repository

import com.po4yka.runatal.data.local.dao.ArchivedQuoteDao
import com.po4yka.runatal.data.local.entity.ArchivedQuoteEntity
import com.po4yka.runatal.domain.model.ArchivedQuote
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of ArchiveRepository.
 * Maps data layer entities to domain models.
 */
@Singleton
class ArchiveRepositoryImpl @Inject constructor(
    private val archivedQuoteDao: ArchivedQuoteDao
) : ArchiveRepository {

    override fun getAllArchivedFlow(): Flow<List<ArchivedQuote>> {
        return archivedQuoteDao.getAllFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getActiveArchivedFlow(): Flow<List<ArchivedQuote>> {
        return archivedQuoteDao.getActiveFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getDeletedFlow(): Flow<List<ArchivedQuote>> {
        return archivedQuoteDao.getDeletedFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun archiveQuote(quote: ArchivedQuote): Long {
        return archivedQuoteDao.archive(quote.toEntity())
    }

    override suspend fun restoreQuote(id: Long) {
        archivedQuoteDao.restore(id)
    }

    override suspend fun softDeleteQuote(id: Long) {
        archivedQuoteDao.softDelete(id)
    }

    override suspend fun emptyTrash() {
        archivedQuoteDao.emptyTrash()
    }

    private fun ArchivedQuoteEntity.toDomain() = ArchivedQuote(
        id = id,
        originalQuoteId = originalQuoteId,
        textLatin = textLatin,
        author = author,
        archivedAt = archivedAt,
        isDeleted = isDeleted
    )

    private fun ArchivedQuote.toEntity() = ArchivedQuoteEntity(
        id = id,
        originalQuoteId = originalQuoteId,
        textLatin = textLatin,
        author = author,
        archivedAt = archivedAt,
        isDeleted = isDeleted
    )
}
