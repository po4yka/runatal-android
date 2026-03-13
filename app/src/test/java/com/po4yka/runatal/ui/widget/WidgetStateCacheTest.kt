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

    @After
    fun tearDown() {
        WidgetStateCache.clear()
    }

    @Test
    fun `put and get returns cached state when inputs match`() {
        WidgetStateCache.put("widget", LocalDate.of(2026, 3, 11), preferences, 300, 151, state)

        val cached = WidgetStateCache.get(
            widgetKey = "widget",
            currentDate = LocalDate.of(2026, 3, 11),
            preferences = preferences,
            widgetWidth = 300,
            widgetHeight = 151
        )

        assertThat(cached).isEqualTo(state)
    }

    @Test
    fun `cache invalidates when date or rendering preferences change`() {
        WidgetStateCache.put("widget", LocalDate.of(2026, 3, 11), preferences, 300, 151, state)

        val differentDate = WidgetStateCache.get(
            widgetKey = "widget",
            currentDate = LocalDate.of(2026, 3, 12),
            preferences = preferences,
            widgetWidth = 300,
            widgetHeight = 151
        )
        val differentScript = WidgetStateCache.get(
            widgetKey = "widget",
            currentDate = LocalDate.of(2026, 3, 11),
            preferences = preferences.copy(selectedScript = RunicScript.ELDER_FUTHARK),
            widgetWidth = 300,
            widgetHeight = 151
        )

        assertThat(differentDate).isNull()
        assertThat(differentScript).isNull()
    }

    @Test
    fun `cache invalidates when widget size changes`() {
        WidgetStateCache.put("widget", LocalDate.of(2026, 3, 11), preferences, 300, 151, state)

        val resized = WidgetStateCache.get(
            widgetKey = "widget",
            currentDate = LocalDate.of(2026, 3, 11),
            preferences = preferences,
            widgetWidth = 400,
            widgetHeight = 151
        )

        assertThat(resized).isNull()
    }

    @Test
    fun `clear removes cache entries`() {
        WidgetStateCache.put("widget", LocalDate.of(2026, 3, 11), preferences, 300, 151, state)
        WidgetStateCache.clear("widget")

        assertThat(
            WidgetStateCache.get(
                widgetKey = "widget",
                currentDate = LocalDate.of(2026, 3, 11),
                preferences = preferences,
                widgetWidth = 300,
                widgetHeight = 151
            )
        ).isNull()
    }
}
