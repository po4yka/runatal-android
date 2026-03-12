package com.po4yka.runicquotes.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.po4yka.runicquotes.data.local.RunicQuotesDatabase
import com.po4yka.runicquotes.data.local.entity.QuoteEntity
import com.po4yka.runicquotes.data.local.entity.TranslationBackfillStateEntity
import com.po4yka.runicquotes.data.local.entity.TranslationRecordEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TranslationDaoTest {

    private lateinit var database: RunicQuotesDatabase
    private lateinit var quoteDao: QuoteDao
    private lateinit var translationRecordDao: TranslationRecordDao
    private lateinit var translationBackfillStateDao: TranslationBackfillStateDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            RunicQuotesDatabase::class.java
        ).allowMainThreadQueries().build()

        quoteDao = database.quoteDao()
        translationRecordDao = database.translationRecordDao()
        translationBackfillStateDao = database.translationBackfillStateDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun getBySelection_matches_variant_and_dataset_exactly() = runTest {
        quoteDao.insertAll(listOf(quote(id = 1L)))
        val longBranch = record(
            quoteId = 1L,
            script = "YOUNGER_FUTHARK",
            variant = "LONG_BRANCH",
            datasetVersion = "dataset-v1",
            glyphOutput = "long"
        )
        val shortTwig = record(
            quoteId = 1L,
            script = "YOUNGER_FUTHARK",
            variant = "SHORT_TWIG",
            datasetVersion = "dataset-v1",
            glyphOutput = "short"
        )
        val elderLegacy = record(
            quoteId = 1L,
            script = "ELDER_FUTHARK",
            variant = null,
            datasetVersion = "dataset-v0",
            glyphOutput = "legacy"
        )

        translationRecordDao.insert(longBranch)
        translationRecordDao.insert(shortTwig)
        translationRecordDao.insert(elderLegacy)

        val youngerMatch = translationRecordDao.getBySelection(
            quoteId = 1L,
            script = "YOUNGER_FUTHARK",
            fidelity = "STRICT",
            variant = "SHORT_TWIG",
            engineVersion = "engine-v1",
            datasetVersion = "dataset-v1"
        )
        val elderMatch = translationRecordDao.getBySelection(
            quoteId = 1L,
            script = "ELDER_FUTHARK",
            fidelity = "STRICT",
            variant = null,
            engineVersion = "engine-v1",
            datasetVersion = "dataset-v0"
        )
        val datasetMiss = translationRecordDao.getBySelection(
            quoteId = 1L,
            script = "ELDER_FUTHARK",
            fidelity = "STRICT",
            variant = null,
            engineVersion = "engine-v1",
            datasetVersion = "dataset-v1"
        )

        assertEquals("short", youngerMatch?.glyphOutput)
        assertEquals("legacy", elderMatch?.glyphOutput)
        assertNull(datasetMiss)
    }

    @Test
    fun getLatestAvailableForScript_ignores_unavailable_and_prefers_latest_valid_row() = runTest {
        quoteDao.insertAll(listOf(quote(id = 2L)))
        translationRecordDao.insert(
            record(
                quoteId = 2L,
                script = "ELDER_FUTHARK",
                resolutionStatus = "RECONSTRUCTED",
                updatedAt = 100L,
                glyphOutput = "older"
            )
        )
        translationRecordDao.insert(
            record(
                quoteId = 2L,
                script = "ELDER_FUTHARK",
                resolutionStatus = "UNAVAILABLE",
                updatedAt = 400L,
                glyphOutput = ""
            )
        )
        translationRecordDao.insert(
            record(
                quoteId = 2L,
                script = "ELDER_FUTHARK",
                resolutionStatus = "APPROXIMATED",
                updatedAt = 250L,
                glyphOutput = "newest-available"
            )
        )

        val latest = translationRecordDao.getLatestAvailableForScript(
            quoteId = 2L,
            script = "ELDER_FUTHARK",
            unavailableStatus = "UNAVAILABLE"
        )

        assertEquals("newest-available", latest?.glyphOutput)
    }

    @Test
    fun deleteForQuote_removes_only_matching_translation_rows() = runTest {
        quoteDao.insertAll(listOf(quote(id = 3L), quote(id = 4L)))
        translationRecordDao.insert(record(quoteId = 3L, script = "ELDER_FUTHARK"))
        translationRecordDao.insert(record(quoteId = 4L, script = "YOUNGER_FUTHARK", variant = "LONG_BRANCH"))

        translationRecordDao.deleteForQuote(3L)

        val deleted = translationRecordDao.getLatestAvailableForScript(
            quoteId = 3L,
            script = "ELDER_FUTHARK",
            unavailableStatus = "UNAVAILABLE"
        )
        val preserved = translationRecordDao.getLatestAvailableForScript(
            quoteId = 4L,
            script = "YOUNGER_FUTHARK",
            unavailableStatus = "UNAVAILABLE"
        )

        assertNull(deleted)
        assertEquals(4L, preserved?.quoteId)
    }

    @Test
    fun translationBackfillStateDao_upsert_replaces_singleton_row() = runTest {
        translationBackfillStateDao.upsert(
            TranslationBackfillStateEntity(
                engineVersion = "engine-v1",
                lastProcessedQuoteId = 3L,
                processedCount = 2,
                startedAt = 10L,
                updatedAt = 20L
            )
        )
        translationBackfillStateDao.upsert(
            TranslationBackfillStateEntity(
                engineVersion = "engine-v2",
                lastProcessedQuoteId = 5L,
                processedCount = 4,
                startedAt = 10L,
                updatedAt = 30L,
                completedAt = 40L
            )
        )

        val state = translationBackfillStateDao.getById()

        assertNotNull(state)
        assertEquals(TranslationBackfillStateEntity.SINGLETON_ID, state?.id)
        assertEquals("engine-v2", state?.engineVersion)
        assertEquals(5L, state?.lastProcessedQuoteId)
        assertEquals(40L, state?.completedAt)
    }

    private fun quote(id: Long): QuoteEntity {
        return QuoteEntity(
            id = id,
            textLatin = "Quote $id",
            author = "Runatal",
            runicElder = null,
            runicYounger = null,
            runicCirth = null,
            isUserCreated = true,
            isFavorite = false,
            createdAt = 1_000L + id
        )
    }

    private fun record(
        quoteId: Long,
        script: String,
        variant: String? = null,
        datasetVersion: String = "dataset-v1",
        resolutionStatus: String = "RECONSTRUCTED",
        updatedAt: Long = 200L,
        glyphOutput: String = "ᚠ"
    ): TranslationRecordEntity {
        return TranslationRecordEntity(
            quoteId = quoteId,
            sourceText = "Quote $quoteId",
            script = script,
            fidelity = "STRICT",
            derivationKind = "TOKEN_COMPOSED",
            normalizedForm = "normalized",
            diplomaticForm = "diplomatic",
            glyphOutput = glyphOutput,
            historicalStage = "OLD_NORSE",
            variant = variant,
            resolutionStatus = resolutionStatus,
            confidence = if (resolutionStatus == "UNAVAILABLE") 0f else 0.82f,
            notesJson = "[]",
            unresolvedTokensJson = "[]",
            provenanceJson = "[]",
            tokenBreakdownJson = "[]",
            engineVersion = "engine-v1",
            datasetVersion = datasetVersion,
            createdAt = 100L,
            updatedAt = updatedAt
        )
    }
}
