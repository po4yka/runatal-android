package com.po4yka.runicquotes.ui.screens.translation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.po4yka.runicquotes.ui.components.RunicArticleCard
import com.po4yka.runicquotes.ui.components.RunicArticleDivider
import com.po4yka.runicquotes.ui.components.RunicArticleLeadCard
import com.po4yka.runicquotes.ui.components.RunicArticleLinkCard
import com.po4yka.runicquotes.ui.components.RunicTopBar
import com.po4yka.runicquotes.ui.components.RunicTopBarIconAction

@Composable
fun TranslationAccuracyScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToReferences: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TranslationAccuracyTopBar(onNavigateBack = onNavigateBack)
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(start = 20.dp, top = 6.dp, end = 20.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ContextLeadCard()
            }
            item {
                AccuracySectionLabel("Known limitations")
            }
            item {
                LimitationsCard()
            }
            item {
                AccuracySectionLabel("Historical context")
            }
            item {
                HistoricalContextCard()
            }
            item {
                RuneReferenceLinkCard(onClick = onNavigateToReferences)
            }
        }
    }
}

@Composable
private fun AccuracySectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun TranslationAccuracyTopBar(onNavigateBack: () -> Unit) {
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
                text = "Accuracy & context",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    )
}

@Composable
private fun ContextLeadCard() {
    RunicArticleLeadCard(
        text = "This is a modern convention, not a historical reconstruction. " +
            "Runes were not designed to encode Modern English. " +
            "Understanding the limits makes the output more meaningful.",
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun LimitationsCard() {
    RunicArticleCard(
        contentGap = 0.dp,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        TranslationLimitation(
            title = "Transliteration, not translation",
            body = "The output is a phonetic re-encoding of your text using runic characters. " +
                "The meaning of the original words is not preserved in the runes, only the sounds.",
            showDivider = true
        )
        TranslationLimitation(
            title = "Modern English was not written in runes",
            body = "Elder Futhark was designed for Proto-Germanic, not Modern English. " +
                "Several sounds exist in English that have no direct runic equivalent.",
            showDivider = true
        )
        TranslationLimitation(
            title = "Shared glyphs reduce uniqueness",
            body = "In Elder Futhark, C, K, and Q map to ᚲ; V and W map to ᚹ. " +
                "The reverse mapping is ambiguous, so runic text does not uniquely encode every Latin letter.",
            showDivider = true
        )
        TranslationLimitation(
            title = "No universally agreed standard",
            body = "Scholars and enthusiasts differ on the correct mapping of modern sounds to ancient glyphs. " +
                "Runatal follows a conventional and widely used approach, not a single authoritative source."
        )
    }
}

@Composable
private fun TranslationLimitation(
    title: String,
    body: String,
    showDivider: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (showDivider) {
            RunicArticleDivider(modifier = Modifier.padding(top = 10.dp))
        }
    }
}

@Composable
private fun HistoricalContextCard() {
    RunicArticleCard(
        containerColor = MaterialTheme.colorScheme.surface,
        contentGap = 12.dp
    ) {
        Text(
            text = "Elder Futhark was used by Germanic peoples from roughly the 2nd to the 8th century AD. " +
                "Inscriptions were carved into stone, metal, and bone, usually in terse commemorative forms.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        RunicArticleDivider()
        Text(
            text = "Younger Futhark reduced the alphabet from 24 to 16 glyphs " +
                "as Old Norse became more phonetically complex, making the script " +
                "historically dense but less precise for modern transliteration.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RuneReferenceLinkCard(onClick: () -> Unit) {
    RunicArticleLinkCard(
        title = "Rune reference",
        description = "Individual rune meanings, history, and aett groupings",
        onClick = onClick
    )
}
