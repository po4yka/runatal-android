package com.po4yka.runatal.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.po4yka.runatal.data.local.entity.TranslationBackfillStateEntity

/**
 * Data Access Object for translation backfill progress.
 */
@Dao
internal interface TranslationBackfillStateDao {

    @Query("SELECT * FROM translation_backfill_state WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int = TranslationBackfillStateEntity.SINGLETON_ID): TranslationBackfillStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: TranslationBackfillStateEntity)
}
