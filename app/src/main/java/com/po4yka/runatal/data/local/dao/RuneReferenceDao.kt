package com.po4yka.runatal.data.local.dao

import androidx.room3.Dao
import androidx.room3.Upsert
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Update
import androidx.room3.Query
import androidx.room3.Transaction
import com.po4yka.runatal.data.local.entity.RuneReferenceEntity
import com.po4yka.runatal.data.seed.RuneReferenceCanonicalIdentity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for RuneReference operations.
 */
@Dao
interface RuneReferenceDao {

    /**
     * Get all rune references as a reactive Flow.
     */
    @Query("SELECT * FROM rune_references ORDER BY script ASC, name ASC")
    fun getAllFlow(): Flow<List<RuneReferenceEntity>>

    /**
     * Get rune references filtered by script type.
     */
    @Query("SELECT * FROM rune_references WHERE script = :script ORDER BY name ASC")
    fun getByScriptFlow(script: String): Flow<List<RuneReferenceEntity>>

    /**
     * Get a single rune reference by ID.
     */
    @Query("SELECT * FROM rune_references WHERE id = :id")
    suspend fun getById(id: Long): RuneReferenceEntity?

    /**
     * Get the total count of rune references.
     */
    @Query("SELECT COUNT(*) FROM rune_references")
    suspend fun getCount(): Int

    /**
     * Update reference metadata in place so saved rune identities survive.
     */
    @Transaction
    suspend fun insertAll(references: List<RuneReferenceEntity>) {
        val preserved = references.map { incoming ->
            val current = if (incoming.id > 0) getById(incoming.id) else null
            incoming.copy(canonicalKey = current?.canonicalKey, canonicalFingerprint = current?.canonicalFingerprint)
        }
        upsertReferences(preserved)
    }

    /** Writes reference rows without replacing their identities. */
    @Upsert
    suspend fun upsertReferences(references: List<RuneReferenceEntity>)

    /** Reconciles every canonical identity under a writer transaction, including a partially populated database. */
    @Transaction
    suspend fun seedCanonicalReferences(references: List<RuneReferenceEntity>, knownLegacy: List<RuneReferenceEntity>) {
        require(references.all { it.id == 0L && it.canonicalKey != null && it.canonicalFingerprint != null })
        references.forEach { canonical ->
            val rows = matchingReferences(checkNotNull(canonical.canonicalKey), canonical.script, canonical.name)
            val owned = rows.firstOrNull { it.canonicalKey == canonical.canonicalKey }
            val proven = RuneReferenceCanonicalIdentity.provenDuplicates(canonical, rows, knownLegacy)
            val survivor = owned ?: proven.firstOrNull()
            if (survivor == null) {
                insertCanonical(canonical)
            } else {
                val duplicates = proven.filter { it.id != survivor.id }.map { it.id }
                if (duplicates.isNotEmpty()) {
                    transferBookmarks(survivor.id, duplicates)
                    deleteCanonicalDuplicates(duplicates)
                }
                val unmodified = owned == null ||
                    RuneReferenceCanonicalIdentity.fingerprint(survivor) == survivor.canonicalFingerprint
                if (unmodified) updateCanonical(canonical.copy(id = survivor.id))
            }
        }
    }

    /** Gets canonical identity candidates and same-name user rows for exact tuple validation. */
    @Query("SELECT * FROM rune_references WHERE canonicalKey = :key OR (script = :script AND name = :name) ORDER BY id")
    suspend fun matchingReferences(key: String, script: String, name: String): List<RuneReferenceEntity>

    /** Adds a missing canonical identity without replacing existing records. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCanonical(reference: RuneReferenceEntity)

    /** Updates only a proven unchanged canonical row. */
    @Update
    suspend fun updateCanonical(reference: RuneReferenceEntity)

    /** Keep an existing survivor bookmark timestamp, or transfer the earliest proven duplicate bookmark. */
    @Query("INSERT OR IGNORE INTO rune_bookmarks(runeId, createdAt) " +
        "SELECT :survivorId, createdAt FROM rune_bookmarks WHERE runeId IN (:duplicateIds) ORDER BY createdAt LIMIT 1")
    suspend fun transferBookmarks(survivorId: Long, duplicateIds: List<Long>)

    /** Removes proven duplicates after transferring their bookmarks. */
    @Query("DELETE FROM rune_references WHERE id IN (:duplicateIds)")
    suspend fun deleteCanonicalDuplicates(duplicateIds: List<Long>)
    /** Live bookmarked references, including updated glyph metadata. */
    @Query("SELECT rune_references.* FROM rune_references JOIN rune_bookmarks " +
        "ON rune_references.id = rune_bookmarks.runeId ORDER BY rune_bookmarks.createdAt DESC, rune_references.id DESC")
    fun getBookmarkedFlow(): Flow<List<RuneReferenceEntity>>

    /** Observes the saved state of one reference. */
    @Query("SELECT EXISTS(SELECT 1 FROM rune_bookmarks WHERE runeId = :id)")
    fun observeBookmark(id: Long): Flow<Boolean>

    /** Reads saved state inside the toggle transaction. */
    @Query("SELECT EXISTS(SELECT 1 FROM rune_bookmarks WHERE runeId = :id)")
    suspend fun isBookmarked(id: Long): Boolean

    /** Saves an existing reference identity. */
    @Query("INSERT INTO rune_bookmarks(runeId, createdAt) " +
        "SELECT :id, :createdAt WHERE EXISTS(SELECT 1 FROM rune_references WHERE id = :id)")
    suspend fun insertBookmark(id: Long, createdAt: Long)

    /** Removes only the bookmark. */
    @Query("DELETE FROM rune_bookmarks WHERE runeId = :id")
    suspend fun deleteBookmark(id: Long)

    /** Toggles under a writer transaction so repeated/concurrent actions cannot lose updates. */
    @Transaction
    suspend fun toggleBookmark(id: Long) {
        checkNotNull(getById(id)) { "Rune no longer exists." }
        if (isBookmarked(id)) deleteBookmark(id) else insertBookmark(id, System.currentTimeMillis())
    }

}
