package com.po4yka.runatal.domain.usecase.quote

import com.po4yka.runatal.domain.repository.ReadingHistoryRepository
import kotlinx.coroutines.flow.first
import com.po4yka.runatal.domain.repository.QuoteRepository
import com.po4yka.runatal.domain.model.Quote
import com.po4yka.runatal.domain.model.ResolvedQuoteRendering
import com.po4yka.runatal.domain.model.RunicScript
import com.po4yka.runatal.domain.transliteration.WordTransliterationPair
import javax.inject.Inject

internal data class QuotePresentation(
    val quote: Quote,
    val runicText: String,
    val wordBreakdown: List<WordTransliterationPair>,
    val recentQuotes: List<QuoteRecentPresentationItem>,
    val rendering: ResolvedQuoteRendering
)

internal data class QuoteRecentPresentationItem(
    val quote: Quote,
    val runicText: String,
    val rendering: ResolvedQuoteRendering
)

internal data class LoadedQuoteSurface(
    val quote: Quote,
    val recentQuoteCandidates: List<Quote>,
    val presentation: QuotePresentation
)

internal enum class QuoteSurfaceSource {
    DAILY,
    RANDOM
}

internal class BuildQuotePresentationUseCase @Inject constructor(
    private val resolveQuoteRenderingUseCase: ResolveQuoteRenderingUseCase
) {

    suspend operator fun invoke(
        quote: Quote,
        selectedScript: RunicScript,
        recentQuoteCandidates: List<Quote>
    ): QuotePresentation {
        val resolvedQuote = resolveQuoteRenderingUseCase(quote, selectedScript)
        val recentQuotes = recentQuoteCandidates.map { recentQuote ->
            val rendering = resolveQuoteRenderingUseCase(recentQuote, selectedScript)
            QuoteRecentPresentationItem(recentQuote, rendering.glyphOutput, rendering)
        }

        return QuotePresentation(
            quote = quote,
            runicText = resolvedQuote.glyphOutput,
            wordBreakdown = resolvedQuote.wordBreakdown,
            recentQuotes = recentQuotes,
            rendering = resolvedQuote
        )
    }

}

internal class LoadQuoteSurfaceUseCase @Inject constructor(
    private val quoteRepository: QuoteRepository,
    private val buildQuotePresentationUseCase: BuildQuotePresentationUseCase,
    private val readingHistoryRepository: ReadingHistoryRepository
) {

    suspend operator fun invoke(
        source: QuoteSurfaceSource,
        selectedScript: RunicScript
    ): LoadedQuoteSurface? {
        val quote = when (source) {
            QuoteSurfaceSource.DAILY -> quoteRepository.quoteOfTheDay()
            QuoteSurfaceSource.RANDOM -> quoteRepository.randomQuote()
        } ?: return null

        readingHistoryRepository.recordRead(quote.id, selectedScript)
        val recentQuoteCandidates = readingHistoryRepository.readings().first()
            .map { it.quote }.distinctBy { it.id }
            .filter { it.id != quote.id }.take(RECENT_QUOTES_LIMIT)

        return LoadedQuoteSurface(
            quote = quote,
            recentQuoteCandidates = recentQuoteCandidates,
            presentation = buildQuotePresentationUseCase(
                quote = quote,
                selectedScript = selectedScript,
                recentQuoteCandidates = recentQuoteCandidates
            )
        )
    }

    private companion object {
        const val RECENT_QUOTES_LIMIT = 3
    }
}
