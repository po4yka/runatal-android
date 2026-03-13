package com.po4yka.runatal.ui.screens.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.po4yka.runatal.domain.model.RunicScript
import com.po4yka.runatal.domain.transliteration.CirthTransliterator
import com.po4yka.runatal.domain.transliteration.ElderFutharkTransliterator
import com.po4yka.runatal.domain.transliteration.TransliterationFactory
import com.po4yka.runatal.domain.transliteration.YoungerFutharkTransliterator
import com.po4yka.runatal.ui.components.RunicText
import com.po4yka.runatal.ui.theme.LocalReduceMotion
import com.po4yka.runatal.ui.theme.RunicExpressiveTheme
import com.po4yka.runatal.ui.theme.RunicTextRole
import com.po4yka.runatal.util.rememberHapticFeedback

/**
 * First-run onboarding aligned to the Runatal Figma flow.
 */
@Composable
fun OnboardingScreen(
    selectedScript: RunicScript,
    onChooseStyle: (RunicScript, String) -> Unit,
    onComplete: () -> Unit
) {
    val haptics = rememberHapticFeedback()
    val stories = SCRIPT_STORIES
    val motion = RunicExpressiveTheme.motion
    val reducedMotion = LocalReduceMotion.current
    val selectedStory = remember(stories, selectedScript) {
        stories.firstOrNull { it.script == selectedScript } ?: stories.first()
    }
    var currentStepIndex by rememberSaveable { mutableIntStateOf(OnboardingStep.WELCOME.ordinal) }
    val onboardingStep = OnboardingStep.entries[currentStepIndex]

    Scaffold(
        modifier = Modifier.testTag("onboarding_screen"),
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            OnboardingFooter(
                currentStep = onboardingStep,
                onAction = {
                    when (onboardingStep) {
                        OnboardingStep.WELCOME -> {
                            haptics.mediumAction()
                            currentStepIndex = OnboardingStep.SCRIPT.ordinal
                        }

                        OnboardingStep.SCRIPT -> {
                            haptics.mediumAction()
                            currentStepIndex = OnboardingStep.FINISH.ordinal
                        }

                        OnboardingStep.FINISH -> {
                            haptics.successPattern()
                            onComplete()
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(paddingValues)
        ) {
            OnboardingStepHeader(
                currentStep = onboardingStep,
                selectedScript = selectedScript
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                AnimatedContent(
                    targetState = onboardingStep,
                    transitionSpec = {
                        if (reducedMotion) {
                            EnterTransition.None togetherWith ExitTransition.None
                        } else {
                            (fadeIn(
                                animationSpec = tween(
                                    durationMillis = motion.mediumDurationMillis,
                                    easing = motion.standardEasing
                                )
                            ) + slideInVertically(
                                animationSpec = tween(
                                    durationMillis = motion.mediumDurationMillis,
                                    easing = motion.emphasizedEasing
                                ),
                                initialOffsetY = { it / 8 }
                            ) + scaleIn(
                                animationSpec = tween(
                                    durationMillis = motion.mediumDurationMillis,
                                    easing = motion.emphasizedEasing
                                ),
                                initialScale = 0.96f
                            )) togetherWith
                                (fadeOut(
                                    animationSpec = tween(
                                        durationMillis = motion.shortDurationMillis,
                                        easing = motion.standardEasing
                                    )
                                ) + slideOutVertically(
                                    animationSpec = tween(
                                        durationMillis = motion.shortDurationMillis,
                                        easing = motion.standardEasing
                                    ),
                                    targetOffsetY = { -it / 10 }
                            ) + scaleOut(
                                    animationSpec = tween(
                                        durationMillis = motion.shortDurationMillis,
                                        easing = motion.standardEasing
                                    ),
                                    targetScale = 1.03f
                                ))
                        }
                    },
                    label = "onboardingStepContent"
                ) { step ->
                    when (step) {
                        OnboardingStep.WELCOME -> WelcomeFeatures()
                        OnboardingStep.SCRIPT -> ScriptSelectionList(
                            stories = stories,
                            selectedScript = selectedScript,
                            onSelect = { story ->
                                haptics.mediumAction()
                                onChooseStyle(story.script, story.suggestedThemePack)
                            }
                        )

                        OnboardingStep.FINISH -> FirstRuneCard(selectedStory = selectedStory)
                    }
                }
            }
        }
    }
}

private enum class OnboardingStep {
    WELCOME,
    SCRIPT,
    FINISH
}

@Composable
private fun OnboardingStepHeader(
    currentStep: OnboardingStep,
    selectedScript: RunicScript
) {
    val title = when (currentStep) {
        OnboardingStep.WELCOME -> "Welcome to Runatal"
        OnboardingStep.SCRIPT -> "Choose Your Script"
        OnboardingStep.FINISH -> "Your First Rune"
    }
    val subtitle = when (currentStep) {
        OnboardingStep.WELCOME -> "Ancient wisdom rendered in runic scripts."
        OnboardingStep.SCRIPT -> "Select a default runic writing system. You can always switch later."
        OnboardingStep.FINISH -> {
            "Here's your first quote, translated into ${selectedScriptLabel(selectedScript)}."
        }
    }

    Column(
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 40.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun WelcomeFeatures() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 20.dp, end = 20.dp, top = 28.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FeatureRow(
            icon = Icons.Default.DateRange,
            title = "Daily rune wisdom",
            description = "A new rune quote each day in Elder Futhark, Younger Futhark, or Cirth."
        )
        FeatureRow(
            icon = Icons.Default.Create,
            title = "Create your own",
            description = "Write quotes and see them in ancient scripts."
        )
        FeatureRow(
            icon = Icons.AutoMirrored.Filled.Send,
            title = "Share as art",
            description = "Export share cards in light or dark themes."
        )
    }
}

@Composable
private fun FeatureRow(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(18.dp)
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ScriptSelectionList(
    stories: List<ScriptStory>,
    selectedScript: RunicScript,
    onSelect: (ScriptStory) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 20.dp, end = 20.dp, top = 18.dp)
            .selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        stories.forEach { story ->
            ScriptOptionCard(
                story = story,
                selected = selectedScript == story.script,
                onSelect = { onSelect(story) }
            )
        }
    }
}

@Composable
private fun ScriptOptionCard(
    story: ScriptStory,
    selected: Boolean,
    onSelect: () -> Unit
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.surfaceContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.outlineVariant
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
    }
    val runicColor = if (selected) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = MaterialTheme.shapes.large,
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .height(126.dp)
            .selectable(
                selected = selected,
                onClick = onSelect,
                role = Role.RadioButton
            )
            .testTag("onboarding_${story.script.name.lowercase()}_card")
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .width(3.dp)
                        .height(100.dp)
                        .align(Alignment.TopStart)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 17.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = story.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    SelectionIndicator(selected = selected)
                }

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    text = story.era,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.45f))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RunicText(
                            text = story.sampleRunes,
                            script = story.script,
                            color = runicColor,
                            role = RunicTextRole.OnboardingSample
                        )
                        Text(
                            text = "\"${story.sampleLatin}\"",
                            style = MaterialTheme.typography.labelMedium,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectionIndicator(selected: Boolean) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(MaterialTheme.shapes.extraLarge)
                .background(Color.Transparent)
                .padding(0.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(MaterialTheme.shapes.extraLarge)
                .background(Color.Transparent)
        )
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = MaterialTheme.shapes.extraLarge,
            color = Color.Transparent,
            border = BorderStroke(
                width = 2.dp,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline
                }
            )
        ) {}
        if (selected) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .align(Alignment.Center)
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(MaterialTheme.colorScheme.primary)
                )
        }
    }
}

@Composable
private fun FirstRuneCard(selectedStory: ScriptStory) {
    val revealRunes = remember(selectedStory.script) {
        onboardingTransliterationFactory.transliterate(ONBOARDING_REVEAL_QUOTE, selectedStory.script)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 18.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                RunicText(
                    text = revealRunes,
                    script = selectedStory.script,
                    color = MaterialTheme.colorScheme.onSurface,
                    role = RunicTextRole.OnboardingReveal
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                )
                Text(
                    text = "\"$ONBOARDING_REVEAL_QUOTE\"",
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "- $ONBOARDING_REVEAL_AUTHOR",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = selectedScriptLabel(selectedStory.script),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
                )
            }
        }
    }
}

@Composable
private fun OnboardingFooter(
    currentStep: OnboardingStep,
    onAction: () -> Unit
) {
    val buttonText = when (currentStep) {
        OnboardingStep.WELCOME -> "Get Started"
        OnboardingStep.SCRIPT -> "Continue"
        OnboardingStep.FINISH -> "Enter Runatal"
    }
    val testTag = when (currentStep) {
        OnboardingStep.WELCOME -> "onboarding_next_button"
        OnboardingStep.SCRIPT -> "onboarding_next_button"
        OnboardingStep.FINISH -> "onboarding_finish_button"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
        OnboardingProgressIndicator(currentStep = currentStep)
        Button(
            onClick = onAction,
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(48.dp)
                .testTag(testTag)
        ) {
            Text(
                text = buttonText,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun OnboardingProgressIndicator(currentStep: OnboardingStep) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 21.dp, bottom = 22.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier.width(56.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OnboardingProgressDot(active = currentStep == OnboardingStep.WELCOME)
            OnboardingProgressDot(active = currentStep == OnboardingStep.SCRIPT)
            OnboardingProgressDot(active = currentStep == OnboardingStep.FINISH)
        }
    }
}

@Composable
private fun RowScope.OnboardingProgressDot(active: Boolean) {
    if (active) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(MaterialTheme.shapes.extraLarge)
                .background(MaterialTheme.colorScheme.primary)
        )
    } else {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(MaterialTheme.shapes.extraLarge)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        )
    }
}

private data class ScriptStory(
    val script: RunicScript,
    val title: String,
    val era: String,
    val sampleRunes: String,
    val sampleLatin: String,
    val suggestedThemePack: String
)

private val SCRIPT_STORIES = listOf(
    ScriptStory(
        script = RunicScript.ELDER_FUTHARK,
        title = "Elder Futhark",
        era = "24 runes · Germanic tribes · 2nd-8th century",
        sampleRunes = "ᚹᛁᛊᛞᛟᛗ",
        sampleLatin = "Wisdom",
        suggestedThemePack = "stone"
    ),
    ScriptStory(
        script = RunicScript.YOUNGER_FUTHARK,
        title = "Younger Futhark",
        era = "16 runes · Viking Age · 9th-11th century",
        sampleRunes = "ᚢᛁᛋᛏᚢᛘ",
        sampleLatin = "Wisdom",
        suggestedThemePack = "night_ink"
    ),
    ScriptStory(
        script = RunicScript.CIRTH,
        title = "Cirth (Angerthas)",
        era = "Tolkien's rune system · Literary",
        sampleRunes = "\uE0B8\uE0C8\uE09C\uE089\uE0CC\uE0B0",
        sampleLatin = "Wisdom",
        suggestedThemePack = "parchment"
    )
)

private const val ONBOARDING_REVEAL_QUOTE = "Not all those who wander are lost."
private const val ONBOARDING_REVEAL_AUTHOR = "J.R.R. Tolkien"

private val onboardingTransliterationFactory = TransliterationFactory(
    elderFutharkTransliterator = ElderFutharkTransliterator(),
    youngerFutharkTransliterator = YoungerFutharkTransliterator(),
    cirthTransliterator = CirthTransliterator()
)

private fun selectedScriptLabel(script: RunicScript): String {
    return when (script) {
        RunicScript.ELDER_FUTHARK -> "Elder Futhark"
        RunicScript.YOUNGER_FUTHARK -> "Younger Futhark"
        RunicScript.CIRTH -> "Cirth"
    }
}
