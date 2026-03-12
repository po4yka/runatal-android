package com.po4yka.runicquotes.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.po4yka.runicquotes.data.local.entity.TranslationRecordEntity

/**
 * Data Access Object for cached historical translations.
 */
@Dao
internal interface TranslationRecordDao {

    @Query(
        """
        SELECT * FROM translation_records
        WHERE quoteId = :quoteId AND script = :script AND fidelity = :fidelity AND engineVersion = :engineVersion
        LIMIT 1
        """
    )
    suspend fun getByQuoteAndScript(
        quoteId: Long,
        script: String,
        fidelity: String,
        engineVersion: String
    ): TranslationRecordEntity?

    @Query("SELECT * FROM translation_records WHERE quoteId = :quoteId ORDER BY updatedAt DESC")
    suspend fun getByQuoteId(quoteId: Long): List<TranslationRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: TranslationRecordEntity): Long

    @Query("DELETE FROM translation_records WHERE quoteId = :quoteId")
    suspend fun deleteForQuote(quoteId: Long)
}
