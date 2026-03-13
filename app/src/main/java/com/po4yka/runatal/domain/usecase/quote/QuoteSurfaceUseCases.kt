package com.po4yka.runatal.domain.usecase.quote

import com.po4yka.runatal.domain.repository.QuoteRepository
import com.po4yka.runatal.domain.repository.TranslationRepository
import com.po4yka.runatal.domain.model.Quote
import com.po4yka.runatal.domain.model.RunicScript
import com.po4yka.runatal.domain.model.getRunicText
import com.po4yka.runatal.domain.transliteration.TransliterationFactory
import com.po4yka.runatal.domain.transliteration.WordTransliterationPair
import javax.inject.Inject

internal data class QuotePresentation(
    val quote: Quote,
    val runicText: String,
    val wordBreakdown: List<WordTransliterationPair>,
    val recentQuotes: List<QuoteRecentPresentationItem>
)

internal data class QuoteRecentPresentationItem(
    val quote: Quote,
    val runicText: String
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
    private val transliterationFactory: TransliterationFactory,
    private val translationRepository: TranslationRepository
) {

    suspend operator fun invoke(
        quote: Quote,
        selectedScript: RunicScript,
        recentQuoteCandidates: List<Quote>
    ): QuotePresentation {
        val resolvedQuote = resolveRunicContent(quote, selectedScript)
        val recentQuotes = recentQuoteCandidates.map { recentQuote ->
            QuoteRecentPresentationItem(
                quote = recentQuote,
                runicText = resolveRunicContent(recentQuote, selectedScript).runicText
            )
        }

        return QuotePresentation(
            quote = quote,
            runicText = resolvedQuote.runicText,
            wordBreakdown = resolvedQuote.wordBreakdown,
            recentQuotes = recentQuotes
        )
    }

    private suspend fun resolveRunicContent(
        quote: Quote,
        script: RunicScript
    ): ResolvedQuoteRunicContent {
        val latestTranslation = translationRepository.getLatestAvailableTranslation(
            quoteId = quote.id,
            script = script
        )
        if (latestTranslation != null) {
            return ResolvedQuoteRunicContent(
                runicText = latestTranslation.glyphOutput,
                wordBreakdown = latestTranslation.tokenBreakdown.map { token ->
                    WordTransliterationPair(
                        sourceToken = token.sourceToken,
                        runicToken = token.glyphToken
                    )
                }
            )
        }

        return ResolvedQuoteRunicContent(
            runicText = quote.getRunicText(script, transliterationFactory),
            wordBreakdown = transliterationFactory.transliterateWordByWord(
                text = quote.textLatin,
                script = script
            ).wordPairs
        )
    }
}

internal class LoadQuoteSurfaceUseCase @Inject constructor(
    private val quoteRepository: QuoteRepository,
    private val buildQuotePresentationUseCase: BuildQuotePresentationUseCase
) {

    suspend operator fun invoke(
        source: QuoteSurfaceSource,
        selectedScript: RunicScript
    ): LoadedQuoteSurface? {
        val quote = when (source) {
            QuoteSurfaceSource.DAILY -> quoteRepository.quoteOfTheDay()
            QuoteSurfaceSource.RANDOM -> quoteRepository.randomQuote()
        } ?: return null

        val recentQuoteCandidates = quoteRepository.getAllQuotes()
            .filter { it.id != quote.id }
            .take(RECENT_QUOTES_LIMIT)

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

private data class ResolvedQuoteRunicContent(
    val runicText: String,
    val wordBreakdown: List<WordTransliterationPair>
)
