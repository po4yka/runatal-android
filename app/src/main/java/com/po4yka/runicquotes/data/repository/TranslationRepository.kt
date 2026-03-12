package com.po4yka.runicquotes.data.repository

import com.po4yka.runicquotes.domain.model.Quote
import com.po4yka.runicquotes.domain.model.RunicScript
import com.po4yka.runicquotes.domain.translation.HistoricalTranslationService
import com.po4yka.runicquotes.domain.translation.HistoricalStage
import com.po4yka.runicquotes.domain.translation.TranslationFidelity
import com.po4yka.runicquotes.domain.translation.TranslationResult
import com.po4yka.runicquotes.domain.translation.YoungerFutharkVariant

/**
 * Repository for structured historical translation results.
 */
internal interface TranslationRepository {
    suspend fun getCachedTranslation(
        quoteId: Long,
        script: RunicScript,
        fidelity: TranslationFidelity = TranslationFidelity.DEFAULT
    ): TranslationResult?

    suspend fun getPreferredTranslation(quoteId: Long): TranslationResult?

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
}

/**
 * No-op implementation used by tests or surfaces that do not participate in historical translation.
 */
internal object NoOpTranslationRepository : TranslationRepository {
    override suspend fun getCachedTranslation(
        quoteId: Long,
        script: RunicScript,
        fidelity: TranslationFidelity
    ): TranslationResult? = null

    override suspend fun getPreferredTranslation(quoteId: Long): TranslationResult? = null

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
            historicalStage = HistoricalStage.MODERN_ENGLISH,
            normalizedForm = sourceText,
            diplomaticForm = sourceText,
            glyphOutput = sourceText,
            confidence = 0f,
            notes = emptyList(),
            engineVersion = "noop"
        )
    }

    override suspend fun backfillQuote(quote: Quote) = Unit

    override suspend fun backfillAllQuotes() = Unit
}
