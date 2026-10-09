package com.po4yka.runatal.data.local.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.PrimaryKey

/** A completed attempt, including unavailable output, for one exact quote source and engine set. */
@Entity(
    tableName = "translation_backfill_completions",
    foreignKeys = [ForeignKey(
        entity = QuoteEntity::class, parentColumns = ["id"], childColumns = ["quoteId"],
        onDelete = ForeignKey.CASCADE
    )]
)
internal data class TranslationBackfillCompletionEntity(
    @PrimaryKey val quoteId: Long,
    val sourceText: String,
    val versionFingerprint: String,
    val completedAt: Long
)
