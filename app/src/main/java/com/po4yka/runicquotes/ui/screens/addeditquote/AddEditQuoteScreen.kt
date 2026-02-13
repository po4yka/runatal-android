package com.po4yka.runicquotes.ui.screens.addeditquote

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.po4yka.runicquotes.domain.model.RunicScript
import com.po4yka.runicquotes.domain.model.displayName
import com.po4yka.runicquotes.ui.components.RunicText
import com.po4yka.runicquotes.ui.theme.RunicExpressiveTheme
import com.po4yka.runicquotes.util.rememberHapticFeedback

/**
 * Screen for adding or editing a user-created quote.
 * Features live runic preview as the user types.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditQuoteScreen(
    onNavigateBack: () -> Unit,
    quoteId: Long = 0L,
    viewModel: AddEditQuoteViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptics = rememberHapticFeedback()
    val shapes = RunicExpressiveTheme.shapes
    val motion = RunicExpressiveTheme.motion
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
    var showDiscardDialog by remember { mutableStateOf(false) }
    val saveEnabled = uiState.canSave && !uiState.isSaving

    fun requestExit() {
        if (uiState.hasUnsavedChanges && !uiState.isSaving) {
            showDiscardDialog = true
        } else {
            onNavigateBack()
        }
    }

    BackHandler(enabled = uiState.hasUnsavedChanges && !uiState.isSaving) {
        requestExit()
    }

    LaunchedEffect(quoteId) {
        viewModel.initializeQuoteIfNeeded(quoteId)
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .imePadding(),
        topBar = {
            MediumTopAppBar(
                title = {
                    Text(if (uiState.isEditing) "Edit Quote" else "Add Quote")
                },
                navigationIcon = {
                    IconButton(onClick = { requestExit() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
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
                text = {
                    Text(if (uiState.isSaving) "Saving..." else "Save Quote")
                },
                icon = {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null
                        )
                    }
                },
                onClick = {
                    if (saveEnabled) {
                        haptics.mediumAction()
                        viewModel.saveQuote {
                            haptics.successPattern()
                            onNavigateBack()
                        }
                    }
                },
                expanded = true,
                containerColor = if (saveEnabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                contentColor = if (saveEnabled) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.testTag("add_edit_save_button")
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.22f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = shapes.panel,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (uiState.isEditing) {
                            "Refine your quote and preview the rune rendering live."
                        } else {
                            "Write your quote and tune how it transliterates into runes."
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = if (uiState.hasUnsavedChanges) {
                            "Unsaved changes"
                        } else {
                            "All changes saved"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = if (uiState.hasUnsavedChanges) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            OutlinedTextField(
                value = uiState.textLatin,
                onValueChange = viewModel::updateTextLatin,
                label = { Text("Quote Text") },
                placeholder = { Text("Enter your quote in Latin script...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_edit_quote_text"),
                minLines = 3,
                maxLines = 6,
                isError = uiState.quoteTextError != null,
                supportingText = {
                    val helper = uiState.quoteTextError
                        ?: "Keep it concise and readable in rune form."
                    Text(
                        text = "$helper (${uiState.quoteCharCount}/280)",
                        color = if (uiState.quoteTextError != null) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                },
                enabled = !uiState.isSaving
            )

            OutlinedTextField(
                value = uiState.author,
                onValueChange = viewModel::updateAuthor,
                label = { Text("Author") },
                placeholder = { Text("Enter author name...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_edit_author_text"),
                singleLine = true,
                isError = uiState.authorError != null,
                supportingText = {
                    val helper = uiState.authorError ?: "Who said this quote?"
                    Text(
                        text = "$helper (${uiState.authorCharCount}/60)",
                        color = if (uiState.authorError != null) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                },
                enabled = !uiState.isSaving
            )

            Text(
                text = "Preview Script",
                style = MaterialTheme.typography.titleMedium
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(RunicScript.entries) { script ->
                    FilterChip(
                        selected = uiState.selectedScript == script,
                        onClick = {
                            haptics.lightToggle()
                            viewModel.updateSelectedScript(script)
                        },
                        label = { Text(script.displayName) },
                        enabled = !uiState.isSaving
                    )
                }
            }

            LivePreviewSection(
                uiState = uiState,
                animationDurationMillis = motion.mediumDurationMillis
            )

            Text(
                text = "All Scripts Preview",
                style = MaterialTheme.typography.titleMedium
            )

            ScriptPreviewCard(
                title = "Elder Futhark",
                script = RunicScript.ELDER_FUTHARK,
                runicText = uiState.runicElderPreview,
                selectedFont = uiState.selectedFont
            )
            ScriptPreviewCard(
                title = "Younger Futhark",
                script = RunicScript.YOUNGER_FUTHARK,
                runicText = uiState.runicYoungerPreview,
                selectedFont = uiState.selectedFont
            )
            ScriptPreviewCard(
                title = "Cirth (Angerthas)",
                script = RunicScript.CIRTH,
                runicText = uiState.runicCirthPreview,
                selectedFont = uiState.selectedFont
            )
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard changes?") },
            text = { Text("You have unsaved changes. If you leave now, they will be lost.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        onNavigateBack()
                    }
                ) {
                    Text("Discard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Keep editing")
                }
            }
        )
    }
}

@Composable
private fun LivePreviewSection(
    uiState: AddEditQuoteUiState,
    animationDurationMillis: Int
) {
    val shapes = RunicExpressiveTheme.shapes
    val progress by animateFloatAsState(
        targetValue = uiState.transliterationConfidence / 100f,
        animationSpec = tween(durationMillis = animationDurationMillis),
        label = "transliterationConfidenceProgress"
    )
    val confidenceColor by animateColorAsState(
        targetValue = when {
            uiState.transliterationConfidence >= 90 -> MaterialTheme.colorScheme.primary
            uiState.transliterationConfidence >= 70 -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.error
        },
        animationSpec = tween(durationMillis = animationDurationMillis),
        label = "transliterationConfidenceColor"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shapes.contentCard,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Live Preview",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val previewText = when (uiState.selectedScript) {
                RunicScript.ELDER_FUTHARK -> uiState.runicElderPreview
                RunicScript.YOUNGER_FUTHARK -> uiState.runicYoungerPreview
                RunicScript.CIRTH -> uiState.runicCirthPreview
            }

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
                    text = "ᚱᚢᚾᛖ",
                    script = uiState.selectedScript,
                    font = uiState.selectedFont,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Type to see runic preview...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Transliteration Confidence: ${uiState.transliterationConfidence}%",
                style = MaterialTheme.typography.labelLarge,
                color = confidenceColor
            )
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = confidenceColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Text(
                text = uiState.confidenceHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
private fun ScriptPreviewCard(
    title: String,
    script: RunicScript,
    runicText: String,
    selectedFont: String
) {
    val shapes = RunicExpressiveTheme.shapes

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shapes.collectionCard,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )

            if (runicText.isNotEmpty()) {
                RunicText(
                    text = runicText,
                    script = script,
                    font = selectedFont,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            } else {
                Text(
                    text = "—",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                )
            }
        }
    }
}
