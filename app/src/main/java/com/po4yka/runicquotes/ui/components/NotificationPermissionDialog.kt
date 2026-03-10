package com.po4yka.runicquotes.ui.components

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
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    color = colors.primaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = null,
                        modifier = Modifier.padding(14.dp).size(28.dp),
                        tint = colors.onPrimaryContainer
                    )
                }
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "Daily Rune Wisdom",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Get a new runic quote every morning to start your day with ancient wisdom.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(41.dp)
                    ) {
                        Text(text = "Not Now")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f).height(41.dp),
                        shape = RunicExpressiveTheme.shapes.segmentedControl
                    ) {
                        Text(text = "Enable")
                    }
                }
            }
        }
    }
}
