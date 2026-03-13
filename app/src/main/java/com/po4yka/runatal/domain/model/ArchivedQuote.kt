package com.po4yka.runatal.domain.model

/**
 * Domain model representing an archived or soft-deleted quote.
 */
data class ArchivedQuote(
    val id: Long,
    val originalQuoteId: Long,
    val textLatin: String,
    val author: String,
    val archivedAt: Long,
    val isDeleted: Boolean = false
)
