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
import com.po4yka.runicquotes.ui.components.runicActionButtonColors
import com.po4yka.runicquotes.ui.components.runicChoiceChipColors
import com.po4yka.runicquotes.ui.theme.RunicExpressiveTheme
import com.po4yka.runicquotes.ui.theme.RunicTextRole
import com.po4yka.runicquotes.ui.theme.RunicTypeRoles
import com.po4yka.runicquotes.ui.theme.SupportingTextRole
import kotlinx.coroutines.launch

@Composable
fun TranslationScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToAccuracyContext: () -> Unit = {},
    viewModel: TranslationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val feedbackMessage by viewModel.feedbackMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(feedbackMessage) {
        feedbackMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearFeedback()
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
            TranslationScriptSelector(
                selectedScript = uiState.selectedScript,
                onSelectScript = viewModel::selectScript
            )

            TranslationSectionLabel("English text")

            TranslationInputCard(
                text = uiState.inputText,
                characterCount = uiState.inputCharacterCount,
                focusRequester = focusRequester,
                onTextChange = viewModel::updateInputText
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))

            TranslationOutputHeader(
                title = if (uiState.inputText.isBlank()) "Runic output" else uiState.scriptDisplayName,
                hasOutput = uiState.transliteratedText.isNotBlank(),
                onCopy = {
                    copyToClipboard(context, uiState.transliteratedText)
                    scope.launch {
                        snackbarHostState.showSnackbar("Copied to clipboard")
                    }
                }
            )

            TranslationOutputCard(
                outputText = uiState.transliteratedText,
                selectedScript = uiState.selectedScript,
                glyphCount = uiState.outputGlyphCount,
                errorMessage = uiState.errorMessage
            )

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
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    )
}

@Composable
private fun TranslationScriptSelector(
    selectedScript: RunicScript,
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
                    text = script.segmentLabel,
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
        modifier = Modifier.padding(top = 2.dp)
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
                    .focusRequester(focusRequester),
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

        if (hasOutput) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onCopy)
                    .padding(horizontal = 2.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy output",
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

@Composable
private fun TranslationOutputCard(
    outputText: String,
    selectedScript: RunicScript,
    glyphCount: Int,
    errorMessage: String?
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
                    text = "Transliteration failed",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = errorMessage,
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
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Translation will appear here",
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
                    script = selectedScript,
                    role = RunicTextRole.TranslationResult,
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
