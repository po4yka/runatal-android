package com.po4yka.runicquotes.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Room entity representing the many-to-many relationship between packs and quotes.
 *
 * Composite primary key of [packId] and [quoteId] ensures each quote
 * appears at most once per pack.
 *
 * @property packId Foreign key referencing [QuotePackEntity.id]
 * @property quoteId Foreign key referencing [QuoteEntity.id]
 */
@Entity(
    tableName = "pack_quotes",
    primaryKeys = ["packId", "quoteId"],
    foreignKeys = [
        ForeignKey(
            entity = QuotePackEntity::class,
            parentColumns = ["id"],
            childColumns = ["packId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = QuoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["quoteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["quoteId"])
    ]
)
data class PackQuoteEntity(
    val packId: Long,
    val quoteId: Long
)
