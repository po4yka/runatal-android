package com.po4yka.runatal.domain.repository

import com.po4yka.runatal.domain.model.Quote
import com.po4yka.runatal.domain.model.RunicScript
import com.po4yka.runatal.domain.translation.HistoricalStage
import com.po4yka.runatal.domain.translation.TranslationDerivationKind
import com.po4yka.runatal.domain.translation.TranslationFidelity
import com.po4yka.runatal.domain.translation.TranslationProvenanceEntry
import com.po4yka.runatal.domain.translation.TranslationResolutionStatus
import com.po4yka.runatal.domain.translation.TranslationResult
import com.po4yka.runatal.domain.translation.YoungerFutharkVariant

/**
 * Repository for structured historical translation results.
 */
internal interface TranslationRepository {
    suspend fun getCachedTranslation(
        quoteId: Long,
        script: RunicScript,
        fidelity: TranslationFidelity = TranslationFidelity.DEFAULT,
        youngerVariant: YoungerFutharkVariant = YoungerFutharkVariant.DEFAULT
    ): TranslationResult?

    suspend fun getLatestAvailableTranslation(
        quoteId: Long,
        script: RunicScript
    ): TranslationResult?

    suspend fun cacheTranslation(
        quoteId: Long,
        result: TranslationResult,
        isBackfilled: Boolean = false
    )

    suspend fun cacheTranslations(
        quoteId: Long,
        results: List<TranslationResult>,
        isBackfilled: Boolean = false
    )

    suspend fun translateAndCache(
        quoteId: Long,
        sourceText: String,
        script: RunicScript,
        fidelity: TranslationFidelity = TranslationFidelity.DEFAULT,
        youngerVariant: YoungerFutharkVariant = YoungerFutharkVariant.DEFAULT,
        isBackfilled: Boolean = false
    ): TranslationResult

    suspend fun backfillQuote(quote: Quote)

    suspend fun backfillAllQuotes()

    suspend fun deleteTranslationsForQuote(quoteId: Long)
}

/**
 * No-op implementation used by tests or surfaces that do not participate in historical translation.
 */
internal object NoOpTranslationRepository : TranslationRepository {
    override suspend fun getCachedTranslation(
        quoteId: Long,
        script: RunicScript,
        fidelity: TranslationFidelity,
        youngerVariant: YoungerFutharkVariant
    ): TranslationResult? = null

    override suspend fun cacheTranslation(
        quoteId: Long,
        result: TranslationResult,
        isBackfilled: Boolean
    ) = Unit

    override suspend fun cacheTranslations(
        quoteId: Long,
        results: List<TranslationResult>,
        isBackfilled: Boolean
    ) = Unit

    override suspend fun translateAndCache(
        quoteId: Long,
        sourceText: String,
        script: RunicScript,
        fidelity: TranslationFidelity,
        youngerVariant: YoungerFutharkVariant,
        isBackfilled: Boolean
    ): TranslationResult {
        return TranslationResult(
            sourceText = sourceText,
            script = script,
            fidelity = fidelity,
            derivationKind = TranslationDerivationKind.TOKEN_COMPOSED,
            historicalStage = HistoricalStage.MODERN_ENGLISH,
            normalizedForm = sourceText,
            diplomaticForm = sourceText,
            glyphOutput = sourceText,
            requestedVariant = if (script == RunicScript.YOUNGER_FUTHARK) youngerVariant.name else null,
            resolutionStatus = TranslationResolutionStatus.UNAVAILABLE,
            confidence = 0f,
            notes = emptyList(),
            unresolvedTokens = emptyList(),
            provenance = emptyList<TranslationProvenanceEntry>(),
            engineVersion = "noop",
            datasetVersion = "noop"
        )
    }

    override suspend fun backfillQuote(quote: Quote) = Unit

    override suspend fun backfillAllQuotes() = Unit

    override suspend fun getLatestAvailableTranslation(
        quoteId: Long,
        script: RunicScript
    ): TranslationResult? = null

    override suspend fun deleteTranslationsForQuote(quoteId: Long) = Unit
}
