package com.po4yka.runatal.data.local

import android.app.Application
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import com.google.common.truth.Truth.assertThat
import com.po4yka.runatal.data.local.entity.QuoteEntity
import com.po4yka.runatal.data.local.migration.CanonicalQuoteRenderingMigration
import com.po4yka.runatal.data.seed.QuoteSeedData
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.SQLiteMode

/** Verifies field-level seed repair against native SQLite without assuming a future Room version number. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class CanonicalQuoteRenderingMigrationTest {

    @Test
    fun `repairs only exact legacy canonical fields and retains identities metadata and foreign key rows`() {
        AndroidSQLiteDriver().open(":memory:").use { connection ->
            createTables(connection)
            val old = QuoteSeedData.getLegacyQuotes()
            val fresh = QuoteSeedData.getCanonicalQuotes()
            val rows = listOf(
                old[0].copy(id = 101L, runicElder = "manual elder", isFavorite = true, createdAt = 42L),
                old[1].copy(id = 102L, runicCirth = null, isFavorite = true, createdAt = 43L),
                old[2].copy(id = 103L, isFavorite = true, createdAt = 44L),
                old[3].copy(id = 104L, textLatin = "Changed canonical text", createdAt = 45L),
                fresh[4].copy(id = 105L, createdAt = 46L),
                old[0].copy(id = 601L, canonicalKey = null, isUserCreated = true, isFavorite = true, createdAt = 47L)
            )
            rows.forEach { insertQuote(connection, it) }
            connection.execSQL("INSERT INTO linked_translation VALUES (900, 103, 'historical snapshot')")

            repeat(2) { CanonicalQuoteRenderingMigration.migrate(connection) }

            val expected = listOf(
                rows[0].copy(runicYounger = fresh[0].runicYounger, runicCirth = fresh[0].runicCirth),
                rows[1].copy(runicElder = fresh[1].runicElder, runicYounger = fresh[1].runicYounger),
                rows[2].copy(
                    runicElder = fresh[2].runicElder,
                    runicYounger = fresh[2].runicYounger,
                    runicCirth = fresh[2].runicCirth
                ),
                rows[3], rows[4], rows[5]
            )
            assertThat(readQuotes(connection)).containsExactlyElementsIn(expected).inOrder()
            connection.prepare("SELECT id, quoteId, glyphOutput FROM linked_translation").use { statement ->
                assertThat(statement.step()).isTrue()
                assertThat(statement.getLong(0)).isEqualTo(900L)
                assertThat(statement.getLong(1)).isEqualTo(103L)
                assertThat(statement.getText(2)).isEqualTo("historical snapshot")
            }
        }
    }

    @Test
    fun `null manual values and user owned canonical identities are never overwritten`() {
        AndroidSQLiteDriver().open(":memory:").use { connection ->
            createTables(connection)
            val row = QuoteSeedData.getLegacyQuotes().first().copy(
                id = 1L, runicElder = null, runicYounger = "custom", isUserCreated = true, createdAt = 99L
            )
            insertQuote(connection, row)

            CanonicalQuoteRenderingMigration.migrate(connection)

            assertThat(readQuotes(connection)).containsExactly(row)
        }
    }

    private fun createTables(connection: SQLiteConnection) {
        connection.execSQL("PRAGMA foreign_keys = ON")
        connection.execSQL(
            "CREATE TABLE quotes (id INTEGER PRIMARY KEY, textLatin TEXT NOT NULL, author TEXT NOT NULL, " +
                "runicElder TEXT, runicYounger TEXT, runicCirth TEXT, isUserCreated INTEGER NOT NULL, " +
                "isFavorite INTEGER NOT NULL, createdAt INTEGER NOT NULL, canonicalKey TEXT UNIQUE)"
        )
        connection.execSQL(
            "CREATE TABLE linked_translation (id INTEGER PRIMARY KEY, quoteId INTEGER NOT NULL " +
                "REFERENCES quotes(id) ON DELETE CASCADE, glyphOutput TEXT NOT NULL)"
        )
    }

    private fun insertQuote(connection: SQLiteConnection, quote: QuoteEntity) {
        connection.prepare("INSERT INTO quotes VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)").use { statement ->
            statement.bindLong(1, quote.id)
            statement.bindText(2, quote.textLatin)
            statement.bindText(3, quote.author)
            listOf(quote.runicElder, quote.runicYounger, quote.runicCirth).forEachIndexed { index, value ->
                if (value == null) statement.bindNull(4 + index) else statement.bindText(4 + index, value)
            }
            statement.bindLong(7, if (quote.isUserCreated) 1L else 0L)
            statement.bindLong(8, if (quote.isFavorite) 1L else 0L)
            statement.bindLong(9, quote.createdAt)
            val key = quote.canonicalKey
            if (key == null) statement.bindNull(10) else statement.bindText(10, key)
            statement.step()
        }
    }

    private fun readQuotes(connection: SQLiteConnection): List<QuoteEntity> {
        return connection.prepare("SELECT * FROM quotes ORDER BY id").use { statement ->
            buildList {
                while (statement.step()) {
                    add(
                        QuoteEntity(
                            id = statement.getLong(0), textLatin = statement.getText(1), author = statement.getText(2),
                            runicElder = if (statement.isNull(3)) null else statement.getText(3),
                            runicYounger = if (statement.isNull(4)) null else statement.getText(4),
                            runicCirth = if (statement.isNull(5)) null else statement.getText(5),
                            isUserCreated = statement.getLong(6) != 0L, isFavorite = statement.getLong(7) != 0L,
                            createdAt = statement.getLong(8),
                            canonicalKey = if (statement.isNull(9)) null else statement.getText(9)
                        )
                    )
                }
            }
        }
    }
}
