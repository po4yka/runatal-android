package com.po4yka.runatal.data.local.dao

import androidx.room3.Dao
import androidx.room3.Upsert
import androidx.room3.Query
import androidx.room3.Transaction
import com.po4yka.runatal.data.local.entity.RuneReferenceEntity
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
    @Upsert
    suspend fun insertAll(references: List<RuneReferenceEntity>)
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
