package com.po4yka.runatal.domain.usecase.quote

import com.google.common.truth.Truth.assertThat
import com.po4yka.runatal.domain.model.Quote
import com.po4yka.runatal.domain.model.RunicScript
import com.po4yka.runatal.domain.repository.TranslationRepository
import com.po4yka.runatal.domain.translation.HistoricalStage
import com.po4yka.runatal.domain.translation.TranslationFidelity
import com.po4yka.runatal.domain.translation.TranslationMode
import com.po4yka.runatal.domain.translation.TranslationProvenanceEntry
import com.po4yka.runatal.domain.translation.TranslationResolutionStatus
import com.po4yka.runatal.domain.translation.TranslationResult
import com.po4yka.runatal.domain.translation.TranslationTokenBreakdown
import com.po4yka.runatal.domain.translation.YoungerFutharkVariant
import com.po4yka.runatal.domain.transliteration.CirthTransliterator
import com.po4yka.runatal.domain.transliteration.ElderFutharkTransliterator
import com.po4yka.runatal.domain.transliteration.TransliterationFactory
import com.po4yka.runatal.domain.transliteration.YoungerFutharkTransliterator
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ResolveQuoteRenderingUseCaseTest {
    private val factory = TransliterationFactory(
        ElderFutharkTransliterator(), YoungerFutharkTransliterator(), CirthTransliterator()
    )
    private val repository = mockk<TranslationRepository>()
    private val resolve = ResolveQuoteRenderingUseCase(factory, repository)
    private val source = Quote(1L, "king hunts", "Author", null, null, null)
    private val selected = source.copy(renderingMode = TranslationMode.TRANSLATE,
        renderingFidelity = TranslationFidelity.READABLE, renderingYoungerVariant = YoungerFutharkVariant.SHORT_TWIG)

    @Test
    fun `ordinary quotes remain direct without querying any cache`() = runTest {
        RunicScript.entries.forEach { script ->
            val output = resolve(source, script)
            assertThat(output.glyphOutput).isEqualTo(factory.transliterate(source.textLatin, script))
            assertThat(output.wordBreakdown.map { it.runicToken }.joinToString(" ")).isEqualTo(output.glyphOutput)
            assertThat(output.mode).isEqualTo(TranslationMode.TRANSLITERATE)
            assertThat(output.resolutionStatus).isNull()
        }
        coVerify(exactly = 0) { repository.getCachedTranslation(any(), any(), any(), any(), any()) }
        coVerify(exactly = 0) { repository.translateAndCache(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `custom stored glyphs are preserved without a fabricated generated word alignment`() = runTest {
        val custom = source.copy(runicElder = "My own glyph arrangement")
        val output = resolve(custom, RunicScript.ELDER_FUTHARK)
        assertThat(output.glyphOutput).isEqualTo(custom.runicElder)
        assertThat(output.wordBreakdown).isEmpty()
        assertThat(output.label).contains("Custom stored glyphs")
        assertThat(output.notes).isNotEmpty()
    }

    @Test
    fun `direct short twig selection is consistent across glyphs and word breakdown`() = runTest {
        val quote = source.copy(runicYounger = factory.transliterate(source.textLatin, RunicScript.YOUNGER_FUTHARK),
            renderingYoungerVariant = YoungerFutharkVariant.SHORT_TWIG)
        val output = resolve(quote, RunicScript.YOUNGER_FUTHARK)
        assertThat(output.glyphOutput).isEqualTo("ᚴᛁᚿ ᚽᚢᚿᛐᛌ")
        assertThat(output.wordBreakdown.map { it.runicToken }.joinToString(" ")).isEqualTo(output.glyphOutput)
        assertThat(output.label).contains("Short-twig")
    }

    @Test
    fun `historical selection requests exact fidelity variant and source while retaining sources`() = runTest {
        val result = result()
        coEvery { repository.getCachedTranslation(1L, RunicScript.YOUNGER_FUTHARK, source.textLatin,
            TranslationFidelity.READABLE, YoungerFutharkVariant.SHORT_TWIG) } returns result
        val output = resolve(selected, RunicScript.YOUNGER_FUTHARK)
        assertThat(output.glyphOutput).isEqualTo(result.glyphOutput)
        assertThat(output.resolutionStatus).isEqualTo(TranslationResolutionStatus.RECONSTRUCTED)
        assertThat(output.provenance).isEqualTo(result.provenance)
        assertThat(output.wordBreakdown.single().runicToken).isEqualTo(result.glyphOutput)
        assertThat(output.label).contains("Readable")
        assertThat(output.label).contains("Short-twig")
        coVerify(exactly = 0) { repository.translateAndCache(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `missing selected cache recomputes the same selection as automatic output without choosing intent`() = runTest {
        coEvery { repository.getCachedTranslation(any(), any(), any(), any(), any()) } returns null
        coEvery { repository.translateAndCache(1L, source.textLatin, RunicScript.YOUNGER_FUTHARK,
            TranslationFidelity.READABLE, YoungerFutharkVariant.SHORT_TWIG, true) } returns result()
        val output = resolve(selected, RunicScript.YOUNGER_FUTHARK)
        assertThat(output.isAvailable).isTrue()
        coVerify { repository.translateAndCache(1L, source.textLatin, RunicScript.YOUNGER_FUTHARK,
            TranslationFidelity.READABLE, YoungerFutharkVariant.SHORT_TWIG, true) }
    }

    @Test
    fun `unavailable selected historical output remains unavailable even when direct glyphs exist`() = runTest {
        val result = result().copy(resolutionStatus = TranslationResolutionStatus.UNAVAILABLE,
            glyphOutput = "", notes = listOf("Unsupported syntax"))
        coEvery { repository.getCachedTranslation(any(), any(), any(), any(), any()) } returns result
        val output = resolve(selected.copy(runicYounger = "stored direct"), RunicScript.YOUNGER_FUTHARK)
        assertThat(output.glyphOutput).isEmpty()
        assertThat(output.wordBreakdown).isEmpty()
        assertThat(output.isAvailable).isFalse()
        assertThat(output.label).contains("Unavailable")
        assertThat(output.provenance).isEqualTo(result.provenance)
        assertThat(output.notes).contains("Unsupported syntax")
    }

    @Test
    fun `mismatched cached and prepared selections cannot display foreign source or variant`() = runTest {
        coEvery { repository.getCachedTranslation(any(), any(), any(), any(), any()) } returns
            result().copy(sourceText = "Changed source")
        coEvery { repository.translateAndCache(any(), any(), any(), any(), any(), any()) } returns
            result().copy(requestedVariant = YoungerFutharkVariant.LONG_BRANCH.name)
        val output = resolve(selected, RunicScript.YOUNGER_FUTHARK)
        assertThat(output.glyphOutput).isEmpty()
        assertThat(output.resolutionStatus).isEqualTo(TranslationResolutionStatus.UNAVAILABLE)
        assertThat(output.provenance).isEmpty()
    }

    @Test
    fun `historical layout without matching token glyphs cannot fabricate a word breakdown`() = runTest {
        val mismatched = result().copy(tokenBreakdown = listOf(TranslationTokenBreakdown(
            "king", "konungr", "kununkr", "different glyphs"
        )))
        coEvery { repository.getCachedTranslation(any(), any(), any(), any(), any()) } returns mismatched
        val output = resolve(selected, RunicScript.YOUNGER_FUTHARK)
        assertThat(output.glyphOutput).isEqualTo(mismatched.glyphOutput)
        assertThat(output.isAvailable).isTrue()
        assertThat(output.wordBreakdown).isEmpty()
    }

    private fun result() = TranslationResult(
        sourceText = source.textLatin, script = RunicScript.YOUNGER_FUTHARK, fidelity = TranslationFidelity.READABLE,
        historicalStage = HistoricalStage.OLD_NORSE, normalizedForm = "konungr veiðir",
        diplomaticForm = "kununkr uiþir",
        glyphOutput = "ᚴᚢᚿᚢᚿᚴᚱ ᚢᛁᚦᛁᚱ", requestedVariant = YoungerFutharkVariant.SHORT_TWIG.name,
        resolutionStatus = TranslationResolutionStatus.RECONSTRUCTED, confidence = 0.9f,
        provenance = listOf(TranslationProvenanceEntry("primary", label = "Primary forms", role = "Forms",
            license = "Reference", url = "https://example.org/forms")),
        tokenBreakdown = listOf(TranslationTokenBreakdown(source.textLatin, "konungr veiðir", "kununkr uiþir",
            "ᚴᚢᚿᚢᚿᚴᚱ ᚢᛁᚦᛁᚱ")), engineVersion = "current", datasetVersion = "current"
    )
}
