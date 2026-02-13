package com.po4yka.runicquotes.ui.screens.quote

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.po4yka.runicquotes.domain.model.RunicScript
import com.po4yka.runicquotes.domain.model.displayName
import com.po4yka.runicquotes.domain.model.getRunicText
import com.po4yka.runicquotes.ui.components.RunicText
import com.po4yka.runicquotes.ui.theme.LocalReduceMotion
import com.po4yka.runicquotes.ui.theme.RunicExpressiveTheme
import com.po4yka.runicquotes.ui.theme.RunicTypeRoles
import com.po4yka.runicquotes.util.ShareTemplate
import com.po4yka.runicquotes.util.rememberHapticFeedback

/**
 * Quote screen that displays the daily runic quote.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuoteScreen(
    viewModel: QuoteViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val reducedMotion = LocalReduceMotion.current
    val haptics = rememberHapticFeedback()
    var showShareDialog by remember { mutableStateOf(false) }
    var selectedShareTemplate by remember { mutableStateOf(ShareTemplate.MINIMAL) }

    Scaffold(
        floatingActionButton = {
            if (uiState is QuoteUiState.Success) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ExtendedFloatingActionButton(
                        onClick = { showShareDialog = true },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share"
                            )
                        },
                        text = { Text("Share") }
                    )
                    ExtendedFloatingActionButton(
                        onClick = { viewModel.getRandomQuote() },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Random"
                            )
                        },
                        text = { Text("Random") }
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
                is QuoteUiState.Loading -> CircularProgressIndicator()
                is QuoteUiState.Success -> {
                    QuoteContent(
                        state = state,
                        reducedMotion = reducedMotion,
                        onToggleFavorite = {
                            haptics.lightToggle()
                            viewModel.toggleFavorite()
                        },
                        onSelectScript = { script ->
                            haptics.lightToggle()
                            viewModel.updateSelectedScript(script)
                        }
                    )
                }

                is QuoteUiState.Error -> ErrorContent(message = state.message)
                is QuoteUiState.Empty -> EmptyContent()
            }
        }
    }

    val successState = uiState as? QuoteUiState.Success
    if (showShareDialog && successState != null) {
        ShareTemplateDialog(
            runicText = successState.runicText,
            latinText = successState.quote.textLatin,
            author = successState.quote.author,
            selectedTemplate = selectedShareTemplate,
            onTemplateSelected = {
                haptics.lightToggle()
                selectedShareTemplate = it
            },
            onDismiss = { showShareDialog = false },
            onShare = {
                haptics.mediumAction()
                viewModel.shareQuoteAsImage(selectedShareTemplate)
                haptics.successPattern()
                showShareDialog = false
            }
        )
    }
}

@Composable
private fun QuoteContent(
    state: QuoteUiState.Success,
    reducedMotion: Boolean,
    onToggleFavorite: () -> Unit,
    onSelectScript: (RunicScript) -> Unit
) {
    val motion = RunicExpressiveTheme.motion
    val shapes = RunicExpressiveTheme.shapes
    val elevations = RunicExpressiveTheme.elevations
    val typeRoles = RunicTypeRoles.current
    var cardVisible by remember(state.quote.id) { mutableStateOf(false) }
    LaunchedEffect(state.quote.id) {
        cardVisible = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Daily Rune",
            style = MaterialTheme.typography.displaySmall
        )
        Text(
            text = "Pick your script and read the same quote in a different voice.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        AnimatedVisibility(
            visible = cardVisible,
            enter = if (reducedMotion) {
                EnterTransition.None
            } else {
                fadeIn(
                    animationSpec = tween(
                        durationMillis = motion.duration(
                            reducedMotion = reducedMotion,
                            base = motion.mediumDurationMillis
                        ),
                        easing = motion.standardEasing
                    )
                ) +
                    slideInVertically(
                        animationSpec = tween(
                            durationMillis = motion.duration(
                                reducedMotion = reducedMotion,
                                base = motion.mediumDurationMillis
                            ),
                            easing = motion.emphasizedEasing
                        ),
                        initialOffsetY = { it / 6 }
                    )
            }
        ) {
            ElevatedCard(
                shape = shapes.heroCard,
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = elevations.raisedCard),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surfaceContainerLow,
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Quote of the Day",
                                style = MaterialTheme.typography.titleLarge
                            )
                            IconButton(onClick = onToggleFavorite) {
                                Icon(
                                    imageVector = if (state.quote.isFavorite) {
                                        Icons.Default.Favorite
                                    } else {
                                        Icons.Default.FavoriteBorder
                                    },
                                    contentDescription = "Favorite",
                                    tint = if (state.quote.isFavorite) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }

                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(RunicScript.entries) { script ->
                                FilterChip(
                                    selected = state.selectedScript == script,
                                    onClick = { onSelectScript(script) },
                                    colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer
                                    ),
                                    label = { Text(script.displayName) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        HeroRunicText(
                            text = state.quote.getRunicText(state.selectedScript),
                            selectedScript = state.selectedScript,
                            selectedFont = state.selectedFont,
                            reducedMotion = reducedMotion
                        )

                        if (state.showTransliteration) {
                            val transliterationAlpha by animateFloatAsState(
                                targetValue = 1f,
                                animationSpec = tween(
                                    durationMillis = motion.duration(
                                        reducedMotion = reducedMotion,
                                        base = motion.mediumDurationMillis
                                    ),
                                    delayMillis = motion.delay(
                                        reducedMotion = reducedMotion,
                                        base = motion.shortDurationMillis
                                    ),
                                    easing = motion.standardEasing
                                ),
                                label = "transliterationAlpha"
                            )
                            Text(
                                text = state.quote.textLatin,
                                style = typeRoles.latinQuote,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 20.dp)
                                    .alpha(transliterationAlpha)
                            )
                        }

                        Text(
                            text = "— ${state.quote.author}",
                            style = typeRoles.quoteMeta,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Runic text with staggered reveal for expressive motion.
 */
@Composable
private fun HeroRunicText(
    text: String,
    selectedScript: RunicScript,
    selectedFont: String,
    reducedMotion: Boolean
) {
    val motion = RunicExpressiveTheme.motion
    val shapes = RunicExpressiveTheme.shapes
    val typeRoles = RunicTypeRoles.current
    val words = text.split(" ")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.panel)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.8f))
            .padding(vertical = 18.dp, horizontal = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        words.forEachIndexed { wordIndex, word ->
            key(wordIndex, word, selectedScript) {
                Row(
                    modifier = Modifier.wrapContentWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    word.forEachIndexed { index, char ->
                        val alpha = remember(word, char, selectedScript) { Animatable(0f) }

                        LaunchedEffect(char, selectedScript) {
                            if (reducedMotion) {
                                alpha.snapTo(1f)
                            } else {
                                val wordStartDelay = wordIndex * word.length * motion.revealStepMillis
                                val charDelay = index * motion.revealStepMillis + wordStartDelay
                                alpha.animateTo(
                                    targetValue = 1f,
                                    animationSpec = tween(
                                        durationMillis = motion.longDurationMillis,
                                        delayMillis = charDelay.coerceAtMost(motion.maxRevealDelayMillis),
                                        easing = motion.standardEasing
                                    )
                                )
                            }
                        }

                        RunicText(
                            text = char.toString(),
                            font = selectedFont,
                            script = selectedScript,
                            modifier = Modifier.alpha(alpha.value),
                            textAlign = TextAlign.Center,
                            style = typeRoles.runicHero,
                            fontSize = when (selectedScript) {
                                RunicScript.ELDER_FUTHARK -> 34.sp
                                RunicScript.YOUNGER_FUTHARK -> 32.sp
                                RunicScript.CIRTH -> 36.sp
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorContent(message: String) {
    Card(
        modifier = Modifier.padding(24.dp),
        shape = RunicExpressiveTheme.shapes.contentCard,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Error",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = message,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun EmptyContent() {
    Text(
        text = "No quotes available",
        style = MaterialTheme.typography.bodyLarge
    )
}

@Composable
private fun ShareTemplateDialog(
    runicText: String,
    latinText: String,
    author: String,
    selectedTemplate: ShareTemplate,
    onTemplateSelected: (ShareTemplate) -> Unit,
    onDismiss: () -> Unit,
    onShare: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Share Style") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ShareTemplate.entries) { template ->
                        FilterChip(
                            selected = selectedTemplate == template,
                            onClick = { onTemplateSelected(template) },
                            label = { Text(template.displayName) }
                        )
                    }
                }
                SharePreviewCard(
                    runicText = runicText,
                    latinText = latinText,
                    author = author,
                    template = selectedTemplate
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onShare) {
                Text("Share Image")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun SharePreviewCard(
    runicText: String,
    latinText: String,
    author: String,
    template: ShareTemplate
) {
    val shapes = RunicExpressiveTheme.shapes
    val typeRoles = RunicTypeRoles.current
    val (backgroundBrush, textColor, authorColor) = when (template) {
        ShareTemplate.MINIMAL -> Triple(
            Brush.verticalGradient(
                listOf(
                    MaterialTheme.colorScheme.surfaceContainerLow,
                    MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ),
            MaterialTheme.colorScheme.onSurface,
            MaterialTheme.colorScheme.onSurfaceVariant
        )

        ShareTemplate.ORNATE -> Triple(
            Brush.verticalGradient(
                listOf(
                    MaterialTheme.colorScheme.tertiaryContainer,
                    MaterialTheme.colorScheme.secondaryContainer
                )
            ),
            MaterialTheme.colorScheme.onTertiaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer
        )

        ShareTemplate.HIGH_CONTRAST -> Triple(
            Brush.verticalGradient(
                listOf(
                    MaterialTheme.colorScheme.inverseSurface,
                    MaterialTheme.colorScheme.inverseSurface
                )
            ),
            MaterialTheme.colorScheme.inverseOnSurface,
            MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.75f)
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shapes.collectionCard
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundBrush)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = runicText.take(80),
                style = typeRoles.runicCard,
                color = textColor
            )
            Text(
                text = latinText.take(90),
                style = typeRoles.latinQuote,
                color = textColor
            )
            Text(
                text = "— $author",
                style = typeRoles.quoteMeta,
                color = authorColor
            )
        }
    }
}
