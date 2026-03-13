package com.po4yka.runatal.ui.theme

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ThemeAccessibilityTest {

    @Test
    fun `reduced motion collapses animation duration and delay`() {
        val motion = runicMotionTokens()

        assertThat(motion.duration(reducedMotion = true, base = motion.mediumDurationMillis)).isEqualTo(0)
        assertThat(motion.delay(reducedMotion = true, base = motion.shortDurationMillis)).isEqualTo(0)
        assertThat(motion.duration(reducedMotion = false, base = motion.mediumDurationMillis))
            .isEqualTo(motion.mediumDurationMillis)
    }

    @Test
    fun `high contrast scheme uses black and white anchors`() {
        val lightScheme = highContrastRunicColorScheme(darkTheme = false)
        val darkScheme = highContrastRunicColorScheme(darkTheme = true)

        assertThat(lightScheme.background).isEqualTo(HighContrastWhite)
        assertThat(lightScheme.onBackground).isEqualTo(HighContrastBlack)
        assertThat(darkScheme.background).isEqualTo(HighContrastBlack)
        assertThat(darkScheme.onBackground).isEqualTo(HighContrastWhite)
    }
}
