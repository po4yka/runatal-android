package com.po4yka.runicquotes.ui.screens.addeditquote

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.po4yka.runicquotes.domain.model.RunicScript
import com.po4yka.runicquotes.domain.model.displayName
import com.po4yka.runicquotes.ui.components.RunicText
import com.po4yka.runicquotes.ui.components.SegmentedControl
import com.po4yka.runicquotes.ui.theme.RunicExpressiveTheme
import com.po4yka.runicquotes.util.rememberHapticFeedback

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditQuoteScreen(
    onNavigateBack: () -> Unit,
    quoteId: Long = 0L,
    viewModel: AddEditQuoteViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(quoteId) {
        viewModel.initializeQuoteIfNeeded(quoteId)
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    if (uiState.showConfirmation) {
        ConfirmationContent(
            uiState = uiState,
            snackbarHostState = snackbarHostState,
            onViewInLibrary = onNavigateBack,
            onCreateAnother = viewModel::resetForNewQuote
        )
        return
    }

    val requestExit = {
        if (uiState.hasUnsavedChanges && !uiState.isSaving) {
            showDiscardDialog = true
        } else {
            onNavigateBack()
        }
    }

    BackHandler(enabled = uiState.hasUnsavedChanges && !uiState.isSaving) {
        requestExit()
    }

    EditorContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onNavigateBack = { requestExit() },
        onSave = viewModel::saveQuote,
        onUpdateText = viewModel::updateTextLatin,
        onUpdateAuthor = viewModel::updateAuthor,
        onUpdateScript = viewModel::updateSelectedScript,
        onRequestDelete = { showDeleteDialog = true }
    )

    if (showDiscardDialog) {
        DiscardDialog(
            onDiscard = {
                showDiscardDialog = false
                onNavigateBack()
            },
            onDismiss = { showDiscardDialog = false }
        )
    }

    if (showDeleteDialog) {
        DeleteDialog(
            onDelete = {
                showDeleteDialog = false
                viewModel.deleteQuote(onNavigateBack)
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorContent(
    uiState: AddEditQuoteUiState,
    snackbarHostState: SnackbarHostState,
    onNavigateBack: () -> Unit,
    onSave: () -> Unit,
    onUpdateText: (String) -> Unit,
    onUpdateAuthor: (String) -> Unit,
    onUpdateScript: (RunicScript) -> Unit,
    onRequestDelete: () -> Unit
) {
    val haptics = rememberHapticFeedback()
    val saveEnabled = uiState.canSave && !uiState.isSaving

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(if (uiState.isEditing) "Edit Quote" else "Create Quote")
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            haptics.mediumAction()
                            onSave()
                        },
                        enabled = saveEnabled
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Save"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            ScriptSelector(
                selectedScript = uiState.selectedScript,
                onScriptSelected = { script ->
                    haptics.lightToggle()
                    onUpdateScript(script)
                },
                enabled = !uiState.isSaving
            )

            RunicPreviewSection(uiState = uiState)

            QuoteTextField(
                value = uiState.textLatin,
                onValueChange = onUpdateText,
                error = uiState.quoteTextError,
                charCount = uiState.quoteCharCount,
                enabled = !uiState.isSaving
            )

            AuthorTextField(
                value = uiState.author,
                onValueChange = onUpdateAuthor,
                error = uiState.authorError,
                charCount = uiState.authorCharCount,
                enabled = !uiState.isSaving
            )

            Spacer(modifier = Modifier.height(8.dp))

            SaveButton(
                uiState = uiState,
                saveEnabled = saveEnabled,
                onSave = {
                    haptics.mediumAction()
                    onSave()
                }
            )

            if (uiState.isEditing) {
                TextButton(
                    onClick = onRequestDelete,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    enabled = !uiState.isDeleting
                ) {
                    Text("Delete Quote")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SaveButton(
    uiState: AddEditQuoteUiState,
    saveEnabled: Boolean,
    onSave: () -> Unit
) {
    Button(
        onClick = onSave,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("add_edit_save_button"),
        enabled = saveEnabled,
        shape = RunicExpressiveTheme.shapes.segmentedControl
    ) {
        Text(
            text = when {
                uiState.isSaving -> "Saving..."
                uiState.isEditing -> "Save Changes"
                else -> "Create Quote"
            }
        )
    }
}

@Composable
private fun DiscardDialog(
    onDiscard: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Discard changes?") },
        text = { Text("You have unsaved changes. If you leave now, they will be lost.") },
        confirmButton = {
            TextButton(onClick = onDiscard) {
                Text("Discard")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Keep editing")
            }
        }
    )
}

@Composable
private fun DeleteDialog(
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val haptics = rememberHapticFeedback()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete quote?") },
        text = { Text("This action cannot be undone.") },
        confirmButton = {
            TextButton(
                onClick = {
                    haptics.mediumAction()
                    onDelete()
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ScriptSelector(
    selectedScript: RunicScript,
    onScriptSelected: (RunicScript) -> Unit,
    enabled: Boolean
) {
    val scripts = RunicScript.entries
    val segments = scripts.map { it.displayName }
    val selectedIndex = scripts.indexOf(selectedScript)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Preview",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SegmentedControl(
            segments = segments,
            selectedIndex = selectedIndex,
            onSegmentSelected = { index ->
                if (enabled) {
                    onScriptSelected(scripts[index])
                }
            }
        )
    }
}

@Composable
private fun RunicPreviewSection(uiState: AddEditQuoteUiState) {
    val previewText = when (uiState.selectedScript) {
        RunicScript.ELDER_FUTHARK -> uiState.runicElderPreview
        RunicScript.YOUNGER_FUTHARK -> uiState.runicYoungerPreview
        RunicScript.CIRTH -> uiState.runicCirthPreview
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (previewText.isNotEmpty()) {
            RunicText(
                text = previewText,
                script = uiState.selectedScript,
                font = uiState.selectedFont,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            RunicText(
                text = "\u16B1\u16A2\u16BE\u16D6",
                script = uiState.selectedScript,
                font = uiState.selectedFont,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (uiState.textLatin.isNotBlank()) {
            Text(
                text = "\u201C${uiState.textLatin.trim()}\u201D",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (uiState.author.isNotBlank()) {
                Text(
                    text = "\u2014 ${uiState.author.trim()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun QuoteTextField(
    value: String,
    onValueChange: (String) -> Unit,
    error: String?,
    charCount: Int,
    enabled: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "Quote Text",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text("Enter your quote in Latin script...") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("add_edit_quote_text"),
            minLines = 3,
            maxLines = 6,
            isError = error != null,
            supportingText = {
                val helper = error ?: "$charCount/280"
                Text(
                    text = helper,
                    color = if (error != null) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            },
            enabled = enabled
        )
    }
}

@Composable
private fun AuthorTextField(
    value: String,
    onValueChange: (String) -> Unit,
    error: String?,
    charCount: Int,
    enabled: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "Author",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text("Enter author name...") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("add_edit_author_text"),
            singleLine = true,
            isError = error != null,
            supportingText = {
                val helper = error ?: "$charCount/60"
                Text(
                    text = helper,
                    color = if (error != null) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            },
            enabled = enabled
        )
    }
}

@Composable
private fun ConfirmationContent(
    uiState: AddEditQuoteUiState,
    snackbarHostState: SnackbarHostState,
    onViewInLibrary: () -> Unit,
    onCreateAnother: () -> Unit
) {
    val previewText = when (uiState.selectedScript) {
        RunicScript.ELDER_FUTHARK -> uiState.runicElderPreview
        RunicScript.YOUNGER_FUTHARK -> uiState.runicYoungerPreview
        RunicScript.CIRTH -> uiState.runicCirthPreview
    }

    LaunchedEffect(Unit) {
        snackbarHostState.showSnackbar("Quote saved successfully")
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Quote created",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Added to your library and ready to share.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (previewText.isNotEmpty()) {
                RunicText(
                    text = previewText,
                    script = uiState.selectedScript,
                    font = uiState.selectedFont,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "\u2014 ${uiState.author.trim()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onViewInLibrary,
                modifier = Modifier.fillMaxWidth(),
                shape = RunicExpressiveTheme.shapes.segmentedControl
            ) {
                Text("View in Library")
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = onCreateAnother,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Create Another")
            }
        }
    }
}
