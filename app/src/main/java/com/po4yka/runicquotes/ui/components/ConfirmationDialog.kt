package com.po4yka.runicquotes.ui.components

import androidx.compose.foundation.layout.Arrangement
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
    isDestructive: Boolean = true
) {
    val colors = MaterialTheme.colorScheme

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 38.dp),
            shape = RunicExpressiveTheme.shapes.dialog,
            tonalElevation = RunicExpressiveTheme.elevations.overlay,
            color = colors.surface
        ) {
            Column(
                modifier = Modifier.padding(start = 25.dp, end = 25.dp, top = 29.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ConfirmationDialogHeader(icon, isDestructive)
                Spacer(modifier = Modifier.height(18.dp))
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
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))
                ConfirmationDialogButtons(
                    dismissLabel = dismissLabel,
                    confirmLabel = confirmLabel,
                    confirmIcon = confirmIcon,
                    onDismiss = onDismiss,
                    onConfirm = onConfirm,
                    containerColor = if (isDestructive) colors.error else colors.primary,
                    contentColor = if (isDestructive) colors.onError else colors.onPrimary
                )
            }
        }
    }
}

@Composable
private fun ConfirmationDialogHeader(icon: ImageVector, isDestructive: Boolean) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.size(56.dp),
        shape = CircleShape,
        color = if (isDestructive) colors.errorContainer else colors.primaryContainer
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.padding(15.dp).size(26.dp),
            tint = if (isDestructive) colors.onErrorContainer else colors.onPrimaryContainer
        )
    }
}

@Composable
private fun ConfirmationDialogButtons(
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
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(
            onClick = onDismiss,
            modifier = Modifier.weight(1f).height(41.dp)
        ) {
            Text(text = dismissLabel)
        }
        Button(
            onClick = onConfirm,
            modifier = Modifier.weight(1f).height(41.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = containerColor,
                contentColor = contentColor
            ),
            shape = RunicExpressiveTheme.shapes.segmentedControl
        ) {
            if (confirmIcon != null) {
                Icon(
                    imageVector = confirmIcon,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(text = confirmLabel)
        }
    }
}
