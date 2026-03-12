package com.po4yka.runicquotes.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@Suppress("DEPRECATION")
class RunicQuotesDatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        RunicQuotesDatabase::class.java.name,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrate5To6_preservesTranslationRowsAndAddsMetadata() {
        helper.createDatabase(TEST_DB, 5).apply {
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
            TEST_DB,
            6,
            true,
            RunicQuotesDatabase.MIGRATION_5_6
        ).apply {
            query(
                """
                SELECT sourceText, resolutionStatus, datasetVersion
                FROM translation_records
                WHERE id = 1
                """.trimIndent()
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("The wolf hunts at night", cursor.getString(0))
                assertEquals("RECONSTRUCTED", cursor.getString(1))
                assertEquals("legacy-v5", cursor.getString(2))
            }
            close()
        }
    }

    private companion object {
        const val TEST_DB = "runic-quotes-migration-test"
    }
}
