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
        fidelity: TranslationFidelity
    ): TranslationResult? {
        val engineVersion = translationEngineFactory.create(script).engineVersion
        return translationRecordDao.getByQuoteAndScript(
            quoteId = quoteId,
            script = script.name,
            fidelity = fidelity.name,
            engineVersion = engineVersion
        )?.toDomain()
    }

    override suspend fun getPreferredTranslation(quoteId: Long): TranslationResult? {
        return translationRecordDao.getByQuoteId(quoteId).firstOrNull()?.toDomain()
    }

    override suspend fun cacheTranslation(
        quoteId: Long,
        result: TranslationResult,
        isBackfilled: Boolean
    ) {
        translationRecordDao.insert(result.toEntity(quoteId = quoteId, isBackfilled = isBackfilled))
    }

    override suspend fun cacheTranslations(
        quoteId: Long,
        results: List<TranslationResult>,
        isBackfilled: Boolean
    ) {
        results.forEach { result ->
            cacheTranslation(quoteId, result, isBackfilled)
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

    private fun buildStrictResults(sourceText: String): List<TranslationResult> {
        return RunicScript.entries.map { script ->
            historicalTranslationService.translate(
                text = sourceText,
                script = script,
                fidelity = TranslationFidelity.STRICT,
                youngerVariant = YoungerFutharkVariant.DEFAULT
            )
        }
    }

    private fun TranslationRecordEntity.toDomain(): TranslationResult {
        return TranslationResult(
            sourceText = "",
            script = RunicScript.valueOf(script),
            fidelity = TranslationFidelity.valueOf(fidelity),
            historicalStage = HistoricalStage.valueOf(historicalStage),
            normalizedForm = normalizedForm,
            diplomaticForm = diplomaticForm,
            glyphOutput = glyphOutput,
            variant = variant,
            confidence = confidence,
            notes = json.decodeFromString(ListSerializer(String.serializer()), notesJson),
            tokenBreakdown = json.decodeFromString(
                ListSerializer(TranslationTokenBreakdown.serializer()),
                tokenBreakdownJson
            ),
            engineVersion = engineVersion,
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
            script = script.name,
            fidelity = fidelity.name,
            normalizedForm = normalizedForm,
            diplomaticForm = diplomaticForm,
            glyphOutput = glyphOutput,
            historicalStage = historicalStage.name,
            variant = variant,
            confidence = confidence,
            notesJson = json.encodeToString(ListSerializer(String.serializer()), notes),
            tokenBreakdownJson = json.encodeToString(
                ListSerializer(TranslationTokenBreakdown.serializer()),
                tokenBreakdown
            ),
            engineVersion = engineVersion,
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

    private companion object {
        const val BACKFILL_ENGINE_VERSION = "historical-backfill-v1"
    }
}
