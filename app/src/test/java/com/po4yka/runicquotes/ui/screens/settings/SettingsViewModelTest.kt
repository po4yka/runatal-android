package com.po4yka.runicquotes.ui.screens.settings

import app.cash.turbine.test
import com.po4yka.runicquotes.data.preferences.UserPreferences
import com.po4yka.runicquotes.data.preferences.UserPreferencesManager
import com.po4yka.runicquotes.domain.model.RunicScript
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Comprehensive unit tests for SettingsViewModel.
 * Tests StateFlow emissions and preference update operations.
 *
 * Coverage goals: >90%
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private lateinit var userPreferencesManager: UserPreferencesManager
    private lateinit var viewModel: SettingsViewModel

    private val testDispatcher = StandardTestDispatcher()

    private val defaultPreferences = UserPreferences(
        selectedScript = RunicScript.ELDER_FUTHARK,
        selectedFont = "noto",
        widgetUpdateMode = "daily",
        lastQuoteDate = 0L,
        lastDailyQuoteId = 0L,
        themeMode = "system",
        showTransliteration = true,
        fontSize = 1.0f
    )

    private lateinit var preferencesFlow: MutableStateFlow<UserPreferences>

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        // Create mock
        userPreferencesManager = mockk(relaxed = true)

        // Set up preferences flow
        preferencesFlow = MutableStateFlow(defaultPreferences)
        every { userPreferencesManager.userPreferencesFlow } returns preferencesFlow
        coEvery { userPreferencesManager.updateSelectedScript(any()) } coAnswers {
            preferencesFlow.value = preferencesFlow.value.copy(selectedScript = firstArg())
        }
        coEvery { userPreferencesManager.updateSelectedFont(any()) } coAnswers {
            preferencesFlow.value = preferencesFlow.value.copy(selectedFont = firstArg())
        }
        coEvery { userPreferencesManager.updateThemeMode(any()) } coAnswers {
            preferencesFlow.value = preferencesFlow.value.copy(themeMode = firstArg())
        }
        coEvery { userPreferencesManager.updateShowTransliteration(any()) } coAnswers {
            preferencesFlow.value = preferencesFlow.value.copy(showTransliteration = firstArg())
        }
        coEvery { userPreferencesManager.updateFontSize(any()) } coAnswers {
            preferencesFlow.value = preferencesFlow.value.copy(fontSize = firstArg())
        }

        // Create ViewModel
        viewModel = SettingsViewModel(userPreferencesManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ==================== Initialization Tests ====================

    @Test
    fun `viewModel initializes with default preferences`() = runTest {
        // When: ViewModel is created
        // Then: Initial preferences are exposed
        viewModel.userPreferences.test {
            val preferences = awaitItem()
            assertEquals(defaultPreferences, preferences)
        }
    }

    @Test
    fun `userPreferences flow reflects initial state`() = runTest {
        // When: Observing preferences
        viewModel.userPreferences.test {
            val prefs = awaitItem()

            // Then: All default values are present
            assertEquals(RunicScript.ELDER_FUTHARK, prefs.selectedScript)
            assertEquals("noto", prefs.selectedFont)
            assertEquals("daily", prefs.widgetUpdateMode)
            assertEquals(0L, prefs.lastQuoteDate)
            assertEquals(0L, prefs.lastDailyQuoteId)
            assertEquals("system", prefs.themeMode)
            assertTrue(prefs.showTransliteration)
            assertEquals(1.0f, prefs.fontSize, 0.001f)
        }
    }

    // ==================== Update Script Tests ====================

    @Test
    fun `updateSelectedScript calls preferences manager`() = runTest {
        // When: Updating selected script
        viewModel.updateSelectedScript(RunicScript.YOUNGER_FUTHARK)
        advanceUntilIdle()

        // Then: Preferences manager is called
        coVerify { userPreferencesManager.updateSelectedScript(RunicScript.YOUNGER_FUTHARK) }
    }

    @Test
    fun `updateSelectedScript updates flow`() = runTest {
        // Given: Observing preferences
        viewModel.userPreferences.test {
            skipItems(1) // Skip initial state

            // When: Updating script
            viewModel.updateSelectedScript(RunicScript.YOUNGER_FUTHARK)
            advanceUntilIdle()

            // Then: Flow emits updated value
            val updatedPrefs = awaitItem()
            assertEquals(RunicScript.YOUNGER_FUTHARK, updatedPrefs.selectedScript)
        }
    }

    @Test
    fun `updateSelectedScript to Cirth works correctly`() = runTest {
        viewModel.userPreferences.test {
            // Given: Initial state
            val initial = awaitItem()
            assertEquals(RunicScript.ELDER_FUTHARK, initial.selectedScript)

            // When: Updating to Cirth
            viewModel.updateSelectedScript(RunicScript.CIRTH)

            // Then: Preferences reflect change
            val updated = awaitItem()
            assertEquals(RunicScript.CIRTH, updated.selectedScript)
        }
    }

    @Test
    fun `multiple script updates are applied in order`() = runTest {
        // When: Multiple rapid updates
        viewModel.updateSelectedScript(RunicScript.YOUNGER_FUTHARK)
        viewModel.updateSelectedScript(RunicScript.CIRTH)
        viewModel.updateSelectedScript(RunicScript.ELDER_FUTHARK)
        advanceUntilIdle()

        // Then: Final value is applied
        viewModel.userPreferences.test {
            val prefs = awaitItem()
            assertEquals(RunicScript.ELDER_FUTHARK, prefs.selectedScript)
        }
    }

    // ==================== Update Font Tests ====================

    @Test
    fun `updateSelectedFont calls preferences manager`() = runTest {
        // When: Updating font
        viewModel.updateSelectedFont("babelstone")
        advanceUntilIdle()

        // Then: Preferences manager is called
        coVerify { userPreferencesManager.updateSelectedFont("babelstone") }
    }

    @Test
    fun `updateSelectedFont updates flow`() = runTest {
        // Given: Observing preferences
        viewModel.userPreferences.test {
            skipItems(1)

            // When: Updating font
            viewModel.updateSelectedFont("babelstone")
            advanceUntilIdle()

            // Then: Flow emits updated value
            val updatedPrefs = awaitItem()
            assertEquals("babelstone", updatedPrefs.selectedFont)
        }
    }

    @Test
    fun `updateSelectedFont handles empty string`() = runTest {
        // When: Updating to empty string
        viewModel.updateSelectedFont("")
        advanceUntilIdle()

        // Then: Manager is called with empty string
        coVerify { userPreferencesManager.updateSelectedFont("") }
    }

    // ==================== Update Theme Mode Tests ====================

    @Test
    fun `updateThemeMode calls preferences manager`() = runTest {
        // When: Updating theme mode
        viewModel.updateThemeMode("dark")
        advanceUntilIdle()

        // Then: Preferences manager is called
        coVerify { userPreferencesManager.updateThemeMode("dark") }
    }

    @Test
    fun `updateThemeMode updates flow`() = runTest {
        // Given: Observing preferences
        viewModel.userPreferences.test {
            skipItems(1)

            // When: Updating theme
            viewModel.updateThemeMode("dark")
            advanceUntilIdle()

            // Then: Flow emits updated value
            val updatedPrefs = awaitItem()
            assertEquals("dark", updatedPrefs.themeMode)
        }
    }

    @Test
    fun `updateThemeMode supports light mode`() = runTest {
        viewModel.userPreferences.test {
            // Given: Initial state
            awaitItem()

            // When: Setting to light mode
            viewModel.updateThemeMode("light")

            // Then: Preferences reflect change
            val updated = awaitItem()
            assertEquals("light", updated.themeMode)
        }
    }

    @Test
    fun `updateThemeMode supports system mode`() = runTest {
        // When: Setting to system mode
        viewModel.updateThemeMode("system")
        advanceUntilIdle()

        // Then: Preferences reflect change
        viewModel.userPreferences.test {
            val prefs = awaitItem()
            assertEquals("system", prefs.themeMode)
        }
    }

    // ==================== Update Show Transliteration Tests ====================

    @Test
    fun `updateShowTransliteration calls preferences manager`() = runTest {
        // When: Toggling transliteration
        viewModel.updateShowTransliteration(false)
        advanceUntilIdle()

        // Then: Preferences manager is called
        coVerify { userPreferencesManager.updateShowTransliteration(false) }
    }

    @Test
    fun `updateShowTransliteration updates flow`() = runTest {
        // Given: Observing preferences
        viewModel.userPreferences.test {
            skipItems(1)

            // When: Disabling transliteration
            viewModel.updateShowTransliteration(false)
            advanceUntilIdle()

            // Then: Flow emits updated value
            val updatedPrefs = awaitItem()
            assertFalse(updatedPrefs.showTransliteration)
        }
    }

    @Test
    fun `updateShowTransliteration toggles correctly`() = runTest {
        // Given: Initially true
        viewModel.userPreferences.test {
            skipItems(1)

            // When: Toggling to false
            viewModel.updateShowTransliteration(false)
            advanceUntilIdle()
            val prefs1 = awaitItem()
            assertFalse(prefs1.showTransliteration)

            // When: Toggling back to true
            viewModel.updateShowTransliteration(true)
            advanceUntilIdle()
            val prefs2 = awaitItem()
            assertTrue(prefs2.showTransliteration)
        }
    }

    // ==================== Update Font Size Tests ====================

    @Test
    fun `updateFontSize calls preferences manager`() = runTest {
        // When: Updating font size
        viewModel.updateFontSize(1.5f)
        advanceUntilIdle()

        // Then: Preferences manager is called
        coVerify { userPreferencesManager.updateFontSize(1.5f) }
    }

    @Test
    fun `updateFontSize updates flow`() = runTest {
        // Given: Observing preferences
        viewModel.userPreferences.test {
            skipItems(1)

            // When: Updating font size
            viewModel.updateFontSize(1.5f)
            advanceUntilIdle()

            // Then: Flow emits updated value
            val updatedPrefs = awaitItem()
            assertEquals(1.5f, updatedPrefs.fontSize, 0.001f)
        }
    }

    @Test
    fun `updateFontSize handles small values`() = runTest {
        // Given: Observing preferences
        viewModel.userPreferences.test {
            // Initial state
            awaitItem()

            // When: Setting to small value
            viewModel.updateFontSize(0.5f)

            // Then: Value is updated
            val updated = awaitItem()
            assertEquals(0.5f, updated.fontSize, 0.001f)
        }
    }

    @Test
    fun `updateFontSize handles large values`() = runTest {
        // Given: Observing preferences
        viewModel.userPreferences.test {
            // Initial state
            awaitItem()

            // When: Setting to large value
            viewModel.updateFontSize(2.5f)

            // Then: Value is updated
            val updated = awaitItem()
            assertEquals(2.5f, updated.fontSize, 0.001f)
        }
    }

    // ==================== Multiple Updates Tests ====================

    @Test
    fun `multiple different preference updates work correctly`() = runTest {
        // Given: Observing preferences
        viewModel.userPreferences.test {
            // Initial state
            awaitItem()

            // When: Updating multiple preferences
            viewModel.updateSelectedScript(RunicScript.YOUNGER_FUTHARK)
            awaitItem() // Script update

            viewModel.updateSelectedFont("babelstone")
            awaitItem() // Font update

            viewModel.updateThemeMode("dark")
            awaitItem() // Theme update

            viewModel.updateShowTransliteration(false)
            awaitItem() // Transliteration update

            viewModel.updateFontSize(1.2f)

            // Then: All updates are reflected in final state
            val final = awaitItem()
            assertEquals(RunicScript.YOUNGER_FUTHARK, final.selectedScript)
            assertEquals("babelstone", final.selectedFont)
            assertEquals("dark", final.themeMode)
            assertFalse(final.showTransliteration)
            assertEquals(1.2f, final.fontSize, 0.001f)
        }
    }

    @Test
    fun `rapid sequential updates are handled`() = runTest {
        // Given: Observing preferences
        viewModel.userPreferences.test {
            // Initial state
            awaitItem()

            // When: Rapid font size changes
            viewModel.updateFontSize(1.1f)
            awaitItem() // 1.1f

            viewModel.updateFontSize(1.2f)
            awaitItem() // 1.2f

            viewModel.updateFontSize(1.3f)
            awaitItem() // 1.3f

            viewModel.updateFontSize(1.4f)
            awaitItem() // 1.4f

            viewModel.updateFontSize(1.5f)

            // Then: Final value is applied
            val final = awaitItem()
            assertEquals(1.5f, final.fontSize, 0.001f)
        }
    }

    // ==================== StateFlow Behavior Tests ====================

    @Test
    fun `userPreferences flow persists after updates`() = runTest {
        // Given: Observing preferences and updating
        viewModel.userPreferences.test {
            // Initial state
            awaitItem()

            // When: Updating preferences
            viewModel.updateSelectedScript(RunicScript.CIRTH)

            // Then: Preferences are updated
            val updated = awaitItem()
            assertEquals(RunicScript.CIRTH, updated.selectedScript)
        }

        // Then: New collectors see the updated state
        viewModel.userPreferences.test {
            val prefs = awaitItem()
            assertEquals(RunicScript.CIRTH, prefs.selectedScript)
        }
    }

    @Test
    fun `userPreferences flow is hot and shares state`() = runTest {
        // When: Collecting from multiple points
        viewModel.userPreferences.test {
            val prefs = awaitItem()
            assertEquals(RunicScript.ELDER_FUTHARK, prefs.selectedScript)
            expectNoEvents()
        }

        viewModel.userPreferences.test {
            val prefs = awaitItem()
            assertEquals(RunicScript.ELDER_FUTHARK, prefs.selectedScript)
        }

        // Both collectors see the same initial state
    }

    // ==================== Edge Cases ====================

    @Test
    fun `updating same value multiple times works`() = runTest {
        // Given: Observing preferences
        viewModel.userPreferences.test {
            // Initial state
            awaitItem()

            // When: Setting same value repeatedly
            viewModel.updateThemeMode("dark")
            viewModel.updateThemeMode("dark")
            viewModel.updateThemeMode("dark")

            // Then: Value is set (only one emission because values are the same)
            val updated = awaitItem()
            assertEquals("dark", updated.themeMode)

            // And preferences manager was called each time
            coVerify(exactly = 3) { userPreferencesManager.updateThemeMode("dark") }
        }
    }

    @Test
    fun `zero font size is handled`() = runTest {
        // Given: Observing preferences
        viewModel.userPreferences.test {
            // Initial state
            awaitItem()

            // When: Setting font size to zero
            viewModel.updateFontSize(0f)

            // Then: Value is updated (validation is in data layer)
            val updated = awaitItem()
            assertEquals(0f, updated.fontSize, 0.001f)
        }
    }

    @Test
    fun `negative font size is passed to manager`() = runTest {
        // When: Setting negative font size
        viewModel.updateFontSize(-1.0f)
        advanceUntilIdle()

        // Then: Manager is called (validation is its responsibility)
        coVerify { userPreferencesManager.updateFontSize(-1.0f) }
    }
}
