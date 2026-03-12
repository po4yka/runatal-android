package com.po4yka.runicquotes.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.po4yka.runicquotes.data.local.entity.TranslationBackfillStateEntity

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
