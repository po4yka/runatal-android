package com.po4yka.runatal.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.po4yka.runatal.R
import com.po4yka.runatal.domain.model.RunicScript

/**
 * Font families for the app
 */
val RobotoFlex = FontFamily(
    Font(R.font.roboto_flex, FontWeight.Normal)
)

val NotoSansRunic = FontFamily(
    Font(R.font.noto_sans_runic, FontWeight.Normal)
)

val BabelStoneRunic = FontFamily(
    Font(R.font.babelstone_runic, FontWeight.Normal)
)

val BabelStoneRunicRuled = FontFamily(
    Font(R.font.babelstone_runic_ruled, FontWeight.Normal)
)

/**
 * Typography aligned to the Figma foundation sheet.
 */
val RunicTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = RobotoFlex,
        fontSize = 40.sp,
        lineHeight = 44.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.6).sp
    ),
    displayMedium = TextStyle(
        fontFamily = RobotoFlex,
        fontSize = 32.sp,
        lineHeight = 36.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.4).sp
    ),
    displaySmall = TextStyle(
        fontFamily = RobotoFlex,
        fontSize = 28.sp,
        lineHeight = 32.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.2).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = RobotoFlex,
        fontSize = 24.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.2).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = RobotoFlex,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.1).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = RobotoFlex,
        fontSize = 20.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = RobotoFlex,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = RobotoFlex,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = RobotoFlex,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = RobotoFlex,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = RobotoFlex,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp
    ),
    bodySmall = TextStyle(
        fontFamily = RobotoFlex,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp
    ),
    labelLarge = TextStyle(
        fontFamily = RobotoFlex,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = RobotoFlex,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.35.sp
    ),
    labelSmall = TextStyle(
        fontFamily = RobotoFlex,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.3.sp
    )
)

/**
 * Legacy pack hook kept while the rest of the app still passes a theme-pack preference.
 */
fun typographyForThemePack(@Suppress("UNUSED_PARAMETER") themePack: String): Typography = RunicTypography

/** Expressive typography roles for runic display and collection text. */
@Immutable
data class RunicExpressiveTypography(
    val runicHero: TextStyle,
    val runicCard: TextStyle,
    val runicCollection: TextStyle
)

/** Semantic roles for runic copy so screens consume shared typography tokens. */
enum class RunicTextRole {
    Default,
    QuoteHero,
    QuoteCard,
    EditorPreview,
    EditorConfirmation,
    OnboardingSample,
    OnboardingReveal,
    ShareCard,
    ShareVerse,
    ShareLandscape,
    BottomSheetPreview,
    TranslationPlaceholder,
    TranslationResult
}

/** Semantic roles for Latin and supporting copy so screens avoid ad hoc style overrides. */
enum class SupportingTextRole {
    DateMeta,
    HelperText,
    CompactMeta,
    QuoteTransliteration,
    QuoteMeta,
    SupportingBodyItalic,
    FormPlaceholder,
    FormPlaceholderEmphasis,
    ShareCardQuote,
    ShareVerseQuote,
    ShareLandscapeQuote,
    ShareAuthor,
    ShareMeta
}

/** Resolved runic text token bundle for a semantic [RunicTextRole]. */
@Immutable
data class RunicTextSpec(
    val style: TextStyle,
    val fontSize: TextUnit,
    val lineHeight: TextUnit,
    val letterSpacing: TextUnit
)

@Immutable
private data class RunicMetricSpec(
    val fontSize: TextUnit,
    val lineHeight: TextUnit,
    val letterSpacing: TextUnit
)

private fun baseExpressiveTypography(typography: Typography): RunicExpressiveTypography {
    return RunicExpressiveTypography(
        runicHero = typography.displayMedium.copy(
            fontFamily = RobotoFlex,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.1.sp
        ),
        runicCard = typography.headlineSmall.copy(
            fontFamily = RobotoFlex,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.1.sp
        ),
        runicCollection = typography.titleLarge.copy(
            fontFamily = RobotoFlex,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.1.sp
        )
    )
}

private fun defaultRunicMetrics(script: RunicScript): RunicMetricSpec {
    return when (script) {
        RunicScript.ELDER_FUTHARK -> RunicMetricSpec(
            fontSize = 33.sp,
            lineHeight = 44.sp,
            letterSpacing = 0.35.sp
        )

        RunicScript.YOUNGER_FUTHARK -> RunicMetricSpec(
            fontSize = 31.sp,
            lineHeight = 42.sp,
            letterSpacing = 0.15.sp
        )

        RunicScript.CIRTH -> RunicMetricSpec(
            fontSize = 35.sp,
            lineHeight = 46.sp,
            letterSpacing = 0.25.sp
        )
    }
}

private fun metricsByScript(
    script: RunicScript,
    elderFuthark: RunicMetricSpec,
    youngerFuthark: RunicMetricSpec,
    cirth: RunicMetricSpec
): RunicMetricSpec {
    return when (script) {
        RunicScript.ELDER_FUTHARK -> elderFuthark
        RunicScript.YOUNGER_FUTHARK -> youngerFuthark
        RunicScript.CIRTH -> cirth
    }
}

private fun fixedRunicMetrics(role: RunicTextRole): RunicMetricSpec? {
    return when (role) {
        RunicTextRole.EditorPreview -> RunicMetricSpec(17.sp, 28.sp, 0.425.sp)
        RunicTextRole.EditorConfirmation -> RunicMetricSpec(13.sp, 21.sp, 0.39.sp)
        RunicTextRole.OnboardingSample -> RunicMetricSpec(16.sp, 24.sp, 0.96.sp)
        RunicTextRole.OnboardingReveal -> RunicMetricSpec(17.sp, 30.sp, 0.68.sp)
        RunicTextRole.ShareCard -> RunicMetricSpec(18.sp, 26.sp, 0.6.sp)
        RunicTextRole.ShareVerse -> RunicMetricSpec(11.sp, 16.sp, 0.45.sp)
        RunicTextRole.ShareLandscape -> RunicMetricSpec(9.sp, 12.sp, 0.42.sp)
        else -> null
    }
}

private fun runicMetricsForRole(role: RunicTextRole, script: RunicScript): RunicMetricSpec {
    val defaultMetrics = defaultRunicMetrics(script)
    fixedRunicMetrics(role)?.let { return it }

    return when (role) {
        RunicTextRole.Default -> defaultMetrics
        RunicTextRole.QuoteHero -> metricsByScript(
            script = script,
            elderFuthark = RunicMetricSpec(19.sp, 34.sp, 0.72.sp),
            youngerFuthark = RunicMetricSpec(18.sp, 32.sp, 0.5.sp),
            cirth = RunicMetricSpec(20.sp, 34.sp, 0.56.sp)
        )

        RunicTextRole.QuoteCard -> metricsByScript(
            script = script,
            elderFuthark = RunicMetricSpec(15.sp, 25.sp, 0.42.sp),
            youngerFuthark = RunicMetricSpec(14.sp, 24.sp, 0.3.sp),
            cirth = RunicMetricSpec(16.sp, 25.sp, 0.34.sp)
        )

        RunicTextRole.BottomSheetPreview -> RunicMetricSpec(
            fontSize = 12.sp,
            lineHeight = 18.sp,
            letterSpacing = defaultMetrics.letterSpacing
        )

        RunicTextRole.TranslationPlaceholder -> RunicMetricSpec(
            fontSize = 28.sp,
            lineHeight = 34.sp,
            letterSpacing = defaultMetrics.letterSpacing
        )

        RunicTextRole.TranslationResult -> RunicMetricSpec(
            fontSize = 28.sp,
            lineHeight = 36.sp,
            letterSpacing = defaultMetrics.letterSpacing
        )

        RunicTextRole.EditorPreview,
        RunicTextRole.EditorConfirmation,
        RunicTextRole.OnboardingSample,
        RunicTextRole.OnboardingReveal,
        RunicTextRole.ShareCard,
        RunicTextRole.ShareVerse,
        RunicTextRole.ShareLandscape -> error("Fixed runic metrics should be handled before scripted roles")
    }
}

/** Resolves the typography and metric tokens for a runic text [role] in a given [script]. */
fun runicTextSpecForRole(
    role: RunicTextRole,
    script: RunicScript,
    typography: Typography,
    expressiveTypography: RunicExpressiveTypography = baseExpressiveTypography(typography)
): RunicTextSpec {
    val metrics = runicMetricsForRole(role, script)
    val style = when (role) {
        RunicTextRole.Default -> typography.bodyLarge
        RunicTextRole.QuoteHero -> typography.bodyMedium
        RunicTextRole.QuoteCard -> typography.bodySmall
        RunicTextRole.EditorPreview -> typography.bodyLarge
        RunicTextRole.EditorConfirmation -> typography.bodyMedium
        RunicTextRole.OnboardingSample -> typography.bodyMedium
        RunicTextRole.OnboardingReveal -> expressiveTypography.runicCollection
        RunicTextRole.ShareCard -> typography.bodyLarge
        RunicTextRole.ShareVerse -> typography.bodyMedium
        RunicTextRole.ShareLandscape -> typography.bodySmall
        RunicTextRole.BottomSheetPreview -> typography.labelLarge
        RunicTextRole.TranslationPlaceholder -> typography.headlineSmall
        RunicTextRole.TranslationResult -> typography.headlineSmall
    }
    return RunicTextSpec(
        style = style,
        fontSize = metrics.fontSize,
        lineHeight = metrics.lineHeight,
        letterSpacing = metrics.letterSpacing
    )
}

/** Legacy pack hook kept while the rest of the app still passes a theme-pack preference. */
fun expressiveTypographyForThemePack(
    @Suppress("UNUSED_PARAMETER") themePack: String,
    typography: Typography
): RunicExpressiveTypography = baseExpressiveTypography(typography)

/** Resolves the typography tokens for a semantic [SupportingTextRole]. */
fun supportingTextStyleForRole(
    role: SupportingTextRole,
    typography: Typography
): TextStyle {
    return when (role) {
        SupportingTextRole.DateMeta -> typography.bodySmall
        SupportingTextRole.HelperText -> typography.bodySmall
        SupportingTextRole.CompactMeta -> typography.labelSmall
        SupportingTextRole.QuoteTransliteration -> typography.bodySmall.copy(
            fontStyle = FontStyle.Italic,
            lineHeight = 20.sp
        )

        SupportingTextRole.QuoteMeta -> typography.labelMedium.copy(
            fontWeight = FontWeight.Medium,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp
        )

        SupportingTextRole.SupportingBodyItalic -> typography.bodySmall.copy(
            fontStyle = FontStyle.Italic
        )

        SupportingTextRole.FormPlaceholder -> typography.bodyMedium
        SupportingTextRole.FormPlaceholderEmphasis -> typography.bodyLarge.copy(
            fontStyle = FontStyle.Italic
        )

        SupportingTextRole.ShareCardQuote -> typography.bodyMedium.copy(
            fontStyle = FontStyle.Italic
        )

        SupportingTextRole.ShareVerseQuote -> typography.headlineSmall.copy(
            fontStyle = FontStyle.Italic
        )

        SupportingTextRole.ShareLandscapeQuote -> typography.titleLarge.copy(
            fontStyle = FontStyle.Italic
        )

        SupportingTextRole.ShareAuthor -> typography.labelLarge.copy(
            fontWeight = FontWeight.Medium
        )

        SupportingTextRole.ShareMeta -> typography.labelSmall
    }
}

val LocalRunicExpressiveType = staticCompositionLocalOf {
    expressiveTypographyForThemePack(
        themePack = "stone",
        typography = RunicTypography
    )
}

/** Provides access to the current [RunicExpressiveTypography] via composition local. */
object RunicTypeRoles {
    val current: RunicExpressiveTypography
        @Composable get() = LocalRunicExpressiveType.current

    /** Reads the resolved runic typography tokens for [role] from the active theme. */
    @Composable
    fun runic(role: RunicTextRole, script: RunicScript): RunicTextSpec {
        return runicTextSpecForRole(
            role = role,
            script = script,
            typography = MaterialTheme.typography,
            expressiveTypography = current
        )
    }

    /** Reads the resolved supporting typography tokens for [role] from the active theme. */
    @Composable
    fun supporting(role: SupportingTextRole): TextStyle {
        return supportingTextStyleForRole(
            role = role,
            typography = MaterialTheme.typography
        )
    }
}
