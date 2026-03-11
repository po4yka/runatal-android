package com.po4yka.runicquotes.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.po4yka.runicquotes.domain.model.RunicScript
import com.po4yka.runicquotes.domain.transliteration.WordTransliterationPair
import com.po4yka.runicquotes.ui.theme.RunicTextRole

/**
 * Renders Latin-to-rune token pairs in a compact wrapped layout.
 */
@Composable
fun WordByWordBreakdown(
    wordPairs: List<WordTransliterationPair>,
    selectedScript: RunicScript,
    selectedFont: String,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        wordPairs.forEach { pair ->
            WordByWordPairChip(
                pair = pair,
                selectedScript = selectedScript,
                selectedFont = selectedFont
            )
        }
    }
}

/**
 * Compact toggle chip for the local word-by-word presentation override.
 */
@Composable
fun WordByWordModeToggleChip(
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    RunicChoiceChip(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.semantics {
            contentDescription = if (selected) {
                "Show full transliteration"
            } else {
                "Show word-by-word transliteration"
            }
        },
        role = Role.Switch,
        stateDescription = toggleStateDescription(selected),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) { contentColor ->
        Text(
            text = "Words",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
            color = contentColor
        )
    }
}

@Composable
private fun WordByWordPairChip(
    pair: WordTransliterationPair,
    selectedScript: RunicScript,
    selectedFont: String
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .semantics(mergeDescendants = true) {
                    contentDescription = "${pair.sourceToken} maps to ${pair.runicToken}"
                }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = pair.sourceToken,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            RunicText(
                text = pair.runicToken,
                font = selectedFont,
                script = selectedScript,
                role = RunicTextRole.Default,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
