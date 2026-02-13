package com.po4yka.runicquotes.ui.screens.addeditquote

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.po4yka.runicquotes.domain.model.RunicScript
import com.po4yka.runicquotes.domain.model.displayName
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

    LaunchedEffect(quoteId) {
        viewModel.initializeQuoteIfNeeded(quoteId)
    }

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
                title = {
                    Text(if (uiState.isEditing) "Edit Quote" else "Add Quote")
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (uiState.canSave && !uiState.isSaving) {
                        haptics.mediumAction()
                        viewModel.saveQuote {
                            haptics.successPattern()
                            onNavigateBack()
                        }
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("add_edit_save_button")
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Save"
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Latin text input
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

            // Author input
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

            Spacer(modifier = Modifier.height(8.dp))

            // Script selector for preview
            Text(
                text = "Preview Script",
                style = MaterialTheme.typography.titleMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RunicScript.entries.forEach { script ->
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

            Spacer(modifier = Modifier.height(8.dp))

            LivePreviewSection(uiState = uiState)

            // All scripts preview
            Text(
                text = "All Scripts Preview",
                style = MaterialTheme.typography.titleMedium
            )

            ScriptPreviewCard(
                title = "Elder Futhark",
                runicText = uiState.runicElderPreview
            )

            ScriptPreviewCard(
                title = "Younger Futhark",
                runicText = uiState.runicYoungerPreview
            )

            ScriptPreviewCard(
                title = "Cirth (Angerthas)",
                runicText = uiState.runicCirthPreview
            )

            // Bottom padding for FAB
            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}

@Composable
private fun LivePreviewSection(uiState: AddEditQuoteUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
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
                Text(
                    text = previewText,
                    style = MaterialTheme.typography.headlineMedium,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(
                    text = "ᚱᚢᚾᛖ",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
                Text(
                    text = "Type to see runic preview...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Transliteration Confidence: ${uiState.transliterationConfidence}%",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LinearProgressIndicator(
                progress = { uiState.transliterationConfidence / 100f },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = uiState.confidenceHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
private fun ScriptPreviewCard(
    title: String,
    runicText: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            if (runicText.isNotEmpty()) {
                Text(
                    text = runicText,
                    style = MaterialTheme.typography.bodyLarge,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface
                )
            } else {
                Text(
                    text = "—",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }
        }
    }
}
