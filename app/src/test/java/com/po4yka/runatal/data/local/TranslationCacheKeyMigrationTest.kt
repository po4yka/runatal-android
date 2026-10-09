package com.po4yka.runatal.data.local

import android.app.Application
import androidx.room3.Room
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import com.google.common.truth.Truth.assertThat
import com.po4yka.runatal.data.local.entity.TranslationRecordEntity
import java.io.File
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.SQLiteMode

/** Validates deduplication and row preservation against the exported v9 schema and generated Room v10. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class TranslationCacheKeyMigrationTest {

    private val context = RuntimeEnvironment.getApplication()
    private val databaseName = "translation-cache-key-migration.db"
    private var database: RunatalDatabase? = null

    @After
    fun tearDown() {
        database?.close()
        context.deleteDatabase(databaseName)
    }

    @Test
    fun `migration retains complete newest records and separate selections without touching quote state`() = runTest {
        val legacy = legacyRecords()
        createVersion9().use { connection ->
            connection.execSQL(
                "INSERT INTO quotes (id, textLatin, author, isUserCreated, isFavorite, createdAt) " +
                    "VALUES (1, 'wolf', 'User', 1, 1, 42), (2, 'wolf', 'Another user', 1, 0, 43)"
            )
            connection.execSQL(
                "INSERT INTO translation_backfill_state " +
                    "VALUES (1, 'saved-checkpoint', 2, 2, 100, 200, 300)"
            )
            legacy.forEach { insertLegacyRecord(connection, it) }
            assertThat(rowCount(connection)).isEqualTo(legacy.size)
        }

        database = Room.databaseBuilder(context, RunatalDatabase::class.java, databaseName)
            .setDriver(AndroidSQLiteDriver())
            .addMigrations(RunatalDatabase.MIGRATION_9_10,
                RunatalDatabase.MIGRATION_10_11,
                RunatalDatabase.MIGRATION_11_12,
                RunatalDatabase.MIGRATION_12_13,
                RunatalDatabase.MIGRATION_13_14,
                RunatalDatabase.MIGRATION_14_15, RunatalDatabase.MIGRATION_15_16,
                RunatalDatabase.MIGRATION_16_17, RunatalDatabase.MIGRATION_17_18)
            .build()
        val migrated = requireNotNull(database)
        val expected = legacy.filter { it.record.id !in setOf(1L, 2L, 4L, 8L) }.map { it.record }
        expected.forEach { record ->
            val selected = migrated.translationRecordDao().getBySelection(
                quoteId = record.quoteId, script = record.script, fidelity = record.fidelity,
                variant = record.variant, engineVersion = record.engineVersion, datasetVersion = record.datasetVersion,
                sourceText = record.sourceText
            )
            assertThat(selected).isEqualTo(record)
        }
        val quote = requireNotNull(migrated.quoteDao().getById(1L))
        assertThat(quote.textLatin).isEqualTo("wolf")
        assertThat(quote.author).isEqualTo("User")
        assertThat(quote.isUserCreated).isTrue()
        assertThat(quote.isFavorite).isTrue()
        assertThat(quote.createdAt).isEqualTo(42L)
        assertThat(migrated.quoteDao().getCount()).isEqualTo(2)
        val checkpoint = requireNotNull(migrated.translationBackfillStateDao().getById())
        assertThat(checkpoint.engineVersion).isEqualTo("saved-checkpoint")
        assertThat(checkpoint.lastProcessedQuoteId).isEqualTo(2L)
        assertThat(checkpoint.completedAt).isEqualTo(300L)

        migrated.close()
        database = null
        AndroidSQLiteDriver().open(context.getDatabasePath(databaseName).absolutePath).use { connection ->
            assertThat(rowCount(connection)).isEqualTo(expected.size)
            connection.execSQL("PRAGMA foreign_keys = ON")
            connection.execSQL("DELETE FROM quotes WHERE id = 2")
            assertThat(rowCount(connection)).isEqualTo(expected.size - 1)
        }
    }

    private fun legacyRecords(): List<LegacyRecord> {
        val base = TranslationRecordEntity(
            id = 1L, quoteId = 1L, sourceText = "wolf", script = "ELDER_FUTHARK", fidelity = "STRICT",
            derivationKind = "TOKEN_COMPOSED", normalizedForm = "wulfaz", diplomaticForm = "wulfaz",
            glyphOutput = "ᚹᚢᛚᚠᚨᛉ", historicalStage = "PROTO_NORSE", resolutionStatus = "RECONSTRUCTED",
            confidence = 0.91f, notesJson = "[\"retained notes\"]", unresolvedTokensJson = "[]",
            provenanceJson = "[]", tokenBreakdownJson = "[]", engineVersion = "engine-current",
            datasetVersion = "dataset-current", isBackfilled = true, createdAt = 12L, updatedAt = 100L
        )
        return listOf(
            LegacyRecord(base),
            LegacyRecord(base.copy(id = 2L, glyphOutput = "newer", updatedAt = 200L)),
            LegacyRecord(base.copy(id = 3L, glyphOutput = "tie-winner", updatedAt = 200L)),
            LegacyRecord(base.copy(id = 4L, glyphOutput = "empty-key-duplicate", updatedAt = 150L), ""),
            LegacyRecord(base.copy(id = 5L, datasetVersion = "dataset-old")),
            LegacyRecord(base.copy(id = 6L, engineVersion = "engine-old")),
            LegacyRecord(base.copy(id = 7L, fidelity = "READABLE")),
            LegacyRecord(base.copy(id = 8L, script = "CIRTH", historicalStage = "EREBOR_ENGLISH")),
            LegacyRecord(base.copy(id = 9L, script = "CIRTH", historicalStage = "EREBOR_ENGLISH", updatedAt = 300L)),
            LegacyRecord(base.copy(id = 10L, script = "YOUNGER_FUTHARK", variant = "LONG_BRANCH"), "LONG_BRANCH"),
            LegacyRecord(base.copy(id = 11L, script = "YOUNGER_FUTHARK", variant = "SHORT_TWIG"), "SHORT_TWIG"),
            LegacyRecord(base.copy(id = 12L, quoteId = 2L)),
            LegacyRecord(
                base.copy(id = 13L, script = "YOUNGER_FUTHARK", variant = "LONG_BRANCH", engineVersion = "engine-old"),
                "LONG_BRANCH"
            )
        )
    }

    private fun createVersion9(): SQLiteConnection {
        context.deleteDatabase(databaseName)
        val file = context.getDatabasePath(databaseName)
        file.parentFile?.mkdirs()
        val connection = AndroidSQLiteDriver().open(file.absolutePath)
        val schemaFile = sequenceOf(
            File("schemas/com.po4yka.runatal.data.local.RunatalDatabase/9.json"),
            File("app/schemas/com.po4yka.runatal.data.local.RunatalDatabase/9.json")
        ).first { it.isFile }
        val schema = Json.parseToJsonElement(schemaFile.readText()).jsonObject.getValue("database").jsonObject
        schema.getValue("entities").jsonArray.forEach { element ->
            val entity = element.jsonObject
            val tableName = entity.getValue("tableName").jsonPrimitive.content
            connection.execSQL(entity.getValue("createSql").jsonPrimitive.content.replace("\${TABLE_NAME}", tableName))
            entity["indices"]?.jsonArray.orEmpty().forEach { index ->
                connection.execSQL(
                    index.jsonObject.getValue("createSql").jsonPrimitive.content.replace("\${TABLE_NAME}", tableName)
                )
            }
        }
        schema.getValue("setupQueries").jsonArray.forEach { connection.execSQL(it.jsonPrimitive.content) }
        connection.execSQL("PRAGMA user_version = 9")
        connection.execSQL("PRAGMA foreign_keys = ON")
        return connection
    }

    private fun insertLegacyRecord(connection: SQLiteConnection, legacy: LegacyRecord) {
        val row = legacy.record
        val columns = "id, quoteId, sourceText, script, fidelity, derivationKind, normalizedForm, diplomaticForm, " +
            "glyphOutput, historicalStage, variant, resolutionStatus, confidence, notesJson, unresolvedTokensJson, " +
            "provenanceJson, tokenBreakdownJson, engineVersion, datasetVersion, isBackfilled, createdAt, updatedAt"
        val values = listOf(
            row.id, row.quoteId, row.sourceText, row.script, row.fidelity, row.derivationKind, row.normalizedForm,
            row.diplomaticForm, row.glyphOutput, row.historicalStage, legacy.variant, row.resolutionStatus,
            row.confidence, row.notesJson, row.unresolvedTokensJson, row.provenanceJson, row.tokenBreakdownJson,
            row.engineVersion, row.datasetVersion, if (row.isBackfilled) 1L else 0L, row.createdAt, row.updatedAt
        )
        val placeholders = values.joinToString(",") { "?" }
        connection.prepare("INSERT INTO translation_records ($columns) VALUES ($placeholders)").use { statement ->
            values.forEachIndexed { index, value ->
                when (value) {
                    null -> statement.bindNull(index + 1)
                    is String -> statement.bindText(index + 1, value)
                    is Long -> statement.bindLong(index + 1, value)
                    is Float -> statement.bindDouble(index + 1, value.toDouble())
                    else -> error("Unexpected stored value type: ${value::class}")
                }
            }
            statement.step()
        }
    }

    private fun rowCount(connection: SQLiteConnection): Int = connection.prepare(
        "SELECT COUNT(*) FROM translation_records"
    ).use { statement ->
        check(statement.step())
        statement.getLong(0).toInt()
    }

    private data class LegacyRecord(val record: TranslationRecordEntity, val variant: String? = null)
}
