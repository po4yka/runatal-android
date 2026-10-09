package com.po4yka.runatal.data.local

import android.app.Application
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import com.google.common.truth.Truth.assertThat
import com.po4yka.runatal.data.local.dao.QuoteDao
import com.po4yka.runatal.data.local.entity.QuoteEntity
import com.po4yka.runatal.data.preferences.UserPreferencesManager
import com.po4yka.runatal.data.repository.QuoteRepositoryImpl
import com.po4yka.runatal.domain.model.Quote
import com.po4yka.runatal.util.TimeProvider
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.SQLiteMode

/** Exercises one persisted daily identity with production repository, native Room and real file-backed DataStore. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class DailyQuoteIdentityDatabaseTest {
    private val context = RuntimeEnvironment.getApplication()
    private val databaseName = "daily-quote-identity.db"
    private val preferencesFile = context.preferencesDataStoreFile("daily_quote_identity")
    private lateinit var database: RunatalDatabase
    private lateinit var preferencesJob: Job
    private lateinit var preferences: UserPreferencesManager
    private lateinit var repository: QuoteRepositoryImpl
    private var date = LocalDate.of(2026, 10, 9)
    private val clock = object : TimeProvider {
        override fun getCurrentDate(): LocalDate = date
        override fun getCurrentDayOfYear(): Int = date.dayOfYear
    }
    private val original = QuoteEntity(
        id = 100L, textLatin = "My current quote", author = "User", isUserCreated = true, createdAt = 42L
    )

    @Before
    fun setUp() {
        context.deleteDatabase(databaseName)
        preferencesFile.delete()
        openStorage()
    }

    @After
    fun tearDown() = runBlocking {
        preferencesJob.cancelAndJoin()
        database.close()
        context.deleteDatabase(databaseName)
        preferencesFile.delete()
        Unit
    }

    @Test
    fun `same day identity survives inserts removals favorites and selected content edits`() = runTest {
        seedUserSelection()
        assertThat(repository.quoteOfTheDay()?.id).isEqualTo(100L)
        database.quoteDao().insert(original.copy(id = 200L, textLatin = "New library quote", createdAt = 999L))
        database.quoteDao().updateFavoriteStatus(100L, true)
        database.quoteDao().updateUserContent(original.copy(textLatin = "Updated content"), original.textLatin, "User")
        database.quoteDao().deleteUserQuote(200L)

        val selected = requireNotNull(repository.quoteOfTheDay())
        assertThat(selected.id).isEqualTo(100L)
        assertThat(selected.textLatin).isEqualTo("Updated content")
        assertThat(selected.isFavorite).isTrue()
        assertThat(selected.createdAt).isEqualTo(42L)
        assertThat(preferences.userPreferencesFlow.first().lastDailyQuoteId).isEqualTo(100L)
    }

    @Test
    fun `same day identity and unrelated preferences survive a full database and DataStore reopen`() = runTest {
        seedUserSelection()
        preferences.updateThemeMode("dark")
        val before = requireNotNull(repository.quoteOfTheDay())
        preferencesJob.cancelAndJoin()
        database.close()
        openStorage()
        database.quoteDao().insert(original.copy(id = 200L, createdAt = 999L))

        assertThat(repository.quoteOfTheDay()?.id).isEqualTo(before.id)
        val restored = preferences.userPreferencesFlow.first()
        assertThat(restored.lastQuoteDate).isEqualTo(date.toEpochDay())
        assertThat(restored.lastDailyQuoteId).isEqualTo(before.id)
        assertThat(restored.themeMode).isEqualTo("dark")
    }

    @Test
    fun `deleting selected identity replaces it once and later restore retains the replacement`() = runTest {
        seedUserSelection()
        repository.quoteOfTheDay()
        repository.deleteUserQuote(100L)
        val replacement = requireNotNull(repository.quoteOfTheDay())
        assertThat(replacement.id).isNotEqualTo(100L)
        assertThat(database.quoteDao().getById(replacement.id)).isNotNull()
        repository.restoreUserQuote(
            Quote(
                id = original.id, textLatin = original.textLatin, author = original.author,
                runicElder = null, runicYounger = null, runicCirth = null, isUserCreated = true
            )
        )
        assertThat(repository.quoteOfTheDay()?.id).isEqualTo(replacement.id)
        assertThat(preferences.userPreferencesFlow.first().lastDailyQuoteId).isEqualTo(replacement.id)
    }

    @Test
    fun `concurrent fresh readers resolve the same persisted identity while seeding and adding quotes`() = runTest {
        database.quoteDao().insert(original)
        val readers = (1..24).map {
            async(Dispatchers.IO) { requireNotNull(repository.quoteOfTheDay()).id }
        }
        val writer = async(Dispatchers.IO) { database.quoteDao().insert(original.copy(id = 200L)) }
        val ids = readers.awaitAll()
        writer.await()
        assertThat(ids.toSet()).hasSize(1)
        val persisted = preferences.userPreferencesFlow.first()
        assertThat(persisted.lastQuoteDate).isEqualTo(date.toEpochDay())
        assertThat(persisted.lastDailyQuoteId).isEqualTo(ids.first())
        assertThat(repository.quoteOfTheDay()?.id).isEqualTo(ids.first())
    }

    @Test
    fun `full dates distinguish years and rotate when another quote is available`() = runTest {
        seedUserSelection()
        val first = requireNotNull(repository.quoteOfTheDay()).id
        date = LocalDate.of(2027, 10, 9)
        val nextYear = requireNotNull(repository.quoteOfTheDay()).id
        assertThat(nextYear).isNotEqualTo(first)
        assertThat(preferences.userPreferencesFlow.first().lastQuoteDate).isEqualTo(date.toEpochDay())
        date = LocalDate.of(2027, 12, 31)
        val yearEnd = requireNotNull(repository.quoteOfTheDay()).id
        date = LocalDate.of(2028, 1, 1)
        val newYear = requireNotNull(repository.quoteOfTheDay()).id
        assertThat(newYear).isNotEqualTo(yearEnd)
        assertThat(repository.quoteOfTheDay()?.id).isEqualTo(newYear)
    }

    @Test
    fun `deletion after persisted selection and before row lookup retries with a current identity`() = runTest {
        seedUserSelection()
        val actual = database.quoteDao()
        var deleted = false
        val racingDao = object : QuoteDao by actual {
            override suspend fun getById(id: Long): QuoteEntity? {
                if (!deleted && id == 100L) {
                    deleted = true
                    actual.deleteUserQuote(id)
                }
                return actual.getById(id)
            }
        }
        val racingRepository = QuoteRepositoryImpl(racingDao, clock, preferences)
        val selected = requireNotNull(racingRepository.quoteOfTheDay())
        assertThat(deleted).isTrue()
        assertThat(selected.id).isNotEqualTo(100L)
        assertThat(actual.getById(selected.id)).isNotNull()
        assertThat(preferences.userPreferencesFlow.first().lastDailyQuoteId).isEqualTo(selected.id)
    }

    private suspend fun seedUserSelection() {
        database.quoteDao().insert(original)
        preferences.selectDailyQuote(date.toEpochDay()) { database.quoteDao().getQuoteIdentities() }
    }

    private fun openStorage() {
        database = Room.databaseBuilder(context, RunatalDatabase::class.java, databaseName)
            .setDriver(AndroidSQLiteDriver()).build()
        preferencesJob = Job()
        val dataStore = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(preferencesJob + Dispatchers.IO), produceFile = { preferencesFile }
        )
        preferences = UserPreferencesManager(dataStore)
        repository = QuoteRepositoryImpl(database.quoteDao(), clock, preferences)
    }
}
