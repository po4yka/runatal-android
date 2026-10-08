package com.po4yka.runatal.data.local.entity

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

/**
 * Room entity representing an archived or soft-deleted quote.
 *
 * @property id Unique identifier for the archived entry
 * @property originalQuoteId ID of the original quote this was archived from
 * @property textLatin The quote text in Latin script (snapshot at archive time)
 * @property author The author of the quote (snapshot at archive time)
 * @property archivedAt Timestamp when the quote was archived (epoch milliseconds)
 * @property isDeleted True if the quote has been moved to trash
 */
@Entity(
    tableName = "archived_quotes",
    indices = [
        Index(value = ["isDeleted"]),
        Index(value = ["archivedAt"])
    ]
)
data class ArchivedQuoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val originalQuoteId: Long,
    val textLatin: String,
    val author: String,
    val archivedAt: Long,
    val isDeleted: Boolean = false
)
