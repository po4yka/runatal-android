package com.po4yka.runatal.data.local

import android.app.Application
import android.database.sqlite.SQLiteException
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.po4yka.runatal.data.local.entity.QuoteEntity
import com.po4yka.runatal.data.local.entity.TranslationBackfillCompletionEntity
import com.po4yka.runatal.data.local.entity.TranslationRecordEntity
import com.po4yka.runatal.data.repository.TranslationRepositoryImpl
import com.po4yka.runatal.data.translation.bundledTranslationTestData
import com.po4yka.runatal.domain.model.RunicScript
import com.po4yka.runatal.domain.translation.ElderFutharkTranslationEngine
import com.po4yka.runatal.domain.translation.EreborCirthTranslationEngine
import com.po4yka.runatal.domain.translation.HistoricalTranslationService
import com.po4yka.runatal.domain.translation.TranslationEngineFactory
import com.po4yka.runatal.domain.translation.YoungerFutharkTranslationEngine
import com.po4yka.runatal.domain.transliteration.CirthTransliterator
import com.po4yka.runatal.domain.transliteration.ElderFutharkTransliterator
import io.mockk.spyk
import io.mockk.verify
import java.io.File
import java.io.IOException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.SQLiteMode

/** Native database and actual bundled engines exercise dirty edits, restores and completed refusals. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class TranslationBackfillDatabaseTest {
    private val context = RuntimeEnvironment.getApplication()
    private val databaseName = "dirty-backfill.db"
    private lateinit var database: RunatalDatabase
    private lateinit var repository: TranslationRepositoryImpl
    private lateinit var service: HistoricalTranslationService
    private lateinit var engines: TranslationEngineFactory
    private val source = "The wolf hunts at night"

    @Before
    fun setUp() {
        context.deleteDatabase(databaseName)
        database = Room.databaseBuilder(context, RunatalDatabase::class.java, databaseName)
            .setDriver(AndroidSQLiteDriver()).build()
        val dataset = bundledTranslationTestData()
        engines = TranslationEngineFactory(
            ElderFutharkTranslationEngine(dataset, dataset, ElderFutharkTransliterator()),
            YoungerFutharkTranslationEngine(dataset, dataset),
            EreborCirthTranslationEngine(dataset, dataset, CirthTransliterator())
        )
        service = spyk(HistoricalTranslationService(engines))
        repository = TranslationRepositoryImpl(
            database.quoteDao(), database.translationRecordDao(), database.translationBackfillCompletionDao(),
            service, engines
        )
    }

    @After
    fun tearDown() {
        database.close()
        context.deleteDatabase(databaseName)
    }

    @Test
    fun `low id edit and restore are dirty despite an old high global cursor`() = runTest {
        database.quoteDao().insert(quote())
        executeSql("INSERT INTO translation_backfill_state VALUES (1, 'legacy', 999, 999, 1, 2, 3)")
        repository.backfillAllQuotes()
        assertThat(pending()).isEmpty()
        val oldResult = requireNotNull(latest(source))
        assertThat(oldResult.glyphOutput).isNotEmpty()

        val changed = quote().copy(textLatin = "I hunt")
        assertThat(database.quoteDao().updateUserContent(changed, source, "User")).isEqualTo(changed)
        assertThat(pending().map { it.id }).containsExactly(1L)
        repository.backfillAllQuotes()
        assertThat(pending()).isEmpty()
        assertThat(latest("I hunt")?.normalizedForm).isEqualTo("ek veiði")

        database.quoteDao().deleteUserQuote(1L)
        database.quoteDao().insert(quote())
        assertThat(pending().map { it.id }).containsExactly(1L)
        repository.backfillAllQuotes()
        assertThat(latest(source)?.glyphOutput).isEqualTo(oldResult.glyphOutput)
    }

    @Test
    fun `unavailable source completes once and engine or dataset versions make it dirty again`() = runTest {
        database.quoteDao().insert(quote().copy(textLatin = "spaceship quasar"))
        repository.backfillAllQuotes()
        repository.backfillAllQuotes()
        assertThat(pending()).isEmpty()
        assertThat(count("translation_records")).isEqualTo(0)
        verify(exactly = 2) { service.translate("spaceship quasar", any(), any(), any()) }
        verify(exactly = 0) { service.translate(any(), RunicScript.CIRTH, any(), any()) }
        assertThat(database.translationBackfillCompletionDao().pendingQuotes(version() + "engine-v2", 25))
            .hasSize(1)
        assertThat(database.translationBackfillCompletionDao().pendingQuotes(version() + "dataset-v2", 25))
            .hasSize(1)
    }

    @Test
    fun `A to B to A invalidates completion even when final source equals original`() = runTest {
        database.quoteDao().insert(quote())
        repository.backfillAllQuotes()
        database.quoteDao().updateUserContent(quote().copy(textLatin = "I hunt"), source, "User")
        database.quoteDao().updateUserContent(quote(), "I hunt", "User")
        assertThat(count("translation_records")).isEqualTo(0)
        assertThat(pending()).hasSize(1)
        repository.backfillAllQuotes()
        assertThat(latest(source)?.glyphOutput).isNotEmpty()
        assertThat(pending()).isEmpty()
    }

    @Test
    fun `stale or deleted attempt writes neither cache nor completion`() = runTest {
        database.quoteDao().insert(quote().copy(textLatin = "changed"))
        val dao = database.translationBackfillCompletionDao()
        assertThat(dao.completeAttempt(completion(), listOf(record()))).isFalse()
        database.quoteDao().deleteUserQuote(1L)
        assertThat(dao.completeAttempt(completion(), listOf(record()))).isFalse()
        assertThat(count("translation_records")).isEqualTo(0)
        assertThat(count("translation_backfill_completions")).isEqualTo(0)
    }

    @Test
    fun `cache and completion rollback together and retry produces one completed attempt`() = runTest {
        database.quoteDao().insert(quote())
        executeSql("CREATE TRIGGER fail_completion BEFORE INSERT ON translation_backfill_completions " +
            "BEGIN SELECT RAISE(ABORT, 'completion failed'); END")
        val failure = runCatching { repository.backfillAllQuotes() }.exceptionOrNull()
        assertThat(failure).isInstanceOf(IOException::class.java)
        assertThat(failure?.cause).isInstanceOf(SQLiteException::class.java)
        assertThat(count("translation_records")).isEqualTo(0)
        assertThat(pending()).hasSize(1)
        executeSql("DROP TRIGGER fail_completion")
        repository.backfillAllQuotes()
        // This source has a cited Younger reconstruction; Elder has no published attestation.
        assertThat(count("translation_records")).isEqualTo(1)
        assertThat(latest(source)?.normalizedForm).isEqualTo("úlfrinn veiðir um nótt")
        assertThat(pending()).isEmpty()
    }

    @Test
    fun `backfill preserves an existing manually saved selection and bounded batch ordering`() = runTest {
        database.quoteDao().insertAll((1L..31L).map { quote(it) })
        assertThat(database.translationBackfillCompletionDao().pendingQuotes(version(), 25).map { it.id })
            .containsExactlyElementsIn(1L..25L).inOrder()
        database.translationRecordDao().insert(record().copy(glyphOutput = "manual glyphs", isBackfilled = false))
        repository.backfillAllQuotes()
        assertThat(pending()).isEmpty()
        assertThat(latest(source, RunicScript.ELDER_FUTHARK)?.glyphOutput).isEqualTo("manual glyphs")
        assertThat(count("translation_backfill_completions")).isEqualTo(31)
    }

    @Test
    fun `quote observer emits committed writes even when identity query remains equal`() = runTest {
        database.quoteDao().insert(quote())
        database.quoteDao().observeQuoteIdentities().test {
            assertThat(awaitItem()).containsExactly(1L)
            database.quoteDao().updateUserContent(quote().copy(textLatin = "I hunt"), source, "User")
            assertThat(awaitItem()).containsExactly(1L)
            database.quoteDao().updateUserContent(quote(), "I hunt", "User")
            assertThat(awaitItem()).containsExactly(1L)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `full schema migration preserves quotes cache and legacy checkpoint but trusts none as completion`() = runTest {
        database.close()
        createVersion10()
        database = Room.databaseBuilder(context, RunatalDatabase::class.java, databaseName)
            .setDriver(AndroidSQLiteDriver()).addMigrations(
                RunatalDatabase.MIGRATION_10_11, RunatalDatabase.MIGRATION_11_12,
                RunatalDatabase.MIGRATION_12_13, RunatalDatabase.MIGRATION_13_14,
                RunatalDatabase.MIGRATION_14_15, RunatalDatabase.MIGRATION_15_16
            ).build()
        val migrated = requireNotNull(database.quoteDao().getById(1L))
        assertThat(migrated.textLatin).isEqualTo(source)
        assertThat(migrated.author).isEqualTo("User")
        assertThat(migrated.isFavorite).isTrue()
        assertThat(migrated.createdAt).isEqualTo(42L)
        assertThat(count("translation_records")).isEqualTo(1)
        assertThat(database.translationBackfillStateDao().getById()?.lastProcessedQuoteId).isEqualTo(999L)
        assertThat(pending()).hasSize(1)
        repository = TranslationRepositoryImpl(
            database.quoteDao(), database.translationRecordDao(), database.translationBackfillCompletionDao(),
            service, engines
        )
        repository.backfillAllQuotes()
        assertThat(pending()).isEmpty()
        assertThat(latest(source, RunicScript.ELDER_FUTHARK)?.glyphOutput).isEqualTo("preserved manual")
    }

    private fun createVersion10() {
        context.deleteDatabase(databaseName)
        val file = context.getDatabasePath(databaseName)
        file.parentFile?.mkdirs()
        val schemaFile = sequenceOf(
            File("schemas/com.po4yka.runatal.data.local.RunatalDatabase/10.json"),
            File("app/schemas/com.po4yka.runatal.data.local.RunatalDatabase/10.json")
        ).first { it.isFile }
        val schema = Json.parseToJsonElement(schemaFile.readText()).jsonObject.getValue("database").jsonObject
        AndroidSQLiteDriver().open(file.absolutePath).use { connection ->
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
            connection.execSQL("PRAGMA user_version = 10")
            connection.execSQL(
                "INSERT INTO quotes (id, textLatin, author, isUserCreated, isFavorite, createdAt) " +
                    "VALUES (1, '$source', 'User', 1, 1, 42)"
            )
            connection.execSQL("INSERT INTO translation_backfill_state VALUES (1, 'legacy', 999, 999, 1, 2, 3)")
            val engine = engines.create(RunicScript.ELDER_FUTHARK)
            connection.prepare(
                """INSERT INTO translation_records (
                    quoteId, sourceText, script, fidelity, derivationKind, normalizedForm, diplomaticForm, glyphOutput,
                    historicalStage, variant, resolutionStatus, confidence, notesJson, unresolvedTokensJson,
                    provenanceJson, tokenBreakdownJson, engineVersion, datasetVersion,
                    isBackfilled, createdAt, updatedAt
                ) VALUES (1, ?, 'ELDER_FUTHARK', 'STRICT', 'TOKEN_COMPOSED', 'manual', 'manual', 'preserved manual',
                    'PROTO_NORSE', '', 'RECONSTRUCTED', 1, '[]', '[]', '[]', '[]', ?, ?, 0, 1, 1)""".trimIndent()
            ).use { statement ->
                statement.bindText(1, source)
                statement.bindText(2, engine.engineVersion)
                statement.bindText(3, engine.datasetVersion)
                statement.step()
            }
        }
    }

    private suspend fun pending() = database.translationBackfillCompletionDao().pendingQuotes(version(), 25)

    private fun version() = listOf(RunicScript.ELDER_FUTHARK, RunicScript.YOUNGER_FUTHARK).joinToString("|") {
        val engine = engines.create(it)
        "${it.name}:${engine.engineVersion}:${engine.datasetVersion}"
    }

    private suspend fun latest(text: String, script: RunicScript = RunicScript.YOUNGER_FUTHARK) =
        repository.getCachedTranslation(1L, script, text)

    private fun quote(id: Long = 1L) = QuoteEntity(id = id, textLatin = source, author = "User", isUserCreated = true)

    private fun completion() = TranslationBackfillCompletionEntity(1L, source, version(), 100L)

    private fun record(): TranslationRecordEntity {
        val engine = engines.create(RunicScript.ELDER_FUTHARK)
        return TranslationRecordEntity(
            quoteId = 1L, sourceText = source, script = "ELDER_FUTHARK", fidelity = "STRICT",
            derivationKind = "TOKEN_COMPOSED", normalizedForm = "manual", diplomaticForm = "manual",
            glyphOutput = "manual", historicalStage = "PROTO_NORSE", resolutionStatus = "RECONSTRUCTED",
            confidence = 1f, notesJson = "[]", unresolvedTokensJson = "[]", provenanceJson = "[]",
            tokenBreakdownJson = "[]", engineVersion = engine.engineVersion, datasetVersion = engine.datasetVersion,
            createdAt = Long.MAX_VALUE, updatedAt = Long.MAX_VALUE
        )
    }

    private fun count(table: String): Int =
        AndroidSQLiteDriver().open(context.getDatabasePath(databaseName).absolutePath).use { connection ->
            connection.prepare("SELECT COUNT(*) FROM $table").use { statement ->
                check(statement.step())
                statement.getLong(0).toInt()
            }
        }

    private fun executeSql(sql: String) {
        AndroidSQLiteDriver().open(context.getDatabasePath(databaseName).absolutePath).use { it.execSQL(sql) }
    }
}
