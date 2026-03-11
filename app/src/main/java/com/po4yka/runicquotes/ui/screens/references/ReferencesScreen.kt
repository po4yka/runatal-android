package com.po4yka.runicquotes.ui.screens.references

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.po4yka.runicquotes.domain.model.RuneReference
import com.po4yka.runicquotes.ui.components.RunicArticleAccentCard
import com.po4yka.runicquotes.ui.components.EmptyState
import com.po4yka.runicquotes.ui.components.ErrorState
import com.po4yka.runicquotes.ui.components.RunicSearchField
import com.po4yka.runicquotes.ui.components.RunicTopBar
import com.po4yka.runicquotes.ui.components.RunicTopBarActionStyle
import com.po4yka.runicquotes.ui.components.RunicTopBarIconAction
import com.po4yka.runicquotes.ui.components.SkeletonCard
import com.po4yka.runicquotes.ui.components.rememberShimmerBrush

@Composable
fun ReferencesScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToRuneDetail: (Long) -> Unit = {},
    viewModel: ReferencesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0)) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            ReferencesTopBar(
                isSearchVisible = uiState.isSearchVisible,
                onNavigateBack = onNavigateBack,
                onToggleSearch = viewModel::toggleSearch
            )

            AnimatedVisibility(visible = uiState.isSearchVisible) {
                ReferencesSearchField(
                    query = uiState.searchQuery,
                    onQueryChange = viewModel::updateSearchQuery,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }

            ReferencesContent(
                uiState = uiState,
                onSegmentSelected = viewModel::selectTab,
                onNavigateToRuneDetail = onNavigateToRuneDetail,
                onRetry = viewModel::retry,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun ReferencesTopBar(
    isSearchVisible: Boolean,
    onNavigateBack: () -> Unit,
    onToggleSearch: () -> Unit
) {
    RunicTopBar(
        navigationIcon = {
            RunicTopBarIconAction(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Navigate back",
                onClick = onNavigateBack,
                style = RunicTopBarActionStyle.Tonal
            )
        },
        trailingContent = {
            RunicTopBarIconAction(
                imageVector = if (isSearchVisible) Icons.Default.Close else Icons.Default.Search,
                contentDescription = if (isSearchVisible) "Close search" else "Search runes",
                onClick = onToggleSearch,
                style = RunicTopBarActionStyle.Tonal
            )
        },
        titleContent = {
            Text(
                text = "Rune Reference",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    )
}

@Composable
private fun ReferencesSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    RunicSearchField(
        query = query,
        onQueryChange = onQueryChange,
        modifier = modifier,
        placeholderText = "Search rune, sound, or meaning"
    )
}

@Composable
private fun ReferencesContent(
    uiState: ReferencesUiState,
    onSegmentSelected: (ScriptTab) -> Unit,
    onNavigateToRuneDetail: (Long) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sections = buildRuneSections(uiState.selectedTab, uiState.runes)

    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = modifier,
        contentPadding = PaddingValues(start = 20.dp, top = 10.dp, end = 20.dp, bottom = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            ReferencesScriptSelector(
                selectedTab = uiState.selectedTab,
                onSegmentSelected = onSegmentSelected
            )
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            ScriptInfoCard(tab = uiState.selectedTab, count = uiState.totalRuneCount)
        }

        when {
            uiState.isLoading -> referencesLoadingItems()
            uiState.errorMessage != null -> item(span = { GridItemSpan(maxLineSpan) }) {
                ErrorState(
                    title = "Something Went Wrong",
                    description = uiState.errorMessage,
                    onRetry = onRetry,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 36.dp, bottom = 60.dp)
                )
            }

            uiState.runes.isEmpty() -> item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyState(
                    icon = Icons.Default.Search,
                    title = if (uiState.searchQuery.isBlank()) "No runes found" else "No matching runes",
                    description = if (uiState.searchQuery.isBlank()) {
                        "Rune references for this script will appear here."
                    } else {
                        "Try another name, sound, or meaning."
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 36.dp, bottom = 60.dp)
                )
            }

            else -> {
                sections.forEach { section ->
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        RuneSectionHeader(section = section)
                    }
                    items(section.runes, key = { it.id }) { rune ->
                        RuneCell(
                            rune = rune,
                            onClick = { onNavigateToRuneDetail(rune.id) }
                        )
                    }
                }

                item(span = { GridItemSpan(maxLineSpan) }) {
                    ReferencesFooter()
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.grid.LazyGridScope.referencesLoadingItems() {
    item(span = { GridItemSpan(maxLineSpan) }) {
        Spacer(modifier = Modifier.height(4.dp))
    }
    items(12) {
        val brush = rememberShimmerBrush()
        SkeletonCard(
            height = 76.dp,
            brush = brush
        )
    }
}

@Composable
private fun ReferencesScriptSelector(
    selectedTab: ScriptTab,
    onSegmentSelected: (ScriptTab) -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            ScriptTab.entries.forEach { tab ->
                val isSelected = tab == selectedTab
                val backgroundColor = if (isSelected) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLow
                }
                val contentColor = if (isSelected) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(MaterialTheme.shapes.medium)
                        .background(backgroundColor)
                        .clickable { onSegmentSelected(tab) }
                        .semantics {
                            role = Role.Tab
                            selected = isSelected
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSelected) {
                        Text(
                            text = tab.selectorRune,
                            style = MaterialTheme.typography.labelLarge,
                            color = contentColor
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = tab.displayName,
                        style = MaterialTheme.typography.labelLarge,
                        color = contentColor
                    )
                }
            }
        }
    }
}

@Composable
private fun ScriptInfoCard(tab: ScriptTab, count: Int) {
    val presentation = tab.presentation(count)

    RunicArticleAccentCard(
        title = presentation.title,
        description = presentation.description,
        leadingIcon = Icons.AutoMirrored.Filled.MenuBook
    )
}

@Composable
private fun RuneSectionHeader(section: RuneSection) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.secondary)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = section.title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = countLabel(section.runes.size),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RuneCell(rune: RuneReference, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)),
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp)
            .semantics { contentDescription = "${rune.name}: ${rune.pronunciation}" }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = rune.character,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = rune.pronunciation.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Text(
                text = rune.name,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ReferencesFooter() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
        )
        Text(
            text = "\u16A0\u16A2\u16A6\u16A8\u16B1\u16B2",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
        )
    }
}

private fun ScriptTab.presentation(count: Int): ScriptPresentation = when (this) {
    ScriptTab.ELDER -> ScriptPresentation(
        title = "Elder Futhark",
        description = "$count runes · 2nd-8th century · Germanic peoples. " +
            "Three groups of eight tied to the classic aettir."
    )

    ScriptTab.YOUNGER -> ScriptPresentation(
        title = "Younger Futhark",
        description = "$count runes · 9th-11th century · Viking Age Scandinavia. " +
            "Reduced forms optimized for Norse inscriptions."
    )

    ScriptTab.CIRTH -> ScriptPresentation(
        title = "Cirth",
        description = "$count runes · Literary tradition · Tolkien's Angerthas. " +
            "Consonant-heavy rows paired with a vowel series."
    )
}

private val ScriptTab.selectorRune: String
    get() = when (this) {
        ScriptTab.ELDER -> "\u16A0"
        ScriptTab.YOUNGER -> "\u16A6"
        ScriptTab.CIRTH -> "\u2D59"
    }

private fun buildRuneSections(tab: ScriptTab, runes: List<RuneReference>): List<RuneSection> = when (tab) {
    ScriptTab.ELDER -> listOf(
        RuneSection("Freyr's Aett", runes.take(8)),
        RuneSection("Heimdall's Aett", runes.drop(8).take(8)),
        RuneSection("Tyr's Aett", runes.drop(16))
    )

    ScriptTab.YOUNGER -> listOf(
        RuneSection("Early Sequence", runes.take(8)),
        RuneSection("Later Sequence", runes.drop(8))
    )

    ScriptTab.CIRTH -> {
        val vowels = runes.filter { it.pronunciation.lowercase() in setOf("a", "e", "i", "o", "u") }
        val consonants = runes - vowels.toSet()
        listOf(
            RuneSection("Consonants", consonants),
            RuneSection("Vowels", vowels)
        )
    }
}.filter { it.runes.isNotEmpty() }

private fun countLabel(count: Int): String = if (count == 1) "1 rune" else "$count runes"

private data class ScriptPresentation(
    val title: String,
    val description: String
)

private data class RuneSection(
    val title: String,
    val runes: List<RuneReference>
)
