package com.po4yka.runicquotes.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import com.po4yka.runicquotes.ui.theme.LocalReduceMotion
import com.po4yka.runicquotes.ui.theme.RunicExpressiveTheme

/**
 * A single setting item with title, subtitle, and optional leading/trailing content.
 *
 * @param title The main title of the setting
 * @param modifier Modifier for the item
 * @param subtitle Optional subtitle/description
 * @param selected Whether this item is currently selected (shows checkmark)
 * @param onClick Callback when the item is clicked
 * @param leadingIcon Optional leading icon content
 * @param trailing Optional trailing content (e.g., Switch, value display)
 */
@Composable
fun SettingItem(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    val reducedMotion = LocalReduceMotion.current
    val motion = RunicExpressiveTheme.motion
    val spacing = RunicExpressiveTheme.spacing
    val controls = RunicExpressiveTheme.controls
    val itemShape = RunicExpressiveTheme.shapes.collectionCard
    val containerColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(
            durationMillis = motion.duration(reducedMotion, motion.shortDurationMillis)
        ),
        label = "settingItemContainer"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth(),
        color = containerColor,
        shape = itemShape
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = controls.settingItemMinHeight)
                .clip(itemShape)
                .clickable(enabled = onClick != null) { onClick?.invoke() }
                .padding(horizontal = spacing.comfortable, vertical = spacing.standard),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                Box(
                    modifier = Modifier,
                    contentAlignment = Alignment.Center
                ) {
                    leadingIcon()
                }
                Spacer(modifier = Modifier.width(spacing.standard))
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(spacing.comfortable))

            // Show trailing content if provided, otherwise show checkmark if selected
            if (trailing != null) {
                Box(contentAlignment = Alignment.Center) {
                    trailing()
                }
            } else if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
