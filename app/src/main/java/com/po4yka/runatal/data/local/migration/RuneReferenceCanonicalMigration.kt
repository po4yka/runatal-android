package com.po4yka.runatal.data.local.migration

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.po4yka.runatal.data.local.entity.RuneReferenceEntity
import com.po4yka.runatal.data.seed.RuneReferenceCanonicalIdentity
import com.po4yka.runatal.data.seed.RuneReferenceSeedData

/** Adopts complete proven canonical tuples, keeps their IDs, and moves bookmarks before deleting proven duplicates. */
internal object RuneReferenceCanonicalMigration {
    fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE rune_references ADD COLUMN canonicalKey TEXT")
        connection.execSQL("ALTER TABLE rune_references ADD COLUMN canonicalFingerprint TEXT")
        connection.execSQL("CREATE UNIQUE INDEX index_rune_references_canonicalKey ON rune_references(canonicalKey)")
        val known = RuneReferenceSeedData.getKnownLegacyReferences()
        RuneReferenceSeedData.getCanonicalReferences().forEach { canonical ->
            val rows = readCandidates(connection, canonical)
            val proven = RuneReferenceCanonicalIdentity.provenDuplicates(canonical, rows, known)
            val survivor = proven.firstOrNull()
            if (survivor == null) {
                insertCanonical(connection, canonical)
            } else {
                val duplicates = proven.drop(1)
                duplicates.sortedBy { bookmarkTime(connection, it.id) }.forEach { duplicate ->
                    mergeBookmarkAndDelete(connection, survivor.id, duplicate.id)
                }
                updateCanonical(connection, canonical.copy(id = survivor.id))
            }
        }
    }

    // Positional SQLite columns and parameters correspond exactly to the declared SQL projection.
    @Suppress("MagicNumber")
    private fun readCandidates(
        connection: SQLiteConnection, canonical: RuneReferenceEntity
    ): List<RuneReferenceEntity> =
        connection.prepare(
            "SELECT id, character, name, pronunciation, meaning, history, script FROM rune_references " +
                "WHERE script = ? AND name = ? ORDER BY id"
        ).use { statement ->
            statement.bindText(1, canonical.script)
            statement.bindText(2, canonical.name)
            val rows = mutableListOf<RuneReferenceEntity>()
            while (statement.step()) rows += RuneReferenceEntity(
                id = statement.getLong(0), character = statement.getText(1), name = statement.getText(2),
                pronunciation = statement.getText(3), meaning = statement.getText(4), history = statement.getText(5),
                script = statement.getText(6)
            )
            rows
        }

    private fun bookmarkTime(connection: SQLiteConnection, id: Long): Long =
        connection.prepare("SELECT createdAt FROM rune_bookmarks WHERE runeId = ?").use { statement ->
            statement.bindLong(1, id)
            if (statement.step()) statement.getLong(0) else Long.MAX_VALUE
        }

    private fun mergeBookmarkAndDelete(connection: SQLiteConnection, survivorId: Long, duplicateId: Long) {
        connection.prepare(
            "INSERT OR IGNORE INTO rune_bookmarks(runeId, createdAt) " +
                "SELECT ?, createdAt FROM rune_bookmarks WHERE runeId = ?"
        ).use { statement ->
            statement.bindLong(1, survivorId)
            statement.bindLong(2, duplicateId)
            statement.step()
        }
        // Room upgrades can run with foreign-key enforcement disabled; remove the old link explicitly.
        connection.prepare("DELETE FROM rune_bookmarks WHERE runeId = ?").use { statement ->
            statement.bindLong(1, duplicateId)
            statement.step()
        }
        connection.prepare("DELETE FROM rune_references WHERE id = ?").use { statement ->
            statement.bindLong(1, duplicateId)
            statement.step()
        }
    }

    private fun insertCanonical(connection: SQLiteConnection, reference: RuneReferenceEntity) {
        connection.prepare(
            "INSERT INTO rune_references(character, name, pronunciation, meaning, history, script, " +
                "canonicalKey, canonicalFingerprint) VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
        ).use { statement ->
            bindMetadata(statement, reference)
            statement.step()
        }
    }

    // Positional SQLite columns and parameters correspond exactly to the declared SQL projection.
    @Suppress("MagicNumber")
    private fun updateCanonical(connection: SQLiteConnection, reference: RuneReferenceEntity) {
        connection.prepare(
            "UPDATE rune_references SET character=?, name=?, pronunciation=?, meaning=?, history=?, script=?, " +
                "canonicalKey=?, canonicalFingerprint=? WHERE id=?"
        ).use { statement ->
            bindMetadata(statement, reference)
            statement.bindLong(9, reference.id)
            statement.step()
        }
    }

    // Positional SQLite columns and parameters correspond exactly to the declared SQL projection.
    @Suppress("MagicNumber")
    private fun bindMetadata(statement: androidx.sqlite.SQLiteStatement, reference: RuneReferenceEntity) {
        statement.bindText(1, reference.character)
        statement.bindText(2, reference.name)
        statement.bindText(3, reference.pronunciation)
        statement.bindText(4, reference.meaning)
        statement.bindText(5, reference.history)
        statement.bindText(6, reference.script)
        statement.bindText(7, checkNotNull(reference.canonicalKey))
        statement.bindText(8, checkNotNull(reference.canonicalFingerprint))
    }
}
