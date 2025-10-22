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
 */
@Entity(tableName = "quotes")
data class QuoteEntity(
    @PrimaryKey val id: Long,
    val textLatin: String,
    val author: String,
    val runicElder: String? = null,
    val runicYounger: String? = null,
    val runicCirth: String? = null
)
