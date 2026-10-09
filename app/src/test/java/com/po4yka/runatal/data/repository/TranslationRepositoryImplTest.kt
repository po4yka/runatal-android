package com.po4yka.runatal.data.repository

import com.google.common.truth.Truth.assertThat
import com.po4yka.runatal.data.local.dao.QuoteDao
import com.po4yka.runatal.data.local.dao.TranslationBackfillCompletionDao
import com.po4yka.runatal.data.local.dao.TranslationRecordDao
import com.po4yka.runatal.data.local.entity.QuoteEntity
import com.po4yka.runatal.data.local.entity.TranslationBackfillCompletionEntity
import com.po4yka.runatal.data.local.entity.TranslationRecordEntity
import com.po4yka.runatal.domain.model.Quote
import com.po4yka.runatal.domain.model.RunicScript
import com.po4yka.runatal.domain.repository.NoOpTranslationRepository
import com.po4yka.runatal.domain.translation.HistoricalStage
import com.po4yka.runatal.domain.translation.HistoricalTranslationService
import com.po4yka.runatal.domain.translation.TranslationDerivationKind
import com.po4yka.runatal.domain.translation.TranslationEngine
import com.po4yka.runatal.domain.translation.TranslationEngineFactory
import com.po4yka.runatal.domain.translation.TranslationFidelity
import com.po4yka.runatal.domain.translation.TranslationProvenanceEntry
import com.po4yka.runatal.domain.translation.TranslationResolutionStatus
import com.po4yka.runatal.domain.translation.TranslationResult
import com.po4yka.runatal.domain.translation.TranslationTokenBreakdown
import com.po4yka.runatal.domain.translation.YoungerFutharkVariant
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class TranslationRepositoryImplTest {

    private lateinit var quoteDao: QuoteDao
    private lateinit var translationRecordDao: TranslationRecordDao
    private lateinit var translationBackfillCompletionDao: TranslationBackfillCompletionDao
    private lateinit var historicalTranslationService: HistoricalTranslationService
    private lateinit var translationEngineFactory: TranslationEngineFactory
    private lateinit var repository: TranslationRepositoryImpl

    @Before
    fun setUp() {
        quoteDao = mockk()
        translationRecordDao = mockk(relaxed = true)
        translationBackfillCompletionDao = mockk(relaxed = true)
        historicalTranslationService = mockk()
        translationEngineFactory = mockk()
        RunicScript.entries.forEach { script ->
            every { translationEngineFactory.create(script) } returns mockEngine(
                script, engineVersionFor(script), "dataset-v1"
            )
        }

        repository = TranslationRepositoryImpl(
            quoteDao = quoteDao,
            translationRecordDao = translationRecordDao,
            translationBackfillCompletionDao = translationBackfillCompletionDao,
            historicalTranslationService = historicalTranslationService,
            translationEngineFactory = translationEngineFactory
        )
    }

    @Test
    fun `getCachedTranslation uses exact cache key including variant and maps entity`() = runTest {
        val engine = mockEngine(
            script = RunicScript.YOUNGER_FUTHARK,
            engineVersion = "yf-engine-v1",
            datasetVersion = "dataset-v1"
        )
        every { translationEngineFactory.create(RunicScript.YOUNGER_FUTHARK) } returns engine

        val insertedEntity = slot<TranslationRecordEntity>()
        val result = translationResult(
            script = RunicScript.YOUNGER_FUTHARK,
            requestedVariant = YoungerFutharkVariant.SHORT_TWIG.name,
            glyphOutput = "ᚿᛁᚴᚼᛏ"
        )
        coEvery { translationRecordDao.insertIfSourceMatches(capture(insertedEntity)) } returns true

        repository.cacheTranslation(quoteId = 7L, result = result, isBackfilled = false)

        coEvery {
            translationRecordDao.getBySelection(
                quoteId = 7L,
                script = RunicScript.YOUNGER_FUTHARK.name,
                fidelity = TranslationFidelity.STRICT.name,
                variant = YoungerFutharkVariant.SHORT_TWIG.name,
                engineVersion = "yf-engine-v1",
                datasetVersion = "dataset-v1",
                sourceText = "The wolf hunts at night"
            )
        } returns insertedEntity.captured

        val cached = repository.getCachedTranslation(
            quoteId = 7L,
            script = RunicScript.YOUNGER_FUTHARK,
            fidelity = TranslationFidelity.STRICT,
            youngerVariant = YoungerFutharkVariant.SHORT_TWIG,
            sourceText = "The wolf hunts at night"
        )

        assertThat(cached).isEqualTo(result)
    }

    @Test
    fun `nonvariant scripts use a nonnull database key while retaining null domain metadata`() = runTest {
        listOf(RunicScript.ELDER_FUTHARK, RunicScript.CIRTH).forEach { script ->
            val stored = slot<TranslationRecordEntity>()
            val result = translationResult(script = script, requestedVariant = null, glyphOutput = "ᚠ")
            coEvery { translationRecordDao.insertIfSourceMatches(capture(stored)) } returns true

            repository.cacheTranslation(quoteId = 7L, result = result, isBackfilled = false)

            assertThat(stored.captured.variant).isEmpty()
            coEvery {
                translationRecordDao.getBySelection(
                    quoteId = 7L, script = script.name, fidelity = TranslationFidelity.STRICT.name,
                    variant = "", engineVersion = engineVersionFor(script), datasetVersion = "dataset-v1",
                    sourceText = "The wolf hunts at night"
                )
            } returns stored.captured

            val cached = repository.getCachedTranslation(
                quoteId = 7L, script = script, fidelity = TranslationFidelity.STRICT,
                youngerVariant = YoungerFutharkVariant.DEFAULT,
                sourceText = "The wolf hunts at night"
            )
            assertThat(cached).isEqualTo(result)
        }
    }

    @Test
    fun `cacheTranslation skips unavailable results`() = runTest {
        repository.cacheTranslation(
            quoteId = 4L,
            result = translationResult(
                script = RunicScript.ELDER_FUTHARK,
                resolutionStatus = TranslationResolutionStatus.UNAVAILABLE,
                glyphOutput = ""
            ),
            isBackfilled = false
        )

        coVerify(exactly = 0) { translationRecordDao.insertIfSourceMatches(any()) }
    }

    @Test
    fun `getLatestAvailableTranslation returns mapped domain result`() = runTest {
        val insertedEntity = slot<TranslationRecordEntity>()
        val result = translationResult(
            script = RunicScript.ELDER_FUTHARK,
            historicalStage = HistoricalStage.PROTO_NORSE,
            glyphOutput = "ᚹᚢᛚᚠᚨᛉ"
        )
        coEvery { translationRecordDao.insertIfSourceMatches(capture(insertedEntity)) } returns true
        repository.cacheTranslation(quoteId = 5L, result = result, isBackfilled = true)

        coEvery {
            translationRecordDao.getLatestAvailableForScript(
                quoteId = 5L,
                script = RunicScript.ELDER_FUTHARK.name,
                unavailableStatus = TranslationResolutionStatus.UNAVAILABLE.name,
                engineVersion = "ef-engine-v1",
                datasetVersion = "dataset-v1",
                sourceText = "The wolf hunts at night"
            )
        } returns insertedEntity.captured

        val latest = repository.getLatestAvailableTranslation(
            quoteId = 5L,
            script = RunicScript.ELDER_FUTHARK,
            sourceText = "The wolf hunts at night"
        )

        assertThat(latest).isEqualTo(result)
    }

    @Test
    fun `translateAndCache delegates to service and persists resolved result`() = runTest {
        val insertedEntity = slot<TranslationRecordEntity>()
        val result = translationResult(script = RunicScript.CIRTH, glyphOutput = "")

        coEvery { translationRecordDao.insertIfSourceMatches(capture(insertedEntity)) } returns true
        every {
            historicalTranslationService.translate(
                text = "night",
                script = RunicScript.CIRTH,
                fidelity = TranslationFidelity.READABLE,
                youngerVariant = YoungerFutharkVariant.DEFAULT
            )
        } returns result

        val translated = repository.translateAndCache(
            quoteId = 9L,
            sourceText = "night",
            script = RunicScript.CIRTH,
            fidelity = TranslationFidelity.READABLE,
            youngerVariant = YoungerFutharkVariant.DEFAULT,
            isBackfilled = false
        )

        assertThat(translated).isEqualTo(result)
        assertThat(insertedEntity.captured.quoteId).isEqualTo(9L)
        assertThat(insertedEntity.captured.glyphOutput).isEqualTo("")
    }

    @Test
    fun `backfill uses bounded pending batches and completes unavailable attempts without Cirth`() = runTest {
        val quote = quoteEntity(1L, "The wolf hunts at night")
        coEvery { translationBackfillCompletionDao.pendingQuotes(any(), 25) } returnsMany
            listOf(listOf(quote), emptyList())
        every { historicalTranslationService.translate(any(), any(), any(), any()) } answers {
            translationResult(
                script = secondArg(), resolutionStatus = TranslationResolutionStatus.UNAVAILABLE, glyphOutput = ""
            )
        }
        val completions = mutableListOf<TranslationBackfillCompletionEntity>()
        coEvery { translationBackfillCompletionDao.completeAttempt(capture(completions), any()) } returns true

        repository.backfillAllQuotes()

        assertThat(completions.single().quoteId).isEqualTo(1L)
        assertThat(completions.single().sourceText).isEqualTo(quote.textLatin)
        coVerify(exactly = 1) { translationBackfillCompletionDao.completeAttempt(any(), emptyList()) }
        coVerify(exactly = 2) { translationBackfillCompletionDao.pendingQuotes(any(), 25) }
        verify(exactly = 2) { historicalTranslationService.translate(any(), any(), any(), any()) }
        verify(exactly = 0) { historicalTranslationService.translate(any(), RunicScript.CIRTH, any(), any()) }
    }

    @Test
    fun `deleteTranslationsForQuote delegates to dao`() = runTest {
        repository.deleteTranslationsForQuote(12L)

        coVerify { translationRecordDao.deleteForQuote(12L) }
    }

    @Test
    fun `noop repository returns unavailable result and variant metadata`() = runTest {
        val result = NoOpTranslationRepository.translateAndCache(
            quoteId = 2L,
            sourceText = "night",
            script = RunicScript.YOUNGER_FUTHARK,
            fidelity = TranslationFidelity.STRICT,
            youngerVariant = YoungerFutharkVariant.SHORT_TWIG,
            isBackfilled = false
        )

        assertThat(result.resolutionStatus).isEqualTo(TranslationResolutionStatus.UNAVAILABLE)
        assertThat(result.requestedVariant).isEqualTo(YoungerFutharkVariant.SHORT_TWIG.name)
        assertThat(result.glyphOutput).isEqualTo("night")
    }

    @Test
    fun `backfill queries dirty sources with the new engine fingerprint`() = runTest {
        assertBackfillRestarts("engine-v2", "dataset-v1")
    }

    @Test
    fun `backfill queries dirty sources with the new dataset fingerprint`() = runTest {
        assertBackfillRestarts("yf-engine-v1", "dataset-v2")
    }

    private suspend fun assertBackfillRestarts(engineVersion: String, datasetVersion: String) {
        every { translationEngineFactory.create(RunicScript.YOUNGER_FUTHARK) } returns
            mockEngine(RunicScript.YOUNGER_FUTHARK, engineVersion, datasetVersion)
        val version = slot<String>()
        coEvery { translationBackfillCompletionDao.pendingQuotes(capture(version), 25) } returns emptyList()

        repository.backfillAllQuotes()

        assertThat(version.captured).contains("YOUNGER_FUTHARK:$engineVersion:$datasetVersion")
        assertThat(version.captured).doesNotContain("CIRTH")
    }

    private fun engineVersionFor(script: RunicScript): String = when (script) {
        RunicScript.ELDER_FUTHARK -> "ef-engine-v1"
        RunicScript.YOUNGER_FUTHARK -> "yf-engine-v1"
        RunicScript.CIRTH -> "cirth-engine-v1"
    }

    private fun mockEngine(
        script: RunicScript,
        engineVersion: String,
        datasetVersion: String
    ): TranslationEngine {
        return mockk<TranslationEngine>().also { engine ->
            every { engine.script } returns script
            every { engine.engineVersion } returns engineVersion
            every { engine.datasetVersion } returns datasetVersion
        }
    }

    private fun quoteEntity(
        id: Long,
        text: String
    ): QuoteEntity {
        return QuoteEntity(
            id = id,
            textLatin = text,
            author = "Runatal",
            runicElder = null,
            runicYounger = null,
            runicCirth = null,
            isUserCreated = true,
            isFavorite = false,
            createdAt = 1_000L + id
        )
    }

    private fun translationResult(
        script: RunicScript,
        fidelity: TranslationFidelity = TranslationFidelity.STRICT,
        historicalStage: HistoricalStage = HistoricalStage.OLD_NORSE,
        requestedVariant: String? = if (script == RunicScript.YOUNGER_FUTHARK) {
            YoungerFutharkVariant.DEFAULT.name
        } else {
            null
        },
        resolutionStatus: TranslationResolutionStatus = TranslationResolutionStatus.RECONSTRUCTED,
        provenance: List<TranslationProvenanceEntry> = listOf(
            TranslationProvenanceEntry(
                sourceId = "runor",
                referenceId = "ref-1",
                label = "Runor",
                role = "Reference",
                license = "Reference only"
            )
        ),
        glyphOutput: String
    ): TranslationResult {
        return TranslationResult(
        sourceText = "The wolf hunts at night",
            script = script,
            fidelity = fidelity,
            derivationKind = TranslationDerivationKind.TOKEN_COMPOSED,
            historicalStage = historicalStage,
            normalizedForm = "normalized",
            diplomaticForm = "diplomatic",
            glyphOutput = glyphOutput,
            requestedVariant = requestedVariant,
            resolutionStatus = resolutionStatus,
            confidence = if (resolutionStatus == TranslationResolutionStatus.UNAVAILABLE) 0f else 0.84f,
            notes = listOf("note-1"),
            unresolvedTokens = if (resolutionStatus == TranslationResolutionStatus.UNAVAILABLE) {
                listOf("signal")
            } else {
                emptyList()
            },
            provenance = provenance,
            tokenBreakdown = listOf(
                TranslationTokenBreakdown(
                    sourceToken = "wolf",
                    normalizedToken = "ulfR",
                    diplomaticToken = "ulfr",
                    glyphToken = glyphOutput.takeIf { it.isNotBlank() } ?: "ᚢᛚᚠᚱ",
                    resolutionStatus = resolutionStatus,
                    provenance = provenance
                )
            ),
            engineVersion = when (script) {
                RunicScript.ELDER_FUTHARK -> "ef-engine-v1"
                RunicScript.YOUNGER_FUTHARK -> "yf-engine-v1"
                RunicScript.CIRTH -> "cirth-engine-v1"
            },
            datasetVersion = "dataset-v1",
            createdAt = 123L,
            updatedAt = 456L
        )
    }
}
