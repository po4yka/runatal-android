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
        WHERE quoteId = :quoteId
            AND script = :script
            AND fidelity = :fidelity
            AND engineVersion = :engineVersion
            AND datasetVersion = :datasetVersion
            AND ((variant IS NULL AND :variant IS NULL) OR variant = :variant)
        LIMIT 1
        """
    )
    suspend fun getBySelection(
        quoteId: Long,
        script: String,
        fidelity: String,
        variant: String?,
        engineVersion: String,
        datasetVersion: String
    ): TranslationRecordEntity?

    @Query(
        """
        SELECT * FROM translation_records
        WHERE quoteId = :quoteId
            AND script = :script
            AND resolutionStatus != :unavailableStatus
        ORDER BY updatedAt DESC
        LIMIT 1
        """
    )
    suspend fun getLatestAvailableForScript(
        quoteId: Long,
        script: String,
        unavailableStatus: String
    ): TranslationRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: TranslationRecordEntity): Long

    @Query("DELETE FROM translation_records WHERE quoteId = :quoteId")
    suspend fun deleteForQuote(quoteId: Long)
}
