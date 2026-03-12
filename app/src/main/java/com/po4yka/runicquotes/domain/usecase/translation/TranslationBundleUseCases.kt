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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
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
    suspend operator fun invoke(
        inputText: String,
        scripts: Set<RunicScript> = RunicScript.entries.toSet()
    ): TransliterationBundle {
        if (inputText.isBlank()) {
            return TransliterationBundle()
        }

        return try {
            val outputs = mutableMapOf<RunicScript, String>()
            val breakdowns = mutableMapOf<RunicScript, TransliterationBreakdown>()

            for (script in scripts) {
                currentCoroutineContext().ensureActive()
                outputs[script] = transliterationFactory.transliterate(inputText, script)
                breakdowns[script] = transliterationFactory.transliterateWordByWord(inputText, script)
            }

            TransliterationBundle(
                elder = outputs[RunicScript.ELDER_FUTHARK].orEmpty(),
                younger = outputs[RunicScript.YOUNGER_FUTHARK].orEmpty(),
                cirth = outputs[RunicScript.CIRTH].orEmpty(),
                elderBreakdown = breakdowns[RunicScript.ELDER_FUTHARK] ?: TransliterationBreakdown(),
                youngerBreakdown = breakdowns[RunicScript.YOUNGER_FUTHARK] ?: TransliterationBreakdown(),
                cirthBreakdown = breakdowns[RunicScript.CIRTH] ?: TransliterationBreakdown()
            )
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            TransliterationBundle(errorMessage = exception.message ?: "Transliteration failed")
        }
    }
}

internal class BuildHistoricalTranslationBundleUseCase @Inject constructor(
    private val historicalTranslationService: HistoricalTranslationService
) {

    @Suppress("TooGenericExceptionCaught")
    suspend operator fun invoke(
        inputText: String,
        fidelity: TranslationFidelity,
        youngerVariant: YoungerFutharkVariant,
        scripts: Set<RunicScript> = RunicScript.entries.toSet()
    ): HistoricalTranslationBundle {
        if (inputText.isBlank()) {
            return HistoricalTranslationBundle()
        }

        return try {
            val results = mutableMapOf<RunicScript, TranslationResult>()

            for (script in scripts) {
                currentCoroutineContext().ensureActive()
                results[script] = historicalTranslationService.translate(
                    text = inputText,
                    script = script,
                    fidelity = fidelity,
                    youngerVariant = youngerVariant
                )
            }

            HistoricalTranslationBundle(
                elder = results[RunicScript.ELDER_FUTHARK],
                younger = results[RunicScript.YOUNGER_FUTHARK],
                cirth = results[RunicScript.CIRTH]
            )
        } catch (exception: CancellationException) {
            throw exception
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
