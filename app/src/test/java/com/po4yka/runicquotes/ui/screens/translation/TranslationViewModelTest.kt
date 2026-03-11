package com.po4yka.runicquotes.ui.screens.translation

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.po4yka.runicquotes.data.preferences.UserPreferences
import com.po4yka.runicquotes.data.preferences.UserPreferencesManager
import com.po4yka.runicquotes.data.repository.QuoteRepository
import com.po4yka.runicquotes.domain.model.RunicScript
import com.po4yka.runicquotes.domain.transliteration.CirthTransliterator
import com.po4yka.runicquotes.domain.transliteration.ElderFutharkTransliterator
import com.po4yka.runicquotes.domain.transliteration.TransliterationFactory
import com.po4yka.runicquotes.domain.transliteration.WordTransliterationPair
import com.po4yka.runicquotes.domain.transliteration.YoungerFutharkTransliterator
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
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
    private lateinit var viewModel: TranslationViewModel

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var preferencesFlow: MutableStateFlow<UserPreferences>

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        quoteRepository = mockk(relaxed = true)
        userPreferencesManager = mockk(relaxed = true)
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
            quoteRepository = quoteRepository,
            userPreferencesManager = userPreferencesManager
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
}
