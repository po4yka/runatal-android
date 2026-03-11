@file:Suppress("TooManyFunctions")

package com.po4yka.runicquotes.ui.screens.addeditquote

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.po4yka.runicquotes.domain.model.RunicScript
import com.po4yka.runicquotes.ui.components.ConfirmationDialog
import com.po4yka.runicquotes.ui.components.RunicInfoCard
import com.po4yka.runicquotes.ui.components.RunicInputCard
import com.po4yka.runicquotes.ui.components.RunicText
import com.po4yka.runicquotes.ui.components.RunicTopBar
import com.po4yka.runicquotes.ui.components.RunicTopBarActionStyle
import com.po4yka.runicquotes.ui.components.RunicTopBarIconAction
import com.po4yka.runicquotes.ui.theme.RunicExpressiveTheme
import com.po4yka.runicquotes.ui.theme.RunicTextRole
import com.po4yka.runicquotes.util.rememberHapticFeedback
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

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
        onNavigateBack = requestExit,
        onSave = { viewModel.saveQuote(onNavigateBack) },
        onUpdateText = viewModel::updateTextLatin,
        onUpdateAuthor = viewModel::updateAuthor,
        onUpdateScript = viewModel::updateSelectedScript,
        onRequestDelete = { showDeleteDialog = true }
    )

    if (showDiscardDialog) {
        DiscardDialog(
            quoteText = uiState.textLatin,
            author = uiState.author,
            onDiscard = {
                showDiscardDialog = false
                onNavigateBack()
            },
            onDismiss = { showDiscardDialog = false }
        )
    }

    if (showDeleteDialog) {
        DeleteDialog(
            quoteText = uiState.textLatin,
            author = uiState.author,
            onDelete = {
                showDeleteDialog = false
                viewModel.deleteQuote(onNavigateBack)
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

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
            EditorTopBar(
                uiState = uiState,
                saveEnabled = saveEnabled,
                onNavigateBack = onNavigateBack,
                onSave = {
                    haptics.mediumAction()
                    onSave()
                }
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            EditorPreviewCard(
                uiState = uiState,
                onScriptSelected = { script ->
                    haptics.lightToggle()
                    onUpdateScript(script)
                },
                enabled = !uiState.isSaving
            )

            QuoteTextField(
                value = uiState.textLatin,
                onValueChange = onUpdateText,
                error = uiState.quoteTextError,
                enabled = !uiState.isSaving
            )

            AuthorTextField(
                value = uiState.author,
                onValueChange = onUpdateAuthor,
                error = uiState.authorError,
                enabled = !uiState.isSaving
            )

            SaveButton(
                uiState = uiState,
                saveEnabled = saveEnabled,
                onSave = {
                    haptics.mediumAction()
                    onSave()
                }
            )

            if (uiState.isEditing) {
                DeleteQuoteButton(
                    enabled = !uiState.isDeleting,
                    onDelete = {
                        haptics.mediumAction()
                        onRequestDelete()
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun EditorTopBar(
    uiState: AddEditQuoteUiState,
    saveEnabled: Boolean,
    onNavigateBack: () -> Unit,
    onSave: () -> Unit
) {
    val title = if (uiState.isEditing) "Edit Quote" else "Create Quote"

    RunicTopBar(
        navigationIcon = {
            RunicTopBarIconAction(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                onClick = onNavigateBack,
                style = RunicTopBarActionStyle.Surface
            )
        },
        trailingContent = {
            FilledIconButton(
                onClick = onSave,
                enabled = saveEnabled,
                modifier = Modifier.size(RunicExpressiveTheme.controls.minimumTouchTarget),
                shape = RoundedCornerShape(12.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Save"
                )
            }
        },
        titleContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
            if (uiState.isEditing) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = "Custom",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                        )
                    }
                    Text(
                        text = formatCreatedAt(uiState.createdAtMillis),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    )
}

@Composable
private fun EditorPreviewCard(
    uiState: AddEditQuoteUiState,
    onScriptSelected: (RunicScript) -> Unit,
    enabled: Boolean
) {
    val previewText = previewTextFor(uiState)
    val hasText = uiState.textLatin.isNotBlank()
    val charsLabel = if (hasText) {
        val runeCount = previewText.count { !it.isWhitespace() }
        "${uiState.textLatin.trim().length} chars · $runeCount runes"
    } else {
        null
    }

    RunicInfoCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 12.dp, top = 12.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Preview",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
                CompactScriptSelector(
                    selectedScript = uiState.selectedScript,
                    onScriptSelected = onScriptSelected,
                    enabled = enabled
                )
            }

            RunicText(
                text = if (previewText.isNotBlank()) previewText else "\u16BA\u16C1\u16BC\u16C0",
                script = uiState.selectedScript,
                font = uiState.selectedFont,
                role = RunicTextRole.EditorPreview,
                color = if (previewText.isNotBlank()) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            HorizontalDivider(
                modifier = Modifier.padding(top = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 11.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (hasText) {
                    Text(
                        text = "\u201C${uiState.textLatin.trim()}\u201D",
                        style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = "Your live preview appears here as you type.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (uiState.author.isNotBlank()) {
                            "\u2014 ${uiState.author.trim()}"
                        } else {
                            "\u2014 Add an author"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (charsLabel != null) {
                        Text(
                            text = charsLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactScriptSelector(
    selectedScript: RunicScript,
    onScriptSelected: (RunicScript) -> Unit,
    enabled: Boolean
) {
    val controls = RunicExpressiveTheme.controls
    val scripts = RunicScript.entries

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
        )
    ) {
        Row(
            modifier = Modifier.padding(1.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            scripts.forEach { script ->
                val selected = script == selectedScript
                val label = when (script) {
                    RunicScript.ELDER_FUTHARK -> "Elder"
                    RunicScript.YOUNGER_FUTHARK -> "Younger"
                    RunicScript.CIRTH -> "Cirth"
                }

                Surface(
                    modifier = Modifier.heightIn(min = controls.minimumTouchTarget),
                    shape = RoundedCornerShape(10.dp),
                    color = if (selected) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    onClick = {
                        if (enabled) {
                            onScriptSelected(script)
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (selected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(11.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuoteTextField(
    value: String,
    onValueChange: (String) -> Unit,
    error: String?,
    enabled: Boolean
) {
    EditorTextField(
        label = "Quote",
        value = value,
        onValueChange = onValueChange,
        placeholder = "Enter your quote in Latin script...",
        error = error,
        enabled = enabled,
        singleLine = false,
        minLines = 3,
        maxLines = 5,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            keyboardType = KeyboardType.Text
        ),
        modifier = Modifier.testTag("add_edit_quote_text")
    )
}

@Composable
private fun AuthorTextField(
    value: String,
    onValueChange: (String) -> Unit,
    error: String?,
    enabled: Boolean
) {
    EditorTextField(
        label = "Author",
        value = value,
        onValueChange = onValueChange,
        placeholder = "Enter author name...",
        error = error,
        enabled = enabled,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Words,
            keyboardType = KeyboardType.Text
        ),
        modifier = Modifier.testTag("add_edit_author_text")
    )
}

@Composable
private fun EditorTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    error: String?,
    enabled: Boolean,
    singleLine: Boolean,
    minLines: Int = 1,
    maxLines: Int = 1,
    keyboardOptions: KeyboardOptions,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(3.dp)
                    .background(
                        color = if (error != null) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        },
                        shape = RoundedCornerShape(2.dp)
                    )
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (error != null) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            if (error != null) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }

        RunicInputCard(
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            isError = error != null,
            borderColor = if (error != null) {
                MaterialTheme.colorScheme.error.copy(alpha = 0.88f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.9f)
            },
            contentPadding = PaddingValues(0.dp)
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = modifier.fillMaxWidth(),
                enabled = enabled,
                singleLine = singleLine,
                minLines = minLines,
                maxLines = maxLines,
                isError = error != null,
                shape = RoundedCornerShape(14.dp),
                textStyle = MaterialTheme.typography.bodyMedium,
                placeholder = {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                keyboardOptions = keyboardOptions,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    errorContainerColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    disabledBorderColor = Color.Transparent,
                    errorBorderColor = Color.Transparent,
                    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    cursorColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }

        if (error != null) {
            Text(
                text = error,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 4.dp)
            )
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
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Text(
            text = when {
                uiState.isSaving -> "Saving..."
                uiState.isEditing -> "Save Changes"
                else -> "Create Quote"
            },
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
private fun DeleteQuoteButton(
    enabled: Boolean,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.18f),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f)
        ),
        onClick = onDelete
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = if (enabled) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = "Delete Quote",
                style = MaterialTheme.typography.labelLarge,
                color = if (enabled) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
private fun DiscardDialog(
    quoteText: String,
    author: String,
    onDiscard: () -> Unit,
    onDismiss: () -> Unit
) {
    ConfirmationDialog(
        title = "Discard changes?",
        message = "Your latest edits haven't been saved yet.",
        confirmLabel = "Discard",
        dismissLabel = "Keep editing",
        onConfirm = onDiscard,
        onDismiss = onDismiss,
        isDestructive = true,
        supportingContent = {
            AddEditDialogPreview(
                text = quoteText,
                author = author.ifBlank { "Unknown" }
            )
        }
    )
}

@Composable
private fun DeleteDialog(
    quoteText: String,
    author: String,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    ConfirmationDialog(
        title = "Delete this quote?",
        message = "This removes the quote from your library immediately.",
        confirmLabel = "Delete",
        onConfirm = onDelete,
        onDismiss = onDismiss,
        isDestructive = true,
        supportingContent = {
            AddEditDialogPreview(
                text = quoteText,
                author = author.ifBlank { "Unknown" }
            )
        }
    )
}

@Composable
private fun AddEditDialogPreview(text: String, author: String) {
    val colors = MaterialTheme.colorScheme

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = colors.surfaceContainerHighest.copy(alpha = 0.18f),
        border = BorderStroke(
            width = 1.dp,
            color = colors.outlineVariant.copy(alpha = 0.46f)
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(
                text = text.ifBlank { "Untitled quote" },
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

@Composable
private fun ConfirmationContent(
    uiState: AddEditQuoteUiState,
    onViewInLibrary: () -> Unit,
    onCreateAnother: () -> Unit
) {
    val previewText = previewTextFor(uiState)

    Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0)) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    modifier = Modifier.size(64.dp),
                    shape = RunicExpressiveTheme.shapes.pill,
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }

                Text(
                    text = "Quote created",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Added to your library and ready to share.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 19.dp, vertical = 17.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        RunicText(
                            text = previewText,
                            script = uiState.selectedScript,
                            font = uiState.selectedFont,
                            role = RunicTextRole.EditorConfirmation,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "\u2014 ${uiState.author.trim()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onViewInLibrary,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    )
                ) {
                    Text("View in Library")
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    onClick = onCreateAnother
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 13.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Create Another",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.inverseSurface
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Quote saved successfully",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.inverseOnSurface
                    )
                    Spacer(modifier = Modifier.size(18.dp))
                    Text(
                        text = "Undo",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.82f)
                    )
                }
            }
        }
    }
}

private fun previewTextFor(uiState: AddEditQuoteUiState): String {
    return when (uiState.selectedScript) {
        RunicScript.ELDER_FUTHARK -> uiState.runicElderPreview
        RunicScript.YOUNGER_FUTHARK -> uiState.runicYoungerPreview
        RunicScript.CIRTH -> uiState.runicCirthPreview
    }
}

private fun formatCreatedAt(createdAtMillis: Long): String {
    if (createdAtMillis == 0L) {
        return ""
    }
    return DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH)
        .format(
            Instant.ofEpochMilli(createdAtMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
        )
}
