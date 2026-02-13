package com.po4yka.runicquotes.ui.screens.quote

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.po4yka.runicquotes.R
import com.po4yka.runicquotes.ui.components.RunicText

/**
 * Quote screen that displays the daily runic quote.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuoteScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToQuoteList: () -> Unit,
    viewModel: QuoteViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.quote_of_the_day),
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                actions = {
                    // Share button (only show when quote is loaded)
                    if (uiState is QuoteUiState.Success) {
                        IconButton(onClick = { viewModel.shareQuoteAsImage() }) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share quote",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Favorite button (only show when quote is loaded)
                    if (uiState is QuoteUiState.Success) {
                        val isFavorite = (uiState as QuoteUiState.Success).quote.isFavorite
                        IconButton(onClick = { viewModel.toggleFavorite() }) {
                            Icon(
                                imageVector = if (isFavorite) {
                                    Icons.Default.Favorite
                                } else {
                                    Icons.Default.FavoriteBorder
                                },
                                contentDescription = if (isFavorite) {
                                    "Remove from favorites"
                                } else {
                                    "Add to favorites"
                                },
                                tint = if (isFavorite) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }

                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings)
                        )
                    }

                    IconButton(
                        onClick = onNavigateToQuoteList,
                        modifier = Modifier.testTag("quote_browse_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.List,
                            contentDescription = stringResource(R.string.browse_quotes)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState is QuoteUiState.Success) {
                FloatingActionButton(
                    onClick = { viewModel.getRandomQuote() }
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.refresh_quote)
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            when (val state = uiState) {
                is QuoteUiState.Loading -> {
                    CircularProgressIndicator()
                }

                is QuoteUiState.Success -> {
                    QuoteContent(
                        runicText = state.runicText,
                        latinText = state.quote.textLatin,
                        author = state.quote.author,
                        selectedFont = state.selectedFont,
                        showTransliteration = state.showTransliteration
                    )
                }

                is QuoteUiState.Error -> {
                    ErrorContent(message = state.message)
                }

                is QuoteUiState.Empty -> {
                    EmptyContent()
                }
            }
        }
    }
}

/**
 * Content displayed when quote is successfully loaded.
 */
@Composable
private fun QuoteContent(
    runicText: String,
    latinText: String,
    author: String,
    selectedFont: String,
    showTransliteration: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Animated runic text with sequential character fade-in
        AnimatedRunicText(
            text = runicText,
            font = selectedFont,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Latin transliteration (if enabled) with fade-in
        if (showTransliteration) {
            val latinAlpha by animateFloatAsState(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 600,
                    delayMillis = (runicText.length * 50).coerceAtMost(1000)
                ),
                label = "latinAlpha"
            )

            Text(
                text = latinText,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(latinAlpha)
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Author with delayed fade-in
        val authorAlpha by animateFloatAsState(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 500,
                delayMillis = (runicText.length * 50 + 200).coerceAtMost(1200)
            ),
            label = "authorAlpha"
        )

        Text(
            text = "— $author",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.alpha(authorAlpha)
        )
    }
}

/**
 * Runic text with sequential character fade-in animation.
 * Each character fades in one by one for an expressive effect.
 */
@Composable
private fun AnimatedRunicText(
    text: String,
    font: String,
    modifier: Modifier = Modifier
) {
    // Split text into words for better layout
    val words = text.split(" ")

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        words.forEach { word ->
            key(word) {
                Row(
                    modifier = Modifier.wrapContentWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    word.forEachIndexed { index, char ->
                        val alpha = remember { Animatable(0f) }

                        LaunchedEffect(char) {
                            // Stagger animation based on character position
                            val wordStartDelay = words.indexOf(word) * word.length * 30
                            val charDelay = index * 50 + wordStartDelay

                            alpha.animateTo(
                                targetValue = 1f,
                                animationSpec = tween(
                                    durationMillis = 400,
                                    delayMillis = charDelay.coerceAtMost(2000)
                                )
                            )
                        }

                        RunicText(
                            text = char.toString(),
                            font = font,
                            modifier = Modifier.alpha(alpha.value),
                            textAlign = TextAlign.Center,
                            fontSize = 32.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Content displayed when there's an error.
 */
@Composable
private fun ErrorContent(message: String) {
    Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Error",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Content displayed when no quotes are available.
 */
@Composable
private fun EmptyContent() {
    Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No quotes available",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Please check back later",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}
