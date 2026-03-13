package com.po4yka.runatal.ui.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class RefreshQuoteActionTest {

    @After
    fun tearDown() {
        WidgetStateCache.clear()
        WidgetInteractionState.clear()
        PersistentWidgetStateCache.clear(RuntimeEnvironment.getApplication())
    }

    @Test
    fun `onAction clears cache requests random quote and refreshes widget`() {
        val refreshRunner = RecordingRefreshRunner()
        val action = RefreshQuoteAction(refreshRunner)
        val context = RuntimeEnvironment.getApplication() as Context
        val glanceId = mockk<GlanceId>()
        every { glanceId.toString() } returns "widget-1"
        WidgetStateCache.put(
            widgetKey = "widget-1",
            date = java.time.LocalDate.of(2026, 3, 11),
            preferences = com.po4yka.runatal.data.preferences.UserPreferences(),
            widgetWidth = 300,
            widgetHeight = 151,
            state = WidgetState(latinText = "Cached")
        )
        PersistentWidgetStateCache.put(
            context = context,
            widgetKey = "widget-1",
            date = java.time.LocalDate.of(2026, 3, 11),
            preferences = com.po4yka.runatal.data.preferences.UserPreferences(),
            widgetWidth = 300,
            widgetHeight = 151,
            state = WidgetState(latinText = "Cached"),
            bitmapCacheKey = null
        )

        kotlinx.coroutines.test.runTest {
            action.onAction(context, glanceId, mockk<ActionParameters>(relaxed = true))
        }

        assertThat(refreshRunner.refreshedIds).containsExactly(glanceId)
        assertThat(
            WidgetStateCache.get(
                widgetKey = "widget-1",
                currentDate = java.time.LocalDate.of(2026, 3, 11),
                preferences = com.po4yka.runatal.data.preferences.UserPreferences(),
                widgetWidth = 300,
                widgetHeight = 151
            )
        ).isNull()
        assertThat(
            PersistentWidgetStateCache.get(
                context = context,
                widgetKey = "widget-1",
                currentDate = java.time.LocalDate.of(2026, 3, 11),
                preferences = com.po4yka.runatal.data.preferences.UserPreferences(),
                widgetWidth = 300,
                widgetHeight = 151,
                palette = WidgetPalette.default(),
                sizeClass = WidgetSizeClass.MEDIUM
            )
        ).isNull()
        assertThat(WidgetInteractionState.consumeRandomQuoteRequest("widget-1")).isTrue()
    }

    private class RecordingRefreshRunner : WidgetRefreshRunner {
        val refreshedIds = mutableListOf<GlanceId>()

        override suspend fun refreshAll(context: Context) = Unit

        override suspend fun refresh(context: Context, glanceId: GlanceId) {
            refreshedIds += glanceId
        }
    }
}
