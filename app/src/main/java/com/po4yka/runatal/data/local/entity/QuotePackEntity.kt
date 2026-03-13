package com.po4yka.runatal.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a curated quote pack.
 *
 * @property id Unique identifier for the pack
 * @property name Display name of the pack
 * @property description Short description of the pack contents
 * @property coverRune Rune character displayed on the pack cover
 * @property quoteCount Number of quotes in the pack
 * @property isInLibrary True if the user has added this pack to their library
 */
@Entity(
    tableName = "quote_packs",
    indices = [
        Index(value = ["isInLibrary"])
    ]
)
data class QuotePackEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
    val coverRune: String,
    val quoteCount: Int,
    val isInLibrary: Boolean = false
)
