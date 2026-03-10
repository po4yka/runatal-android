# Compose Antipatterns

8 common mistakes with before/after fixes specific to the Runatal codebase.

## 1. Unstable List Parameter

```kotlin
// BAD: List<String> is not stable -- causes unnecessary recomposition
@Composable
fun QuoteList(items: List<String>) { ... }

// GOOD: Use ImmutableList from kotlinx.collections.immutable
@Composable
fun QuoteList(items: ImmutableList<String>) { ... }

// ALT: Wrap in @Immutable data class if ImmutableList is not available
@Immutable
data class QuoteItems(val items: List<String>)
```

## 2. Missing `remember` Keys

```kotlin
// BAD: Stale computation when inputs change
val normalizedText = remember { CirthGlyphCompat.normalizeLegacyPuaGlyphs(text) }

// GOOD: Recompute when text or script changes (from RunicText.kt)
val normalizedText = remember(text, script) {
    if (script == RunicScript.CIRTH) {
        CirthGlyphCompat.normalizeLegacyPuaGlyphs(text)
    } else {
        text
    }
}
```

## 3. Side Effect in Composition

```kotlin
// BAD: Direct call during composition
@Composable
fun QuoteCard(viewModel: QuoteViewModel) {
    viewModel.trackView()  // Called on every recomposition!
    Text(text = "...")
}

// GOOD: Wrap in LaunchedEffect with proper key
@Composable
fun QuoteCard(quoteId: Long, onTrackView: (Long) -> Unit) {
    LaunchedEffect(quoteId) {
        onTrackView(quoteId)
    }
    Text(text = "...")
}
```

## 4. Backwards Write

```kotlin
// BAD: Writing state during composition triggers recomposition loop
@Composable
fun BadCounter(count: MutableState<Int>) {
    count.value = count.value + 1  // Backwards write!
    Text("Count: ${count.value}")
}

// GOOD: Use LaunchedEffect or event callback
@Composable
fun GoodCounter(onIncrement: () -> Unit) {
    LaunchedEffect(Unit) { onIncrement() }
}
```

## 5. Hardcoded Animation Duration

```kotlin
// BAD: Ignores theme tokens and reduced motion
animateColorAsState(
    targetValue = color,
    animationSpec = tween(durationMillis = 300)
)

// GOOD: Use motion tokens with reduced motion support
val reducedMotion = LocalReduceMotion.current
val motion = RunicExpressiveTheme.motion
animateColorAsState(
    targetValue = color,
    animationSpec = tween(
        durationMillis = motion.duration(reducedMotion, motion.shortDurationMillis)
    )
)
```

## 6. Missing contentDescription on Actionable Icon

```kotlin
// BAD: No accessibility info
IconButton(onClick = onShare) {
    Icon(imageVector = Icons.Default.Share, contentDescription = null)
}

// GOOD: Descriptive contentDescription (from QuoteScreen.kt)
IconButton(onClick = onShare) {
    Icon(
        imageVector = Icons.Default.Share,
        contentDescription = "Share quote"
    )
}

// Decorative-only icons (not actionable) may use null contentDescription
```

## 7. Business State in `remember`

```kotlin
// BAD: Business logic state managed in composition
@Composable
fun QuoteScreen() {
    var quotes by remember { mutableStateOf(emptyList<Quote>()) }
    var isLoading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        quotes = repository.getQuotes()
        isLoading = false
    }
}

// GOOD: ViewModel owns business state (from QuoteScreen.kt)
@Composable
fun QuoteScreen(viewModel: QuoteViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // Only use remember for local UI state like showBottomSheet, cardVisible
}
```

`remember { mutableStateOf() }` is fine for transient UI state only (dialog visibility, scroll position).

## 8. Eager State Read in Composition

```kotlin
// BAD: Reads state eagerly -- recomposes entire scope on every change
Box(modifier = Modifier.offset(x = offsetState.value.dp, y = 0.dp))

// GOOD: Lambda modifier defers read to layout phase
Box(modifier = Modifier.offset { IntOffset(offsetState.value, 0) })
```

This matters for frequently-changing values (scroll offsets, drag position, animation progress).
