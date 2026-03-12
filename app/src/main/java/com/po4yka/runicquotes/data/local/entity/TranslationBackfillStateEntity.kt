package com.po4yka.runicquotes.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tracks resumable translation backfill progress.
 */
@Entity(tableName = "translation_backfill_state")
internal data class TranslationBackfillStateEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val engineVersion: String,
    val lastProcessedQuoteId: Long = 0L,
    val processedCount: Int = 0,
    val startedAt: Long = 0L,
    val updatedAt: Long = 0L,
    val completedAt: Long? = null
) {
    /** Singleton row identity used for resumable backfill state. */
    companion object {
        const val SINGLETON_ID = 1
    }
}
