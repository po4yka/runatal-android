package com.po4yka.runicquotes.data.repository

import com.po4yka.runicquotes.data.local.dao.QuoteDao
import com.po4yka.runicquotes.data.local.dao.TranslationBackfillStateDao
import com.po4yka.runicquotes.data.local.dao.TranslationRecordDao
import com.po4yka.runicquotes.data.local.entity.QuoteEntity
import com.po4yka.runicquotes.data.local.entity.TranslationBackfillStateEntity
import com.po4yka.runicquotes.data.local.entity.TranslationRecordEntity
import com.po4yka.runicquotes.domain.model.Quote
import com.po4yka.runicquotes.domain.model.RunicScript
import com.po4yka.runicquotes.domain.translation.HistoricalStage
import com.po4yka.runicquotes.domain.translation.HistoricalTranslationService
import com.po4yka.runicquotes.domain.translation.TranslationEngineFactory
import com.po4yka.runicquotes.domain.translation.TranslationFidelity
import com.po4yka.runicquotes.domain.translation.TranslationProvenanceEntry
import com.po4yka.runicquotes.domain.translation.TranslationResolutionStatus
import com.po4yka.runicquotes.domain.translation.TranslationResult
import com.po4yka.runicquotes.domain.translation.TranslationTokenBreakdown
import com.po4yka.runicquotes.domain.translation.YoungerFutharkVariant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * Room-backed historical translation cache and backfill repository.
 */
@Singleton
internal class TranslationRepositoryImpl @Inject constructor(
    private val quoteDao: QuoteDao,
    private val translationRecordDao: TranslationRecordDao,
    private val translationBackfillStateDao: TranslationBackfillStateDao,
    private val historicalTranslationService: HistoricalTranslationService,
    private val translationEngineFactory: TranslationEngineFactory
) : TranslationRepository {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    override suspend fun getCachedTranslation(
        quoteId: Long,
        script: RunicScript,
        fidelity: TranslationFidelity,
        youngerVariant: YoungerFutharkVariant
    ): TranslationResult? {
        val engine = translationEngineFactory.create(script)
        return translationRecordDao.getBySelection(
            quoteId = quoteId,
            script = script.name,
            fidelity = fidelity.name,
            variant = requestedVariant(script, youngerVariant),
            engineVersion = engine.engineVersion,
            datasetVersion = engine.datasetVersion
        )?.toDomain()
    }

    override suspend fun getLatestAvailableTranslation(
        quoteId: Long,
        script: RunicScript
    ): TranslationResult? {
        return translationRecordDao.getLatestAvailableForScript(
            quoteId = quoteId,
            script = script.name,
            unavailableStatus = TranslationResolutionStatus.UNAVAILABLE.name
        )?.toDomain()
    }

    override suspend fun cacheTranslation(
        quoteId: Long,
        result: TranslationResult,
        isBackfilled: Boolean
    ) {
        if (result.resolutionStatus == TranslationResolutionStatus.UNAVAILABLE) {
            return
        }
        translationRecordDao.insert(result.toEntity(quoteId = quoteId, isBackfilled = isBackfilled))
    }

    override suspend fun cacheTranslations(
        quoteId: Long,
        results: List<TranslationResult>,
        isBackfilled: Boolean
    ) {
        results.forEach { result ->
            cacheTranslation(quoteId = quoteId, result = result, isBackfilled = isBackfilled)
        }
    }

    override suspend fun translateAndCache(
        quoteId: Long,
        sourceText: String,
        script: RunicScript,
        fidelity: TranslationFidelity,
        youngerVariant: YoungerFutharkVariant,
        isBackfilled: Boolean
    ): TranslationResult {
        val result = historicalTranslationService.translate(
            text = sourceText,
            script = script,
            fidelity = fidelity,
            youngerVariant = youngerVariant
        )
        cacheTranslation(
            quoteId = quoteId,
            result = result,
            isBackfilled = isBackfilled
        )
        return result
    }

    override suspend fun backfillQuote(quote: Quote) {
        cacheTranslations(
            quoteId = quote.id,
            results = buildStrictResults(quote.textLatin),
            isBackfilled = true
        )
    }

    override suspend fun backfillAllQuotes() {
        val now = System.currentTimeMillis()
        val previousState = translationBackfillStateDao.getById()
        val state = if (previousState == null || previousState.engineVersion != BACKFILL_ENGINE_VERSION) {
            TranslationBackfillStateEntity(
                engineVersion = BACKFILL_ENGINE_VERSION,
                startedAt = now,
                updatedAt = now
            )
        } else {
            previousState
        }
        translationBackfillStateDao.upsert(state)

        val quotes = quoteDao.getAll()
            .filter { it.id > state.lastProcessedQuoteId }
            .sortedBy { it.id }

        var processedCount = state.processedCount
        var lastProcessedQuoteId = state.lastProcessedQuoteId

        quotes.forEach { quoteEntity ->
            val quote = quoteEntity.toDomain()
            cacheTranslations(
                quoteId = quote.id,
                results = buildStrictResults(quote.textLatin),
                isBackfilled = true
            )
            processedCount += 1
            lastProcessedQuoteId = quote.id
            translationBackfillStateDao.upsert(
                state.copy(
                    lastProcessedQuoteId = lastProcessedQuoteId,
                    processedCount = processedCount,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }

        translationBackfillStateDao.upsert(
            state.copy(
                lastProcessedQuoteId = lastProcessedQuoteId,
                processedCount = processedCount,
                updatedAt = System.currentTimeMillis(),
                completedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun deleteTranslationsForQuote(quoteId: Long) {
        translationRecordDao.deleteForQuote(quoteId)
    }

    private fun buildStrictResults(sourceText: String): List<TranslationResult> {
        return RunicScript.entries.mapNotNull { script ->
            historicalTranslationService.translate(
                text = sourceText,
                script = script,
                fidelity = TranslationFidelity.STRICT,
                youngerVariant = YoungerFutharkVariant.DEFAULT
            )
        }.filter { result ->
            result.resolutionStatus != TranslationResolutionStatus.UNAVAILABLE &&
                result.provenance.isNotEmpty() &&
                result.script != RunicScript.CIRTH
        }
    }

    private fun TranslationRecordEntity.toDomain(): TranslationResult {
        return TranslationResult(
            sourceText = sourceText,
            script = RunicScript.valueOf(script),
            fidelity = TranslationFidelity.valueOf(fidelity),
            historicalStage = HistoricalStage.valueOf(historicalStage),
            normalizedForm = normalizedForm,
            diplomaticForm = diplomaticForm,
            glyphOutput = glyphOutput,
            requestedVariant = variant,
            resolutionStatus = TranslationResolutionStatus.valueOf(resolutionStatus),
            confidence = confidence,
            notes = json.decodeFromString(ListSerializer(String.serializer()), notesJson),
            unresolvedTokens = json.decodeFromString(ListSerializer(String.serializer()), unresolvedTokensJson),
            provenance = json.decodeFromString(
                ListSerializer(TranslationProvenanceEntry.serializer()),
                provenanceJson
            ),
            tokenBreakdown = json.decodeFromString(
                ListSerializer(TranslationTokenBreakdown.serializer()),
                tokenBreakdownJson
            ),
            engineVersion = engineVersion,
            datasetVersion = datasetVersion,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun TranslationResult.toEntity(
        quoteId: Long,
        isBackfilled: Boolean
    ): TranslationRecordEntity {
        return TranslationRecordEntity(
            quoteId = quoteId,
            sourceText = sourceText,
            script = script.name,
            fidelity = fidelity.name,
            normalizedForm = normalizedForm,
            diplomaticForm = diplomaticForm,
            glyphOutput = glyphOutput,
            historicalStage = historicalStage.name,
            variant = requestedVariant,
            resolutionStatus = resolutionStatus.name,
            confidence = confidence,
            notesJson = json.encodeToString(ListSerializer(String.serializer()), notes),
            unresolvedTokensJson = json.encodeToString(ListSerializer(String.serializer()), unresolvedTokens),
            provenanceJson = json.encodeToString(
                ListSerializer(TranslationProvenanceEntry.serializer()),
                provenance
            ),
            tokenBreakdownJson = json.encodeToString(
                ListSerializer(TranslationTokenBreakdown.serializer()),
                tokenBreakdown
            ),
            engineVersion = engineVersion,
            datasetVersion = datasetVersion,
            isBackfilled = isBackfilled,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun QuoteEntity.toDomain() = Quote(
        id = id,
        textLatin = textLatin,
        author = author,
        runicElder = runicElder,
        runicYounger = runicYounger,
        runicCirth = runicCirth,
        isUserCreated = isUserCreated,
        isFavorite = isFavorite,
        createdAt = createdAt
    )

    private fun requestedVariant(
        script: RunicScript,
        youngerVariant: YoungerFutharkVariant
    ): String? = if (script == RunicScript.YOUNGER_FUTHARK) youngerVariant.name else null

    private companion object {
        const val BACKFILL_ENGINE_VERSION = "historical-backfill-v2"
    }
}
