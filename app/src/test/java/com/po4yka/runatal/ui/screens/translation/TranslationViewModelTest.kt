package com.po4yka.runatal.ui.screens.translation

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.po4yka.runatal.data.preferences.UserPreferences
import com.po4yka.runatal.data.preferences.UserPreferencesManager
import com.po4yka.runatal.domain.repository.QuoteRepository
import com.po4yka.runatal.domain.repository.TranslationRepository
import com.po4yka.runatal.domain.model.RunicScript
import com.po4yka.runatal.domain.transliteration.CirthTransliterator
import com.po4yka.runatal.domain.transliteration.ElderFutharkTransliterator
import com.po4yka.runatal.domain.transliteration.TransliterationFactory
import com.po4yka.runatal.domain.transliteration.WordTransliterationPair
import com.po4yka.runatal.domain.transliteration.YoungerFutharkTransliterator
import com.po4yka.runatal.domain.translation.HistoricalStage
import com.po4yka.runatal.domain.translation.HistoricalTranslationService
import com.po4yka.runatal.domain.translation.TranslationDerivationKind
import com.po4yka.runatal.domain.translation.TranslationFidelity
import com.po4yka.runatal.domain.translation.TranslationMode
import com.po4yka.runatal.domain.translation.TranslationProvenanceEntry
import com.po4yka.runatal.domain.translation.TranslationResolutionStatus
import com.po4yka.runatal.domain.translation.TranslationResult
import com.po4yka.runatal.domain.translation.TranslationTokenBreakdown
import com.po4yka.runatal.domain.translation.YoungerFutharkVariant
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TranslationViewModelTest {

    private lateinit var quoteRepository: QuoteRepository
    private lateinit var userPreferencesManager: UserPreferencesManager
    private lateinit var transliterationFactory: TransliterationFactory
    private lateinit var historicalTranslationService: HistoricalTranslationService
    private lateinit var translationRepository: TranslationRepository
    private lateinit var viewModel: TranslationViewModel

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var preferencesFlow: MutableStateFlow<UserPreferences>

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        quoteRepository = mockk(relaxed = true)
        userPreferencesManager = mockk(relaxed = true)
        historicalTranslationService = mockk(relaxed = true)
        translationRepository = mockk(relaxed = true)
        transliterationFactory = TransliterationFactory(
            elderFutharkTransliterator = ElderFutharkTransliterator(),
            youngerFutharkTransliterator = YoungerFutharkTransliterator(),
            cirthTransliterator = CirthTransliterator()
        )

        preferencesFlow = MutableStateFlow(
            UserPreferences(
                selectedScript = RunicScript.ELDER_FUTHARK,
                selectedFont = "babelstone",
                wordByWordTransliterationEnabled = false
            )
        )
        every { userPreferencesManager.userPreferencesFlow } returns preferencesFlow
        coEvery { userPreferencesManager.updateSelectedScript(any()) } coAnswers {
            preferencesFlow.value = preferencesFlow.value.copy(selectedScript = firstArg())
        }

        viewModel = TranslationViewModel(
            transliterationFactory = transliterationFactory,
            historicalTranslationService = historicalTranslationService,
            quoteRepository = quoteRepository,
            translationRepository = translationRepository,
            userPreferencesManager = userPreferencesManager,
            translationDispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `ui state reflects persisted word by word default and selected font`() = runTest {
        viewModel.uiState.test {
            awaitItem()

            preferencesFlow.value = preferencesFlow.value.copy(wordByWordTransliterationEnabled = true)
            advanceUntilIdle()

            val state = awaitItem()
            assertThat(state.wordByWordEnabled).isTrue()
            assertThat(state.selectedFont).isEqualTo("babelstone")

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `toggleWordByWordMode keeps local override when preferences change`() = runTest {
        viewModel.uiState.test {
            awaitItem()

            viewModel.updateInputText("rune stone")
            advanceUntilIdle()

            val initialTranslatedState = awaitItem()
            assertThat(initialTranslatedState.wordByWordEnabled).isFalse()

            viewModel.toggleWordByWordMode()
            advanceUntilIdle()

            val locallyOverridden = awaitItem()
            assertThat(locallyOverridden.wordByWordEnabled).isTrue()

            preferencesFlow.value = preferencesFlow.value.copy(
                selectedScript = RunicScript.YOUNGER_FUTHARK,
                wordByWordTransliterationEnabled = false
            )
            advanceUntilIdle()

            val state = awaitItem()
            assertThat(state.selectedScript).isEqualTo(RunicScript.YOUNGER_FUTHARK)
            assertThat(state.wordByWordEnabled).isTrue()

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `word breakdown updates with input and script changes`() = runTest {
        viewModel.uiState.test {
            awaitItem()

            viewModel.updateInputText("thing change")
            advanceUntilIdle()

            val elderState = awaitItem()
            assertThat(elderState.wordBreakdown).containsExactly(
                WordTransliterationPair(
                    sourceToken = "thing",
                    runicToken = transliterationFactory.transliterate("thing", RunicScript.ELDER_FUTHARK)
                ),
                WordTransliterationPair(
                    sourceToken = "change",
                    runicToken = transliterationFactory.transliterate("change", RunicScript.ELDER_FUTHARK)
                )
            ).inOrder()

            viewModel.selectScript(RunicScript.CIRTH)
            advanceUntilIdle()

            val cirthState = awaitItem()
            assertThat(cirthState.wordBreakdown).containsExactly(
                WordTransliterationPair(
                    sourceToken = "thing",
                    runicToken = transliterationFactory.transliterate("thing", RunicScript.CIRTH)
                ),
                WordTransliterationPair(
                    sourceToken = "change",
                    runicToken = transliterationFactory.transliterate("change", RunicScript.CIRTH)
                )
            ).inOrder()
            assertThat(cirthState.wordBreakdown).isNotEqualTo(elderState.wordBreakdown)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `translate mode surfaces derivation kind and Erebor track label`() = runTest {
        every {
            historicalTranslationService.translate(any(), any(), any(), any())
        } answers {
            when (secondArg<RunicScript>()) {
                RunicScript.CIRTH -> TranslationResult(
                    sourceText = firstArg(),
                    script = RunicScript.CIRTH,
                    fidelity = thirdArg(),
                    derivationKind = TranslationDerivationKind.SEQUENCE_TRANSCRIPTION,
                    historicalStage = HistoricalStage.EREBOR_ENGLISH,
                    normalizedForm = "night",
                    diplomaticForm = "n·i·gh·t",
                    glyphOutput = "",
                    resolutionStatus = TranslationResolutionStatus.RECONSTRUCTED,
                    confidence = 0.72f,
                    provenance = listOf(
                        TranslationProvenanceEntry(
                            sourceId = "tolkien_appendix_e",
                            referenceId = "cirth_ref_night",
                            label = "Appendix E",
                            role = "Cirth transcription reference",
                            license = "Reference only"
                        )
                    ),
                    tokenBreakdown = listOf(
                        TranslationTokenBreakdown(
                            sourceToken = "night",
                            normalizedToken = "night",
                            diplomaticToken = "n·i·gh·t",
                            glyphToken = "",
                            resolutionStatus = TranslationResolutionStatus.RECONSTRUCTED
                        )
                    ),
                    engineVersion = "engine",
                    datasetVersion = "dataset"
                )

                RunicScript.YOUNGER_FUTHARK -> placeholderTranslation(
                    text = firstArg(),
                    script = RunicScript.YOUNGER_FUTHARK
                )

                RunicScript.ELDER_FUTHARK -> placeholderTranslation(
                    text = firstArg(),
                    script = RunicScript.ELDER_FUTHARK
                )
            }
        }

        val collector = launch { viewModel.uiState.collect { } }
        viewModel.selectMode(TranslationMode.TRANSLATE)
        viewModel.selectScript(RunicScript.CIRTH)
        viewModel.updateInputText("night")
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.translationTrackLabel).isEqualTo("Erebor transcription")
        assertThat(viewModel.uiState.value.derivationKindLabel).isEqualTo("Sequence transcription")
        collector.cancel()
    }

    @Test
    fun `translate mode only computes the selected script`() = runTest {
        every {
            historicalTranslationService.translate(any(), any(), any(), any())
        } answers {
            placeholderTranslation(
                text = firstArg(),
                script = secondArg()
            )
        }

        val collector = launch { viewModel.uiState.collect { } }
        viewModel.selectMode(TranslationMode.TRANSLATE)
        viewModel.selectScript(RunicScript.CIRTH)
        viewModel.updateInputText("night")
        advanceUntilIdle()

        verify(exactly = 1) {
            historicalTranslationService.translate(
                "night",
                RunicScript.CIRTH,
                TranslationFidelity.DEFAULT,
                YoungerFutharkVariant.DEFAULT
            )
        }
        verify(exactly = 0) {
            historicalTranslationService.translate("night", RunicScript.ELDER_FUTHARK, any(), any())
        }
        verify(exactly = 0) {
            historicalTranslationService.translate("night", RunicScript.YOUNGER_FUTHARK, any(), any())
        }
        collector.cancel()
    }

    @Test
    fun `translate mode debounces input updates before rendering`() = runTest {
        every {
            historicalTranslationService.translate(any(), any(), any(), any())
        } answers {
            placeholderTranslation(
                text = firstArg(),
                script = secondArg()
            )
        }

        val collector = launch { viewModel.uiState.collect { } }
        viewModel.selectMode(TranslationMode.TRANSLATE)
        runCurrent()

        viewModel.updateInputText("night")
        runCurrent()

        verify(exactly = 0) {
            historicalTranslationService.translate(any(), any(), any(), any())
        }

        advanceTimeBy(149)
        runCurrent()

        verify(exactly = 0) {
            historicalTranslationService.translate(any(), any(), any(), any())
        }

        advanceTimeBy(1)
        advanceUntilIdle()

        verify(exactly = 1) {
            historicalTranslationService.translate(
                "night",
                RunicScript.ELDER_FUTHARK,
                TranslationFidelity.DEFAULT,
                YoungerFutharkVariant.DEFAULT
            )
        }
        collector.cancel()
    }

    @Test
    fun `strict unavailable result exposes missing lemma explanation`() = runTest {
        every {
            historicalTranslationService.translate(any(), any(), any(), any())
        } answers {
            when (secondArg<RunicScript>()) {
                RunicScript.YOUNGER_FUTHARK -> TranslationResult(
                    sourceText = firstArg(),
                    script = RunicScript.YOUNGER_FUTHARK,
                    fidelity = TranslationFidelity.STRICT,
                    derivationKind = TranslationDerivationKind.TOKEN_COMPOSED,
                    historicalStage = HistoricalStage.OLD_NORSE,
                    normalizedForm = "",
                    diplomaticForm = "",
                    glyphOutput = "",
                    requestedVariant = YoungerFutharkVariant.LONG_BRANCH.name,
                    resolutionStatus = TranslationResolutionStatus.UNAVAILABLE,
                    confidence = 0f,
                    notes = listOf("Missing Old Norse lemma for 'satellite'."),
                    unresolvedTokens = listOf("satellite"),
                    engineVersion = "engine",
                    datasetVersion = "dataset"
                )

                RunicScript.ELDER_FUTHARK -> placeholderTranslation(
                    text = firstArg(),
                    script = RunicScript.ELDER_FUTHARK
                )

                RunicScript.CIRTH -> placeholderTranslation(
                    text = firstArg(),
                    script = RunicScript.CIRTH
                )
            }
        }

        val collector = launch { viewModel.uiState.collect { } }
        viewModel.selectMode(TranslationMode.TRANSLATE)
        viewModel.selectScript(RunicScript.YOUNGER_FUTHARK)
        viewModel.updateInputText("satellite")
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.unavailableExplanation).isEqualTo(
            "Missing lemma coverage for: satellite."
        )
        assertThat(viewModel.uiState.value.fallbackSuggestion).isEqualTo(
            "Try Readable or Decorative for a best-effort result."
        )
        collector.cancel()
    }

    @Test
    fun `saveToLibrary emits one shot success event`() = runTest {
        coEvery { quoteRepository.saveUserQuote(any()) } returns 21L

        val collector = launch { viewModel.uiState.collect { } }
        viewModel.updateInputText("rune song")
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.saveToLibrary()
            advanceUntilIdle()

            assertThat(awaitItem()).isEqualTo(TranslationEvent.ShowMessage("Saved to library"))
            cancelAndIgnoreRemainingEvents()
        }

        collector.cancel()
    }

    @Test
    fun `saveToLibrary caches structured translations in translate mode`() = runTest {
        coEvery { quoteRepository.saveUserQuote(any()) } returns 34L
        val cachedResults = slot<List<TranslationResult>>()
        every {
            historicalTranslationService.translate(any(), any(), any(), any())
        } answers {
            TranslationResult(
                sourceText = firstArg(),
                script = secondArg(),
                fidelity = thirdArg(),
                derivationKind = TranslationDerivationKind.TOKEN_COMPOSED,
                historicalStage = when (secondArg<RunicScript>()) {
                    RunicScript.YOUNGER_FUTHARK -> HistoricalStage.OLD_NORSE
                    RunicScript.ELDER_FUTHARK -> HistoricalStage.PROTO_NORSE
                    RunicScript.CIRTH -> HistoricalStage.EREBOR_ENGLISH
                },
                normalizedForm = "${firstArg<String>().lowercase()} normalized",
                diplomaticForm = "${firstArg<String>().lowercase()} diplomatic",
                glyphOutput = "${secondArg<RunicScript>().name.lowercase()} glyphs",
                resolutionStatus = TranslationResolutionStatus.RECONSTRUCTED,
                confidence = 0.8f,
                provenance = listOf(
                    TranslationProvenanceEntry(
                        sourceId = "runor",
                        label = "Runor",
                        role = "Reference",
                        license = "Reference only"
                    )
                ),
                engineVersion = "engine",
                datasetVersion = "dataset"
            )
        }
        coEvery {
            translationRepository.cacheTranslations(
                quoteId = 34L,
                results = capture(cachedResults),
                isBackfilled = false
            )
        } returns Unit

        val collector = launch { viewModel.uiState.collect { } }
        viewModel.selectMode(TranslationMode.TRANSLATE)
        viewModel.updateInputText("night")
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.saveToLibrary()
            advanceUntilIdle()

            assertThat(awaitItem()).isEqualTo(TranslationEvent.ShowMessage("Saved translation to library"))
            cancelAndIgnoreRemainingEvents()
        }

        assertThat(cachedResults.captured.map { it.script }).containsExactly(
            RunicScript.ELDER_FUTHARK,
            RunicScript.YOUNGER_FUTHARK,
            RunicScript.CIRTH
        )
        collector.cancel()
    }

    @Test
    fun `selecting fidelity and younger variant updates translation requests`() = runTest {
        every {
            historicalTranslationService.translate(any(), any(), any(), any())
        } returns placeholderTranslation(
            text = "night",
            script = RunicScript.YOUNGER_FUTHARK
        )

        val collector = launch { viewModel.uiState.collect { } }
        viewModel.selectMode(TranslationMode.TRANSLATE)
        viewModel.selectScript(RunicScript.YOUNGER_FUTHARK)
        viewModel.selectFidelity(TranslationFidelity.DECORATIVE)
        viewModel.selectYoungerVariant(YoungerFutharkVariant.SHORT_TWIG)
        viewModel.updateInputText("night")
        advanceUntilIdle()

        verify(atLeast = 1) {
            historicalTranslationService.translate(
                "night",
                RunicScript.YOUNGER_FUTHARK,
                TranslationFidelity.DECORATIVE,
                YoungerFutharkVariant.SHORT_TWIG
            )
        }
        collector.cancel()
    }

    @Test
    fun `saveToLibrary is a no op when input is blank`() = runTest {
        val collector = launch { viewModel.uiState.collect { } }

        viewModel.saveToLibrary()
        advanceUntilIdle()

        coVerify(exactly = 0) { quoteRepository.saveUserQuote(any()) }
        coVerify(exactly = 0) { translationRepository.cacheTranslations(any(), any(), any()) }
        collector.cancel()
    }

    private fun placeholderTranslation(
        text: String,
        script: RunicScript
    ): TranslationResult = TranslationResult(
        sourceText = text,
        script = script,
        fidelity = TranslationFidelity.STRICT,
        derivationKind = TranslationDerivationKind.TOKEN_COMPOSED,
        historicalStage = when (script) {
            RunicScript.YOUNGER_FUTHARK -> HistoricalStage.OLD_NORSE
            RunicScript.ELDER_FUTHARK -> HistoricalStage.PROTO_NORSE
            RunicScript.CIRTH -> HistoricalStage.EREBOR_ENGLISH
        },
        normalizedForm = "",
        diplomaticForm = "",
        glyphOutput = "",
        resolutionStatus = TranslationResolutionStatus.UNAVAILABLE,
        confidence = 0f,
        notes = listOf("placeholder"),
        unresolvedTokens = listOf(text),
        engineVersion = "engine",
        datasetVersion = "dataset"
    )
}
