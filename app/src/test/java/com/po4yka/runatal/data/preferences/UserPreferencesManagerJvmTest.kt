package com.po4yka.runatal.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.google.common.truth.Truth.assertThat
import com.po4yka.runatal.domain.model.RunicScript
import io.mockk.every
import io.mockk.mockk
import java.util.UUID
import java.io.IOException
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import android.app.Application
import org.robolectric.annotation.Config
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
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

    @After
    fun tearDown() {
        testScope.cancel()
    }

    @Test
    fun `a retired search owner cannot overwrite a newer intent even with a late final flush`() = runTest {
        val oldGeneration = preferencesManager.reserveQuoteSearchWrite()
        preferencesManager.updateQuoteSearchQuery("a", oldGeneration)
        val oldPendingGeneration = preferencesManager.reserveQuoteSearchWrite()
        val newGeneration = preferencesManager.reserveQuoteSearchWrite()
        preferencesManager.updateQuoteSearchQuery("xyz", newGeneration)
        preferencesManager.updateQuoteSearchQuery("abc", oldPendingGeneration)
        assertThat(preferencesManager.userPreferencesFlow.first().quoteSearchQuery).isEqualTo("xyz")
    }

    @Test
    fun `daily selection retains identity across same day additions reordering and duplicate candidates`() =
        testScope.runTest {
            preferencesManager.updateThemeMode("dark")
            val chosen = preferencesManager.selectDailyQuote(20_735L) { listOf(1L, 2L, 3L) }
            val again = preferencesManager.selectDailyQuote(20_735L) { listOf(99L, 3L, 1L, 2L, 2L) }
            assertThat(again).isEqualTo(chosen)
            val preferences = preferencesManager.userPreferencesFlow.first()
            assertThat(preferences.lastQuoteDate).isEqualTo(20_735L)
            assertThat(preferences.lastDailyQuoteId).isEqualTo(chosen)
            assertThat(preferences.themeMode).isEqualTo("dark")
        }

    @Test
    fun `daily selection replaces missing identity and rotates on full local date changes`() = testScope.runTest {
        assertThat(preferencesManager.selectDailyQuote(0L) { listOf(7L) }).isEqualTo(7L)
        assertThat(preferencesManager.selectDailyQuote(0L) { listOf(8L) }).isEqualTo(8L)
        val tomorrow = preferencesManager.selectDailyQuote(1L) { listOf(7L, 8L) }
        assertThat(tomorrow).isEqualTo(7L)
        val preferences = preferencesManager.userPreferencesFlow.first()
        assertThat(preferences.lastQuoteDate).isEqualTo(1L)
        assertThat(preferences.lastDailyQuoteId).isEqualTo(7L)
        assertThat(preferencesManager.selectDailyQuote(-1L) { listOf(7L, 8L) }).isEqualTo(8L)
    }

    @Test
    fun `empty daily catalog clears both identity fields and invalid ids never mutate state`() = testScope.runTest {
        preferencesManager.selectDailyQuote(10L) { listOf(7L) }
        val failure = runCatching { preferencesManager.selectDailyQuote(11L) { listOf(0L) } }.exceptionOrNull()
        assertThat(failure).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(preferencesManager.userPreferencesFlow.first().lastDailyQuoteId).isEqualTo(7L)
        assertThat(preferencesManager.selectDailyQuote(11L) { emptyList() }).isNull()
        val preferences = preferencesManager.userPreferencesFlow.first()
        assertThat(preferences.lastQuoteDate).isEqualTo(0L)
        assertThat(preferences.lastDailyQuoteId).isEqualTo(0L)
    }

    @Test
    fun `concurrent daily readers persist one complete day and identity pair`() = testScope.runTest {
        val selected = (1..24).map {
            async { preferencesManager.selectDailyQuote(12L) { listOf(3L, 1L, 2L) } }
        }.awaitAll()
        assertThat(selected.toSet()).hasSize(1)
        val preferences = preferencesManager.userPreferencesFlow.first()
        assertThat(preferences.lastQuoteDate).isEqualTo(12L)
        assertThat(preferences.lastDailyQuoteId).isEqualTo(selected.first())
    }

    @Test
    fun `candidate providers run within serialized edits and see the newly selected identity`() = testScope.runTest {
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val first = async {
            preferencesManager.selectDailyQuote(0L) {
                entered.complete(Unit)
                release.await()
                listOf(100L)
            }
        }
        entered.await()
        var secondEntered = false
        val second = async {
            preferencesManager.selectDailyQuote(0L) {
                secondEntered = true
                listOf(1L, 100L)
            }
        }
        assertThat(secondEntered).isFalse()
        release.complete(Unit)
        assertThat(first.await()).isEqualTo(100L)
        assertThat(second.await()).isEqualTo(100L)
        assertThat(secondEntered).isTrue()
    }

    @Test
    fun `unchanged daily catalog visits every identity instead of alternating a subset`() = testScope.runTest {
        val selected = (0L..5L).map { day -> preferencesManager.selectDailyQuote(day) { listOf(1L, 2L, 3L) } }
        assertThat(selected).containsExactly(1L, 2L, 3L, 1L, 2L, 3L).inOrder()
    }

    @Test
    fun `candidate read failure leaves persisted daily identity untouched`() = testScope.runTest {
        preferencesManager.selectDailyQuote(10L) { listOf(7L) }
        val failure = runCatching {
            preferencesManager.selectDailyQuote(11L) { throw IOException("database unavailable") }
        }.exceptionOrNull()
        assertThat(failure).isInstanceOf(IOException::class.java)
        val preferences = preferencesManager.userPreferencesFlow.first()
        assertThat(preferences.lastQuoteDate).isEqualTo(10L)
        assertThat(preferences.lastDailyQuoteId).isEqualTo(7L)
    }

    @Test
    fun `widget and library filter preferences persist`() = testScope.runTest {
        preferencesManager.updateWidgetDisplayMode("daily_random_tap")
        preferencesManager.updateWidgetUpdateMode("every_12_hours")
        preferencesManager.updateQuoteListFilter("favorites")
        preferencesManager.updateQuoteSearchQuery("tolkien", preferencesManager.reserveQuoteSearchWrite())
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
        preferencesManager.selectDailyQuote(10L) { listOf(22L) }

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
