package com.po4yka.runatal.data.local

import android.app.Application
import android.database.sqlite.SQLiteException
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import com.google.common.truth.Truth.assertThat
import com.po4yka.runatal.data.local.entity.QuoteEntity
import com.po4yka.runatal.data.local.entity.TranslationRecordEntity
import com.po4yka.runatal.data.repository.ArchiveRepositoryImpl
import com.po4yka.runatal.data.repository.QuoteRepositoryImpl
import com.po4yka.runatal.domain.model.Quote
import com.po4yka.runatal.domain.model.QuoteLifecycleState
import com.po4yka.runatal.util.TimeProvider
import java.io.IOException
import java.time.LocalDate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.SQLiteMode

/** Exercises persisted lifecycle commands and all metadata retention with native Room/SQLite. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class QuoteLifecycleDatabaseTest {
    private val context = RuntimeEnvironment.getApplication()
    private val databaseName = "quote-lifecycle.db"
    private lateinit var database: RunatalDatabase
    private lateinit var quotes: QuoteRepositoryImpl
    private lateinit var archive: ArchiveRepositoryImpl
    private val clock = object : TimeProvider {
        override fun getCurrentDate(): LocalDate = LocalDate.of(2026, 10, 9)
        override fun getCurrentDayOfYear(): Int = getCurrentDate().dayOfYear
    }
    private val original = QuoteEntity(
        id = 1L, textLatin = "Stored source", author = "User", runicElder = "Manual elder",
        runicYounger = "Manual younger", runicCirth = "Manual Cirth \uE088", isUserCreated = true,
        isFavorite = true, createdAt = 42L, renderingMode = "TRANSLATE",
        renderingFidelity = "READABLE", renderingYoungerVariant = "SHORT_TWIG"
    )

    @Before
    fun setUp() {
        context.deleteDatabase(databaseName)
        openStorage()
    }

    @After
    fun tearDown() {
        database.close()
        context.deleteDatabase(databaseName)
    }

    @Test
    fun `archive hide and trash retain the aggregate and exact undo restores visibility`() = runTest {
        seedAggregate()
        val records = recordSnapshot()
        listOf(QuoteLifecycleState.ARCHIVED, QuoteLifecycleState.HIDDEN, QuoteLifecycleState.TRASH).forEach { state ->
            val change = when (state) {
                QuoteLifecycleState.ARCHIVED -> quotes.archiveQuote(1L)
                QuoteLifecycleState.HIDDEN -> quotes.hideQuote(1L)
                QuoteLifecycleState.TRASH -> quotes.deleteUserQuote(1L)
                QuoteLifecycleState.ACTIVE -> error("Only inactive states in this test")
            }
            assertThat(quotes.getQuoteById(1L)).isNull()
            assertThat(quotes.getAllQuotes().map { it.id }).doesNotContain(1L)
            assertThat(quotes.getFavorites().map { it.id }).doesNotContain(1L)
            val retained = requireNotNull(database.archivedQuoteDao().getRetainedById(1L))
            assertThat(retained.lifecycleState).isEqualTo(state.name)
            assertThat(retained.lifecycleMutationId).isEqualTo(change.mutationId)
            assertThat(withoutLifecycle(retained)).isEqualTo(original)
            assertThat(recordSnapshot()).isEqualTo(records)
            assertThat(archive.getRetainedQuotesFlow().first().single().lifecycleState).isEqualTo(state)
            assertThat(selectedRecord()).isNull()
            quotes.undoLifecycleChange(change)
            assertThat(database.quoteDao().getById(1L)).isEqualTo(original)
            assertThat(selectedRecord()?.glyphOutput).isEqualTo("record ELDER_FUTHARK")
            assertThat(recordSnapshot()).isEqualTo(records)
        }
    }

    @Test
    fun `retained state survives reopen and restore undo preserves original trash timestamp and metadata`() = runTest {
        seedAggregate()
        quotes.deleteUserQuote(1L)
        val before = requireNotNull(database.archivedQuoteDao().getRetainedById(1L))
        val records = recordSnapshot()
        database.close()
        openStorage()
        assertThat(database.archivedQuoteDao().getRetainedById(1L)).isEqualTo(before)
        val trashed = archive.getRetainedQuotesFlow().first().single()
        val restore = archive.restoreQuotes(listOf(trashed))
        assertThat(quotes.getQuoteById(1L)).isNotNull()
        archive.undoChanges(restore)
        assertThat(database.archivedQuoteDao().getRetainedById(1L))
            .isEqualTo(before.copy(lifecycleMutationId = null))
        assertThat(recordSnapshot()).isEqualTo(records)
    }

    @Test
    fun `stale undo cannot replace a newer lifecycle action and a receipt is not replayable`() = runTest {
        seedAggregate()
        val deletion = quotes.deleteUserQuote(1L)
        archive.restoreQuotes(archive.getRetainedQuotesFlow().first())
        val restored = requireNotNull(database.quoteDao().getById(1L))
        val hidden = quotes.hideQuote(1L)
        val failure = runCatching { quotes.undoLifecycleChange(deletion) }.exceptionOrNull()
        assertThat(failure).isInstanceOf(IllegalStateException::class.java)
        assertThat(database.archivedQuoteDao().getRetainedById(1L)?.lifecycleState).isEqualTo("HIDDEN")
        quotes.undoLifecycleChange(hidden)
        val replay = runCatching { quotes.undoLifecycleChange(hidden) }.exceptionOrNull()
        assertThat(replay).isInstanceOf(IllegalStateException::class.java)
        assertThat(database.quoteDao().getById(1L)).isEqualTo(restored.copy(lifecycleMutationId = null))
    }

    @Test
    fun `competing archive and hide commands commit exactly one guarded state transition`() = runTest {
        seedAggregate()
        val start = CompletableDeferred<Unit>()
        val outcomes = listOf(
            async(Dispatchers.IO) {
                start.await()
                runCatching { quotes.archiveQuote(1L) }
            },
            async(Dispatchers.IO) {
                start.await()
                runCatching { quotes.hideQuote(1L) }
            }
        )
        start.complete(Unit)
        val results = outcomes.awaitAll()
        assertThat(results.count { it.isSuccess }).isEqualTo(1)
        assertThat(results.single { it.isFailure }.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
        val receipt = results.single { it.isSuccess }.getOrThrow()
        assertThat(database.archivedQuoteDao().getRetainedById(1L)?.lifecycleMutationId).isEqualTo(receipt.mutationId)
        assertThat(recordSnapshot()).hasSize(3)
    }

    @Test
    fun `bulk restore is atomic on a physical second-row failure and retry preserves every record`() = runTest {
        seedAggregate()
        database.quoteDao().insert(original.copy(id = 2L))
        quotes.archiveQuote(1L)
        quotes.hideQuote(2L)
        val before = archive.getRetainedQuotesFlow().first().sortedBy { it.id }
        val snapshots = before.map { requireNotNull(database.archivedQuoteDao().getRetainedById(it.id)) }
        val records = recordSnapshot()
        executeSql("CREATE TRIGGER fail_restore BEFORE UPDATE OF lifecycleState ON quotes " +
            "WHEN NEW.lifecycleState = 'ACTIVE' AND OLD.id = 2 BEGIN SELECT RAISE(ABORT, 'restore failed'); END")
        val failure = runCatching { archive.restoreQuotes(before) }.exceptionOrNull()
        assertThat(failure).isInstanceOf(IOException::class.java)
        assertThat(failure?.cause).isInstanceOf(SQLiteException::class.java)
        assertThat(before.map { requireNotNull(database.archivedQuoteDao().getRetainedById(it.id)) })
            .containsExactlyElementsIn(snapshots)
        assertThat(quotes.getAllQuotes()).isEmpty()
        assertThat(recordSnapshot()).isEqualTo(records)
        executeSql("DROP TRIGGER fail_restore")
        val changes = archive.restoreQuotes(before)
        assertThat(changes).hasSize(2)
        assertThat(quotes.getAllQuotes().map { it.id }).containsExactly(1L, 2L)
        assertThat(recordSnapshot()).isEqualTo(records)
    }

    @Test
    fun `bulk undo is atomic when one receipt became stale`() = runTest {
        seedAggregate()
        database.quoteDao().insert(original.copy(id = 2L))
        quotes.archiveQuote(1L)
        quotes.hideQuote(2L)
        val restored = archive.restoreQuotes(archive.getRetainedQuotesFlow().first().sortedBy { it.id })
        quotes.hideQuote(2L)
        val failure = runCatching { archive.undoChanges(restored) }.exceptionOrNull()
        assertThat(failure).isInstanceOf(IllegalStateException::class.java)
        assertThat(database.quoteDao().getById(1L)).isNotNull()
        assertThat(database.archivedQuoteDao().getRetainedById(2L)?.lifecycleState).isEqualTo("HIDDEN")
    }

    @Test
    fun `purge rollback has no tombstone side effect and successful purge cascades only trash metadata`() = runTest {
        seedAggregate()
        quotes.seedIfNeeded()
        val canonical = database.quoteDao().getAll().first { it.canonicalKey != null }
        val keep = database.quoteDao().getAll().first { it.canonicalKey != null && it.id != canonical.id }
        quotes.hideQuote(keep.id)
        quotes.deleteUserQuote(1L)
        database.archivedQuoteDao().transition(canonical.id, QuoteLifecycleState.ACTIVE, QuoteLifecycleState.TRASH,
            "canonical-trash", 100L)
        executeSql("CREATE TRIGGER fail_purge BEFORE DELETE ON quotes WHEN OLD.id = 1 " +
            "BEGIN SELECT RAISE(ABORT, 'purge failed'); END")
        val failure = runCatching { archive.emptyTrash() }.exceptionOrNull()
        assertThat(failure).isInstanceOf(IOException::class.java)
        assertThat(count("canonical_quote_tombstones")).isEqualTo(0)
        assertThat(recordSnapshot()).hasSize(3)
        assertThat(database.archivedQuoteDao().getRetainedById(1L)).isNotNull()
        executeSql("DROP TRIGGER fail_purge")
        archive.emptyTrash()
        assertThat(database.archivedQuoteDao().getRetainedById(1L)).isNull()
        assertThat(database.archivedQuoteDao().getRetainedById(canonical.id)).isNull()
        assertThat(database.archivedQuoteDao().getRetainedById(keep.id)?.lifecycleState).isEqualTo("HIDDEN")
        assertThat(recordSnapshot()).isEmpty()
        assertThat(count("canonical_quote_tombstones")).isEqualTo(1)
        quotes.seedIfNeeded()
        assertThat(database.quoteDao().getCanonicalKeys()).contains(canonical.canonicalKey)
        assertThat(database.quoteDao().getAll().map { it.canonicalKey }).doesNotContain(canonical.canonicalKey)
        val manual = quotes.saveUserQuote(Quote(
            0L, canonical.textLatin, canonical.author, null, null, null, isUserCreated = true
        ))
        assertThat(quotes.getQuoteById(manual)?.textLatin).isEqualTo(canonical.textLatin)
    }

    @Test
    fun `a purged mutation cannot resurrect a quote and ordinary user deletion rejects canonical rows`() = runTest {
        seedAggregate()
        val deletion = quotes.deleteUserQuote(1L)
        archive.emptyTrash()
        val failure = runCatching { quotes.undoLifecycleChange(deletion) }.exceptionOrNull()
        assertThat(failure).isInstanceOf(IllegalStateException::class.java)
        assertThat(database.archivedQuoteDao().getRetainedById(1L)).isNull()
        quotes.seedIfNeeded()
        val canonical = database.quoteDao().getAll().first { !it.isUserCreated }
        val denied = runCatching { quotes.deleteUserQuote(canonical.id) }.exceptionOrNull()
        assertThat(denied).isInstanceOf(IllegalStateException::class.java)
        assertThat(database.quoteDao().getById(canonical.id)).isEqualTo(canonical)
    }

    private fun openStorage() {
        database = Room.databaseBuilder(context, RunatalDatabase::class.java, databaseName)
            .setDriver(AndroidSQLiteDriver()).build()
        quotes = QuoteRepositoryImpl(database.quoteDao(), clock, io.mockk.mockk(), database.archivedQuoteDao())
        archive = ArchiveRepositoryImpl(database.archivedQuoteDao())
    }

    private suspend fun seedAggregate() {
        database.quoteDao().insert(original)
        listOf("ELDER_FUTHARK", "YOUNGER_FUTHARK", "CIRTH").forEachIndexed { index, script ->
            database.translationRecordDao().insert(TranslationRecordEntity(
                id = 100L + index, quoteId = 1L, sourceText = original.textLatin, script = script, fidelity = "STRICT",
                derivationKind = "TOKEN_COMPOSED", normalizedForm = "normalized $script", diplomaticForm = "diplomatic",
                glyphOutput = "record $script", historicalStage = when (script) {
                    "ELDER_FUTHARK" -> "PROTO_NORSE"
                    "YOUNGER_FUTHARK" -> "OLD_NORSE"
                    else -> "EREBOR_ENGLISH"
                },
                variant = if (script == "YOUNGER_FUTHARK") "SHORT_TWIG" else "", resolutionStatus = "RECONSTRUCTED",
                confidence = 0.7f, notesJson = "[\"retained note\"]", unresolvedTokensJson = "[]",
                provenanceJson = "[]", tokenBreakdownJson = "[]", engineVersion = "manual-engine",
                datasetVersion = "manual-dataset", isBackfilled = false, createdAt = 12L, updatedAt = 13L
            ))
        }
    }

    private suspend fun selectedRecord() = database.translationRecordDao().getBySelection(
        1L, "ELDER_FUTHARK", "STRICT", "", "manual-engine", "manual-dataset", original.textLatin
    )

    private fun withoutLifecycle(quote: QuoteEntity) = quote.copy(
        lifecycleState = "ACTIVE", lifecycleChangedAt = 0L, lifecycleMutationId = null
    )

    private fun recordSnapshot(): List<List<String>> =
        AndroidSQLiteDriver().open(context.getDatabasePath(databaseName).absolutePath).use { connection ->
            connection.prepare("SELECT * FROM translation_records ORDER BY id").use { statement ->
                val rows = mutableListOf<List<String>>()
                while (statement.step()) rows.add((0 until statement.getColumnCount()).map { column ->
                    if (statement.isNull(column)) "NULL" else statement.getText(column)
                })
                rows
            }
        }

    private fun count(table: String) =
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
