package com.po4yka.runatal.data.local

import android.app.Application
import androidx.room3.Room
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import com.google.common.truth.Truth.assertThat
import com.po4yka.runatal.data.local.entity.QuoteEntity
import com.po4yka.runatal.data.seed.QuoteSeedData
import com.po4yka.runatal.domain.transliteration.YoungerFutharkTransliterator
import com.po4yka.runatal.domain.transliteration.ElderFutharkTransliterator
import com.po4yka.runatal.domain.transliteration.CirthTransliterator
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

/** Migrates the actual exported v8 schema through generated Room v9 validation and native SQLite. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class YoungerFutharkMigrationTest {

    private val context = RuntimeEnvironment.getApplication()
    private val databaseName = "younger-rendering-migration-test.db"
    private var database: RunatalDatabase? = null

    @After
    fun tearDown() {
        database?.close()
        context.deleteDatabase(databaseName)
    }

    @Test
    fun `v8 migration repairs proven generated strings while preserving quote identities and stored historical data`() =
        runTest {
            val quotes = fixtureQuotes()
            createVersion8().use { connection ->
                quotes.forEach { insertQuote(connection, it) }
                insertTranslation(connection, 1L, "ELDER_FUTHARK", "keep elder", 41L)
                insertTranslation(connection, 10L, "YOUNGER_FUTHARK", "ᚲᛁᚾ", 42L)
                insertTranslation(connection, 15L, "CIRTH", "historical cirth", 43L)
                insertTranslation(connection, 16L, "ELDER_FUTHARK", "mismatched source", 44L, "Other")
                connection.execSQL(
                    "INSERT INTO rune_references VALUES " +
                        "(91, 'ᛊ', 'Sol', 's', 'Sun', 'Keep history', 'younger_futhark')"
                )
                connection.execSQL(
                    "INSERT INTO rune_references VALUES " +
                        "(92, 'ᛗ', 'Madr', 'm', 'Man', 'Keep history', 'younger_futhark')"
                )
                connection.execSQL(
                    "INSERT INTO rune_references VALUES " +
                        "(93, 'ᛊ', 'Sowilo', 's', 'Sun', 'Elder', 'elder_futhark')"
                )
            }
            database = Room.databaseBuilder(context, RunatalDatabase::class.java, databaseName)
                .setDriver(AndroidSQLiteDriver())
                .addMigrations(RunatalDatabase.MIGRATION_8_9, RunatalDatabase.MIGRATION_9_10)
                .build()
            val migrated = requireNotNull(database)
            val converter = YoungerFutharkTransliterator()
            quotes.forEach { before ->
                val expected = when {
                    before.id == 1L -> before.copy(
                        runicElder = ElderFutharkTransliterator().transliterate(before.textLatin),
                        runicYounger = converter.transliterate(before.textLatin)
                    )
                    before.id == 10L || before.canonicalKey != null ->
                        before.copy(runicYounger = converter.transliterate(before.textLatin))
                    before.id == 15L -> before.copy(
                        runicCirth = CirthTransliterator().transliterate(before.textLatin)
                    )
                    else -> before
                }
                assertThat(migrated.quoteDao().getById(before.id)).isEqualTo(expected)
            }
            assertThat(migrated.quoteDao().getAll()).hasSize(quotes.size)
            val record = migrated.translationRecordDao().getLatestAvailableForScript(
                10L, "YOUNGER_FUTHARK", "UNAVAILABLE", "old-engine", "old-dataset", "King"
            )
            assertThat(record?.id).isEqualTo(42L)
            assertThat(record?.glyphOutput).isEqualTo("ᚲᛁᚾ")
            val elderRecord = migrated.translationRecordDao().getLatestAvailableForScript(
                1L, "ELDER_FUTHARK", "UNAVAILABLE", "old-engine", "old-dataset", "King"
            )
            assertThat(elderRecord?.id).isEqualTo(41L)
            assertThat(elderRecord?.glyphOutput).isEqualTo("keep elder")
            val cirthRecord = migrated.translationRecordDao().getLatestAvailableForScript(
                15L, "CIRTH", "UNAVAILABLE", "old-engine", "old-dataset", "King"
            )
            assertThat(cirthRecord?.id).isEqualTo(43L)
            assertThat(cirthRecord?.glyphOutput).isEqualTo("historical cirth")
            assertThat(migrated.runeReferenceDao().getById(91L)?.character).isEqualTo("ᛋ")
            assertThat(migrated.runeReferenceDao().getById(92L)?.character).isEqualTo("ᛘ")
            assertThat(migrated.runeReferenceDao().getById(93L)?.character).isEqualTo("ᛊ")
        }

    private fun fixtureQuotes(): List<QuoteEntity> {
        val generated = QuoteEntity(
            id = 1L, textLatin = "King", author = "User", runicElder = "keep elder", runicYounger = "ᚲᛁᚾ",
            runicCirth = "keep cirth", isUserCreated = true, isFavorite = true, createdAt = 123L
        )
        return listOf(
            generated,
            generated.copy(id = 10L, author = "Historical with cache"),
            generated.copy(id = 11L, textLatin = "wolf", runicYounger = "ᚢᛚᚠᚱ", author = "Historical without cache"),
            generated.copy(id = 12L, runicYounger = "handwritten custom glyphs"),
            generated.copy(id = 13L, runicYounger = null),
            generated.copy(id = 14L, runicYounger = "ᚴᛁᚾ"),
            generated.copy(id = 15L, runicYounger = null, runicCirth = "historical cirth"),
            generated.copy(id = 16L, runicYounger = null, runicElder = "mismatched source"),
            generated.copy(id = 17L, runicYounger = null, runicElder = null, runicCirth = null)
        ) + QuoteSeedData.getLegacyQuotes().mapIndexed { index, quote ->
            quote.copy(id = 100L + index, isFavorite = true, createdAt = 456L)
        }
    }

    private fun createVersion8(): SQLiteConnection {
        context.deleteDatabase(databaseName)
        val file = context.getDatabasePath(databaseName)
        file.parentFile?.mkdirs()
        val connection = AndroidSQLiteDriver().open(file.absolutePath)
        val schemaFile = sequenceOf(
            File("schemas/com.po4yka.runatal.data.local.RunatalDatabase/8.json"),
            File("app/schemas/com.po4yka.runatal.data.local.RunatalDatabase/8.json")
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
        connection.execSQL("PRAGMA user_version = 8")
        connection.execSQL("PRAGMA foreign_keys = ON")
        return connection
    }

    private fun insertQuote(connection: SQLiteConnection, quote: QuoteEntity) {
        connection.prepare(
            "INSERT INTO quotes (id, textLatin, author, runicElder, runicYounger, runicCirth, " +
                "isUserCreated, isFavorite, createdAt, canonicalKey) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
        ).use { statement ->
            statement.bindLong(1, quote.id)
            statement.bindText(2, quote.textLatin)
            statement.bindText(3, quote.author)
            listOf(quote.runicElder, quote.runicYounger, quote.runicCirth).forEachIndexed { index, value ->
                if (value == null) statement.bindNull(4 + index) else statement.bindText(4 + index, value)
            }
            statement.bindLong(7, if (quote.isUserCreated) 1L else 0L)
            statement.bindLong(8, if (quote.isFavorite) 1L else 0L)
            statement.bindLong(9, quote.createdAt)
            val canonicalKey = quote.canonicalKey
            if (canonicalKey == null) statement.bindNull(10) else statement.bindText(10, canonicalKey)
            statement.step()
        }
    }

    private fun insertTranslation(
        connection: SQLiteConnection, quoteId: Long, script: String, glyphs: String, id: Long,
        sourceText: String = "King"
    ) {
        connection.prepare(
            "INSERT INTO translation_records (id, quoteId, sourceText, script, fidelity, derivationKind, " +
                "normalizedForm, diplomaticForm, glyphOutput, historicalStage, resolutionStatus, confidence, " +
                "notesJson, unresolvedTokensJson, provenanceJson, tokenBreakdownJson, engineVersion, " +
                "datasetVersion, isBackfilled, createdAt, updatedAt) " +
                "VALUES (?, ?, ?, ?, 'STRICT', 'TOKEN_COMPOSED', 'historical', 'historic', ?, " +
                "'OLD_NORSE', 'RECONSTRUCTED', 0.9, '[]', '[]', '[]', '[]', 'old-engine', 'old-dataset', 0, 12, 34)"
        ).use { statement ->
            statement.bindLong(1, id)
            statement.bindLong(2, quoteId)
            statement.bindText(3, sourceText)
            statement.bindText(4, script)
            statement.bindText(5, glyphs)
            statement.step()
        }
    }
}
