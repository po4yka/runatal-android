package com.po4yka.runatal.data.local

import android.app.Application
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import com.google.common.truth.Truth.assertThat
import com.po4yka.runatal.data.local.entity.QuoteEntity
import com.po4yka.runatal.data.local.entity.TranslationRecordEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.SQLiteMode

/** Exercises version selection using generated Room queries and native SQLite. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class TranslationCacheDatabaseTest {

    private lateinit var database: RunatalDatabase

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
    fun `latest available query ignores newer successes from obsolete engine and dataset versions`() = runTest {
        database.quoteDao().insert(QuoteEntity(id = 1L, textLatin = "wolf", author = "Test"))
        val dao = database.translationRecordDao()
        dao.insert(record("engine-current", "dataset-current", 10L))
        dao.insert(record("engine-obsolete", "dataset-current", 20L))
        dao.insert(record("engine-current", "dataset-obsolete", 30L))

        val latest = dao.getLatestAvailableForScript(
            quoteId = 1L,
            script = "YOUNGER_FUTHARK",
            unavailableStatus = "UNAVAILABLE",
            engineVersion = "engine-current",
            datasetVersion = "dataset-current"
        )

        assertThat(latest?.updatedAt).isEqualTo(10L)
        assertThat(latest?.engineVersion).isEqualTo("engine-current")
        assertThat(latest?.datasetVersion).isEqualTo("dataset-current")
    }

    @Test
    fun `obsolete cached success cannot be selected when no current result exists`() = runTest {
        database.quoteDao().insert(QuoteEntity(id = 1L, textLatin = "123", author = "Test"))
        val dao = database.translationRecordDao()
        dao.insert(record("engine-obsolete", "dataset-current", 20L).copy(glyphOutput = ""))

        val latest = dao.getLatestAvailableForScript(
            quoteId = 1L,
            script = "YOUNGER_FUTHARK",
            unavailableStatus = "UNAVAILABLE",
            engineVersion = "engine-current",
            datasetVersion = "dataset-current"
        )

        assertThat(latest).isNull()
    }

    private fun record(engine: String, dataset: String, timestamp: Long) = TranslationRecordEntity(
        quoteId = 1L,
        sourceText = "wolf",
        script = "YOUNGER_FUTHARK",
        fidelity = "STRICT",
        derivationKind = "TOKEN_COMPOSED",
        normalizedForm = "úlfr",
        diplomaticForm = "ulfr",
        glyphOutput = "ᚢᛚᚠᚱ",
        historicalStage = "OLD_NORSE",
        variant = "LONG_BRANCH",
        resolutionStatus = "RECONSTRUCTED",
        confidence = 0.9f,
        notesJson = "[]",
        unresolvedTokensJson = "[]",
        provenanceJson = "[]",
        tokenBreakdownJson = "[]",
        engineVersion = engine,
        datasetVersion = dataset,
        createdAt = timestamp,
        updatedAt = timestamp
    )
}
