package com.po4yka.runicquotes.ui.screens.references

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.po4yka.runicquotes.domain.model.RuneReference
import com.po4yka.runicquotes.ui.components.RunicArticleCard
import com.po4yka.runicquotes.ui.components.RunicArticleSectionCard
import com.po4yka.runicquotes.ui.components.ErrorState
import com.po4yka.runicquotes.ui.components.RunicTopBar
import com.po4yka.runicquotes.ui.components.RunicTopBarActionStyle
import com.po4yka.runicquotes.ui.components.RunicTopBarIconAction
import com.po4yka.runicquotes.ui.theme.RunicExpressiveTheme

@Composable
fun RuneDetailScreen(
    onNavigateBack: () -> Unit = {},
    runeId: Long = 0L,
    viewModel: RuneDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(runeId) { viewModel.initializeRuneIfNeeded(runeId) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            RuneDetailTopBar(onNavigateBack = onNavigateBack)

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
}

@Composable
private fun RuneDetailTopBar(onNavigateBack: () -> Unit) {
    RunicTopBar(
        navigationIcon = {
            RunicTopBarIconAction(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Navigate back",
                onClick = onNavigateBack,
                style = RunicTopBarActionStyle.Tonal
            )
        },
        titleContent = {
            Text(
                text = "Rune Detail",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    )
}

@Composable
private fun RuneDetailContent(rune: RuneReference) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 10.dp, end = 20.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            RuneHeroCard(rune = rune)
        }
        item {
            RuneDetailSection(
                label = "Meaning",
                text = rune.meaning,
                emphasis = true
            )
        }
        item {
            RuneDetailSection(
                label = "History",
                text = rune.history
            )
        }
        item {
            RuneDetailSection(
                label = "Script",
                text = scriptContext(rune.script)
            )
        }
        item {
            RuneDetailFooter()
        }
    }
}

@Composable
private fun RuneHeroCard(rune: RuneReference) {
    RunicArticleCard(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 22.dp),
        contentGap = 14.dp,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(116.dp)
                .clip(RunicExpressiveTheme.shapes.heroCard)
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .semantics { contentDescription = "Rune character: ${rune.character}" },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = rune.character,
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = rune.name,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = "/${rune.pronunciation}/",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RuneMetaChip(text = formatScriptName(rune.script))
            RuneMetaChip(text = rune.meaning)
        }
    }
}

@Composable
private fun RuneMetaChip(text: String) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
        )
    }
}

@Composable
private fun RuneDetailSection(
    label: String,
    text: String,
    emphasis: Boolean = false
) {
    RunicArticleSectionCard(
        label = label,
        body = text,
        bodyStyle = if (emphasis) {
            MaterialTheme.typography.titleMedium
        } else {
            MaterialTheme.typography.bodyMedium
        }
    )
}

@Composable
private fun RuneDetailFooter() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
        )
        Text(
            text = "\u16A0\u16A2\u16A6\u16A8\u16B1\u16B2",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
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

private fun scriptContext(script: String): String = when (script) {
    "elder_futhark" -> "The oldest canonical rune row, used across early Germanic inscriptions."
    "younger_futhark" -> "The Viking Age simplification of Elder Futhark, compressed into 16 signs."
    "cirth" -> "Tolkien's Cirth tradition, designed as a systematic family of sound-based runes."
    else -> formatScriptName(script)
}

private fun formatScriptName(script: String): String = when (script) {
    "elder_futhark" -> "Elder Futhark"
    "younger_futhark" -> "Younger Futhark"
    "cirth" -> "Cirth"
    else -> script.replaceFirstChar { it.uppercase() }
}
