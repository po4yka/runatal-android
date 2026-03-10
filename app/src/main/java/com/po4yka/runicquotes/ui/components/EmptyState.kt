package com.po4yka.runicquotes.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Reusable empty state composable with centered icon, title, description,
 * and optional primary/secondary action buttons.
 *
 * Follows the Runatal App States design (Figma node 14:18746).
 *
 * @param icon Icon displayed in a circular container at the top
 * @param title Heading text (e.g. "Your Library is Empty")
 * @param description Supporting body text
 * @param modifier Modifier for the outer container
 * @param primaryActionLabel Label for the primary (filled) button
 * @param primaryActionIcon Optional leading icon for the primary button
 * @param onPrimaryAction Callback for the primary button tap
 * @param secondaryActionLabel Label for the secondary (outlined) button
 * @param secondaryActionIcon Optional leading icon for the secondary button
 * @param onSecondaryAction Callback for the secondary button tap
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    primaryActionLabel: String? = null,
    primaryActionIcon: ImageVector? = null,
    onPrimaryAction: (() -> Unit)? = null,
    secondaryActionLabel: String? = null,
    secondaryActionIcon: ImageVector? = null,
    onSecondaryAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        if (primaryActionLabel != null && onPrimaryAction != null) {
            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onPrimaryAction,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                if (primaryActionIcon != null) {
                    Icon(
                        imageVector = primaryActionIcon,
                        contentDescription = null,
                        modifier = Modifier
                            .size(18.dp)
                            .padding(end = 0.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                }
                Text(text = primaryActionLabel)
            }
        }

        if (secondaryActionLabel != null && onSecondaryAction != null) {
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onSecondaryAction,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                if (secondaryActionIcon != null) {
                    Icon(
                        imageVector = secondaryActionIcon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                }
                Text(text = secondaryActionLabel)
            }
        }
    }
}
