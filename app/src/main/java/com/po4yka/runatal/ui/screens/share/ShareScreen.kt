@file:Suppress("TooManyFunctions")

package com.po4yka.runatal.ui.screens.share

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.po4yka.runatal.domain.model.QuoteShareContent
import com.po4yka.runatal.ui.components.RunicActionButton
import com.po4yka.runatal.ui.components.RunicActionButtonStyle
import com.po4yka.runatal.ui.components.RunicActionIconButton
import com.po4yka.runatal.ui.components.RunicChoiceChip
import com.po4yka.runatal.ui.components.RunicChoiceGroup
import com.po4yka.runatal.ui.components.ErrorState
import com.po4yka.runatal.ui.components.RunicTopBar
import com.po4yka.runatal.ui.components.RunicTopBarIconAction
import com.po4yka.runatal.ui.components.runicActionButtonColors
import com.po4yka.runatal.ui.components.runicChoiceChipColors
import com.po4yka.runatal.ui.theme.RunicExpressiveTheme
import com.po4yka.runatal.ui.theme.RunicSharePalette
import com.po4yka.runatal.ui.theme.RunicShareStyleTokens
import com.po4yka.runatal.ui.theme.RunicTypeRoles
import com.po4yka.runatal.ui.theme.SupportingTextRole
import com.po4yka.runatal.ui.theme.runicSharePalette
import com.po4yka.runatal.ui.theme.runicShareStyleTokens
import com.po4yka.runatal.ui.util.rememberQuoteShareManager
import com.po4yka.runatal.util.ShareAppearance
import com.po4yka.runatal.util.ShareTemplate
import kotlinx.coroutines.launch

@Composable
internal fun ShareScreen(
    quoteId: Long,
    onNavigateBack: () -> Unit = {},
    viewModel: ShareViewModel = hiltViewModel<ShareViewModel, ShareViewModel.Factory>(
        creationCallback = { factory -> factory.create(quoteId) }
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedTemplate by viewModel.selectedTemplate.collectAsStateWithLifecycle()
    val selectedAppearance by viewModel.selectedAppearance.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val quoteShareManager = rememberQuoteShareManager()
    val coroutineScope = rememberCoroutineScope()

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
                content = state.content,
                selectedTemplate = selectedTemplate,
                selectedAppearance = selectedAppearance,
                onSelectTemplate = viewModel::selectTemplate,
                onSelectAppearance = viewModel::selectAppearance,
                onShareAsText = {
                    quoteShareManager.shareQuoteText(
                        latinText = state.content.textLatin,
                        author = state.content.author
                    )
                },
                onShareAsImage = {
                    coroutineScope.launch {
                        val didShare = quoteShareManager.shareQuoteAsImage(
                            content = state.content,
                            template = selectedTemplate,
                            appearance = selectedAppearance
                        )
                        if (!didShare) {
                            snackbarHostState.showSnackbar("Couldn't share image")
                        }
                    }
                },
                onCopyQuote = {
                    quoteShareManager.copyQuoteToClipboard(
                        latinText = state.content.textLatin,
                        author = state.content.author
                    )
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Quote copied")
                    }
                },
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
    RunicTopBar(
        navigationIcon = {
            RunicTopBarIconAction(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                onClick = onNavigateBack
            )
        },
        titleContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() }
            )
        }
    )
}

@Composable
private fun ShareContent(
    content: QuoteShareContent,
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
            content = content,
            selectedTemplate = selectedTemplate,
            selectedAppearance = selectedAppearance,
            shareStyle = shareStyle
        )

        selectedTemplate.helperText?.let { helper ->
            Text(
                text = helper,
                style = RunicTypeRoles.supporting(SupportingTextRole.HelperText),
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
        RunicChoiceGroup(
            shape = shareStyle.appearanceToggleShape,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
            contentPadding = PaddingValues(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            ShareAppearance.entries.forEach { appearance ->
                val selected = appearance == selectedAppearance
                val appearancePalette = runicSharePalette(appearance)

                RunicChoiceChip(
                    selected = selected,
                    onClick = { onSelectAppearance(appearance) },
                    shape = shareStyle.appearanceOptionShape,
                    colors = runicChoiceChipColors(
                        selected = selected,
                        unselectedContainerColor = Color.Transparent
                    ),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) { contentColor ->
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(RunicExpressiveTheme.shapes.pill)
                            .background(appearancePalette.appearanceSwatch)
                    )
                    Text(
                        text = appearance.displayName,
                        style = MaterialTheme.typography.labelLarge,
                        color = contentColor
                    )
                }
            }
        }
    }
}

@Composable
private fun SharePreview(
    content: QuoteShareContent,
    selectedTemplate: ShareTemplate,
    selectedAppearance: ShareAppearance,
    shareStyle: RunicShareStyleTokens
) {
    val manager = rememberQuoteShareManager()
    val bitmap by androidx.compose.runtime.produceState<android.graphics.Bitmap?>(
        null, content, selectedTemplate, selectedAppearance
    ) {
        value = null
        value = manager.renderQuoteImage(content, selectedTemplate, selectedAppearance)
    }
    val aspectRatio = if (selectedTemplate == ShareTemplate.LANDSCAPE) 1600f / 900f else 1080f / 1920f
    Surface(
        modifier = Modifier.fillMaxWidth().aspectRatio(aspectRatio),
        shape = shareStyle.cardPreviewShape,
        shadowElevation = shareStyle.previewCardElevation
    ) {
        val rendered = bitmap
        if (rendered != null) {
            androidx.compose.foundation.Image(
                bitmap = rendered.asImageBitmap(),
                contentDescription = "${content.scriptLabel} share preview. ${content.textLatin}. By ${content.author}",
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                androidx.compose.material3.CircularProgressIndicator()
            }
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
                modifier = Modifier
                    .size(width = 124.dp, height = 94.dp)
                    .semantics(mergeDescendants = true) {},
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
                    style = RunicTypeRoles.supporting(SupportingTextRole.ShareMeta),
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
    val primaryButtonColors = sharePrimaryActionColors(palette)
    val utilityButtonColors = shareUtilityActionColors(palette)

    if (selectedTemplate == ShareTemplate.CARD) {
        RunicActionButton(
            label = "Share as Text",
            leadingContent = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = null,
                    modifier = Modifier.size(RunicExpressiveTheme.icons.standard)
                )
            },
            onClick = onShareAsText,
            modifier = Modifier.fillMaxWidth(),
            accessibilityLabel = "Share quote as text",
            shape = shareStyle.actionButtonShape,
            colors = primaryButtonColors
        )

        Spacer(modifier = Modifier.height(10.dp))

        RunicActionButton(
            label = "Export as Image",
            leadingContent = {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = null,
                    modifier = Modifier.size(RunicExpressiveTheme.icons.standard)
                )
            },
            onClick = onShareAsImage,
            modifier = Modifier.fillMaxWidth(),
            accessibilityLabel = "Export quote as image",
            shape = shareStyle.actionButtonShape,
            colors = utilityButtonColors
        )
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RunicActionButton(
                label = selectedTemplate.primaryActionLabel,
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(RunicExpressiveTheme.icons.standard)
                    )
                },
                onClick = onShareAsImage,
                modifier = Modifier.weight(1f),
                accessibilityLabel = selectedTemplate.primaryActionLabel,
                shape = shareStyle.actionButtonShape,
                colors = primaryButtonColors
            )

            RunicActionIconButton(
                onClick = onShareAsText,
                contentDescription = "Share text",
                shape = shareStyle.actionButtonShape,
                colors = utilityButtonColors,
                iconContent = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        modifier = Modifier.size(RunicExpressiveTheme.icons.standard)
                    )
                }
            )

            RunicActionIconButton(
                onClick = onCopyQuote,
                contentDescription = "Copy quote",
                shape = shareStyle.actionButtonShape,
                colors = utilityButtonColors,
                iconContent = {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(RunicExpressiveTheme.icons.standard)
                    )
                }
            )
        }
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

@Composable
private fun sharePrimaryActionColors(palette: RunicSharePalette) = runicActionButtonColors(
    style = RunicActionButtonStyle.Primary,
    containerColor = palette.actionFill,
    contentColor = palette.actionText,
    disabledContainerColor = palette.actionFill.copy(alpha = 0.68f),
    disabledContentColor = palette.actionText.copy(alpha = 0.72f)
)

@Composable
private fun shareUtilityActionColors(palette: RunicSharePalette) = runicActionButtonColors(
    style = RunicActionButtonStyle.Outlined,
    containerColor = palette.utilityFill,
    contentColor = palette.utilityContent,
    borderColor = palette.utilityBorder,
    disabledContainerColor = palette.utilityFill,
    disabledContentColor = palette.utilityContent.copy(alpha = 0.72f),
    disabledBorderColor = palette.utilityBorder.copy(alpha = 0.62f)
)
