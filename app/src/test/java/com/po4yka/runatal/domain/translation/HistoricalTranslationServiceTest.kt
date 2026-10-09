package com.po4yka.runatal.domain.translation

import com.google.common.truth.Truth.assertThat
import com.po4yka.runatal.data.translation.bundledTranslationTestData
import com.po4yka.runatal.domain.model.RunicScript
import com.po4yka.runatal.domain.transliteration.CirthTransliterator
import com.po4yka.runatal.domain.transliteration.ElderFutharkTransliterator
import org.junit.Test

class HistoricalTranslationServiceTest {

    private val dataset = bundledTranslationTestData()
    private val service = HistoricalTranslationService(
        TranslationEngineFactory(
            elderEngine = ElderFutharkTranslationEngine(
                lexiconStore = dataset,
                runicCorpusStore = dataset,
                elderTransliterator = ElderFutharkTransliterator()
            ),
            youngerEngine = YoungerFutharkTranslationEngine(
                lexiconStore = dataset,
                runicCorpusStore = dataset
            ),
            cirthEngine = EreborCirthTranslationEngine(
                runicCorpusStore = dataset,
                ereborStore = dataset,
                cirthTransliterator = CirthTransliterator()
            )
        )
    )

    @Test
    fun `strict younger translation applies cited grammar without a corpus bypass`() {
        val result = service.translate(
            text = "The wolf hunts at night",
            script = RunicScript.YOUNGER_FUTHARK
        )

        assertThat(result.derivationKind).isEqualTo(TranslationDerivationKind.TOKEN_COMPOSED)
        assertThat(result.historicalStage).isEqualTo(HistoricalStage.OLD_NORSE)
        assertThat(result.normalizedForm).isEqualTo("úlfrinn veiðir um nótt")
        assertThat(result.glyphOutput).contains("ᚢᛚᚠᚱ")
        assertThat(result.provenance.map { it.sourceId }).contains("barnes_nion")
        assertThat(result.datasetVersion).isEqualTo(dataset.datasetManifest().version)
    }

    @Test
    fun `younger location clause uses cited dative grammar instead of the legacy template`() {
        val result = service.translate(
            text = "The king walks under the mountain",
            script = RunicScript.YOUNGER_FUTHARK,
            fidelity = TranslationFidelity.STRICT
        )

        assertThat(result.resolutionStatus).isEqualTo(TranslationResolutionStatus.RECONSTRUCTED)
        assertThat(result.derivationKind).isEqualTo(TranslationDerivationKind.TOKEN_COMPOSED)
        assertThat(result.normalizedForm).isEqualTo("konungrinn gengr undir fjallinu")
        assertThat(result.notes.joinToString()).contains("under as spatial location")
    }

    @Test
    fun `strict elder translation uses curated attested short formula when available`() {
        val result = service.translate(
            text = "The king",
            script = RunicScript.ELDER_FUTHARK,
            fidelity = TranslationFidelity.STRICT
        )

        assertThat(result.derivationKind).isEqualTo(TranslationDerivationKind.PHRASE_TEMPLATE)
        assertThat(result.resolutionStatus).isEqualTo(TranslationResolutionStatus.ATTESTED)
        assertThat(result.normalizedForm).isEqualTo("kuningaz")
        assertThat(result.glyphOutput).isEqualTo("ᚲᚢᚾᛁᛜᚨᛉ")
        assertThat(result.provenance.single().referenceId).isEqualTo("ef_ref_kuningaz")
    }

    @Test
    fun `strict elder translation returns unavailable for unsupported words`() {
        val result = service.translate(
            text = "signal",
            script = RunicScript.ELDER_FUTHARK,
            fidelity = TranslationFidelity.STRICT
        )

        assertThat(result.resolutionStatus).isEqualTo(TranslationResolutionStatus.UNAVAILABLE)
        assertThat(result.unresolvedTokens).containsExactly("signal")
        assertThat(result.glyphOutput).isEmpty()
        assertThat(result.notes.single()).contains("Missing attested or reconstructed Elder Futhark pattern")
    }

    @Test
    fun `readable elder translation falls back with approximation note`() {
        val result = service.translate(
            text = "signal",
            script = RunicScript.ELDER_FUTHARK,
            fidelity = TranslationFidelity.READABLE
        )

        assertThat(result.derivationKind).isEqualTo(TranslationDerivationKind.TOKEN_COMPOSED)
        assertThat(result.resolutionStatus).isEqualTo(TranslationResolutionStatus.APPROXIMATED)
        assertThat(result.notes.joinToString()).contains("phonological preservation")
        assertThat(result.glyphOutput).isNotEmpty()
    }

    @Test
    fun `cirth phrase mapping is preferred over sequence transcription when curated`() {
        val result = service.translate(
            text = "Under the mountain",
            script = RunicScript.CIRTH,
            fidelity = TranslationFidelity.STRICT
        )

        assertThat(result.derivationKind).isEqualTo(TranslationDerivationKind.PHRASE_TEMPLATE)
        assertThat(result.diplomaticForm).isEqualTo("u·n·d·e·r th·e m·ou·n·t·ai·n")
        assertThat(result.provenance.single().referenceId).isEqualTo("cirth_ref_under_mountain")
    }

    @Test
    fun `cirth sequence transcription records expanded orthography handling`() {
        val result = service.translate(
            text = "night",
            script = RunicScript.CIRTH,
            fidelity = TranslationFidelity.STRICT
        )

        assertThat(result.derivationKind).isEqualTo(TranslationDerivationKind.SEQUENCE_TRANSCRIPTION)
        assertThat(result.diplomaticForm).contains("gh")
        assertThat(result.notes.joinToString()).contains("sequence-table transcription")
        assertThat(result.provenance.single().sourceId).isEqualTo("tolkien_appendix_e")
    }

    @Test
    fun `strict younger translation returns unavailable when lemma coverage is missing`() {
        val result = service.translate(
            text = "satellite",
            script = RunicScript.YOUNGER_FUTHARK,
            fidelity = TranslationFidelity.STRICT
        )

        assertThat(result.resolutionStatus).isEqualTo(TranslationResolutionStatus.UNAVAILABLE)
        assertThat(result.unresolvedTokens).containsExactly("satellite")
        assertThat(result.notes.single()).contains("missing Old Norse lemma")
        assertThat(result.glyphOutput).isEmpty()
    }

    @Test
    fun `readable younger translation preserves unknown tokens phonetically`() {
        val result = service.translate(
            text = "satellite",
            script = RunicScript.YOUNGER_FUTHARK,
            fidelity = TranslationFidelity.READABLE
        )

        assertThat(result.derivationKind).isEqualTo(TranslationDerivationKind.TOKEN_COMPOSED)
        assertThat(result.resolutionStatus).isEqualTo(TranslationResolutionStatus.APPROXIMATED)
        assertThat(result.notes.joinToString()).contains("Readable mode preserved 'satellite' phonetically")
        assertThat(result.provenance.single().sourceId).isEqualTo("internal_heuristics")
        assertThat(result.glyphOutput).isNotEmpty()
    }

    @Test
    fun `younger token composed translation changes glyphs by branch variant only`() {
        val longBranch = service.translate(
            text = "great king",
            script = RunicScript.YOUNGER_FUTHARK,
            fidelity = TranslationFidelity.STRICT,
            youngerVariant = YoungerFutharkVariant.LONG_BRANCH
        )
        val shortTwig = service.translate(
            text = "great king",
            script = RunicScript.YOUNGER_FUTHARK,
            fidelity = TranslationFidelity.STRICT,
            youngerVariant = YoungerFutharkVariant.SHORT_TWIG
        )

        assertThat(longBranch.derivationKind).isEqualTo(TranslationDerivationKind.TOKEN_COMPOSED)
        assertThat(shortTwig.derivationKind).isEqualTo(TranslationDerivationKind.TOKEN_COMPOSED)
        assertThat(longBranch.normalizedForm).isEqualTo(shortTwig.normalizedForm)
        assertThat(longBranch.diplomaticForm).isEqualTo(shortTwig.diplomaticForm)
        assertThat(longBranch.glyphOutput).isNotEqualTo(shortTwig.glyphOutput)
    }
}
