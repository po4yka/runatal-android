package com.po4yka.runicquotes.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.po4yka.runicquotes.R

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

/** Expressive typography roles for runic and Latin quote text. */
@Immutable
data class RunicExpressiveTypography(
    val runicHero: TextStyle,
    val runicCard: TextStyle,
    val runicCollection: TextStyle,
    val latinQuote: TextStyle,
    val quoteMeta: TextStyle
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
        ),
        latinQuote = typography.bodyLarge.copy(
            fontFamily = RobotoFlex,
            letterSpacing = 0.sp
        ),
        quoteMeta = typography.labelLarge.copy(
            fontFamily = RobotoFlex,
            letterSpacing = 0.1.sp
        )
    )
}

/** Legacy pack hook kept while the rest of the app still passes a theme-pack preference. */
fun expressiveTypographyForThemePack(
    @Suppress("UNUSED_PARAMETER") themePack: String,
    typography: Typography
): RunicExpressiveTypography = baseExpressiveTypography(typography)

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
}
