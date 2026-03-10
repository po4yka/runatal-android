package com.po4yka.runicquotes.ui.screens.references

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.po4yka.runicquotes.domain.model.RuneReference
import com.po4yka.runicquotes.ui.components.EmptyState
import com.po4yka.runicquotes.ui.components.ErrorState
import com.po4yka.runicquotes.ui.components.SegmentedControl
import com.po4yka.runicquotes.ui.components.SkeletonCard
import com.po4yka.runicquotes.ui.components.rememberShimmerBrush
import com.po4yka.runicquotes.ui.theme.RunicExpressiveTheme

@Composable
fun ReferencesScreen(
    onNavigateToRuneDetail: (Long) -> Unit = {},
    viewModel: ReferencesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        ReferencesHeader(runeCount = uiState.runes.size)
        SegmentedControl(
            segments = ScriptTab.entries.map { it.displayName },
            selectedIndex = uiState.selectedTab.ordinal,
            onSegmentSelected = { viewModel.selectTab(ScriptTab.entries[it]) },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        ScriptInfoLine(tab = uiState.selectedTab, count = uiState.runes.size)

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        when {
            uiState.isLoading -> ReferencesLoadingSkeleton()
            uiState.errorMessage != null -> ErrorState(
                title = "Something Went Wrong",
                description = uiState.errorMessage ?: "An unexpected error occurred.",
                onRetry = viewModel::retry,
                modifier = Modifier.fillMaxSize().padding(vertical = 48.dp)
            )
            uiState.runes.isEmpty() -> EmptyState(
                icon = Icons.AutoMirrored.Filled.List,
                title = "No Runes Found",
                description = "Rune references for this script will appear here.",
                modifier = Modifier.fillMaxSize().padding(vertical = 48.dp)
            )
            else -> RuneGrid(
                runes = uiState.runes,
                onRuneClick = onNavigateToRuneDetail
            )
        }
    }
}

@Composable
private fun ReferencesHeader(runeCount: Int) {
    Row(
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "\u16DE",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Reference",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "$runeCount",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ScriptInfoLine(tab: ScriptTab, count: Int) {
    val description = when (tab) {
        ScriptTab.ELDER -> "$count runes \u00B7 2nd\u20138th century \u00B7 Germanic peoples"
        ScriptTab.YOUNGER -> "$count runes \u00B7 9th\u201311th century \u00B7 Viking Age"
        ScriptTab.CIRTH -> "$count runes \u00B7 Tolkien\u2019s Angerthas script"
    }
    Text(
        text = description,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun RuneGrid(runes: List<RuneReference>, onRuneClick: (Long) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(runes, key = { it.id }) { rune ->
            RuneCell(rune = rune, onClick = { onRuneClick(rune.id) })
        }
    }
}

@Composable
private fun RuneCell(rune: RuneReference, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RunicExpressiveTheme.shapes.contentCard)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .heightIn(min = 80.dp)
            .padding(vertical = 8.dp, horizontal = 4.dp)
            .semantics { contentDescription = "${rune.name}: ${rune.pronunciation}" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = rune.character,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = rune.name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = rune.pronunciation,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ReferencesLoadingSkeleton() {
    val brush = rememberShimmerBrush()
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(12) {
            SkeletonCard(height = 80.dp, brush = brush)
        }
    }
}
