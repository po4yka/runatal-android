package com.po4yka.runatal.ui.widget

import com.google.common.truth.Truth.assertThat
import com.po4yka.runatal.data.preferences.UserPreferences
import com.po4yka.runatal.domain.model.RunicScript
import java.time.LocalDate
import org.junit.After
import org.junit.Test

class WidgetStateCacheTest {

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

    private val state = WidgetState(
        runicText = "\u16A0\u16A2",
        latinText = "Fehu Uruz",
        sizeClass = WidgetSizeClass.COMPACT
    )

    private val expectedContent = WidgetQuoteContent(
        state.quoteId, state.latinText, state.author, state.runicText, state.scriptLabel
    )

    @After
    fun tearDown() {
        WidgetStateCache.clear()
    }

    @Test
    fun `put and get returns cached state when inputs match`() {
        WidgetStateCache.put("widget", LocalDate.of(2026, 3, 11), preferences, 300, 151, "test", state)

        val cached = WidgetStateCache.get(
            widgetKey = "widget",
            currentDate = LocalDate.of(2026, 3, 11),
            preferences = preferences,
            widgetWidth = 300,
            widgetHeight = 151,
            renderEnvironment = "test",
            expectedContent = expectedContent
        )

        assertThat(cached).isEqualTo(state)
    }

    @Test
    fun `cache invalidates when date or rendering preferences change`() {
        WidgetStateCache.put("widget", LocalDate.of(2026, 3, 11), preferences, 300, 151, "test", state)

        val differentDate = WidgetStateCache.get(
            widgetKey = "widget",
            currentDate = LocalDate.of(2026, 3, 12),
            preferences = preferences,
            widgetWidth = 300,
            widgetHeight = 151,
            renderEnvironment = "test",
            expectedContent = expectedContent
        )
        val differentScript = WidgetStateCache.get(
            widgetKey = "widget",
            currentDate = LocalDate.of(2026, 3, 11),
            preferences = preferences.copy(selectedScript = RunicScript.ELDER_FUTHARK),
            widgetWidth = 300,
            widgetHeight = 151,
            renderEnvironment = "test",
            expectedContent = expectedContent
        )

        assertThat(differentDate).isNull()
        assertThat(differentScript).isNull()
    }

    @Test
    fun `cache invalidates when widget size changes`() {
        WidgetStateCache.put("widget", LocalDate.of(2026, 3, 11), preferences, 300, 151, "test", state)

        val resized = WidgetStateCache.get(
            widgetKey = "widget",
            currentDate = LocalDate.of(2026, 3, 11),
            preferences = preferences,
            widgetWidth = 400,
            widgetHeight = 151,
            renderEnvironment = "test",
            expectedContent = expectedContent
        )

        assertThat(resized).isNull()
    }

    @Test
    fun `cache invalidates when resolved palette or device scaling changes`() {
        WidgetStateCache.put("widget", LocalDate.of(2026, 3, 11), preferences, 300, 151, "dark-1", state)

        val changed = WidgetStateCache.get(
            "widget", LocalDate.of(2026, 3, 11), preferences, 300, 151, "light-2", expectedContent
        )

        assertThat(changed).isNull()
    }

    @Test
    fun `current source rejects edited author runes and deleted identities`() {
        WidgetStateCache.put("widget", LocalDate.of(2026, 3, 11), preferences, 300, 151, "test", state)
        listOf(
            expectedContent.copy(quoteId = 999), expectedContent.copy(latinText = "Edited"),
            expectedContent.copy(author = "Different"), expectedContent.copy(runicText = "ᚾ")
        ).forEach { changed ->
            assertThat(WidgetStateCache.get(
                "widget", LocalDate.of(2026, 3, 11), preferences, 300, 151, "test", changed
            )).isNull()
        }
    }

    @Test
    fun `same glyphs cannot reuse a cache with a different rendering mode or resolution label`() {
        val today = LocalDate.of(2026, 3, 11)
        val direct = state.copy(scriptLabel = "Elder Futhark · Transliteration")
        val original = expectedContent.copy(scriptLabel = direct.scriptLabel)
        WidgetStateCache.put("widget", today, preferences, 300, 151, "test", direct)
        assertThat(WidgetStateCache.get("widget", today, preferences, 300, 151, "test", original)).isEqualTo(direct)
        listOf(
            "Elder Futhark · Proto-Norse · Strict · Attested",
            "Elder Futhark · Proto-Norse · Readable · Reconstructed"
        ).forEach { changedLabel ->
                val changed = original.copy(scriptLabel = changedLabel)
                assertThat(changed.runicText).isEqualTo(direct.runicText)
                assertThat(WidgetStateCache.get("widget", today, preferences, 300, 151, "test", changed)).isNull()
            }
    }

    @Test
    fun `clear removes cache entries`() {
        WidgetStateCache.put("widget", LocalDate.of(2026, 3, 11), preferences, 300, 151, "test", state)
        WidgetStateCache.clear("widget")

        assertThat(
            WidgetStateCache.get(
                widgetKey = "widget",
                currentDate = LocalDate.of(2026, 3, 11),
                preferences = preferences,
                widgetWidth = 300,
                widgetHeight = 151,
                renderEnvironment = "test",
            expectedContent = expectedContent
            )
        ).isNull()
    }
}
