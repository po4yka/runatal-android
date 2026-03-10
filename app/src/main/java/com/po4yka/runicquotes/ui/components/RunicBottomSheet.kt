package com.po4yka.runicquotes.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.po4yka.runicquotes.ui.theme.RunicExpressiveTheme

/**
 * Quote actions bottom sheet following Runatal design system (Figma node 14:19678).
 *
 * Displays a list of actions, each as a row with circular icon container,
 * title, and subtitle. Destructive actions render in error color.
 *
 * @param actions List of actions to display
 * @param onDismiss Callback when the sheet is dismissed
 * @param modifier Modifier for the bottom sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunicBottomSheet(
    actions: List<BottomSheetAction>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RunicExpressiveTheme.shapes.bottomSheet,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            actions.forEachIndexed { index, action ->
                BottomSheetActionRow(action)
                if (index < actions.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 21.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
        }
    }
}

/**
 * Data class representing a single action in the bottom sheet.
 *
 * @param icon Icon displayed in a circular container
 * @param title Primary action label
 * @param subtitle Supporting description text
 * @param isDestructive When true, renders in error color (e.g. Delete)
 * @param onClick Callback when this action is tapped
 */
data class BottomSheetAction(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val isDestructive: Boolean = false,
    val onClick: () -> Unit
)

@Composable
private fun BottomSheetActionRow(action: BottomSheetAction) {
    val colors = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current

    val iconContainerColor: Color
    val iconTint: Color
    val titleColor: Color
    val subtitleColor: Color

    if (action.isDestructive) {
        iconContainerColor = colors.errorContainer
        iconTint = colors.onErrorContainer
        titleColor = colors.error
        subtitleColor = colors.error.copy(alpha = 0.7f)
    } else {
        iconContainerColor = colors.surfaceContainerHighest
        iconTint = colors.onSurface
        titleColor = colors.onSurface
        subtitleColor = colors.onSurfaceVariant
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                action.onClick()
            }
            .padding(horizontal = 24.dp, vertical = 10.dp)
            .height(40.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(38.dp),
            shape = CircleShape,
            color = iconContainerColor
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = action.title,
                modifier = Modifier.padding(10.dp).size(17.dp),
                tint = iconTint
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(
                text = action.title,
                style = MaterialTheme.typography.bodyLarge,
                color = titleColor
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = action.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = subtitleColor
            )
        }
    }
}
