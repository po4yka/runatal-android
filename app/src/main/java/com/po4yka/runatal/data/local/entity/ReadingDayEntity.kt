package com.po4yka.runatal.data.local.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/** Activity contains only a calendar day, with no retained quote content. */
@Entity(tableName = "reading_days")
internal data class ReadingDayEntity(@PrimaryKey val epochDay: Long)
