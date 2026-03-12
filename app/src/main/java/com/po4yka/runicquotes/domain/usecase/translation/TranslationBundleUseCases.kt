package com.po4yka.runicquotes.domain.usecase.translation

import com.po4yka.runicquotes.domain.model.RunicScript
import com.po4yka.runicquotes.domain.transliteration.TransliterationBreakdown
import com.po4yka.runicquotes.domain.transliteration.TransliterationFactory
import com.po4yka.runicquotes.domain.transliteration.WordTransliterationPair
import com.po4yka.runicquotes.domain.translation.HistoricalTranslationService
import com.po4yka.runicquotes.domain.translation.TranslationFidelity
import com.po4yka.runicquotes.domain.translation.TranslationResult
import com.po4yka.runicquotes.domain.translation.TranslationTokenBreakdown
import com.po4yka.runicquotes.domain.translation.YoungerFutharkVariant
import javax.inject.Inject

internal data class TransliterationBundle(
    val elder: String = "",
    val younger: String = "",
    val cirth: String = "",
    val elderBreakdown: TransliterationBreakdown = TransliterationBreakdown(),
    val youngerBreakdown: TransliterationBreakdown = TransliterationBreakdown(),
    val cirthBreakdown: TransliterationBreakdown = TransliterationBreakdown(),
    val errorMessage: String? = null
) {
    fun outputFor(script: RunicScript): String = when (script) {
        RunicScript.ELDER_FUTHARK -> elder
        RunicScript.YOUNGER_FUTHARK -> younger
        RunicScript.CIRTH -> cirth
    }

    fun breakdownFor(script: RunicScript): TransliterationBreakdown = when (script) {
        RunicScript.ELDER_FUTHARK -> elderBreakdown
        RunicScript.YOUNGER_FUTHARK -> youngerBreakdown
        RunicScript.CIRTH -> cirthBreakdown
    }
}

internal data class HistoricalTranslationBundle(
    val elder: TranslationResult? = null,
    val younger: TranslationResult? = null,
    val cirth: TranslationResult? = null,
    val errorMessage: String? = null
) {
    fun outputFor(script: RunicScript): String = resultFor(script)?.glyphOutput.orEmpty()

    fun resultFor(script: RunicScript): TranslationResult? = when (script) {
        RunicScript.ELDER_FUTHARK -> elder
        RunicScript.YOUNGER_FUTHARK -> younger
        RunicScript.CIRTH -> cirth
    }

    fun results(): List<TranslationResult> = listOfNotNull(elder, younger, cirth)
}

internal class BuildTransliterationBundleUseCase @Inject constructor(
    private val transliterationFactory: TransliterationFactory
) {

    @Suppress("TooGenericExceptionCaught")
    operator fun invoke(inputText: String): TransliterationBundle {
        if (inputText.isBlank()) {
            return TransliterationBundle()
        }

        return try {
            TransliterationBundle(
                elder = transliterationFactory.transliterate(inputText, RunicScript.ELDER_FUTHARK),
                younger = transliterationFactory.transliterate(inputText, RunicScript.YOUNGER_FUTHARK),
                cirth = transliterationFactory.transliterate(inputText, RunicScript.CIRTH),
                elderBreakdown = transliterationFactory.transliterateWordByWord(
                    inputText,
                    RunicScript.ELDER_FUTHARK
                ),
                youngerBreakdown = transliterationFactory.transliterateWordByWord(
                    inputText,
                    RunicScript.YOUNGER_FUTHARK
                ),
                cirthBreakdown = transliterationFactory.transliterateWordByWord(
                    inputText,
                    RunicScript.CIRTH
                )
            )
        } catch (exception: Exception) {
            TransliterationBundle(errorMessage = exception.message ?: "Transliteration failed")
        }
    }
}

internal class BuildHistoricalTranslationBundleUseCase @Inject constructor(
    private val historicalTranslationService: HistoricalTranslationService
) {

    @Suppress("TooGenericExceptionCaught")
    operator fun invoke(
        inputText: String,
        fidelity: TranslationFidelity,
        youngerVariant: YoungerFutharkVariant
    ): HistoricalTranslationBundle {
        if (inputText.isBlank()) {
            return HistoricalTranslationBundle()
        }

        return try {
            HistoricalTranslationBundle(
                elder = historicalTranslationService.translate(
                    text = inputText,
                    script = RunicScript.ELDER_FUTHARK,
                    fidelity = fidelity,
                    youngerVariant = youngerVariant
                ),
                younger = historicalTranslationService.translate(
                    text = inputText,
                    script = RunicScript.YOUNGER_FUTHARK,
                    fidelity = fidelity,
                    youngerVariant = youngerVariant
                ),
                cirth = historicalTranslationService.translate(
                    text = inputText,
                    script = RunicScript.CIRTH,
                    fidelity = fidelity,
                    youngerVariant = youngerVariant
                )
            )
        } catch (exception: Exception) {
            HistoricalTranslationBundle(errorMessage = exception.message ?: "Historical translation failed")
        }
    }
}

internal fun String.glyphCount(): Int = count { character -> !character.isWhitespace() }

internal fun List<WordTransliterationPair>.toTranslationBreakdown(): List<TranslationTokenBreakdown> {
    return map { pair ->
        TranslationTokenBreakdown(
            sourceToken = pair.sourceToken,
            normalizedToken = pair.sourceToken,
            diplomaticToken = pair.runicToken,
            glyphToken = pair.runicToken
        )
    }
}

internal fun List<TranslationTokenBreakdown>.toWordPairs(): List<WordTransliterationPair> {
    return map { token ->
        WordTransliterationPair(
            sourceToken = token.sourceToken,
            runicToken = token.glyphToken
        )
    }
}
