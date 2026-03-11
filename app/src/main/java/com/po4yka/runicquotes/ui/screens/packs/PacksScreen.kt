package com.po4yka.runicquotes.ui.screens.packs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.po4yka.runicquotes.domain.model.QuotePack
import com.po4yka.runicquotes.ui.components.ErrorState
import com.po4yka.runicquotes.ui.components.RunicChoiceChip
import com.po4yka.runicquotes.ui.components.RunicInfoCard
import com.po4yka.runicquotes.ui.components.RunicSearchField
import com.po4yka.runicquotes.ui.components.RunicTopBar
import com.po4yka.runicquotes.ui.components.RunicTopBarIconAction
import com.po4yka.runicquotes.ui.components.SkeletonCard
import com.po4yka.runicquotes.ui.components.rememberShimmerBrush
import com.po4yka.runicquotes.ui.components.runicChoiceChipColors
import com.po4yka.runicquotes.ui.theme.RunicExpressiveTheme

@Composable
fun PacksScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToPackDetail: (Long) -> Unit = {},
    viewModel: PacksViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { PacksTopBar(onNavigateBack = onNavigateBack) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        when {
            uiState.isLoading -> PacksLoadingSkeleton(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )

            uiState.errorMessage != null -> ErrorState(
                title = "Something Went Wrong",
                description = uiState.errorMessage ?: "An unexpected error occurred.",
                onRetry = viewModel::retry,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(vertical = 48.dp)
            )

            else -> PacksContent(
                uiState = uiState,
                onNavigateToPackDetail = onNavigateToPackDetail,
                onToggleLibrary = viewModel::toggleLibrary,
                onQueryChange = viewModel::updateSearchQuery,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun PacksTopBar(onNavigateBack: () -> Unit) {
    RunicTopBar(
        navigationIcon = {
            RunicTopBarIconAction(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                onClick = onNavigateBack
            )
        },
        titleContent = {}
    )
}

@Composable
private fun PacksContent(
    uiState: PacksUiState,
    onNavigateToPackDetail: (Long) -> Unit,
    onToggleLibrary: (QuotePack) -> Unit,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (uiState.packs.isEmpty()) {
        PacksEmptyState(
            query = uiState.searchQuery,
            onQueryChange = onQueryChange,
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        )
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            PacksHeader()
        }

        item {
            PacksSearchBar(
                query = uiState.searchQuery,
                onQueryChange = onQueryChange
            )
        }

        item {
            PacksStats(
                totalCount = uiState.totalCount,
                libraryCount = uiState.libraryCount
            )
        }

        items(uiState.packs, key = { it.id }) { pack ->
            PackListCard(
                pack = pack,
                onClick = { onNavigateToPackDetail(pack.id) },
                onToggleLibrary = { onToggleLibrary(pack) }
            )
        }
    }
}

@Composable
private fun PacksHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "Quote Packs",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Curated sets of runic wisdom",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PacksSearchBar(query: String, onQueryChange: (String) -> Unit) {
    RunicSearchField(
        query = query,
        onQueryChange = onQueryChange,
        placeholderText = "Search packs...",
        leadingContentDescription = "Search packs"
    )
}

@Composable
private fun PacksStats(totalCount: Int, libraryCount: Int) {
    Text(
        text = "$totalCount packs available · $libraryCount in library",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun PackListCard(
    pack: QuotePack,
    onClick: () -> Unit,
    onToggleLibrary: () -> Unit
) {
    val sourceLabel = PackPresentationCatalog.sourceLabel(pack)

    RunicInfoCard(
        modifier = Modifier.semantics { contentDescription = "Pack: ${pack.name}" },
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 15.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = pack.coverRune,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = pack.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = pack.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MetaPill(text = "${pack.quoteCount} quotes")
                    Text(
                        text = sourceLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            PackLibraryPill(
                isInLibrary = pack.isInLibrary,
                onClick = onToggleLibrary
            )
        }
    }
}

@Composable
private fun PackLibraryPill(
    isInLibrary: Boolean,
    onClick: () -> Unit
) {
    RunicChoiceChip(
        selected = isInLibrary,
        onClick = onClick,
        role = Role.Checkbox,
        shape = RoundedCornerShape(12.dp),
        colors = runicChoiceChipColors(
            selected = isInLibrary,
            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            unselectedContainerColor = MaterialTheme.colorScheme.surface,
            selectedContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedBorderColor = Color.Transparent,
            unselectedBorderColor = MaterialTheme.colorScheme.outlineVariant
        ),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp)
    ) { contentColor ->
        Icon(
            imageVector = if (isInLibrary) Icons.Default.Check else Icons.Default.Add,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = contentColor
        )
        Text(
            text = if (isInLibrary) "Added" else "Add",
            style = MaterialTheme.typography.labelSmall,
            color = contentColor
        )
    }
}

@Composable
private fun MetaPill(text: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun PacksEmptyState(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PacksHeader()

        PacksSearchBar(
            query = query,
            onQueryChange = onQueryChange
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerLow),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = "No packs found",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = if (query.isBlank()) {
                "No packs are available right now."
            } else {
                "No packs match \"$query\". Try a different search or browse all packs."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
    }
}

@Composable
private fun PacksLoadingSkeleton(modifier: Modifier = Modifier) {
    val brush = rememberShimmerBrush()

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SkeletonCard(height = 32.dp, brush = brush, modifier = Modifier.fillMaxWidth(0.48f))
                SkeletonCard(height = 18.dp, brush = brush, modifier = Modifier.fillMaxWidth(0.58f))
            }
        }

        item {
            SkeletonCard(height = 56.dp, brush = brush)
        }

        item {
            SkeletonCard(height = 18.dp, brush = brush, modifier = Modifier.fillMaxWidth(0.42f))
        }

        items(4) {
            SkeletonCard(height = 112.dp, brush = brush)
        }
    }
}
