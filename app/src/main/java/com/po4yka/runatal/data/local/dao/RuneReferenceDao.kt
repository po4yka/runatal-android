package com.po4yka.runatal.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
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
     * Insert multiple rune references, replacing on conflict.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(references: List<RuneReferenceEntity>)
}
