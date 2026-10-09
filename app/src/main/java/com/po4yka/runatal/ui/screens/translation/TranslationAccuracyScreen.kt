package com.po4yka.runatal.ui.screens.translation

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
import com.po4yka.runatal.ui.components.RunicArticleCard
import com.po4yka.runatal.ui.components.RunicArticleDivider
import com.po4yka.runatal.ui.components.RunicArticleLeadCard
import com.po4yka.runatal.ui.components.RunicArticleLinkCard
import com.po4yka.runatal.ui.components.RunicTopBar
import com.po4yka.runatal.ui.components.RunicTopBarIconAction

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
        text = "Transliterate converts modern Latin spelling into runic glyphs. " +
            "Historical mode, when enabled, uses a limited bundled corpus and reviewed language rules. " +
            "Check each result's status, notes, and sources to understand what it supports.",
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
            title = "Transliterate follows spelling",
            body = "Direct output maps letters and selected letter sequences; it does not infer pronunciation " +
                "or translate meaning. Modern spelling can produce ambiguous or approximate rune choices.",
            showDivider = true
        )
        TranslationLimitation(
            title = "Historical mode has limited coverage",
            body = "Younger Futhark supports reviewed Old Norse forms and grammar. Elder Futhark supports " +
                "curated forms and witnessed formulas. Strict returns Unavailable when required evidence or " +
                "forms are missing. Readable and Decorative allow approximations; check their notes.",
            showDivider = true
        )
        TranslationLimitation(
            title = "Modern Elder mappings",
            body = "C and K map to K (ᚲ); Q and QU to KW (ᚲᚹ); X to KS (ᚲᛊ). " +
                "V shares U (ᚢ), while W uses W (ᚹ). Shared mappings cannot uniquely recover the original spelling.",
            showDivider = true
        )
        TranslationLimitation(
            title = "Statuses describe evidence",
            body = "Attested identifies a source-backed form or formula. Reconstructed uses reviewed forms, " +
                "grammar, or a named transcription profile. Approximated can include glyph substitution or " +
                "preserved modern spelling. These labels do not guarantee correctness for every interpretation.",
            showDivider = true
        )
        TranslationLimitation(
            title = "Cirth uses an English profile",
            body = "In Historical mode, Strict Cirth covers the documented title-page English profile and " +
                "its reviewed words. General Latin spelling is an educational glyph substitution marked " +
                "Approximated. This is not arbitrary translation into Tolkien's fictional languages.",
            showDivider = true
        )
        TranslationLimitation(
            title = "Scores are heuristic",
            body = "The decimal score summarizes local evidence and rule coverage. It is not calibrated " +
                "against measured accuracy and is not a probability that a translation is correct. " +
                "Use the result's status, notes, and cited sources alongside it."
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
