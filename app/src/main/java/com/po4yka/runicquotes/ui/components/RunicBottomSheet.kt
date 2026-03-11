package com.po4yka.runicquotes.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.po4yka.runicquotes.domain.model.RunicScript
import com.po4yka.runicquotes.ui.theme.RunicExpressiveTheme
import com.po4yka.runicquotes.ui.theme.RunicTextRole
import com.po4yka.runicquotes.ui.theme.RunicTypeRoles
import com.po4yka.runicquotes.ui.theme.SupportingTextRole

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
    preview: BottomSheetQuotePreview? = null,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RunicExpressiveTheme.shapes.bottomSheet,
        modifier = modifier,
        containerColor = colors.surfaceContainerLow,
        scrimColor = colors.scrim.copy(alpha = 0.45f),
        dragHandle = null,
        tonalElevation = RunicExpressiveTheme.elevations.overlay
    ) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            SheetHandle()
            if (preview != null) {
                QuotePreviewCard(preview = preview)
                Spacer(modifier = Modifier.height(10.dp))
            }
            actions.forEachIndexed { index, action ->
                BottomSheetActionRow(action)
                if (index < actions.lastIndex) {
                    Spacer(modifier = Modifier.height(2.dp))
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

/**
 * Compact quote preview shown at the top of the actions sheet.
 *
 * @property runicText Transliterated rune text displayed in the preview card
 * @property author Quote author rendered beneath the preview text
 * @property font Active rune font key used by [RunicText]
 * @property script Active script used to tune glyph metrics
 */
data class BottomSheetQuotePreview(
    val runicText: String,
    val author: String,
    val font: String,
    val script: RunicScript
)

@Composable
private fun SheetHandle() {
    val colors = MaterialTheme.colorScheme

    RunicOrnamentRule(
        modifier = Modifier
            .padding(top = 12.dp, bottom = 10.dp),
        width = 36.dp,
        thickness = 4.dp,
        color = colors.onSurface.copy(alpha = 0.12f)
    )
}

@Composable
private fun QuotePreviewCard(preview: BottomSheetQuotePreview) {
    val colors = MaterialTheme.colorScheme

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp),
        color = colors.surfaceContainerHighest.copy(alpha = 0.18f),
        border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.42f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.Top
        ) {
            RunicOrnamentRule(
                modifier = Modifier
                    .padding(top = 2.dp),
                width = 3.dp,
                thickness = 38.dp,
                color = colors.secondary.copy(alpha = 0.48f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                RunicText(
                    text = preview.runicText,
                    font = preview.font,
                    script = preview.script,
                    role = RunicTextRole.BottomSheetPreview,
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RunicOrnamentRule(
                        width = 14.dp,
                        thickness = 1.5.dp,
                        color = colors.secondary.copy(alpha = 0.28f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = preview.author,
                        style = RunicTypeRoles.supporting(SupportingTextRole.CompactMeta),
                        color = colors.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomSheetActionRow(action: BottomSheetAction) {
    val colors = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current

    val iconContainerColor: Color
    val iconTint: Color
    val titleColor: Color
    val subtitleColor: Color

    if (action.isDestructive) {
        iconContainerColor = colors.errorContainer.copy(alpha = 0.38f)
        iconTint = colors.error
        titleColor = colors.error
        subtitleColor = colors.error.copy(alpha = 0.58f)
    } else {
        iconContainerColor = colors.onSurface.copy(alpha = 0.035f)
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
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .height(60.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RunicGlyphBadge(
            size = 38.dp,
            shape = RoundedCornerShape(16.dp),
            containerColor = iconContainerColor
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = action.title,
                modifier = Modifier
                    .padding(10.dp)
                    .size(17.dp),
                tint = iconTint
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(
                text = action.title,
                style = if (action.isDestructive) {
                    MaterialTheme.typography.labelLarge
                } else {
                    MaterialTheme.typography.titleSmall
                },
                color = titleColor
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = action.subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = subtitleColor
            )
        }
    }
}
