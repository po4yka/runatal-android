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

    private fun open(): RunatalDatabase = Room.databaseBuilder(context, RunatalDatabase::class.java, name)
        .setDriver(AndroidSQLiteDriver()).build().also { database = it }
}
