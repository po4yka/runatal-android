package com.po4yka.runatal.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import com.po4yka.runatal.data.local.entity.QuoteEntity
import com.po4yka.runatal.data.local.entity.TranslationBackfillCompletionEntity
import com.po4yka.runatal.data.local.entity.TranslationRecordEntity

/** Bounded dirty-source discovery and atomic completion of historical backfill attempts. */
@Dao
internal interface TranslationBackfillCompletionDao {
    @Query(
        """SELECT quotes.* FROM quotes WHERE lifecycleState = 'ACTIVE' AND NOT EXISTS (
            SELECT 1 FROM translation_backfill_completions completion
            WHERE completion.quoteId = quotes.id AND completion.sourceText = quotes.textLatin
                AND completion.versionFingerprint = :versionFingerprint
        ) ORDER BY quotes.id LIMIT :limit"""
    )
    suspend fun pendingQuotes(versionFingerprint: String, limit: Int): List<QuoteEntity>

    /** A stale translation attempt cannot mark a changed or deleted quote complete. */
    @Transaction
    suspend fun completeAttempt(
        completion: TranslationBackfillCompletionEntity,
        records: List<TranslationRecordEntity>
    ): Boolean {
        require(records.all { it.quoteId == completion.quoteId && it.sourceText == completion.sourceText })
        if (currentSource(completion.quoteId) != completion.sourceText) return false
        if (records.isNotEmpty()) insertMissingTranslations(records)
        insertCompletion(completion)
        return true
    }

    @Query("SELECT textLatin FROM quotes WHERE id = :quoteId AND lifecycleState = 'ACTIVE'")
    suspend fun currentSource(quoteId: Long): String?

    /** Background work must not replace an existing manually saved translation selection. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMissingTranslations(records: List<TranslationRecordEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompletion(completion: TranslationBackfillCompletionEntity)
}
