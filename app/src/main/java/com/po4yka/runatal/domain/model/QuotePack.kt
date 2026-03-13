package com.po4yka.runatal.domain.model

/**
 * Domain model representing a curated quote pack.
 */
data class QuotePack(
    val id: Long,
    val name: String,
    val description: String,
    val coverRune: String,
    val quoteCount: Int,
    val isInLibrary: Boolean = false
)
