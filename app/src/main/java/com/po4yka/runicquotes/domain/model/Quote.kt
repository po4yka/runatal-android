package com.po4yka.runicquotes.domain.model

/**
 * Domain model representing a quote.
 * This is separate from the data layer entity (QuoteEntity) to follow
 * clean architecture principles and maintain separation of concerns.
 */
data class Quote(
    val id: Long,
    val textLatin: String,
    val author: String,
    val runicElder: String?,
    val runicYounger: String?,
    val runicCirth: String?,
    val isUserCreated: Boolean = false,
    val isFavorite: Boolean = false,
    val createdAt: Long = 0L
)
