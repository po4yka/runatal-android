package com.po4yka.runatal.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Update
import com.po4yka.runatal.data.local.entity.PackQuoteEntity
import com.po4yka.runatal.data.local.entity.QuoteEntity
import com.po4yka.runatal.data.local.entity.QuotePackEntity
import kotlinx.coroutines.flow.Flow

/** Owns the atomic pack, quote-content and membership aggregate. */
@Dao
interface QuotePackDao {
    /** Observes all pack metadata. */
    @Query("SELECT * FROM quote_packs ORDER BY id ASC")
    fun getAllFlow(): Flow<List<QuotePackEntity>>

    /** Gets persisted membership state before an atomic command. */
    @Query("SELECT * FROM quote_packs WHERE id = :id")
    suspend fun getById(id: Long): QuotePackEntity?

    /** Observes installed packs. */
    @Query("SELECT * FROM quote_packs WHERE isInLibrary = 1 ORDER BY id ASC")
    fun getLibraryPacksFlow(): Flow<List<QuotePackEntity>>

    /** Searches pack metadata. */
    @Query("SELECT * FROM quote_packs WHERE name LIKE '%' || :query || '%' " +
        "OR description LIKE '%' || :query || '%' ORDER BY id ASC")
    fun search(query: String): Flow<List<QuotePackEntity>>

    /** Inserts missing packs without cascading deletion of memberships. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(pack: QuotePackEntity): Long

    /** Updates metadata in place. */
    @Update
    suspend fun update(pack: QuotePackEntity)

    /** Reuses identical library content, including favorite, user and shared quotes. */
    @Query("SELECT * FROM quotes WHERE textLatin=:text AND author=:author " +
        "ORDER BY CASE WHEN canonicalKey=:key THEN 0 ELSE 1 END, id LIMIT 1")
    suspend fun findContent(text: String, author: String, key: String): QuoteEntity?

    /** Inserts pack-owned canonical content without replacing any existing row. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertContent(quote: QuoteEntity): Long

    /** Idempotently attaches existing quote content. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMembership(membership: PackQuoteEntity)

    /** Detaches a pack while preserving shared quotes. */
    @Query("DELETE FROM pack_quotes WHERE packId=:packId")
    suspend fun detachPack(packId: Long)

    /** Deletes only unprotected content owned by the removed pack and no other pack. */
    @Query("DELETE FROM quotes WHERE canonicalKey LIKE 'pack:%' AND isFavorite=0 AND isUserCreated=0 " +
        "AND id IN (SELECT quoteId FROM pack_quotes WHERE packId=:packId) " +
        "AND NOT EXISTS (SELECT 1 FROM pack_quotes WHERE quoteId=quotes.id AND packId<>:packId)")
    suspend fun deleteUnprotectedContent(packId: Long)

    /** Refreshes truthful metadata and materializes installed packs in one transaction. */
    @Transaction
    suspend fun seedCanonicalPacks(packs: List<QuotePackEntity>, content: Map<Long, List<QuoteEntity>>) {
        packs.forEach { canonical ->
            insert(canonical)
            val existing = checkNotNull(getById(canonical.id))
            val quotes = content.getValue(canonical.id)
            val synced = canonical.copy(isInLibrary = existing.isInLibrary, quoteCount = quotes.size)
            update(synced)
            if (synced.isInLibrary) installContent(synced.id, quotes)
        }
    }

    /** Toggles persisted state, content and joins together; callers cannot race stale copies. */
    @Transaction
    suspend fun toggleLibrary(packId: Long, content: List<QuoteEntity>): QuotePackEntity {
        val current = checkNotNull(getById(packId)) { "Pack not found" }
        check(content.isNotEmpty()) { "No quote content is available for this pack" }
        return setLibraryMembership(packId, !current.isInLibrary, content)
    }

    /** Idempotently adds or removes real content and commits the membership flag together. */
    @Transaction
    suspend fun setLibraryMembership(
        packId: Long,
        isInLibrary: Boolean,
        content: List<QuoteEntity>
    ): QuotePackEntity {
        val current = checkNotNull(getById(packId)) { "Pack not found" }
        check(content.isNotEmpty()) { "No quote content is available for this pack" }
        if (isInLibrary) {
            installContent(packId, content)
        } else {
            deleteUnprotectedContent(packId)
            detachPack(packId)
        }
        val updated = current.copy(isInLibrary = isInLibrary, quoteCount = content.size)
        update(updated)
        return updated
    }

    private suspend fun installContent(packId: Long, content: List<QuoteEntity>) {
        content.forEach { quote ->
            check(!quote.isUserCreated && quote.id == 0L) { "Pack content must use database-assigned identities" }
            val key = checkNotNull(quote.canonicalKey)
            val existing = findContent(quote.textLatin, quote.author, key)
            val quoteId = existing?.id ?: insertContent(quote)
            check(quoteId > 0L) { "Pack content identity conflicts with an existing quote" }
            insertMembership(PackQuoteEntity(packId, quoteId))
        }
    }
}
