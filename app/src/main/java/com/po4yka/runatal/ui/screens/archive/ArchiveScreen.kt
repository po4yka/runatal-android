package com.po4yka.runatal.ui.screens.archive

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.po4yka.runatal.domain.model.ArchivedQuote
import com.po4yka.runatal.ui.components.ConfirmationDialog
import com.po4yka.runatal.ui.components.ErrorState
import com.po4yka.runatal.ui.components.RunicTopBar
import com.po4yka.runatal.ui.components.RunicTopBarIconAction
import com.po4yka.runatal.ui.components.SegmentedControl
import com.po4yka.runatal.ui.components.SkeletonCard
import com.po4yka.runatal.ui.components.rememberShimmerBrush
import com.po4yka.runatal.ui.theme.RunicExpressiveTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ArchiveScreen(
    onNavigateBack: () -> Unit,
    viewModel: ArchiveViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showEmptyTrashDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.snackbarEvent.collect { event ->
            val (message, undoPayload) = when (event) {
                is ArchiveSnackbarEvent.RestoredQuote -> {
                    "Quote restored to library" to event.quotes
                }

                is ArchiveSnackbarEvent.RestoredBatch -> {
                    "${event.quotes.size} quotes restored to library" to event.quotes
                }
            }
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = "Undo",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoRestore(undoPayload)
            }
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .navigationBarsPadding()
            ) { data ->
                ArchiveSnackbar(data)
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                ArchiveTopBar(onNavigateBack = onNavigateBack)
                ArchiveTabSelector(
                    selectedTab = uiState.selectedTab,
                    onTabSelected = viewModel::selectTab
                )

                when {
                    uiState.isLoading -> ArchiveLoadingState(uiState.selectedTab)
                    uiState.errorMessage != null -> ErrorState(
                        title = "Couldn’t load archive",
                        description = uiState.errorMessage ?: "An unexpected error occurred.",
                        onRetry = viewModel::retry,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 48.dp)
                    )

                    else -> ArchiveContent(
                        uiState = uiState,
                        onRestoreQuote = viewModel::restoreQuote,
                        onDeleteQuote = viewModel::softDeleteQuote,
                        onRestoreAll = viewModel::restoreAllArchivedQuotes,
                        onEmptyTrash = { showEmptyTrashDialog = true }
                    )
                }
            }

            if (showEmptyTrashDialog) {
                ConfirmationDialog(
                    title = "Empty trash?",
                    message = "All deleted quotes will be permanently removed. This action cannot be undone.",
                    confirmLabel = "Empty Trash",
                    confirmIcon = Icons.Default.Delete,
                    onConfirm = {
                        showEmptyTrashDialog = false
                        viewModel.emptyTrash()
                    },
                    onDismiss = { showEmptyTrashDialog = false }
                )
            }
        }
    }
}

@Composable
private fun ArchiveTopBar(onNavigateBack: () -> Unit) {
    RunicTopBar(
        navigationIcon = {
            RunicTopBarIconAction(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                onClick = onNavigateBack
            )
        },
        titleContent = {
            Text(
                text = "Archive",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() }
            )
        }
    )
}

@Composable
private fun ArchiveTabSelector(selectedTab: ArchiveTab, onTabSelected: (ArchiveTab) -> Unit) {
    val tabs = listOf("Archived", "Hidden", "Deleted")
    val selectedIndex = when (selectedTab) {
        ArchiveTab.ARCHIVED -> 0
        ArchiveTab.HIDDEN -> 1
        ArchiveTab.DELETED -> 2
    }

    SegmentedControl(
        segments = tabs,
        selectedIndex = selectedIndex,
        onSegmentSelected = { index ->
            onTabSelected(
                when (index) {
                    0 -> ArchiveTab.ARCHIVED
                    1 -> ArchiveTab.HIDDEN
                    else -> ArchiveTab.DELETED
                }
            )
        },
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
    )
}

@Composable
private fun ArchiveContent(
    uiState: ArchiveUiState,
    onRestoreQuote: (ArchivedQuote) -> Unit,
    onDeleteQuote: (Long) -> Unit,
    onRestoreAll: () -> Unit,
    onEmptyTrash: () -> Unit
) {
    val quotes = uiState.quotesForSelectedTab
    val showBottomAction = quotes.isNotEmpty() && uiState.selectedTab != ArchiveTab.HIDDEN

    Column(modifier = Modifier.fillMaxSize()) {
        ArchiveInfoRow(uiState)

        if (quotes.isEmpty()) {
            ArchiveEmptyState(
                selectedTab = uiState.selectedTab,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
            )
            return
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 10.dp,
                bottom = if (showBottomAction) 100.dp else 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(quotes, key = { _, quote -> quote.id }) { index, quote ->
                ArchiveQuoteCard(
                    quote = quote,
                    selectedTab = uiState.selectedTab,
                    onRestore = { onRestoreQuote(quote) },
                    onDelete = { onDeleteQuote(quote.id) }
                )
                if (index == 0 && uiState.selectedTab == ArchiveTab.ARCHIVED && quotes.size > 1) {
                    Spacer(modifier = Modifier.height(2.dp))
                }
            }
        }

        if (showBottomAction) {
            ArchiveBottomAction(
                selectedTab = uiState.selectedTab,
                onRestoreAll = onRestoreAll,
                onEmptyTrash = onEmptyTrash
            )
        }
    }
}

@Composable
private fun ArchiveInfoRow(uiState: ArchiveUiState) {
    val (icon, text) = when (uiState.selectedTab) {
        ArchiveTab.ARCHIVED -> Icons.Default.Archive to buildString {
            append(uiState.archivedQuotes.size)
            append(" archived quotes")
            if (uiState.archivedQuotes.isNotEmpty()) {
                append(" · Won't appear in daily rotation")
            }
        }

        ArchiveTab.HIDDEN -> Icons.Default.VisibilityOff to buildString {
            append(uiState.hiddenQuotes.size)
            append(" hidden quotes")
        }

        ArchiveTab.DELETED -> Icons.Default.Delete to buildString {
            append(uiState.deletedQuotes.size)
            append(" recently deleted")
            if (uiState.deletedQuotes.isNotEmpty()) {
                append(" · Permanently removed after 30 days")
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ArchiveQuoteCard(
    quote: ArchivedQuote,
    selectedTab: ArchiveTab,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RunicExpressiveTheme.shapes.contentCard,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(horizontal = 15.dp, vertical = 13.dp)) {
            Text(
                text = "\u201C${quote.textLatin}\u201D",
                style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = buildMetaText(quote, selectedTab),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = if (selectedTab == ArchiveTab.DELETED) onDelete else onRestore,
                    modifier = Modifier.size(RunicExpressiveTheme.controls.minimumTouchTarget)
                ) {
                    Icon(
                        imageVector = if (selectedTab == ArchiveTab.DELETED) {
                            Icons.Default.Delete
                        } else {
                            Icons.Default.Restore
                        },
                        contentDescription = if (selectedTab == ArchiveTab.DELETED) {
                            "Delete permanently"
                        } else {
                            "Restore quote"
                        },
                        modifier = Modifier.size(14.dp),
                        tint = if (selectedTab == ArchiveTab.DELETED) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}

private fun buildMetaText(quote: ArchivedQuote, selectedTab: ArchiveTab): String {
    val base = "${quote.author} · ${formatDate(quote.archivedAt)}"
    return when (selectedTab) {
        ArchiveTab.ARCHIVED -> base
        ArchiveTab.HIDDEN -> "$base · Hidden"
        ArchiveTab.DELETED -> "$base · Pending removal"
    }
}

@Composable
private fun ArchiveBottomAction(
    selectedTab: ArchiveTab,
    onRestoreAll: () -> Unit,
    onEmptyTrash: () -> Unit
) {
    val actionConfig = when (selectedTab) {
        ArchiveTab.ARCHIVED -> ArchiveActionConfig(
            label = "Restore All",
            icon = Icons.Default.Restore,
            onClick = onRestoreAll,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface
            )
        )

        ArchiveTab.DELETED -> ArchiveActionConfig(
            label = "Empty Trash",
            icon = Icons.Default.Delete,
            onClick = onEmptyTrash,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        )

        ArchiveTab.HIDDEN -> return
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .navigationBarsPadding()
    ) {
        Button(
            onClick = actionConfig.onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(RunicExpressiveTheme.controls.minimumTouchTarget),
            shape = RunicExpressiveTheme.shapes.segmentedControl,
            colors = actionConfig.colors
        ) {
            Icon(
                imageVector = actionConfig.icon,
                contentDescription = null,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(text = actionConfig.label)
        }
    }
}

@Composable
private fun ArchiveSnackbar(data: SnackbarData) {
    Snackbar(
        snackbarData = data,
        modifier = Modifier.clip(RunicExpressiveTheme.shapes.segmentedControl),
        containerColor = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        actionColor = MaterialTheme.colorScheme.primary,
        dismissActionContentColor = MaterialTheme.colorScheme.inverseOnSurface
    )
}

@Composable
private fun ArchiveEmptyState(selectedTab: ArchiveTab, modifier: Modifier = Modifier) {
    val (icon, title, description) = when (selectedTab) {
        ArchiveTab.ARCHIVED -> Triple(
            Icons.Default.Archive,
            "Nothing archived",
            "Quotes you archive from the library will appear here."
        )

        ArchiveTab.HIDDEN -> Triple(
            Icons.Default.VisibilityOff,
            "Nothing hidden",
            "Hidden quotes are not available in this build yet."
        )

        ArchiveTab.DELETED -> Triple(
            Icons.Default.Delete,
            "Nothing deleted",
            "Deleted quotes stay here for up to 30 days."
        )
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RunicExpressiveTheme.shapes.segmentedControl)
                .background(MaterialTheme.colorScheme.surfaceContainerLow),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ArchiveLoadingState(selectedTab: ArchiveTab) {
    val brush = rememberShimmerBrush()
    Column(modifier = Modifier.fillMaxSize()) {
        ArchiveInfoRow(
            ArchiveUiState(
                selectedTab = selectedTab,
                archivedQuotes = List(4) { ArchivedQuote(0, 0, "", "", 0L) },
                deletedQuotes = List(2) { ArchivedQuote(0, 0, "", "", 0L, isDeleted = true) }
            )
        )
        ArchiveLoadingSkeleton(
            modifier = Modifier.weight(1f),
            brush = brush
        )
        if (selectedTab != ArchiveTab.HIDDEN) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .navigationBarsPadding()
            ) {
                SkeletonCard(height = 44.dp, brush = brush)
            }
        }
    }
}

@Composable
private fun ArchiveLoadingSkeleton(
    modifier: Modifier = Modifier,
    brush: Brush
) {
    Column(
        modifier = modifier.padding(horizontal = 20.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(5) { index ->
            SkeletonCard(
                height = if (index % 2 == 0) 90.dp else 70.dp,
                brush = brush
            )
        }
    }
}

private data class ArchiveActionConfig(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val colors: ButtonColors
)

private fun formatDate(timestamp: Long): String {
    if (timestamp <= 0L) return "Feb 14"
    val formatter = SimpleDateFormat("MMM d", Locale.getDefault())
    return formatter.format(Date(timestamp))
}
