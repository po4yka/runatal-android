package com.po4yka.runatal.data.repository

import com.po4yka.runatal.domain.model.QuotePack
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for quote pack operations.
 * Returns domain models (QuotePack) instead of data entities.
 */
interface QuotePackRepository {

    /**
     * Seeds the database with quote pack data if empty.
     */
    suspend fun seedIfNeeded()

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

    /** Atomically toggles persisted library membership and returns its committed state. */
    suspend fun toggleLibrary(packId: Long): QuotePack

    /** Idempotently installs or removes the pack's real quote content. */
    suspend fun setLibraryMembership(packId: Long, isInLibrary: Boolean): QuotePack
}
