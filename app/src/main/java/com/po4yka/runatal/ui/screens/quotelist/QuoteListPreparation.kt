package com.po4yka.runatal.ui.screens.quotelist

import com.po4yka.runatal.domain.model.Quote
import com.po4yka.runatal.domain.model.ResolvedQuoteRendering
import com.po4yka.runatal.domain.model.RunicScript
import com.po4yka.runatal.domain.usecase.quote.ResolveQuoteRenderingUseCase
import java.util.Locale
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** Reuses rendering only for unchanged current quotes; search and font never alter rune resolution. */
internal class QuoteListPreparation(private val resolve: ResolveQuoteRenderingUseCase) {
    private var previousQuotes: List<Quote>? = null
    private var previousLocale: Locale? = null
    private var previousScript: RunicScript? = null
    private var index = emptyList<IndexedQuote>()
    private var counts = emptyMap<QuoteFilter, Int>()
    private val renderings = mutableMapOf<Quote, ResolvedQuoteRendering>()

    suspend fun prepare(
        quotes: List<Quote>, script: RunicScript, font: String, filter: QuoteFilter, query: String
    ): QuoteListUiState {
        val locale = Locale.getDefault()
        if (previousQuotes != quotes || previousLocale != locale) rebuildIndex(quotes, locale)
        if (previousScript != script) {
            renderings.clear()
            previousScript = script
        }
        val normalizedQuery = query.trim().lowercase(locale)
        val items = buildList {
            for (entry in index) {
                currentCoroutineContext().ensureActive()
                if (!entry.matches(filter, normalizedQuery)) continue
                val rendering = renderings[entry.quote] ?: resolve(entry.quote, script).also {
                    renderings[entry.quote] = it
                }
                add(QuoteListItemUiModel(entry.quote, rendering.glyphOutput, rendering))
            }
        }
        return QuoteListUiState(
            quotes = items.map { it.quote }, quoteItems = items, currentFilter = filter, searchQuery = query,
            selectedScript = script, selectedFont = font, isLoading = false, filterCounts = counts
        )
    }

    private fun rebuildIndex(quotes: List<Quote>, locale: Locale) {
        renderings.keys.retainAll(quotes.toSet())
        index = quotes.map { IndexedQuote(it, it.textLatin.lowercase(locale), it.author.lowercase(locale)) }
        counts = mapOf(QuoteFilter.ALL to quotes.size, QuoteFilter.FAVORITES to quotes.count { it.isFavorite },
            QuoteFilter.USER_CREATED to quotes.count { it.isUserCreated })
        previousQuotes = quotes
        previousLocale = locale
    }

    private data class IndexedQuote(val quote: Quote, val text: String, val author: String) {
        fun matches(filter: QuoteFilter, query: String): Boolean {
            val matchesTab = when (filter) {
                QuoteFilter.ALL -> true
                QuoteFilter.FAVORITES -> quote.isFavorite
                QuoteFilter.USER_CREATED -> quote.isUserCreated
            }
            return matchesTab && (query.isEmpty() || text.contains(query) || author.contains(query))
        }
    }
}
