package com.po4yka.runicquotes.domain.usecase.translation

import com.po4yka.runicquotes.data.repository.QuoteRepository
import com.po4yka.runicquotes.data.repository.TranslationRepository
import com.po4yka.runicquotes.domain.model.Quote
import com.po4yka.runicquotes.domain.model.RunicScript
import com.po4yka.runicquotes.domain.translation.TranslationFidelity
import com.po4yka.runicquotes.domain.translation.TranslationMode
import com.po4yka.runicquotes.domain.translation.YoungerFutharkVariant
import javax.inject.Inject

internal data class SaveTranslationRequest(
    val inputText: String,
    val translationMode: TranslationMode,
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
        val input = request.inputText.trim()
        val transliterationBundle = buildTransliterationBundleUseCase(input)
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
            runicElder = if (request.translationMode == TranslationMode.TRANSLATE) {
                historicalBundle.outputFor(RunicScript.ELDER_FUTHARK)
            } else {
                transliterationBundle.outputFor(RunicScript.ELDER_FUTHARK)
            },
            runicYounger = if (request.translationMode == TranslationMode.TRANSLATE) {
                historicalBundle.outputFor(RunicScript.YOUNGER_FUTHARK)
            } else {
                transliterationBundle.outputFor(RunicScript.YOUNGER_FUTHARK)
            },
            runicCirth = if (request.translationMode == TranslationMode.TRANSLATE) {
                historicalBundle.outputFor(RunicScript.CIRTH)
            } else {
                transliterationBundle.outputFor(RunicScript.CIRTH)
            },
            isUserCreated = true,
            isFavorite = false,
            createdAt = System.currentTimeMillis()
        )

        val quoteId = quoteRepository.saveUserQuote(quote)
        return if (request.translationMode == TranslationMode.TRANSLATE) {
            translationRepository.cacheTranslations(
                quoteId = quoteId,
                results = historicalBundle.results(),
                isBackfilled = false
            )
            SaveTranslationResult(message = "Saved translation to library")
        } else {
            SaveTranslationResult(message = "Saved to library")
        }
    }

    private companion object {
        const val DEFAULT_TRANSLATION_AUTHOR = "Runatal"
    }
}
