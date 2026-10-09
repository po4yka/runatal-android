package com.po4yka.runatal.data.local.migration

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/** Normalizes absent variants and retains the newest complete record for each cache selection. */
internal object TranslationCacheKeyMigration {

    fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """CREATE TABLE `translation_records_new` (
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
                `variant` TEXT NOT NULL,
                `resolutionStatus` TEXT NOT NULL,
                `confidence` REAL NOT NULL,
                `notesJson` TEXT NOT NULL,
                `unresolvedTokensJson` TEXT NOT NULL,
                `provenanceJson` TEXT NOT NULL,
                `tokenBreakdownJson` TEXT NOT NULL,
                `engineVersion` TEXT NOT NULL,
                `datasetVersion` TEXT NOT NULL,
                `isBackfilled` INTEGER NOT NULL,
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
                current.`id`, current.`quoteId`, current.`sourceText`, current.`script`, current.`fidelity`,
                current.`derivationKind`, current.`normalizedForm`, current.`diplomaticForm`, current.`glyphOutput`,
                current.`historicalStage`, COALESCE(current.`variant`, ''), current.`resolutionStatus`,
                current.`confidence`, current.`notesJson`, current.`unresolvedTokensJson`, current.`provenanceJson`,
                current.`tokenBreakdownJson`, current.`engineVersion`, current.`datasetVersion`,
                current.`isBackfilled`, current.`createdAt`, current.`updatedAt`
            FROM `translation_records` current
            WHERE NOT EXISTS (
                SELECT 1 FROM `translation_records` newer
                WHERE newer.`quoteId` = current.`quoteId`
                    AND newer.`script` = current.`script`
                    AND newer.`fidelity` = current.`fidelity`
                    AND COALESCE(newer.`variant`, '') = COALESCE(current.`variant`, '')
                    AND newer.`engineVersion` = current.`engineVersion`
                    AND newer.`datasetVersion` = current.`datasetVersion`
                    AND (newer.`updatedAt` > current.`updatedAt`
                        OR (newer.`updatedAt` = current.`updatedAt` AND newer.`id` > current.`id`))
            )""".trimIndent()
        )
        connection.execSQL("DROP TABLE `translation_records`")
        connection.execSQL("ALTER TABLE `translation_records_new` RENAME TO `translation_records`")
        val uniqueIndex = "index_translation_records_quoteId_script_fidelity_variant_engineVersion_datasetVersion"
        connection.execSQL(
            "CREATE UNIQUE INDEX `$uniqueIndex` ON translation_records " +
                "(quoteId, script, fidelity, variant, engineVersion, datasetVersion)"
        )
        connection.execSQL("CREATE INDEX index_translation_records_quoteId ON translation_records (quoteId)")
        connection.execSQL("CREATE INDEX index_translation_records_script ON translation_records (script)")
    }
}
