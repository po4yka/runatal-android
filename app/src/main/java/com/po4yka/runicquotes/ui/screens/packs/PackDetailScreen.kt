package com.po4yka.runicquotes.ui.screens.packs

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.po4yka.runicquotes.domain.model.QuotePack
import com.po4yka.runicquotes.ui.components.RunicBadge
import com.po4yka.runicquotes.ui.components.RunicChoiceChip
import com.po4yka.runicquotes.ui.components.ErrorState
import com.po4yka.runicquotes.ui.components.RunicGlyphBadge
import com.po4yka.runicquotes.ui.components.RunicInfoCard
import com.po4yka.runicquotes.ui.components.RunicTopBar
import com.po4yka.runicquotes.ui.components.RunicTopBarIconAction
import com.po4yka.runicquotes.ui.components.SkeletonCard
import com.po4yka.runicquotes.ui.theme.LocalReduceMotion
import com.po4yka.runicquotes.ui.theme.RunicExpressiveTheme
import com.po4yka.runicquotes.ui.components.SkeletonCircle
import com.po4yka.runicquotes.ui.components.rememberShimmerBrush
import com.po4yka.runicquotes.ui.components.runicChoiceChipColors

@Composable
fun PackDetailScreen(
    onNavigateBack: () -> Unit = {},
    onViewLibrary: () -> Unit = {},
    viewModel: PackDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is PackDetailEvent.ShowMessage -> {
                    val result = snackbarHostState.showSnackbar(
                        message = event.message,
                        actionLabel = event.actionLabel
                    )
                    if (result == SnackbarResult.ActionPerformed &&
                        event.action == PackDetailEventAction.VIEW_LIBRARY
                    ) {
                        onViewLibrary()
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            PackDetailTopBar(
                onNavigateBack = onNavigateBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is PackDetailUiState.Loading -> PackDetailLoadingSkeleton(
                    modifier = Modifier.fillMaxSize()
                )

                is PackDetailUiState.Error -> ErrorState(
                    title = "Something Went Wrong",
                    description = state.message,
                    onRetry = viewModel::retry,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 48.dp)
                )

                is PackDetailUiState.Success -> PackDetailContent(
                    pack = state.pack,
                    onToggleLibrary = viewModel::toggleLibrary
                )
            }
        }
    }
}

@Composable
private fun PackDetailTopBar(onNavigateBack: () -> Unit) {
    RunicTopBar(
        navigationIcon = {
            RunicTopBarIconAction(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                onClick = onNavigateBack
            )
        },
        titleContent = {}
    )
}

@Composable
private fun PackDetailContent(
    pack: QuotePack,
    onToggleLibrary: () -> Unit
) {
    val previewQuotes = PackPresentationCatalog.previewQuotes(pack)
    val sourceLabel = PackPresentationCatalog.sourceLabel(pack)
    val readTimeLabel = PackPresentationCatalog.readTimeLabel(pack)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            PackHeroCard(
                pack = pack,
                sourceLabel = sourceLabel,
                readTimeLabel = readTimeLabel,
                onToggleLibrary = onToggleLibrary
            )
        }

        item {
            Text(
                text = "Quotes in this pack",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        items(previewQuotes) { quote ->
            PackQuoteCard(quote = quote)
        }
    }
}

@Composable
private fun PackHeroCard(
    pack: QuotePack,
    sourceLabel: String,
    readTimeLabel: String,
    onToggleLibrary: () -> Unit
) {
    val reducedMotion = LocalReduceMotion.current
    val motion = RunicExpressiveTheme.motion
    var heroVisible by remember(pack.id) { mutableStateOf(reducedMotion) }

    LaunchedEffect(pack.id, reducedMotion) {
        if (!reducedMotion) {
            heroVisible = true
        }
    }

    val badgeScale by animateFloatAsState(
        targetValue = if (heroVisible) 1f else 0.74f,
        animationSpec = if (reducedMotion) {
            tween(durationMillis = 0)
        } else {
            spring(
                dampingRatio = 0.8f,
                stiffness = 560f
            )
        },
        label = "packHeroBadgeScale"
    )
    val badgeOffsetY by animateFloatAsState(
        targetValue = if (heroVisible) 0f else 30f,
        animationSpec = tween(
            durationMillis = motion.duration(reducedMotion, motion.mediumDurationMillis),
            easing = motion.emphasizedEasing
        ),
        label = "packHeroBadgeOffset"
    )
    val copyAlpha by animateFloatAsState(
        targetValue = if (heroVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = motion.duration(reducedMotion, motion.mediumDurationMillis),
            delayMillis = motion.delay(reducedMotion, motion.shortDurationMillis / 3),
            easing = motion.standardEasing
        ),
        label = "packHeroCopyAlpha"
    )
    val copyOffsetY by animateFloatAsState(
        targetValue = if (heroVisible) 0f else 18f,
        animationSpec = tween(
            durationMillis = motion.duration(reducedMotion, motion.mediumDurationMillis),
            delayMillis = motion.delay(reducedMotion, motion.shortDurationMillis / 3),
            easing = motion.emphasizedEasing
        ),
        label = "packHeroCopyOffset"
    )

    RunicInfoCard(
        shape = RunicExpressiveTheme.shapes.panel,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RunicGlyphBadge(
                    size = 48.dp,
                    shape = RoundedCornerShape(16.dp),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = badgeScale
                            scaleY = badgeScale
                            translationY = badgeOffsetY
                        }
                        .semantics { contentDescription = "Cover rune: ${pack.coverRune}" }
                ) {
                    Text(
                        text = pack.coverRune,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .graphicsLayer {
                            alpha = copyAlpha
                            translationY = copyOffsetY
                        },
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = pack.name,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = sourceLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = pack.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.graphicsLayer {
                    alpha = copyAlpha
                    translationY = copyOffsetY
                }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = copyAlpha
                        translationY = copyOffsetY / 1.4f
                    },
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RunicBadge(text = "${pack.quoteCount} quotes")
                Text(
                    text = "·",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = readTimeLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.weight(1f))

                RunicChoiceChip(
                    selected = pack.isInLibrary,
                    onClick = onToggleLibrary,
                    role = Role.Checkbox,
                    shape = RoundedCornerShape(12.dp),
                    colors = runicChoiceChipColors(
                        selected = pack.isInLibrary,
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        unselectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        unselectedContentColor = MaterialTheme.colorScheme.onPrimary,
                        selectedBorderColor = Color.Transparent,
                        unselectedBorderColor = Color.Transparent
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) { contentColor ->
                    Icon(
                        imageVector = if (pack.isInLibrary) Icons.Default.Check else Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = contentColor
                    )
                    Text(
                        text = if (pack.isInLibrary) "In Library" else "Add to Library",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                        color = contentColor
                    )
                }
            }
        }
    }
}

@Composable
private fun PackQuoteCard(quote: PackPreviewQuote) {
    RunicInfoCard(
        containerColor = if (quote.isHighlighted) {
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        borderColor = if (quote.isHighlighted) {
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)
        } else {
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f)
        },
        contentPadding = PaddingValues(horizontal = 15.dp, vertical = 13.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = quote.rune,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = quote.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = quote.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PackDetailLoadingSkeleton(modifier: Modifier = Modifier) {
    val brush = rememberShimmerBrush()

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SkeletonCircle(size = 48.dp, brush = brush)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            SkeletonCard(height = 24.dp, brush = brush, modifier = Modifier.fillMaxWidth(0.5f))
                            SkeletonCard(height = 16.dp, brush = brush, modifier = Modifier.fillMaxWidth(0.35f))
                        }
                    }
                    SkeletonCard(height = 56.dp, brush = brush)
                    SkeletonCard(height = 40.dp, brush = brush)
                }
            }
        }

        item {
            SkeletonCard(height = 24.dp, brush = brush, modifier = Modifier.fillMaxWidth(0.36f))
        }

        items(4) {
            SkeletonCard(height = 104.dp, brush = brush)
        }
    }
}
