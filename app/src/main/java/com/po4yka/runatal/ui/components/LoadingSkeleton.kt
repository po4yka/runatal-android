package com.po4yka.runatal.ui.components

import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.po4yka.runatal.ui.theme.LocalReduceMotion
import com.po4yka.runatal.ui.theme.RunicExpressiveTheme

/**
 * Animated shimmer brush that sweeps left-to-right across skeleton placeholders.
 *
 * When reduced motion is enabled, returns a flat color with no animation.
 */
@Composable
fun rememberShimmerBrush(
    baseColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    highlightColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest
): Brush {
    val reducedMotion = LocalReduceMotion.current
    if (reducedMotion) {
        return Brush.linearGradient(listOf(baseColor, baseColor))
    }

    val transition: InfiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = RunicExpressiveTheme.motion.longDurationMillis * 2,
                easing = LinearEasing
            )
        ),
        label = "shimmerProgress"
    )

    return SizeAwareShimmerBrush(
        baseColor = baseColor,
        highlightColor = highlightColor,
        progress = progress
    )
}

private class SizeAwareShimmerBrush(
    private val baseColor: Color,
    private val highlightColor: Color,
    private val progress: Float
) : ShaderBrush() {
    override fun createShader(size: Size): Shader {
        val width = size.width.coerceAtLeast(1f)
        val height = size.height.coerceAtLeast(1f)
        val highlightWidth = (width * 0.45f).coerceAtLeast(height * 1.2f)
        val travel = width + highlightWidth
        val startX = -highlightWidth + (travel * progress)

        return LinearGradientShader(
            from = Offset(startX, 0f),
            to = Offset(startX + highlightWidth, height),
            colors = listOf(baseColor, highlightColor, baseColor)
        )
    }
}

/**
 * A rectangular skeleton placeholder with rounded corners and shimmer animation.
 *
 * Follows the Runatal App States design (Figma node 14:18746).
 *
 * @param modifier Modifier for the skeleton box
 * @param height Height of the skeleton placeholder
 * @param brush Shimmer brush; share across siblings for synchronized animation
 */
@Composable
fun SkeletonRect(
    modifier: Modifier = Modifier,
    height: Dp = 16.dp,
    brush: Brush = rememberShimmerBrush()
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(brush = brush, shape = RunicExpressiveTheme.shapes.skeleton)
    )
}

/**
 * A circular skeleton placeholder with shimmer animation.
 *
 * @param size Diameter of the circle
 * @param modifier Modifier for the skeleton circle
 * @param brush Shimmer brush; share across siblings for synchronized animation
 */
@Composable
fun SkeletonCircle(
    size: Dp,
    modifier: Modifier = Modifier,
    brush: Brush = rememberShimmerBrush()
) {
    Box(
        modifier = modifier
            .size(size)
            .background(brush = brush, shape = CircleShape)
    )
}

/**
 * A skeleton placeholder shaped like a card with configurable height.
 *
 * Uses the contentCard shape token for rounded corners.
 *
 * @param modifier Modifier for the skeleton card
 * @param height Height of the card placeholder
 * @param brush Shimmer brush; share across siblings for synchronized animation
 */
@Composable
fun SkeletonCard(
    modifier: Modifier = Modifier,
    height: Dp = 200.dp,
    brush: Brush = rememberShimmerBrush()
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(brush = brush, shape = RunicExpressiveTheme.shapes.contentCard)
    )
}

/**
 * A group of text-line skeleton placeholders, simulating a paragraph.
 *
 * The last line is shorter (75% width) to mimic natural text flow.
 *
 * @param lines Number of skeleton text lines
 * @param lineHeight Height of each line
 * @param lineSpacing Vertical spacing between lines
 * @param modifier Modifier for the outer column
 * @param brush Shimmer brush; share across siblings for synchronized animation
 */
@Composable
fun SkeletonTextBlock(
    modifier: Modifier = Modifier,
    lines: Int = 3,
    lineHeight: Dp = 14.dp,
    lineSpacing: Dp = 10.dp,
    brush: Brush = rememberShimmerBrush()
) {
    Column(modifier = modifier) {
        repeat(lines) { index ->
            if (index > 0) {
                Spacer(modifier = Modifier.height(lineSpacing))
            }
            val widthFraction = if (index == lines - 1 && lines > 1) 0.75f else 1f
            SkeletonRect(
                modifier = Modifier.fillMaxWidth(widthFraction),
                height = lineHeight,
                brush = brush
            )
        }
    }
}
