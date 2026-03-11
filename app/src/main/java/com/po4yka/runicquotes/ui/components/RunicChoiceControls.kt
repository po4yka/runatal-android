package com.po4yka.runicquotes.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.po4yka.runicquotes.ui.theme.LocalReduceMotion
import com.po4yka.runicquotes.ui.theme.RunicExpressiveTheme

/** Namespace for the shared runic choice-control family. */
object RunicChoiceControls

/** Color roles for a selected or unselected runic choice chip. */
@Immutable
data class RunicChoiceChipColors(
    val containerColor: Color,
    val contentColor: Color,
    val borderColor: Color = Color.Transparent
)

/** Shared bordered container for grouped choice chips. */
@Composable
fun RunicChoiceGroup(
    modifier: Modifier = Modifier,
    expand: Boolean = false,
    shape: Shape? = null,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
    borderWidth: Dp = Dp.Unspecified,
    contentPadding: PaddingValues = PaddingValues(1.dp),
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(0.dp),
    content: @Composable RowScope.() -> Unit
) {
    val resolvedShape = shape ?: RunicExpressiveTheme.shapes.segmentedControl
    val resolvedBorderWidth = if (borderWidth != Dp.Unspecified) {
        borderWidth
    } else {
        RunicExpressiveTheme.strokes.subtle
    }

    Surface(
        modifier = modifier,
        shape = resolvedShape,
        color = containerColor,
        border = BorderStroke(width = resolvedBorderWidth, color = borderColor)
    ) {
        Row(
            modifier = if (expand) {
                Modifier
                    .fillMaxWidth()
                    .padding(contentPadding)
            } else {
                Modifier.padding(contentPadding)
            },
            horizontalArrangement = horizontalArrangement,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

/**
 * Shared selectable pill used for segmented controls, compact choice rows, and toggle chips.
 *
 * The slot receives the resolved content color so callers can tint icons and text consistently.
 */
@Composable
fun RunicChoiceChip(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape? = null,
    minHeight: Dp = Dp.Unspecified,
    role: Role = Role.Tab,
    stateDescription: String? = null,
    colors: RunicChoiceChipColors? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(4.dp),
    content: @Composable RowScope.(Color) -> Unit
) {
    val reducedMotion = LocalReduceMotion.current
    val motion = RunicExpressiveTheme.motion
    val resolvedShape = shape ?: RunicExpressiveTheme.shapes.segment
    val resolvedMinHeight = if (minHeight != Dp.Unspecified) {
        minHeight
    } else {
        RunicExpressiveTheme.controls.minimumTouchTarget
    }
    val resolvedColors = colors ?: runicChoiceChipColors(selected = selected)
    val animDuration = motion.duration(reducedMotion, motion.shortDurationMillis)

    val containerColor by animateColorAsState(
        targetValue = resolvedColors.containerColor,
        animationSpec = tween(durationMillis = animDuration),
        label = "runicChoiceContainer"
    )
    val contentColor by animateColorAsState(
        targetValue = resolvedColors.contentColor,
        animationSpec = tween(durationMillis = animDuration),
        label = "runicChoiceContent"
    )
    val borderColor by animateColorAsState(
        targetValue = resolvedColors.borderColor,
        animationSpec = tween(durationMillis = animDuration),
        label = "runicChoiceBorder"
    )

    Surface(
        modifier = modifier.semantics {
            this.role = role
            this.selected = selected
            stateDescription?.let { this.stateDescription = it }
        },
        onClick = onClick,
        enabled = enabled,
        shape = resolvedShape,
        color = choiceStateColor(containerColor, enabled, disabledAlpha = 0.72f),
        border = if (borderColor.alpha > 0.01f) {
            BorderStroke(
                width = RunicExpressiveTheme.strokes.subtle,
                color = choiceStateColor(borderColor, enabled, disabledAlpha = 0.56f)
            )
        } else {
            null
        }
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = resolvedMinHeight)
                .padding(contentPadding),
            horizontalArrangement = horizontalArrangement,
            verticalAlignment = Alignment.CenterVertically
        ) {
            content(choiceStateColor(contentColor, enabled, disabledAlpha = 0.62f))
        }
    }
}

/** Returns the selected or unselected palette for a runic choice chip. */
@Composable
fun runicChoiceChipColors(
    selected: Boolean,
    selectedContainerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    unselectedContainerColor: Color = Color.Transparent,
    selectedContentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    unselectedContentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    selectedBorderColor: Color = Color.Transparent,
    unselectedBorderColor: Color = Color.Transparent
): RunicChoiceChipColors {
    return if (selected) {
        RunicChoiceChipColors(
            containerColor = selectedContainerColor,
            contentColor = selectedContentColor,
            borderColor = selectedBorderColor
        )
    } else {
        RunicChoiceChipColors(
            containerColor = unselectedContainerColor,
            contentColor = unselectedContentColor,
            borderColor = unselectedBorderColor
        )
    }
}

private fun choiceStateColor(color: Color, enabled: Boolean, disabledAlpha: Float): Color {
    return if (enabled) color else color.copy(alpha = disabledAlpha)
}
