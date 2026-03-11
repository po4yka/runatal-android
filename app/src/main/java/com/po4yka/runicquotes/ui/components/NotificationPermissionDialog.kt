package com.po4yka.runicquotes.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.po4yka.runicquotes.ui.theme.RunicExpressiveTheme

/**
 * Pre-permission dialog explaining the value of daily rune notifications.
 * Shown from the QuoteScreen bell icon before navigating to notification settings.
 */
@Composable
fun NotificationPermissionDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val spacing = RunicExpressiveTheme.spacing
    val strokes = RunicExpressiveTheme.strokes
    val controls = RunicExpressiveTheme.controls
    val icons = RunicExpressiveTheme.icons

    RunicDialogSurface(
        onDismiss = onDismiss,
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier.size(controls.leadingBadgeLarge),
            shape = RunicExpressiveTheme.shapes.pill,
            color = colors.secondaryContainer.copy(alpha = 0.55f)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = "Notifications",
                    modifier = Modifier.size(icons.standard),
                    tint = colors.onSecondaryContainer
                )
            }
        }
        Spacer(modifier = Modifier.height(spacing.comfortable))
        Text(
            text = "Daily Rune Wisdom",
            style = MaterialTheme.typography.titleLarge,
            color = colors.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(spacing.small))
        Text(
            text = "Allow Runatal to send you a new runic quote each morning.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(spacing.comfortable))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RunicExpressiveTheme.shapes.contentCard,
            color = colors.surfaceContainerHighest.copy(alpha = 0.22f),
            border = BorderStroke(strokes.subtle, colors.outlineVariant.copy(alpha = 0.45f))
        ) {
            Column(
                modifier = Modifier.padding(horizontal = spacing.standard, vertical = spacing.standard),
                verticalArrangement = Arrangement.spacedBy(spacing.medium)
            ) {
                NotificationBenefitRow("Fresh quote every morning")
                NotificationBenefitRow("Personalized based on your favorite script")
                NotificationBenefitRow("Quiet, respectful reminders")
            }
        }
        Spacer(modifier = Modifier.height(spacing.comfortable + spacing.micro))
        Button(
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth().height(controls.dialogActionHeight),
            shape = RunicExpressiveTheme.shapes.segmentedControl,
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.secondary,
                contentColor = colors.onSecondary
            )
        ) {
            Text(
                text = "Allow Notifications",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun NotificationBenefitRow(label: String) {
    val colors = MaterialTheme.colorScheme
    val spacing = RunicExpressiveTheme.spacing
    val icons = RunicExpressiveTheme.icons

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(icons.compact),
            tint = colors.secondary
        )
        Spacer(modifier = Modifier.width(spacing.medium))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = colors.onSurfaceVariant
        )
    }
}
