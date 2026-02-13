package com.po4yka.runicquotes.ui.screens.onboarding

import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.po4yka.runicquotes.domain.model.RunicScript
import com.po4yka.runicquotes.ui.components.RunicText
import com.po4yka.runicquotes.ui.theme.RunicExpressiveTheme
import com.po4yka.runicquotes.ui.theme.RunicTypeRoles
import com.po4yka.runicquotes.util.rememberHapticFeedback

/**
 * First-run onboarding with story cards for each runic script.
 */
@Composable
fun OnboardingScreen(
    selectedScript: RunicScript,
    selectedThemePack: String,
    onChooseStyle: (RunicScript, String) -> Unit,
    onComplete: () -> Unit
) {
    val haptics = rememberHapticFeedback()
    val stories = rememberScriptStories()
    val typeRoles = RunicTypeRoles.current

    Scaffold(
        modifier = Modifier.testTag("onboarding_screen"),
        bottomBar = {
            Button(
                onClick = {
                    haptics.successPattern()
                    onComplete()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .testTag("onboarding_finish_button")
            ) {
                Text("Start with this style")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(top = 18.dp)
        ) {
            Text(
                text = "Choose your runic voice",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Text(
                text = "Read each story card, then set the script and mood you want by default.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(stories, key = { it.script.name }) { story ->
                    ScriptStoryCard(
                        story = story,
                        selected = selectedScript == story.script,
                        onSelect = {
                            haptics.mediumAction()
                            onChooseStyle(story.script, story.suggestedThemePack)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Selected: ${selectedScriptLabel(selectedScript)} + ${themeLabel(selectedThemePack)}",
                style = typeRoles.quoteMeta,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Text(
                text = "You can change this any time in Settings.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun ScriptStoryCard(
    story: ScriptStory,
    selected: Boolean,
    onSelect: () -> Unit
) {
    val shapes = RunicExpressiveTheme.shapes
    val elevations = RunicExpressiveTheme.elevations
    val motion = RunicExpressiveTheme.motion
    val typeRoles = RunicTypeRoles.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val elevation by animateDpAsState(
        targetValue = when {
            selected -> elevations.raisedCard
            isPressed -> elevations.card
            else -> elevations.flat
        },
        animationSpec = tween(
            durationMillis = motion.shortDurationMillis,
            easing = motion.standardEasing
        ),
        label = "onboardingCardElevation"
    )

    Card(
        shape = shapes.contentCard,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        modifier = Modifier
            .width(320.dp)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
                shape = shapes.contentCard
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onSelect
            )
            .testTag("onboarding_${story.script.name.lowercase()}_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = story.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = story.era,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            RunicText(
                text = story.sampleRunes,
                script = story.script,
                style = typeRoles.runicCard
            )
            Text(
                text = "\"${story.sampleLatin}\"",
                style = typeRoles.quoteMeta
            )
            Text(
                text = story.story,
                style = typeRoles.latinQuote
            )
            Text(
                text = "Suggested palette: ${themeLabel(story.suggestedThemePack)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private data class ScriptStory(
    val script: RunicScript,
    val title: String,
    val era: String,
    val sampleRunes: String,
    val sampleLatin: String,
    val story: String,
    val suggestedThemePack: String
)

private fun rememberScriptStories(): List<ScriptStory> {
    return listOf(
        ScriptStory(
            script = RunicScript.ELDER_FUTHARK,
            title = "Elder Futhark",
            era = "Early Germanic era (ca. 150-800 AD)",
            sampleRunes = "ᚨᚾᛞ ᛏᚺᛖ ᚱᚢᚾᛖᛋ",
            sampleLatin = "and the runes",
            story = "A crisp, archeological feel. Great when you want classic rune energy.",
            suggestedThemePack = "stone"
        ),
        ScriptStory(
            script = RunicScript.YOUNGER_FUTHARK,
            title = "Younger Futhark",
            era = "Viking Age (ca. 800-1100 AD)",
            sampleRunes = "ᚢᛁᚴᛁᚾᚴ ᛊᛏᛁᛚ",
            sampleLatin = "viking style",
            story = "Compact and direct shapes. A great fit for punchy, modern reading.",
            suggestedThemePack = "night_ink"
        ),
        ScriptStory(
            script = RunicScript.CIRTH,
            title = "Cirth",
            era = "Tolkien-inspired runes (Middle-earth)",
            sampleRunes = "\uE088\uE0B4\uE0CB\uE09C \uE0B8\uE0CA\uE0A8\uE0A8",
            sampleLatin = "not all",
            story = "Literary and atmospheric. Ideal for fantasy mood and Tolkien quotes.",
            suggestedThemePack = "parchment"
        )
    )
}

private fun selectedScriptLabel(script: RunicScript): String {
    return when (script) {
        RunicScript.ELDER_FUTHARK -> "Elder Futhark"
        RunicScript.YOUNGER_FUTHARK -> "Younger Futhark"
        RunicScript.CIRTH -> "Cirth"
    }
}

private fun themeLabel(themePack: String): String {
    return when (themePack) {
        "parchment" -> "Parchment"
        "night_ink" -> "Night Ink"
        else -> "Stone"
    }
}
