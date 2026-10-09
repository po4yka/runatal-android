package com.po4yka.runatal.domain.usecase.translation

import com.google.common.truth.Truth.assertThat
import com.po4yka.runatal.data.translation.bundledTranslationTestData
import com.po4yka.runatal.domain.model.Quote
import com.po4yka.runatal.domain.model.RunicScript
import com.po4yka.runatal.domain.repository.QuoteRepository
import com.po4yka.runatal.domain.repository.TranslationRepository
import com.po4yka.runatal.domain.translation.ElderFutharkTranslationEngine
import com.po4yka.runatal.domain.translation.EreborCirthTranslationEngine
import com.po4yka.runatal.domain.translation.HistoricalTranslationService
import com.po4yka.runatal.domain.translation.TranslationEngineFactory
import com.po4yka.runatal.domain.translation.TranslationFidelity
import com.po4yka.runatal.domain.translation.TranslationMode
import com.po4yka.runatal.domain.translation.TranslationResolutionStatus
import com.po4yka.runatal.domain.translation.TranslationResult
import com.po4yka.runatal.domain.translation.YoungerFutharkTranslationEngine
import com.po4yka.runatal.domain.translation.YoungerFutharkVariant
import com.po4yka.runatal.domain.transliteration.CirthTransliterator
import com.po4yka.runatal.domain.transliteration.ElderFutharkTransliterator
import com.po4yka.runatal.domain.transliteration.TransliterationFactory
import com.po4yka.runatal.domain.transliteration.YoungerFutharkTransliterator
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Test

class PreparedSourcePreviewTest {

    private val factory = TransliterationFactory(
        ElderFutharkTransliterator(), YoungerFutharkTransliterator(), CirthTransliterator()
    )
    private val dataset = bundledTranslationTestData()
    private val historical = BuildHistoricalTranslationBundleUseCase(
        HistoricalTranslationService(
            TranslationEngineFactory(
                ElderFutharkTranslationEngine(dataset, dataset, ElderFutharkTransliterator()),
                YoungerFutharkTranslationEngine(dataset, dataset),
                EreborCirthTranslationEngine(dataset, dataset, CirthTransliterator())
            )
        )
    )
    private val direct = BuildTransliterationBundleUseCase(factory)
    private val presentation = BuildTranslationPresentationUseCase(direct, historical)
    private val quotes = mockk<QuoteRepository>()
    private val translations = mockk<TranslationRepository>()
    private val save = SaveTranslationToLibraryUseCase(quotes, translations, direct, historical)

    @Test
    fun `direct preview equals saved output for every script and variant with raw draft retained`() = runTest {
        val source = "Wolf,  king!\tcafe\u0301 😀\u200B"
        val draft = " \t\n\u00A0$source\u2003\n "
        val saved = slot<Quote>()
        coEvery { quotes.saveUserQuote(capture(saved)) } returns 17L

        RunicScript.entries.forEach { script ->
            YoungerFutharkVariant.entries.forEach { variant ->
                val preview = preview(draft, script, TranslationMode.TRANSLITERATE, variant)
                save(request(draft, script, TranslationMode.TRANSLITERATE, variant))

                assertThat(preview.inputText).isEqualTo(draft)
                assertThat(preview.inputCharacterCount).isEqualTo(draft.length)
                assertThat(preview.canSave).isTrue()
                assertThat(saved.captured.textLatin).isEqualTo(source)
                assertThat(preview.transliteratedText).isEqualTo(factory.transliterate(source, script, variant))
                assertThat(preview.transliteratedText).isEqualTo(output(saved.captured, script))
                assertThat(preview.wordBreakdown.map { it.sourceToken })
                    .containsExactly("Wolf,", "king!", "cafe\u0301", "😀\u200B").inOrder()
            }
        }
    }

    @Test
    fun `actual supported historical preview equals saved result with the same prepared source`() = runTest {
        val saved = slot<Quote>()
        val records = slot<List<TranslationResult>>()
        coEvery { translations.saveUserQuoteWithTranslations(capture(saved), capture(records)) } returns 17L
        val sources = mapOf(
            RunicScript.ELDER_FUTHARK to "Hlewagastiz",
            RunicScript.YOUNGER_FUTHARK to "I hunt",
            RunicScript.CIRTH to "The Lord of the Rings translated from the Red Book"
        )

        sources.forEach { (script, source) ->
            YoungerFutharkVariant.entries.forEach { variant ->
                val draft = "\n\u00A0$source\u2003\t"
                val preview = preview(draft, script, TranslationMode.TRANSLATE, variant)
                save(request(draft, script, TranslationMode.TRANSLATE, variant))
                val selected = records.captured.single { it.script == script }

                assertThat(preview.canSave).isTrue()
                assertThat(preview.resolutionStatus).isNotEqualTo(TranslationResolutionStatus.UNAVAILABLE)
                assertThat(preview.transliteratedText).isNotEmpty()
                assertThat(preview.normalizedForm).isEqualTo(
                    when (script) {
                        RunicScript.ELDER_FUTHARK -> "hlewagastiz"
                        RunicScript.YOUNGER_FUTHARK -> "ek veiði"
                        RunicScript.CIRTH -> "the lord of the rings translated from the red book"
                    }
                )
                assertThat(preview.inputText).isEqualTo(draft)
                assertThat(saved.captured.textLatin).isEqualTo(source)
                assertThat(records.captured.map { it.sourceText }.distinct()).containsExactly(source)
                assertThat(preview.transliteratedText).isEqualTo(selected.glyphOutput)
                assertThat(preview.normalizedForm).isEqualTo(selected.normalizedForm)
                assertThat(preview.diplomaticForm).isEqualTo(selected.diplomaticForm)
                assertThat(preview.tokenBreakdown).isEqualTo(selected.tokenBreakdown)
                assertThat(preview.provenance).isEqualTo(selected.provenance)
            }
        }
    }

    @Test
    fun `prepared previews do not shrink the raw input validation budget`() = runTest {
        val draft = " " + "a".repeat(280)
        val preview = preview(draft, RunicScript.ELDER_FUTHARK, TranslationMode.TRANSLITERATE)

        assertThat(preview.inputCharacterCount).isEqualTo(281)
        assertThat(preview.canSave).isFalse()
        assertThat(preview.transliteratedText).isEqualTo("ᚨ".repeat(280))
        val failure = runCatching {
            save(request(draft, RunicScript.ELDER_FUTHARK, TranslationMode.TRANSLITERATE))
        }.exceptionOrNull()
        assertThat(failure).isInstanceOf(IllegalStateException::class.java)
    }

    private suspend fun preview(
        draft: String,
        script: RunicScript,
        mode: TranslationMode,
        variant: YoungerFutharkVariant = YoungerFutharkVariant.DEFAULT
    ) = presentation(
        TranslationPreferencesSnapshot(script, "noto", true, mode, TranslationFidelity.STRICT, variant),
        TranslationInputSnapshot(draft, false, null),
        translateFeatureEnabled = true
    )

    private fun request(
        draft: String,
        script: RunicScript,
        mode: TranslationMode,
        variant: YoungerFutharkVariant = YoungerFutharkVariant.DEFAULT
    ) = SaveTranslationRequest(draft, mode, script, TranslationFidelity.STRICT, variant)

    private fun output(quote: Quote, script: RunicScript): String? = when (script) {
        RunicScript.ELDER_FUTHARK -> quote.runicElder
        RunicScript.YOUNGER_FUTHARK -> quote.runicYounger
        RunicScript.CIRTH -> quote.runicCirth
    }
}
