package com.po4yka.runicquotes.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.po4yka.runicquotes.data.local.dao.QuoteDao
import com.po4yka.runicquotes.data.local.entity.QuoteEntity

/**
 * Room database for Runic Quotes.
 */
@Database(
    entities = [QuoteEntity::class],
    version = 3,
    exportSchema = true
)
abstract class RunicQuotesDatabase : RoomDatabase() {

    /**
     * Provides access to the QuoteDao.
     */
    abstract fun quoteDao(): QuoteDao

    companion object {
        /**
         * Migration from version 1 to version 2.
         * Adds isUserCreated, isFavorite, and createdAt columns.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE quotes ADD COLUMN isUserCreated INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE quotes ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE quotes ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        /**
         * Migration from version 2 to version 3.
         * Adds indices on isUserCreated and isFavorite for query performance.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_quotes_isUserCreated ON quotes (isUserCreated)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_quotes_isFavorite ON quotes (isFavorite)"
                )
            }
        }
    }
}
