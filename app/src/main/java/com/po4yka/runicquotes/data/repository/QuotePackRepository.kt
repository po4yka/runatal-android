package com.po4yka.runicquotes.data.repository

import com.po4yka.runicquotes.domain.model.QuotePack
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for quote pack operations.
 * Returns domain models (QuotePack) instead of data entities.
 */
interface QuotePackRepository {

    /**
     * Gets all quote packs as a reactive Flow.
     */
    fun getAllPacksFlow(): Flow<List<QuotePack>>

    /**
     * Gets a single pack by ID.
     */
    suspend fun getPackById(id: Long): QuotePack?

    /**
     * Gets packs that the user has added to their library.
     */
    fun getLibraryPacksFlow(): Flow<List<QuotePack>>

    /**
     * Searches packs by name or description.
     */
    fun searchPacks(query: String): Flow<List<QuotePack>>

    /**
     * Inserts a pack, returning its ID.
     */
    suspend fun insertPack(pack: QuotePack): Long

    /**
     * Inserts multiple packs.
     */
    suspend fun insertAllPacks(packs: List<QuotePack>)

    /**
     * Updates an existing pack.
     */
    suspend fun updatePack(pack: QuotePack)

    /**
     * Deletes a pack.
     */
    suspend fun deletePack(pack: QuotePack)
}
