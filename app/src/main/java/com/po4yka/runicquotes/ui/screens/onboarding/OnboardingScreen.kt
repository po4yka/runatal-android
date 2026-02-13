package com.po4yka.runicquotes.ui.screens.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.po4yka.runicquotes.domain.model.RunicScript
import com.po4yka.runicquotes.ui.components.RunicText
import com.po4yka.runicquotes.ui.theme.LocalReduceMotion
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
    val shapes = RunicExpressiveTheme.shapes
    val motion = RunicExpressiveTheme.motion
    val reducedMotion = LocalReduceMotion.current
    val listState = rememberLazyListState()
    val visibleStoryIndex by remember(stories, listState) {
        derivedStateOf {
            val firstVisibleIndex = listState.firstVisibleItemIndex
            val offset = listState.firstVisibleItemScrollOffset
            val activeIndex = if (offset > 120 && firstVisibleIndex < stories.lastIndex) {
                firstVisibleIndex + 1
            } else {
                firstVisibleIndex
            }
            activeIndex.coerceIn(0, stories.lastIndex)
        }
    }
    val selectedStory = remember(stories, selectedScript) {
        stories.firstOrNull { it.script == selectedScript } ?: stories.first()
    }

    Scaffold(
        modifier = Modifier.testTag("onboarding_screen"),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            OnboardingActionsPanel(
                selectedScript = selectedScript,
                selectedThemePack = selectedThemePack,
                onSkip = {
                    haptics.lightToggle()
                    onComplete()
                },
                onContinue = {
                    haptics.successPattern()
                    onComplete()
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.38f),
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.24f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 14.dp)
            ) {
                Surface(
                    shape = shapes.heroCard,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    tonalElevation = 3.dp,
                    shadowElevation = 2.dp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Choose your runic script style",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "Swipe through cards and set the style that should shape your default quote experience.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                Text(
                    text = "Swipe to compare scripts",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )

                LazyRow(
                    state = listState,
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

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CarouselIndicator(
                        itemCount = stories.size,
                        selectedIndex = visibleStoryIndex
                    )
                    Text(
                        text = "${visibleStoryIndex + 1} of ${stories.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Crossfade(
                    targetState = selectedStory,
                    animationSpec = tween(
                        durationMillis = motion.duration(
                            reducedMotion = reducedMotion,
                            base = motion.mediumDurationMillis
                        ),
                        easing = motion.emphasizedEasing
                    ),
                    label = "selectedStylePanel"
                ) { story ->
                    Surface(
                        shape = shapes.panel,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        tonalElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "Current selection",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${selectedScriptLabel(story.script)} + ${themeLabel(story.suggestedThemePack)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = story.story,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun OnboardingActionsPanel(
    selectedScript: RunicScript,
    selectedThemePack: String,
    onSkip: () -> Unit,
    onContinue: () -> Unit
) {
    val shapes = RunicExpressiveTheme.shapes

    Surface(
        shape = shapes.panel,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 6.dp,
        shadowElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag("onboarding_actions")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                text = "Default style: ${selectedScriptLabel(selectedScript)} + ${themeLabel(selectedThemePack)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "You can change this anytime in Settings.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onSkip,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("onboarding_skip_button")
                ) {
                    Text("Skip for now")
                }
                Button(
                    onClick = onContinue,
                    shape = shapes.heroCard,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .weight(1.7f)
                        .height(56.dp)
                        .testTag("onboarding_finish_button")
                ) {
                    Text(
                        text = "Continue with this style",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .size(18.dp)
                    )
                }
            }
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
    val reducedMotion = LocalReduceMotion.current
    val typeRoles = RunicTypeRoles.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val elevation by animateDpAsState(
        targetValue = when {
            selected -> elevations.raisedCard
            isPressed -> elevations.pressedCard
            else -> elevations.card
        },
        animationSpec = tween(
            durationMillis = motion.duration(
                reducedMotion = reducedMotion,
                base = motion.shortDurationMillis
            ),
            easing = motion.standardEasing
        ),
        label = "onboardingCardElevation"
    )
    val scale by animateFloatAsState(
        targetValue = when {
            selected -> 1f
            isPressed -> 0.985f
            else -> 0.965f
        },
        animationSpec = tween(
            durationMillis = motion.duration(
                reducedMotion = reducedMotion,
                base = motion.shortDurationMillis
            ),
            easing = motion.emphasizedEasing
        ),
        label = "onboardingCardScale"
    )
    val containerColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        animationSpec = tween(
            durationMillis = motion.duration(
                reducedMotion = reducedMotion,
                base = motion.mediumDurationMillis
            )
        ),
        label = "onboardingCardContainer"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outlineVariant
        },
        animationSpec = tween(
            durationMillis = motion.duration(
                reducedMotion = reducedMotion,
                base = motion.mediumDurationMillis
            )
        ),
        label = "onboardingCardBorder"
    )

    val cardShape = if (selected) shapes.heroCard else shapes.collectionCard

    Card(
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        modifier = Modifier
            .width(304.dp)
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale
            )
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = borderColor,
                shape = cardShape
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = story.title,
                    style = if (selected) {
                        MaterialTheme.typography.headlineSmall
                    } else {
                        MaterialTheme.typography.titleLarge
                    },
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                AnimatedVisibility(visible = selected) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(shapes.collectionCard)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Selected",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            Text(
                text = story.era,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Surface(
                shape = shapes.collectionCard,
                color = if (selected) {
                    MaterialTheme.colorScheme.tertiaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                tonalElevation = if (selected) 2.dp else 0.dp
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                    RunicText(
                        text = story.sampleRunes,
                        script = story.script,
                        style = if (selected) typeRoles.runicHero else typeRoles.runicCard,
                        color = if (selected) {
                            MaterialTheme.colorScheme.onTertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                    Text(
                        text = "Sample transliteration: ${story.sampleLatin}",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) {
                            MaterialTheme.colorScheme.onTertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

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

@Composable
private fun CarouselIndicator(
    itemCount: Int,
    selectedIndex: Int
) {
    val motion = RunicExpressiveTheme.motion
    val reducedMotion = LocalReduceMotion.current
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(itemCount) { index ->
            val isSelected = index == selectedIndex
            val width by animateDpAsState(
                targetValue = if (isSelected) 24.dp else 8.dp,
                animationSpec = tween(
                    durationMillis = motion.duration(
                        reducedMotion = reducedMotion,
                        base = motion.shortDurationMillis
                    )
                ),
                label = "carouselDotWidth"
            )
            val color by animateColorAsState(
                targetValue = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
                animationSpec = tween(
                    durationMillis = motion.duration(
                        reducedMotion = reducedMotion,
                        base = motion.shortDurationMillis
                    )
                ),
                label = "carouselDotColor"
            )
            Spacer(
                modifier = Modifier
                    .size(width = width, height = 8.dp)
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(color)
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
            story = "Crisp, archaeological letterforms that feel traditional and grounded.",
            suggestedThemePack = "stone"
        ),
        ScriptStory(
            script = RunicScript.YOUNGER_FUTHARK,
            title = "Younger Futhark",
            era = "Viking Age (ca. 800-1100 AD)",
            sampleRunes = "ᚢᛁᚴᛁᚾᚴ ᛊᛏᛁᛚ",
            sampleLatin = "Viking style",
            story = "Compact and direct forms that read cleanly in short, punchy quotes.",
            suggestedThemePack = "night_ink"
        ),
        ScriptStory(
            script = RunicScript.CIRTH,
            title = "Cirth",
            era = "Tolkien-inspired runes (Middle-earth)",
            sampleRunes = "\uE088\uE0B4\uE0CB\uE09C \uE0B8\uE0CA\uE0A8\uE0A8",
            sampleLatin = "Not all who wander",
            story = "Literary and atmospheric, built for a fantasy-forward reading style.",
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
