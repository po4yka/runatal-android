package com.po4yka.runicquotes.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.unit.dp
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

    RunicDialogSurface(
        onDismiss = onDismiss,
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = colors.secondaryContainer.copy(alpha = 0.55f)
        ) {
            Icon(
                imageVector = Icons.Outlined.Notifications,
                contentDescription = "Notifications",
                modifier = Modifier.padding(11.dp).size(18.dp),
                tint = colors.onSecondaryContainer
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Daily Rune Wisdom",
            style = MaterialTheme.typography.titleLarge,
            color = colors.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Allow Runatal to send you a new runic quote each morning.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RunicExpressiveTheme.shapes.contentCard,
            color = colors.surfaceContainerHighest.copy(alpha = 0.22f),
            border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.45f))
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                NotificationBenefitRow("Fresh quote every morning")
                NotificationBenefitRow("Personalized based on your favorite script")
                NotificationBenefitRow("Quiet, respectful reminders")
            }
        }
        Spacer(modifier = Modifier.height(18.dp))
        Button(
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth().height(40.dp),
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

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = colors.secondary
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = colors.onSurfaceVariant
        )
    }
}
