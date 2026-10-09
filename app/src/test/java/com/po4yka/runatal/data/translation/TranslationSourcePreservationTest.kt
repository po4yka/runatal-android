package com.po4yka.runatal.data.translation

import android.content.Context
import android.content.res.AssetManager
import com.google.common.truth.Truth.assertThat
import com.po4yka.runatal.domain.model.RunicScript
import com.po4yka.runatal.domain.translation.ElderFutharkTranslationEngine
import com.po4yka.runatal.domain.translation.EreborCirthTranslationEngine
import com.po4yka.runatal.domain.translation.HistoricalTranslationService
import com.po4yka.runatal.domain.translation.TranslationDerivationKind
import com.po4yka.runatal.domain.translation.TranslationEngineFactory
import com.po4yka.runatal.domain.translation.TranslationFidelity
import com.po4yka.runatal.domain.translation.TranslationResolutionStatus
import com.po4yka.runatal.domain.translation.YoungerFutharkTranslationEngine
import com.po4yka.runatal.domain.transliteration.CirthTransliterator
import com.po4yka.runatal.domain.transliteration.ElderFutharkTransliterator
import io.mockk.every
import io.mockk.mockk
import java.io.File
import org.junit.Test

/** Regression coverage uses the actual bundled datasets rather than a reduced lexicon fixture. */
class TranslationSourcePreservationTest {

    private val service = bundledService()

    @Test
    fun `strict engines never claim success after discarding unsupported source content`() {
        val sources = listOf("123", "Привет", "2 wolves", "wolf 😀", "café", "cafe\u0301")
        RunicScript.entries.forEach { script ->
            sources.forEach { source ->
                val result = service.translate(source, script, TranslationFidelity.STRICT)
                assertThat(result.sourceText).isEqualTo(source)
                assertThat(result.resolutionStatus).isEqualTo(TranslationResolutionStatus.UNAVAILABLE)
                assertThat(result.confidence).isEqualTo(0f)
                assertThat(result.glyphOutput).isEmpty()
                assertThat(result.unresolvedTokens).isNotEmpty()
            }
        }
    }

    @Test
    fun `Cirth preserves contractions as an explicit approximation including the apostrophe`() {
        listOf("don’t", "don't").forEach { source ->
            val result = service.translate(source, RunicScript.CIRTH, TranslationFidelity.READABLE)
            assertThat(result.resolutionStatus).isEqualTo(TranslationResolutionStatus.APPROXIMATED)
            assertThat(result.normalizedForm).isEqualTo("don't")
            assertThat(result.diplomaticForm).isEqualTo("don't")
            assertThat(result.glyphOutput).isEqualTo("\uE088\uE0B3\uE08B'\uE087")
            assertThat(result.tokenBreakdown.single().sourceToken).isEqualTo(source)
            assertThat(result.unresolvedTokens).isEmpty()
        }
    }

    @Test
    fun `readable and decorative engines preserve unsupported spans with explicit approximation`() {
        val source = "wolf 123 Привет 😀 cafe\u0301"
        RunicScript.entries.forEach { script ->
            listOf(TranslationFidelity.READABLE, TranslationFidelity.DECORATIVE).forEach { fidelity ->
                val result = service.translate(source, script, fidelity)
                assertThat(result.resolutionStatus).isEqualTo(TranslationResolutionStatus.APPROXIMATED)
                listOf("123", "Привет", "😀", "cafe\u0301").forEach { span ->
                    assertThat(result.normalizedForm).contains(span)
                    assertThat(result.glyphOutput).contains(span)
                    assertThat(result.tokenBreakdown.map { it.sourceToken }).contains(span)
                }
                assertThat(result.notes.joinToString()).contains("Unsupported source token")
            }
        }
    }

    @Test
    fun `empty whitespace and punctuation only input is unavailable in every engine and fidelity`() {
        RunicScript.entries.forEach { script ->
            TranslationFidelity.entries.forEach { fidelity ->
                listOf("", " \t\n\u00A0", "?", "...!?—()").forEach { source ->
                    val result = service.translate(source, script, fidelity)
                    assertThat(result.resolutionStatus).isEqualTo(TranslationResolutionStatus.UNAVAILABLE)
                    assertThat(result.glyphOutput).isEmpty()
                    assertThat(result.confidence).isEqualTo(0f)
                }
            }
        }
    }

    @Test
    fun `numeric literals and identifiers preserve exact source spelling in approximation modes`() {
        val sources = listOf("12.5", "12:30", "2026-10-09", "$12.50", "runatal42", "42runatal", "user_42")
        RunicScript.entries.forEach { script ->
            sources.forEach { source ->
                val strict = service.translate(source, script, TranslationFidelity.STRICT)
                assertThat(strict.resolutionStatus).isEqualTo(TranslationResolutionStatus.UNAVAILABLE)
                listOf(TranslationFidelity.READABLE, TranslationFidelity.DECORATIVE).forEach { fidelity ->
                    val result = service.translate(source, script, fidelity)
                    assertThat(result.resolutionStatus).isEqualTo(TranslationResolutionStatus.APPROXIMATED)
                    assertThat(result.glyphOutput).isEqualTo(source)
                    assertThat(result.normalizedForm).isEqualTo(source)
                    assertThat(result.tokenBreakdown.single().sourceToken).isEqualTo(source)
                }
            }
        }
    }

    @Test
    fun `invisible only input is unavailable while retaining source diagnostics`() {
        val sources = listOf("\u200B", "\u200D", "\uFEFF", "\u0000", "\u0301", "\u200B\u200D")
        RunicScript.entries.forEach { script ->
            TranslationFidelity.entries.forEach { fidelity ->
                sources.forEach { source ->
                    val result = service.translate(source, script, fidelity)
                    assertThat(result.sourceText).isEqualTo(source)
                    assertThat(result.resolutionStatus).isEqualTo(TranslationResolutionStatus.UNAVAILABLE)
                    assertThat(result.glyphOutput).isEmpty()
                    assertThat(result.confidence).isEqualTo(0f)
                    assertThat(result.unresolvedTokens).contains(source)
                }
            }
        }
    }

    @Test
    fun `visible text with a format character preserves the format source span explicitly`() {
        RunicScript.entries.forEach { script ->
            val result = service.translate("wolf\u200B", script, TranslationFidelity.READABLE)
            assertThat(result.resolutionStatus).isEqualTo(TranslationResolutionStatus.APPROXIMATED)
            assertThat(result.glyphOutput).contains("\u200B")
            assertThat(result.tokenBreakdown.map { it.sourceToken }).contains("\u200B")
        }
    }

    @Test
    fun `removing articles cannot produce empty successful younger translation`() {
        val result = service.translate("the a an.", RunicScript.YOUNGER_FUTHARK, TranslationFidelity.STRICT)

        assertThat(result.resolutionStatus).isEqualTo(TranslationResolutionStatus.UNAVAILABLE)
        assertThat(result.glyphOutput).isEmpty()
    }

    @Test
    fun `appending numbers or negation to a curated phrase cannot reuse its successful resolution`() {
        val source = "The wolf hunts at night"
        RunicScript.entries.forEach { script ->
            val original = service.translate(source, script, TranslationFidelity.STRICT)
            if (script == RunicScript.ELDER_FUTHARK) {
                assertThat(original.resolutionStatus).isEqualTo(TranslationResolutionStatus.UNAVAILABLE)
            } else {
                if (script == RunicScript.YOUNGER_FUTHARK) {
                    assertThat(original.derivationKind).isEqualTo(TranslationDerivationKind.TOKEN_COMPOSED)
                } else {
                    assertThat(original.resolutionStatus).isEqualTo(TranslationResolutionStatus.UNAVAILABLE)
                    assertThat(original.glyphOutput).isEmpty()
                }
            }
            listOf(" 2", " 😀").forEach { suffix ->
                val altered = service.translate(source + suffix, script, TranslationFidelity.STRICT)
                assertThat(altered.resolutionStatus).isEqualTo(TranslationResolutionStatus.UNAVAILABLE)
                assertThat(altered.glyphOutput).isEmpty()
            }
        }
        val negated = service.translate("wolf does not hunt", RunicScript.YOUNGER_FUTHARK)
        assertThat(negated.resolutionStatus).isEqualTo(TranslationResolutionStatus.UNAVAILABLE)
        assertThat(negated.unresolvedTokens).contains("not")
    }

    private fun bundledService(): HistoricalTranslationService {
        val directory = sequenceOf(
            File("build/generated/translationAssets/translation"),
            File("app/build/generated/translationAssets/translation"),
            File("src/main/translationSeed/translation"),
            File("app/src/main/translationSeed/translation")
        ).first { it.isDirectory }
        val context = mockk<Context>()
        val assets = mockk<AssetManager>()
        every { context.assets } returns assets
        every { assets.open(any()) } answers {
            directory.resolve(firstArg<String>().removePrefix("translation/")).inputStream()
        }
        val dataset = AssetTranslationDatasetProvider(context)
        return HistoricalTranslationService(
            TranslationEngineFactory(
                ElderFutharkTranslationEngine(dataset, dataset, ElderFutharkTransliterator()),
                YoungerFutharkTranslationEngine(dataset, dataset),
                EreborCirthTranslationEngine(dataset, dataset, CirthTransliterator())
            )
        )
    }
}
