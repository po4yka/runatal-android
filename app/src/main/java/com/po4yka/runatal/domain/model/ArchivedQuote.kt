package com.po4yka.runatal.domain.model

/** Archive presentation of the authoritative retained quote identity. */
data class ArchivedQuote(
    val id: Long,
    val textLatin: String,
    val author: String,
    val archivedAt: Long,
    val lifecycleState: QuoteLifecycleState = QuoteLifecycleState.ARCHIVED
) {
    /** Whether this retained quote is pending permanent removal. */
    val isDeleted: Boolean get() = lifecycleState == QuoteLifecycleState.TRASH
}
