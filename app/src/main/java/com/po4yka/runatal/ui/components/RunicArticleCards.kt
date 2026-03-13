package com.po4yka.runatal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.po4yka.runatal.ui.theme.RunicExpressiveTheme

/**
 * Shared shell for read-only article and knowledge cards.
 */
@Composable
fun RunicArticleCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape = RunicExpressiveTheme.shapes.heroCard,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f),
    contentPadding: PaddingValues = PaddingValues(
        horizontal = RunicExpressiveTheme.spacing.comfortable,
        vertical = RunicExpressiveTheme.spacing.comfortable
    ),
    contentGap: Dp = RunicExpressiveTheme.spacing.small,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit
) {
    RunicInfoCard(
        modifier = modifier,
        onClick = onClick,
        shape = shape,
        containerColor = containerColor,
        borderColor = borderColor,
        contentPadding = PaddingValues(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(contentGap),
            horizontalAlignment = horizontalAlignment,
            content = content
        )
    }
}

/**
 * Shared lead card for contextual article copy with a single leading icon.
 */
@Composable
fun RunicArticleLeadCard(
    text: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector = Icons.Outlined.Info,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    iconTint: Color = contentColor
) {
    RunicArticleCard(
        modifier = modifier,
        containerColor = containerColor,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp),
        contentGap = 0.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                modifier = Modifier.padding(top = 2.dp),
                tint = iconTint
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = contentColor
            )
        }
    }
}

/**
 * Shared labeled article section card for read-only reference content.
 */
@Composable
fun RunicArticleSectionCard(
    label: String,
    body: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    labelColor: Color = MaterialTheme.colorScheme.secondary,
    bodyColor: Color = MaterialTheme.colorScheme.onSurface,
    bodyStyle: TextStyle = MaterialTheme.typography.bodyMedium
) {
    RunicArticleCard(
        modifier = modifier,
        containerColor = containerColor,
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        contentGap = 8.dp
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = labelColor
        )
        Text(
            text = body,
            style = bodyStyle,
            color = bodyColor
        )
    }
}

/**
 * Shared accent card for reference-style overview content with a leading stripe and icon badge.
 */
@Composable
fun RunicArticleAccentCard(
    title: String,
    description: String,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    accentColor: Color = MaterialTheme.colorScheme.secondary,
    iconContainerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    iconTint: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    minHeight: Dp = 96.dp
) {
    RunicArticleCard(
        modifier = modifier,
        containerColor = containerColor,
        contentPadding = PaddingValues(0.dp),
        contentGap = 0.dp
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .heightIn(min = minHeight)
                    .background(accentColor)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RunicExpressiveTheme.shapes.contentCard)
                        .background(iconContainerColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = iconTint
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Shared tappable article link card with supporting description.
 */
@Composable
fun RunicArticleLinkCard(
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    titleColor: Color = MaterialTheme.colorScheme.secondary,
    leadingIcon: ImageVector? = null,
    leadingIconContainerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    leadingIconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    trailingIcon: ImageVector = Icons.AutoMirrored.Filled.ArrowForward,
    trailingIconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    RunicArticleCard(
        modifier = modifier,
        onClick = onClick,
        containerColor = containerColor,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        contentGap = 0.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                RunicGlyphBadge(
                    size = RunicExpressiveTheme.controls.leadingBadgeMedium,
                    containerColor = leadingIconContainerColor
                ) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = leadingIconTint
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = titleColor
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = trailingIcon,
                contentDescription = null,
                tint = trailingIconTint
            )
        }
    }
}

/**
 * Shared divider for article and knowledge cards.
 */
@Composable
fun RunicArticleDivider(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f)
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(RunicExpressiveTheme.strokes.subtle)
            .background(color)
    )
}
