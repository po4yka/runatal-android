package com.po4yka.runicquotes.ui.widget

import android.graphics.Bitmap
import com.google.common.truth.Truth.assertThat
import com.po4yka.runicquotes.data.preferences.UserPreferences
import com.po4yka.runicquotes.domain.model.RunicScript
import com.po4yka.runicquotes.util.BitmapCache
import java.time.LocalDate
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class PersistentWidgetStateCacheTest {

    private val context = RuntimeEnvironment.getApplication()
    private val preferences = UserPreferences(
        selectedScript = RunicScript.CIRTH,
        selectedFont = "babelstone",
        widgetDisplayMode = "daily_random_tap",
        widgetUpdateMode = "every_6_hours",
        themeMode = "dark",
        themePack = "stone",
        highContrastEnabled = true,
        dynamicColorEnabled = false
    )

    @After
    fun tearDown() {
        PersistentWidgetStateCache.clear(context)
        BitmapCache.clear()
    }

    @Test
    fun `put and get restore cached widget state from disk`() {
        val bitmap = Bitmap.createBitmap(8, 4, Bitmap.Config.ARGB_8888)
        val originalState = WidgetState(
            runicText = "\u16A0\u16A2",
            runicBitmap = bitmap,
            latinText = "Fehu Uruz",
            author = "Skald",
            scriptLabel = "Cirth",
            modeLabel = "Daily random",
            updateModeLabel = "Every 6 hours",
            sizeClass = WidgetSizeClass.COMPACT
        )

        PersistentWidgetStateCache.put(
            context = context,
            widgetKey = "widget-1",
            date = LocalDate.of(2026, 3, 11),
            preferences = preferences,
            widgetWidth = 300,
            widgetHeight = 151,
            state = originalState,
            bitmapCacheKey = "widget-bitmap"
        )
        BitmapCache.clear()

        val restoredState = PersistentWidgetStateCache.get(
            context = context,
            widgetKey = "widget-1",
            currentDate = LocalDate.of(2026, 3, 11),
            preferences = preferences,
            widgetWidth = 300,
            widgetHeight = 151,
            palette = WidgetPalette.default(),
            sizeClass = WidgetSizeClass.COMPACT
        )

        assertThat(restoredState).isNotNull()
        assertThat(restoredState?.runicText).isEqualTo(originalState.runicText)
        assertThat(restoredState?.latinText).isEqualTo(originalState.latinText)
        assertThat(restoredState?.author).isEqualTo(originalState.author)
        assertThat(restoredState?.runicBitmap).isNotNull()
        assertThat(restoredState?.runicBitmap?.width).isEqualTo(8)
    }

    @Test
    fun `get returns null when preferences no longer match`() {
        PersistentWidgetStateCache.put(
            context = context,
            widgetKey = "widget-1",
            date = LocalDate.of(2026, 3, 11),
            preferences = preferences,
            widgetWidth = 300,
            widgetHeight = 151,
            state = WidgetState(latinText = "Cached"),
            bitmapCacheKey = null
        )

        val restoredState = PersistentWidgetStateCache.get(
            context = context,
            widgetKey = "widget-1",
            currentDate = LocalDate.of(2026, 3, 11),
            preferences = preferences.copy(selectedScript = RunicScript.ELDER_FUTHARK),
            widgetWidth = 300,
            widgetHeight = 151,
            palette = WidgetPalette.default(),
            sizeClass = WidgetSizeClass.MEDIUM
        )

        assertThat(restoredState).isNull()
    }
}
