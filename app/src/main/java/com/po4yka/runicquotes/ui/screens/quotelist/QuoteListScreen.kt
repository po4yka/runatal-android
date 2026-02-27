package com.po4yka.runicquotes.ui.screens.quotelist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.po4yka.runicquotes.domain.model.Quote
import com.po4yka.runicquotes.domain.model.RunicScript
import com.po4yka.runicquotes.domain.model.getRunicText
import com.po4yka.runicquotes.domain.transliteration.TransliterationFactory
import com.po4yka.runicquotes.ui.components.RunicText
import com.po4yka.runicquotes.ui.theme.LocalReduceMotion
import com.po4yka.runicquotes.ui.theme.RunicExpressiveTheme
import com.po4yka.runicquotes.ui.theme.RunicTypeRoles
import com.po4yka.runicquotes.util.rememberHapticFeedback
import kotlinx.coroutines.launch

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
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val reducedMotion = LocalReduceMotion.current
    val motion = RunicExpressiveTheme.motion
    val shapes = RunicExpressiveTheme.shapes
    val haptics = rememberHapticFeedback()
    val coroutineScope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    var showAdvancedFilters by rememberSaveable { mutableStateOf(false) }
    var authorSearchQuery by rememberSaveable { mutableStateOf("") }
    var deleteCandidate by remember { mutableStateOf<Quote?>(null) }

    val filteredAuthors = remember(uiState.availableAuthors, authorSearchQuery) {
        uiState.availableAuthors.filter {
            it.contains(authorSearchQuery.trim(), ignoreCase = true)
        }
    }
    val hasSmartFilters = viewModel.hasSmartFilters(uiState)
    val smartFilterCount = remember(
        uiState.searchQuery,
        uiState.selectedAuthor,
        uiState.lengthFilter,
        uiState.currentFilter,
        uiState.selectedCollection
    ) {
        var count = 0
        if (uiState.searchQuery.isNotBlank()) count += 1
        if (uiState.selectedAuthor != null) count += 1
        if (uiState.lengthFilter != QuoteLengthFilter.ANY) count += 1
        if (uiState.currentFilter != QuoteFilter.ALL) count += 1
        if (uiState.selectedCollection != QuoteCollection.ALL) count += 1
        count
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
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
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                scrollBehavior = scrollBehavior
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
                fadeIn(
                    animationSpec = tween(
                        durationMillis = motion.duration(
                            reducedMotion = reducedMotion,
                            base = motion.mediumDurationMillis
                        ),
                        easing = motion.standardEasing
                    )
                ) + slideInVertically(
                    animationSpec = tween(
                        durationMillis = motion.duration(
                            reducedMotion = reducedMotion,
                            base = motion.mediumDurationMillis
                        ),
                        easing = motion.emphasizedEasing
                    ),
                    initialOffsetY = { it / 8 }
                )
            }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.background,
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f),
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
                    .padding(padding)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = shapes.panel,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        tonalElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = uiState.searchQuery,
                                onValueChange = viewModel::updateSearchQuery,
                                modifier = Modifier.fillMaxWidth(),
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
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(QuoteCollection.entries) { collection ->
                                    CollectionCoverCard(
                                        collection = collection,
                                        count = uiState.collectionCounts[collection] ?: 0,
                                        selected = uiState.selectedCollection == collection,
                                        selectedFont = uiState.selectedFont,
                                        reducedMotion = reducedMotion,
                                        onClick = {
                                            haptics.lightToggle()
                                            viewModel.setCollection(collection)
                                        }
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Smart filters",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                TextButton(
                                    onClick = { showAdvancedFilters = !showAdvancedFilters }
                                ) {
                                    Text(
                                        if (showAdvancedFilters) {
                                            "Hide (${smartFilterCount})"
                                        } else {
                                            "Show (${smartFilterCount})"
                                        }
                                    )
                                }
                            }

                            AnimatedVisibility(visible = showAdvancedFilters) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    LazyRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(QuoteFilter.entries) { filter ->
                                            ExpressiveSelectableChip(
                                                selected = uiState.currentFilter == filter,
                                                label = filter.displayName,
                                                onClick = {
                                                    haptics.lightToggle()
                                                    viewModel.setFilter(filter)
                                                }
                                            )
                                        }
                                    }

                                    OutlinedTextField(
                                        value = authorSearchQuery,
                                        onValueChange = { authorSearchQuery = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        placeholder = { Text("Search authors") },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Search,
                                                contentDescription = "Search authors"
                                            )
                                        },
                                        singleLine = true
                                    )

                                    LazyRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        item {
                                            ExpressiveSelectableChip(
                                                selected = uiState.selectedAuthor == null,
                                                label = "Any Author",
                                                onClick = {
                                                    haptics.lightToggle()
                                                    viewModel.updateAuthorFilter(null)
                                                }
                                            )
                                        }
                                        items(filteredAuthors) { author ->
                                            ExpressiveSelectableChip(
                                                selected = uiState.selectedAuthor == author,
                                                label = author,
                                                onClick = {
                                                    haptics.lightToggle()
                                                    viewModel.updateAuthorFilter(author)
                                                }
                                            )
                                        }
                                    }

                                    LazyRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(QuoteLengthFilter.entries) { lengthFilter ->
                                            ExpressiveSelectableChip(
                                                selected = uiState.lengthFilter == lengthFilter,
                                                label = lengthFilter.displayName,
                                                onClick = {
                                                    haptics.lightToggle()
                                                    viewModel.updateLengthFilter(lengthFilter)
                                                }
                                            )
                                        }
                                        if (hasSmartFilters) {
                                            item {
                                                OutlinedButton(
                                                    onClick = {
                                                        haptics.lightToggle()
                                                        viewModel.clearSmartFilters()
                                                        authorSearchQuery = ""
                                                    }
                                                ) {
                                                    Text("Clear Filters")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        } else if (uiState.quotes.isEmpty()) {
                            EmptyQuoteListState(
                                hasSmartFilters = hasSmartFilters,
                                currentFilter = uiState.currentFilter,
                                onResetFilters = {
                                    haptics.lightToggle()
                                    viewModel.clearSmartFilters()
                                    authorSearchQuery = ""
                                },
                                onBrowseAll = {
                                    haptics.lightToggle()
                                    viewModel.setFilter(QuoteFilter.ALL)
                                    viewModel.setCollection(QuoteCollection.ALL)
                                    viewModel.clearSmartFilters()
                                    authorSearchQuery = ""
                                },
                                onCreateQuote = {
                                    haptics.mediumAction()
                                    onNavigateToAddQuote()
                                }
                            )
                        } else {
                            LazyColumn(
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
private fun ExpressiveSelectableChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        leadingIcon = {
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null
                )
            }
        },
        colors = expressiveFilterChipColors(),
        label = {
            Text(
                text = label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    )
}

@Composable
private fun expressiveFilterChipColors() = FilterChipDefaults.filterChipColors(
    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
    labelColor = MaterialTheme.colorScheme.onSurface,
    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
    selectedLeadingIconColor = MaterialTheme.colorScheme.onSecondaryContainer
)

@Composable
private fun CollectionCoverCard(
    collection: QuoteCollection,
    count: Int,
    selected: Boolean,
    selectedFont: String,
    reducedMotion: Boolean,
    onClick: () -> Unit
) {
    val shapes = RunicExpressiveTheme.shapes
    val elevations = RunicExpressiveTheme.elevations
    val motion = RunicExpressiveTheme.motion
    val typeRoles = RunicTypeRoles.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val elevation by animateDpAsState(
        targetValue = when {
            selected -> elevations.raisedCard
            isPressed -> elevations.card
            else -> elevations.flat
        },
        animationSpec = tween(
            durationMillis = motion.duration(
                reducedMotion = reducedMotion,
                base = motion.shortDurationMillis
            ),
            easing = motion.standardEasing
        ),
        label = "collectionCardElevation"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outlineVariant
        },
        animationSpec = tween(
            durationMillis = motion.duration(
                reducedMotion = reducedMotion,
                base = motion.shortDurationMillis
            ),
            easing = motion.standardEasing
        ),
        label = "collectionCardBorder"
    )

    Card(
        shape = shapes.collectionCard,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        modifier = Modifier
            .width(176.dp)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = borderColor,
                shape = shapes.collectionCard
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .testTag("collection_card_${collection.persistedValue}")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = collection.displayName,
                style = typeRoles.runicCollection
            )
            RunicText(
                text = collection.coverRunes,
                font = selectedFont,
                script = RunicScript.ELDER_FUTHARK,
                style = typeRoles.runicCard
            )
            Text(
                text = collection.subtitle,
                style = typeRoles.quoteMeta,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$count quotes",
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

@Composable
private fun EmptyQuoteListState(
    hasSmartFilters: Boolean,
    currentFilter: QuoteFilter,
    onResetFilters: () -> Unit,
    onBrowseAll: () -> Unit,
    onCreateQuote: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 20.dp)
        ) {
            Text(
                text = "ᚱᚢᚾᛖ",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = if (hasSmartFilters) {
                    "No quotes matched your search and smart filters."
                } else {
                    when (currentFilter) {
                        QuoteFilter.ALL -> "Your quote library is empty for now."
                        QuoteFilter.FAVORITES -> "No favorites yet. Mark quotes with the heart icon."
                        QuoteFilter.USER_CREATED -> "You have not created quotes yet."
                        QuoteFilter.SYSTEM -> "No system quotes available."
                    }
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )

            if (hasSmartFilters) {
                OutlinedButton(onClick = onResetFilters) {
                    Text("Reset Filters")
                }
            }

            Button(onClick = onBrowseAll) {
                Text("Browse All Quotes")
            }
            OutlinedButton(onClick = onCreateQuote) {
                Text("Create Quote")
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
    transliterationFactory: TransliterationFactory,
    reducedMotion: Boolean,
    onToggleFavorite: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val shapes = RunicExpressiveTheme.shapes
    val elevations = RunicExpressiveTheme.elevations
    val typeRoles = RunicTypeRoles.current
    val runicText = remember(quote, selectedScript) { quote.getRunicText(selectedScript, transliterationFactory) }
    val elevation = if (reducedMotion) elevations.card else elevations.raisedCard
    val yOffset = 0.dp

    Card(
        shape = shapes.contentCard,
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = yOffset),
        colors = CardDefaults.cardColors(
            containerColor = if (quote.isUserCreated) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
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
                color = if (quote.isUserCreated) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = quote.textLatin,
                style = typeRoles.latinQuote,
                color = if (quote.isUserCreated) {
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
                },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "— ${quote.author}",
                    style = typeRoles.quoteMeta,
                    color = if (quote.isUserCreated) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    }
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
                            } else if (quote.isUserCreated) {
                                MaterialTheme.colorScheme.onPrimaryContainer
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
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
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
