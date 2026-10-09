package com.po4yka.runatal.domain.usecase.quote

import com.po4yka.runatal.domain.model.Quote
import com.po4yka.runatal.domain.model.ResolvedQuoteRendering
import com.po4yka.runatal.domain.model.RunicScript
import com.po4yka.runatal.domain.model.displayName
import com.po4yka.runatal.domain.repository.TranslationRepository
import com.po4yka.runatal.domain.translation.HistoricalStage
import com.po4yka.runatal.domain.translation.TranslationMode
import com.po4yka.runatal.domain.translation.TranslationResolutionStatus
import com.po4yka.runatal.domain.translation.TranslationResult
import com.po4yka.runatal.domain.translation.label
import com.po4yka.runatal.domain.translation.stitchTokens
import com.po4yka.runatal.domain.transliteration.TransliterationFactory
import com.po4yka.runatal.domain.transliteration.WordTransliterationPair
import javax.inject.Inject

/** Resolves persisted intent independently of experimental controls and automatic backfill records. */
internal class ResolveQuoteRenderingUseCase @Inject constructor(
    private val transliterationFactory: TransliterationFactory,
    private val translationRepository: TranslationRepository
) {
    suspend operator fun invoke(quote: Quote, script: RunicScript): ResolvedQuoteRendering =
        when (quote.renderingMode) {
            TranslationMode.TRANSLITERATE -> direct(quote, script)
            TranslationMode.TRANSLATE -> historical(quote, script)
        }

    private fun direct(quote: Quote, script: RunicScript): ResolvedQuoteRendering {
        val generated = transliterationFactory.transliterateWordByWord(
            quote.textLatin, script, quote.renderingYoungerVariant
        )
        val stored = when (script) {
            RunicScript.ELDER_FUTHARK -> quote.runicElder
            RunicScript.YOUNGER_FUTHARK -> quote.runicYounger
            RunicScript.CIRTH -> quote.runicCirth
        }
        val defaultGlyphs = transliterationFactory.transliterate(quote.textLatin, script)
        val custom = !stored.isNullOrBlank() && stored != generated.fullText && stored != defaultGlyphs
        return ResolvedQuoteRendering(
            glyphOutput = if (custom) stored.orEmpty() else generated.fullText,
            wordBreakdown = if (custom) emptyList() else generated.wordPairs,
            mode = TranslationMode.TRANSLITERATE,
            label = if (custom) "${script.displayName} · Custom stored glyphs"
                else "${script.displayName} · Transliteration${variantLabel(quote, script)}",
            notes = if (custom) listOf("Stored custom glyphs have no verified word alignment.") else emptyList()
        )
    }

    private suspend fun historical(quote: Quote, script: RunicScript): ResolvedQuoteRendering {
        val cached = translationRepository.getCachedTranslation(
            quote.id, script, quote.textLatin, quote.renderingFidelity, quote.renderingYoungerVariant
        )
        val result = cached?.takeIf { matches(it, quote, script) }
            ?: translationRepository.translateAndCache(
                quote.id, quote.textLatin, script, quote.renderingFidelity, quote.renderingYoungerVariant,
                isBackfilled = true
            )
        val valid = matches(result, quote, script)
        val available = valid && result.resolutionStatus != TranslationResolutionStatus.UNAVAILABLE &&
            result.glyphOutput.isNotBlank()
        val status = if (available) result.resolutionStatus else TranslationResolutionStatus.UNAVAILABLE
        val pairs = if (available) result.tokenBreakdown.map {
            WordTransliterationPair(it.sourceToken, it.glyphToken)
        } else emptyList()
        val alignedPairs = pairs.takeIf { stitchTokens(it.map { pair -> pair.runicToken }) == result.glyphOutput }
            .orEmpty()
        return ResolvedQuoteRendering(
            glyphOutput = if (available) result.glyphOutput else "",
            wordBreakdown = alignedPairs,
            mode = TranslationMode.TRANSLATE,
            label = "${script.displayName} · ${if (available) trackLabel(result) else "Historical output"} · " +
                "${quote.renderingFidelity.label} · ${status.label}${variantLabel(quote, script)}",
            resolutionStatus = status,
            provenance = if (valid) result.provenance else emptyList(),
            notes = if (valid) result.notes else
                listOf("The prepared result does not match the stored quote selection.")
        )
    }

    private fun matches(result: TranslationResult, quote: Quote, script: RunicScript): Boolean =
        result.sourceText == quote.textLatin && result.script == script && result.fidelity == quote.renderingFidelity &&
            result.requestedVariant ==
                (if (script == RunicScript.YOUNGER_FUTHARK) quote.renderingYoungerVariant.name else null)

    private fun trackLabel(result: TranslationResult): String = when (result.historicalStage) {
        HistoricalStage.OLD_NORSE -> "Old Norse"
        HistoricalStage.PROTO_NORSE -> "Proto-Norse"
        HistoricalStage.PRESERVED_SOURCE, HistoricalStage.MODERN_ENGLISH -> "Preserved source"
        HistoricalStage.MIXED_PROTO_NORSE_SOURCE -> "Mixed Proto-Norse/source"
        HistoricalStage.EREBOR_ENGLISH -> if (result.resolutionStatus == TranslationResolutionStatus.APPROXIMATED)
            "Cirth glyph approximation" else "Published English Cirth profile"
    }

    private fun variantLabel(quote: Quote, script: RunicScript): String =
        if (script == RunicScript.YOUNGER_FUTHARK) " · ${quote.renderingYoungerVariant.label}" else ""
}
