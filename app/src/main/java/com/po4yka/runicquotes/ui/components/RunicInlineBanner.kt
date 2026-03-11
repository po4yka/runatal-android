package com.po4yka.runicquotes.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.po4yka.runicquotes.ui.theme.RunicExpressiveTheme

/** Shared color roles for inline feedback banners and compact notice surfaces. */
@Immutable
data class RunicInlineBannerColors(
    val containerColor: Color,
    val messageColor: Color,
    val emphasisColor: Color,
    val iconContainerColor: Color,
    val iconTint: Color,
    val borderColor: Color
)

/** Shared banner styles for tonal feedback and inverse floating notices. */
enum class RunicInlineBannerStyle {
    Tonal,
    Inverse
}

/** Returns the shared color treatment for [RunicInlineBanner]. */
@Composable
fun runicInlineBannerColors(
    style: RunicInlineBannerStyle = RunicInlineBannerStyle.Tonal
): RunicInlineBannerColors {
    val colors = MaterialTheme.colorScheme

    return when (style) {
        RunicInlineBannerStyle.Tonal -> RunicInlineBannerColors(
            containerColor = colors.surfaceContainerHighest,
            messageColor = colors.onSurface,
            emphasisColor = colors.primary,
            iconContainerColor = colors.surface,
            iconTint = colors.onSurfaceVariant,
            borderColor = colors.outlineVariant.copy(alpha = 0.52f)
        )

        RunicInlineBannerStyle.Inverse -> RunicInlineBannerColors(
            containerColor = colors.inverseSurface,
            messageColor = colors.inverseOnSurface,
            emphasisColor = colors.inverseOnSurface.copy(alpha = 0.82f),
            iconContainerColor = colors.inverseOnSurface.copy(alpha = 0.12f),
            iconTint = colors.inverseOnSurface,
            borderColor = Color.Transparent
        )
    }
}

/** Shared inline banner for transient feedback, notices, and compact callouts. */
@Composable
fun RunicInlineBanner(
    message: String,
    modifier: Modifier = Modifier,
    trailingText: String? = null,
    onTrailingClick: (() -> Unit)? = null,
    trailingContentDescription: String? = null,
    leadingIcon: ImageVector? = null,
    colors: RunicInlineBannerColors = runicInlineBannerColors(),
    shape: Shape = RunicExpressiveTheme.shapes.contentCard,
    shadowElevation: Dp = RunicExpressiveTheme.elevations.raisedCard,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    expandContent: Boolean = false
) {
    val border = if (colors.borderColor.alpha > 0f) {
        BorderStroke(
            width = RunicExpressiveTheme.strokes.subtle,
            color = colors.borderColor
        )
    } else {
        null
    }

    Surface(
        modifier = modifier,
        shape = shape,
        color = colors.containerColor,
        border = border,
        shadowElevation = shadowElevation
    ) {
        Row(
            modifier = Modifier
                .then(if (expandContent) Modifier.fillMaxWidth() else Modifier)
                .padding(contentPadding),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                RunicGlyphBadge(
                    size = RunicExpressiveTheme.controls.leadingBadgeMedium,
                    shape = RunicExpressiveTheme.shapes.segment,
                    containerColor = colors.iconContainerColor
                ) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = colors.iconTint
                    )
                }
            }

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.messageColor,
                modifier = if (expandContent && trailingText != null) {
                    Modifier.weight(1f)
                } else {
                    Modifier
                }
            )

            if (trailingText != null) {
                Box(
                    modifier = if (onTrailingClick != null) {
                        Modifier
                            .clip(RunicExpressiveTheme.shapes.segment)
                            .clickable(onClick = onTrailingClick)
                            .semantics {
                                role = Role.Button
                                if (trailingContentDescription != null) {
                                    contentDescription = trailingContentDescription
                                }
                            }
                            .sizeIn(minHeight = RunicExpressiveTheme.controls.minimumTouchTarget)
                            .padding(horizontal = 8.dp)
                    } else {
                        Modifier
                    },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = trailingText,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                        color = colors.emphasisColor
                    )
                }
            }
        }
    }
}
