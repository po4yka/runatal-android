package com.po4yka.runatal.data.local

import android.app.Application
import android.database.sqlite.SQLiteException
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import com.google.common.truth.Truth.assertThat
import com.po4yka.runatal.data.local.entity.QuoteEntity
import com.po4yka.runatal.data.local.entity.TranslationRecordEntity
import com.po4yka.runatal.data.repository.QuoteRepositoryImpl
import com.po4yka.runatal.util.TimeProvider
import java.time.LocalDate
import java.io.IOException
import kotlinx.coroutines.test.runTest
import io.mockk.mockk
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.SQLiteMode

/** Exercises source races, content ownership and transaction rollback against native Room/SQLite. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class SourceContentDatabaseTest {

    private val context = RuntimeEnvironment.getApplication()
    private val databaseName = "source-content-transactions.db"
    private lateinit var database: RunatalDatabase
    private lateinit var repository: QuoteRepositoryImpl
    private val original = QuoteEntity(
        id = 1L, textLatin = "wolf", author = "User", runicElder = "old direct",
        isUserCreated = true, createdAt = 42L
    )
    private val clock = object : TimeProvider {
        override fun getCurrentDate(): LocalDate = LocalDate.of(2026, 10, 9)
        override fun getCurrentDayOfYear(): Int = getCurrentDate().dayOfYear
    }

    @Before
    fun setUp() {
        context.deleteDatabase(databaseName)
        database = Room.databaseBuilder(context, RunatalDatabase::class.java, databaseName)
            .setDriver(AndroidSQLiteDriver())
            .build()
        repository = QuoteRepositoryImpl(database.quoteDao(), clock, mockk(), database.archivedQuoteDao())
    }

    @After
    fun tearDown() {
        database.close()
        context.deleteDatabase(databaseName)
    }

    @Test
    fun `readers reject obsolete persisted source and a stale caller snapshot`() = runTest {
        database.quoteDao().insert(original)
        val dao = database.translationRecordDao()
        dao.insert(record(source = "older source"))

        assertThat(selected("wolf")).isNull()
        assertThat(selected("older source")).isNull()

        assertThat(dao.insertIfSourceMatches(record())).isTrue()
        assertThat(selected("wolf")?.sourceText).isEqualTo("wolf")
        assertThat(selected("older source")).isNull()
    }

    @Test
    fun `late old-source cache writes cannot overwrite a new-source result or attach to a deleted quote`() = runTest {
        seedUserAndCache()
        val previous = requireNotNull(repository.getQuoteById(1L))
        repository.updateUserQuoteContent(previous.copy(textLatin = "king"), "wolf", "User")
        val dao = database.translationRecordDao()
        assertThat(dao.insertIfSourceMatches(record(source = "king", glyphs = "new output"))).isTrue()

        assertThat(dao.insertIfSourceMatches(record(glyphs = "late old output"))).isFalse()
        assertThat(selected("king")?.glyphOutput).isEqualTo("new output")
        assertThat(cacheCount()).isEqualTo(1)

        val deletion = repository.deleteUserQuote(1L)
        assertThat(dao.insertIfSourceMatches(record(source = "king"))).isFalse()
        assertThat(cacheCount()).isEqualTo(1)
        assertThat(selected("king")).isNull()
        assertThat(database.archivedQuoteDao().getRetainedById(1L)?.lifecycleState).isEqualTo("TRASH")
        repository.undoLifecycleChange(deletion)
        assertThat(selected("king")?.glyphOutput).isEqualTo("new output")
        repository.deleteUserQuote(1L)
        database.archivedQuoteDao().emptyTrash(100L)
        assertThat(cacheCount()).isEqualTo(0)
    }

    @Test
    fun `returning to the exact source permits a still-valid same-engine result`() = runTest {
        seedUserAndCache()
        val wolf = requireNotNull(repository.getQuoteById(1L))
        val king = repository.updateUserQuoteContent(wolf.copy(textLatin = "king"), "wolf", "User")
        repository.updateUserQuoteContent(king.copy(textLatin = "wolf"), "king", "User")

        assertThat(database.translationRecordDao().insertIfSourceMatches(record())).isTrue()
        assertThat(selected("wolf")?.glyphOutput).isEqualTo("old output")
    }

    @Test
    fun `content writes preserve concurrently changed favorite and creation identity metadata`() = runTest {
        seedUserAndCache()
        val loaded = requireNotNull(repository.getQuoteById(1L))
        database.quoteDao().updateFavoriteStatus(1L, true)

        val saved = repository.updateUserQuoteContent(
            loaded.copy(textLatin = "king", author = "Updated author", runicElder = "new direct", createdAt = 999L),
            "wolf", "User"
        )

        assertThat(saved.isFavorite).isTrue()
        assertThat(saved.createdAt).isEqualTo(42L)
        assertThat(saved.id).isEqualTo(1L)
        assertThat(saved.textLatin).isEqualTo("king")
        assertThat(saved.author).isEqualTo("Updated author")
        assertThat(saved.runicElder).isEqualTo("new direct")
        assertThat(saved.isUserCreated).isTrue()
        assertThat(cacheCount()).isEqualTo(0)
    }

    @Test
    fun `an author-only edit preserves translations for the unchanged source`() = runTest {
        seedUserAndCache()
        val loaded = requireNotNull(repository.getQuoteById(1L))

        repository.updateUserQuoteContent(loaded.copy(author = "New author"), "wolf", "User")

        assertThat(selected("wolf")?.glyphOutput).isEqualTo("old output")
        assertThat(cacheCount()).isEqualTo(1)
    }

    @Test
    fun `missing stale and canonical quote identities never report a successful edit`() = runTest {
        seedUserAndCache()
        val loaded = requireNotNull(repository.getQuoteById(1L))
        val failures = listOf(
            runCatching { repository.updateUserQuoteContent(loaded.copy(id = 99L), "wolf", "User") },
            runCatching { repository.updateUserQuoteContent(loaded, "older source", "User") },
            runCatching { repository.updateUserQuoteContent(loaded, "wolf", "Former author") }
        )
        failures.forEach { assertThat(it.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java) }
        database.quoteDao().insert(original.copy(id = 2L, isUserCreated = false, canonicalKey = "canonical"))
        val canonical = requireNotNull(repository.getQuoteById(2L))
        val denied = runCatching {
            repository.updateUserQuoteContent(canonical.copy(textLatin = "replacement"), "wolf", "User")
        }
        assertThat(denied.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
        assertThat(database.quoteDao().getById(1L)).isEqualTo(original)
        assertThat(database.quoteDao().getById(2L)?.isUserCreated).isFalse()
        assertThat(cacheCount()).isEqualTo(1)
    }

    @Test
    fun `cache invalidation failure rolls back the content update and retains the original aggregate`() = runTest {
        seedUserAndCache()
        val loaded = requireNotNull(repository.getQuoteById(1L))
        withNativeConnection { connection ->
            connection.execSQL(
                "CREATE TRIGGER fail_invalidation BEFORE DELETE ON translation_records " +
                    "BEGIN SELECT RAISE(ABORT, 'forced invalidation failure'); END"
            )
        }

        val failure = runCatching {
            repository.updateUserQuoteContent(loaded.copy(textLatin = "king", author = "Changed"), "wolf", "User")
        }.exceptionOrNull()

        assertThat(failure).isInstanceOf(IOException::class.java)
        assertThat(failure?.cause).isInstanceOf(SQLiteException::class.java)
        assertThat(database.quoteDao().getById(1L)).isEqualTo(original)
        assertThat(selected("wolf")?.glyphOutput).isEqualTo("old output")
        assertThat(cacheCount()).isEqualTo(1)
    }

    private suspend fun seedUserAndCache() {
        database.quoteDao().insert(original)
        assertThat(database.translationRecordDao().insertIfSourceMatches(record())).isTrue()
    }

    private suspend fun selected(source: String) = database.translationRecordDao().getBySelection(
        1L, "ELDER_FUTHARK", "STRICT", "", "engine-current", "dataset-current", source
    )

    private fun cacheCount(): Int = withNativeConnection { connection ->
        connection.prepare("SELECT COUNT(*) FROM translation_records").use { statement ->
            check(statement.step())
            statement.getLong(0).toInt()
        }
    }

    private fun <T> withNativeConnection(block: (androidx.sqlite.SQLiteConnection) -> T): T =
        AndroidSQLiteDriver().open(context.getDatabasePath(databaseName).absolutePath).use(block)

    private fun record(source: String = "wolf", glyphs: String = "old output") = TranslationRecordEntity(
        quoteId = 1L, sourceText = source, script = "ELDER_FUTHARK", fidelity = "STRICT",
        derivationKind = "TOKEN_COMPOSED", normalizedForm = "wulfaz", diplomaticForm = "wulfaz",
        glyphOutput = glyphs, historicalStage = "PROTO_NORSE", resolutionStatus = "RECONSTRUCTED", confidence = 0.9f,
        notesJson = "[]", unresolvedTokensJson = "[]", provenanceJson = "[]", tokenBreakdownJson = "[]",
        engineVersion = "engine-current", datasetVersion = "dataset-current", createdAt = 1L, updatedAt = 1L
    )
}
