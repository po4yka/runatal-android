package com.po4yka.runicquotes.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a rune reference entry.
 *
 * @property id Unique identifier for the rune reference
 * @property character The rune character (Unicode)
 * @property name Traditional name of the rune
 * @property pronunciation How the rune is pronounced
 * @property meaning Symbolic meaning of the rune
 * @property history Historical context and usage
 * @property script The runic script this rune belongs to (e.g. "elder_futhark")
 */
@Entity(
    tableName = "rune_references",
    indices = [
        Index(value = ["script"])
    ]
)
data class RuneReferenceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val character: String,
    val name: String,
    val pronunciation: String,
    val meaning: String,
    val history: String,
    val script: String
)
