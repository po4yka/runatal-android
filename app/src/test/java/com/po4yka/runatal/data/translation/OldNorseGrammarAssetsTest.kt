package com.po4yka.runatal.data.translation

import com.google.common.truth.Truth.assertThat
import com.po4yka.runatal.domain.model.RunicScript
import com.po4yka.runatal.domain.translation.EnglishSyntaxParser
import com.po4yka.runatal.domain.translation.HistoricalSourceCatalog
import com.po4yka.runatal.domain.translation.HistoricalLexiconLookup
import com.po4yka.runatal.domain.translation.HistoricalLexiconStore
import com.po4yka.runatal.domain.translation.OldNorseGrammarStage
import com.po4yka.runatal.domain.translation.TranslationDerivationKind
import com.po4yka.runatal.domain.translation.TranslationFidelity
import com.po4yka.runatal.domain.translation.TranslationRequest
import com.po4yka.runatal.domain.translation.TranslationResolutionStatus
import com.po4yka.runatal.domain.translation.YoungerFutharkTranslationEngine
import com.po4yka.runatal.domain.translation.YoungerFutharkVariant
import org.junit.Test

/** Expected language forms are primary-source oracles, independent of the implementation's form tables. */
class OldNorseGrammarAssetsTest {
    private val data = bundledTranslationTestData()
    private val engine = YoungerFutharkTranslationEngine(data, data)

    @Test
    fun `supported fragments inflect person case gender and number before runic rendering`() {
        val examples = mapOf(
            "I hunt" to "ek veiði", "with king" to "með konungi", "great work" to "mikit verk",
            "mountains" to "fjǫll", "great king" to "mikill konungr", "great journey" to "mikil ferð",
            "great wolves" to "miklir úlfar", "great mountains" to "mikil fjǫll",
            "with great king" to "með miklum konungi", "with great journey" to "með mikilli ferð",
            "with great work" to "með miklu verki", "with great wolves" to "með miklum úlfum",
            "to king" to "til konungs", "to great mountains" to "til mikilla fjalla",
            "at night" to "um nótt", "at nights" to "um nætr"
        )
        examples.forEach { (source, expected) -> assertReconstruction(source, expected) }
        assertThat(translate("I hunt").glyphOutput).isEqualTo("ᛁᚴ ᚢᛁᚦᛁ")
        assertThat(translate("great work").glyphOutput).isEqualTo("ᛘᛁᚴᛁᛏ ᚢᛁᚱᚴ")
    }

    @Test
    fun `finite hunt forms agree with all six person and number combinations`() {
        val present = mapOf(
            "I hunt" to "ek veiði", "you hunt" to "þú veiðir", "he hunts" to "hann veiðir",
            "we hunt" to "vér veiðum", "you all hunt" to "þér veiðið", "they hunt" to "þeir veiða"
        )
        val past = mapOf(
            "I hunted" to "ek veidda", "you hunted" to "þú veiddir", "he hunted" to "hann veiddi",
            "we hunted" to "vér veiddum", "you all hunted" to "þér veidduð", "they hunted" to "þeir veiddu"
        )
        (present + past).forEach { (source, expected) -> assertReconstruction(source, expected) }
    }

    @Test
    fun `finite be forms agree with all six combinations and copular predicate nominatives`() {
        val examples = mapOf(
            "I am a king" to "ek em konungr", "you are a king" to "þú ert konungr",
            "he is a king" to "hann er konungr", "we are kings" to "vér erum konungar",
            "you all are kings" to "þér eruð konungar", "they are kings" to "þeir eru konungar",
            "I was a king" to "ek var konungr", "you were a king" to "þú vart konungr",
            "he was a king" to "hann var konungr", "we were kings" to "vér várum konungar",
            "you all were kings" to "þér váruð konungar", "they were kings" to "þeir váru konungar",
            "she is great" to "hon er mikil", "it is great" to "þat er mikit"
        )
        examples.forEach { (source, expected) -> assertReconstruction(source, expected) }
    }

    @Test
    fun `subject object and governed noun phrases receive distinct appropriate cases`() {
        val examples = mapOf(
            "king hunts wolf" to "konungr veiðir úlf",
            "wolves hunt kings" to "úlfar veiða konunga",
            "I hunt great king" to "ek veiði mikinn konung",
            "I hunt great wolves with king" to "ek veiði mikla úlfa með konungi",
            "I hunt with king at night" to "ek veiði með konungi um nótt",
            "The wolf hunts at night" to "úlfrinn veiðir um nótt",
            "I hunt the wolf" to "ek veiði úlfinn",
            "with the king" to "með konunginum",
            "to the mountains" to "til fjallanna",
            "he walks" to "hann gengr", "he walked" to "hann gekk"
        )
        examples.forEach { (source, expected) -> assertReconstruction(source, expected) }
    }

    @Test
    fun `locative under uses dative in standalone and finite clauses with definite nouns`() {
        val examples = mapOf(
            "under the mountain" to "undir fjallinu",
            "The king walks under the mountain" to "konungrinn gengr undir fjallinu",
            "under great mountain" to "undir miklu fjalli",
            "under the mountains" to "undir fjǫllunum"
        )
        examples.forEach { (source, expected) ->
            assertReconstruction(source, expected)
            assertThat(translate(source).notes.joinToString()).contains("under as spatial location")
        }
    }

    @Test
    fun `unsupported structures English disagreement and missing cited forms fail strictly`() {
        val failures = listOf(
            "I hunts", "wolves hunts", "I be", "we is kings", "I am kings", "king wolf", "with hunt",
            "great", "I walk", "I walked", "with difficulty", "at king", "into mountain",
            "I hunt and walk", "I do not hunt", "I hunt?", "king, wolf", "a mountains", "the the king",
            "the great king", "I am great", "The king walks towards under the mountain", "I hunt 12.5"
        )
        failures.forEach { source ->
            val result = translate(source)
            assertThat(result.resolutionStatus).isEqualTo(TranslationResolutionStatus.UNAVAILABLE)
            assertThat(result.glyphOutput).isEmpty()
            assertThat(result.confidence).isEqualTo(0f)
        }
    }

    @Test
    fun `fallback outside grammar is explicitly approximate and preserves unsupported spans`() {
        val result = engine.translate(request("I hunt 12.5", TranslationFidelity.READABLE))
        assertThat(result.resolutionStatus).isEqualTo(TranslationResolutionStatus.APPROXIMATED)
        assertThat(result.glyphOutput).contains("12.5")
        assertThat(result.tokenBreakdown.map { it.sourceToken }).contains("12.5")
        assertThat(engine.translate(request("love", TranslationFidelity.READABLE)).resolutionStatus)
            .isEqualTo(TranslationResolutionStatus.APPROXIMATED)
    }

    @Test
    fun `branches share grammar and diplomatic forms and produce only their own sixteen glyphs`() {
        val long = translate("I hunt great wolves with king")
        val short = engine.translate(
            request("I hunt great wolves with king", variant = YoungerFutharkVariant.SHORT_TWIG)
        )
        assertThat(long.normalizedForm).isEqualTo(short.normalizedForm)
        assertThat(long.diplomaticForm).isEqualTo(short.diplomaticForm)
        assertThat(short.glyphOutput).isNotEqualTo(long.glyphOutput)
        assertThat(long.glyphOutput.filterNot { it.isWhitespace() }.all { it in "ᚠᚢᚦᚬᚱᚴᚼᚾᛁᛅᛋᛏᛒᛘᛚᛦ" }).isTrue()
        assertThat(short.glyphOutput.filterNot { it.isWhitespace() }.all { it in "ᚠᚢᚦᚭᚱᚴᚽᚿᛁᛆᛌᛐᛓᛙᛚᛧ" }).isTrue()
    }

    @Test
    fun `a form table without citations cannot manufacture a reconstructed result`() {
        val lookup = HistoricalLexiconLookup(data, HistoricalSourceCatalog(data.sourceManifest()))
        val original = requireNotNull(lookup.oldNorseFor("wolf", TranslationFidelity.STRICT))
        val altered = original.copy(inflectionCitations = emptyList())
        val alteredData = object : HistoricalLexiconStore by data {
            override fun oldNorseLexicon() = data.oldNorseLexicon().map { if (it.id == altered.id) altered else it }
        }
        val stage = OldNorseGrammarStage(
            HistoricalLexiconLookup(alteredData, HistoricalSourceCatalog(data.sourceManifest()))
        )
        val result = stage.resolve(EnglishSyntaxParser().parse("wolves"))
        assertThat(result.single().resolutionStatus).isEqualTo(TranslationResolutionStatus.UNAVAILABLE)
    }

    private fun translate(text: String) = engine.translate(request(text))
    private fun request(
        text: String, fidelity: TranslationFidelity = TranslationFidelity.STRICT,
        variant: YoungerFutharkVariant = YoungerFutharkVariant.LONG_BRANCH
    ) = TranslationRequest(text, RunicScript.YOUNGER_FUTHARK, fidelity, variant)

    private fun assertReconstruction(source: String, expected: String) {
        val result = translate(source)
        assertThat(result.normalizedForm).isEqualTo(expected)
        assertThat(result.resolutionStatus).isEqualTo(TranslationResolutionStatus.RECONSTRUCTED)
        assertThat(result.derivationKind).isEqualTo(TranslationDerivationKind.TOKEN_COMPOSED)
        assertThat(result.glyphOutput).isNotEmpty()
        assertThat(result.provenance.any { it.sourceId in setOf("barnes_nion", "zoega") }).isTrue()
        assertThat(result.provenance.none { it.sourceId == "internal_heuristics" }).isTrue()
    }
}
