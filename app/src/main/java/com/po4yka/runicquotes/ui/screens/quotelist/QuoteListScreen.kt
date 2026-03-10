package com.po4yka.runicquotes.ui.screens.quotelist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.po4yka.runicquotes.domain.model.Quote
import com.po4yka.runicquotes.domain.model.RunicScript
import com.po4yka.runicquotes.domain.model.getRunicText
import com.po4yka.runicquotes.ui.components.EmptyState
import com.po4yka.runicquotes.ui.components.RunicText
import com.po4yka.runicquotes.ui.components.SegmentedControl
import com.po4yka.runicquotes.ui.components.SkeletonCard
import com.po4yka.runicquotes.ui.components.rememberShimmerBrush
import com.po4yka.runicquotes.ui.theme.RunicExpressiveTheme
import com.po4yka.runicquotes.ui.theme.RunicTypeRoles
import com.po4yka.runicquotes.util.rememberHapticFeedback
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuoteListScreen(
    onNavigateToAddQuote: () -> Unit,
    onNavigateToEditQuote: (Long) -> Unit,
    viewModel: QuoteListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptics = rememberHapticFeedback()
    val coroutineScope = rememberCoroutineScope()
    var deleteCandidate by remember { mutableStateOf<Quote?>(null) }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    val filterSegments = remember { QuoteFilter.entries.map { it.displayName } }
    val selectedFilterIndex = remember(uiState.currentFilter) {
        QuoteFilter.entries.indexOf(uiState.currentFilter)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Library") }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SegmentedControl(
                    segments = filterSegments,
                    selectedIndex = selectedFilterIndex,
                    onSegmentSelected = { index ->
                        haptics.lightToggle()
                        viewModel.setFilter(QuoteFilter.entries[index])
                    }
                )

                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::updateSearchQuery,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search quotes or authors") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    },
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

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
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = uiState.quotes,
                            key = { it.id }
                        ) { quote ->
                            QuoteListItem(
                                quote = quote,
                                selectedScript = uiState.selectedScript,
                                selectedFont = uiState.selectedFont,
                                transliterationFactory = viewModel.transliterationFactory,
                                onToggleFavorite = {
                                    haptics.lightToggle()
                                    viewModel.toggleFavorite(quote)
                                },
                                onEdit = {
                                    haptics.mediumAction()
                                    onNavigateToEditQuote(quote.id)
                                },
                                onDelete = {
                                    haptics.lightToggle()
                                    deleteCandidate = quote
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (deleteCandidate != null) {
        val quoteToDelete = deleteCandidate!!
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text("Delete quote?") },
            text = {
                Text(
                    text = "\"${quoteToDelete.textLatin}\" by ${quoteToDelete.author}",
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteCandidate = null
                        haptics.mediumAction()
                        viewModel.deleteQuote(quoteToDelete.id)
                        coroutineScope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = "Quote deleted",
                                actionLabel = "Undo",
                                duration = SnackbarDuration.Short
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                haptics.lightToggle()
                                viewModel.restoreDeletedQuote(quoteToDelete)
                            }
                        }
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidate = null }) {
                    Text("Cancel")
                }
            }
        )
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
            title = "No custom quotes",
            description = "Create your own quotes to see them here",
            primaryActionLabel = "Create Quote",
            onPrimaryAction = onNavigateToAddQuote,
            modifier = modifier
        )
    }
}

@Composable
private fun LibraryLoadingSkeleton() {
    val brush = rememberShimmerBrush()

    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(4) {
            SkeletonCard(height = 120.dp, brush = brush)
        }
    }
}

@Composable
private fun QuoteListItem(
    quote: Quote,
    selectedScript: RunicScript,
    selectedFont: String,
    transliterationFactory: com.po4yka.runicquotes.domain.transliteration.TransliterationFactory,
    onToggleFavorite: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val shapes = RunicExpressiveTheme.shapes
    val typeRoles = RunicTypeRoles.current
    val runicText = remember(quote, selectedScript) {
        quote.getRunicText(selectedScript, transliterationFactory)
    }

    Card(
        shape = shapes.contentCard,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            RunicText(
                text = runicText,
                font = selectedFont,
                script = selectedScript,
                style = typeRoles.runicCard,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = quote.textLatin,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "\u2014 ${quote.author}",
                    style = typeRoles.quoteMeta,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            imageVector = if (quote.isFavorite) {
                                Icons.Default.Favorite
                            } else {
                                Icons.Default.FavoriteBorder
                            },
                            contentDescription = if (quote.isFavorite) {
                                "Remove from favorites"
                            } else {
                                "Add to favorites"
                            },
                            tint = if (quote.isFavorite) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }

                    if (quote.isUserCreated) {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.testTag("quote_list_edit_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit quote",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = onDelete) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete quote",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}
