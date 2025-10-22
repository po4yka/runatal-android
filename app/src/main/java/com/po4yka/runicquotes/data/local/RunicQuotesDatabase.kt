package com.po4yka.runicquotes.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.po4yka.runicquotes.data.local.dao.QuoteDao
import com.po4yka.runicquotes.data.local.entity.QuoteEntity

/**
 * Room database for Runic Quotes.
 */
@Database(
    entities = [QuoteEntity::class],
    version = 1,
    exportSchema = true
)
abstract class RunicQuotesDatabase : RoomDatabase() {

    /**
     * Provides access to the QuoteDao.
     */
    abstract fun quoteDao(): QuoteDao
}
