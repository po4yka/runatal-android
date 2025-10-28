package com.po4yka.runicquotes.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a quote in the database.
 *
 * @property id Unique identifier for the quote
 * @property textLatin The quote text in Latin script
 * @property author The author of the quote
 * @property runicElder The quote transliterated to Elder Futhark (optional)
 * @property runicYounger The quote transliterated to Younger Futhark (optional)
 * @property runicCirth The quote transliterated to Cirth/Angerthas (optional)
 * @property isUserCreated True if this quote was created by the user
 * @property isFavorite True if this quote is marked as favorite
 * @property createdAt Timestamp when the quote was created (epoch milliseconds)
 */
@Entity(tableName = "quotes")
data class QuoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val textLatin: String,
    val author: String,
    val runicElder: String? = null,
    val runicYounger: String? = null,
    val runicCirth: String? = null,
    val isUserCreated: Boolean = false,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
