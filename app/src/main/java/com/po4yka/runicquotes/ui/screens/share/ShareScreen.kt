package com.po4yka.runicquotes.ui.screens.share

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.po4yka.runicquotes.domain.model.Quote
import com.po4yka.runicquotes.ui.components.ErrorState
import com.po4yka.runicquotes.ui.theme.RunicExpressiveTheme
import com.po4yka.runicquotes.util.ShareTemplate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareScreen(
    onNavigateBack: () -> Unit = {},
    quoteId: Long = 0L,
    viewModel: ShareViewModel = hiltViewModel()
) {
    LaunchedEffect(quoteId) {
        viewModel.initializeQuoteIfNeeded(quoteId)
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedTemplate by viewModel.selectedTemplate.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Share Quote") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Navigate back"
                    )
                }
            }
        )

        when (val state = uiState) {
            is ShareUiState.Loading -> ShareLoading()
            is ShareUiState.Error -> ErrorState(
                title = "Something Went Wrong",
                description = state.message,
                onRetry = viewModel::retry,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 48.dp)
            )
            is ShareUiState.Success -> ShareContent(
                quote = state.quote,
                selectedTemplate = selectedTemplate,
                onSelectTemplate = viewModel::selectTemplate,
                onShareAsText = viewModel::shareAsText,
                onShareAsImage = viewModel::shareAsImage
            )
        }
    }
}

@Composable
private fun ShareContent(
    quote: Quote,
    selectedTemplate: ShareTemplate,
    onSelectTemplate: (ShareTemplate) -> Unit,
    onShareAsText: () -> Unit,
    onShareAsImage: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        QuotePreviewCard(quote = quote)

        Spacer(modifier = Modifier.height(24.dp))

        TemplateSelector(
            selectedTemplate = selectedTemplate,
            onSelectTemplate = onSelectTemplate
        )

        Spacer(modifier = Modifier.height(24.dp))

        ShareActions(onShareAsText = onShareAsText, onShareAsImage = onShareAsImage)

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun QuotePreviewCard(quote: Quote) {
    val shapes = RunicExpressiveTheme.shapes
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.contentCard)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val runicText = quote.runicElder ?: quote.runicYounger ?: quote.runicCirth
            if (runicText != null) {
                Text(
                    text = runicText,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            Text(
                text = quote.textLatin,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = quote.author,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun TemplateSelector(
    selectedTemplate: ShareTemplate,
    onSelectTemplate: (ShareTemplate) -> Unit
) {
    Text(
        text = "Template",
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(modifier = Modifier.height(8.dp))
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(end = 16.dp)
    ) {
        items(ShareTemplate.entries.toList()) { template ->
            FilterChip(
                selected = template == selectedTemplate,
                onClick = { onSelectTemplate(template) },
                label = { Text(template.displayName) }
            )
        }
    }
}

@Composable
private fun ShareActions(onShareAsText: () -> Unit, onShareAsImage: () -> Unit) {
    OutlinedButton(
        onClick = onShareAsText,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Send,
            contentDescription = "Share as Text",
            modifier = Modifier.padding(end = 8.dp)
        )
        Text("Share as Text")
    }
    Spacer(modifier = Modifier.height(12.dp))
    Button(
        onClick = onShareAsImage,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Share,
            contentDescription = "Share as Image",
            modifier = Modifier.padding(end = 8.dp)
        )
        Text("Share as Image")
    }
}

@Composable
private fun ShareLoading() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}
