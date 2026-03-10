package com.po4yka.runicquotes.ui.screens.references

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.po4yka.runicquotes.domain.model.RuneReference
import com.po4yka.runicquotes.ui.components.ErrorState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuneDetailScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: RuneDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        RuneDetailTopBar(uiState = uiState, onNavigateBack = onNavigateBack)

        when (val state = uiState) {
            is RuneDetailUiState.Loading -> RuneDetailLoading()
            is RuneDetailUiState.Error -> ErrorState(
                title = "Something Went Wrong",
                description = state.message,
                onRetry = viewModel::retry,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 48.dp)
            )
            is RuneDetailUiState.Success -> RuneDetailContent(rune = state.rune)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RuneDetailTopBar(uiState: RuneDetailUiState, onNavigateBack: () -> Unit) {
    val title = (uiState as? RuneDetailUiState.Success)?.rune?.name ?: "Rune Detail"
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Navigate back"
                )
            }
        }
    )
}

@Composable
private fun RuneDetailContent(rune: RuneReference) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .semantics { contentDescription = "Rune character: ${rune.character}" },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = rune.character,
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = rune.name,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = rune.pronunciation,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        RuneInfoSection(label = "Meaning", text = rune.meaning)
        RuneInfoSection(label = "History", text = rune.history)
        RuneInfoSection(label = "Script", text = formatScriptName(rune.script))

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun RuneInfoSection(label: String, text: String) {
    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = text,
            style = if (label == "Meaning") {
                MaterialTheme.typography.bodyLarge
            } else {
                MaterialTheme.typography.bodyMedium
            },
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun RuneDetailLoading() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.CircularProgressIndicator()
    }
}

private fun formatScriptName(script: String): String = when (script) {
    "elder_futhark" -> "Elder Futhark"
    "younger_futhark" -> "Younger Futhark"
    "cirth" -> "Cirth"
    else -> script.replaceFirstChar { it.uppercase() }
}
