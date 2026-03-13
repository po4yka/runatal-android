package com.po4yka.runatal.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
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

/** Namespace for shared badge and ornament primitives. */
object RunicBadges

/** Shared pill badge for compact meta labels. */
@Composable
fun RunicBadge(
    text: String,
    modifier: Modifier = Modifier,
    shape: Shape = RunicExpressiveTheme.shapes.pill,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    contentPadding: PaddingValues = PaddingValues(horizontal = 10.dp, vertical = 5.dp)
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = containerColor
    ) {
        Text(
            text = text,
            style = RunicTypeRoles.supporting(SupportingTextRole.CompactMeta),
            color = contentColor,
            modifier = Modifier.padding(contentPadding)
        )
    }
}

/** Shared rounded badge block for runes, icons, or other compact leading ornaments. */
@Composable
fun RunicGlyphBadge(
    modifier: Modifier = Modifier,
    size: Dp = RunicExpressiveTheme.controls.leadingBadgeLarge,
    shape: Shape = RunicExpressiveTheme.shapes.collectionCard,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    border: BorderStroke? = null,
    content: @Composable BoxScope.() -> Unit
) {
    Surface(
        modifier = modifier.size(size),
        shape = shape,
        color = containerColor,
        border = border
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
            content = content
        )
    }
}

/** Shared slim decorative rule used for preview accents and meta separators. */
@Composable
fun RunicOrnamentRule(
    modifier: Modifier = Modifier,
    width: Dp,
    thickness: Dp,
    color: Color,
    shape: Shape = RunicExpressiveTheme.shapes.pill
) {
    Surface(
        modifier = modifier.size(width = width, height = thickness),
        shape = shape,
        color = color
    ) {}
}

/** Shared text row for badges with leading or trailing ornament content. */
@Composable
fun RunicBadgeRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(8.dp),
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit
) {
    Row(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}
