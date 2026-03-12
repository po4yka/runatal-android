package com.po4yka.runicquotes.ui.screens.addeditquote

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.po4yka.runicquotes.data.preferences.UserPreferences
import com.po4yka.runicquotes.data.preferences.UserPreferencesManager
import com.po4yka.runicquotes.data.repository.QuoteRepository
import com.po4yka.runicquotes.data.repository.TranslationRepository
import com.po4yka.runicquotes.domain.model.Quote
import com.po4yka.runicquotes.domain.model.RunicScript
import com.po4yka.runicquotes.domain.transliteration.CirthTransliterator
import com.po4yka.runicquotes.domain.transliteration.ElderFutharkTransliterator
import com.po4yka.runicquotes.domain.transliteration.TransliterationFactory
import com.po4yka.runicquotes.domain.transliteration.YoungerFutharkTransliterator
import io.mockk.coEvery
import io.mockk.coVerify
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
import java.io.IOException

/**
 * Comprehensive unit tests for AddEditQuoteViewModel.
 * Tests quote creation, editing, validation, and live runic preview generation.
 *
 * Coverage goals: >85%
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AddEditQuoteViewModelTest {

    private lateinit var quoteRepository: QuoteRepository
    private lateinit var userPreferencesManager: UserPreferencesManager
    private lateinit var transliterationFactory: TransliterationFactory
    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var viewModel: AddEditQuoteViewModel

    private val testDispatcher = StandardTestDispatcher()

    private val defaultPreferences = UserPreferences(
        selectedScript = RunicScript.ELDER_FUTHARK,
        selectedFont = "noto",
        showTransliteration = true
    )

    private val testQuote = Quote(
        id = 1,
        textLatin = "Test quote",
        author = "Test Author",
        runicElder = "\u16CF\u16D6\u16CA\u16CF \u16B2\u16A2\u16DF\u16CF\u16D6",
        runicYounger = "\u16CF\u16D6\u16CA\u16CF \u16B2\u16A2\u16DF\u16CF\u16D6",
        runicCirth = "\uE088\uE0C9\uE09C\uE088",
        isUserCreated = true,
        isFavorite = false,
        createdAt = 1_708_387_200_000L
    )

    private lateinit var preferencesFlow: MutableStateFlow<UserPreferences>

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        // Create mocks
        quoteRepository = mockk()
        userPreferencesManager = mockk()

        // Create real TransliterationFactory with real transliterators
        transliterationFactory = TransliterationFactory(
            ElderFutharkTransliterator(),
            YoungerFutharkTransliterator(),
            CirthTransliterator()
        )

        // Set up preferences flow
        preferencesFlow = MutableStateFlow(defaultPreferences)
        every { userPreferencesManager.userPreferencesFlow } returns preferencesFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ==================== Initialization Tests - New Quote ====================

    @Test
    fun `viewModel initializes with empty state for new quote`() = runTest {
        // Given: SavedStateHandle with no quoteId (new quote)
        savedStateHandle = SavedStateHandle(mapOf("quoteId" to 0L))

        // When: ViewModel is created
        viewModel = AddEditQuoteViewModel(
            quoteRepository,
            userPreferencesManager,
            transliterationFactory,
            savedStateHandle
        )
        advanceUntilIdle()

        // Then: State is empty
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.textLatin).isEqualTo("")
            assertThat(state.author).isEqualTo("")
            assertThat(state.runicElderPreview).isEqualTo("")
            assertThat(state.runicYoungerPreview).isEqualTo("")
            assertThat(state.runicCirthPreview).isEqualTo("")
            assertThat(state.isEditing).isFalse()
            assertThat(state.isSaving).isFalse()
            assertThat(state.quoteTextError).isNull()
            assertThat(state.authorError).isNull()
            assertThat(state.errorMessage).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `viewModel loads preferences on initialization`() = runTest {
        // Given: SavedStateHandle with no quoteId
        savedStateHandle = SavedStateHandle(mapOf("quoteId" to 0L))

        // When: ViewModel is created
        viewModel = AddEditQuoteViewModel(
            quoteRepository,
            userPreferencesManager,
            transliterationFactory,
            savedStateHandle
        )
        advanceUntilIdle()

        // Then: Preferences are loaded
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.selectedScript).isEqualTo(RunicScript.ELDER_FUTHARK)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ==================== Initialization Tests - Edit Quote ====================

    @Test
    fun `viewModel loads existing quote for editing`() = runTest {
        // Given: SavedStateHandle with quoteId
        savedStateHandle = SavedStateHandle(mapOf("quoteId" to 1L))
        coEvery { quoteRepository.getQuoteById(1L) } returns testQuote

        // When: ViewModel is created
        viewModel = AddEditQuoteViewModel(
            quoteRepository,
            userPreferencesManager,
            transliterationFactory,
            savedStateHandle
        )
        advanceUntilIdle()

        // Then: Quote is loaded and previews generated
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.textLatin).isEqualTo(testQuote.textLatin)
            assertThat(state.author).isEqualTo(testQuote.author)
            assertThat(state.isEditing).isTrue()
            assertThat(state.runicElderPreview).isEqualTo(testQuote.runicElder)
            assertThat(state.runicYoungerPreview).isEqualTo(testQuote.runicYounger)
            assertThat(state.runicCirthPreview).isEqualTo(testQuote.runicCirth)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `viewModel does not load system quote for editing`() = runTest {
        // Given: SavedStateHandle with quoteId for system quote
        val systemQuote = testQuote.copy(isUserCreated = false)
        savedStateHandle = SavedStateHandle(mapOf("quoteId" to 1L))
        coEvery { quoteRepository.getQuoteById(1L) } returns systemQuote

        // When: ViewModel is created
        viewModel = AddEditQuoteViewModel(
            quoteRepository,
            userPreferencesManager,
            transliterationFactory,
            savedStateHandle
        )
        advanceUntilIdle()

        // Then: Quote is not loaded (system quotes can't be edited)
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.textLatin).isEqualTo("")
            assertThat(state.author).isEqualTo("")
            assertThat(state.isEditing).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `viewModel handles null quote gracefully`() = runTest {
        // Given: SavedStateHandle with quoteId that doesn't exist
        savedStateHandle = SavedStateHandle(mapOf("quoteId" to 999L))
        coEvery { quoteRepository.getQuoteById(999L) } returns null

        // When: ViewModel is created
        viewModel = AddEditQuoteViewModel(
            quoteRepository,
            userPreferencesManager,
            transliterationFactory,
            savedStateHandle
        )
        advanceUntilIdle()

        // Then: Empty state
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.textLatin).isEqualTo("")
            assertThat(state.author).isEqualTo("")
            assertThat(state.isEditing).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ==================== Text Update Tests ====================

    @Test
    fun `updateTextLatin updates text and generates previews`() = runTest {
        // Given: ViewModel initialized
        savedStateHandle = SavedStateHandle(mapOf("quoteId" to 0L))
        viewModel = AddEditQuoteViewModel(
            quoteRepository,
            userPreferencesManager,
            transliterationFactory,
            savedStateHandle
        )
        advanceUntilIdle()

        // When: Updating text
        val newText = "Hello World"
        viewModel.updateTextLatin(newText)
        advanceUntilIdle()

        // Then: Text and previews are updated
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.textLatin).isEqualTo(newText)
            assertThat(state.runicElderPreview).isNotEmpty()
            assertThat(state.runicYoungerPreview).isNotEmpty()
            assertThat(state.runicCirthPreview).isNotEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `updateTextLatin with empty string clears previews`() = runTest {
        // Given: ViewModel with text
        savedStateHandle = SavedStateHandle(mapOf("quoteId" to 0L))
        viewModel = AddEditQuoteViewModel(
            quoteRepository,
            userPreferencesManager,
            transliterationFactory,
            savedStateHandle
        )
        advanceUntilIdle()

        viewModel.updateTextLatin("Test")
        advanceUntilIdle()

        // When: Clearing text
        viewModel.updateTextLatin("")
        advanceUntilIdle()

        // Then: Previews are empty
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.textLatin).isEqualTo("")
            assertThat(state.runicElderPreview).isEqualTo("")
            assertThat(state.runicYoungerPreview).isEqualTo("")
            assertThat(state.runicCirthPreview).isEqualTo("")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `updateTextLatin handles special characters`() = runTest {
        // Given: ViewModel initialized
        savedStateHandle = SavedStateHandle(mapOf("quoteId" to 0L))
        viewModel = AddEditQuoteViewModel(
            quoteRepository,
            userPreferencesManager,
            transliterationFactory,
            savedStateHandle
        )
        advanceUntilIdle()

        // When: Updating with special characters
        val textWithSpecialChars = "Hello, World! It's great."
        viewModel.updateTextLatin(textWithSpecialChars)
        advanceUntilIdle()

        // Then: Previews are generated
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.textLatin).isEqualTo(textWithSpecialChars)
            assertThat(state.runicElderPreview).isNotEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ==================== Author Update Tests ====================

    @Test
    fun `updateAuthor updates author field`() = runTest {
        // Given: ViewModel initialized
        savedStateHandle = SavedStateHandle(mapOf("quoteId" to 0L))
        viewModel = AddEditQuoteViewModel(
            quoteRepository,
            userPreferencesManager,
            transliterationFactory,
            savedStateHandle
        )
        advanceUntilIdle()

        // When: Updating author
        val newAuthor = "John Doe"
        viewModel.updateAuthor(newAuthor)
        advanceUntilIdle()

        // Then: Author is updated
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.author).isEqualTo(newAuthor)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `updateAuthor does not affect runic previews`() = runTest {
        // Given: ViewModel with text
        savedStateHandle = SavedStateHandle(mapOf("quoteId" to 0L))
        viewModel = AddEditQuoteViewModel(
            quoteRepository,
            userPreferencesManager,
            transliterationFactory,
            savedStateHandle
        )
        advanceUntilIdle()

        viewModel.updateTextLatin("Test quote")
        advanceUntilIdle()

        viewModel.uiState.test {
            val stateBeforeAuthorUpdate = awaitItem()
            val previewBefore = stateBeforeAuthorUpdate.runicElderPreview

            // When: Updating author
            viewModel.updateAuthor("New Author")
            advanceUntilIdle()

            // Then: Runic preview unchanged
            val stateAfter = awaitItem()
            assertThat(stateAfter.runicElderPreview).isEqualTo(previewBefore)
            assertThat(stateAfter.author).isEqualTo("New Author")

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ==================== Script Selection Tests ====================

    @Test
    fun `updateSelectedScript changes selected script`() = runTest {
        // Given: ViewModel initialized
        savedStateHandle = SavedStateHandle(mapOf("quoteId" to 0L))
        viewModel = AddEditQuoteViewModel(
            quoteRepository,
            userPreferencesManager,
            transliterationFactory,
            savedStateHandle
        )
        advanceUntilIdle()

        // When: Updating script
        viewModel.updateSelectedScript(RunicScript.YOUNGER_FUTHARK)
        advanceUntilIdle()

        // Then: Script is updated
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.selectedScript).isEqualTo(RunicScript.YOUNGER_FUTHARK)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `updateSelectedScript to all scripts works correctly`() = runTest {
        // Given: ViewModel initialized
        savedStateHandle = SavedStateHandle(mapOf("quoteId" to 0L))
        viewModel = AddEditQuoteViewModel(
            quoteRepository,
            userPreferencesManager,
            transliterationFactory,
            savedStateHandle
        )
        advanceUntilIdle()

        viewModel.uiState.test {
            // Initial
            val initial = awaitItem()
            assertThat(initial.selectedScript).isEqualTo(RunicScript.ELDER_FUTHARK)

            // Younger Futhark
            viewModel.updateSelectedScript(RunicScript.YOUNGER_FUTHARK)
            val younger = awaitItem()
            assertThat(younger.selectedScript).isEqualTo(RunicScript.YOUNGER_FUTHARK)

            // Cirth
            viewModel.updateSelectedScript(RunicScript.CIRTH)
            val cirth = awaitItem()
            assertThat(cirth.selectedScript).isEqualTo(RunicScript.CIRTH)

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ==================== Save Quote Tests - New Quote ====================

    @Test
    fun `saveQuote validates empty text`() = runTest {
        // Given: ViewModel with empty text
        savedStateHandle = SavedStateHandle(mapOf("quoteId" to 0L))
        viewModel = AddEditQuoteViewModel(
            quoteRepository,
            userPreferencesManager,
            transliterationFactory,
            savedStateHandle
        )
        advanceUntilIdle()

        viewModel.updateAuthor("Test Author")

        // When: Saving with empty text
        viewModel.saveQuote()
        advanceUntilIdle()

        // Then: Inline validation is shown and confirmation is not triggered
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.errorMessage).isNull()
            assertThat(state.quoteTextError).isEqualTo("Quote must be at least 3 characters")
            assertThat(state.showConfirmation).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `saveQuote validates empty author`() = runTest {
        // Given: ViewModel with empty author
        savedStateHandle = SavedStateHandle(mapOf("quoteId" to 0L))
        viewModel = AddEditQuoteViewModel(
            quoteRepository,
            userPreferencesManager,
            transliterationFactory,
            savedStateHandle
        )
        advanceUntilIdle()

        viewModel.updateTextLatin("Test quote")

        // When: Saving with empty author
        viewModel.saveQuote()
        advanceUntilIdle()

        // Then: Inline validation is shown and confirmation is not triggered
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.errorMessage).isNull()
            assertThat(state.authorError).isEqualTo("Author is required")
            assertThat(state.showConfirmation).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `saveQuote validates blank text with spaces`() = runTest {
        // Given: ViewModel with blank text
        savedStateHandle = SavedStateHandle(mapOf("quoteId" to 0L))
        viewModel = AddEditQuoteViewModel(
            quoteRepository,
            userPreferencesManager,
            transliterationFactory,
            savedStateHandle
        )
        advanceUntilIdle()

        viewModel.updateTextLatin("   ")
        viewModel.updateAuthor("Test Author")

        // When: Saving with blank text
        viewModel.saveQuote()
        advanceUntilIdle()

        // Then: Inline validation is shown
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.errorMessage).isNull()
            assertThat(state.quoteTextError).isEqualTo("Quote must be at least 3 characters")
            assertThat(state.showConfirmation).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `saveQuote creates new quote successfully`() = runTest {
        // Given: ViewModel with valid data
        savedStateHandle = SavedStateHandle(mapOf("quoteId" to 0L))
        coEvery { quoteRepository.saveUserQuote(any()) } returns 1L

        viewModel = AddEditQuoteViewModel(
            quoteRepository,
            userPreferencesManager,
            transliterationFactory,
            savedStateHandle
        )
        advanceUntilIdle()

        viewModel.updateTextLatin("Test quote")
        viewModel.updateAuthor("Test Author")
        advanceUntilIdle()

        // When: Saving quote
        viewModel.saveQuote()
        advanceUntilIdle()

        // Then: Quote is saved and confirmation shown
        coVerify { quoteRepository.saveUserQuote(any()) }
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.showConfirmation).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `saveQuote includes all runic previews`() = runTest {
        // Given: ViewModel with valid data
        savedStateHandle = SavedStateHandle(mapOf("quoteId" to 0L))
        var savedQuote: Quote? = null
        coEvery { quoteRepository.saveUserQuote(any()) } coAnswers {
            savedQuote = firstArg()
            1L
        }

        viewModel = AddEditQuoteViewModel(
            quoteRepository,
            userPreferencesManager,
            transliterationFactory,
            savedStateHandle
        )
        advanceUntilIdle()

        viewModel.updateTextLatin("Hello world")
        viewModel.updateAuthor("Author")
        advanceUntilIdle()

        // When: Saving quote
        viewModel.saveQuote()
        advanceUntilIdle()

        // Then: All runic fields are populated
        assertThat(savedQuote).isNotNull()
        assertThat(savedQuote!!.runicElder?.isNotEmpty() == true).isTrue()
        assertThat(savedQuote!!.runicYounger?.isNotEmpty() == true).isTrue()
        assertThat(savedQuote!!.runicCirth?.isNotEmpty() == true).isTrue()
    }

    @Test
    fun `saveQuote trims text and author`() = runTest {
        // Given: ViewModel with text containing extra spaces
        savedStateHandle = SavedStateHandle(mapOf("quoteId" to 0L))
        var savedQuote: Quote? = null
        coEvery { quoteRepository.saveUserQuote(any()) } coAnswers {
            savedQuote = firstArg()
            1L
        }

        viewModel = AddEditQuoteViewModel(
            quoteRepository,
            userPreferencesManager,
            transliterationFactory,
            savedStateHandle
        )
        advanceUntilIdle()

        viewModel.updateTextLatin("  Test quote  ")
        viewModel.updateAuthor("  Test Author  ")
        advanceUntilIdle()

        // When: Saving quote
        viewModel.saveQuote()
        advanceUntilIdle()

        // Then: Text is trimmed
        assertThat(savedQuote).isNotNull()
        assertThat(savedQuote!!.textLatin).isEqualTo("Test quote")
        assertThat(savedQuote!!.author).isEqualTo("Test Author")
    }

    @Test
    fun `saveQuote sets isSaving flag`() = runTest {
        // Given: ViewModel with valid data
        savedStateHandle = SavedStateHandle(mapOf("quoteId" to 0L))
        coEvery { quoteRepository.saveUserQuote(any()) } returns 1L

        viewModel = AddEditQuoteViewModel(
            quoteRepository,
            userPreferencesManager,
            transliterationFactory,
            savedStateHandle
        )
        advanceUntilIdle()

        viewModel.updateTextLatin("Test quote!")
        viewModel.updateAuthor("Author")
        advanceUntilIdle()

        // When: Saving quote
        viewModel.uiState.test {
            val initial = awaitItem()
            assertThat(initial.isSaving).isFalse()

            viewModel.saveQuote()

            val saving = awaitItem()
            assertThat(saving.isSaving).isTrue()

            advanceUntilIdle()

            val saved = awaitItem()
            assertThat(saved.isSaving).isFalse()

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ==================== Save Quote Tests - Edit Quote ====================

    @Test
    fun `saveQuote updates existing quote`() = runTest {
        // Given: ViewModel editing existing quote
        val favoriteQuote = testQuote.copy(isFavorite = true)
        savedStateHandle = SavedStateHandle(mapOf("quoteId" to 1L))
        coEvery { quoteRepository.getQuoteById(1L) } returns favoriteQuote
        var savedQuote: Quote? = null
        coEvery { quoteRepository.saveUserQuote(any()) } coAnswers {
            savedQuote = firstArg()
            1L
        }

        viewModel = AddEditQuoteViewModel(
            quoteRepository,
            userPreferencesManager,
            transliterationFactory,
            savedStateHandle
        )
        advanceUntilIdle()

        viewModel.updateTextLatin("Updated quote")
        advanceUntilIdle()

        // When: Saving quote
        viewModel.events.test {
            viewModel.saveQuote()
            advanceUntilIdle()

            assertThat(awaitItem()).isEqualTo(AddEditQuoteEvent.NavigateBackAfterEdit)
            cancelAndIgnoreRemainingEvents()
        }

        // Then: Quote is saved with original ID, original timestamp, and no create confirmation
        coVerify { quoteRepository.saveUserQuote(match { it.id == 1L }) }
        assertThat(savedQuote).isNotNull()
        assertThat(savedQuote!!.createdAt).isEqualTo(favoriteQuote.createdAt)
        assertThat(savedQuote!!.isFavorite).isTrue()
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.showConfirmation).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `saveQuote invalidates cached translations when edited text changes`() = runTest {
        val translationRepository = mockk<TranslationRepository>(relaxed = true)
        savedStateHandle = SavedStateHandle(mapOf("quoteId" to 1L))
        coEvery { quoteRepository.getQuoteById(1L) } returns testQuote
        coEvery { quoteRepository.saveUserQuote(any()) } returns 1L

        viewModel = AddEditQuoteViewModel(
            quoteRepository,
            userPreferencesManager,
            transliterationFactory,
            savedStateHandle,
            translationRepository
        )
        advanceUntilIdle()

        viewModel.updateTextLatin("Updated quote")
        advanceUntilIdle()
        viewModel.saveQuote()
        advanceUntilIdle()

        coVerify(exactly = 1) { translationRepository.deleteTranslationsForQuote(1L) }
    }

    // ==================== Error Handling Tests ====================

    @Test
    fun `saveQuote handles IOException`() = runTest {
        // Given: Repository throws IOException
        savedStateHandle = SavedStateHandle(mapOf("quoteId" to 0L))
        coEvery { quoteRepository.saveUserQuote(any()) } throws IOException("Database error")

        viewModel = AddEditQuoteViewModel(
            quoteRepository,
            userPreferencesManager,
            transliterationFactory,
            savedStateHandle
        )
        advanceUntilIdle()

        viewModel.updateTextLatin("Test quote!")
        viewModel.updateAuthor("Author")
        advanceUntilIdle()

        // When: Saving quote
        viewModel.saveQuote()
        advanceUntilIdle()

        // Then: Error message is set and confirmation not shown
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.errorMessage).isNotNull()
            assertThat(state.errorMessage).contains("Failed to save quote")
            assertThat(state.isSaving).isFalse()
            assertThat(state.showConfirmation).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `saveQuote handles IllegalStateException`() = runTest {
        // Given: Repository throws IllegalStateException
        savedStateHandle = SavedStateHandle(mapOf("quoteId" to 0L))
        coEvery { quoteRepository.saveUserQuote(any()) } throws IllegalStateException("Invalid state")

        viewModel = AddEditQuoteViewModel(
            quoteRepository,
            userPreferencesManager,
            transliterationFactory,
            savedStateHandle
        )
        advanceUntilIdle()

        viewModel.updateTextLatin("Test quote!")
        viewModel.updateAuthor("Author")
        advanceUntilIdle()

        // When: Saving quote
        viewModel.saveQuote()
        advanceUntilIdle()

        // Then: Error message is set
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.errorMessage).isNotNull()
            assertThat(state.errorMessage).contains("Invalid state")
            assertThat(state.showConfirmation).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `clearError removes error message`() = runTest {
        // Given: ViewModel with repository error
        savedStateHandle = SavedStateHandle(mapOf("quoteId" to 0L))
        coEvery { quoteRepository.saveUserQuote(any()) } throws IOException("Database error")
        viewModel = AddEditQuoteViewModel(
            quoteRepository,
            userPreferencesManager,
            transliterationFactory,
            savedStateHandle
        )
        advanceUntilIdle()

        viewModel.updateTextLatin("Test quote")
        viewModel.updateAuthor("Author")
        advanceUntilIdle()

        // Trigger repository error
        viewModel.saveQuote()
        advanceUntilIdle()

        // When: Clearing error
        viewModel.uiState.test {
            val stateWithError = awaitItem()
            assertThat(stateWithError.errorMessage).isNotNull()

            viewModel.clearError()
            advanceUntilIdle()

            val stateWithoutError = awaitItem()
            assertThat(stateWithoutError.errorMessage).isNull()

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ==================== Preferences Update Tests ====================

    @Test
    fun `preferences change updates selectedScript`() = runTest {
        // Given: ViewModel initialized
        savedStateHandle = SavedStateHandle(mapOf("quoteId" to 0L))
        viewModel = AddEditQuoteViewModel(
            quoteRepository,
            userPreferencesManager,
            transliterationFactory,
            savedStateHandle
        )
        advanceUntilIdle()

        viewModel.uiState.test {
            // Initial state
            val initial = awaitItem()
            assertThat(initial.selectedScript).isEqualTo(RunicScript.ELDER_FUTHARK)

            // When: Preferences change
            preferencesFlow.value = defaultPreferences.copy(selectedScript = RunicScript.CIRTH)
            advanceUntilIdle()

            // Then: State reflects new script
            val updated = awaitItem()
            assertThat(updated.selectedScript).isEqualTo(RunicScript.CIRTH)

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ==================== Edge Cases ====================

    @Test
    fun `multiple rapid text updates generate correct previews`() = runTest {
        // Given: ViewModel initialized
        savedStateHandle = SavedStateHandle(mapOf("quoteId" to 0L))
        viewModel = AddEditQuoteViewModel(
            quoteRepository,
            userPreferencesManager,
            transliterationFactory,
            savedStateHandle
        )
        advanceUntilIdle()

        // When: Multiple rapid text updates
        viewModel.updateTextLatin("A")
        viewModel.updateTextLatin("AB")
        viewModel.updateTextLatin("ABC")
        viewModel.updateTextLatin("ABCD")
        advanceUntilIdle()

        // Then: Final preview matches final text
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.textLatin).isEqualTo("ABCD")
            assertThat(state.runicElderPreview).isNotEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `long text generates preview correctly`() = runTest {
        // Given: ViewModel initialized
        savedStateHandle = SavedStateHandle(mapOf("quoteId" to 0L))
        viewModel = AddEditQuoteViewModel(
            quoteRepository,
            userPreferencesManager,
            transliterationFactory,
            savedStateHandle
        )
        advanceUntilIdle()

        // When: Setting long text
        val longText = "This is a very long quote that contains many words and should still generate proper runic transliterations for all three scripts."
        viewModel.updateTextLatin(longText)
        advanceUntilIdle()

        // Then: Previews are generated
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.textLatin).isEqualTo(longText)
            assertThat(state.runicElderPreview.length).isGreaterThan(longText.length / 2) // Rough check
            assertThat(state.runicYoungerPreview).isNotEmpty()
            assertThat(state.runicCirthPreview).isNotEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `quote with unicode characters is handled`() = runTest {
        // Given: ViewModel initialized
        savedStateHandle = SavedStateHandle(mapOf("quoteId" to 0L))
        viewModel = AddEditQuoteViewModel(
            quoteRepository,
            userPreferencesManager,
            transliterationFactory,
            savedStateHandle
        )
        advanceUntilIdle()

        // When: Setting text with unicode
        val unicodeText = "Caf\u00E9 na\u00EFve r\u00E9sum\u00E9"
        viewModel.updateTextLatin(unicodeText)
        advanceUntilIdle()

        // Then: Previews are generated
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.textLatin).isEqualTo(unicodeText)
            assertThat(state.runicElderPreview).isNotEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }
}
