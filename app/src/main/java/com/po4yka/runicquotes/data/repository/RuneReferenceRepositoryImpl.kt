package com.po4yka.runicquotes.data.repository

import com.po4yka.runicquotes.data.local.dao.RuneReferenceDao
import com.po4yka.runicquotes.data.local.entity.RuneReferenceEntity
import com.po4yka.runicquotes.domain.model.RuneReference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of RuneReferenceRepository.
 * Maps data layer entities to domain models.
 */
@Singleton
class RuneReferenceRepositoryImpl @Inject constructor(
    private val runeReferenceDao: RuneReferenceDao
) : RuneReferenceRepository {

    override fun getAllRunesFlow(): Flow<List<RuneReference>> {
        return runeReferenceDao.getAllFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getRunesByScriptFlow(script: String): Flow<List<RuneReference>> {
        return runeReferenceDao.getByScriptFlow(script).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getRuneById(id: Long): RuneReference? {
        return runeReferenceDao.getById(id)?.toDomain()
    }

    override suspend fun insertAllRunes(runes: List<RuneReference>) {
        runeReferenceDao.insertAll(runes.map { it.toEntity() })
    }

    private fun RuneReferenceEntity.toDomain() = RuneReference(
        id = id,
        character = character,
        name = name,
        pronunciation = pronunciation,
        meaning = meaning,
        history = history,
        script = script
    )

    private fun RuneReference.toEntity() = RuneReferenceEntity(
        id = id,
        character = character,
        name = name,
        pronunciation = pronunciation,
        meaning = meaning,
        history = history,
        script = script
    )
}
