package com.po4yka.runicquotes.data.repository

import com.google.common.truth.Truth.assertThat
import com.po4yka.runicquotes.data.local.dao.QuoteDao
import com.po4yka.runicquotes.data.local.dao.TranslationBackfillStateDao
import com.po4yka.runicquotes.data.local.dao.TranslationRecordDao
import com.po4yka.runicquotes.data.local.entity.QuoteEntity
import com.po4yka.runicquotes.data.local.entity.TranslationBackfillStateEntity
import com.po4yka.runicquotes.data.local.entity.TranslationRecordEntity
import com.po4yka.runicquotes.domain.model.Quote
import com.po4yka.runicquotes.domain.model.RunicScript
import com.po4yka.runicquotes.domain.repository.NoOpTranslationRepository
import com.po4yka.runicquotes.domain.translation.HistoricalStage
import com.po4yka.runicquotes.domain.translation.HistoricalTranslationService
import com.po4yka.runicquotes.domain.translation.TranslationDerivationKind
import com.po4yka.runicquotes.domain.translation.TranslationEngine
import com.po4yka.runicquotes.domain.translation.TranslationEngineFactory
import com.po4yka.runicquotes.domain.translation.TranslationFidelity
import com.po4yka.runicquotes.domain.translation.TranslationProvenanceEntry
import com.po4yka.runicquotes.domain.translation.TranslationResolutionStatus
import com.po4yka.runicquotes.domain.translation.TranslationResult
import com.po4yka.runicquotes.domain.translation.TranslationTokenBreakdown
import com.po4yka.runicquotes.domain.translation.YoungerFutharkVariant
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class TranslationRepositoryImplTest {

    private lateinit var quoteDao: QuoteDao
    private lateinit var translationRecordDao: TranslationRecordDao
    private lateinit var translationBackfillStateDao: TranslationBackfillStateDao
    private lateinit var historicalTranslationService: HistoricalTranslationService
    private lateinit var translationEngineFactory: TranslationEngineFactory
    private lateinit var repository: TranslationRepositoryImpl

    @Before
    fun setUp() {
        quoteDao = mockk()
        translationRecordDao = mockk(relaxed = true)
        translationBackfillStateDao = mockk(relaxed = true)
        historicalTranslationService = mockk()
        translationEngineFactory = mockk()

        repository = TranslationRepositoryImpl(
            quoteDao = quoteDao,
            translationRecordDao = translationRecordDao,
            translationBackfillStateDao = translationBackfillStateDao,
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
        coEvery { translationRecordDao.insert(capture(insertedEntity)) } returns 11L

        repository.cacheTranslation(quoteId = 7L, result = result, isBackfilled = false)

        coEvery {
            translationRecordDao.getBySelection(
                quoteId = 7L,
                script = RunicScript.YOUNGER_FUTHARK.name,
                fidelity = TranslationFidelity.STRICT.name,
                variant = YoungerFutharkVariant.SHORT_TWIG.name,
                engineVersion = "yf-engine-v1",
                datasetVersion = "dataset-v1"
            )
        } returns insertedEntity.captured

        val cached = repository.getCachedTranslation(
            quoteId = 7L,
            script = RunicScript.YOUNGER_FUTHARK,
            fidelity = TranslationFidelity.STRICT,
            youngerVariant = YoungerFutharkVariant.SHORT_TWIG
        )

        assertThat(cached).isEqualTo(result)
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

        coVerify(exactly = 0) { translationRecordDao.insert(any()) }
    }

    @Test
    fun `getLatestAvailableTranslation returns mapped domain result`() = runTest {
        val insertedEntity = slot<TranslationRecordEntity>()
        val result = translationResult(
            script = RunicScript.ELDER_FUTHARK,
            historicalStage = HistoricalStage.PROTO_NORSE,
            glyphOutput = "ᚹᚢᛚᚠᚨᛉ"
        )
        coEvery { translationRecordDao.insert(capture(insertedEntity)) } returns 3L
        repository.cacheTranslation(quoteId = 5L, result = result, isBackfilled = true)

        coEvery {
            translationRecordDao.getLatestAvailableForScript(
                quoteId = 5L,
                script = RunicScript.ELDER_FUTHARK.name,
                unavailableStatus = TranslationResolutionStatus.UNAVAILABLE.name
            )
        } returns insertedEntity.captured

        val latest = repository.getLatestAvailableTranslation(
            quoteId = 5L,
            script = RunicScript.ELDER_FUTHARK
        )

        assertThat(latest).isEqualTo(result)
    }

    @Test
    fun `translateAndCache delegates to service and persists resolved result`() = runTest {
        val insertedEntity = slot<TranslationRecordEntity>()
        val result = translationResult(script = RunicScript.CIRTH, glyphOutput = "")

        coEvery { translationRecordDao.insert(capture(insertedEntity)) } returns 9L
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
    fun `backfillAllQuotes resumes from saved state and caches only supported strict results`() = runTest {
        val priorState = TranslationBackfillStateEntity(
            engineVersion = "historical-backfill-v2",
            lastProcessedQuoteId = 1L,
            processedCount = 1,
            startedAt = 100L,
            updatedAt = 150L
        )
        val quoteEntity = quoteEntity(id = 2L, text = "The wolf hunts at night")
        val elderResult = translationResult(
            script = RunicScript.ELDER_FUTHARK,
            historicalStage = HistoricalStage.PROTO_NORSE,
            glyphOutput = "ᚹᚢᛚᚠᚨᛉ"
        )
        val youngerWithoutProvenance = translationResult(
            script = RunicScript.YOUNGER_FUTHARK,
            historicalStage = HistoricalStage.OLD_NORSE,
            provenance = emptyList(),
            glyphOutput = "ᚢᛚᚠᚱ"
        )
        val cirthResult = translationResult(
            script = RunicScript.CIRTH,
            historicalStage = HistoricalStage.EREBOR_ENGLISH,
            glyphOutput = ""
        )

        coEvery { translationBackfillStateDao.getById() } returns priorState
        coEvery { quoteDao.getAll() } returns listOf(quoteEntity(id = 1L, text = "old"), quoteEntity)
        every {
            historicalTranslationService.translate(
                text = quoteEntity.textLatin,
                script = RunicScript.ELDER_FUTHARK,
                fidelity = TranslationFidelity.STRICT,
                youngerVariant = YoungerFutharkVariant.DEFAULT
            )
        } returns elderResult
        every {
            historicalTranslationService.translate(
                text = quoteEntity.textLatin,
                script = RunicScript.YOUNGER_FUTHARK,
                fidelity = TranslationFidelity.STRICT,
                youngerVariant = YoungerFutharkVariant.DEFAULT
            )
        } returns youngerWithoutProvenance
        every {
            historicalTranslationService.translate(
                text = quoteEntity.textLatin,
                script = RunicScript.CIRTH,
                fidelity = TranslationFidelity.STRICT,
                youngerVariant = YoungerFutharkVariant.DEFAULT
            )
        } returns cirthResult

        val insertedEntities = mutableListOf<TranslationRecordEntity>()
        val upsertedStates = mutableListOf<TranslationBackfillStateEntity>()
        coEvery { translationRecordDao.insert(capture(insertedEntities)) } returnsMany listOf(1L)
        coEvery { translationBackfillStateDao.upsert(capture(upsertedStates)) } returns Unit

        repository.backfillAllQuotes()

        assertThat(insertedEntities).hasSize(1)
        assertThat(insertedEntities.single().script).isEqualTo(RunicScript.ELDER_FUTHARK.name)
        assertThat(insertedEntities.single().quoteId).isEqualTo(2L)
        assertThat(upsertedStates.first().lastProcessedQuoteId).isEqualTo(1L)
        assertThat(upsertedStates.last().lastProcessedQuoteId).isEqualTo(2L)
        assertThat(upsertedStates.last().processedCount).isEqualTo(2)
        assertThat(upsertedStates.last().completedAt).isNotNull()
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
