package com.po4yka.runicquotes.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.po4yka.runicquotes.domain.model.RunicScript
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for UserPreferencesManager.
 * Tests DataStore operations on an Android device/emulator.
 *
 * These tests verify:
 * - Preference reads and writes
 * - Flow emissions
 * - Default values
 * - Data persistence
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class UserPreferencesManagerTest {

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var preferencesManager: UserPreferencesManager
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher + Job())

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // Create a test DataStore with unique name to avoid conflicts
        dataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = {
                context.preferencesDataStoreFile("test_preferences_${System.currentTimeMillis()}")
            }
        )

        preferencesManager = UserPreferencesManager(dataStore)
    }

    @After
    fun tearDown() {
        // DataStore will be cleaned up when testScope is cancelled
    }

    // ==================== Default Values Tests ====================

    @Test
    fun preferencesFlow_emitsDefaultValues_onFirstAccess() = testScope.runTest {
        // When: Getting preferences for the first time
        val preferences = preferencesManager.userPreferencesFlow.first()

        // Then: Default values are returned
        assertEquals(RunicScript.DEFAULT, preferences.selectedScript)
        assertEquals("noto", preferences.selectedFont)
        assertEquals("daily", preferences.widgetUpdateMode)
        assertEquals(0L, preferences.lastQuoteDate)
        assertEquals(0L, preferences.lastDailyQuoteId)
        assertEquals("system", preferences.themeMode)
        assertTrue(preferences.showTransliteration)
        assertEquals(1.0f, preferences.fontSize, 0.001f)
    }

    // ==================== Update Selected Script Tests ====================

    @Test
    fun updateSelectedScript_updatesAndPersists() = testScope.runTest {
        // When: Updating selected script
        preferencesManager.updateSelectedScript(RunicScript.YOUNGER_FUTHARK)

        // Then: Preference is updated
        val preferences = preferencesManager.userPreferencesFlow.first()
        assertEquals(RunicScript.YOUNGER_FUTHARK, preferences.selectedScript)
    }

    @Test
    fun updateSelectedScript_toAllScripts_works() = testScope.runTest {
        // When: Updating to Elder Futhark
        preferencesManager.updateSelectedScript(RunicScript.ELDER_FUTHARK)
        var prefs = preferencesManager.userPreferencesFlow.first()
        assertEquals(RunicScript.ELDER_FUTHARK, prefs.selectedScript)

        // When: Updating to Younger Futhark
        preferencesManager.updateSelectedScript(RunicScript.YOUNGER_FUTHARK)
        prefs = preferencesManager.userPreferencesFlow.first()
        assertEquals(RunicScript.YOUNGER_FUTHARK, prefs.selectedScript)

        // When: Updating to Cirth
        preferencesManager.updateSelectedScript(RunicScript.CIRTH)
        prefs = preferencesManager.userPreferencesFlow.first()
        assertEquals(RunicScript.CIRTH, prefs.selectedScript)
    }

    // ==================== Update Selected Font Tests ====================

    @Test
    fun updateSelectedFont_updatesAndPersists() = testScope.runTest {
        // When: Updating selected font
        preferencesManager.updateSelectedFont("babelstone")

        // Then: Preference is updated
        val preferences = preferencesManager.userPreferencesFlow.first()
        assertEquals("babelstone", preferences.selectedFont)
    }

    @Test
    fun updateSelectedFont_handlesEmptyString() = testScope.runTest {
        // When: Updating to empty string
        preferencesManager.updateSelectedFont("")

        // Then: Empty string is persisted
        val preferences = preferencesManager.userPreferencesFlow.first()
        assertEquals("", preferences.selectedFont)
    }

    @Test
    fun updateSelectedFont_handlesCirthFont() = testScope.runTest {
        // When: Updating to cirth font
        preferencesManager.updateSelectedFont("cirth")

        // Then: Preference is updated
        val preferences = preferencesManager.userPreferencesFlow.first()
        assertEquals("cirth", preferences.selectedFont)
    }

    // ==================== Update Widget Update Mode Tests ====================

    @Test
    fun updateWidgetUpdateMode_updatesAndPersists() = testScope.runTest {
        // When: Updating widget update mode
        preferencesManager.updateWidgetUpdateMode("manual")

        // Then: Preference is updated
        val preferences = preferencesManager.userPreferencesFlow.first()
        assertEquals("manual", preferences.widgetUpdateMode)
    }

    @Test
    fun updateWidgetUpdateMode_handlesVariousModes() = testScope.runTest {
        // When: Updating to different modes
        preferencesManager.updateWidgetUpdateMode("hourly")
        var prefs = preferencesManager.userPreferencesFlow.first()
        assertEquals("hourly", prefs.widgetUpdateMode)

        preferencesManager.updateWidgetUpdateMode("daily")
        prefs = preferencesManager.userPreferencesFlow.first()
        assertEquals("daily", prefs.widgetUpdateMode)
    }

    // ==================== Update Last Quote Date Tests ====================

    @Test
    fun updateLastQuoteDate_updatesAndPersists() = testScope.runTest {
        // When: Updating last quote date
        preferencesManager.updateLastQuoteDate(12345L)

        // Then: Preference is updated
        val preferences = preferencesManager.userPreferencesFlow.first()
        assertEquals(12345L, preferences.lastQuoteDate)
    }

    @Test
    fun updateLastQuoteDate_handlesLargeValues() = testScope.runTest {
        // When: Updating with large timestamp
        val largeTimestamp = System.currentTimeMillis()
        preferencesManager.updateLastQuoteDate(largeTimestamp)

        // Then: Large value is persisted
        val preferences = preferencesManager.userPreferencesFlow.first()
        assertEquals(largeTimestamp, preferences.lastQuoteDate)
    }

    @Test
    fun updateLastQuoteDate_handlesZero() = testScope.runTest {
        // Given: Non-zero date
        preferencesManager.updateLastQuoteDate(12345L)

        // When: Resetting to zero
        preferencesManager.updateLastQuoteDate(0L)

        // Then: Zero is persisted
        val preferences = preferencesManager.userPreferencesFlow.first()
        assertEquals(0L, preferences.lastQuoteDate)
    }

    // ==================== Update Last Daily Quote ID Tests ====================

    @Test
    fun updateLastDailyQuoteId_updatesAndPersists() = testScope.runTest {
        // When: Updating last daily quote ID
        preferencesManager.updateLastDailyQuoteId(42L)

        // Then: Preference is updated
        val preferences = preferencesManager.userPreferencesFlow.first()
        assertEquals(42L, preferences.lastDailyQuoteId)
    }

    @Test
    fun updateLastDailyQuoteId_handlesSequentialUpdates() = testScope.runTest {
        // When: Updating multiple times
        preferencesManager.updateLastDailyQuoteId(1L)
        var prefs = preferencesManager.userPreferencesFlow.first()
        assertEquals(1L, prefs.lastDailyQuoteId)

        preferencesManager.updateLastDailyQuoteId(2L)
        prefs = preferencesManager.userPreferencesFlow.first()
        assertEquals(2L, prefs.lastDailyQuoteId)

        preferencesManager.updateLastDailyQuoteId(3L)
        prefs = preferencesManager.userPreferencesFlow.first()
        assertEquals(3L, prefs.lastDailyQuoteId)
    }

    // ==================== Update Theme Mode Tests ====================

    @Test
    fun updateThemeMode_updatesAndPersists() = testScope.runTest {
        // When: Updating theme mode
        preferencesManager.updateThemeMode("dark")

        // Then: Preference is updated
        val preferences = preferencesManager.userPreferencesFlow.first()
        assertEquals("dark", preferences.themeMode)
    }

    @Test
    fun updateThemeMode_supportsAllModes() = testScope.runTest {
        // When: Setting to light mode
        preferencesManager.updateThemeMode("light")
        var prefs = preferencesManager.userPreferencesFlow.first()
        assertEquals("light", prefs.themeMode)

        // When: Setting to dark mode
        preferencesManager.updateThemeMode("dark")
        prefs = preferencesManager.userPreferencesFlow.first()
        assertEquals("dark", prefs.themeMode)

        // When: Setting to system mode
        preferencesManager.updateThemeMode("system")
        prefs = preferencesManager.userPreferencesFlow.first()
        assertEquals("system", prefs.themeMode)
    }

    // ==================== Update Show Transliteration Tests ====================

    @Test
    fun updateShowTransliteration_updatesAndPersists() = testScope.runTest {
        // When: Disabling transliteration
        preferencesManager.updateShowTransliteration(false)

        // Then: Preference is updated
        val preferences = preferencesManager.userPreferencesFlow.first()
        assertFalse(preferences.showTransliteration)
    }

    @Test
    fun updateShowTransliteration_togglesCorrectly() = testScope.runTest {
        // When: Setting to false
        preferencesManager.updateShowTransliteration(false)
        var prefs = preferencesManager.userPreferencesFlow.first()
        assertFalse(prefs.showTransliteration)

        // When: Setting to true
        preferencesManager.updateShowTransliteration(true)
        prefs = preferencesManager.userPreferencesFlow.first()
        assertTrue(prefs.showTransliteration)

        // When: Setting to false again
        preferencesManager.updateShowTransliteration(false)
        prefs = preferencesManager.userPreferencesFlow.first()
        assertFalse(prefs.showTransliteration)
    }

    // ==================== Update Font Size Tests ====================

    @Test
    fun updateFontSize_updatesAndPersists() = testScope.runTest {
        // When: Updating font size
        preferencesManager.updateFontSize(1.5f)

        // Then: Preference is updated
        val preferences = preferencesManager.userPreferencesFlow.first()
        assertEquals(1.5f, preferences.fontSize, 0.001f)
    }

    @Test
    fun updateFontSize_handlesSmallValues() = testScope.runTest {
        // When: Setting to small value
        preferencesManager.updateFontSize(0.5f)

        // Then: Small value is persisted
        val preferences = preferencesManager.userPreferencesFlow.first()
        assertEquals(0.5f, preferences.fontSize, 0.001f)
    }

    @Test
    fun updateFontSize_handlesLargeValues() = testScope.runTest {
        // When: Setting to large value
        preferencesManager.updateFontSize(3.0f)

        // Then: Large value is persisted
        val preferences = preferencesManager.userPreferencesFlow.first()
        assertEquals(3.0f, preferences.fontSize, 0.001f)
    }

    @Test
    fun updateFontSize_handlesPreciseValues() = testScope.runTest {
        // When: Setting to precise value
        preferencesManager.updateFontSize(1.2345f)

        // Then: Precise value is persisted
        val preferences = preferencesManager.userPreferencesFlow.first()
        assertEquals(1.2345f, preferences.fontSize, 0.0001f)
    }

    // ==================== Clear Preferences Tests ====================

    @Test
    fun clearPreferences_resetsToDefaults() = testScope.runTest {
        // Given: Multiple preferences set
        preferencesManager.updateSelectedScript(RunicScript.YOUNGER_FUTHARK)
        preferencesManager.updateSelectedFont("babelstone")
        preferencesManager.updateThemeMode("dark")
        preferencesManager.updateShowTransliteration(false)
        preferencesManager.updateFontSize(2.0f)

        // When: Clearing preferences
        preferencesManager.clearPreferences()

        // Then: All preferences reset to defaults
        val preferences = preferencesManager.userPreferencesFlow.first()
        assertEquals(RunicScript.DEFAULT, preferences.selectedScript)
        assertEquals("noto", preferences.selectedFont)
        assertEquals("daily", preferences.widgetUpdateMode)
        assertEquals(0L, preferences.lastQuoteDate)
        assertEquals(0L, preferences.lastDailyQuoteId)
        assertEquals("system", preferences.themeMode)
        assertTrue(preferences.showTransliteration)
        assertEquals(1.0f, preferences.fontSize, 0.001f)
    }

    // ==================== Multiple Updates Tests ====================

    @Test
    fun multipleUpdates_allPersist() = testScope.runTest {
        // When: Updating multiple preferences
        preferencesManager.updateSelectedScript(RunicScript.CIRTH)
        preferencesManager.updateSelectedFont("babelstone")
        preferencesManager.updateThemeMode("dark")
        preferencesManager.updateShowTransliteration(false)
        preferencesManager.updateFontSize(1.8f)
        preferencesManager.updateWidgetUpdateMode("manual")
        preferencesManager.updateLastQuoteDate(99999L)
        preferencesManager.updateLastDailyQuoteId(88L)

        // Then: All preferences are updated
        val preferences = preferencesManager.userPreferencesFlow.first()
        assertEquals(RunicScript.CIRTH, preferences.selectedScript)
        assertEquals("babelstone", preferences.selectedFont)
        assertEquals("dark", preferences.themeMode)
        assertFalse(preferences.showTransliteration)
        assertEquals(1.8f, preferences.fontSize, 0.001f)
        assertEquals("manual", preferences.widgetUpdateMode)
        assertEquals(99999L, preferences.lastQuoteDate)
        assertEquals(88L, preferences.lastDailyQuoteId)
    }

    @Test
    fun rapidSequentialUpdates_lastUpdateWins() = testScope.runTest {
        // When: Rapidly updating same preference
        preferencesManager.updateFontSize(1.0f)
        preferencesManager.updateFontSize(1.5f)
        preferencesManager.updateFontSize(2.0f)
        preferencesManager.updateFontSize(2.5f)

        // Then: Last update is persisted
        val preferences = preferencesManager.userPreferencesFlow.first()
        assertEquals(2.5f, preferences.fontSize, 0.001f)
    }

    // ==================== Flow Emission Tests ====================

    @Test
    fun preferencesFlow_emitsAfterUpdate() = testScope.runTest {
        // When: Updating a preference
        preferencesManager.updateSelectedScript(RunicScript.CIRTH)

        // Then: Flow emits updated value
        val preferences = preferencesManager.userPreferencesFlow.first()
        assertEquals(RunicScript.CIRTH, preferences.selectedScript)
    }

    @Test
    fun preferencesFlow_emitsOnlyChangedValues() = testScope.runTest {
        // Given: Initial state
        val initial = preferencesManager.userPreferencesFlow.first()

        // When: Updating one preference
        preferencesManager.updateThemeMode("dark")

        // Then: Only that preference changes
        val updated = preferencesManager.userPreferencesFlow.first()
        assertEquals("dark", updated.themeMode)
        assertEquals(initial.selectedScript, updated.selectedScript)
        assertEquals(initial.selectedFont, updated.selectedFont)
        assertEquals(initial.showTransliteration, updated.showTransliteration)
    }

    // ==================== Edge Cases ====================

    @Test
    fun updateAfterClear_works() = testScope.runTest {
        // Given: Preferences set then cleared
        preferencesManager.updateSelectedScript(RunicScript.YOUNGER_FUTHARK)
        preferencesManager.clearPreferences()

        // When: Updating again after clear
        preferencesManager.updateSelectedScript(RunicScript.CIRTH)

        // Then: Update persists
        val preferences = preferencesManager.userPreferencesFlow.first()
        assertEquals(RunicScript.CIRTH, preferences.selectedScript)
    }

    @Test
    fun specialCharactersInStrings_arePersisted() = testScope.runTest {
        // When: Setting strings with special characters
        preferencesManager.updateSelectedFont("font-name_123")
        preferencesManager.updateThemeMode("custom:dark:variant")

        // Then: Special characters are preserved
        val preferences = preferencesManager.userPreferencesFlow.first()
        assertEquals("font-name_123", preferences.selectedFont)
        assertEquals("custom:dark:variant", preferences.themeMode)
    }

    @Test
    fun zeroFontSize_isPersisted() = testScope.runTest {
        // When: Setting font size to zero
        preferencesManager.updateFontSize(0.0f)

        // Then: Zero is persisted
        val preferences = preferencesManager.userPreferencesFlow.first()
        assertEquals(0.0f, preferences.fontSize, 0.001f)
    }

    @Test
    fun negativeFontSize_isPersisted() = testScope.runTest {
        // When: Setting negative font size
        preferencesManager.updateFontSize(-1.0f)

        // Then: Negative value is persisted (validation is in UI layer)
        val preferences = preferencesManager.userPreferencesFlow.first()
        assertEquals(-1.0f, preferences.fontSize, 0.001f)
    }
}
