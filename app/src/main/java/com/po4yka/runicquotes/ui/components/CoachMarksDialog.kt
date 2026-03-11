package com.po4yka.runicquotes.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.po4yka.runicquotes.ui.theme.RunicExpressiveTheme

/** Step-by-step feature tour dialog with skip/next navigation. */
@Composable
fun CoachMarksDialog(
    steps: List<CoachMarkStep>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentIndex by remember { mutableIntStateOf(0) }
    val step = steps[currentIndex]
    val isLast = currentIndex == steps.lastIndex
    val colors = MaterialTheme.colorScheme
    val controls = RunicExpressiveTheme.controls

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RunicExpressiveTheme.shapes.dialog,
                tonalElevation = RunicExpressiveTheme.elevations.overlay,
                color = colors.surfaceContainerLow,
                border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.62f))
            ) {
                Column(
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AnimatedContent(
                        targetState = step,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "coachStep"
                    ) { current ->
                        CoachStepContent(current, colors)
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    CoachStepDots(
                        total = steps.size,
                        currentIndex = currentIndex
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f).height(controls.dialogActionHeight)
                        ) {
                            Text(text = "Skip")
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Button(
                            onClick = { if (isLast) onDismiss() else currentIndex++ },
                            modifier = Modifier.weight(1f).height(controls.dialogActionHeight),
                            shape = RunicExpressiveTheme.shapes.segmentedControl,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.secondary,
                                contentColor = colors.onSecondary
                            )
                        ) {
                            Text(
                                text = if (isLast) "Done" else "Next",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
    }
}

/** A single step in a coach marks tour. */
data class CoachMarkStep(
    val number: Int,
    val title: String,
    val description: String
)

@Composable
private fun CoachStepContent(
    step: CoachMarkStep,
    colors: androidx.compose.material3.ColorScheme
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            modifier = Modifier.size(32.dp),
            shape = CircleShape,
            color = colors.secondaryContainer.copy(alpha = 0.55f)
        ) {
            Text(
                text = step.number.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = colors.onSecondaryContainer,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = step.title,
            style = MaterialTheme.typography.titleLarge,
            color = colors.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = step.description,
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun CoachStepDots(total: Int, currentIndex: Int) {
    val colors = MaterialTheme.colorScheme

    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(total) { index ->
            Surface(
                modifier = Modifier.size(width = if (index == currentIndex) 18.dp else 6.dp, height = 6.dp),
                shape = RunicExpressiveTheme.shapes.pill,
                color = if (index == currentIndex) {
                    colors.secondary
                } else {
                    colors.outlineVariant.copy(alpha = 0.8f)
                }
            ) {}
        }
    }
}
