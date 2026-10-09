package com.po4yka.runatal.data.local.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

/** A source quote reference without retaining a second copy of user text. */
@Entity(
    tableName = "quote_reads",
    foreignKeys = [ForeignKey(
        entity = QuoteEntity::class, parentColumns = ["id"], childColumns = ["quoteId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["quoteId", "epochDay", "script"], unique = true), Index(value = ["quoteId"])]
)
internal data class QuoteReadEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val quoteId: Long,
    val epochDay: Long,
    val script: String,
    val readAt: Long
)
