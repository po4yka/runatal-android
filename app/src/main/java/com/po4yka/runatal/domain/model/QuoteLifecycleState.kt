package com.po4yka.runatal.domain.model

/** Persisted visibility and retention states for a quote. */
enum class QuoteLifecycleState { ACTIVE, ARCHIVED, HIDDEN, TRASH }

/** A state-only mutation receipt; undo succeeds only while this exact mutation remains current. */
data class QuoteLifecycleChange(
    val quoteId: Long,
    val previousState: QuoteLifecycleState,
    val state: QuoteLifecycleState,
    val previousChangedAt: Long,
    val mutationId: String
)
