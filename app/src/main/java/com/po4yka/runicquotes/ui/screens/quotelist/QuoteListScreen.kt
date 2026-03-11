package com.po4yka.runicquotes.ui.screens.quotelist

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.po4yka.runicquotes.domain.model.Quote
import com.po4yka.runicquotes.domain.model.displayName
import com.po4yka.runicquotes.domain.model.getRunicText
import com.po4yka.runicquotes.ui.components.BottomSheetAction
import com.po4yka.runicquotes.ui.components.BottomSheetQuotePreview
import com.po4yka.runicquotes.ui.components.ConfirmationDialog
import com.po4yka.runicquotes.ui.components.EmptyState
import com.po4yka.runicquotes.ui.components.RunicBottomSheet
import com.po4yka.runicquotes.ui.components.RunicSearchField
import com.po4yka.runicquotes.ui.components.SegmentedControl
import com.po4yka.runicquotes.ui.components.SkeletonCard
import com.po4yka.runicquotes.ui.components.rememberShimmerBrush
import com.po4yka.runicquotes.ui.theme.RunicExpressiveTheme
import com.po4yka.runicquotes.util.rememberHapticFeedback
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuoteListScreen(
    onNavigateToAddQuote: () -> Unit,
    onNavigateToEditQuote: (Long) -> Unit,
    onNavigateToShare: (Long) -> Unit = {},
    onNavigateToArchive: () -> Unit = {},
    onNavigateToPacks: () -> Unit = {},
    viewModel: QuoteListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptics = rememberHapticFeedback()
    val coroutineScope = rememberCoroutineScope()
    var deleteCandidate by remember { mutableStateOf<Quote?>(null) }
    var bottomSheetQuote by remember { mutableStateOf<Quote?>(null) }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    val filterSegments = remember { QuoteFilter.entries.map { it.displayName } }
    val filterIcons = remember {
        listOf(
            Icons.AutoMirrored.Filled.MenuBook,
            Icons.Default.Favorite,
            Icons.Default.Edit
        )
    }
    val filterCounts = QuoteFilter.entries.map { uiState.filterCounts[it] ?: 0 }
    val selectedFilterIndex = remember(uiState.currentFilter) {
        QuoteFilter.entries.indexOf(uiState.currentFilter)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LibraryHeader(
                onNavigateToPacks = {
                    haptics.lightToggle()
                    onNavigateToPacks()
                },
                onNavigateToArchive = {
                    haptics.lightToggle()
                    onNavigateToArchive()
                }
            )

            SegmentedControl(
                segments = filterSegments,
                selectedIndex = selectedFilterIndex,
                onSegmentSelected = { index ->
                    haptics.lightToggle()
                    viewModel.setFilter(QuoteFilter.entries[index])
                },
                leadingIcons = filterIcons,
                counts = filterCounts
            )

            RunicSearchField(
                query = uiState.searchQuery,
                onQueryChange = viewModel::updateSearchQuery,
                modifier = Modifier.fillMaxWidth(),
                placeholderText = "Search quotes or authors",
                leadingContentDescription = "Search quotes",
                shape = RunicExpressiveTheme.shapes.segmentedControl
            )

            Text(
                text = "${uiState.quotes.size} quotes",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when {
                    uiState.isLoading -> LibraryLoadingSkeleton()

                    uiState.quotes.isEmpty() -> LibraryEmptyState(
                        currentFilter = uiState.currentFilter,
                        onNavigateToAddQuote = {
                            haptics.mediumAction()
                            onNavigateToAddQuote()
                        },
                        modifier = Modifier.align(Alignment.Center)
                    )

                    else -> LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("quote_list_lazy"),
                        contentPadding = PaddingValues(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = uiState.quotes,
                            key = { it.id }
                        ) { quote ->
                            QuoteListItem(
                                quote = quote,
                                onToggleFavorite = {
                                    haptics.lightToggle()
                                    viewModel.toggleFavorite(quote)
                                },
                                onShowActions = {
                                    haptics.lightToggle()
                                    bottomSheetQuote = quote
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    bottomSheetQuote?.let { quote ->
        LibraryActionsBottomSheet(
            quote = quote,
            selectedScript = uiState.selectedScript,
            selectedFont = uiState.selectedFont,
            transliterationFactory = viewModel.transliterationFactory,
            onDismiss = { bottomSheetQuote = null },
            onToggleFavorite = {
                haptics.lightToggle()
                viewModel.toggleFavorite(quote)
                bottomSheetQuote = null
            },
            onShare = {
                haptics.mediumAction()
                onNavigateToShare(quote.id)
                bottomSheetQuote = null
            },
            onCopyText = {
                haptics.lightToggle()
                viewModel.copyQuoteText(quote)
                bottomSheetQuote = null
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Quote text copied")
                }
            },
            onCopyRunes = {
                haptics.lightToggle()
                viewModel.copyRunicText(quote)
                bottomSheetQuote = null
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Runes copied")
                }
            },
            onEdit = {
                haptics.mediumAction()
                onNavigateToEditQuote(quote.id)
                bottomSheetQuote = null
            },
            onDelete = {
                bottomSheetQuote = null
                deleteCandidate = quote
            }
        )
    }

    deleteCandidate?.let { quote ->
        ConfirmationDialog(
            title = "Delete quote?",
            message = "This quote will be removed from your library. You can't undo this action.",
            confirmLabel = "Delete",
            onConfirm = {
                deleteCandidate = null
                haptics.mediumAction()
                viewModel.deleteQuote(quote.id)
                coroutineScope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = "Quote deleted",
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        haptics.lightToggle()
                        viewModel.restoreDeletedQuote(quote)
                    }
                }
            },
            onDismiss = { deleteCandidate = null },
            isDestructive = true,
            supportingContent = {
                DeleteDialogQuotePreview(
                    text = quote.textLatin,
                    author = quote.author
                )
            }
        )
    }
}

@Composable
private fun LibraryHeader(
    onNavigateToPacks: () -> Unit,
    onNavigateToArchive: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "Library",
                style = MaterialTheme.typography.headlineLarge
            )
            Text(
                text = "Your collection",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = onNavigateToPacks) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text("Packs")
            }
            FilledTonalIconButton(onClick = onNavigateToArchive) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Archive"
                )
            }
        }
    }
}

@Composable
private fun LibraryEmptyState(
    currentFilter: QuoteFilter,
    onNavigateToAddQuote: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (currentFilter) {
        QuoteFilter.ALL -> EmptyState(
            icon = Icons.Default.Star,
            title = "No quotes yet",
            description = "Your library is empty",
            modifier = modifier
        )

        QuoteFilter.FAVORITES -> EmptyState(
            icon = Icons.Default.FavoriteBorder,
            title = "No favorites yet",
            description = "Mark quotes with the heart icon to save them here",
            modifier = modifier
        )

        QuoteFilter.USER_CREATED -> EmptyState(
            icon = Icons.Default.Edit,
            title = "No custom quotes yet",
            description = "Create your first custom quote and see it rendered in ancient runic scripts.",
            primaryActionLabel = "Create Quote",
            onPrimaryAction = onNavigateToAddQuote,
            modifier = modifier
        )
    }
}

@Composable
private fun LibraryLoadingSkeleton() {
    val brush = rememberShimmerBrush()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(4) {
            SkeletonCard(height = 96.dp, brush = brush)
        }
    }
}

@Composable
private fun QuoteListItem(
    quote: Quote,
    onToggleFavorite: () -> Unit,
    onShowActions: () -> Unit
) {
    Card(
        shape = RunicExpressiveTheme.shapes.contentCard,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 4.dp, height = 62.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        if (quote.isFavorite) {
                            MaterialTheme.colorScheme.secondary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                        }
                    )
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "\"${quote.textLatin}\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = quote.author,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (quote.isFavorite) {
                        Text(
                            text = "Saved",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    if (quote.isUserCreated) {
                        Text(
                            text = "Custom",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (quote.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (quote.isFavorite) {
                        "Remove from favorites"
                    } else {
                        "Add to favorites"
                    },
                    tint = if (quote.isFavorite) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            IconButton(onClick = onShowActions) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More actions",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LibraryActionsBottomSheet(
    quote: Quote,
    selectedScript: com.po4yka.runicquotes.domain.model.RunicScript,
    selectedFont: String,
    transliterationFactory: com.po4yka.runicquotes.domain.transliteration.TransliterationFactory,
    onDismiss: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit,
    onCopyText: () -> Unit,
    onCopyRunes: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val actions = buildList {
        add(
            BottomSheetAction(
                icon = if (quote.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                title = if (quote.isFavorite) "Remove from Favorites" else "Add to Favorites",
                subtitle = if (quote.isFavorite) {
                    "Remove this quote from your collection"
                } else {
                    "Save this quote to your favorites"
                },
                onClick = onToggleFavorite
            )
        )
        add(
            BottomSheetAction(
                icon = Icons.Default.Share,
                title = "Share",
                subtitle = "Send or export this quote",
                onClick = onShare
            )
        )
        add(
            BottomSheetAction(
                icon = Icons.Outlined.ContentCopy,
                title = "Copy Text",
                subtitle = "Copy original Latin text",
                onClick = onCopyText
            )
        )
        add(
            BottomSheetAction(
                icon = Icons.Default.TextFields,
                title = "Copy Runes",
                subtitle = "Copy ${selectedScript.displayName} transliteration",
                onClick = onCopyRunes
            )
        )
        if (quote.isUserCreated) {
            add(
                BottomSheetAction(
                    icon = Icons.Default.Edit,
                    title = "Edit Quote",
                    subtitle = "Modify this quote",
                    onClick = onEdit
                )
            )
            add(
                BottomSheetAction(
                    icon = Icons.Default.Delete,
                    title = "Delete Quote",
                    subtitle = "Remove permanently",
                    isDestructive = true,
                    onClick = onDelete
                )
            )
        }
    }

    RunicBottomSheet(
        actions = actions,
        preview = BottomSheetQuotePreview(
            runicText = quote.getRunicText(selectedScript, transliterationFactory),
            author = quote.author,
            font = selectedFont,
            script = selectedScript
        ),
        onDismiss = onDismiss
    )
}

@Composable
private fun DeleteDialogQuotePreview(text: String, author: String) {
    val colors = MaterialTheme.colorScheme

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RunicExpressiveTheme.shapes.contentCard,
        colors = CardDefaults.cardColors(
            containerColor = colors.surfaceContainerHighest.copy(alpha = 0.18f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = colors.outlineVariant.copy(alpha = 0.46f)
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = author,
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant
            )
        }
    }
}
