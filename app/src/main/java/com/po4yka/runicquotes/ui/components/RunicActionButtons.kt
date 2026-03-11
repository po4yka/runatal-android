package com.po4yka.runicquotes.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import com.po4yka.runicquotes.ui.theme.RunicExpressiveTheme

/**
 * Visual variants for shared runic action buttons.
 */
enum class RunicActionButtonStyle {
    Primary,
    Secondary,
    Outlined,
    Tonal,
    DestructiveOutlined
}

/**
 * Color roles for the shared runic action-button family.
 */
@Immutable
data class RunicActionButtonColors(
    val containerColor: Color,
    val contentColor: Color,
    val borderColor: Color,
    val disabledContainerColor: Color,
    val disabledContentColor: Color,
    val disabledBorderColor: Color
)

/**
 * Resolves default or customized colors for a shared runic action button.
 */
@Composable
fun runicActionButtonColors(
    style: RunicActionButtonStyle = RunicActionButtonStyle.Primary,
    containerColor: Color = Color.Unspecified,
    contentColor: Color = Color.Unspecified,
    borderColor: Color = Color.Unspecified,
    disabledContainerColor: Color = Color.Unspecified,
    disabledContentColor: Color = Color.Unspecified,
    disabledBorderColor: Color = Color.Unspecified
): RunicActionButtonColors {
    val scheme = MaterialTheme.colorScheme
    val defaults = when (style) {
        RunicActionButtonStyle.Primary -> RunicActionButtonColors(
            containerColor = scheme.secondary,
            contentColor = scheme.onSecondary,
            borderColor = Color.Transparent,
            disabledContainerColor = scheme.surfaceContainerHigh,
            disabledContentColor = scheme.onSurfaceVariant,
            disabledBorderColor = Color.Transparent
        )

        RunicActionButtonStyle.Secondary -> RunicActionButtonColors(
            containerColor = scheme.surfaceContainerLow,
            contentColor = scheme.onSurface,
            borderColor = Color.Transparent,
            disabledContainerColor = scheme.surfaceContainerLow,
            disabledContentColor = scheme.onSurfaceVariant,
            disabledBorderColor = Color.Transparent
        )

        RunicActionButtonStyle.Outlined -> RunicActionButtonColors(
            containerColor = scheme.surfaceContainerLow,
            contentColor = scheme.onSurface,
            borderColor = scheme.outlineVariant.copy(alpha = 0.85f),
            disabledContainerColor = scheme.surfaceContainerLow,
            disabledContentColor = scheme.onSurfaceVariant,
            disabledBorderColor = scheme.outlineVariant.copy(alpha = 0.55f)
        )

        RunicActionButtonStyle.Tonal -> RunicActionButtonColors(
            containerColor = scheme.surfaceContainerHighest,
            contentColor = scheme.onSurfaceVariant,
            borderColor = Color.Transparent,
            disabledContainerColor = scheme.surfaceContainerHigh,
            disabledContentColor = scheme.onSurfaceVariant,
            disabledBorderColor = Color.Transparent
        )

        RunicActionButtonStyle.DestructiveOutlined -> RunicActionButtonColors(
            containerColor = scheme.errorContainer.copy(alpha = 0.18f),
            contentColor = scheme.error,
            borderColor = scheme.error.copy(alpha = 0.14f),
            disabledContainerColor = scheme.errorContainer.copy(alpha = 0.12f),
            disabledContentColor = scheme.onSurfaceVariant,
            disabledBorderColor = scheme.error.copy(alpha = 0.08f)
        )
    }

    return RunicActionButtonColors(
        containerColor = containerColor.orElse(defaults.containerColor),
        contentColor = contentColor.orElse(defaults.contentColor),
        borderColor = borderColor.orElse(defaults.borderColor),
        disabledContainerColor = disabledContainerColor.orElse(defaults.disabledContainerColor),
        disabledContentColor = disabledContentColor.orElse(defaults.disabledContentColor),
        disabledBorderColor = disabledBorderColor.orElse(defaults.disabledBorderColor)
    )
}

/**
 * Shared text action button used for primary, secondary, and destructive flows.
 */
@Composable
fun RunicActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accessibilityLabel: String? = null,
    stateDescription: String? = null,
    colors: RunicActionButtonColors = runicActionButtonColors(),
    shape: Shape = RunicExpressiveTheme.shapes.collectionCard,
    minHeight: Dp = RunicExpressiveTheme.controls.minimumTouchTarget,
    borderWidth: Dp = RunicExpressiveTheme.strokes.subtle,
    contentPadding: PaddingValues = PaddingValues(horizontal = RunicExpressiveTheme.spacing.roomy),
    textStyle: TextStyle = MaterialTheme.typography.labelLarge,
    leadingContent: (@Composable BoxScope.() -> Unit)? = null,
    trailingContent: (@Composable BoxScope.() -> Unit)? = null
) {
    val iconSize = RunicExpressiveTheme.icons.standard
    val spacing = RunicExpressiveTheme.spacing.small
    val resolvedColors = colors.resolve(enabled = enabled)
    val accessibilityModifier = if (!accessibilityLabel.isNullOrBlank() || !stateDescription.isNullOrBlank()) {
        Modifier.semantics(mergeDescendants = true) {
            accessibilityLabel?.let { contentDescription = it }
            stateDescription?.let { this.stateDescription = it }
        }
    } else {
        Modifier
    }

    RunicActionButtonSurface(
        modifier = modifier
            .heightIn(min = minHeight)
            .then(accessibilityModifier),
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        borderWidth = borderWidth,
        colors = resolvedColors
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingContent != null) {
                ActionButtonSlot(
                    size = iconSize,
                    contentColor = resolvedColors.contentColor,
                    content = leadingContent
                )
            }
            if (leadingContent != null) {
                Box(modifier = Modifier.size(spacing))
            }
            Text(
                text = label,
                style = textStyle,
                color = resolvedColors.contentColor
            )
            if (trailingContent != null) {
                Box(modifier = Modifier.size(spacing))
                ActionButtonSlot(
                    size = iconSize,
                    contentColor = resolvedColors.contentColor,
                    content = trailingContent
                )
            }
        }
    }
}

/**
 * Shared icon-only action button for compact utility actions.
 */
@Composable
fun RunicActionIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    stateDescription: String? = null,
    colors: RunicActionButtonColors = runicActionButtonColors(
        style = RunicActionButtonStyle.Outlined
    ),
    shape: Shape = RunicExpressiveTheme.shapes.collectionCard,
    size: Dp = RunicExpressiveTheme.controls.minimumTouchTarget,
    borderWidth: Dp = RunicExpressiveTheme.strokes.subtle,
    iconContent: @Composable BoxScope.() -> Unit
) {
    val resolvedColors = colors.resolve(enabled = enabled)

    RunicActionButtonSurface(
        modifier = modifier.size(size),
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        borderWidth = borderWidth,
        colors = resolvedColors
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .semantics {
                    this.contentDescription = contentDescription
                    stateDescription?.let { this.stateDescription = it }
                },
            contentAlignment = Alignment.Center
        ) {
            ActionButtonSlot(
                size = RunicExpressiveTheme.icons.standard,
                contentColor = resolvedColors.contentColor,
                content = iconContent
            )
        }
    }
}

@Composable
private fun RunicActionButtonSurface(
    modifier: Modifier,
    onClick: () -> Unit,
    enabled: Boolean,
    shape: Shape,
    borderWidth: Dp,
    colors: ResolvedRunicActionButtonColors,
    content: @Composable () -> Unit
) {
    val border = if (colors.borderColor.alpha > 0f) {
        BorderStroke(width = borderWidth, color = colors.borderColor)
    } else {
        null
    }

    Surface(
        modifier = modifier,
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        color = colors.containerColor,
        border = border,
        content = content
    )
}

@Composable
private fun ActionButtonSlot(
    size: Dp,
    contentColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            content()
        }
    }
}

@Immutable
private data class ResolvedRunicActionButtonColors(
    val containerColor: Color,
    val contentColor: Color,
    val borderColor: Color
)

private fun RunicActionButtonColors.resolve(enabled: Boolean): ResolvedRunicActionButtonColors {
    return if (enabled) {
        ResolvedRunicActionButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            borderColor = borderColor
        )
    } else {
        ResolvedRunicActionButtonColors(
            containerColor = disabledContainerColor,
            contentColor = disabledContentColor,
            borderColor = disabledBorderColor
        )
    }
}

private fun Color.orElse(fallback: Color): Color = if (this == Color.Unspecified) fallback else this
