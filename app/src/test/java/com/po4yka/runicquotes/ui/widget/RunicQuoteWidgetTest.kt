package com.po4yka.runicquotes.ui.widget

import android.content.res.Resources
import android.util.DisplayMetrics
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import org.junit.Test

class RunicQuoteWidgetTest {

    @Test
    fun `resolveSizeClass uses compact and medium thresholds`() {
        assertThat(RunicQuoteWidgetMetrics.resolveSizeClass(99)).isEqualTo(WidgetSizeClass.COMPACT)
        assertThat(RunicQuoteWidgetMetrics.resolveSizeClass(100)).isEqualTo(WidgetSizeClass.MEDIUM)
        assertThat(RunicQuoteWidgetMetrics.resolveSizeClass(180)).isEqualTo(WidgetSizeClass.EXPANDED)
    }

    @Test
    fun `runicTextSize scales by widget class`() {
        assertThat(RunicQuoteWidgetMetrics.runicTextSize(WidgetSizeClass.COMPACT)).isEqualTo(12f)
        assertThat(RunicQuoteWidgetMetrics.runicTextSize(WidgetSizeClass.MEDIUM)).isEqualTo(14f)
        assertThat(RunicQuoteWidgetMetrics.runicTextSize(WidgetSizeClass.EXPANDED)).isEqualTo(15f)
    }

    @Test
    fun `maxRunicWidthPx applies density and minimum content width`() {
        val metrics = DisplayMetrics().apply { density = 2f }
        val resources = mockk<Resources>()
        every { resources.displayMetrics } returns metrics

        val compactWidth = RunicQuoteWidgetMetrics.maxRunicWidthPx(resources, 300, WidgetSizeClass.COMPACT)
        val minimumWidth = RunicQuoteWidgetMetrics.maxRunicWidthPx(resources, 100, WidgetSizeClass.EXPANDED)

        assertThat(compactWidth).isEqualTo(404)
        assertThat(minimumWidth).isEqualTo(240)
    }
}
