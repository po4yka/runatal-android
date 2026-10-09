package com.po4yka.runatal.data.local

import android.app.Application
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import com.google.common.truth.Truth.assertThat
import com.po4yka.runatal.data.local.entity.QuoteReadEntity
import com.po4yka.runatal.data.local.entity.QuoteEntity
import com.po4yka.runatal.data.repository.QuoteRepositoryImpl
import com.po4yka.runatal.data.repository.ReadingHistoryRepositoryImpl
import com.po4yka.runatal.domain.model.RunicScript
import com.po4yka.runatal.util.TimeProvider
import io.mockk.mockk
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.SQLiteMode

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class ReadingHistoryDatabaseTest {
    private val context = RuntimeEnvironment.getApplication()
    private val name = "reading-history.db"
    private var database: RunatalDatabase? = null

    @After
    fun tearDown() {
        database?.close()
        context.deleteDatabase(name)
    }

    @Test
    fun `reads persist once per quote script and day and deleted content is not copied into history`() = runTest {
        val day = LocalDate.of(2026, 10, 9)
        val time = object : TimeProvider {
            override fun getCurrentDate() = day
            override fun getCurrentDayOfYear() = day.dayOfYear
        }
        val initial = open()
        val quote = QuoteEntity(id = 1, textLatin = "Actual quote", author = "Reader", isUserCreated = true)
        initial.quoteDao().insert(quote)
        val repository = ReadingHistoryRepositoryImpl(
            initial.readingHistoryDao(), QuoteRepositoryImpl(initial.quoteDao(), time, mockk(), initial.archivedQuoteDao()), time
        )
        repository.recordRead(1, RunicScript.ELDER_FUTHARK)
        repository.recordRead(1, RunicScript.ELDER_FUTHARK)
        repository.recordRead(1, RunicScript.CIRTH)
        assertThat(repository.readings().first()).hasSize(2)
        assertThat(repository.stats().first().streakDays).isEqualTo(1)
        initial.close()

        val reopened = open()
        assertThat(reopened.readingHistoryDao().readings().first()).hasSize(2)
        assertThat(reopened.readingHistoryDao().days().first()).containsExactly(day.toEpochDay())
        reopened.archivedQuoteDao().updateState(1L, "ACTIVE", "TRASH", "delete", 1L)
        reopened.archivedQuoteDao().emptyTrash(2L)
        assertThat(reopened.readingHistoryDao().readings().first()).isEmpty()
        assertThat(reopened.readingHistoryDao().days().first()).hasSize(1)
        reopened.readingHistoryDao().record(QuoteReadEntity(
            quoteId = 999, epochDay = day.plusDays(1).toEpochDay(), script = "ELDER_FUTHARK", readAt = 1
        ))
        assertThat(reopened.readingHistoryDao().days().first()).hasSize(1)
    }

    @Test
    fun `a physical read write failure is reported as IO without recording a partial activity day`() = runTest {
        val storage = open()
        storage.quoteDao().insert(QuoteEntity(1L, "Read this", "Author", isUserCreated = true))
        val day = LocalDate.of(2026, 10, 9)
        val time = object : TimeProvider {
            override fun getCurrentDate() = day
            override fun getCurrentDayOfYear() = day.dayOfYear
        }
        val repository = ReadingHistoryRepositoryImpl(storage.readingHistoryDao(),
            QuoteRepositoryImpl(storage.quoteDao(), time, mockk(), storage.archivedQuoteDao()), time)
        storage.useWriterConnection { connection ->
            connection.usePrepared("CREATE TRIGGER fail_day BEFORE INSERT ON reading_days " +
                "BEGIN SELECT RAISE(ABORT, 'activity write failed'); END") { it.step() }
        }
        val failure = runCatching { repository.recordRead(1L, RunicScript.ELDER_FUTHARK) }.exceptionOrNull()
        assertThat(failure).isInstanceOf(java.io.IOException::class.java)
        assertThat(failure?.cause).isInstanceOf(android.database.sqlite.SQLiteException::class.java)
        assertThat(storage.readingHistoryDao().readings().first()).isEmpty()
        assertThat(storage.readingHistoryDao().days().first()).isEmpty()
        storage.useWriterConnection { connection ->
            connection.usePrepared("DROP TRIGGER fail_day") { it.step() }
        }
        repository.recordRead(1L, RunicScript.ELDER_FUTHARK)
        assertThat(storage.readingHistoryDao().readings().first()).hasSize(1)
        assertThat(repository.stats().first().totalDays).isEqualTo(1)
    }

    private fun open(): RunatalDatabase = Room.databaseBuilder(context, RunatalDatabase::class.java, name)
        .setDriver(AndroidSQLiteDriver()).build().also { database = it }
}
