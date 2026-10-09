package com.po4yka.runatal.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import com.po4yka.runatal.data.local.entity.TranslationRecordEntity

/**
 * Data Access Object for cached historical translations.
 */
@Dao
internal interface TranslationRecordDao {

    @Query(
        """
        SELECT records.* FROM translation_records records
        JOIN quotes quote ON quote.id = records.quoteId AND quote.textLatin = records.sourceText
        WHERE records.quoteId = :quoteId
            AND records.script = :script
            AND records.fidelity = :fidelity
            AND records.engineVersion = :engineVersion
            AND records.datasetVersion = :datasetVersion
            AND records.variant = :variant
            AND records.sourceText = :sourceText
        LIMIT 1
        """
    )
    suspend fun getBySelection(
        quoteId: Long,
        script: String,
        fidelity: String,
        variant: String,
        engineVersion: String,
        datasetVersion: String,
        sourceText: String
    ): TranslationRecordEntity?

    /** Inserts only derived output whose source still belongs to this quote, under the writer transaction. */
    @Transaction
    suspend fun insertIfSourceMatches(record: TranslationRecordEntity): Boolean {
        if (currentQuoteSource(record.quoteId) != record.sourceText) return false
        insert(record)
        return true
    }

    @Query("SELECT textLatin FROM quotes WHERE id = :quoteId")
    suspend fun currentQuoteSource(quoteId: Long): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: TranslationRecordEntity): Long

    @Query("DELETE FROM translation_records WHERE quoteId = :quoteId")
    suspend fun deleteForQuote(quoteId: Long)
}
