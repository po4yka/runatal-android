package com.po4yka.runatal.ui.widget

import android.app.Application
import android.content.Context
import androidx.glance.GlanceId
import com.google.common.truth.Truth.assertThat
import com.po4yka.runatal.data.preferences.UserPreferences
import com.po4yka.runatal.domain.model.Quote
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class WidgetLibraryInvalidationTest {
    private val context = RuntimeEnvironment.getApplication()
    private val date = LocalDate.of(2026, 10, 9)
    private val preferences = UserPreferences()

    @After
    fun tearDown() {
        WidgetStateCache.clear()
        PersistentWidgetStateCache.clear(context)
    }

    @Test
    fun `committed source change invalidates snapshots after an older loader finishes`() = runTest {
        val quotes = MutableStateFlow(emptyList<Quote>())
        var refreshes = 0
        val manager = WidgetSyncManager(backgroundScope, refreshRunner = object : WidgetRefreshRunner {
            override suspend fun refreshAll(context: Context) {
                refreshes++
                assertThat(WidgetStateCache.quoteId("widget", date)).isNull()
                assertThat(PersistentWidgetStateCache.quoteId(context, "widget", date)).isNull()
            }

            override suspend fun refresh(context: Context, glanceId: GlanceId) = Unit
        })
        val observer = manager.observeLibrary(context, quotes)
        runCurrent()
        assertThat(refreshes).isEqualTo(1)

        WidgetContentLock.mutex.lock()
        try {
            quotes.value = listOf(Quote(7, "Edited", "Author", "ᚾ", null, null))
            runCurrent()
            assertThat(refreshes).isEqualTo(1)
            val stale = WidgetState(quoteId = 7, latinText = "Before edit", author = "Author", runicText = "ᚠ")
            WidgetStateCache.put("widget", date, preferences, 300, 151, "test", stale)
            PersistentWidgetStateCache.put(context, "widget", date, preferences, 300, 151, "test", stale, null)
        } finally {
            WidgetContentLock.mutex.unlock()
        }
        runCurrent()

        assertThat(refreshes).isEqualTo(2)
        assertThat(WidgetStateCache.quoteId("widget", date)).isNull()
        assertThat(PersistentWidgetStateCache.quoteId(context, "widget", date)).isNull()
        observer.cancel()
    }
}
