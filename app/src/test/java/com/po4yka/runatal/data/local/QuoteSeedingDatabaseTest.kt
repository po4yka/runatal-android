package com.po4yka.runatal.data.local

import android.app.Application
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import com.google.common.truth.Truth.assertThat
import com.po4yka.runatal.data.local.entity.QuoteEntity
import com.po4yka.runatal.data.repository.QuoteRepositoryImpl
import com.po4yka.runatal.data.seed.QuoteSeedData
import com.po4yka.runatal.util.TimeProvider
import java.time.LocalDate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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

/** Exercises canonical seeding against Room and native SQLite, including competing writers. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class QuoteSeedingDatabaseTest {

    private lateinit var database: RunatalDatabase
    private val timeProvider = object : TimeProvider {
        override fun getCurrentDate(): LocalDate = LocalDate.of(2026, 10, 9)
        override fun getCurrentDayOfYear(): Int = getCurrentDate().dayOfYear
    }
    private val userQuote = QuoteEntity(
        id = 1L,
        textLatin = "My own quote at the old seed identity",
        author = "User",
        runicElder = "User's stored rendering",
        isUserCreated = true,
        isFavorite = true,
        createdAt = 42L
    )

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            RunatalDatabase::class.java
        ).setDriver(AndroidSQLiteDriver()).build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `seeding preserves a user quote at legacy id and works across repository instances`() = runTest {
        val dao = database.quoteDao()
        dao.insert(userQuote)

        repeat(3) {
            QuoteRepositoryImpl(dao, timeProvider, mockk(), database.archivedQuoteDao()).seedIfNeeded()
        }

        assertThat(dao.getById(userQuote.id)).isEqualTo(userQuote)
        assertThat(dao.getAll()).hasSize(6)
        assertCanonicalIdentities()
    }

    @Test
    fun `seeding repairs missing canonical rows without replacing existing ids or favorites`() = runTest {
        val dao = database.quoteDao()
        val existingCanonical = QuoteSeedData.getCanonicalQuotes().first().copy(id = 99L, isFavorite = true)
        dao.insert(existingCanonical)
        dao.insert(userQuote)

        QuoteRepositoryImpl(dao, timeProvider, mockk(), database.archivedQuoteDao()).seedIfNeeded()

        assertThat(dao.getById(existingCanonical.id)).isEqualTo(existingCanonical)
        assertThat(dao.getById(userQuote.id)).isEqualTo(userQuote)
        assertThat(dao.getAll()).hasSize(6)
        assertCanonicalIdentities()
    }

    @Test
    fun `concurrent seed and user inserts preserve each user and exactly one canonical set`() = runTest {
        val dao = database.quoteDao()
        dao.insert(userQuote)
        val start = CompletableDeferred<Unit>()
        val seedJobs = (1..8).map {
            async(Dispatchers.Default) {
                start.await()
                QuoteRepositoryImpl(dao, timeProvider, mockk(), database.archivedQuoteDao()).seedIfNeeded()
            }
        }
        val secondUser = userQuote.copy(id = 0L, textLatin = "A concurrent user quote")
        val userInsert = async(Dispatchers.Default) {
            start.await()
            dao.insert(secondUser)
        }
        start.complete(Unit)
        seedJobs.awaitAll()
        val insertedId = userInsert.await()

        assertThat(dao.getById(userQuote.id)).isEqualTo(userQuote)
        assertThat(dao.getById(insertedId)).isEqualTo(secondUser.copy(id = insertedId))
        assertThat(dao.getAll()).hasSize(7)
        assertCanonicalIdentities()
    }

    @Test
    fun `migration recognizes only exact legacy canonical rows and preserves favorites`() = runTest {
        AndroidSQLiteDriver().open(":memory:").use { connection ->
            connection.execSQL(
                "CREATE TABLE quotes (id INTEGER PRIMARY KEY, textLatin TEXT NOT NULL, " +
                    "author TEXT NOT NULL, isUserCreated INTEGER NOT NULL, isFavorite INTEGER NOT NULL)"
            )
            val legacyQuotes = QuoteSeedData.getLegacyQuotes()
            legacyQuotes.take(3).forEachIndexed { index, quote ->
                connection.prepare("INSERT INTO quotes VALUES (?, ?, ?, ?, 1)").use { statement ->
                    statement.bindLong(1, quote.id)
                    statement.bindText(2, if (index == 2) "Unrelated non-user quote" else quote.textLatin)
                    statement.bindText(3, quote.author)
                    statement.bindLong(4, if (index == 0) 1L else 0L)
                    statement.step()
                }
            }

            RunatalDatabase.MIGRATION_7_8.migrate(connection)

            connection.prepare("SELECT id, canonicalKey, isFavorite FROM quotes ORDER BY id").use { statement ->
                assertThat(statement.step()).isTrue()
                assertThat(statement.isNull(1)).isTrue()
                assertThat(statement.getLong(2)).isEqualTo(1L)
                assertThat(statement.step()).isTrue()
                assertThat(statement.getText(1)).isEqualTo(legacyQuotes[1].canonicalKey)
                assertThat(statement.getLong(2)).isEqualTo(1L)
                assertThat(statement.step()).isTrue()
                assertThat(statement.isNull(1)).isTrue()
            }
        }
    }

    private suspend fun assertCanonicalIdentities() {
        val canonicalKeys = database.quoteDao().getAll().mapNotNull { it.canonicalKey }
        assertThat(canonicalKeys).containsExactlyElementsIn(
            QuoteSeedData.getCanonicalQuotes().map { it.canonicalKey }
        )
    }
}
