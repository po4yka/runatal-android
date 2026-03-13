package com.po4yka.runatal.ui.widget

import com.google.common.truth.Truth.assertThat
import com.po4yka.runatal.data.preferences.WidgetUpdateMode
import org.junit.Test

class WidgetUpdateModeTest {

    @Test
    fun `fromPersistedValue returns matching cadence`() {
        assertThat(WidgetUpdateMode.fromPersistedValue("manual"))
            .isEqualTo(WidgetUpdateMode.MANUAL)
        assertThat(WidgetUpdateMode.fromPersistedValue("every_6_hours"))
            .isEqualTo(WidgetUpdateMode.EVERY_6_HOURS)
        assertThat(WidgetUpdateMode.fromPersistedValue("every_12_hours"))
            .isEqualTo(WidgetUpdateMode.EVERY_12_HOURS)
    }

    @Test
    fun `fromPersistedValue falls back to daily`() {
        assertThat(WidgetUpdateMode.fromPersistedValue("unexpected"))
            .isEqualTo(WidgetUpdateMode.DAILY)
    }

    @Test
    fun `interval hours match expected cadences`() {
        assertThat(WidgetUpdateMode.MANUAL.intervalHours).isNull()
        assertThat(WidgetUpdateMode.EVERY_6_HOURS.intervalHours).isEqualTo(6)
        assertThat(WidgetUpdateMode.EVERY_12_HOURS.intervalHours).isEqualTo(12)
        assertThat(WidgetUpdateMode.DAILY.intervalHours).isEqualTo(24)
    }
}
