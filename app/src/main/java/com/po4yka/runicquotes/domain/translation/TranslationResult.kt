package com.po4yka.runicquotes.domain.translation

import com.po4yka.runicquotes.domain.model.RunicScript

/**
 * Structured translation output with intermediate layers.
 */
internal data class TranslationResult(
    val sourceText: String,
    val script: RunicScript,
    val fidelity: TranslationFidelity,
    val historicalStage: HistoricalStage,
    val normalizedForm: String,
    val diplomaticForm: String,
    val glyphOutput: String,
    val requestedVariant: String? = null,
    val resolutionStatus: TranslationResolutionStatus = TranslationResolutionStatus.UNAVAILABLE,
    val confidence: Float = 0f,
    val notes: List<String> = emptyList(),
    val unresolvedTokens: List<String> = emptyList(),
    val provenance: List<TranslationProvenanceEntry> = emptyList(),
    val tokenBreakdown: List<TranslationTokenBreakdown> = emptyList(),
    val engineVersion: String,
    val datasetVersion: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt
)
