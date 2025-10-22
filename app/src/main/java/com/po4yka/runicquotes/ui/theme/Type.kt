package com.po4yka.runicquotes.ui.theme

import androidx.compose.material3.Typography
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
 * Material 3 Expressive Typography with Roboto Flex
 */
val RunicTypography = Typography(
    // Display styles - for large, impactful text
    displayLarge = TextStyle(
        fontFamily = RobotoFlex,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = RobotoFlex,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = RobotoFlex,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp
    ),

    // Headline styles - for section headers
    headlineLarge = TextStyle(
        fontFamily = RobotoFlex,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = RobotoFlex,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = RobotoFlex,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp
    ),

    // Title styles - for card titles, dialog headers
    titleLarge = TextStyle(
        fontFamily = RobotoFlex,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = RobotoFlex,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = RobotoFlex,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.1.sp
    ),

    // Body styles - for main content
    bodyLarge = TextStyle(
        fontFamily = RobotoFlex,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = RobotoFlex,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = RobotoFlex,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.4.sp
    ),

    // Label styles - for buttons, small UI elements
    labelLarge = TextStyle(
        fontFamily = RobotoFlex,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = RobotoFlex,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = RobotoFlex,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.5.sp
    )
)
