package com.po4yka.runicquotes.ui.theme

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
    heroCard = RoundedCornerShape(28.dp),
    contentCard = RoundedCornerShape(20.dp),
    collectionCard = RoundedCornerShape(16.dp),
    panel = RoundedCornerShape(24.dp),
    bottomSheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    segmentedControl = RoundedCornerShape(16.dp),
    segment = RoundedCornerShape(12.dp),
    dialog = RoundedCornerShape(28.dp),
    skeleton = RoundedCornerShape(12.dp),
    pill = RoundedCornerShape(9.dp)
)

private val DefaultRunicElevationTokens = RunicElevationTokens(
    flat = 0.dp,
    card = 3.dp,
    raisedCard = 8.dp,
    pressedCard = 10.dp,
    overlay = 16.dp
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

/** Returns default [RunicElevationTokens]. */
fun runicElevationTokens(): RunicElevationTokens = DefaultRunicElevationTokens

/** Returns default [RunicMotionTokens]. */
fun runicMotionTokens(): RunicMotionTokens = DefaultRunicMotionTokens

val LocalRunicShapeTokens = staticCompositionLocalOf { DefaultRunicShapeTokens }
val LocalRunicElevationTokens = staticCompositionLocalOf { DefaultRunicElevationTokens }
val LocalRunicMotionTokens = staticCompositionLocalOf { DefaultRunicMotionTokens }

/** Provides access to runic expressive design tokens via composition locals. */
object RunicExpressiveTheme {
    val shapes: RunicShapeTokens
        @Composable get() = LocalRunicShapeTokens.current

    val elevations: RunicElevationTokens
        @Composable get() = LocalRunicElevationTokens.current

    val motion: RunicMotionTokens
        @Composable get() = LocalRunicMotionTokens.current
}
