package com.po4yka.runicquotes.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.google.common.truth.Truth.assertThat
import com.po4yka.runicquotes.domain.model.RunicScript
import io.mockk.every
import io.mockk.mockk
import java.util.UUID
import java.io.IOException
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class UserPreferencesManagerJvmTest {

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var preferencesManager: UserPreferencesManager
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher + Job())

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        dataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = {
                context.preferencesDataStoreFile("unit_preferences_${UUID.randomUUID()}")
            }
        )
        preferencesManager = UserPreferencesManager(dataStore)
    }

    @Test
    fun `widget and library filter preferences persist`() = testScope.runTest {
        preferencesManager.updateWidgetDisplayMode("daily_random_tap")
        preferencesManager.updateWidgetUpdateMode("every_12_hours")
        preferencesManager.updateQuoteListFilter("favorites")
        preferencesManager.updateQuoteSearchQuery("tolkien")
        preferencesManager.updateQuoteAuthorFilter("Le Guin")
        preferencesManager.updateQuoteLengthFilter("medium")
        preferencesManager.updateQuoteCollectionFilter("wisdom")

        val preferences = preferencesManager.userPreferencesFlow.first()
        assertThat(preferences.widgetDisplayMode).isEqualTo("daily_random_tap")
        assertThat(preferences.widgetUpdateMode).isEqualTo("every_12_hours")
        assertThat(preferences.quoteListFilter).isEqualTo("favorites")
        assertThat(preferences.quoteSearchQuery).isEqualTo("tolkien")
        assertThat(preferences.quoteAuthorFilter).isEqualTo("Le Guin")
        assertThat(preferences.quoteLengthFilter).isEqualTo("medium")
        assertThat(preferences.quoteCollectionFilter).isEqualTo("wisdom")
    }

    @Test
    fun `theme icon and accessibility preferences persist`() = testScope.runTest {
        preferencesManager.updateSelectedScript(RunicScript.YOUNGER_FUTHARK)
        preferencesManager.updateThemeMode("dark")
        preferencesManager.updateDynamicColorEnabled(true)
        preferencesManager.updateThemePack("night_ink")
        preferencesManager.updateAppIconVariant("ember")
        preferencesManager.updateLargeRunesEnabled(true)
        preferencesManager.updateHighContrastEnabled(true)
        preferencesManager.updateReducedMotionEnabled(true)

        val preferences = preferencesManager.userPreferencesFlow.first()
        assertThat(preferences.selectedScript).isEqualTo(RunicScript.YOUNGER_FUTHARK)
        assertThat(preferences.themeMode).isEqualTo("dark")
        assertThat(preferences.dynamicColorEnabled).isTrue()
        assertThat(preferences.themePack).isEqualTo("night_ink")
        assertThat(preferences.appIconVariant).isEqualTo("ember")
        assertThat(preferences.largeRunesEnabled).isTrue()
        assertThat(preferences.highContrastEnabled).isTrue()
        assertThat(preferences.reducedMotionEnabled).isTrue()
    }

    @Test
    fun `notification and onboarding preferences persist`() = testScope.runTest {
        preferencesManager.updateHasCompletedOnboarding(true)
        preferencesManager.updateDailyQuoteNotifications(false)
        preferencesManager.updateStreakNotifications(false)
        preferencesManager.updatePackUpdateNotifications(false)
        preferencesManager.updateLastQuoteDate(10L)
        preferencesManager.updateLastDailyQuoteId(22L)

        val preferences = preferencesManager.userPreferencesFlow.first()
        assertThat(preferences.hasCompletedOnboarding).isTrue()
        assertThat(preferences.dailyQuoteNotifications).isFalse()
        assertThat(preferences.streakNotifications).isFalse()
        assertThat(preferences.packUpdateNotifications).isFalse()
        assertThat(preferences.lastQuoteDate).isEqualTo(10L)
        assertThat(preferences.lastDailyQuoteId).isEqualTo(22L)
    }

    @Test
    fun `clearPreferences resets extended preference set to defaults`() = testScope.runTest {
        preferencesManager.updateWidgetDisplayMode("daily_random_tap")
        preferencesManager.updateThemePack("night_ink")
        preferencesManager.updateAppIconVariant("ember")
        preferencesManager.updateHighContrastEnabled(true)
        preferencesManager.updateHasCompletedOnboarding(true)
        preferencesManager.updateDailyQuoteNotifications(false)

        preferencesManager.clearPreferences()

        val preferences = preferencesManager.userPreferencesFlow.first()
        assertThat(preferences.widgetDisplayMode).isEqualTo("rune_latin")
        assertThat(preferences.themePack).isEqualTo("stone")
        assertThat(preferences.appIconVariant).isEqualTo("storm_slate")
        assertThat(preferences.highContrastEnabled).isFalse()
        assertThat(preferences.hasCompletedOnboarding).isFalse()
        assertThat(preferences.dailyQuoteNotifications).isTrue()
    }

    @Test
    fun `io failures while reading preferences fall back to defaults`() = testScope.runTest {
        val failingDataStore = mockk<DataStore<Preferences>>()
        every { failingDataStore.data } returns flow { throw IOException("disk") }

        val manager = UserPreferencesManager(failingDataStore)
        val preferences = manager.userPreferencesFlow.first()

        assertThat(preferences.selectedScript).isEqualTo(RunicScript.DEFAULT)
        assertThat(preferences.themeMode).isEqualTo("system")
        assertThat(preferences.widgetUpdateMode).isEqualTo("daily")
    }
}
