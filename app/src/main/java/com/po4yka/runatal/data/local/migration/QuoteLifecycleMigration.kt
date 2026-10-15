package com.po4yka.runatal.data.local.migration

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/** Retains authoritative quotes and safely imports legacy snapshots without replacing colliding quote IDs. */
internal object QuoteLifecycleMigration {
    fun migrate(connection: SQLiteConnection) {
        val originalMaximumId = connection.prepare("SELECT COALESCE(MAX(id), 0) FROM quotes").use { statement ->
            check(statement.step())
            statement.getLong(0)
        }
        val originalMaximumRecordId = connection.prepare(
            "SELECT COALESCE(MAX(id), 0) FROM translation_records"
        ).use { statement ->
            check(statement.step())
            statement.getLong(0)
        }
        connection.execSQL("ALTER TABLE quotes ADD COLUMN lifecycleState TEXT NOT NULL DEFAULT 'ACTIVE'")
        connection.execSQL("ALTER TABLE quotes ADD COLUMN lifecycleChangedAt INTEGER NOT NULL DEFAULT 0")
        connection.execSQL("ALTER TABLE quotes ADD COLUMN lifecycleMutationId TEXT")
        connection.execSQL("CREATE INDEX index_quotes_lifecycleState ON quotes(lifecycleState)")
        connection.execSQL(
            "CREATE TABLE canonical_quote_tombstones " +
                "(canonicalKey TEXT NOT NULL PRIMARY KEY, purgedAt INTEGER NOT NULL)"
        )
        connection.prepare(
            "SELECT originalQuoteId, textLatin, author, archivedAt, isDeleted " +
                "FROM archived_quotes ORDER BY archivedAt, id"
        ).use { snapshots ->
            while (snapshots.step()) {
                importSnapshot(connection, snapshots, originalMaximumId, originalMaximumRecordId)
            }
        }
        connection.execSQL("DROP TABLE archived_quotes")
    }

    private fun importSnapshot(
        connection: SQLiteConnection, snapshots: androidx.sqlite.SQLiteStatement,
        originalMaximumId: Long, originalMaximumRecordId: Long
    ) {
        val originalId = snapshots.getLong(0)
        val source = snapshots.getText(1)
        val author = snapshots.getText(2)
        val changedAt = snapshots.getLong(3)
        val state = if (snapshots.getLong(4) == 1L) "TRASH" else "ARCHIVED"
        val matchesOriginal = originalId <= originalMaximumId &&
            matchesAuthoritativeQuote(connection, originalId, source, author)
        if (matchesOriginal) {
            connection.prepare(
                "UPDATE quotes SET lifecycleState = ?, lifecycleChangedAt = ? WHERE id = ?"
            ).use { update ->
                update.bindText(1, state)
                update.bindLong(2, changedAt)
                update.bindLong(3, originalId)
                update.step()
            }
        } else {
            // Recover as a manual quote; glyphs, favorite and creation metadata were absent from this snapshot.
            connection.prepare(
                "INSERT INTO quotes(textLatin, author, isUserCreated, isFavorite, createdAt, " +
                    "lifecycleState, lifecycleChangedAt) VALUES (?, ?, 1, 0, 0, ?, ?)"
            ).use { insert ->
                insert.bindText(1, source)
                insert.bindText(2, author)
                insert.bindText(3, state)
                insert.bindLong(4, changedAt)
                insert.step()
            }
            val recoveredId = connection.prepare("SELECT last_insert_rowid()").use { statement ->
                check(statement.step())
                statement.getLong(0)
            }
            copyKnownHistoricalRecords(connection, originalId, recoveredId, source, originalMaximumRecordId)
        }
    }

    private fun copyKnownHistoricalRecords(
        connection: SQLiteConnection, originalId: Long, recoveredId: Long, source: String, maximumRecordId: Long
    ) {
        // Translation metadata depends on exact source text. Current colliding quote metadata is never inherited.
        connection.prepare(
            """INSERT INTO translation_records (
                quoteId, sourceText, script, fidelity, derivationKind, normalizedForm, diplomaticForm,
                glyphOutput, historicalStage, variant, resolutionStatus, confidence, notesJson, unresolvedTokensJson,
                provenanceJson, tokenBreakdownJson, engineVersion, datasetVersion,
                isBackfilled, createdAt, updatedAt
            ) SELECT ?, sourceText, script, fidelity, derivationKind, normalizedForm, diplomaticForm,
                glyphOutput, historicalStage, variant, resolutionStatus, confidence, notesJson, unresolvedTokensJson,
                provenanceJson, tokenBreakdownJson, engineVersion, datasetVersion,
                isBackfilled, createdAt, updatedAt
            FROM translation_records WHERE quoteId = ? AND sourceText = ? AND id <= ?""".trimIndent()
        ).use { statement ->
            statement.bindLong(1, recoveredId)
            statement.bindLong(2, originalId)
            statement.bindText(3, source)
            statement.bindLong(4, maximumRecordId)
            statement.step()
        }
    }

    private fun matchesAuthoritativeQuote(
        connection: SQLiteConnection, id: Long, source: String, author: String
    ): Boolean =
        connection.prepare("SELECT 1 FROM quotes WHERE id = ? AND textLatin = ? AND author = ?").use { statement ->
            statement.bindLong(1, id)
            statement.bindText(2, source)
            statement.bindText(3, author)
            statement.step()
        }
}
