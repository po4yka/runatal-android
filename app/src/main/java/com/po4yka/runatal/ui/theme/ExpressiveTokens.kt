package com.po4yka.runatal.ui.theme

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Shape tokens for runic UI components. */
@Immutable
data class RunicShapeTokens(
    val heroCard: CornerBasedShape,
    val contentCard: CornerBasedShape,
    val collectionCard: CornerBasedShape,
    val panel: CornerBasedShape,
    val bottomSheet: CornerBasedShape,
    val segmentedControl: CornerBasedShape,
    val segment: CornerBasedShape,
    val dialog: CornerBasedShape,
    val skeleton: CornerBasedShape,
    val pill: CornerBasedShape
)

/** Elevation tokens for runic UI components. */
@Immutable
data class RunicElevationTokens(
    val flat: Dp,
    val card: Dp,
    val raisedCard: Dp,
    val pressedCard: Dp,
    val overlay: Dp
)

/** Spacing scale for layout gaps, padding, and compact surface insets. */
@Immutable
data class RunicSpacingTokens(
    val micro: Dp,
    val tight: Dp,
    val compact: Dp,
    val small: Dp,
    val medium: Dp,
    val standard: Dp,
    val comfortable: Dp,
    val roomy: Dp,
    val spacious: Dp
)

/** Stroke widths for borders, dividers, and emphasized outlines. */
@Immutable
data class RunicStrokeTokens(
    val subtle: Dp,
    val emphasized: Dp
)

/** Component size roles for shared controls and selection affordances. */
@Immutable
data class RunicControlSizeTokens(
    val minimumTouchTarget: Dp,
    val segmentedControlMinHeight: Dp,
    val settingItemMinHeight: Dp,
    val dialogActionHeight: Dp,
    val leadingBadgeLarge: Dp,
    val leadingBadgeMedium: Dp,
    val aboutBadge: Dp,
    val selectionTrack: Dp,
    val selectionThumb: Dp,
    val selectionDot: Dp,
    val switchThumbContent: Dp
)

/** Icon scale roles for compact inline icons and larger badge treatments. */
@Immutable
data class RunicIconSizeTokens(
    val compact: Dp,
    val inline: Dp,
    val standard: Dp,
    val appIcon: Dp,
    val selectedAppIcon: Dp
)

/** Layout defaults for shared top app bars and their centered title treatment. */
@Immutable
data class RunicTopBarTokens(
    val horizontalPadding: Dp,
    val verticalPadding: Dp,
    val titleSideClearance: Dp
)

/** Motion and animation tokens for runic UI components. */
@Immutable
data class RunicMotionTokens(
    val shortDurationMillis: Int,
    val mediumDurationMillis: Int,
    val longDurationMillis: Int,
    val revealStepMillis: Int,
    val maxRevealDelayMillis: Int,
    val emphasizedEasing: Easing,
    val standardEasing: Easing
) {
    /** Returns [base] duration in millis, or zero when reduced motion is enabled. */
    fun duration(reducedMotion: Boolean, base: Int): Int = if (reducedMotion) 0 else base

    /** Returns [base] delay in millis, or zero when reduced motion is enabled. */
    fun delay(reducedMotion: Boolean, base: Int): Int = if (reducedMotion) 0 else base
}

private val DefaultRunicShapeTokens = RunicShapeTokens(
    heroCard = RoundedCornerShape(24.dp),
    contentCard = RoundedCornerShape(16.dp),
    collectionCard = RoundedCornerShape(12.dp),
    panel = RoundedCornerShape(20.dp),
    bottomSheet = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    segmentedControl = RoundedCornerShape(16.dp),
    segment = RoundedCornerShape(12.dp),
    dialog = RoundedCornerShape(20.dp),
    skeleton = RoundedCornerShape(12.dp),
    pill = RoundedCornerShape(999.dp)
)

private val DefaultRunicElevationTokens = RunicElevationTokens(
    flat = 0.dp,
    card = 1.dp,
    raisedCard = 4.dp,
    pressedCard = 6.dp,
    overlay = 12.dp
)

private val DefaultRunicSpacingTokens = RunicSpacingTokens(
    micro = 2.dp,
    tight = 4.dp,
    compact = 6.dp,
    small = 8.dp,
    medium = 10.dp,
    standard = 12.dp,
    comfortable = 16.dp,
    roomy = 20.dp,
    spacious = 24.dp
)

private val DefaultRunicStrokeTokens = RunicStrokeTokens(
    subtle = 1.dp,
    emphasized = 2.dp
)

private val DefaultRunicControlSizeTokens = RunicControlSizeTokens(
    minimumTouchTarget = 48.dp,
    segmentedControlMinHeight = 48.dp,
    settingItemMinHeight = 57.dp,
    dialogActionHeight = 48.dp,
    leadingBadgeLarge = 40.dp,
    leadingBadgeMedium = 36.dp,
    aboutBadge = 44.dp,
    selectionTrack = 20.dp,
    selectionThumb = 16.dp,
    selectionDot = 10.dp,
    switchThumbContent = 18.dp
)

private val DefaultRunicIconSizeTokens = RunicIconSizeTokens(
    compact = 14.dp,
    inline = 16.dp,
    standard = 18.dp,
    appIcon = 21.dp,
    selectedAppIcon = 23.dp
)

private val DefaultRunicTopBarTokens = RunicTopBarTokens(
    horizontalPadding = 12.dp,
    verticalPadding = 6.dp,
    titleSideClearance = 56.dp
)

private val DefaultRunicMotionTokens = RunicMotionTokens(
    shortDurationMillis = 220,
    mediumDurationMillis = 420,
    longDurationMillis = 620,
    revealStepMillis = 42,
    maxRevealDelayMillis = 1600,
    emphasizedEasing = FastOutSlowInEasing,
    standardEasing = LinearOutSlowInEasing
)

/** Returns default [RunicShapeTokens]. */
fun runicShapeTokens(): RunicShapeTokens = DefaultRunicShapeTokens

/**
 * Maps the runic shape roles onto Material 3 shape slots so both APIs resolve to the same geometry.
 */
fun runicMaterialShapes(shapeTokens: RunicShapeTokens = DefaultRunicShapeTokens): Shapes = Shapes(
    extraSmall = shapeTokens.segment,
    small = shapeTokens.collectionCard,
    medium = shapeTokens.contentCard,
    large = shapeTokens.panel,
    extraLarge = shapeTokens.heroCard
)

/** Returns default [RunicElevationTokens]. */
fun runicElevationTokens(): RunicElevationTokens = DefaultRunicElevationTokens

/** Returns default [RunicSpacingTokens]. */
fun runicSpacingTokens(): RunicSpacingTokens = DefaultRunicSpacingTokens

/** Returns default [RunicStrokeTokens]. */
fun runicStrokeTokens(): RunicStrokeTokens = DefaultRunicStrokeTokens

/** Returns default [RunicControlSizeTokens]. */
fun runicControlSizeTokens(): RunicControlSizeTokens = DefaultRunicControlSizeTokens

/** Returns default [RunicIconSizeTokens]. */
fun runicIconSizeTokens(): RunicIconSizeTokens = DefaultRunicIconSizeTokens

/** Returns default [RunicTopBarTokens]. */
fun runicTopBarTokens(): RunicTopBarTokens = DefaultRunicTopBarTokens

/** Returns default [RunicMotionTokens]. */
fun runicMotionTokens(): RunicMotionTokens = DefaultRunicMotionTokens

val LocalRunicShapeTokens = staticCompositionLocalOf { DefaultRunicShapeTokens }
val LocalRunicElevationTokens = staticCompositionLocalOf { DefaultRunicElevationTokens }
val LocalRunicSpacingTokens = staticCompositionLocalOf { DefaultRunicSpacingTokens }
val LocalRunicStrokeTokens = staticCompositionLocalOf { DefaultRunicStrokeTokens }
val LocalRunicControlSizeTokens = staticCompositionLocalOf { DefaultRunicControlSizeTokens }
val LocalRunicIconSizeTokens = staticCompositionLocalOf { DefaultRunicIconSizeTokens }
val LocalRunicTopBarTokens = staticCompositionLocalOf { DefaultRunicTopBarTokens }
val LocalRunicMotionTokens = staticCompositionLocalOf { DefaultRunicMotionTokens }

/** Provides access to runic expressive design tokens via composition locals. */
object RunicExpressiveTheme {
    val shapes: RunicShapeTokens
        @Composable get() = LocalRunicShapeTokens.current

    val elevations: RunicElevationTokens
        @Composable get() = LocalRunicElevationTokens.current

    val spacing: RunicSpacingTokens
        @Composable get() = LocalRunicSpacingTokens.current

    val strokes: RunicStrokeTokens
        @Composable get() = LocalRunicStrokeTokens.current

    val controls: RunicControlSizeTokens
        @Composable get() = LocalRunicControlSizeTokens.current

    val icons: RunicIconSizeTokens
        @Composable get() = LocalRunicIconSizeTokens.current

    val topBars: RunicTopBarTokens
        @Composable get() = LocalRunicTopBarTokens.current

    val motion: RunicMotionTokens
        @Composable get() = LocalRunicMotionTokens.current
}
