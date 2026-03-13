package com.po4yka.runatal.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.po4yka.runatal.ui.theme.RunicExpressiveTheme
import com.po4yka.runatal.ui.theme.RunicTypeRoles
import com.po4yka.runatal.ui.theme.SupportingTextRole

/**
 * Shared bordered information card for read-only or lightly interactive surfaces.
 */
@Composable
fun RunicInfoCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape = RunicExpressiveTheme.shapes.contentCard,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f),
    borderWidth: Dp = RunicExpressiveTheme.strokes.subtle,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable BoxScope.() -> Unit
) {
    RunicCardSurface(
        modifier = modifier,
        onClick = onClick,
        shape = shape,
        containerColor = containerColor,
        borderColor = borderColor,
        borderWidth = borderWidth
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            content = content
        )
    }
}

/**
 * Shared input shell for form fields and free-form text entry surfaces.
 */
@Composable
fun RunicInputCard(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    shape: Shape = RunicExpressiveTheme.shapes.contentCard,
    containerColor: Color = if (enabled) {
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    },
    borderColor: Color = if (isError) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.outline
    },
    borderWidth: Dp = RunicExpressiveTheme.strokes.subtle,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable BoxScope.() -> Unit
) {
    RunicCardSurface(
        modifier = modifier,
        shape = shape,
        containerColor = containerColor,
        borderColor = borderColor,
        borderWidth = borderWidth
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            content = content
        )
    }
}

/**
 * Shared search field treatment for pack and reference browsing flows.
 */
@Composable
fun RunicSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholderText: String = "Search",
    enabled: Boolean = true,
    leadingContentDescription: String? = null,
    shape: Shape = RunicExpressiveTheme.shapes.contentCard
) {
    val colors = MaterialTheme.colorScheme

    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = true,
        shape = shape,
        placeholder = {
            Text(
                text = placeholderText,
                style = RunicTypeRoles.supporting(SupportingTextRole.FormPlaceholder),
                color = colors.onSurfaceVariant.copy(alpha = 0.72f)
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = leadingContentDescription,
                tint = colors.onSurfaceVariant
            )
        },
        trailingIcon = if (query.isNotBlank()) {
            {
                IconButton(
                    onClick = { onQueryChange("") },
                    enabled = enabled
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear search",
                        tint = colors.onSurfaceVariant
                    )
                }
            }
        } else {
            null
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = colors.onSurface,
            unfocusedTextColor = colors.onSurface,
            disabledTextColor = colors.onSurfaceVariant,
            focusedContainerColor = colors.surface,
            unfocusedContainerColor = colors.surface,
            disabledContainerColor = colors.surfaceContainerLow,
            focusedBorderColor = colors.outline,
            unfocusedBorderColor = colors.outlineVariant.copy(alpha = 0.85f),
            disabledBorderColor = colors.outlineVariant.copy(alpha = 0.55f),
            focusedLeadingIconColor = colors.onSurfaceVariant,
            unfocusedLeadingIconColor = colors.onSurfaceVariant,
            focusedTrailingIconColor = colors.onSurfaceVariant,
            unfocusedTrailingIconColor = colors.onSurfaceVariant,
            cursorColor = colors.onSurface
        )
    )
}

@Composable
private fun RunicCardSurface(
    modifier: Modifier,
    shape: Shape,
    containerColor: Color,
    borderColor: Color,
    borderWidth: Dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val border = BorderStroke(width = borderWidth, color = borderColor)

    if (onClick == null) {
        Surface(
            modifier = modifier,
            shape = shape,
            color = containerColor,
            border = border,
            content = content
        )
    } else {
        Surface(
            modifier = modifier,
            onClick = onClick,
            shape = shape,
            color = containerColor,
            border = border,
            content = content
        )
    }
}
