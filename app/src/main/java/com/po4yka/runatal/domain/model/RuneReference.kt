package com.po4yka.runatal.domain.model

/**
 * Domain model representing a rune reference entry.
 */
data class RuneReference(
    val id: Long,
    val character: String,
    val name: String,
    val pronunciation: String,
    val meaning: String,
    val history: String,
    val script: String
)
