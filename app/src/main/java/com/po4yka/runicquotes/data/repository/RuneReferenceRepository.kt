package com.po4yka.runicquotes.data.repository

import com.po4yka.runicquotes.domain.model.RuneReference
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for rune reference operations.
 * Returns domain models (RuneReference) instead of data entities.
 */
interface RuneReferenceRepository {

    /**
     * Seeds the database with rune reference data if empty.
     */
    suspend fun seedIfNeeded()

    /**
     * Gets all rune references as a reactive Flow.
     */
    fun getAllRunesFlow(): Flow<List<RuneReference>>

    /**
     * Gets rune references filtered by script type.
     */
    fun getRunesByScriptFlow(script: String): Flow<List<RuneReference>>

    /**
     * Gets a single rune reference by ID.
     */
    suspend fun getRuneById(id: Long): RuneReference?

    /**
     * Inserts multiple rune references.
     */
    suspend fun insertAllRunes(runes: List<RuneReference>)
}
