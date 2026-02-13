package com.po4yka.runicquotes.ui.screens.addeditquote

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.po4yka.runicquotes.data.preferences.UserPreferences
import com.po4yka.runicquotes.data.preferences.UserPreferencesManager
import com.po4yka.runicquotes.data.repository.QuoteRepository
import com.po4yka.runicquotes.domain.model.Quote
import com.po4yka.runicquotes.domain.model.RunicScript
import com.po4yka.runicquotes.domain.transliteration.CirthTransliterator
import com.po4yka.runicquotes.domain.transliteration.ElderFutharkTransliterator
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
    private lateinit var elderFutharkTransliterator: ElderFutharkTransliterator
    private lateinit var youngerFutharkTransliterator: YoungerFutharkTransliterator
    private lateinit var cirthTransliterator: CirthTransliterator
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
        runicElder = "ᛏᛖᛋᛏ ᛩᚢᛟᛏᛖ",
        runicYounger = "ᛏᛖᛋᛏ ᛩᚢᛟᛏᛖ",
        runicCirth = "\uE088\uE0C9\uE09C\uE088",
        isUserCreated = true,
        isFavorite = false
    )

    private lateinit var preferencesFlow: MutableStateFlow<UserPreferences>

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        // Create mocks
        quoteRepository = mockk()
        userPreferencesManager = mockk()

        // Create real transliterators
        elderFutharkTransliterator = ElderFutharkTransliterator()
        youngerFutharkTransliterator = YoungerFutharkTransliterator()
        cirthTransliterator = CirthTransliterator()

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
            elderFutharkTransliterator,
            youngerFutharkTransliterator,
            cirthTransliterator,
            savedStateHandle
        )
        advanceUntilIdle()

        // Then: State is empty
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("", state.textLatin)
            assertEquals("", state.author)
            assertEquals("", state.runicElderPreview)
            assertEquals("", state.runicYoungerPreview)
            assertEquals("", state.runicCirthPreview)
            assertFalse(state.isEditing)
            assertFalse(state.isSaving)
            assertNull(state.errorMessage)
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
            elderFutharkTransliterator,
            youngerFutharkTransliterator,
            cirthTransliterator,
            savedStateHandle
        )
        advanceUntilIdle()

        // Then: Preferences are loaded
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(RunicScript.ELDER_FUTHARK, state.selectedScript)
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
            elderFutharkTransliterator,
            youngerFutharkTransliterator,
            cirthTransliterator,
            savedStateHandle
        )
        advanceUntilIdle()

        // Then: Quote is loaded and previews generated
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(testQuote.textLatin, state.textLatin)
            assertEquals(testQuote.author, state.author)
            assertTrue(state.isEditing)
            assertTrue(state.runicElderPreview.isNotEmpty())
            assertTrue(state.runicYoungerPreview.isNotEmpty())
            assertTrue(state.runicCirthPreview.isNotEmpty())
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
            elderFutharkTransliterator,
            youngerFutharkTransliterator,
            cirthTransliterator,
            savedStateHandle
        )
        advanceUntilIdle()

        // Then: Quote is not loaded (system quotes can't be edited)
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("", state.textLatin)
            assertEquals("", state.author)
            assertFalse(state.isEditing)
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
            elderFutharkTransliterator,
            youngerFutharkTransliterator,
            cirthTransliterator,
            savedStateHandle
        )
        advanceUntilIdle()

        // Then: Empty state
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("", state.textLatin)
            assertEquals("", state.author)
            assertFalse(state.isEditing)
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
            elderFutharkTransliterator,
            youngerFutharkTransliterator,
            cirthTransliterator,
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
            assertEquals(newText, state.textLatin)
            assertTrue(state.runicElderPreview.isNotEmpty())
            assertTrue(state.runicYoungerPreview.isNotEmpty())
            assertTrue(state.runicCirthPreview.isNotEmpty())
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
            elderFutharkTransliterator,
            youngerFutharkTransliterator,
            cirthTransliterator,
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
            assertEquals("", state.textLatin)
            assertEquals("", state.runicElderPreview)
            assertEquals("", state.runicYoungerPreview)
            assertEquals("", state.runicCirthPreview)
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
            elderFutharkTransliterator,
            youngerFutharkTransliterator,
            cirthTransliterator,
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
            assertEquals(textWithSpecialChars, state.textLatin)
            assertTrue(state.runicElderPreview.isNotEmpty())
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
            elderFutharkTransliterator,
            youngerFutharkTransliterator,
            cirthTransliterator,
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
            assertEquals(newAuthor, state.author)
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
            elderFutharkTransliterator,
            youngerFutharkTransliterator,
            cirthTransliterator,
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
            assertEquals(previewBefore, stateAfter.runicElderPreview)
            assertEquals("New Author", stateAfter.author)

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
            elderFutharkTransliterator,
            youngerFutharkTransliterator,
            cirthTransliterator,
            savedStateHandle
        )
        advanceUntilIdle()

        // When: Updating script
        viewModel.updateSelectedScript(RunicScript.YOUNGER_FUTHARK)
        advanceUntilIdle()

        // Then: Script is updated
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(RunicScript.YOUNGER_FUTHARK, state.selectedScript)
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
            elderFutharkTransliterator,
            youngerFutharkTransliterator,
            cirthTransliterator,
            savedStateHandle
        )
        advanceUntilIdle()

        viewModel.uiState.test {
            // Initial
            val initial = awaitItem()
            assertEquals(RunicScript.ELDER_FUTHARK, initial.selectedScript)

            // Younger Futhark
            viewModel.updateSelectedScript(RunicScript.YOUNGER_FUTHARK)
            val younger = awaitItem()
            assertEquals(RunicScript.YOUNGER_FUTHARK, younger.selectedScript)

            // Cirth
            viewModel.updateSelectedScript(RunicScript.CIRTH)
            val cirth = awaitItem()
            assertEquals(RunicScript.CIRTH, cirth.selectedScript)

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
            elderFutharkTransliterator,
            youngerFutharkTransliterator,
            cirthTransliterator,
            savedStateHandle
        )
        advanceUntilIdle()

        var callbackInvoked = false
        viewModel.updateAuthor("Test Author")

        // When: Saving with empty text
        viewModel.saveQuote { callbackInvoked = true }
        advanceUntilIdle()

        // Then: Error is shown and callback not invoked
        viewModel.uiState.test {
            val state = awaitItem()
            assertNotNull(state.errorMessage)
            assertTrue(state.errorMessage!!.contains("cannot be empty"))
            assertFalse(callbackInvoked)
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
            elderFutharkTransliterator,
            youngerFutharkTransliterator,
            cirthTransliterator,
            savedStateHandle
        )
        advanceUntilIdle()

        var callbackInvoked = false
        viewModel.updateTextLatin("Test quote")

        // When: Saving with empty author
        viewModel.saveQuote { callbackInvoked = true }
        advanceUntilIdle()

        // Then: Error is shown and callback not invoked
        viewModel.uiState.test {
            val state = awaitItem()
            assertNotNull(state.errorMessage)
            assertTrue(state.errorMessage!!.contains("cannot be empty"))
            assertFalse(callbackInvoked)
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
            elderFutharkTransliterator,
            youngerFutharkTransliterator,
            cirthTransliterator,
            savedStateHandle
        )
        advanceUntilIdle()

        var callbackInvoked = false
        viewModel.updateTextLatin("   ")
        viewModel.updateAuthor("Test Author")

        // When: Saving with blank text
        viewModel.saveQuote { callbackInvoked = true }
        advanceUntilIdle()

        // Then: Error is shown
        viewModel.uiState.test {
            val state = awaitItem()
            assertNotNull(state.errorMessage)
            assertFalse(callbackInvoked)
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
            elderFutharkTransliterator,
            youngerFutharkTransliterator,
            cirthTransliterator,
            savedStateHandle
        )
        advanceUntilIdle()

        viewModel.updateTextLatin("Test quote")
        viewModel.updateAuthor("Test Author")
        advanceUntilIdle()

        var callbackInvoked = false

        // When: Saving quote
        viewModel.saveQuote { callbackInvoked = true }
        advanceUntilIdle()

        // Then: Quote is saved and callback invoked
        coVerify { quoteRepository.saveUserQuote(any()) }
        assertTrue(callbackInvoked)
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
            elderFutharkTransliterator,
            youngerFutharkTransliterator,
            cirthTransliterator,
            savedStateHandle
        )
        advanceUntilIdle()

        viewModel.updateTextLatin("Hello world")
        viewModel.updateAuthor("Author")
        advanceUntilIdle()

        // When: Saving quote
        viewModel.saveQuote { }
        advanceUntilIdle()

        // Then: All runic fields are populated
        assertNotNull(savedQuote)
        assertTrue(savedQuote!!.runicElder?.isNotEmpty() == true)
        assertTrue(savedQuote!!.runicYounger?.isNotEmpty() == true)
        assertTrue(savedQuote!!.runicCirth?.isNotEmpty() == true)
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
            elderFutharkTransliterator,
            youngerFutharkTransliterator,
            cirthTransliterator,
            savedStateHandle
        )
        advanceUntilIdle()

        viewModel.updateTextLatin("  Test quote  ")
        viewModel.updateAuthor("  Test Author  ")
        advanceUntilIdle()

        // When: Saving quote
        viewModel.saveQuote { }
        advanceUntilIdle()

        // Then: Text is trimmed
        assertNotNull(savedQuote)
        assertEquals("Test quote", savedQuote!!.textLatin)
        assertEquals("Test Author", savedQuote!!.author)
    }

    @Test
    fun `saveQuote sets isSaving flag`() = runTest {
        // Given: ViewModel with valid data
        savedStateHandle = SavedStateHandle(mapOf("quoteId" to 0L))
        coEvery { quoteRepository.saveUserQuote(any()) } returns 1L

        viewModel = AddEditQuoteViewModel(
            quoteRepository,
            userPreferencesManager,
            elderFutharkTransliterator,
            youngerFutharkTransliterator,
            cirthTransliterator,
            savedStateHandle
        )
        advanceUntilIdle()

        viewModel.updateTextLatin("Test quote!")
        viewModel.updateAuthor("Author")
        advanceUntilIdle()

        // When: Saving quote
        viewModel.uiState.test {
            val initial = awaitItem()
            assertFalse(initial.isSaving)

            viewModel.saveQuote { }

            val saving = awaitItem()
            assertTrue(saving.isSaving)

            advanceUntilIdle()

            val saved = awaitItem()
            assertFalse(saved.isSaving)

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ==================== Save Quote Tests - Edit Quote ====================

    @Test
    fun `saveQuote updates existing quote`() = runTest {
        // Given: ViewModel editing existing quote
        savedStateHandle = SavedStateHandle(mapOf("quoteId" to 1L))
        coEvery { quoteRepository.getQuoteById(1L) } returns testQuote
        coEvery { quoteRepository.saveUserQuote(any()) } returns 1L

        viewModel = AddEditQuoteViewModel(
            quoteRepository,
            userPreferencesManager,
            elderFutharkTransliterator,
            youngerFutharkTransliterator,
            cirthTransliterator,
            savedStateHandle
        )
        advanceUntilIdle()

        viewModel.updateTextLatin("Updated quote")
        advanceUntilIdle()

        var callbackInvoked = false

        // When: Saving quote
        viewModel.saveQuote { callbackInvoked = true }
        advanceUntilIdle()

        // Then: Quote is saved with original ID
        coVerify { quoteRepository.saveUserQuote(match { it.id == 1L }) }
        assertTrue(callbackInvoked)
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
            elderFutharkTransliterator,
            youngerFutharkTransliterator,
            cirthTransliterator,
            savedStateHandle
        )
        advanceUntilIdle()

        viewModel.updateTextLatin("Test quote!")
        viewModel.updateAuthor("Author")
        advanceUntilIdle()

        var callbackInvoked = false

        // When: Saving quote
        viewModel.saveQuote { callbackInvoked = true }
        advanceUntilIdle()

        // Then: Error message is set and callback not invoked
        viewModel.uiState.test {
            val state = awaitItem()
            assertNotNull(state.errorMessage)
            assertTrue(state.errorMessage!!.contains("Failed to save quote"))
            assertFalse(state.isSaving)
            assertFalse(callbackInvoked)
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
            elderFutharkTransliterator,
            youngerFutharkTransliterator,
            cirthTransliterator,
            savedStateHandle
        )
        advanceUntilIdle()

        viewModel.updateTextLatin("Test quote!")
        viewModel.updateAuthor("Author")
        advanceUntilIdle()

        var callbackInvoked = false

        // When: Saving quote
        viewModel.saveQuote { callbackInvoked = true }
        advanceUntilIdle()

        // Then: Error message is set
        viewModel.uiState.test {
            val state = awaitItem()
            assertNotNull(state.errorMessage)
            assertTrue(state.errorMessage!!.contains("Invalid state"))
            assertFalse(callbackInvoked)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `clearError removes error message`() = runTest {
        // Given: ViewModel with error
        savedStateHandle = SavedStateHandle(mapOf("quoteId" to 0L))
        viewModel = AddEditQuoteViewModel(
            quoteRepository,
            userPreferencesManager,
            elderFutharkTransliterator,
            youngerFutharkTransliterator,
            cirthTransliterator,
            savedStateHandle
        )
        advanceUntilIdle()

        // Trigger validation error
        viewModel.saveQuote { }
        advanceUntilIdle()

        // When: Clearing error
        viewModel.uiState.test {
            val stateWithError = awaitItem()
            assertNotNull(stateWithError.errorMessage)

            viewModel.clearError()
            advanceUntilIdle()

            val stateWithoutError = awaitItem()
            assertNull(stateWithoutError.errorMessage)

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
            elderFutharkTransliterator,
            youngerFutharkTransliterator,
            cirthTransliterator,
            savedStateHandle
        )
        advanceUntilIdle()

        viewModel.uiState.test {
            // Initial state
            val initial = awaitItem()
            assertEquals(RunicScript.ELDER_FUTHARK, initial.selectedScript)

            // When: Preferences change
            preferencesFlow.value = defaultPreferences.copy(selectedScript = RunicScript.CIRTH)
            advanceUntilIdle()

            // Then: State reflects new script
            val updated = awaitItem()
            assertEquals(RunicScript.CIRTH, updated.selectedScript)

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
            elderFutharkTransliterator,
            youngerFutharkTransliterator,
            cirthTransliterator,
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
            assertEquals("ABCD", state.textLatin)
            assertTrue(state.runicElderPreview.isNotEmpty())
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
            elderFutharkTransliterator,
            youngerFutharkTransliterator,
            cirthTransliterator,
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
            assertEquals(longText, state.textLatin)
            assertTrue(state.runicElderPreview.length > longText.length / 2) // Rough check
            assertTrue(state.runicYoungerPreview.isNotEmpty())
            assertTrue(state.runicCirthPreview.isNotEmpty())
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
            elderFutharkTransliterator,
            youngerFutharkTransliterator,
            cirthTransliterator,
            savedStateHandle
        )
        advanceUntilIdle()

        // When: Setting text with unicode
        val unicodeText = "Café naïve résumé"
        viewModel.updateTextLatin(unicodeText)
        advanceUntilIdle()

        // Then: Previews are generated
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(unicodeText, state.textLatin)
            assertTrue(state.runicElderPreview.isNotEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
