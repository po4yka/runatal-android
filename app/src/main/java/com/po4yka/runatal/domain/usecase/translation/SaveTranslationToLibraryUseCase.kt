package com.po4yka.runatal.domain.usecase.translation

import com.po4yka.runatal.domain.repository.QuoteRepository
import com.po4yka.runatal.domain.repository.TranslationRepository
import com.po4yka.runatal.domain.model.Quote
import com.po4yka.runatal.domain.model.QuoteInputPolicy
import com.po4yka.runatal.domain.model.RunicScript
import com.po4yka.runatal.domain.translation.TranslationFidelity
import com.po4yka.runatal.domain.translation.TranslationMode
import com.po4yka.runatal.domain.translation.TranslationResolutionStatus
import com.po4yka.runatal.domain.translation.TranslationResult
import com.po4yka.runatal.domain.translation.YoungerFutharkVariant
import javax.inject.Inject

internal data class SaveTranslationRequest(
    val inputText: String,
    val translationMode: TranslationMode,
    val selectedScript: RunicScript,
    val fidelity: TranslationFidelity,
    val youngerVariant: YoungerFutharkVariant
)

internal data class SaveTranslationResult(
    val message: String
)

internal class SaveTranslationToLibraryUseCase @Inject constructor(
    private val quoteRepository: QuoteRepository,
    private val translationRepository: TranslationRepository,
    private val buildTransliterationBundleUseCase: BuildTransliterationBundleUseCase,
    private val buildHistoricalTranslationBundleUseCase: BuildHistoricalTranslationBundleUseCase
) {

    suspend operator fun invoke(request: SaveTranslationRequest): SaveTranslationResult {
        val input = QuoteInputPolicy.prepareSourceText(request.inputText)
        check(QuoteInputPolicy.quoteTextError(request.inputText) == null) { "Enter valid source text before saving." }
        val transliterationBundle = buildTransliterationBundleUseCase(input, youngerVariant = request.youngerVariant)
        check(transliterationBundle.errorMessage == null) { "Could not prepare direct renderings." }
        check(transliterationBundle.outputFor(request.selectedScript).isNotBlank()) { "No selected output to save." }
        val historicalBundle = if (request.translationMode == TranslationMode.TRANSLATE) {
            buildHistoricalTranslationBundleUseCase(
                inputText = input,
                fidelity = request.fidelity,
                youngerVariant = request.youngerVariant
            )
        } else {
            HistoricalTranslationBundle()
        }

        val quote = Quote(
            id = 0L,
            textLatin = input,
            author = DEFAULT_TRANSLATION_AUTHOR,
            runicElder = transliterationBundle.outputFor(RunicScript.ELDER_FUTHARK),
            runicYounger = transliterationBundle.outputFor(RunicScript.YOUNGER_FUTHARK),
            runicCirth = transliterationBundle.outputFor(RunicScript.CIRTH),
            isUserCreated = true,
            isFavorite = false,
            createdAt = System.currentTimeMillis(),
            renderingMode = request.translationMode,
            renderingFidelity = request.fidelity,
            renderingYoungerVariant = request.youngerVariant
        )

        return if (request.translationMode == TranslationMode.TRANSLATE) {
            val records = validatedHistoricalResults(input, request, historicalBundle)
            translationRepository.saveUserQuoteWithTranslations(quote, records)
            SaveTranslationResult(message = "Saved translation to library")
        } else {
            quoteRepository.saveUserQuote(quote)
            SaveTranslationResult(message = "Saved to library")
        }
    }

    private fun validatedHistoricalResults(
        input: String,
        request: SaveTranslationRequest,
        bundle: HistoricalTranslationBundle
    ): List<TranslationResult> {
        check(bundle.errorMessage == null) { "Could not prepare historical translations." }
        val results = RunicScript.entries.map { script ->
            val result = checkNotNull(bundle.resultFor(script)) { "Missing prepared translation for $script." }
            val expectedVariant = if (script == RunicScript.YOUNGER_FUTHARK) request.youngerVariant.name else null
            check(result.sourceText == input && result.script == script && result.fidelity == request.fidelity &&
                result.requestedVariant == expectedVariant) {
                "Prepared translation does not match the save selection."
            }
            check(result.confidence.isFinite() && result.confidence in 0f..1f) { "Invalid translation confidence." }
            if (result.resolutionStatus != TranslationResolutionStatus.UNAVAILABLE) {
                check(result.glyphOutput.isNotBlank() && result.unresolvedTokens.isEmpty()) {
                    "Prepared translation is incomplete."
                }
            }
            result
        }
        val selected = results.first { it.script == request.selectedScript }
        check(selected.resolutionStatus != TranslationResolutionStatus.UNAVAILABLE) {
            "The selected historical translation is unavailable."
        }
        return results.filter { it.resolutionStatus != TranslationResolutionStatus.UNAVAILABLE }
    }

    private companion object {
        const val DEFAULT_TRANSLATION_AUTHOR = "Runatal"
    }
}
