# Animation Patterns

5 reusable animation patterns from the Runatal codebase. Each includes a reduced-motion fallback.

## 1. Color Transition

From `SegmentedControl.kt` -- animate between two colors on state change.

```kotlin
val reducedMotion = LocalReduceMotion.current
val motion = RunicExpressiveTheme.motion
val animDuration = motion.duration(reducedMotion, motion.shortDurationMillis)

val backgroundColor by animateColorAsState(
    targetValue = if (isSelected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    },
    animationSpec = tween(durationMillis = animDuration),
    label = "segmentBackground"
)
```

Reduced motion: `motion.duration()` returns 0, so the snap is instant.

## 2. Enter Animation

From `QuoteScreen.kt` -- fade + slide for card entrance.

```kotlin
val reducedMotion = LocalReduceMotion.current
val motion = RunicExpressiveTheme.motion

AnimatedVisibility(
    visible = cardVisible,
    enter = if (reducedMotion) {
        EnterTransition.None
    } else {
        fadeIn(
            animationSpec = tween(
                durationMillis = motion.duration(reducedMotion, motion.mediumDurationMillis),
                easing = motion.standardEasing
            )
        ) + slideInVertically(
            animationSpec = tween(
                durationMillis = motion.duration(reducedMotion, motion.mediumDurationMillis),
                easing = motion.emphasizedEasing
            ),
            initialOffsetY = { it / 6 }
        )
    }
) {
    // Content
}
```

Key: use `EnterTransition.None` when `reducedMotion` is true.

## 3. Character Reveal (Staggered)

From `QuoteScreen.kt:HeroRunicText` -- per-character fade-in with stagger.

```kotlin
// Early return for reduced motion -- render all characters instantly
if (reducedMotion) {
    RunicText(text = text, /* ... */)
    return
}

val words = remember(text) { text.split(" ") }

words.forEachIndexed { wordIndex, word ->
    key(wordIndex, word, selectedScript) {
        Row {
            word.forEachIndexed { index, char ->
                val alpha = remember(word, char, selectedScript) { Animatable(0f) }

                LaunchedEffect(char, selectedScript) {
                    val wordStartDelay = wordIndex * word.length * motion.revealStepMillis
                    val charDelay = index * motion.revealStepMillis + wordStartDelay
                    alpha.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = motion.longDurationMillis,
                            delayMillis = charDelay.coerceAtMost(motion.maxRevealDelayMillis),
                            easing = motion.standardEasing
                        )
                    )
                }

                RunicText(
                    text = char.toString(),
                    modifier = Modifier.alpha(alpha.value),
                    /* ... */
                )
            }
        }
    }
}
```

Key: `coerceAtMost(motion.maxRevealDelayMillis)` caps total delay at 1600ms.

## 4. Alpha Fade

From `QuoteScreen.kt:HeroQuoteCard` -- delayed fade for secondary content.

```kotlin
val transliterationAlpha by animateFloatAsState(
    targetValue = 1f,
    animationSpec = tween(
        durationMillis = motion.duration(reducedMotion, motion.mediumDurationMillis),
        delayMillis = motion.delay(reducedMotion, motion.shortDurationMillis),
        easing = motion.standardEasing
    ),
    label = "transliterationAlpha"
)

Text(
    text = "\"$latinText\"",
    modifier = Modifier.alpha(transliterationAlpha)
)
```

Reduced motion: both duration and delay become 0, so alpha snaps to 1f instantly.

## 5. Shimmer Skeleton

From `LoadingSkeleton.kt` -- infinite shimmer sweep for loading placeholders.

```kotlin
val reducedMotion = LocalReduceMotion.current
if (reducedMotion) {
    // Flat color, no animation
    return Brush.linearGradient(listOf(baseColor, baseColor))
}

val transition = rememberInfiniteTransition(label = "shimmer")
val translateX by transition.animateFloat(
    initialValue = -400f,
    targetValue = 400f,
    animationSpec = infiniteRepeatable(
        animation = tween(
            durationMillis = RunicExpressiveTheme.motion.longDurationMillis * 2,  // 1240ms
            easing = LinearEasing
        )
    ),
    label = "shimmerTranslateX"
)

Brush.linearGradient(
    colors = listOf(baseColor, highlightColor, baseColor),
    start = Offset(translateX, 0f),
    end = Offset(translateX + 400f, 0f)
)
```

Key: share a single `rememberShimmerBrush()` across sibling skeletons for synchronized animation.
