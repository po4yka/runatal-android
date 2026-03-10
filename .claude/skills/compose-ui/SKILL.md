---
name: compose-ui
description: "Guide for writing, refactoring, and reviewing Jetpack Compose UI code in the Runic Quotes app. Use when: (1) building or modifying Compose composables, (2) adding animations, (3) fixing recomposition or performance issues, (4) adding accessibility, (5) working with Glance widgets, (6) reviewing Compose code quality. Triggers on: compose UI, composable, animation, recomposition, accessibility, widget, performance, Modifier."
---

# Compose UI Guide

Rules and patterns for writing Compose UI in the Runatal codebase.
For scaffolding new screens (route + ViewModel + NavGraph), see the `compose-screen` skill instead.

## Reference Files

Load on demand when you need detailed lookup tables or extended code examples:

| File | Content |
|------|---------|
| `references/theme-tokens.md` | Full token tables: shapes, elevations, motion, type roles, locals |
| `references/animation-patterns.md` | 5 animation recipes with reduced-motion fallbacks |
| `references/antipatterns.md` | 8 before/after mistake pairs |

## 1. Writing New Composables

### Parameter Order Convention

Follow this parameter order consistently:

```kotlin
@Composable
fun RunicCard(
    // 1. Required data
    text: String,
    author: String,
    // 2. Event callbacks
    onClick: () -> Unit,
    onFavorite: () -> Unit,
    // 3. Modifier (always has default)
    modifier: Modifier = Modifier,
    // 4. Optional configuration with defaults
    style: TextStyle = RunicTypeRoles.current.runicCard,
    isSelected: Boolean = false,
    // 5. Slot content (trailing lambda)
    content: @Composable () -> Unit = {}
)
```

### Naming and Structure

- PascalCase for composable functions: `HeroQuoteCard`, `SegmentedControl`
- `private` for composables used only within the same file
- Stateless by default: composables receive state, emit events via callbacks
- KDoc required for public composables (detekt enforces this)
- One public composable per file for components; private helpers in same file

### Theme Token Access

```kotlin
@Composable
fun MyComponent() {
    val shapes = RunicExpressiveTheme.shapes      // RunicShapeTokens
    val elevations = RunicExpressiveTheme.elevations  // RunicElevationTokens
    val motion = RunicExpressiveTheme.motion       // RunicMotionTokens
    val typeRoles = RunicTypeRoles.current         // RunicExpressiveTypography
    val reducedMotion = LocalReduceMotion.current  // Boolean

    Card(
        shape = shapes.contentCard,                // NOT RoundedCornerShape(20.dp)
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh  // NOT Color(...)
        )
    ) { /* ... */ }
}
```

Rules:
- Always use `RunicExpressiveTheme.shapes.*` -- never hardcode `RoundedCornerShape()`
- Always use `MaterialTheme.colorScheme.*` -- never use raw `Color()` literals
- Use `RunicTypeRoles.current.*` for runic/quote text styles
- Use `MaterialTheme.typography.*` for standard UI text (labels, headers)

### Modifier Convention

- First Modifier parameter always named `modifier` with default `Modifier`
- Apply `modifier` to the outermost container only
- Chain internal modifiers separately -- do not append to the passed-in `modifier`
- Use `Modifier.fillMaxWidth()` as default for full-width components

## 2. State Management

### When to Use What

| Mechanism | Use Case | Example |
|-----------|----------|---------|
| `ViewModel` + `StateFlow` | Business logic, data from repository | Quote list, loading state, errors |
| `remember { mutableStateOf() }` | Transient UI state | `showBottomSheet`, `cardVisible` |
| `remember(key) { computation }` | Derived/computed values | `normalizedText`, `todayDate` |
| `derivedStateOf` | Expensive derivation from other state | Filtered list from large collection |
| `rememberSaveable` | UI state surviving config changes | Scroll position, expanded state |

### ViewModel State Pattern

```kotlin
// In ViewModel
private val _uiState = MutableStateFlow(FeatureUiState())
val uiState: StateFlow<FeatureUiState> = _uiState.asStateFlow()

// Thread-safe updates
_uiState.update { it.copy(isLoading = true) }
```

### Collecting State in Compose

```kotlin
val uiState by viewModel.uiState.collectAsStateWithLifecycle()
```

Always use `collectAsStateWithLifecycle()` -- never plain `collectAsState()`.

## 3. Performance

### Recomposition Control

- Provide keys to `remember`: `remember(text, script) { ... }`
- Use `key()` in loops: `items.forEachIndexed { i, item -> key(i, item.id) { ... } }`
- Mark data classes with `@Immutable` or `@Stable` when they hold only vals
- Prefer `ImmutableList` over `List` for composable parameters
- Keep composable scope small: extract private composables to limit recomposition blast radius

### Deferred State Reads

For frequently-changing values (scroll, drag, animation), defer the read:

```kotlin
// BAD: recomposes on every pixel
Modifier.offset(x = scrollOffset.value.dp)

// GOOD: read deferred to layout phase
Modifier.offset { IntOffset(scrollOffset.value, 0) }
```

### Shared Resources

Share expensive objects across siblings:

```kotlin
// Share a single shimmer brush across all skeleton components
val brush = rememberShimmerBrush()
SkeletonRect(brush = brush)
SkeletonCard(brush = brush)
SkeletonTextBlock(brush = brush)
```

## 4. Animation

All animations must use theme motion tokens and support reduced motion.
See `references/animation-patterns.md` for 5 detailed recipes.

### Motion Token Usage

```kotlin
val reducedMotion = LocalReduceMotion.current
val motion = RunicExpressiveTheme.motion

// Duration that respects reduced motion
val duration = motion.duration(reducedMotion, motion.shortDurationMillis)  // 220 or 0
val delay = motion.delay(reducedMotion, motion.revealStepMillis)          // 42 or 0
```

### Duration Selection

| Duration | Value | Use |
|----------|-------|-----|
| `shortDurationMillis` | 220ms | Color transitions, toggles |
| `mediumDurationMillis` | 420ms | Enter/exit, fade, slide |
| `longDurationMillis` | 620ms | Character reveal, complex sequences |

### Easing Selection

| Easing | Token | Use |
|--------|-------|-----|
| `emphasizedEasing` | `FastOutSlowInEasing` | Enter animations, emphasis |
| `standardEasing` | `LinearOutSlowInEasing` | Standard transitions |

### Reduced Motion Pattern

Every animation must handle `LocalReduceMotion.current`:

```kotlin
if (reducedMotion) {
    EnterTransition.None  // For AnimatedVisibility
    // OR: instant snap (duration = 0 via motion.duration())
    // OR: early return rendering final state directly
}
```

### Animation Label

Always provide a `label` parameter to animation APIs for debugging:

```kotlin
animateColorAsState(targetValue = color, label = "cardBackground")
```

## 5. Accessibility

### Reduced Motion

- Read `LocalReduceMotion.current` at the top of any composable with animation
- Pass through `motion.duration(reducedMotion, base)` -- returns 0 when reduced
- For complex animations (character reveal), early-return with static rendering

### Semantics

```kotlin
// Tab/selection semantics (from SegmentedControl.kt)
Modifier.semantics {
    role = Role.Tab
    selected = isSelected
}

// Merge children for screen readers
Modifier.semantics(mergeDescendants = true) { }
```

### Content Descriptions

- All actionable icons MUST have `contentDescription`
- Decorative-only icons may use `contentDescription = null`
- Use descriptive text: "Share quote", "Remove from saved", "New random quote"
- Toggle states: change description based on state (see `ActionButtonsRow`)

### Font Scaling

- Runic text respects `LocalRunicFontScale` via `RunicText` component
- All font sizes use `sp` units (never `dp` for text)
- Script-specific sizing handled by `RunicText` internally

### Haptics

```kotlin
val haptics = rememberHapticFeedback()
haptics.lightToggle()   // Selection changes, favorites toggle
haptics.mediumAction()  // Share, refresh, delete
```

## 6. Refactoring Guide

### Extract a Composable

When a composable exceeds ~80 lines or has distinct logical sections:

1. Identify the section with its own data/event boundary
2. Extract as `private` composable in the same file
3. Pass only needed state (not entire ViewModel or UiState)
4. Pass events as `() -> Unit` callbacks

### Fix Recomposition Issue

1. Run Layout Inspector with "Show recomposition counts"
2. Identify composables recomposing more than expected
3. Common fixes:
   - Add missing `remember` keys
   - Extract lambda: `val onClick = remember { { viewModel.doThing() } }`
   - Use `@Stable`/`@Immutable` on data classes
   - Split composable to limit recomposition scope

### Hoist State

Move state from inner composable to caller:

```kotlin
// Before: state trapped inside
@Composable fun ScriptSelector() {
    var selected by remember { mutableStateOf(0) }
    // ...
}

// After: state hoisted
@Composable fun ScriptSelector(
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
)
```

### Add Loading Skeleton

1. Create a `*LoadingSkeleton` private composable matching the loaded layout
2. Use `rememberShimmerBrush()` -- share one brush across all skeleton elements
3. Use `SkeletonRect`, `SkeletonCard`, `SkeletonTextBlock`, `SkeletonCircle`
4. Match dimensions of the real content for seamless transition
5. Reduced motion: shimmer brush automatically returns flat color

### Convert to Slot API

Replace fixed content with a composable lambda:

```kotlin
// Before: fixed icon
@Composable fun ActionButton(icon: ImageVector, label: String, onClick: () -> Unit)

// After: slot API
@Composable fun ActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit = {}
)
```

## 7. Glance Widget

The widget uses Glance (not regular Compose). See `ui/widget/RunicQuoteWidget.kt`.

### Key Differences from Regular Compose

| Regular Compose | Glance |
|----------------|--------|
| `Modifier` | `GlanceModifier` |
| `MaterialTheme` | Not available -- use `WidgetPalette` |
| `remember`, `LaunchedEffect` | Not available |
| `animateXAsState` | Not available -- no animation |
| `Text`, `Icon` | `androidx.glance.text.Text`, `Image` |

### Glance Rules

- Use `GlanceModifier` exclusively -- never `Modifier`
- No Material3 components: build from `Column`, `Row`, `Text`, `Image`
- No `remember` or `mutableStateOf` -- state comes from `provideGlance()`
- Pre-render runic text to bitmap via `RunicTextRenderer` + `BitmapCache`
- Use `ColorProvider(Color(intColor))` for colors from `WidgetPalette`
- Size-responsive: check `WidgetSizeClass` (COMPACT/MEDIUM/EXPANDED)
- All text uses `sp` via `TextStyle(fontSize = N.sp)`

### Widget Size Classes

| Class | Width | Height | Content |
|-------|-------|--------|---------|
| COMPACT | <150 | <120 | Minimal: runic text + action |
| MEDIUM | 150-250 | 120-180 | + Latin text + context badge |
| EXPANDED | >250 | >180 | + Author + all badges |

## 8. Quick Checklist

Run through before submitting Compose UI changes:

### Modifier
- [ ] First `modifier` parameter has default `Modifier`
- [ ] `modifier` applied to outermost container only
- [ ] No hardcoded shapes -- use `RunicExpressiveTheme.shapes.*`
- [ ] No raw `Color()` -- use `MaterialTheme.colorScheme.*`

### Icons and Accessibility
- [ ] Actionable icons have `contentDescription`
- [ ] Toggle icons change `contentDescription` based on state
- [ ] Interactive elements have semantics roles where appropriate

### Animation
- [ ] Uses `motion.duration(reducedMotion, base)` -- not hardcoded millis
- [ ] Complex animations early-return static content when `reducedMotion`
- [ ] Animation APIs have `label` parameter
- [ ] Uses `motion.emphasizedEasing` or `motion.standardEasing`

### Performance
- [ ] `remember` calls have all relevant keys
- [ ] `key()` used in loops/`forEach`
- [ ] No side effects during composition (wrapped in `LaunchedEffect`)
- [ ] Shared brush/resources across skeleton siblings

### State
- [ ] Business state in ViewModel, not `remember`
- [ ] Using `collectAsStateWithLifecycle()` for Flow collection
- [ ] Thread-safe `_uiState.update { }` in ViewModel

### Detekt
- [ ] `./gradlew detekt` passes with 0 issues
- [ ] KDoc on public composables
- [ ] 120-char line limit
