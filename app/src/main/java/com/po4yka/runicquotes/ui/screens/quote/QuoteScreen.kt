package com.po4yka.runicquotes.ui.screens.quote

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.po4yka.runicquotes.domain.model.RunicScript
import com.po4yka.runicquotes.domain.model.displayName
import com.po4yka.runicquotes.domain.model.segmentLabel
import com.po4yka.runicquotes.ui.components.BottomSheetAction
import com.po4yka.runicquotes.ui.components.CoachMarkStep
import com.po4yka.runicquotes.ui.components.CoachMarksDialog
import com.po4yka.runicquotes.ui.components.EmptyState
import com.po4yka.runicquotes.ui.components.ErrorState
import com.po4yka.runicquotes.ui.components.NotificationPermissionDialog
import com.po4yka.runicquotes.ui.components.RunicBottomSheet
import com.po4yka.runicquotes.ui.components.SegmentedControl
import com.po4yka.runicquotes.ui.components.RunicText
import com.po4yka.runicquotes.ui.components.SkeletonCard
import com.po4yka.runicquotes.ui.components.SkeletonRect
import com.po4yka.runicquotes.ui.components.SkeletonTextBlock
import com.po4yka.runicquotes.ui.components.rememberShimmerBrush
import com.po4yka.runicquotes.ui.theme.LocalReduceMotion
import com.po4yka.runicquotes.ui.theme.RunicExpressiveTheme
import com.po4yka.runicquotes.ui.theme.RunicTypeRoles
import com.po4yka.runicquotes.util.rememberHapticFeedback
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Redesigned Today screen matching Figma 14:2296.
 *
 * Layout: date header, segmented script control, hero quote card,
 * action buttons (save/share/shuffle), recent quotes, history link.
 */
@Composable
fun QuoteScreen(
    onNavigateToHistory: () -> Unit = {},
    onNavigateToShare: (Long) -> Unit = {},
    onBrowseLibrary: (() -> Unit)? = null,
    viewModel: QuoteViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val haptics = rememberHapticFeedback()

    Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0)) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is QuoteUiState.Loading -> TodayLoadingSkeleton(
                    modifier = Modifier.fillMaxSize()
                )

                is QuoteUiState.Success -> TodayContent(
                    state = state,
                    onToggleFavorite = {
                        haptics.lightToggle()
                        viewModel.toggleFavorite()
                    },
                    onShare = {
                        haptics.mediumAction()
                        onNavigateToShare(state.quote.id)
                    },
                    onNewQuote = {
                        haptics.mediumAction()
                        viewModel.getRandomQuote()
                    },
                    onToggleTransliteration = {
                        haptics.lightToggle()
                        viewModel.toggleTransliterationVisibility()
                    },
                    onSelectScript = { script ->
                        haptics.lightToggle()
                        viewModel.updateSelectedScript(script)
                    },
                    onNavigateToHistory = onNavigateToHistory
                )

                is QuoteUiState.Error -> ErrorState(
                    title = "Something Went Wrong",
                    description = state.message,
                    onRetry = {
                        haptics.lightToggle()
                        viewModel.refreshQuote()
                    },
                    modifier = Modifier.align(Alignment.Center)
                )

                is QuoteUiState.Empty -> EmptyState(
                    icon = Icons.Default.Star,
                    title = "No Quotes Yet",
                    description = "Try loading a random quote or browse the library.",
                    primaryActionLabel = "Random Quote",
                    onPrimaryAction = {
                        haptics.mediumAction()
                        viewModel.getRandomQuote()
                    },
                    secondaryActionLabel = if (onBrowseLibrary != null) "Open Library" else null,
                    onSecondaryAction = onBrowseLibrary,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
private fun TodayContent(
    state: QuoteUiState.Success,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit,
    onNewQuote: () -> Unit,
    onToggleTransliteration: () -> Unit,
    onSelectScript: (RunicScript) -> Unit,
    onNavigateToHistory: () -> Unit
) {
    val motion = RunicExpressiveTheme.motion
    val reducedMotion = LocalReduceMotion.current
    val scrollState = rememberScrollState()
    var cardVisible by remember(state.quote.id) { mutableStateOf(false) }
    LaunchedEffect(state.quote.id) { cardVisible = true }

    val todayDate = remember {
        LocalDate.now().format(
            DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.ENGLISH)
        )
    }

    val scripts = remember { RunicScript.entries }
    val selectedScriptIndex = remember(state.selectedScript) {
        scripts.indexOf(state.selectedScript)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = todayDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Quote of the Day",
                    style = MaterialTheme.typography.headlineLarge
                )
            }
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RunicExpressiveTheme.shapes.pill,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                onClick = onToggleTransliteration
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (state.showTransliteration) {
                            Icons.Default.Visibility
                        } else {
                            Icons.Default.VisibilityOff
                        },
                        contentDescription = if (state.showTransliteration) {
                            "Hide transliteration"
                        } else {
                            "Show transliteration"
                        },
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        SegmentedControl(
            segments = scripts.map { it.segmentLabel },
            selectedIndex = selectedScriptIndex,
            onSegmentSelected = { index -> onSelectScript(scripts[index]) }
        )

        Spacer(modifier = Modifier.height(14.dp))

        AnimatedVisibility(
            visible = cardVisible,
            enter = if (reducedMotion) {
                EnterTransition.None
            } else {
                fadeIn(
                    animationSpec = tween(
                        durationMillis = motion.duration(reducedMotion, motion.mediumDurationMillis),
                        easing = motion.standardEasing
                    )
                ) + slideInVertically(
                    animationSpec = tween(
                        durationMillis = motion.duration(reducedMotion, motion.mediumDurationMillis),
                        easing = motion.emphasizedEasing
                    ),
                    initialOffsetY = { it / 6 }
                )
            }
        ) {
            HeroQuoteCard(
                runicText = state.runicText,
                latinText = state.quote.textLatin,
                author = state.quote.author,
                scriptLabel = state.selectedScript.displayName,
                selectedScript = state.selectedScript,
                selectedFont = state.selectedFont,
                showTransliteration = state.showTransliteration,
                reducedMotion = reducedMotion
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        ActionButtonsRow(
            isFavorite = state.quote.isFavorite,
            onToggleFavorite = onToggleFavorite,
            onShare = onShare,
            onNewQuote = onNewQuote
        )

        if (state.recentQuotes.isNotEmpty()) {
            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = "Recent quotes",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            state.recentQuotes.forEach { item ->
                RecentQuoteCard(
                    runicText = item.runicText,
                    latinText = item.quote.textLatin,
                    author = item.quote.author,
                    isFavorite = item.quote.isFavorite
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            HistoryLink(onClick = onNavigateToHistory)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun HeroQuoteCard(
    runicText: String,
    latinText: String,
    author: String,
    scriptLabel: String,
    selectedScript: RunicScript,
    selectedFont: String,
    showTransliteration: Boolean,
    reducedMotion: Boolean
) {
    val shapes = RunicExpressiveTheme.shapes
    val motion = RunicExpressiveTheme.motion
    val typeRoles = RunicTypeRoles.current

    Card(
        shape = shapes.contentCard,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 21.dp, vertical = 23.dp)
        ) {
            HeroRunicText(
                text = runicText,
                selectedScript = selectedScript,
                selectedFont = selectedFont,
                reducedMotion = reducedMotion
            )

            if (showTransliteration) {
                val transliterationAlpha by animateFloatAsState(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = motion.duration(reducedMotion, motion.mediumDurationMillis),
                        delayMillis = motion.delay(reducedMotion, motion.shortDurationMillis),
                        easing = motion.standardEasing
                    ),
                    label = "transliterationAlpha"
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "\"$latinText\"",
                    style = typeRoles.latinQuote,
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(transliterationAlpha)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "— $author",
                    style = typeRoles.quoteMeta,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = scriptLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Runic text with staggered per-character reveal animation.
 */
@Composable
private fun HeroRunicText(
    text: String,
    selectedScript: RunicScript,
    selectedFont: String,
    reducedMotion: Boolean
) {
    val motion = RunicExpressiveTheme.motion
    val typeRoles = RunicTypeRoles.current
    val scriptFontSize = when (selectedScript) {
        RunicScript.ELDER_FUTHARK -> 34.sp
        RunicScript.YOUNGER_FUTHARK -> 32.sp
        RunicScript.CIRTH -> 36.sp
    }

    if (reducedMotion) {
        RunicText(
            text = text,
            font = selectedFont,
            script = selectedScript,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start,
            style = typeRoles.runicHero,
            fontSize = scriptFontSize
        )
        return
    }

    val words = remember(text) { text.split(" ") }

    Column(
        modifier = Modifier.fillMaxWidth(),
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

                        RunicText(
                            text = char.toString(),
                            font = selectedFont,
                            script = selectedScript,
                            modifier = Modifier.alpha(alpha.value),
                            textAlign = TextAlign.Start,
                            style = typeRoles.runicHero,
                            fontSize = scriptFontSize
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionButtonsRow(
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit,
    onNewQuote: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            shape = RunicExpressiveTheme.shapes.segment,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            onClick = onToggleFavorite
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (isFavorite) "Remove from saved" else "Save quote",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Saved",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Surface(
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            shape = RunicExpressiveTheme.shapes.segment,
            color = MaterialTheme.colorScheme.secondary,
            onClick = onShare
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share quote",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSecondary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Share",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSecondary
                )
            }
        }

        Surface(
            modifier = Modifier.size(42.dp),
            shape = RunicExpressiveTheme.shapes.segment,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            onClick = onNewQuote
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "New random quote",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun RecentQuoteCard(
    runicText: String,
    latinText: String,
    author: String,
    isFavorite: Boolean
) {
    val shapes = RunicExpressiveTheme.shapes
    val typeRoles = RunicTypeRoles.current

    Card(
        shape = shapes.contentCard,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = runicText,
                    style = typeRoles.runicCard,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = latinText,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = author,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isFavorite) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = "Saved",
                    modifier = Modifier
                        .size(16.dp)
                        .align(Alignment.BottomEnd),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun HistoryLink(onClick: () -> Unit) {
    val shapes = RunicExpressiveTheme.shapes

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.contentCard)
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = shapes.contentCard
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 17.dp, vertical = 15.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = "History",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "View full quote history",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "View full quote history",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


@Composable
private fun TodayLoadingSkeleton(modifier: Modifier = Modifier) {
    val brush = rememberShimmerBrush()

    Column(
        modifier = modifier
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Header skeleton
        SkeletonRect(
            modifier = Modifier.fillMaxWidth(0.4f),
            height = 14.dp,
            brush = brush
        )
        Spacer(modifier = Modifier.height(8.dp))
        SkeletonRect(
            modifier = Modifier.fillMaxWidth(0.6f),
            height = 24.dp,
            brush = brush
        )
        Spacer(modifier = Modifier.height(14.dp))

        // Segmented control skeleton
        SkeletonRect(height = 42.dp, brush = brush)
        Spacer(modifier = Modifier.height(14.dp))

        // Hero card skeleton
        SkeletonCard(height = 180.dp, brush = brush)
        Spacer(modifier = Modifier.height(14.dp))

        // Action buttons skeleton
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SkeletonRect(
                modifier = Modifier.weight(1f),
                height = 48.dp,
                brush = brush
            )
            SkeletonRect(
                modifier = Modifier.weight(1f),
                height = 48.dp,
                brush = brush
            )
            SkeletonRect(
                modifier = Modifier.width(48.dp),
                height = 48.dp,
                brush = brush
            )
        }
        Spacer(modifier = Modifier.height(24.dp))

        // Recent quotes skeleton
        SkeletonRect(
            modifier = Modifier.fillMaxWidth(0.35f),
            height = 16.dp,
            brush = brush
        )
        Spacer(modifier = Modifier.height(10.dp))
        SkeletonCard(height = 100.dp, brush = brush)
        Spacer(modifier = Modifier.height(12.dp))
        SkeletonCard(height = 100.dp, brush = brush)
    }
}
