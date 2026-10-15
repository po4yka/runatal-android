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
    fun `exact selection query ignores newer successes from obsolete engine and dataset versions`() = runTest {
        database.quoteDao().insert(QuoteEntity(id = 1L, textLatin = "wolf", author = "Test"))
        val dao = database.translationRecordDao()
        dao.insert(record("engine-current", "dataset-current", 10L))
        dao.insert(record("engine-obsolete", "dataset-current", 20L))
        dao.insert(record("engine-current", "dataset-obsolete", 30L))

        val latest = dao.getBySelection(
            quoteId = 1L,
            script = "YOUNGER_FUTHARK",
            fidelity = "STRICT", variant = "LONG_BRANCH",
            engineVersion = "engine-current",
            datasetVersion = "dataset-current", sourceText = "wolf"
        )

        assertThat(latest?.updatedAt).isEqualTo(10L)
        assertThat(latest?.engineVersion).isEqualTo("engine-current")
        assertThat(latest?.datasetVersion).isEqualTo("dataset-current")
    }

    @Test
    fun `obsolete cached success cannot be selected when no current result exists`() = runTest {
        database.quoteDao().insert(QuoteEntity(id = 1L, textLatin = "123", author = "Test"))
        val dao = database.translationRecordDao()
        dao.insert(record("engine-obsolete", "dataset-current", 20L).copy(sourceText = "123", glyphOutput = ""))

        val latest = dao.getBySelection(
            quoteId = 1L,
            script = "YOUNGER_FUTHARK",
            fidelity = "STRICT", variant = "LONG_BRANCH",
            engineVersion = "engine-current",
            datasetVersion = "dataset-current", sourceText = "123"
        )

        assertThat(latest).isNull()
    }

    @Test
    fun `repeated writes for scripts without variants replace the selected result`() = runTest {
        database.quoteDao().insert(QuoteEntity(id = 1L, textLatin = "wolf", author = "Test"))
        val dao = database.translationRecordDao()
        listOf("ELDER_FUTHARK", "CIRTH").forEach { script ->
            val original = record("engine-current", "dataset-current", 10L)
                .copy(script = script, variant = "", glyphOutput = "first")
            dao.insert(original)
            val replacement = original.copy(glyphOutput = "replacement", updatedAt = 20L)
            val replacementId = dao.insert(replacement)

            val selected = dao.getBySelection(
                quoteId = 1L, script = script, fidelity = "STRICT", variant = "",
                engineVersion = "engine-current", datasetVersion = "dataset-current", sourceText = "wolf"
            )

            assertThat(selected).isEqualTo(replacement.copy(id = replacementId))
        }
    }

    @Test
    fun `distinct variants fidelities engines and datasets remain independent selections`() = runTest {
        database.quoteDao().insert(QuoteEntity(id = 1L, textLatin = "wolf", author = "Test"))
        val dao = database.translationRecordDao()
        val original = record("engine-current", "dataset-current", 10L)
        val selections = listOf(
            original,
            original.copy(variant = "SHORT_TWIG", glyphOutput = "short twig"),
            original.copy(fidelity = "READABLE", glyphOutput = "readable"),
            original.copy(engineVersion = "engine-old", glyphOutput = "old engine"),
            original.copy(datasetVersion = "dataset-old", glyphOutput = "old dataset")
        )
        val stored = selections.map { it.copy(id = dao.insert(it)) }

        stored.forEach { expected ->
            val selected = dao.getBySelection(
                quoteId = expected.quoteId, script = expected.script, fidelity = expected.fidelity,
                variant = expected.variant, engineVersion = expected.engineVersion,
                datasetVersion = expected.datasetVersion, sourceText = expected.sourceText
            )
            assertThat(selected).isEqualTo(expected)
        }
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
