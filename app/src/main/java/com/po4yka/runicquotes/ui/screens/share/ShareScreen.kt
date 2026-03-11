@file:Suppress("TooManyFunctions")

package com.po4yka.runicquotes.ui.screens.share

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.po4yka.runicquotes.domain.model.Quote
import com.po4yka.runicquotes.ui.components.ErrorState
import com.po4yka.runicquotes.ui.components.RunicText
import com.po4yka.runicquotes.ui.theme.RunicExpressiveTheme
import com.po4yka.runicquotes.ui.theme.RunicSharePalette
import com.po4yka.runicquotes.ui.theme.RunicShareStyleTokens
import com.po4yka.runicquotes.ui.theme.RunicTextRole
import com.po4yka.runicquotes.ui.theme.runicSharePalette
import com.po4yka.runicquotes.ui.theme.runicShareStyleTokens
import com.po4yka.runicquotes.util.ShareAppearance
import com.po4yka.runicquotes.util.ShareTemplate

@Composable
fun ShareScreen(
    onNavigateBack: () -> Unit = {},
    quoteId: Long = 0L,
    viewModel: ShareViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedTemplate by viewModel.selectedTemplate.collectAsStateWithLifecycle()
    val selectedAppearance by viewModel.selectedAppearance.collectAsStateWithLifecycle()
    val feedbackMessage by viewModel.feedbackMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(quoteId) {
        viewModel.initializeQuoteIfNeeded(quoteId)
    }

    LaunchedEffect(feedbackMessage) {
        feedbackMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearFeedback()
        }
    }

    Scaffold(
        topBar = {
            ShareTopBar(
                title = selectedTemplate.screenTitle,
                onNavigateBack = onNavigateBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        when (val state = uiState) {
            is ShareUiState.Loading -> ShareLoading(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )

            is ShareUiState.Error -> ErrorState(
                title = "Something Went Wrong",
                description = state.message,
                onRetry = viewModel::retry,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(vertical = 48.dp)
            )

            is ShareUiState.Success -> ShareContent(
                quote = state.quote,
                selectedTemplate = selectedTemplate,
                selectedAppearance = selectedAppearance,
                onSelectTemplate = viewModel::selectTemplate,
                onSelectAppearance = viewModel::selectAppearance,
                onShareAsText = viewModel::shareAsText,
                onShareAsImage = viewModel::shareAsImage,
                onCopyQuote = viewModel::copyQuote,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun ShareTopBar(
    title: String,
    onNavigateBack: () -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(42.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.align(Alignment.Center)
            )

            Spacer(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(42.dp)
            )
        }
    }
}

@Composable
private fun ShareContent(
    quote: Quote,
    selectedTemplate: ShareTemplate,
    selectedAppearance: ShareAppearance,
    onSelectTemplate: (ShareTemplate) -> Unit,
    onSelectAppearance: (ShareAppearance) -> Unit,
    onShareAsText: () -> Unit,
    onShareAsImage: () -> Unit,
    onCopyQuote: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = remember(selectedAppearance) { runicSharePalette(selectedAppearance) }
    val shareStyle = remember { runicShareStyleTokens() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        AppearanceToggle(
            selectedAppearance = selectedAppearance,
            onSelectAppearance = onSelectAppearance,
            shareStyle = shareStyle
        )

        SharePreview(
            quote = quote,
            selectedTemplate = selectedTemplate,
            palette = palette,
            shareStyle = shareStyle
        )

        selectedTemplate.helperText?.let { helper ->
            Text(
                text = helper,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        TemplateSelector(
            selectedTemplate = selectedTemplate,
            onSelectTemplate = onSelectTemplate,
            palette = palette,
            shareStyle = shareStyle
        )

        ShareActions(
            selectedTemplate = selectedTemplate,
            palette = palette,
            shareStyle = shareStyle,
            onShareAsText = onShareAsText,
            onShareAsImage = onShareAsImage,
            onCopyQuote = onCopyQuote
        )

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun AppearanceToggle(
    selectedAppearance: ShareAppearance,
    onSelectAppearance: (ShareAppearance) -> Unit,
    shareStyle: RunicShareStyleTokens
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = shareStyle.appearanceToggleShape,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            )
        ) {
            Row(
                modifier = Modifier.padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                ShareAppearance.entries.forEach { appearance ->
                    val selected = appearance == selectedAppearance
                    val appearancePalette = runicSharePalette(appearance)
                    Surface(
                        shape = shareStyle.appearanceOptionShape,
                        color = if (selected) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            Color.Transparent
                        },
                        onClick = { onSelectAppearance(appearance) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(RunicExpressiveTheme.shapes.pill)
                                    .background(appearancePalette.appearanceSwatch)
                            )
                            Text(
                                text = appearance.displayName,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SharePreview(
    quote: Quote,
    selectedTemplate: ShareTemplate,
    palette: RunicSharePalette,
    shareStyle: RunicShareStyleTokens
) {
    when (selectedTemplate) {
        ShareTemplate.CARD -> CardPreview(quote = quote, palette = palette, shareStyle = shareStyle)
        ShareTemplate.VERSE -> VersePreview(quote = quote, palette = palette, shareStyle = shareStyle)
        ShareTemplate.LANDSCAPE -> LandscapePreview(quote = quote, palette = palette, shareStyle = shareStyle)
    }
}

@Composable
private fun CardPreview(
    quote: Quote,
    palette: RunicSharePalette,
    shareStyle: RunicShareStyleTokens
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .aspectRatio(292f / 365f),
        shape = shareStyle.cardPreviewShape,
        color = palette.surface,
        shadowElevation = shareStyle.previewCardElevation,
        border = BorderStroke(1.dp, palette.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PreviewBrandBar(palette = palette)

            DecorativeRule(palette = palette, shareStyle = shareStyle)

            RunicText(
                text = quote.previewRunicText,
                role = RunicTextRole.ShareCard,
                color = palette.primaryText,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Spacer(
                    modifier = Modifier
                        .size(width = 32.dp, height = 1.dp)
                        .background(palette.rule)
                )
            }

            Text(
                text = "“${quote.textLatin}”",
                style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                color = palette.secondaryText,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "— ${quote.author}",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                color = palette.secondaryText,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "Runatal · ${quote.previewScriptLabel}",
                style = MaterialTheme.typography.labelSmall,
                color = palette.tertiaryText,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun VersePreview(
    quote: Quote,
    palette: RunicSharePalette,
    shareStyle: RunicShareStyleTokens
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .aspectRatio(292f / 389.328125f),
        shape = shareStyle.cardPreviewShape,
        color = palette.surface,
        shadowElevation = shareStyle.previewCardElevation,
        border = BorderStroke(1.dp, palette.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DecorativeDots(palette = palette, shareStyle = shareStyle)

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "“${quote.textLatin}”",
                style = MaterialTheme.typography.headlineSmall.copy(fontStyle = FontStyle.Italic),
                color = palette.primaryText,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))

            DividerWithDots(palette = palette, shareStyle = shareStyle)

            Spacer(modifier = Modifier.height(18.dp))

            RunicText(
                text = quote.previewRunicText,
                role = RunicTextRole.ShareVerse,
                color = palette.tertiaryText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = quote.author,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                color = palette.secondaryText,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "ᚱ  Runatal",
                style = MaterialTheme.typography.labelSmall,
                color = palette.tertiaryText,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun LandscapePreview(
    quote: Quote,
    palette: RunicSharePalette,
    shareStyle: RunicShareStyleTokens
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .aspectRatio(16f / 9f),
        shape = shareStyle.landscapePreviewShape,
        color = palette.surface,
        shadowElevation = shareStyle.landscapePreviewElevation,
        border = BorderStroke(1.dp, palette.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = "ᚱ  Runatal · ${quote.previewScriptLabel}",
                style = MaterialTheme.typography.labelSmall,
                color = palette.tertiaryText
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "“${quote.textLatin}”",
                style = MaterialTheme.typography.titleLarge.copy(fontStyle = FontStyle.Italic),
                color = palette.primaryText,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            AuthorRule(author = quote.author, palette = palette, shareStyle = shareStyle)

            Spacer(modifier = Modifier.height(8.dp))

            RunicText(
                text = quote.previewRunicText,
                role = RunicTextRole.ShareLandscape,
                color = palette.tertiaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun TemplateSelector(
    selectedTemplate: ShareTemplate,
    onSelectTemplate: (ShareTemplate) -> Unit,
    palette: RunicSharePalette,
    shareStyle: RunicShareStyleTokens
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ShareTemplate.entries.forEach { template ->
            val selected = template == selectedTemplate
            Surface(
                modifier = Modifier.size(width = 124.dp, height = 94.dp),
                shape = shareStyle.templateTileShape,
                color = if (selected) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLow
                },
                border = BorderStroke(
                    1.dp,
                    if (selected) {
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)
                    } else {
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                    }
                ),
                onClick = { onSelectTemplate(template) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MiniTemplatePreview(
                        template = template,
                        palette = palette,
                        shareStyle = shareStyle
                    )
                    Text(
                        text = template.displayName,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniTemplatePreview(
    template: ShareTemplate,
    palette: RunicSharePalette,
    shareStyle: RunicShareStyleTokens
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
        shape = shareStyle.miniPreviewShape,
        color = palette.surface,
        border = BorderStroke(1.dp, palette.outline)
    ) {
        when (template) {
            ShareTemplate.CARD -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "ᚱ",
                    style = MaterialTheme.typography.labelLarge,
                    color = palette.tertiaryText
                )
            }

            ShareTemplate.VERSE -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                DecorativeDots(palette = palette, shareStyle = shareStyle)
            }

            ShareTemplate.LANDSCAPE -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "16:9",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.tertiaryText
                )
            }
        }
    }
}

@Composable
private fun ShareActions(
    selectedTemplate: ShareTemplate,
    palette: RunicSharePalette,
    shareStyle: RunicShareStyleTokens,
    onShareAsText: () -> Unit,
    onShareAsImage: () -> Unit,
    onCopyQuote: () -> Unit
) {
    if (selectedTemplate == ShareTemplate.CARD) {
        PrimaryShareButton(
            label = "Share as Text",
            icon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            },
            onClick = onShareAsText,
            palette = palette,
            shareStyle = shareStyle
        )

        Spacer(modifier = Modifier.height(10.dp))

        SecondaryShareButton(
            label = "Export as Image",
            icon = {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            },
            onClick = onShareAsImage,
            palette = palette,
            shareStyle = shareStyle
        )
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PrimaryShareButton(
                label = selectedTemplate.primaryActionLabel,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                onClick = onShareAsImage,
                palette = palette,
                shareStyle = shareStyle,
                modifier = Modifier.weight(1f)
            )

            UtilityActionButton(
                icon = Icons.AutoMirrored.Filled.Send,
                label = "Share text",
                onClick = onShareAsText,
                palette = palette,
                shareStyle = shareStyle
            )

            UtilityActionButton(
                icon = Icons.Default.ContentCopy,
                label = "Copy quote",
                onClick = onCopyQuote,
                palette = palette,
                shareStyle = shareStyle
            )
        }
    }
}

@Composable
private fun PrimaryShareButton(
    label: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    palette: RunicSharePalette,
    shareStyle: RunicShareStyleTokens,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = shareStyle.actionButtonShape,
        color = palette.actionFill,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(18.dp), contentAlignment = Alignment.Center) {
                CompositionLocalProviderForActionContent(palette.actionText, icon)
            }
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = palette.actionText
            )
        }
    }
}

@Composable
private fun SecondaryShareButton(
    label: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    palette: RunicSharePalette,
    shareStyle: RunicShareStyleTokens
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = shareStyle.actionButtonShape,
        color = palette.utilityFill,
        border = BorderStroke(1.dp, palette.utilityBorder),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(18.dp), contentAlignment = Alignment.Center) {
                CompositionLocalProviderForActionContent(palette.utilityContent, icon)
            }
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = palette.utilityContent
            )
        }
    }
}

@Composable
private fun UtilityActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    palette: RunicSharePalette,
    shareStyle: RunicShareStyleTokens
) {
    Surface(
        modifier = Modifier.size(48.dp),
        shape = shareStyle.actionButtonShape,
        color = palette.utilityFill,
        border = BorderStroke(1.dp, palette.utilityBorder),
        onClick = onClick
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = palette.utilityContent
            )
        }
    }
}

@Composable
private fun PreviewBrandBar(palette: RunicSharePalette) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(palette.rule)
        )
        Text(
            text = "ᚱ",
            style = MaterialTheme.typography.labelLarge,
            color = palette.tertiaryText
        )
        Spacer(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(palette.rule)
        )
    }
}

@Composable
private fun DecorativeRule(
    palette: RunicSharePalette,
    shareStyle: RunicShareStyleTokens
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Spacer(
            modifier = Modifier
                .size(width = shareStyle.ruleWidth, height = 1.dp)
                .background(palette.rule)
        )
    }
}

@Composable
private fun DecorativeDots(
    palette: RunicSharePalette,
    shareStyle: RunicShareStyleTokens
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(
            shareStyle.dotSmallSize,
            shareStyle.dotLargeSize,
            shareStyle.dotSmallSize
        ).forEach { size ->
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(RunicExpressiveTheme.shapes.pill)
                    .background(palette.secondaryText.copy(alpha = 0.65f))
            )
        }
    }
}

@Composable
private fun DividerWithDots(
    palette: RunicSharePalette,
    shareStyle: RunicShareStyleTokens
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(2) {
            Box(
                modifier = Modifier
                    .size(width = 4.dp, height = 1.dp)
                    .background(palette.rule)
            )
        }
        Box(
            modifier = Modifier
                .size(width = shareStyle.ruleWidth, height = 1.dp)
                .background(palette.rule)
        )
        repeat(2) {
            Box(
                modifier = Modifier
                    .size(width = 4.dp, height = 1.dp)
                    .background(palette.rule)
            )
        }
    }
}

@Composable
private fun AuthorRule(
    author: String,
    palette: RunicSharePalette,
    shareStyle: RunicShareStyleTokens
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(
            modifier = Modifier
                .size(width = shareStyle.authorRuleWidth, height = 1.dp)
                .background(palette.rule)
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = author,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
            color = palette.secondaryText
        )
        Spacer(modifier = Modifier.size(8.dp))
        Spacer(
            modifier = Modifier
                .size(width = shareStyle.authorRuleWidth, height = 1.dp)
                .background(palette.rule)
        )
    }
}

@Composable
private fun ShareLoading(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

private val ShareTemplate.screenTitle: String
    get() = when (this) {
        ShareTemplate.CARD -> "Share Quote"
        ShareTemplate.VERSE -> "Share — Verse"
        ShareTemplate.LANDSCAPE -> "Share — Landscape"
    }

private val ShareTemplate.primaryActionLabel: String
    get() = when (this) {
        ShareTemplate.CARD -> "Export as Image"
        ShareTemplate.VERSE -> "Share Verse"
        ShareTemplate.LANDSCAPE -> "Share Landscape"
    }

private val ShareTemplate.helperText: String?
    get() = when (this) {
        ShareTemplate.CARD -> null
        ShareTemplate.VERSE -> null
        ShareTemplate.LANDSCAPE -> "16:9 · Ideal for social media headers and banners"
    }

private val Quote.previewRunicText: String
    get() = runicElder ?: runicYounger ?: runicCirth ?: textLatin

private val Quote.previewScriptLabel: String
    get() = when {
        runicElder != null -> "Elder Futhark"
        runicYounger != null -> "Younger Futhark"
        runicCirth != null -> "Cirth"
        else -> "Runic"
    }

@Composable
private fun CompositionLocalProviderForActionContent(
    tint: Color,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        androidx.compose.material3.LocalContentColor provides tint
    ) {
        content()
    }
}
