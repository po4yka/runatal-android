package com.po4yka.runicquotes.ui.screens.translation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.po4yka.runicquotes.domain.model.RuneReference
import com.po4yka.runicquotes.domain.model.RunicScript
import com.po4yka.runicquotes.domain.model.segmentLabel
import com.po4yka.runicquotes.ui.components.ErrorState
import com.po4yka.runicquotes.ui.components.SegmentedControl
import com.po4yka.runicquotes.ui.components.SkeletonRect
import com.po4yka.runicquotes.ui.components.rememberShimmerBrush
import com.po4yka.runicquotes.ui.theme.RunicExpressiveTheme

@Composable
fun TranslationScreen(
    viewModel: TranslationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        TranslationHeader()
        TranslationInput(
            text = uiState.inputText,
            onTextChange = viewModel::updateInputText,
            onClear = viewModel::clearInput
        )
        Spacer(modifier = Modifier.height(12.dp))
        ScriptSelector(
            selectedScript = uiState.selectedScript,
            onSelectScript = viewModel::selectScript
        )
        Spacer(modifier = Modifier.height(16.dp))
        val errorMessage = uiState.errorMessage
        if (errorMessage != null) {
            ErrorState(
                title = "Transliteration Failed",
                description = errorMessage,
                onRetry = viewModel::retryTransliteration,
                modifier = Modifier.padding(vertical = 24.dp)
            )
        } else {
            TranslationResult(
                transliteratedText = uiState.transliteratedText,
                scriptName = uiState.scriptDisplayName,
                onCopy = { copyToClipboard(context, uiState.transliteratedText) }
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        RuneGrid(
            runes = uiState.runeCharacters,
            isLoaded = uiState.isRunesLoaded
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun TranslationHeader() {
    Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
        Text(
            text = "Translate",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Transliterate text into runic script",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TranslationInput(
    text: String,
    onTextChange: (String) -> Unit,
    onClear: () -> Unit
) {
    OutlinedTextField(
        value = text,
        onValueChange = onTextChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        placeholder = { Text("Enter text to transliterate...") },
        trailingIcon = {
            if (text.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear input"
                    )
                }
            }
        },
        minLines = 2,
        maxLines = 4,
        shape = RunicExpressiveTheme.shapes.segmentedControl
    )
}

@Composable
private fun ScriptSelector(
    selectedScript: RunicScript,
    onSelectScript: (RunicScript) -> Unit
) {
    val scripts = RunicScript.entries
    SegmentedControl(
        segments = scripts.map { it.segmentLabel },
        selectedIndex = scripts.indexOf(selectedScript),
        onSegmentSelected = { index -> onSelectScript(scripts[index]) },
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

@Composable
private fun TranslationResult(
    transliteratedText: String,
    scriptName: String,
    onCopy: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = "Result",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RunicExpressiveTheme.shapes.contentCard,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            if (transliteratedText.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = transliteratedText,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onCopy,
                        modifier = Modifier.semantics {
                            contentDescription = "Copy transliteration"
                        }
                    ) {
                        Text(
                            text = "\u2398",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = scriptName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, bottom = 12.dp)
                )
            } else {
                Text(
                    text = "Transliterated runes will appear here",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun RuneGrid(runes: List<RuneReference>, isLoaded: Boolean) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = "Rune Characters",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (!isLoaded) {
            RuneGridSkeleton()
        } else if (runes.isNotEmpty()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                contentPadding = PaddingValues(0.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(((runes.size / 4 + 1) * 88).dp)
            ) {
                items(runes, key = { it.id }) { rune ->
                    RuneCell(rune = rune)
                }
            }
        }
    }
}

@Composable
private fun RuneGridSkeleton() {
    val brush = rememberShimmerBrush()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(3) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(4) {
                    SkeletonRect(
                        modifier = Modifier.size(80.dp),
                        height = 80.dp,
                        brush = brush
                    )
                }
            }
        }
    }
}

@Composable
private fun RuneCell(rune: RuneReference) {
    Box(
        modifier = Modifier
            .clip(RunicExpressiveTheme.shapes.contentCard)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .semantics { contentDescription = "${rune.name}: ${rune.pronunciation}" }
            .size(80.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = rune.character,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = rune.name,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Runic transliteration", text))
}
