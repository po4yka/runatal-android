package com.po4yka.runicquotes.ui.theme

import com.google.common.truth.Truth.assertThat
import com.po4yka.runicquotes.domain.model.RunicScript
import org.junit.Test
import androidx.compose.ui.unit.sp

class RunicTypographyTokensTest {

    @Test
    fun `quote hero role returns scripted metrics`() {
        val spec = runicTextSpecForRole(
            role = RunicTextRole.QuoteHero,
            script = RunicScript.ELDER_FUTHARK,
            typography = RunicTypography
        )

        assertThat(spec.style).isEqualTo(RunicTypography.bodyMedium)
        assertThat(spec.fontSize).isEqualTo(19.sp)
        assertThat(spec.lineHeight).isEqualTo(34.sp)
        assertThat(spec.letterSpacing).isEqualTo(0.72.sp)
    }

    @Test
    fun `onboarding reveal role reuses expressive collection style`() {
        val expressive = expressiveTypographyForThemePack(
            themePack = "stone",
            typography = RunicTypography
        )
        val spec = runicTextSpecForRole(
            role = RunicTextRole.OnboardingReveal,
            script = RunicScript.CIRTH,
            typography = RunicTypography,
            expressiveTypography = expressive
        )

        assertThat(spec.style).isEqualTo(expressive.runicCollection)
        assertThat(spec.fontSize).isEqualTo(17.sp)
        assertThat(spec.lineHeight).isEqualTo(30.sp)
        assertThat(spec.letterSpacing).isEqualTo(0.68.sp)
    }

    @Test
    fun `translation result role keeps script default spacing while changing scale`() {
        val spec = runicTextSpecForRole(
            role = RunicTextRole.TranslationResult,
            script = RunicScript.YOUNGER_FUTHARK,
            typography = RunicTypography
        )

        assertThat(spec.style).isEqualTo(RunicTypography.headlineSmall)
        assertThat(spec.fontSize).isEqualTo(28.sp)
        assertThat(spec.lineHeight).isEqualTo(36.sp)
        assertThat(spec.letterSpacing).isEqualTo(0.15.sp)
    }
}
