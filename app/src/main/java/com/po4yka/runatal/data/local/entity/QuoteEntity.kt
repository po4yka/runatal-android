package com.po4yka.runatal.data.local.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.po4yka.runatal.domain.translation.TranslationMode
import com.po4yka.runatal.domain.translation.TranslationFidelity
import com.po4yka.runatal.domain.translation.YoungerFutharkVariant

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
 * @property canonicalKey Stable identity for canonical quotes; null for user-created quotes
 * @property createdAt Timestamp when the quote was created (epoch milliseconds)
 */
@Entity(
    tableName = "quotes",
    indices = [
        Index(value = ["isUserCreated"]),
        Index(value = ["isFavorite"]),
        Index(value = ["canonicalKey"], unique = true)
    ]
)
data class QuoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val textLatin: String,
    val author: String,
    val runicElder: String? = null,
    val runicYounger: String? = null,
    val runicCirth: String? = null,
    val isUserCreated: Boolean = false,
    val isFavorite: Boolean = false,
    val createdAt: Long = 0L,
    val canonicalKey: String? = null,
    @ColumnInfo(defaultValue = "'TRANSLITERATE'")
    val renderingMode: String = TranslationMode.TRANSLITERATE.name,
    @ColumnInfo(defaultValue = "'STRICT'")
    val renderingFidelity: String = TranslationFidelity.STRICT.name,
    @ColumnInfo(defaultValue = "'LONG_BRANCH'")
    val renderingYoungerVariant: String = YoungerFutharkVariant.LONG_BRANCH.name
)
