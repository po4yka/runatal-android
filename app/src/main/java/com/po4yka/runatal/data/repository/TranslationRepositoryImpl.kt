package com.po4yka.runatal.data.repository

import com.po4yka.runatal.data.local.dao.QuoteDao
import com.po4yka.runatal.data.local.dao.TranslationBackfillCompletionDao
import com.po4yka.runatal.data.local.dao.TranslationRecordDao
import com.po4yka.runatal.data.local.entity.QuoteEntity
import com.po4yka.runatal.data.local.entity.TranslationBackfillCompletionEntity
import com.po4yka.runatal.data.local.entity.TranslationRecordEntity
import com.po4yka.runatal.domain.model.Quote
import com.po4yka.runatal.domain.model.RunicScript
import com.po4yka.runatal.domain.repository.TranslationRepository
import com.po4yka.runatal.domain.translation.HistoricalStage
import com.po4yka.runatal.domain.translation.HistoricalTranslationService
import com.po4yka.runatal.domain.translation.TranslationDerivationKind
import com.po4yka.runatal.domain.translation.TranslationEngineFactory
import com.po4yka.runatal.domain.translation.TranslationFidelity
import com.po4yka.runatal.domain.translation.TranslationProvenanceEntry
import com.po4yka.runatal.domain.translation.TranslationResolutionStatus
import com.po4yka.runatal.domain.translation.TranslationResult
import com.po4yka.runatal.domain.translation.TranslationTokenBreakdown
import com.po4yka.runatal.domain.translation.YoungerFutharkVariant
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
    private val translationBackfillCompletionDao: TranslationBackfillCompletionDao,
    private val historicalTranslationService: HistoricalTranslationService,
    private val translationEngineFactory: TranslationEngineFactory
) : TranslationRepository {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    override suspend fun getCachedTranslation(
        quoteId: Long,
        script: RunicScript,
        sourceText: String,
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
            datasetVersion = engine.datasetVersion,
            sourceText = sourceText
        )?.toDomain()
    }

    override suspend fun getLatestAvailableTranslation(
        quoteId: Long,
        script: RunicScript,
        sourceText: String
    ): TranslationResult? {
        val engine = translationEngineFactory.create(script)
        return translationRecordDao.getLatestAvailableForScript(
            quoteId = quoteId,
            script = script.name,
            unavailableStatus = TranslationResolutionStatus.UNAVAILABLE.name,
            engineVersion = engine.engineVersion,
            datasetVersion = engine.datasetVersion,
            sourceText = sourceText
        )?.toDomain()
    }

    override suspend fun saveUserQuoteWithTranslations(quote: Quote, results: List<TranslationResult>): Long {
        require(quote.id == 0L && results.isNotEmpty())
        require(results.all { result ->
            result.sourceText == quote.textLatin &&
                result.resolutionStatus != TranslationResolutionStatus.UNAVAILABLE && result.glyphOutput.isNotBlank()
        })
        val records = results.map { it.toEntity(quoteId = 0L, isBackfilled = false) }
        return storageWrite { quoteDao.insertUserQuoteWithTranslations(quote.toEntity(), records) }
    }

    override suspend fun cacheTranslation(
        quoteId: Long,
        result: TranslationResult,
        isBackfilled: Boolean
    ) {
        if (result.resolutionStatus == TranslationResolutionStatus.UNAVAILABLE) {
            return
        }
        storageWrite {
            translationRecordDao.insertIfSourceMatches(result.toEntity(quoteId = quoteId, isBackfilled = isBackfilled))
        }
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
        completeBackfillAttempt(quote, backfillVersion())
    }

    override suspend fun backfillAllQuotes() {
        val version = backfillVersion()
        while (true) {
            val batch = translationBackfillCompletionDao.pendingQuotes(version, BACKFILL_BATCH_SIZE)
            if (batch.isEmpty()) return
            batch.forEach { completeBackfillAttempt(it.toDomain(), version) }
        }
    }

    private suspend fun completeBackfillAttempt(quote: Quote, version: String) {
        val records = buildStrictResults(quote.textLatin).map { it.toEntity(quote.id, isBackfilled = true) }
        storageWrite {
            translationBackfillCompletionDao.completeAttempt(
                TranslationBackfillCompletionEntity(
                    quoteId = quote.id, sourceText = quote.textLatin,
                    versionFingerprint = version, completedAt = System.currentTimeMillis()
                ),
                records
            )
        }
    }

    override suspend fun deleteTranslationsForQuote(quoteId: Long) {
        translationRecordDao.deleteForQuote(quoteId)
    }

    private fun buildStrictResults(sourceText: String): List<TranslationResult> {
        return BACKFILL_SCRIPTS.map { script ->
            historicalTranslationService.translate(
                text = sourceText,
                script = script,
                fidelity = TranslationFidelity.STRICT,
                youngerVariant = YoungerFutharkVariant.DEFAULT
            )
        }.filter { result ->
            result.resolutionStatus != TranslationResolutionStatus.UNAVAILABLE &&
                result.provenance.isNotEmpty()
        }
    }

    private fun TranslationRecordEntity.toDomain(): TranslationResult {
        return TranslationResult(
            sourceText = sourceText,
            script = RunicScript.valueOf(script),
            fidelity = TranslationFidelity.valueOf(fidelity),
            derivationKind = TranslationDerivationKind.valueOf(derivationKind),
            historicalStage = HistoricalStage.valueOf(historicalStage),
            normalizedForm = normalizedForm,
            diplomaticForm = diplomaticForm,
            glyphOutput = glyphOutput,
            requestedVariant = variant.takeIf { it.isNotEmpty() },
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
            derivationKind = derivationKind.name,
            normalizedForm = normalizedForm,
            diplomaticForm = diplomaticForm,
            glyphOutput = glyphOutput,
            historicalStage = historicalStage.name,
            variant = requestedVariant.orEmpty(),
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

    private fun Quote.toEntity() = QuoteEntity(
        textLatin = textLatin, author = author, runicElder = runicElder,
        runicYounger = runicYounger, runicCirth = runicCirth,
        isUserCreated = true, isFavorite = isFavorite, createdAt = createdAt
    )

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
    ): String = if (script == RunicScript.YOUNGER_FUTHARK) youngerVariant.name else ""

    private fun backfillVersion(): String = BACKFILL_SCRIPTS.joinToString("|") { script ->
        val engine = translationEngineFactory.create(script)
        "${script.name}:${engine.engineVersion}:${engine.datasetVersion}"
    }
    private companion object {
        const val BACKFILL_BATCH_SIZE = 25
        val BACKFILL_SCRIPTS = listOf(RunicScript.ELDER_FUTHARK, RunicScript.YOUNGER_FUTHARK)
    }
}
