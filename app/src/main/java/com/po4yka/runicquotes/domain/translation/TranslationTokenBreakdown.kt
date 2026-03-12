package com.po4yka.runicquotes.domain.translation

import kotlinx.serialization.Serializable

/**
 * Per-token translation trace for educational UI and persistence.
 */
@Serializable
internal data class TranslationTokenBreakdown(
    val sourceToken: String,
    val normalizedToken: String,
    val diplomaticToken: String,
    val glyphToken: String,
    val resolutionStatus: TranslationResolutionStatus = TranslationResolutionStatus.UNAVAILABLE,
    val provenance: List<TranslationProvenanceEntry> = emptyList()
)
