package com.po4yka.runicquotes.ui.screens.onboarding

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
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
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            OnboardingBottomButton(
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OnboardingStepHeader(currentStep = onboardingStep, selectedScript = selectedScript)

            Crossfade(
                targetState = onboardingStep,
                animationSpec = tween(
                    durationMillis = motion.duration(
                        reducedMotion = reducedMotion,
                        base = motion.mediumDurationMillis
                    ),
                    easing = motion.emphasizedEasing
                ),
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

private enum class OnboardingStep {
    WELCOME,
    SCRIPT,
    FINISH
}

@Composable
private fun OnboardingStepHeader(currentStep: OnboardingStep, selectedScript: RunicScript) {
    val title = when (currentStep) {
        OnboardingStep.WELCOME -> "Welcome to Runatal"
        OnboardingStep.SCRIPT -> "Choose Your Script"
        OnboardingStep.FINISH -> "Your First Rune"
    }
    val subtitle = when (currentStep) {
        OnboardingStep.WELCOME -> "Ancient wisdom rendered in runic scripts."
        OnboardingStep.SCRIPT -> "Select a default runic writing system. You can always switch later."
        OnboardingStep.FINISH -> "Here's your first quote, translated into " +
            "${selectedScriptLabel(selectedScript)}."
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun WelcomeFeatures() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        FeatureRow(
            icon = Icons.Default.DateRange,
            title = "Daily rune wisdom",
            description = "A new runic quote each day in Elder Futhark, Younger Futhark, or Cirth."
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
private fun FeatureRow(icon: ImageVector, title: String, description: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FirstRuneCard(selectedStory: ScriptStory) {
    val shapes = RunicExpressiveTheme.shapes
    val typeRoles = RunicTypeRoles.current
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Surface(
            shape = shapes.contentCard,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                RunicText(
                    text = selectedStory.sampleRunes,
                    script = selectedStory.script,
                    style = typeRoles.runicHero,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "\"${selectedStory.sampleLatin}\"",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = selectedStory.story,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            text = selectedScriptLabel(selectedStory.script),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun OnboardingBottomButton(
    currentStep: OnboardingStep,
    onAction: () -> Unit
) {
    val shapes = RunicExpressiveTheme.shapes
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

    Button(
        onClick = onAction,
        shape = shapes.contentCard,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .height(52.dp)
            .testTag(testTag)
    ) {
        Text(text = buttonText)
    }
}

@Composable
private fun ScriptSelectionList(
    stories: List<ScriptStory>,
    selectedScript: RunicScript,
    onSelect: (ScriptStory) -> Unit
) {
    Column(
        modifier = Modifier.selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
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
    val shapes = RunicExpressiveTheme.shapes
    val typeRoles = RunicTypeRoles.current
    Surface(
        shape = shapes.contentCard,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = if (selected) 2.dp else 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onSelect,
                role = Role.RadioButton
            )
            .testTag("onboarding_${story.script.name.lowercase()}_card")
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = story.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = story.era,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                RunicText(
                    text = story.sampleRunes,
                    script = story.script,
                    style = typeRoles.runicCard,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "*${story.sampleLatin}*",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            RadioButton(selected = selected, onClick = null)
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

private val SCRIPT_STORIES = listOf(
    ScriptStory(
        script = RunicScript.ELDER_FUTHARK,
        title = "Elder Futhark",
        era = "24 runes - Germanic & East - 2nd-8th century",
        sampleRunes = "ᚨᚾᛞ ᛏᚺᛖ ᚱᚢᚾᛖᛋ",
        sampleLatin = "and the runes",
        story = "Crisp, archaeological letterforms that feel traditional and grounded.",
        suggestedThemePack = "stone"
    ),
    ScriptStory(
        script = RunicScript.YOUNGER_FUTHARK,
        title = "Younger Futhark",
        era = "16 runes - Viking Age - 9th-11th century",
        sampleRunes = "ᚢᛁᚴᛁᚾᚴ ᛊᛏᛁᛚ",
        sampleLatin = "Viking style",
        story = "Compact and direct forms that read cleanly in short, punchy quotes.",
        suggestedThemePack = "night_ink"
    ),
    ScriptStory(
        script = RunicScript.CIRTH,
        title = "Cirth (Angerthas)",
        era = "Tolkien's rune system - Literary",
        sampleRunes = "\uE088\uE0B4\uE0CB\uE09C \uE0B8\uE0CA\uE0A8\uE0A8",
        sampleLatin = "Not all who wander",
        story = "Literary and atmospheric, built for a fantasy-forward reading style.",
        suggestedThemePack = "parchment"
    )
)

private fun selectedScriptLabel(script: RunicScript): String {
    return when (script) {
        RunicScript.ELDER_FUTHARK -> "Elder Futhark"
        RunicScript.YOUNGER_FUTHARK -> "Younger Futhark"
        RunicScript.CIRTH -> "Cirth"
    }
}

