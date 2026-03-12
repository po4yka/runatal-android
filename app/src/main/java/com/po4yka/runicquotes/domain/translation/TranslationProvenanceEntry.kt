package com.po4yka.runicquotes.domain.translation

import kotlinx.serialization.Serializable

/**
 * Source attribution for a translated result or one of its stages.
 */
@Serializable
internal data class TranslationProvenanceEntry(
    val sourceId: String,
    val referenceId: String? = null,
    val label: String,
    val role: String,
    val license: String,
    val detail: String? = null,
    val url: String? = null
)
