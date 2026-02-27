package com.po4yka.runicquotes.ui.screens.settings

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
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
            assertThat(preferences).isEqualTo(defaultPreferences)
        }
    }

    @Test
    fun `userPreferences flow reflects initial state`() = runTest {
        // When: Observing preferences
        viewModel.userPreferences.test {
            val prefs = awaitItem()

            // Then: All default values are present
            assertThat(prefs.selectedScript).isEqualTo(RunicScript.ELDER_FUTHARK)
            assertThat(prefs.selectedFont).isEqualTo("noto")
            assertThat(prefs.widgetUpdateMode).isEqualTo("daily")
            assertThat(prefs.lastQuoteDate).isEqualTo(0L)
            assertThat(prefs.lastDailyQuoteId).isEqualTo(0L)
            assertThat(prefs.themeMode).isEqualTo("system")
            assertThat(prefs.showTransliteration).isTrue()
            assertThat(prefs.fontSize).isWithin(0.001f).of(1.0f)
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
            assertThat(updatedPrefs.selectedScript).isEqualTo(RunicScript.YOUNGER_FUTHARK)
        }
    }

    @Test
    fun `updateSelectedScript to Cirth works correctly`() = runTest {
        viewModel.userPreferences.test {
            // Given: Initial state
            val initial = awaitItem()
            assertThat(initial.selectedScript).isEqualTo(RunicScript.ELDER_FUTHARK)

            // When: Updating to Cirth
            viewModel.updateSelectedScript(RunicScript.CIRTH)

            // Then: Preferences reflect change
            val updated = awaitItem()
            assertThat(updated.selectedScript).isEqualTo(RunicScript.CIRTH)
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
            assertThat(prefs.selectedScript).isEqualTo(RunicScript.ELDER_FUTHARK)
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
            assertThat(updatedPrefs.selectedFont).isEqualTo("babelstone")
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
            assertThat(updatedPrefs.themeMode).isEqualTo("dark")
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
            assertThat(updated.themeMode).isEqualTo("light")
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
            assertThat(prefs.themeMode).isEqualTo("system")
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
            assertThat(updatedPrefs.showTransliteration).isFalse()
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
            assertThat(prefs1.showTransliteration).isFalse()

            // When: Toggling back to true
            viewModel.updateShowTransliteration(true)
            advanceUntilIdle()
            val prefs2 = awaitItem()
            assertThat(prefs2.showTransliteration).isTrue()
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
            assertThat(updatedPrefs.fontSize).isWithin(0.001f).of(1.5f)
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
            assertThat(updated.fontSize).isWithin(0.001f).of(0.5f)
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
            assertThat(updated.fontSize).isWithin(0.001f).of(2.5f)
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
            assertThat(final.selectedScript).isEqualTo(RunicScript.YOUNGER_FUTHARK)
            assertThat(final.selectedFont).isEqualTo("babelstone")
            assertThat(final.themeMode).isEqualTo("dark")
            assertThat(final.showTransliteration).isFalse()
            assertThat(final.fontSize).isWithin(0.001f).of(1.2f)
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
            assertThat(final.fontSize).isWithin(0.001f).of(1.5f)
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
            assertThat(updated.selectedScript).isEqualTo(RunicScript.CIRTH)
        }

        // Then: New collectors see the updated state
        viewModel.userPreferences.test {
            val prefs = awaitItem()
            assertThat(prefs.selectedScript).isEqualTo(RunicScript.CIRTH)
        }
    }

    @Test
    fun `userPreferences flow is hot and shares state`() = runTest {
        // When: Collecting from multiple points
        viewModel.userPreferences.test {
            val prefs = awaitItem()
            assertThat(prefs.selectedScript).isEqualTo(RunicScript.ELDER_FUTHARK)
            expectNoEvents()
        }

        viewModel.userPreferences.test {
            val prefs = awaitItem()
            assertThat(prefs.selectedScript).isEqualTo(RunicScript.ELDER_FUTHARK)
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
            assertThat(updated.themeMode).isEqualTo("dark")

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
            assertThat(updated.fontSize).isWithin(0.001f).of(0f)
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
