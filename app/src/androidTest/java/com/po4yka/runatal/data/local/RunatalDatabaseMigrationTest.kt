package com.po4yka.runatal.data.local

import androidx.room3.Room
import androidx.room3.testing.MigrationTestHelper
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import com.po4yka.runatal.data.seed.QuoteSeedData
import kotlinx.coroutines.test.runTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RunatalDatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        InstrumentationRegistry.getInstrumentation().targetContext.getDatabasePath(TEST_DB),
        AndroidSQLiteDriver(),
        RunatalDatabase::class
    )

    @Before
    fun setUp() {
        InstrumentationRegistry.getInstrumentation().targetContext.deleteDatabase(TEST_DB)
    }

    @Test
    fun migrate7To8_preservesUserQuotesAndCanonicalIdentity() = runTest {
        val legacyQuotes = QuoteSeedData.getLegacyQuotes()
        helper.createDatabase(7).use { connection ->
            legacyQuotes.take(2).forEachIndexed { index, quote ->
                connection.prepare(
                    "INSERT INTO quotes (id, textLatin, author, isUserCreated, isFavorite, createdAt) " +
                        "VALUES (?, ?, ?, ?, 1, 42)"
                ).use { statement ->
                    statement.bindLong(1, quote.id)
                    statement.bindText(2, quote.textLatin)
                    statement.bindText(3, quote.author)
                    statement.bindLong(4, if (index == 0) 1L else 0L)
                    statement.step()
                }
            }
        }

        helper.runMigrationsAndValidate(8, listOf(RunatalDatabase.MIGRATION_7_8)).use { connection ->
            assertNoUnexpectedTables(connection)
            connection.prepare("SELECT canonicalKey, isFavorite FROM quotes ORDER BY id").use { statement ->
                assertTrue(statement.step())
                assertTrue(statement.isNull(0))
                assertEquals(1L, statement.getLong(1))
                assertTrue(statement.step())
                assertEquals(legacyQuotes[1].canonicalKey, statement.getText(0))
                assertEquals(1L, statement.getLong(1))
            }
        }
    }

    @Test
    fun migrate5To6_preservesTranslationRowsAndAddsMetadata() = runTest {
        helper.createDatabase(5).apply {
            execSQL(
                """
                INSERT INTO quotes (
                    id, textLatin, author, runicElder, runicYounger, runicCirth,
                    isUserCreated, isFavorite, createdAt
                ) VALUES (
                    1, 'The wolf hunts at night', 'Runatal', '', '', '',
                    1, 0, 1234
                )
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO translation_records (
                    id, quoteId, script, fidelity, normalizedForm, diplomaticForm,
                    glyphOutput, historicalStage, variant, confidence, notesJson,
                    tokenBreakdownJson, engineVersion, isBackfilled, createdAt, updatedAt
                ) VALUES (
                    1, 1, 'YOUNGER_FUTHARK', 'STRICT', 'úlfr veiðir um nótt', 'ulfr uiþir um nutt',
                    'ᚢᛚᚠᚱ ᚢᛁᚦᛁᚱ ᚢᛘ ᚾᚢᛏᛏ', 'OLD_NORSE', 'LONG_BRANCH', 0.97, '[]',
                    '[]', 'yf-translation-v1', 1, 1234, 1234
                )
                """.trimIndent()
            )
            close()
        }

        helper.runMigrationsAndValidate(
            6,
            listOf(RunatalDatabase.MIGRATION_5_6)
        ).apply {
            assertNoUnexpectedTables(this)
            prepare(
                """
                SELECT sourceText, resolutionStatus, datasetVersion
                FROM translation_records
                WHERE id = 1
                """.trimIndent()
            ).use { statement ->
                assertTrue(statement.step())
                assertEquals("The wolf hunts at night", statement.getText(0))
                assertEquals("RECONSTRUCTED", statement.getText(1))
                assertEquals("legacy-v5", statement.getText(2))
            }
            close()
        }
    }

    @Test
    fun migrate6To7_preservesTranslationRowsAndAddsDerivationMetadata() = runTest {
        helper.createDatabase(6).apply {
            execSQL(
                """
                INSERT INTO quotes (
                    id, textLatin, author, runicElder, runicYounger, runicCirth,
                    isUserCreated, isFavorite, createdAt
                ) VALUES (
                    1, 'The wolf hunts at night', 'Runatal', '', '', '',
                    1, 0, 1234
                )
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO translation_records (
                    id, quoteId, sourceText, script, fidelity, normalizedForm, diplomaticForm,
                    glyphOutput, historicalStage, variant, resolutionStatus, confidence, notesJson,
                    unresolvedTokensJson, provenanceJson, tokenBreakdownJson, engineVersion,
                    datasetVersion, isBackfilled, createdAt, updatedAt
                ) VALUES (
                    1, 1, 'The wolf hunts at night', 'CIRTH', 'STRICT', 'the wolf hunts at night',
                    'th·e w·o·l·f h·u·n·t·s a·t n·i·gh·t', 'ᚦ', 'EREBOR_ENGLISH', NULL,
                    'APPROXIMATED', 0.78, '[]', '[]', '[]', '[]', 'cirth-translation-v2',
                    '2026.03-curated-v1', 1, 1234, 1234
                )
                """.trimIndent()
            )
            close()
        }

        helper.runMigrationsAndValidate(
            7,
            listOf(RunatalDatabase.MIGRATION_6_7)
        ).apply {
            assertNoUnexpectedTables(this)
            prepare(
                """
                SELECT derivationKind, datasetVersion
                FROM translation_records
                WHERE id = 1
                """.trimIndent()
            ).use { statement ->
                assertTrue(statement.step())
                assertEquals("SEQUENCE_TRANSCRIPTION", statement.getText(0))
                assertEquals("2026.03-curated-v1", statement.getText(1))
            }
            close()
        }
    }

    @Test
    fun migrate3To7_preservesQuotesAcrossAllAvailableLegacySchemas() = runTest {
        helper.createDatabase(3).use { connection ->
            connection.execSQL(
                """
                INSERT INTO quotes (
                    id, textLatin, author, runicElder, runicYounger, runicCirth,
                    isUserCreated, isFavorite, createdAt
                ) VALUES (1, 'Legacy quote', 'Runatal', '', '', '', 1, 1, 1234)
                """.trimIndent()
            )
        }

        helper.runMigrationsAndValidate(
            7,
            listOf(
                RunatalDatabase.MIGRATION_3_4,
                RunatalDatabase.MIGRATION_4_5,
                RunatalDatabase.MIGRATION_5_6,
                RunatalDatabase.MIGRATION_6_7
            )
        ).use { connection ->
            assertNoUnexpectedTables(connection)
            connection.prepare("SELECT textLatin, isFavorite FROM quotes WHERE id = 1").use { statement ->
                assertTrue(statement.step())
                assertEquals("Legacy quote", statement.getText(0))
                assertEquals(1L, statement.getLong(1))
            }
        }
    }

    @Test
    fun openRoom2Version7DatabaseWithRoom3_preservesExistingQuotes() = runTest {
        helper.createDatabase(7).use { connection ->
            connection.prepare("SELECT identity_hash FROM room_master_table WHERE id = 42").use { statement ->
                assertTrue(statement.step())
                assertEquals(ROOM_2_VERSION_7_IDENTITY_HASH, statement.getText(0))
            }
            connection.execSQL(
                """
                INSERT INTO quotes (
                    id, textLatin, author, runicElder, runicYounger, runicCirth,
                    isUserCreated, isFavorite, createdAt
                ) VALUES (1, 'Existing Room 2 quote', 'Runatal', '', '', '', 1, 1, 1234)
                """.trimIndent()
            )
        }

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.databaseBuilder(context, RunatalDatabase::class.java, TEST_DB)
            .setDriver(AndroidSQLiteDriver())
            .addMigrations(RunatalDatabase.MIGRATION_7_8, RunatalDatabase.MIGRATION_8_9)
            .build()
        try {
            val quote = requireNotNull(database.quoteDao().getById(1))
            assertEquals("Existing Room 2 quote", quote.textLatin)
            assertTrue(quote.isFavorite)
            assertEquals(1234L, quote.createdAt)
        } finally {
            database.close()
        }
    }

    private fun assertNoUnexpectedTables(connection: SQLiteConnection) {
        val tables = buildSet {
            connection.prepare(
                "SELECT name FROM sqlite_master WHERE type = 'table' " +
                    "AND name NOT LIKE 'sqlite_%' AND name != 'android_metadata'"
            ).use { statement ->
                while (statement.step()) {
                    add(statement.getText(0))
                }
            }
        }
        assertEquals(
            setOf(
                "room_master_table", "quotes", "quote_packs", "pack_quotes", "archived_quotes",
                "rune_references", "translation_records", "translation_backfill_state"
            ),
            tables
        )
    }

    private companion object {
        const val ROOM_2_VERSION_7_IDENTITY_HASH = "4941e15bb26effbd69673c51cf6a6ecd"
        const val TEST_DB = "runic-quotes-migration-test"
    }
}
