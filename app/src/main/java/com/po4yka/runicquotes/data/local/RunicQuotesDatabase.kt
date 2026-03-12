package com.po4yka.runicquotes.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.po4yka.runicquotes.data.local.dao.ArchivedQuoteDao
import com.po4yka.runicquotes.data.local.dao.QuoteDao
import com.po4yka.runicquotes.data.local.dao.QuotePackDao
import com.po4yka.runicquotes.data.local.dao.RuneReferenceDao
import com.po4yka.runicquotes.data.local.dao.TranslationBackfillStateDao
import com.po4yka.runicquotes.data.local.dao.TranslationRecordDao
import com.po4yka.runicquotes.data.local.entity.ArchivedQuoteEntity
import com.po4yka.runicquotes.data.local.entity.PackQuoteEntity
import com.po4yka.runicquotes.data.local.entity.QuoteEntity
import com.po4yka.runicquotes.data.local.entity.QuotePackEntity
import com.po4yka.runicquotes.data.local.entity.RuneReferenceEntity
import com.po4yka.runicquotes.data.local.entity.TranslationBackfillStateEntity
import com.po4yka.runicquotes.data.local.entity.TranslationRecordEntity

/**
 * Room database for Runic Quotes.
 */
@Database(
    entities = [
        QuoteEntity::class,
        QuotePackEntity::class,
        PackQuoteEntity::class,
        ArchivedQuoteEntity::class,
        RuneReferenceEntity::class,
        TranslationRecordEntity::class,
        TranslationBackfillStateEntity::class
    ],
    version = 5,
    exportSchema = true
)
internal abstract class RunicQuotesDatabase : RoomDatabase() {

    /**
     * Provides access to the QuoteDao.
     */
    abstract fun quoteDao(): QuoteDao

    /** Provides access to the QuotePackDao. */
    abstract fun quotePackDao(): QuotePackDao

    /** Provides access to the ArchivedQuoteDao. */
    abstract fun archivedQuoteDao(): ArchivedQuoteDao

    /** Provides access to the RuneReferenceDao. */
    abstract fun runeReferenceDao(): RuneReferenceDao

    /** Provides access to the TranslationRecordDao. */
    abstract fun translationRecordDao(): TranslationRecordDao

    /** Provides access to the TranslationBackfillStateDao. */
    abstract fun translationBackfillStateDao(): TranslationBackfillStateDao

    /** Database migrations and constants. */
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

        /**
         * Migration from version 3 to version 4.
         * Creates quote_packs, pack_quotes, archived_quotes, and rune_references tables.
         */
        @Suppress("MaxLineLength")
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `quote_packs` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `coverRune` TEXT NOT NULL,
                        `quoteCount` INTEGER NOT NULL,
                        `isInLibrary` INTEGER NOT NULL DEFAULT 0
                    )""".trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_quote_packs_isInLibrary` ON `quote_packs` (`isInLibrary`)"
                )

                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `pack_quotes` (
                        `packId` INTEGER NOT NULL,
                        `quoteId` INTEGER NOT NULL,
                        PRIMARY KEY(`packId`, `quoteId`),
                        FOREIGN KEY(`packId`) REFERENCES `quote_packs`(`id`) ON DELETE CASCADE,
                        FOREIGN KEY(`quoteId`) REFERENCES `quotes`(`id`) ON DELETE CASCADE
                    )""".trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_pack_quotes_quoteId` ON `pack_quotes` (`quoteId`)"
                )

                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `archived_quotes` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `originalQuoteId` INTEGER NOT NULL,
                        `textLatin` TEXT NOT NULL,
                        `author` TEXT NOT NULL,
                        `archivedAt` INTEGER NOT NULL,
                        `isDeleted` INTEGER NOT NULL DEFAULT 0
                    )""".trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_archived_quotes_isDeleted` ON `archived_quotes` (`isDeleted`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_archived_quotes_archivedAt` ON `archived_quotes` (`archivedAt`)"
                )

                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `rune_references` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `character` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `pronunciation` TEXT NOT NULL,
                        `meaning` TEXT NOT NULL,
                        `history` TEXT NOT NULL,
                        `script` TEXT NOT NULL
                    )""".trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_rune_references_script` ON `rune_references` (`script`)"
                )
            }
        }

        /**
         * Migration from version 4 to version 5.
         * Adds structured historical translation cache tables.
         */
        @Suppress("MaxLineLength")
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `translation_records` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `quoteId` INTEGER NOT NULL,
                        `script` TEXT NOT NULL,
                        `fidelity` TEXT NOT NULL,
                        `normalizedForm` TEXT NOT NULL,
                        `diplomaticForm` TEXT NOT NULL,
                        `glyphOutput` TEXT NOT NULL,
                        `historicalStage` TEXT NOT NULL,
                        `variant` TEXT,
                        `confidence` REAL NOT NULL,
                        `notesJson` TEXT NOT NULL,
                        `tokenBreakdownJson` TEXT NOT NULL,
                        `engineVersion` TEXT NOT NULL,
                        `isBackfilled` INTEGER NOT NULL DEFAULT 0,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        FOREIGN KEY(`quoteId`) REFERENCES `quotes`(`id`) ON DELETE CASCADE
                    )""".trimIndent()
                )
                db.execSQL(
                    """CREATE UNIQUE INDEX IF NOT EXISTS `index_translation_records_quoteId_script_fidelity_engineVersion`
                        ON `translation_records` (`quoteId`, `script`, `fidelity`, `engineVersion`)""".trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_translation_records_quoteId` ON `translation_records` (`quoteId`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_translation_records_script` ON `translation_records` (`script`)"
                )
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `translation_backfill_state` (
                        `id` INTEGER PRIMARY KEY NOT NULL,
                        `engineVersion` TEXT NOT NULL,
                        `lastProcessedQuoteId` INTEGER NOT NULL,
                        `processedCount` INTEGER NOT NULL,
                        `startedAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `completedAt` INTEGER
                    )""".trimIndent()
                )
            }
        }
    }
}
