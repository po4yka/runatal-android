package com.po4yka.runatal.data.local

import com.po4yka.runatal.data.local.migration.QuoteRenderingSelectionMigration
import com.po4yka.runatal.data.local.migration.QuoteLifecycleMigration
import com.po4yka.runatal.data.local.entity.RuneBookmarkEntity
import androidx.room3.Database
import com.po4yka.runatal.data.local.dao.ReadingHistoryDao
import com.po4yka.runatal.data.local.entity.QuoteReadEntity
import com.po4yka.runatal.data.local.entity.ReadingDayEntity
import com.po4yka.runatal.data.local.migration.ElderFutharkSequenceMigration
import com.po4yka.runatal.data.local.migration.CanonicalQuoteRenderingMigration
import com.po4yka.runatal.data.local.migration.CirthEncodingMigration
import com.po4yka.runatal.data.local.dao.TranslationBackfillCompletionDao
import com.po4yka.runatal.data.local.entity.TranslationBackfillCompletionEntity
import com.po4yka.runatal.data.local.migration.TranslationBackfillCompletionMigration
import androidx.room3.RoomDatabase
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.po4yka.runatal.data.local.dao.ArchivedQuoteDao
import com.po4yka.runatal.data.local.dao.QuoteDao
import com.po4yka.runatal.data.local.dao.QuotePackDao
import com.po4yka.runatal.data.local.dao.RuneReferenceDao
import com.po4yka.runatal.data.local.dao.TranslationBackfillStateDao
import com.po4yka.runatal.data.local.dao.TranslationRecordDao
import com.po4yka.runatal.data.local.entity.CanonicalQuoteTombstoneEntity
import com.po4yka.runatal.data.local.entity.PackQuoteEntity
import com.po4yka.runatal.data.local.entity.QuoteEntity
import com.po4yka.runatal.data.local.entity.QuotePackEntity
import com.po4yka.runatal.data.local.entity.RuneReferenceEntity
import com.po4yka.runatal.data.local.entity.TranslationBackfillStateEntity
import com.po4yka.runatal.data.local.entity.TranslationRecordEntity
import com.po4yka.runatal.data.seed.QuoteSeedData
import com.po4yka.runatal.data.local.migration.YoungerFutharkRenderingMigration
import com.po4yka.runatal.data.local.migration.TranslationCacheKeyMigration

/**
 * Room database for Runic Quotes.
 */
@Database(
    entities = [
        QuoteEntity::class,
        QuotePackEntity::class,
        PackQuoteEntity::class,
        CanonicalQuoteTombstoneEntity::class,
        RuneReferenceEntity::class,
        TranslationRecordEntity::class,
        TranslationBackfillStateEntity::class,
        QuoteReadEntity::class,
        ReadingDayEntity::class,
        TranslationBackfillCompletionEntity::class,
        RuneBookmarkEntity::class
    ],
    version = 18,
    exportSchema = true
)
internal abstract class RunatalDatabase : RoomDatabase() {
    /** Discovers and commits completion for exact quote sources. */
    abstract fun translationBackfillCompletionDao(): TranslationBackfillCompletionDao

    /** Persists viewed quotes and reading activity. */
    abstract fun readingHistoryDao(): ReadingHistoryDao

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
            override suspend fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    "ALTER TABLE quotes ADD COLUMN isUserCreated INTEGER NOT NULL DEFAULT 0"
                )
                connection.execSQL(
                    "ALTER TABLE quotes ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0"
                )
                connection.execSQL(
                    "ALTER TABLE quotes ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        /**
         * Migration from version 2 to version 3.
         * Adds indices on isUserCreated and isFavorite for query performance.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override suspend fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_quotes_isUserCreated ON quotes (isUserCreated)"
                )
                connection.execSQL(
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
            override suspend fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    """CREATE TABLE IF NOT EXISTS `quote_packs` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `coverRune` TEXT NOT NULL,
                        `quoteCount` INTEGER NOT NULL,
                        `isInLibrary` INTEGER NOT NULL DEFAULT 0
                    )""".trimIndent()
                )
                connection.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_quote_packs_isInLibrary` ON `quote_packs` (`isInLibrary`)"
                )

                connection.execSQL(
                    """CREATE TABLE IF NOT EXISTS `pack_quotes` (
                        `packId` INTEGER NOT NULL,
                        `quoteId` INTEGER NOT NULL,
                        PRIMARY KEY(`packId`, `quoteId`),
                        FOREIGN KEY(`packId`) REFERENCES `quote_packs`(`id`) ON DELETE CASCADE,
                        FOREIGN KEY(`quoteId`) REFERENCES `quotes`(`id`) ON DELETE CASCADE
                    )""".trimIndent()
                )
                connection.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_pack_quotes_quoteId` ON `pack_quotes` (`quoteId`)"
                )

                connection.execSQL(
                    """CREATE TABLE IF NOT EXISTS `archived_quotes` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `originalQuoteId` INTEGER NOT NULL,
                        `textLatin` TEXT NOT NULL,
                        `author` TEXT NOT NULL,
                        `archivedAt` INTEGER NOT NULL,
                        `isDeleted` INTEGER NOT NULL DEFAULT 0
                    )""".trimIndent()
                )
                connection.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_archived_quotes_isDeleted` ON `archived_quotes` (`isDeleted`)"
                )
                connection.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_archived_quotes_archivedAt` ON `archived_quotes` (`archivedAt`)"
                )

                connection.execSQL(
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
                connection.execSQL(
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
            override suspend fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
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
                connection.execSQL(
                    """CREATE UNIQUE INDEX IF NOT EXISTS `index_translation_records_quoteId_script_fidelity_engineVersion`
                        ON `translation_records` (`quoteId`, `script`, `fidelity`, `engineVersion`)""".trimIndent()
                )
                connection.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_translation_records_quoteId` ON `translation_records` (`quoteId`)"
                )
                connection.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_translation_records_script` ON `translation_records` (`script`)"
                )
                connection.execSQL(
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

        /**
         * Migration from version 5 to version 6.
         * Adds deterministic cache keys, source text, resolution metadata, and provenance fields.
         */
        @Suppress("MaxLineLength")
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override suspend fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    """CREATE TABLE IF NOT EXISTS `translation_records_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `quoteId` INTEGER NOT NULL,
                        `sourceText` TEXT NOT NULL,
                        `script` TEXT NOT NULL,
                        `fidelity` TEXT NOT NULL,
                        `normalizedForm` TEXT NOT NULL,
                        `diplomaticForm` TEXT NOT NULL,
                        `glyphOutput` TEXT NOT NULL,
                        `historicalStage` TEXT NOT NULL,
                        `variant` TEXT,
                        `resolutionStatus` TEXT NOT NULL,
                        `confidence` REAL NOT NULL,
                        `notesJson` TEXT NOT NULL,
                        `unresolvedTokensJson` TEXT NOT NULL,
                        `provenanceJson` TEXT NOT NULL,
                        `tokenBreakdownJson` TEXT NOT NULL,
                        `engineVersion` TEXT NOT NULL,
                        `datasetVersion` TEXT NOT NULL,
                        `isBackfilled` INTEGER NOT NULL DEFAULT 0,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        FOREIGN KEY(`quoteId`) REFERENCES `quotes`(`id`) ON DELETE CASCADE
                    )""".trimIndent()
                )
                connection.execSQL(
                    """INSERT INTO `translation_records_new` (
                        `id`, `quoteId`, `sourceText`, `script`, `fidelity`, `normalizedForm`, `diplomaticForm`,
                        `glyphOutput`, `historicalStage`, `variant`, `resolutionStatus`, `confidence`, `notesJson`,
                        `unresolvedTokensJson`, `provenanceJson`, `tokenBreakdownJson`, `engineVersion`,
                        `datasetVersion`, `isBackfilled`, `createdAt`, `updatedAt`
                    )
                    SELECT
                        tr.`id`,
                        tr.`quoteId`,
                        COALESCE((SELECT q.`textLatin` FROM `quotes` q WHERE q.`id` = tr.`quoteId`), ''),
                        tr.`script`,
                        tr.`fidelity`,
                        tr.`normalizedForm`,
                        tr.`diplomaticForm`,
                        tr.`glyphOutput`,
                        tr.`historicalStage`,
                        tr.`variant`,
                        CASE
                            WHEN tr.`glyphOutput` = '' THEN 'UNAVAILABLE'
                            WHEN tr.`script` = 'CIRTH' THEN 'APPROXIMATED'
                            ELSE 'RECONSTRUCTED'
                        END,
                        tr.`confidence`,
                        tr.`notesJson`,
                        '[]',
                        '[]',
                        tr.`tokenBreakdownJson`,
                        tr.`engineVersion`,
                        'legacy-v5',
                        tr.`isBackfilled`,
                        tr.`createdAt`,
                        tr.`updatedAt`
                    FROM `translation_records` tr""".trimIndent()
                )
                connection.execSQL("DROP TABLE `translation_records`")
                connection.execSQL("ALTER TABLE `translation_records_new` RENAME TO `translation_records`")
                connection.execSQL(
                    """CREATE UNIQUE INDEX IF NOT EXISTS `index_translation_records_quoteId_script_fidelity_variant_engineVersion_datasetVersion`
                        ON `translation_records` (`quoteId`, `script`, `fidelity`, `variant`, `engineVersion`, `datasetVersion`)""".trimIndent()
                )
                connection.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_translation_records_quoteId` ON `translation_records` (`quoteId`)"
                )
                connection.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_translation_records_script` ON `translation_records` (`script`)"
                )
            }
        }

        /**
         * Migration from version 6 to version 7.
         * Persists derivation metadata for structured translation results.
         */
        @Suppress("MaxLineLength")
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override suspend fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    """CREATE TABLE IF NOT EXISTS `translation_records_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `quoteId` INTEGER NOT NULL,
                        `sourceText` TEXT NOT NULL,
                        `script` TEXT NOT NULL,
                        `fidelity` TEXT NOT NULL,
                        `derivationKind` TEXT NOT NULL,
                        `normalizedForm` TEXT NOT NULL,
                        `diplomaticForm` TEXT NOT NULL,
                        `glyphOutput` TEXT NOT NULL,
                        `historicalStage` TEXT NOT NULL,
                        `variant` TEXT,
                        `resolutionStatus` TEXT NOT NULL,
                        `confidence` REAL NOT NULL,
                        `notesJson` TEXT NOT NULL,
                        `unresolvedTokensJson` TEXT NOT NULL,
                        `provenanceJson` TEXT NOT NULL,
                        `tokenBreakdownJson` TEXT NOT NULL,
                        `engineVersion` TEXT NOT NULL,
                        `datasetVersion` TEXT NOT NULL,
                        `isBackfilled` INTEGER NOT NULL DEFAULT 0,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        FOREIGN KEY(`quoteId`) REFERENCES `quotes`(`id`) ON DELETE CASCADE
                    )""".trimIndent()
                )
                connection.execSQL(
                    """INSERT INTO `translation_records_new` (
                        `id`, `quoteId`, `sourceText`, `script`, `fidelity`, `derivationKind`,
                        `normalizedForm`, `diplomaticForm`, `glyphOutput`, `historicalStage`, `variant`,
                        `resolutionStatus`, `confidence`, `notesJson`, `unresolvedTokensJson`, `provenanceJson`,
                        `tokenBreakdownJson`, `engineVersion`, `datasetVersion`, `isBackfilled`, `createdAt`, `updatedAt`
                    )
                    SELECT
                        `id`,
                        `quoteId`,
                        `sourceText`,
                        `script`,
                        `fidelity`,
                        CASE
                            WHEN `engineVersion` LIKE 'gold-example%' THEN 'GOLD_EXAMPLE'
                            WHEN `script` = 'CIRTH' THEN 'SEQUENCE_TRANSCRIPTION'
                            ELSE 'TOKEN_COMPOSED'
                        END,
                        `normalizedForm`,
                        `diplomaticForm`,
                        `glyphOutput`,
                        `historicalStage`,
                        `variant`,
                        `resolutionStatus`,
                        `confidence`,
                        `notesJson`,
                        `unresolvedTokensJson`,
                        `provenanceJson`,
                        `tokenBreakdownJson`,
                        `engineVersion`,
                        `datasetVersion`,
                        `isBackfilled`,
                        `createdAt`,
                        `updatedAt`
                    FROM `translation_records`""".trimIndent()
                )
                connection.execSQL(
                    "DROP TABLE `translation_records`"
                )
                connection.execSQL(
                    "ALTER TABLE `translation_records_new` RENAME TO `translation_records`"
                )
                connection.execSQL(
                    """CREATE UNIQUE INDEX IF NOT EXISTS `index_translation_records_quoteId_script_fidelity_variant_engineVersion_datasetVersion`
                        ON `translation_records` (`quoteId`, `script`, `fidelity`, `variant`, `engineVersion`, `datasetVersion`)""".trimIndent()
                )
                connection.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_translation_records_quoteId` ON `translation_records` (`quoteId`)"
                )
                connection.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_translation_records_script` ON `translation_records` (`script`)"
                )
            }
        }

        /** Adds stable canonical identities while preserving legacy IDs and user-owned state. */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override suspend fun migrate(connection: SQLiteConnection) {
                connection.execSQL("ALTER TABLE quotes ADD COLUMN canonicalKey TEXT")
                connection.execSQL(
                    "CREATE UNIQUE INDEX index_quotes_canonicalKey ON quotes (canonicalKey)"
                )
                QuoteSeedData.getLegacyQuotes().forEach { quote ->
                    connection.prepare(
                        "UPDATE quotes SET canonicalKey = ? WHERE id = ? " +
                            "AND textLatin = ? AND author = ? AND isUserCreated = 0"
                    ).use { statement ->
                        statement.bindText(1, requireNotNull(quote.canonicalKey))
                        statement.bindLong(2, quote.id)
                        statement.bindText(3, quote.textLatin)
                        statement.bindText(4, quote.author)
                        statement.step()
                    }
                }
            }
        }

        /** Repairs only known generated Younger Futhark strings and canonical reference glyphs. */
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override suspend fun migrate(connection: SQLiteConnection) {
                YoungerFutharkRenderingMigration.migrate(connection)
            }
        }

        /** Makes every translation-selection key unique, including scripts without variants. */
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override suspend fun migrate(connection: SQLiteConnection) {
                TranslationCacheKeyMigration.migrate(connection)
            }
        }

        /** Adds real reading activity without changing existing quote data. */
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override suspend fun migrate(connection: SQLiteConnection) {
                connection.execSQL("CREATE TABLE IF NOT EXISTS reading_days (epochDay INTEGER NOT NULL PRIMARY KEY)")
                connection.execSQL(
                    "CREATE TABLE IF NOT EXISTS quote_reads (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, quoteId INTEGER NOT NULL, " +
                        "epochDay INTEGER NOT NULL, script TEXT NOT NULL, readAt INTEGER NOT NULL, " +
                        "FOREIGN KEY(quoteId) REFERENCES quotes(id) ON DELETE CASCADE)"
                )
                connection.execSQL("CREATE INDEX index_quote_reads_quoteId ON quote_reads(quoteId)")
                connection.execSQL(
                    "CREATE UNIQUE INDEX index_quote_reads_quoteId_epochDay_script " +
                        "ON quote_reads(quoteId, epochDay, script)"
                )
            }
        }

        /** Repairs stored direct Elder x/q sequences while preserving quote identities and metadata. */
        val MIGRATION_11_12 = object : Migration(11, 12) {
            override suspend fun migrate(connection: SQLiteConnection) {
                ElderFutharkSequenceMigration.migrate(connection)
            }
        }

        /** Refreshes only proven legacy renderings of canonical quotes. */
        val MIGRATION_12_13 = object : Migration(12, 13) {
            override suspend fun migrate(connection: SQLiteConnection) {
                CanonicalQuoteRenderingMigration.migrate(connection)
            }
        }

        /** Re-encodes proven legacy Cirth output and reference rows without reinterpreting manual content. */
        val MIGRATION_13_14 = object : Migration(13, 14) {
            override suspend fun migrate(connection: SQLiteConnection) {
                CirthEncodingMigration.migrate(connection)
            }
        }

        /** Adds per-source completion without dropping the historic global checkpoint. */
        val MIGRATION_14_15 = object : Migration(14, 15) {
            override suspend fun migrate(connection: SQLiteConnection) {
                TranslationBackfillCompletionMigration.migrate(connection)
            }
        }
        /** Persists rendering intent independently of automatic translation caches. */
        val MIGRATION_15_16 = object : Migration(15, 16) {
            override suspend fun migrate(connection: SQLiteConnection) {
                QuoteRenderingSelectionMigration.migrate(connection)
            }
        }

        /** Retains archive, hidden and trash rows without reinserting snapshots. */
        val MIGRATION_16_17 = object : Migration(16, 17) {
            override suspend fun migrate(connection: SQLiteConnection) {
                QuoteLifecycleMigration.migrate(connection)
            }
        }

        /** Adds bookmarks without copying or replacing rune reference rows. */
        val MIGRATION_17_18 = object : Migration(17, 18) {
            override suspend fun migrate(connection: SQLiteConnection) {
                connection.execSQL("CREATE TABLE IF NOT EXISTS rune_bookmarks (" +
                    "runeId INTEGER NOT NULL PRIMARY KEY, createdAt INTEGER NOT NULL, " +
                    "FOREIGN KEY(runeId) REFERENCES rune_references(id) ON DELETE CASCADE)")
            }
        }

    }
}
