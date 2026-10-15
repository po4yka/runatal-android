package com.po4yka.runatal.data.local.migration

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/** Creates dirty-source completion tracking without trusting the old global cursor. */
internal object TranslationBackfillCompletionMigration {
    fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """CREATE TABLE IF NOT EXISTS translation_backfill_completions (
                quoteId INTEGER NOT NULL PRIMARY KEY,
                sourceText TEXT NOT NULL,
                versionFingerprint TEXT NOT NULL,
                completedAt INTEGER NOT NULL,
                FOREIGN KEY(quoteId) REFERENCES quotes(id) ON DELETE CASCADE
            )""".trimIndent()
        )
    }
}
