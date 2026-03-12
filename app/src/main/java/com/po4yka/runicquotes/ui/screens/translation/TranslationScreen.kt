@file:Suppress("TooManyFunctions")

package com.po4yka.runicquotes.ui.screens.translation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.po4yka.runicquotes.domain.model.RunicScript
import com.po4yka.runicquotes.domain.model.segmentLabel
import com.po4yka.runicquotes.domain.transliteration.WordTransliterationPair
import com.po4yka.runicquotes.domain.translation.TranslationFidelity
import com.po4yka.runicquotes.domain.translation.TranslationMode
import com.po4yka.runicquotes.domain.translation.TranslationProvenanceEntry
import com.po4yka.runicquotes.domain.translation.TranslationResolutionStatus
import com.po4yka.runicquotes.domain.translation.TranslationTokenBreakdown
import com.po4yka.runicquotes.domain.translation.YoungerFutharkVariant
import com.po4yka.runicquotes.domain.translation.label
import com.po4yka.runicquotes.ui.components.RunicActionButton
import com.po4yka.runicquotes.ui.components.RunicActionButtonStyle
import com.po4yka.runicquotes.ui.components.RunicArticleLinkCard
import com.po4yka.runicquotes.ui.components.RunicChoiceChip
import com.po4yka.runicquotes.ui.components.RunicChoiceGroup
import com.po4yka.runicquotes.ui.components.RunicInfoCard
import com.po4yka.runicquotes.ui.components.RunicInputCard
import com.po4yka.runicquotes.ui.components.RunicText
import com.po4yka.runicquotes.ui.components.RunicTopBar
import com.po4yka.runicquotes.ui.components.RunicTopBarActionStyle
import com.po4yka.runicquotes.ui.components.RunicTopBarIconAction
import com.po4yka.runicquotes.ui.components.WordByWordBreakdown
import com.po4yka.runicquotes.ui.components.WordByWordModeToggleChip
import com.po4yka.runicquotes.ui.components.runicActionButtonColors
import com.po4yka.runicquotes.ui.components.runicChoiceChipColors
import com.po4yka.runicquotes.ui.theme.RunicExpressiveTheme
import com.po4yka.runicquotes.ui.theme.RunicTextRole
import com.po4yka.runicquotes.ui.theme.RunicTypeRoles
import com.po4yka.runicquotes.ui.theme.SupportingTextRole
import kotlinx.coroutines.launch

@Composable
internal fun TranslationScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToAccuracyContext: () -> Unit = {},
    viewModel: TranslationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val showHistoricalBreakdown = uiState.wordByWordEnabled &&
        uiState.translationMode == TranslationMode.TRANSLATE &&
        uiState.tokenBreakdown.isNotEmpty()
    val showTransliterationBreakdown = uiState.wordByWordEnabled && uiState.wordBreakdown.isNotEmpty()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is TranslationEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        topBar = {
            TranslationTopBar(onNavigateBack = onNavigateBack)
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (uiState.translateFeatureEnabled) {
                TranslationModeSelector(
                    selectedMode = uiState.translationMode,
                    onSelectMode = viewModel::selectMode
                )
            }

            TranslationScriptSelector(
                selectedScript = uiState.selectedScript,
                translationMode = uiState.translationMode,
                onSelectScript = viewModel::selectScript
            )

            if (uiState.translationMode == TranslationMode.TRANSLATE) {
                TranslationFidelitySelector(
                    selectedFidelity = uiState.selectedFidelity,
                    onSelectFidelity = viewModel::selectFidelity
                )

                if (uiState.selectedScript == RunicScript.YOUNGER_FUTHARK) {
                    YoungerVariantSelector(
                        selectedVariant = uiState.selectedYoungerVariant,
                        onSelectVariant = viewModel::selectYoungerVariant
                    )
                }
            }

            TranslationSectionLabel("English text")

            TranslationInputCard(
                text = uiState.inputText,
                characterCount = uiState.inputCharacterCount,
                focusRequester = focusRequester,
                onTextChange = viewModel::updateInputText
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))

            TranslationOutputHeader(
                title = when {
                    uiState.inputText.isBlank() -> "Runic output"
                    uiState.translationMode == TranslationMode.TRANSLATE ->
                        uiState.translationTrackLabel.ifBlank { "${uiState.scriptDisplayName} translation" }
                    else -> uiState.scriptDisplayName
                },
                hasOutput = uiState.transliteratedText.isNotBlank(),
                wordByWordEnabled = uiState.wordByWordEnabled,
                onToggleWordByWord = viewModel::toggleWordByWordMode,
                onCopy = {
                    copyToClipboard(context, uiState.transliteratedText)
                    scope.launch {
                        snackbarHostState.showSnackbar("Copied to clipboard")
                    }
                }
            )

            TranslationOutputCard(
                outputText = uiState.transliteratedText,
                accessibilityText = uiState.inputText,
                translationMode = uiState.translationMode,
                selectedScript = uiState.selectedScript,
                selectedFont = uiState.selectedFont,
                glyphCount = uiState.outputGlyphCount,
                errorMessage = uiState.errorMessage,
                resolutionStatus = uiState.resolutionStatus,
                translationTrackLabel = uiState.translationTrackLabel,
                unavailableExplanation = uiState.unavailableExplanation
            )

            if (uiState.translationMode == TranslationMode.TRANSLATE) {
                HistoricalTranslationMetaSection(
                    normalizedForm = uiState.normalizedForm,
                    diplomaticForm = uiState.diplomaticForm,
                    confidence = uiState.confidence,
                    notes = uiState.notes,
                    resolutionStatus = uiState.resolutionStatus,
                    unresolvedTokens = uiState.unresolvedTokens,
                    provenance = uiState.provenance,
                    fallbackSuggestion = uiState.fallbackSuggestion,
                    derivationKindLabel = uiState.derivationKindLabel,
                    unavailableExplanation = uiState.unavailableExplanation
                )
            }

            if (showHistoricalBreakdown) {
                TranslationTokenBreakdownSection(
                    tokenBreakdown = uiState.tokenBreakdown,
                    selectedScript = uiState.selectedScript,
                    selectedFont = uiState.selectedFont
                )
            } else if (showTransliterationBreakdown) {
                TranslationWordByWordSection(
                    wordPairs = uiState.wordBreakdown,
                    selectedScript = uiState.selectedScript,
                    selectedFont = uiState.selectedFont
                )
            }

            TranslationActionRow(
                hasInput = uiState.inputText.isNotBlank(),
                isSaving = uiState.isSaving,
                canSave = uiState.canSave,
                onCycleScript = viewModel::cycleScript,
                onFocusInput = {
                    focusRequester.requestFocus()
                },
                onClear = {
                    focusManager.clearFocus(force = true)
                    viewModel.clearInput()
                },
                onSave = {
                    focusManager.clearFocus(force = true)
                    viewModel.saveToLibrary()
                }
            )

            AccuracyContextLink(
                onClick = onNavigateToAccuracyContext
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun TranslationTopBar(onNavigateBack: () -> Unit) {
    RunicTopBar(
        navigationIcon = {
            RunicTopBarIconAction(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                onClick = onNavigateBack
            )
        },
        trailingContent = {
            RunicTopBarIconAction(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                onClick = onNavigateBack,
                style = RunicTopBarActionStyle.Outlined,
                shape = RoundedCornerShape(12.dp)
            )
        },
        titleContent = {
            Text(
                text = "Translate",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() }
            )
        }
    )
}

@Composable
private fun TranslationModeSelector(
    selectedMode: TranslationMode,
    onSelectMode: (TranslationMode) -> Unit
) {
    RunicChoiceGroup(
        modifier = Modifier.fillMaxWidth(),
        expand = true,
        shape = RoundedCornerShape(12.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
        contentPadding = PaddingValues(1.dp)
    ) {
        TranslationMode.entries.forEachIndexed { index, mode ->
            val isSelected = selectedMode == mode
            RunicChoiceChip(
                selected = isSelected,
                onClick = { onSelectMode(mode) },
                modifier = Modifier.weight(1f),
                shape = when (index) {
                    0 -> RoundedCornerShape(topStart = 11.dp, bottomStart = 11.dp)
                    TranslationMode.entries.lastIndex -> RoundedCornerShape(topEnd = 11.dp, bottomEnd = 11.dp)
                    else -> RoundedCornerShape(0.dp)
                },
                colors = runicChoiceChipColors(
                    selected = isSelected,
                    unselectedContainerColor = androidx.compose.ui.graphics.Color.Transparent
                ),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) { contentColor ->
                Text(
                    text = if (mode == TranslationMode.TRANSLITERATE) "Transliterate" else "Translate",
                    style = MaterialTheme.typography.labelMedium,
                    color = contentColor
                )
            }
        }
    }
}

@Composable
private fun TranslationScriptSelector(
    selectedScript: RunicScript,
    translationMode: TranslationMode,
    onSelectScript: (RunicScript) -> Unit
) {
    RunicChoiceGroup(
        modifier = Modifier.fillMaxWidth(),
        expand = true,
        shape = RoundedCornerShape(12.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
        contentPadding = PaddingValues(1.dp)
    ) {
        RunicScript.entries.forEachIndexed { index, script ->
            val isSelected = script == selectedScript

            RunicChoiceChip(
                selected = isSelected,
                onClick = { onSelectScript(script) },
                modifier = Modifier.weight(1f),
                shape = when (index) {
                    0 -> RoundedCornerShape(topStart = 11.dp, bottomStart = 11.dp)
                    RunicScript.entries.lastIndex -> RoundedCornerShape(topEnd = 11.dp, bottomEnd = 11.dp)
                    else -> RoundedCornerShape(0.dp)
                },
                colors = runicChoiceChipColors(
                    selected = isSelected,
                    unselectedContainerColor = androidx.compose.ui.graphics.Color.Transparent
                ),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) { contentColor ->
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = contentColor
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                }
                Text(
                    text = if (translationMode == TranslationMode.TRANSLATE && script == RunicScript.CIRTH) {
                        "Erebor"
                    } else {
                        script.segmentLabel
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = contentColor
                )
            }
        }
    }
}

@Composable
private fun TranslationFidelitySelector(
    selectedFidelity: TranslationFidelity,
    onSelectFidelity: (TranslationFidelity) -> Unit
) {
    TranslationSectionLabel("Historical fidelity")
    RunicChoiceGroup(
        modifier = Modifier.fillMaxWidth(),
        expand = true,
        shape = RoundedCornerShape(12.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
        contentPadding = PaddingValues(1.dp)
    ) {
        TranslationFidelity.entries.forEachIndexed { index, fidelity ->
            val isSelected = selectedFidelity == fidelity
            RunicChoiceChip(
                selected = isSelected,
                onClick = { onSelectFidelity(fidelity) },
                modifier = Modifier.weight(1f),
                shape = when (index) {
                    0 -> RoundedCornerShape(topStart = 11.dp, bottomStart = 11.dp)
                    TranslationFidelity.entries.lastIndex -> RoundedCornerShape(topEnd = 11.dp, bottomEnd = 11.dp)
                    else -> RoundedCornerShape(0.dp)
                },
                colors = runicChoiceChipColors(
                    selected = isSelected,
                    unselectedContainerColor = androidx.compose.ui.graphics.Color.Transparent
                ),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) { contentColor ->
                Text(
                    text = fidelity.name.lowercase().replaceFirstChar(Char::titlecase),
                    style = MaterialTheme.typography.labelMedium,
                    color = contentColor
                )
            }
        }
    }
}

@Composable
private fun YoungerVariantSelector(
    selectedVariant: YoungerFutharkVariant,
    onSelectVariant: (YoungerFutharkVariant) -> Unit
) {
    TranslationSectionLabel("Younger Futhark variant")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        YoungerFutharkVariant.entries.forEach { variant ->
            val isSelected = selectedVariant == variant
            RunicChoiceChip(
                selected = isSelected,
                onClick = { onSelectVariant(variant) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = runicChoiceChipColors(selected = isSelected)
            ) { contentColor ->
                Text(
                    text = if (variant == YoungerFutharkVariant.LONG_BRANCH) "Long-branch" else "Short-twig",
                    style = MaterialTheme.typography.labelMedium,
                    color = contentColor
                )
            }
        }
    }
}

@Composable
private fun TranslationSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .padding(top = 2.dp)
            .semantics { heading() }
    )
}

@Composable
private fun TranslationInputCard(
    text: String,
    characterCount: Int,
    focusRequester: FocusRequester,
    onTextChange: (String) -> Unit
) {
    RunicInputCard(
        borderWidth = 1.5.dp,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = if (text.isBlank()) 126.dp else 104.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .testTag("translation_input_text")
                    .semantics {
                        contentDescription = "English text input"
                        stateDescription = "$characterCount of 280 characters"
                    },
                textStyle = RunicTypeRoles.supporting(
                    SupportingTextRole.FormPlaceholderEmphasis
                ).copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.secondary),
                minLines = 2,
                maxLines = 4,
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                    ) {
                        if (text.isBlank()) {
                            Text(
                                text = "Enter English text to translate…",
                                style = RunicTypeRoles.supporting(
                                    SupportingTextRole.FormPlaceholderEmphasis
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        innerTextField()
                    }
                }
            )

            Text(
                text = "$characterCount / 280",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Composable
private fun TranslationOutputHeader(
    title: String,
    hasOutput: Boolean,
    wordByWordEnabled: Boolean,
    onToggleWordByWord: () -> Unit,
    onCopy: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            WordByWordModeToggleChip(
                selected = wordByWordEnabled,
                enabled = hasOutput,
                onClick = onToggleWordByWord,
                modifier = Modifier.testTag("translation_word_by_word_toggle")
            )

            if (hasOutput) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(role = Role.Button, onClick = onCopy)
                        .padding(horizontal = 2.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Copy",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun TranslationOutputCard(
    outputText: String,
    accessibilityText: String,
    translationMode: TranslationMode,
    selectedScript: RunicScript,
    selectedFont: String,
    glyphCount: Int,
    errorMessage: String?,
    resolutionStatus: TranslationResolutionStatus?,
    translationTrackLabel: String,
    unavailableExplanation: String?
) {
    RunicInfoCard(
        containerColor = MaterialTheme.colorScheme.surface,
        borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)
    ) {
        when {
            errorMessage != null -> Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (translationMode == TranslationMode.TRANSLATE) {
                        "Historical translation failed"
                    } else {
                        "Transliteration failed"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            outputText.isBlank() && resolutionStatus == TranslationResolutionStatus.UNAVAILABLE -> Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${translationTrackLabel.ifBlank { "Historical translation" }} unavailable",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = unavailableExplanation
                        ?: "This strict request does not have a defensible historical " +
                        "result yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            outputText.isBlank() -> Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(316.dp)
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                RunicText(
                    text = "\u16A0\u16A2\u16A6\u16A8\u16B1\u16B2",
                    script = RunicScript.ELDER_FUTHARK,
                    role = RunicTextRole.TranslationPlaceholder,
                    accessibilityText = "Runic translation placeholder",
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = if (translationMode == TranslationMode.TRANSLATE) {
                        "Historical layers and runes will appear here"
                    } else {
                        "Transliteration will appear here"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            else -> Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                RunicText(
                    text = outputText,
                    font = selectedFont,
                    script = selectedScript,
                    role = RunicTextRole.TranslationResult,
                    accessibilityText = accessibilityText,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "$glyphCount glyphs",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoricalTranslationMetaSection(
    normalizedForm: String,
    diplomaticForm: String,
    confidence: Float?,
    notes: List<String>,
    resolutionStatus: TranslationResolutionStatus?,
    unresolvedTokens: List<String>,
    provenance: List<TranslationProvenanceEntry>,
    fallbackSuggestion: String?,
    derivationKindLabel: String,
    unavailableExplanation: String?
) {
    val hasNoMetadata = normalizedForm.isBlank() &&
        diplomaticForm.isBlank() &&
        confidence == null &&
        notes.isEmpty() &&
        resolutionStatus == null &&
        unresolvedTokens.isEmpty() &&
        provenance.isEmpty() &&
        fallbackSuggestion == null &&
        derivationKindLabel.isBlank() &&
        unavailableExplanation == null
    if (hasNoMetadata) {
        return
    }

    RunicInfoCard(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            TranslationStatusRow(
                resolutionStatus = resolutionStatus,
                confidence = confidence,
                derivationKindLabel = derivationKindLabel
            )
            TranslationLayersSection(
                normalizedForm = normalizedForm,
                diplomaticForm = diplomaticForm,
                unresolvedTokens = unresolvedTokens,
                fallbackSuggestion = fallbackSuggestion,
                derivationKindLabel = derivationKindLabel,
                unavailableExplanation = unavailableExplanation
            )
            NotesSection(notes = notes)
            ProvenanceSection(provenance = provenance)
        }
    }
}

@Composable
private fun TranslationStatusRow(
    resolutionStatus: TranslationResolutionStatus?,
    confidence: Float?,
    derivationKindLabel: String
) {
    if (resolutionStatus != null || derivationKindLabel.isNotBlank()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                resolutionStatus?.let { status ->
                    Text(
                        text = status.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (derivationKindLabel.isNotBlank()) {
                    Text(
                        text = derivationKindLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            confidence?.let { value ->
                Text(
                    text = "Confidence ${(value * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TranslationLayersSection(
    normalizedForm: String,
    diplomaticForm: String,
    unresolvedTokens: List<String>,
    fallbackSuggestion: String?,
    derivationKindLabel: String,
    unavailableExplanation: String?
) {
    if (derivationKindLabel.isNotBlank()) {
        TranslationLayerRow(
            title = "Derivation",
            body = derivationKindLabel
        )
    }

    if (normalizedForm.isNotBlank()) {
        TranslationLayerRow(
            title = "Normalized form",
            body = normalizedForm
        )
    }

    if (diplomaticForm.isNotBlank()) {
        TranslationLayerRow(
            title = "Diplomatic form",
            body = diplomaticForm
        )
    }

    if (unresolvedTokens.isNotEmpty()) {
        TranslationLayerRow(
            title = "Unavailable tokens",
            body = unresolvedTokens.joinToString(", ")
        )
    }

    unavailableExplanation?.let { explanation ->
        TranslationLayerRow(
            title = "Why unavailable",
            body = explanation
        )
    }

    fallbackSuggestion?.let { suggestion ->
        TranslationLayerRow(
            title = "Suggested fallback",
            body = suggestion
        )
    }
}

@Composable
private fun NotesSection(notes: List<String>) {
    if (notes.isEmpty()) {
        return
    }

    TranslationBulletSection(
        title = "Notes",
        items = notes
    )
}

@Composable
private fun ProvenanceSection(provenance: List<TranslationProvenanceEntry>) {
    if (provenance.isEmpty()) {
        return
    }

    TranslationBulletSection(
        title = "Why this output?",
        items = provenance.map { entry ->
            buildString {
                append(entry.label)
                append(" — ")
                append(entry.role)
                entry.referenceId?.let {
                    append(" [")
                    append(it)
                    append(']')
                }
                entry.detail?.let {
                    append(" (")
                    append(it)
                    append(')')
                }
            }
        }
    )
}

@Composable
private fun TranslationBulletSection(
    title: String,
    items: List<String>
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        items.forEach { item ->
            Text(
                text = "\u2022 $item",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun TranslationLayerRow(
    title: String,
    body: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun TranslationWordByWordSection(
    wordPairs: List<WordTransliterationPair>,
    selectedScript: RunicScript,
    selectedFont: String
) {
    RunicInfoCard(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f),
        modifier = Modifier.testTag("translation_word_breakdown")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Word by word",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            WordByWordBreakdown(
                wordPairs = wordPairs,
                selectedScript = selectedScript,
                selectedFont = selectedFont,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun TranslationTokenBreakdownSection(
    tokenBreakdown: List<TranslationTokenBreakdown>,
    selectedScript: RunicScript,
    selectedFont: String
) {
    RunicInfoCard(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f),
        modifier = Modifier.testTag("translation_token_breakdown")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Token breakdown",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            tokenBreakdown.forEach { token ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = token.sourceToken,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Status: ${token.resolutionStatus.label}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Normalized: ${token.normalizedToken}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Diplomatic: ${token.diplomaticToken}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    RunicText(
                        text = token.glyphToken,
                        font = selectedFont,
                        script = selectedScript,
                        role = RunicTextRole.TranslationResult,
                        accessibilityText = token.sourceToken,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (token.provenance.isNotEmpty()) {
                        Text(
                            text = "Sources: ${token.provenance.joinToString { provenance -> provenance.label }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TranslationActionRow(
    hasInput: Boolean,
    isSaving: Boolean,
    canSave: Boolean,
    onCycleScript: () -> Unit,
    onFocusInput: () -> Unit,
    onClear: () -> Unit,
    onSave: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val secondaryActionColors = runicActionButtonColors(
            style = RunicActionButtonStyle.Secondary
        )
        val primaryActionColors = if (canSave || !hasInput) {
            runicActionButtonColors(style = RunicActionButtonStyle.Primary)
        } else {
            runicActionButtonColors(style = RunicActionButtonStyle.Tonal)
        }
        val saveLeadingContent: (@Composable BoxScope.() -> Unit)? = if (isSaving) {
            {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = LocalContentColor.current
                )
            }
        } else {
            null
        }

        RunicActionButton(
            label = if (hasInput) "Clear" else "Script",
            modifier = Modifier.weight(0.95f),
            enabled = true,
            onClick = if (hasInput) onClear else onCycleScript,
            colors = secondaryActionColors,
            textStyle = MaterialTheme.typography.titleMedium
        )
        RunicActionButton(
            label = if (hasInput) "Save to library" else "Translate",
            modifier = Modifier.weight(2.05f),
            enabled = if (hasInput) canSave else true,
            onClick = if (hasInput) onSave else onFocusInput,
            colors = primaryActionColors,
            textStyle = MaterialTheme.typography.titleMedium,
            leadingContent = saveLeadingContent
        )
    }
}

@Composable
private fun AccuracyContextLink(onClick: () -> Unit) {
    RunicArticleLinkCard(
        title = "Accuracy & context",
        description = "Modern conventions, limitations, and historical notes",
        onClick = onClick,
        titleColor = MaterialTheme.colorScheme.onSurface
    )
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Runic transliteration", text))
}
