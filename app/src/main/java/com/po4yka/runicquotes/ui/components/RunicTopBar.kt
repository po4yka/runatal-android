package com.po4yka.runicquotes.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.graphics.Shape
import com.po4yka.runicquotes.ui.theme.RunicExpressiveTheme

/**
 * Shared centered top app bar for Runatal screens.
 *
 * It keeps title content centered while reserving symmetrical space for navigation
 * and trailing actions so screens do not reimplement the same bar structure.
 */
@Composable
fun RunicTopBar(
    modifier: Modifier = Modifier,
    navigationIcon: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    titleContent: @Composable ColumnScope.() -> Unit
) {
    val controls = RunicExpressiveTheme.controls
    val topBars = RunicExpressiveTheme.topBars

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = topBars.horizontalPadding,
                    vertical = topBars.verticalPadding
                )
                .heightIn(min = controls.minimumTouchTarget)
        ) {
            if (navigationIcon != null) {
                Box(
                    modifier = Modifier.align(Alignment.CenterStart),
                    contentAlignment = Alignment.CenterStart
                ) {
                    navigationIcon()
                }
            } else {
                Spacer(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(controls.minimumTouchTarget)
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(horizontal = topBars.titleSideClearance),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(RunicExpressiveTheme.spacing.tight),
                    content = titleContent
                )
            }

            if (trailingContent != null) {
                Box(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    trailingContent()
                }
            } else {
                Spacer(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(controls.minimumTouchTarget)
                )
            }
        }
    }
}

/** Visual variants for icon actions placed inside a [RunicTopBar]. */
@Immutable
enum class RunicTopBarActionStyle {
    Plain,
    Surface,
    Tonal,
    Filled,
    Outlined
}

/**
 * Standard icon action surface for [RunicTopBar] leading and trailing controls.
 */
@Composable
fun RunicTopBarIconAction(
    painter: VectorPainter,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: RunicTopBarActionStyle = RunicTopBarActionStyle.Plain,
    shape: Shape = RunicExpressiveTheme.shapes.pill
) {
    val controls = RunicExpressiveTheme.controls
    val strokes = RunicExpressiveTheme.strokes
    val palette = topBarActionPalette(
        style = style,
        colorScheme = MaterialTheme.colorScheme
    )

    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.size(controls.minimumTouchTarget),
        shape = shape,
        color = actionStateColor(palette.containerColor, enabled, disabledAlpha = 0.72f),
        border = palette.borderColor?.let { borderColor ->
            BorderStroke(
                width = strokes.subtle,
                color = actionStateColor(borderColor, enabled, disabledAlpha = 0.56f)
            )
        }
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painter,
                contentDescription = contentDescription,
                tint = actionStateColor(palette.contentColor, enabled, disabledAlpha = 0.62f)
            )
        }
    }
}

/** Convenience overload for vector icons used in [RunicTopBarIconAction]. */
@Composable
fun RunicTopBarIconAction(
    imageVector: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: RunicTopBarActionStyle = RunicTopBarActionStyle.Plain,
    shape: Shape = RunicExpressiveTheme.shapes.pill
) {
    RunicTopBarIconAction(
        painter = rememberVectorPainter(image = imageVector),
        contentDescription = contentDescription,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        style = style,
        shape = shape
    )
}

@Immutable
private data class RunicTopBarActionPalette(
    val containerColor: Color,
    val contentColor: Color,
    val borderColor: Color? = null
)

private fun topBarActionPalette(
    style: RunicTopBarActionStyle,
    colorScheme: ColorScheme
): RunicTopBarActionPalette {
    return when (style) {
        RunicTopBarActionStyle.Plain -> RunicTopBarActionPalette(
            containerColor = Color.Transparent,
            contentColor = colorScheme.onSurface
        )

        RunicTopBarActionStyle.Surface -> RunicTopBarActionPalette(
            containerColor = colorScheme.surfaceContainerLow,
            contentColor = colorScheme.onSurface
        )

        RunicTopBarActionStyle.Tonal -> RunicTopBarActionPalette(
            containerColor = colorScheme.secondaryContainer,
            contentColor = colorScheme.onSecondaryContainer
        )

        RunicTopBarActionStyle.Filled -> RunicTopBarActionPalette(
            containerColor = colorScheme.secondary,
            contentColor = colorScheme.onSecondary
        )

        RunicTopBarActionStyle.Outlined -> RunicTopBarActionPalette(
            containerColor = colorScheme.surfaceContainerLow,
            contentColor = colorScheme.onSurface,
            borderColor = colorScheme.outlineVariant.copy(alpha = 0.7f)
        )
    }
}

private fun actionStateColor(color: Color, enabled: Boolean, disabledAlpha: Float): Color {
    return if (enabled) color else color.copy(alpha = disabledAlpha)
}
