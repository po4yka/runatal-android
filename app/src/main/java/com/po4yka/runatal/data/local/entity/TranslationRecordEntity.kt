package com.po4yka.runatal.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Cached structured historical translation output for a quote.
 */
@Entity(
    tableName = "translation_records",
    foreignKeys = [
        ForeignKey(
            entity = QuoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["quoteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(
            value = ["quoteId", "script", "fidelity", "variant", "engineVersion", "datasetVersion"],
            unique = true
        ),
        Index(value = ["quoteId"]),
        Index(value = ["script"])
    ]
)
internal data class TranslationRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val quoteId: Long,
    val sourceText: String,
    val script: String,
    val fidelity: String,
    val derivationKind: String,
    val normalizedForm: String,
    val diplomaticForm: String,
    val glyphOutput: String,
    val historicalStage: String,
    val variant: String? = null,
    val resolutionStatus: String,
    val confidence: Float,
    val notesJson: String,
    val unresolvedTokensJson: String,
    val provenanceJson: String,
    val tokenBreakdownJson: String,
    val engineVersion: String,
    val datasetVersion: String,
    val isBackfilled: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)
