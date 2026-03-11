package com.po4yka.runicquotes.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.po4yka.runicquotes.ui.theme.LocalReduceMotion
import com.po4yka.runicquotes.ui.theme.RunicExpressiveTheme

/**
 * A segmented control with 2 or 3 segments, following the Runatal design system.
 *
 * Active segment shows a filled background with a checkmark and bold text.
 * Inactive segments show regular text on a transparent background.
 *
 * @param segments List of segment labels (2 or 3 items)
 * @param selectedIndex Currently selected segment index
 * @param onSegmentSelected Callback when a segment is tapped
 * @param modifier Modifier for the container
 * @param leadingIcons Optional leading icon for each segment
 * @param counts Optional trailing count for each segment
 */
@Composable
fun SegmentedControl(
    segments: List<String>,
    selectedIndex: Int,
    onSegmentSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    leadingIcons: List<ImageVector>? = null,
    counts: List<Int>? = null
) {
    val reducedMotion = LocalReduceMotion.current
    val motion = RunicExpressiveTheme.motion
    val spacing = RunicExpressiveTheme.spacing
    val animDuration = motion.duration(reducedMotion, motion.shortDurationMillis)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RunicExpressiveTheme.shapes.segmentedControl,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.tight),
            horizontalArrangement = Arrangement.spacedBy(spacing.micro)
        ) {
            segments.forEachIndexed { index, label ->
                Segment(
                    label = label,
                    isSelected = index == selectedIndex,
                    onClick = { onSegmentSelected(index) },
                    animDurationMillis = animDuration,
                    leadingIcon = leadingIcons?.getOrNull(index),
                    count = counts?.getOrNull(index),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun Segment(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    animDurationMillis: Int,
    leadingIcon: ImageVector? = null,
    count: Int? = null,
    modifier: Modifier = Modifier
) {
    val spacing = RunicExpressiveTheme.spacing
    val controls = RunicExpressiveTheme.controls
    val icons = RunicExpressiveTheme.icons
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        animationSpec = tween(durationMillis = animDurationMillis),
        label = "segmentBackground"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(durationMillis = animDurationMillis),
        label = "segmentContent"
    )

    Box(
        modifier = modifier
            .clip(RunicExpressiveTheme.shapes.segment)
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .semantics {
                role = Role.Tab
                selected = isSelected
            }
            .heightIn(min = controls.segmentedControlMinHeight)
            .padding(horizontal = spacing.standard, vertical = spacing.small),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.tight),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val icon = leadingIcon ?: if (isSelected) Icons.Default.Check else null
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(icons.inline),
                    tint = contentColor
                )
            }
            Text(
                text = if (count != null) "$label $count" else label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = contentColor
            )
        }
    }
}
