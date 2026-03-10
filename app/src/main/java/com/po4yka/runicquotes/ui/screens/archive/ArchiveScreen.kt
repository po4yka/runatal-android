package com.po4yka.runicquotes.ui.screens.archive

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.po4yka.runicquotes.domain.model.ArchivedQuote
import com.po4yka.runicquotes.ui.components.ConfirmationDialog
import com.po4yka.runicquotes.ui.components.EmptyState
import com.po4yka.runicquotes.ui.components.ErrorState
import com.po4yka.runicquotes.ui.components.SegmentedControl
import com.po4yka.runicquotes.ui.components.SkeletonCard
import com.po4yka.runicquotes.ui.components.rememberShimmerBrush
import com.po4yka.runicquotes.ui.theme.RunicExpressiveTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ArchiveScreen(viewModel: ArchiveViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showEmptyTrashDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.snackbarEvent.collect { event ->
            val result = snackbarHostState.showSnackbar(
                message = "Quote restored to library",
                actionLabel = "Undo",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoRestore(event.restoredQuote)
            }
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    shape = RunicExpressiveTheme.shapes.segmentedControl
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            ArchiveHeader()
            ArchiveTabSelector(
                selectedTab = uiState.selectedTab,
                onTabSelected = viewModel::selectTab
            )

            when {
                uiState.isLoading -> ArchiveLoadingSkeleton()
                uiState.errorMessage != null -> ErrorState(
                    title = "Something Went Wrong",
                    description = uiState.errorMessage ?: "An unexpected error occurred.",
                    onRetry = viewModel::retry,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 48.dp)
                )
                else -> ArchiveContent(
                    uiState = uiState,
                    onRestoreQuote = viewModel::restoreQuote,
                    onDeleteQuote = viewModel::softDeleteQuote,
                    onEmptyTrash = { showEmptyTrashDialog = true }
                )
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
private fun ArchiveHeader() {
    Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
        Text(
            text = "Archive",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ArchiveTabSelector(selectedTab: ArchiveTab, onTabSelected: (ArchiveTab) -> Unit) {
    val tabs = listOf("Archived", "Deleted")
    val selectedIndex = if (selectedTab == ArchiveTab.ARCHIVED) 0 else 1

    SegmentedControl(
        segments = tabs,
        selectedIndex = selectedIndex,
        onSegmentSelected = { index ->
            onTabSelected(if (index == 0) ArchiveTab.ARCHIVED else ArchiveTab.DELETED)
        },
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
private fun ArchiveContent(
    uiState: ArchiveUiState,
    onRestoreQuote: (ArchivedQuote) -> Unit,
    onDeleteQuote: (Long) -> Unit,
    onEmptyTrash: () -> Unit
) {
    val quotes = if (uiState.selectedTab == ArchiveTab.ARCHIVED) {
        uiState.archivedQuotes
    } else {
        uiState.deletedQuotes
    }

    if (quotes.isEmpty()) {
        val (title, description) = if (uiState.selectedTab == ArchiveTab.ARCHIVED) {
            "Nothing archived" to "Quotes you archive from the library will appear here."
        } else {
            "No deleted quotes" to "Deleted quotes will appear here for 30 days."
        }
        EmptyState(
            icon = Icons.AutoMirrored.Filled.List,
            title = title,
            description = description,
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 48.dp)
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ArchiveInfoLine(uiState)
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        LazyColumn(
            contentPadding = PaddingValues(vertical = 8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(quotes, key = { it.id }) { quote ->
                ArchiveQuoteItem(
                    quote = quote,
                    isDeletedTab = uiState.selectedTab == ArchiveTab.DELETED,
                    onRestore = { onRestoreQuote(quote) },
                    onDelete = { onDeleteQuote(quote.id) }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
        }

        ArchiveBottomAction(
            selectedTab = uiState.selectedTab,
            onRestoreAll = { /* TODO(po4yka): implement restore all */ },
            onEmptyTrash = onEmptyTrash
        )
    }
}

@Composable
private fun ArchiveInfoLine(uiState: ArchiveUiState) {
    val (count, label) = if (uiState.selectedTab == ArchiveTab.ARCHIVED) {
        uiState.archivedQuotes.size to "archived quotes \u00B7 Won't appear in daily rotation"
    } else {
        uiState.deletedQuotes.size to "recently deleted \u00B7 Permanently removed after 30 days"
    }

    Text(
        text = "$count $label",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun ArchiveQuoteItem(
    quote: ArchivedQuote,
    isDeletedTab: Boolean,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RunicExpressiveTheme.shapes.contentCard,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "\u201C${quote.textLatin}\u201D",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${quote.author} \u00B7 ${formatDate(quote.archivedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                if (isDeletedTab) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete permanently",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    IconButton(onClick = onRestore) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Restore quote",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ArchiveBottomAction(
    selectedTab: ArchiveTab,
    onRestoreAll: () -> Unit,
    onEmptyTrash: () -> Unit
) {
    val (label, action) = if (selectedTab == ArchiveTab.ARCHIVED) {
        "Restore All" to onRestoreAll
    } else {
        "Empty Trash" to onEmptyTrash
    }

    OutlinedButton(
        onClick = action,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RunicExpressiveTheme.shapes.segmentedControl
    ) {
        Text(text = label)
    }
}

@Composable
private fun ArchiveLoadingSkeleton() {
    val brush = rememberShimmerBrush()
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(4) {
            SkeletonCard(height = 100.dp, brush = brush)
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val formatter = SimpleDateFormat("MMM dd", Locale.getDefault())
    return formatter.format(Date(timestamp))
}
