package com.po4yka.runatal.data.local

import android.app.Application
import androidx.room3.Room
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import com.google.common.truth.Truth.assertThat
import com.po4yka.runatal.data.local.entity.QuoteEntity
import com.po4yka.runatal.data.repository.ArchiveRepositoryImpl
import com.po4yka.runatal.domain.model.QuoteLifecycleState
import java.io.File
import kotlinx.coroutines.flow.first
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

/** Full historical-schema migration verifies exact known snapshots and collision-safe manual recovery. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class QuoteLifecycleMigrationTest {
    private val context = RuntimeEnvironment.getApplication()
    private val databaseName = "quote-lifecycle-migration.db"
    private var database: RunatalDatabase? = null
    private val known = QuoteEntity(
        id = 1L, textLatin = "Known archived source", author = "User", runicElder = "manual elder",
        runicYounger = "manual younger", runicCirth = "manual Cirth", isUserCreated = false, isFavorite = true,
        createdAt = 42L, canonicalKey = "known-catalog-key"
    )
    private val edited = QuoteEntity(id = 2L, textLatin = "Edited after snapshot", author = "User",
        runicElder = "keep edited rendering", isUserCreated = true, isFavorite = true, createdAt = 99L)

    @After
    fun tearDown() {
        database?.close()
        context.deleteDatabase(databaseName)
    }

    @Test
    fun `migration preserves originals and records across collisions and fresh recovery ids`() = runTest {
        createVersion10().use { connection ->
            insertQuote(connection, known)
            insertQuote(connection, edited)
            insertQuote(connection, edited.copy(id = 5L, textLatin = "Unaffected source"))
            connection.execSQL(
                """INSERT INTO translation_records (
                    id, quoteId, sourceText, script, fidelity, derivationKind, normalizedForm, diplomaticForm,
                    glyphOutput, historicalStage, variant, resolutionStatus, confidence,
                    notesJson, unresolvedTokensJson,
                    provenanceJson, tokenBreakdownJson, engineVersion, datasetVersion,
                    isBackfilled, createdAt, updatedAt
                ) VALUES (11, 1, 'Known archived source', 'ELDER_FUTHARK', 'STRICT', 'TOKEN_COMPOSED',
                    'normalized', 'diplomatic', 'record retained', 'PROTO_NORSE', '', 'RECONSTRUCTED', 0.9,
                    '["retained note"]', '[]', '[]', '[]', 'manual-engine', 'manual-dataset', 0, 12, 13)""".trimIndent()
            )
            connection.execSQL(
                """INSERT INTO translation_records (
                    id, quoteId, sourceText, script, fidelity, derivationKind, normalizedForm, diplomaticForm,
                    glyphOutput, historicalStage, variant, resolutionStatus, confidence,
                    notesJson, unresolvedTokensJson,
                    provenanceJson, tokenBreakdownJson, engineVersion, datasetVersion,
                    isBackfilled, createdAt, updatedAt
                ) SELECT 12, 2, 'Previous content snapshot', script, fidelity, derivationKind,
                    normalizedForm, diplomaticForm, 'snapshot historical', historicalStage, variant, resolutionStatus,
                    confidence, '["snapshot record retained"]', unresolvedTokensJson,
                    provenanceJson, tokenBreakdownJson,
                    engineVersion, datasetVersion, isBackfilled, createdAt, updatedAt
                FROM translation_records WHERE id = 11
                """.trimIndent()
            )
            snapshot(connection, 1L, known.textLatin, 100L, false)
            snapshot(connection, 999L, "Recovered source", 120L, false)
            // Auto-allocation of the previous recovery creates ID6. This original ID did not exist before migration.
            snapshot(connection, 6L, "Recovered source", 130L, true)
            snapshot(connection, 2L, "Previous content snapshot", 150L, false)
            snapshot(connection, 1L, known.textLatin, 200L, true)
        }
        database = Room.databaseBuilder(context, RunatalDatabase::class.java, databaseName)
            .setDriver(AndroidSQLiteDriver()).addMigrations(RunatalDatabase.MIGRATION_16_17).build()
        val migrated = requireNotNull(database)
        val retained = requireNotNull(migrated.archivedQuoteDao().getRetainedById(1L))
        assertThat(retained.copy(lifecycleState = "ACTIVE", lifecycleChangedAt = 0L)).isEqualTo(known)
        assertThat(retained.lifecycleState).isEqualTo("TRASH")
        assertThat(retained.lifecycleChangedAt).isEqualTo(200L)
        assertThat(migrated.quoteDao().getById(1L)).isNull()
        assertThat(migrated.quoteDao().getById(2L)).isEqualTo(edited)
        val archive = ArchiveRepositoryImpl(migrated.archivedQuoteDao())
        val snapshots = archive.getRetainedQuotesFlow().first()
        assertThat(snapshots.map { it.id }).containsExactly(1L, 6L, 7L, 8L)
        val recoveries = snapshots.filter { it.textLatin == "Recovered source" }
        assertThat(recoveries.map { it.lifecycleState })
            .containsExactly(QuoteLifecycleState.ARCHIVED, QuoteLifecycleState.TRASH)
        recoveries.forEach { recovered ->
            val row = requireNotNull(migrated.archivedQuoteDao().getRetainedById(recovered.id))
            assertThat(row.runicElder).isNull()
            assertThat(row.runicYounger).isNull()
            assertThat(row.runicCirth).isNull()
            assertThat(row.createdAt).isEqualTo(0L)
            assertThat(row.canonicalKey).isNull()
            assertThat(row.textLatin).isEqualTo("Recovered source")
        }
        assertThat(count("translation_records")).isEqualTo(3)
        assertThat(tableExists("archived_quotes")).isFalse()
        val original = snapshots.single { it.id == 1L }
        val collision = snapshots.single { it.textLatin == "Previous content snapshot" }
        archive.restoreQuotes(listOf(original, collision))
        assertThat(migrated.quoteDao().getById(2L)).isEqualTo(edited)
        assertThat(migrated.quoteDao().getById(collision.id)?.textLatin).isEqualTo("Previous content snapshot")
        val recoveredRecord = requireNotNull(migrated.translationRecordDao().getBySelection(
            collision.id, "ELDER_FUTHARK", "STRICT", "", "manual-engine", "manual-dataset", collision.textLatin
        ))
        assertThat(recoveredRecord.glyphOutput).isEqualTo("snapshot historical")
        assertThat(recoveredRecord.notesJson).isEqualTo("[\"snapshot record retained\"]")
        assertThat(recoveredRecord.id).isGreaterThan(12L)
        assertThat(migrated.translationRecordDao().getBySelection(
            2L, "ELDER_FUTHARK", "STRICT", "", "manual-engine", "manual-dataset", "Previous content snapshot"
        )).isNull()
        val restored = requireNotNull(migrated.quoteDao().getById(1L))
        assertThat(restored.copy(lifecycleState = "ACTIVE", lifecycleChangedAt = 0L, lifecycleMutationId = null))
            .isEqualTo(known)
        assertThat(migrated.translationRecordDao().getBySelection(
            1L, "ELDER_FUTHARK", "STRICT", "", "manual-engine", "manual-dataset", known.textLatin
        )?.notesJson).isEqualTo("[\"retained note\"]")
    }

    private fun createVersion10(): SQLiteConnection {
        context.deleteDatabase(databaseName)
        val file = context.getDatabasePath(databaseName)
        file.parentFile?.mkdirs()
        val connection = AndroidSQLiteDriver().open(file.absolutePath)
        val schemaFile = sequenceOf(
            File("schemas/com.po4yka.runatal.data.local.RunatalDatabase/16.json"),
            File("app/schemas/com.po4yka.runatal.data.local.RunatalDatabase/16.json")
        ).first { it.isFile }
        val schema = Json.parseToJsonElement(schemaFile.readText()).jsonObject.getValue("database").jsonObject
        schema.getValue("entities").jsonArray.forEach { element ->
            val entity = element.jsonObject
            val table = entity.getValue("tableName").jsonPrimitive.content
            connection.execSQL(entity.getValue("createSql").jsonPrimitive.content.replace("\${TABLE_NAME}", table))
            entity["indices"]?.jsonArray.orEmpty().forEach { index ->
                connection.execSQL(
                    index.jsonObject.getValue("createSql").jsonPrimitive.content.replace("\${TABLE_NAME}", table)
                )
            }
        }
        schema.getValue("setupQueries").jsonArray.forEach { connection.execSQL(it.jsonPrimitive.content) }
        connection.execSQL("PRAGMA user_version = 16")
        return connection
    }

    private fun insertQuote(connection: SQLiteConnection, quote: QuoteEntity) {
        connection.prepare(
            "INSERT INTO quotes(id, textLatin, author, runicElder, runicYounger, runicCirth, " +
                "isUserCreated, isFavorite, createdAt, canonicalKey) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
        ).use { statement ->
            statement.bindLong(1, quote.id)
            statement.bindText(2, quote.textLatin)
            statement.bindText(3, quote.author)
            quote.runicElder?.let { statement.bindText(4, it) } ?: statement.bindNull(4)
            quote.runicYounger?.let { statement.bindText(5, it) } ?: statement.bindNull(5)
            quote.runicCirth?.let { statement.bindText(6, it) } ?: statement.bindNull(6)
            statement.bindLong(7, if (quote.isUserCreated) 1L else 0L)
            statement.bindLong(8, if (quote.isFavorite) 1L else 0L)
            statement.bindLong(9, quote.createdAt)
            quote.canonicalKey?.let { statement.bindText(10, it) } ?: statement.bindNull(10)
            statement.step()
        }
    }

    private fun snapshot(connection: SQLiteConnection, id: Long, source: String, at: Long, deleted: Boolean) {
        connection.prepare(
            "INSERT INTO archived_quotes(originalQuoteId, textLatin, author, archivedAt, isDeleted) " +
                "VALUES (?, ?, 'User', ?, ?)"
        ).use { statement ->
            statement.bindLong(1, id)
            statement.bindText(2, source)
            statement.bindLong(3, at)
            statement.bindLong(4, if (deleted) 1L else 0L)
            statement.step()
        }
    }

    private fun count(table: String): Int = withNative { connection ->
        connection.prepare("SELECT COUNT(*) FROM $table").use { statement ->
            check(statement.step())
            statement.getLong(0).toInt()
        }
    }

    private fun tableExists(table: String): Boolean = withNative { connection ->
        connection.prepare("SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?").use { statement ->
            statement.bindText(1, table)
            statement.step()
        }
    }

    private fun <T> withNative(block: (SQLiteConnection) -> T): T =
        AndroidSQLiteDriver().open(context.getDatabasePath(databaseName).absolutePath).use(block)
}
