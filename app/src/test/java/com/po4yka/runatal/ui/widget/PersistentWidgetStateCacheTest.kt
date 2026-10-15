package com.po4yka.runatal.ui.widget

import android.graphics.Bitmap
import com.google.common.truth.Truth.assertThat
import com.po4yka.runatal.data.preferences.UserPreferences
import com.po4yka.runatal.domain.model.RunicScript
import com.po4yka.runatal.util.BitmapCache
import java.time.LocalDate
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import android.app.Application
import org.robolectric.annotation.Config
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
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
            renderEnvironment = "test",
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
            renderEnvironment = "test",
            expectedContent = WidgetQuoteContent(0, "Fehu Uruz", "Skald", "\u16A0\u16A2", "Cirth"),
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
    fun `disk cache invalidates when rendering environment changes`() {
        PersistentWidgetStateCache.put(
            context = context,
            widgetKey = "widget-1",
            date = LocalDate.of(2026, 3, 11),
            preferences = preferences,
            widgetWidth = 300,
            widgetHeight = 151,
            renderEnvironment = "dark-1",
            state = WidgetState(latinText = "Cached"),
            bitmapCacheKey = null
        )

        val changed = PersistentWidgetStateCache.get(
            context = context,
            widgetKey = "widget-1",
            currentDate = LocalDate.of(2026, 3, 11),
            preferences = preferences,
            widgetWidth = 300,
            widgetHeight = 151,
            renderEnvironment = "light-2",
            expectedContent = WidgetQuoteContent(0, "Cached", "", "", ""),
            palette = WidgetPalette.default(),
            sizeClass = WidgetSizeClass.COMPACT
        )

        assertThat(changed).isNull()
    }

    @Test
    fun `disk snapshot rejects source edits and deletions after process death`() {
        PersistentWidgetStateCache.put(
            context, "widget-1", LocalDate.of(2026, 3, 11), preferences, 300, 151, "test",
            WidgetState(quoteId = 7, latinText = "Original", author = "Author", runicText = "ᚠ"), null
        )
        val original = WidgetQuoteContent(7, "Original", "Author", "ᚠ", "")
        listOf(original.copy(latinText = "Edited"), original.copy(quoteId = 0)).forEach { current ->
            val restored = PersistentWidgetStateCache.get(
                context, "widget-1", LocalDate.of(2026, 3, 11), preferences, 300, 151, "test",
                current, WidgetPalette.default(), WidgetSizeClass.COMPACT
            )
            assertThat(restored).isNull()
        }
        assertThat(PersistentWidgetStateCache.quoteId(context, "widget-1", LocalDate.of(2026, 3, 11)))
            .isEqualTo(7)
    }

    @Test
    fun `disk snapshot rejects changed rendering labels even when source and glyphs are identical`() {
        val today = LocalDate.of(2026, 3, 11)
        val direct = WidgetState(quoteId = 7, latinText = "Original", author = "Author", runicText = "ᚠ",
            scriptLabel = "Elder Futhark · Transliteration")
        PersistentWidgetStateCache.put(context, "widget-1", today, preferences, 300, 151, "test", direct, null)
        val original = WidgetQuoteContent(7, "Original", "Author", "ᚠ", direct.scriptLabel)
        assertThat(PersistentWidgetStateCache.get(context, "widget-1", today, preferences, 300, 151, "test",
            original, WidgetPalette.default(), WidgetSizeClass.COMPACT)?.scriptLabel).isEqualTo(direct.scriptLabel)
        listOf(
            "Elder Futhark · Proto-Norse · Strict · Attested",
            "Elder Futhark · Proto-Norse · Readable · Reconstructed"
        ).forEach { changedLabel ->
                val current = original.copy(scriptLabel = changedLabel)
                assertThat(current.runicText).isEqualTo(direct.runicText)
                assertThat(PersistentWidgetStateCache.get(context, "widget-1", today, preferences, 300, 151,
                    "test", current, WidgetPalette.default(), WidgetSizeClass.COMPACT)).isNull()
            }
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
            renderEnvironment = "test",
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
            renderEnvironment = "test",
            expectedContent = WidgetQuoteContent(0, "Cached", "", "", ""),
            palette = WidgetPalette.default(),
            sizeClass = WidgetSizeClass.MEDIUM
        )

        assertThat(restoredState).isNull()
    }
}
