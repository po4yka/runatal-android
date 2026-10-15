package com.po4yka.runatal.data.local.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.PrimaryKey

/** A persisted bookmark retaining the reference identity through chart updates. */
@Entity(tableName = "rune_bookmarks", foreignKeys = [ForeignKey(
    entity = RuneReferenceEntity::class, parentColumns = ["id"], childColumns = ["runeId"],
    onDelete = ForeignKey.CASCADE
)])
internal data class RuneBookmarkEntity(@PrimaryKey val runeId: Long, val createdAt: Long)
