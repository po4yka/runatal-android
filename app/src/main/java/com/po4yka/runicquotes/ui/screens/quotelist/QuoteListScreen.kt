package com.po4yka.runicquotes.ui.screens.quotelist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.po4yka.runicquotes.domain.model.Quote
import com.po4yka.runicquotes.domain.model.RunicScript
import com.po4yka.runicquotes.domain.model.getRunicText
import com.po4yka.runicquotes.ui.components.RunicText
import com.po4yka.runicquotes.ui.theme.LocalReduceMotion
import com.po4yka.runicquotes.util.rememberHapticFeedback

/**
 * Screen for browsing all quotes with filtering and management.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("CyclomaticComplexMethod")
fun QuoteListScreen(
    onNavigateBack: (() -> Unit)? = null,
    onNavigateToAddQuote: () -> Unit,
    onNavigateToEditQuote: (Long) -> Unit,
    viewModel: QuoteListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val reducedMotion = LocalReduceMotion.current
    val haptics = rememberHapticFeedback()

    // Show error messages
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Browse Quotes") },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    haptics.mediumAction()
                    onNavigateToAddQuote()
                },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("quote_list_add_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Quote"
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "New Quote")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        AnimatedVisibility(
            visible = true,
            enter = if (reducedMotion) {
                EnterTransition.None
            } else {
                fadeIn() + slideInVertically(initialOffsetY = { it / 8 })
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Filter chips
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::updateSearchQuery,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Search quote text or author") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    },
                    singleLine = true
                )

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(QuoteFilter.entries) { filter ->
                        FilterChip(
                            selected = uiState.currentFilter == filter,
                            onClick = {
                                haptics.lightToggle()
                                viewModel.setFilter(filter)
                            },
                            label = { Text(filter.displayName) }
                        )
                    }
                }

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = uiState.selectedAuthor == null,
                            onClick = {
                                haptics.lightToggle()
                                viewModel.updateAuthorFilter(null)
                            },
                            label = { Text("Any Author") }
                        )
                    }
                    items(uiState.availableAuthors) { author ->
                        FilterChip(
                            selected = uiState.selectedAuthor == author,
                            onClick = {
                                haptics.lightToggle()
                                viewModel.updateAuthorFilter(author)
                            },
                            label = { Text(author) }
                        )
                    }
                }

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(QuoteLengthFilter.entries) { lengthFilter ->
                        FilterChip(
                            selected = uiState.lengthFilter == lengthFilter,
                            onClick = {
                                haptics.lightToggle()
                                viewModel.updateLengthFilter(lengthFilter)
                            },
                            label = { Text(lengthFilter.displayName) }
                        )
                    }
                    item {
                        if (viewModel.hasSmartFilters(uiState)) {
                            OutlinedButton(
                                onClick = {
                                    haptics.lightToggle()
                                    viewModel.clearSmartFilters()
                                }
                            ) {
                                Text("Clear smart filters")
                            }
                        }
                    }
                }

                // Quote list
                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (uiState.quotes.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "ᚱᚢᚾᛖ",
                                style = MaterialTheme.typography.displayMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (viewModel.hasSmartFilters(uiState)) {
                                    "No quotes matched your search and smart filters."
                                } else {
                                    when (uiState.currentFilter) {
                                        QuoteFilter.ALL -> "Your quote library is empty for now."
                                        QuoteFilter.FAVORITES -> "No favorites yet. Mark quotes with the heart icon."
                                        QuoteFilter.USER_CREATED -> "You have not created quotes yet."
                                        QuoteFilter.SYSTEM -> "No system quotes available."
                                    }
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )

                            if (viewModel.hasSmartFilters(uiState)) {
                                OutlinedButton(
                                    onClick = {
                                        haptics.lightToggle()
                                        viewModel.clearSmartFilters()
                                    }
                                ) {
                                    Text("Reset Filters")
                                }
                            } else if (uiState.currentFilter == QuoteFilter.USER_CREATED ||
                                uiState.currentFilter == QuoteFilter.ALL
                            ) {
                                Text(
                                    text = "Start your collection with your first custom quote.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Button(
                                    onClick = {
                                        haptics.mediumAction()
                                        onNavigateToAddQuote()
                                    }
                                ) {
                                    Text("Create First Quote")
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("quote_list_lazy"),
                        contentPadding = PaddingValues(16.dp),
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
                                reducedMotion = reducedMotion,
                                onToggleFavorite = {
                                    haptics.lightToggle()
                                    viewModel.toggleFavorite(quote)
                                },
                                onEdit = {
                                    haptics.mediumAction()
                                    onNavigateToEditQuote(quote.id)
                                },
                                onDelete = {
                                    haptics.mediumAction()
                                    viewModel.deleteQuote(quote.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
@Suppress("CyclomaticComplexMethod")
private fun QuoteListItem(
    quote: Quote,
    selectedScript: RunicScript,
    selectedFont: String,
    reducedMotion: Boolean,
    onToggleFavorite: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val elevation by animateDpAsState(
        targetValue = if (reducedMotion) {
            3.dp
        } else if (quote.isUserCreated && isPressed) {
            10.dp
        } else {
            3.dp
        },
        label = "quoteCardElevation"
    )
    val yOffset by animateDpAsState(
        targetValue = if (reducedMotion) {
            0.dp
        } else if (quote.isUserCreated && isPressed) {
            (-2).dp
        } else {
            0.dp
        },
        label = "quoteCardOffset"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = yOffset),
        colors = CardDefaults.cardColors(
            containerColor = if (quote.isUserCreated) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    enabled = quote.isUserCreated,
                    interactionSource = interactionSource,
                    indication = null
                ) { onEdit() }
                .padding(16.dp)
        ) {
            // Runic text
            RunicText(
                text = quote.getRunicText(selectedScript),
                font = selectedFont,
                script = selectedScript,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = FontFamily.Monospace
                ),
                color = if (quote.isUserCreated) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Latin text
            Text(
                text = quote.textLatin,
                style = MaterialTheme.typography.bodyMedium,
                color = if (quote.isUserCreated) {
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Author and actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "— ${quote.author}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (quote.isUserCreated) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    }
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Favorite button
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
                            } else if (quote.isUserCreated) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }

                    // Edit and delete buttons (only for user-created quotes)
                    if (quote.isUserCreated) {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.testTag("quote_list_edit_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        IconButton(onClick = onDelete) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}
