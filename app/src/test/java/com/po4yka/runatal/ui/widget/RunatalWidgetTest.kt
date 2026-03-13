package com.po4yka.runatal.ui.widget

import android.content.res.Resources
import android.util.DisplayMetrics
import com.google.common.truth.Truth.assertThat
import com.po4yka.runatal.data.preferences.WidgetDisplayMode
import io.mockk.every
import io.mockk.mockk
import org.junit.Test

class RunatalWidgetTest {

    @Test
    fun `resolveSizeClass uses compact and medium thresholds`() {
        assertThat(RunatalWidgetMetrics.resolveSizeClass(99)).isEqualTo(WidgetSizeClass.COMPACT)
        assertThat(RunatalWidgetMetrics.resolveSizeClass(100)).isEqualTo(WidgetSizeClass.MEDIUM)
        assertThat(RunatalWidgetMetrics.resolveSizeClass(180)).isEqualTo(WidgetSizeClass.EXPANDED)
    }

    @Test
    fun `runicTextSize scales by widget class`() {
        assertThat(RunatalWidgetMetrics.runicTextSize(WidgetSizeClass.COMPACT)).isEqualTo(12f)
        assertThat(RunatalWidgetMetrics.runicTextSize(WidgetSizeClass.MEDIUM)).isEqualTo(14f)
        assertThat(RunatalWidgetMetrics.runicTextSize(WidgetSizeClass.EXPANDED)).isEqualTo(15f)
    }

    @Test
    fun `maxRunicWidthPx applies density and minimum content width`() {
        val metrics = DisplayMetrics().apply { density = 2f }
        val resources = mockk<Resources>()
        every { resources.displayMetrics } returns metrics

        val compactWidth = RunatalWidgetMetrics.maxRunicWidthPx(resources, 300, WidgetSizeClass.COMPACT)
        val minimumWidth = RunatalWidgetMetrics.maxRunicWidthPx(resources, 100, WidgetSizeClass.EXPANDED)

        assertThat(compactWidth).isEqualTo(404)
        assertThat(minimumWidth).isEqualTo(240)
    }

    @Test
    fun `widget accessibility description reflects interaction mode and quote summary`() {
        val description = widgetAccessibilityDescription(
            WidgetState(
                latinText = "Seek wisdom",
                author = "Havamal",
                scriptLabel = "Elder Futhark",
                displayMode = WidgetDisplayMode.DAILY_RANDOM_TAP
            )
        )

        assertThat(description)
            .isEqualTo(
                "Runatal widget. Double tap to refresh quote. Seek wisdom. By Havamal. " +
                    "Script: Elder Futhark"
            )
    }

    @Test
    fun `widget accessibility description covers loading and error states`() {
        assertThat(widgetAccessibilityDescription(WidgetState(isLoading = true)))
            .isEqualTo("Runatal widget loading quote")
        assertThat(widgetAccessibilityDescription(WidgetState(error = "Unavailable")))
            .isEqualTo("Runatal widget unavailable. Tap to open app.")
    }
}
