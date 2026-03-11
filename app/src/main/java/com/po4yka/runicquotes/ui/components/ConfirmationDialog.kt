package com.po4yka.runicquotes.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.po4yka.runicquotes.ui.theme.RunicExpressiveTheme

/**
 * Styled confirmation dialog following Runatal design system (Figma node 14:19678).
 *
 * Displays a centered icon, title, message, and confirm/dismiss buttons.
 * The confirm button uses error color for destructive actions by default.
 *
 * @param title Dialog heading (e.g. "Delete this quote?")
 * @param message Supporting description text
 * @param confirmLabel Label for the confirm button
 * @param onConfirm Callback when confirm is tapped
 * @param onDismiss Callback when dismiss is tapped or dialog is dismissed
 * @param modifier Modifier for the dialog surface
 * @param dismissLabel Label for the dismiss button
 * @param icon Icon displayed in a circular container at the top
 * @param confirmIcon Optional leading icon for the confirm button
 * @param isDestructive When true, confirm button uses error colors
 * @param supportingContent Optional content shown between body copy and actions
 */
@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dismissLabel: String = "Cancel",
    icon: ImageVector = Icons.Filled.Warning,
    confirmIcon: ImageVector? = null,
    isDestructive: Boolean = true,
    supportingContent: (@Composable ColumnScope.() -> Unit)? = null
) {
    val colors = MaterialTheme.colorScheme

    RunicDialogSurface(
        onDismiss = onDismiss,
        modifier = modifier
    ) {
        RunicDialogIconBadge(
            icon = icon,
            isDestructive = isDestructive
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = colors.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        if (supportingContent != null) {
            Spacer(modifier = Modifier.height(16.dp))
            supportingContent()
        }
        Spacer(modifier = Modifier.height(20.dp))
        RunicDialogButtonRow(
            dismissLabel = dismissLabel,
            confirmLabel = confirmLabel,
            confirmIcon = confirmIcon,
            onDismiss = onDismiss,
            onConfirm = onConfirm,
            containerColor = if (isDestructive) colors.error else colors.secondary,
            contentColor = if (isDestructive) colors.onError else colors.onSecondary
        )
    }
}

@Composable
internal fun RunicDialogSurface(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 34.dp),
            shape = RunicExpressiveTheme.shapes.dialog,
            tonalElevation = RunicExpressiveTheme.elevations.overlay,
            color = colors.surfaceContainerLow,
            border = BorderStroke(
                width = 1.dp,
                color = colors.outlineVariant.copy(alpha = 0.72f)
            )
        ) {
            Column(
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 22.dp, bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                content = content
            )
        }
    }
}

@Composable
internal fun RunicDialogIconBadge(icon: ImageVector, isDestructive: Boolean) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.size(40.dp),
        shape = CircleShape,
        color = if (isDestructive) {
            colors.errorContainer.copy(alpha = 0.45f)
        } else {
            colors.secondaryContainer.copy(alpha = 0.55f)
        }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "Dialog icon",
            modifier = Modifier.padding(11.dp).size(18.dp),
            tint = if (isDestructive) colors.error else colors.onSecondaryContainer
        )
    }
}

@Composable
internal fun RunicDialogButtonRow(
    dismissLabel: String,
    confirmLabel: String,
    confirmIcon: ImageVector?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    containerColor: Color,
    contentColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(
            onClick = onDismiss,
            modifier = Modifier.weight(1f).height(40.dp)
        ) {
            Text(
                text = dismissLabel,
                style = MaterialTheme.typography.labelLarge
            )
        }
        Button(
            onClick = onConfirm,
            modifier = Modifier.weight(1f).height(40.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = containerColor,
                contentColor = contentColor
            ),
            shape = RunicExpressiveTheme.shapes.segmentedControl
        ) {
            if (confirmIcon != null) {
                Icon(
                    imageVector = confirmIcon,
                    contentDescription = confirmLabel,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = confirmLabel,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}
