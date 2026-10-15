package com.po4yka.runatal.data.local.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/** Prevents automatic catalogs from resurrecting canonical quotes explicitly purged by the user. */
@Entity(tableName = "canonical_quote_tombstones")
internal data class CanonicalQuoteTombstoneEntity(@PrimaryKey val canonicalKey: String, val purgedAt: Long)
