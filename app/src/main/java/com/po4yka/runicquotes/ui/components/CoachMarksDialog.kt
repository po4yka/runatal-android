package com.po4yka.runicquotes.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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

/** A single step in a coach marks tour. */
data class CoachMarkStep(
    val number: Int,
    val title: String,
    val description: String
)

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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier.fillMaxWidth().padding(horizontal = 38.dp),
            shape = RunicExpressiveTheme.shapes.dialog,
            tonalElevation = RunicExpressiveTheme.elevations.overlay,
            color = colors.surface
        ) {
            Column(
                modifier = Modifier.padding(start = 25.dp, end = 25.dp, top = 29.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AnimatedContent(
                    targetState = step,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "coachStep"
                ) { current ->
                    CoachStepContent(current, colors)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Step ${currentIndex + 1} of ${steps.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(41.dp)
                    ) { Text(text = "Skip Tour") }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = { if (isLast) onDismiss() else currentIndex++ },
                        modifier = Modifier.weight(1f).height(41.dp),
                        shape = RunicExpressiveTheme.shapes.segmentedControl
                    ) { Text(text = if (isLast) "Done" else "Next") }
                }
            }
        }
    }
}

@Composable
private fun CoachStepContent(
    step: CoachMarkStep,
    colors: androidx.compose.material3.ColorScheme
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(modifier = Modifier.size(28.dp), shape = CircleShape, color = colors.primaryContainer) {
            Text(
                text = step.number.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = colors.onPrimaryContainer,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = step.title,
            style = MaterialTheme.typography.titleMedium,
            color = colors.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = step.description,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
