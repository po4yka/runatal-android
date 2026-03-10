# Theme Token Reference

Source of truth: `ui/theme/ExpressiveTokens.kt`, `ui/theme/Type.kt`, `ui/theme/Theme.kt`

## Access Patterns

```kotlin
// Shapes, elevations, motion
val shapes = RunicExpressiveTheme.shapes
val elevations = RunicExpressiveTheme.elevations
val motion = RunicExpressiveTheme.motion

// Type roles (runic-specific typography)
val typeRoles = RunicTypeRoles.current

// Composition locals
val reducedMotion = LocalReduceMotion.current
val runicFontScale = LocalRunicFontScale.current
val expressiveType = LocalRunicExpressiveType.current

// Colors -- always via MaterialTheme
MaterialTheme.colorScheme.primary
MaterialTheme.colorScheme.surfaceContainerHigh
// NEVER use raw Color(0xFF...) literals
```

## Shape Tokens (`RunicExpressiveTheme.shapes`)

| Token | Value | Use |
|-------|-------|-----|
| `heroCard` | `RoundedCornerShape(28.dp)` | Featured/hero cards |
| `contentCard` | `RoundedCornerShape(20.dp)` | Standard content cards |
| `collectionCard` | `RoundedCornerShape(16.dp)` | Grid/collection items |
| `panel` | `RoundedCornerShape(24.dp)` | Side/bottom panels |
| `bottomSheet` | `RoundedCornerShape(topStart=28.dp, topEnd=28.dp)` | Bottom sheets |
| `segmentedControl` | `RoundedCornerShape(16.dp)` | Outer segmented control |
| `segment` | `RoundedCornerShape(12.dp)` | Individual segments, action buttons |
| `dialog` | `RoundedCornerShape(28.dp)` | Dialogs |
| `skeleton` | `RoundedCornerShape(12.dp)` | Loading skeleton placeholders |
| `pill` | `RoundedCornerShape(9.dp)` | Small badges/pills |

## Elevation Tokens (`RunicExpressiveTheme.elevations`)

| Token | Value | Use |
|-------|-------|-----|
| `flat` | `0.dp` | Flat surfaces, backgrounds |
| `card` | `3.dp` | Standard cards |
| `raisedCard` | `8.dp` | Emphasized cards |
| `pressedCard` | `10.dp` | Pressed/active card state |
| `overlay` | `16.dp` | Dialogs, bottom sheets |

## Motion Tokens (`RunicExpressiveTheme.motion`)

| Token | Value | Use |
|-------|-------|-----|
| `shortDurationMillis` | `220` | Quick transitions (color, toggle) |
| `mediumDurationMillis` | `420` | Enter/exit, fades, slides |
| `longDurationMillis` | `620` | Character reveal, shimmer base |
| `revealStepMillis` | `42` | Per-character stagger delay |
| `maxRevealDelayMillis` | `1600` | Cap on total reveal delay |
| `emphasizedEasing` | `FastOutSlowInEasing` | Enter animations, emphasis |
| `standardEasing` | `LinearOutSlowInEasing` | Standard transitions |

### Helper Methods

```kotlin
// Returns 0 when reducedMotion is true, otherwise returns base
motion.duration(reducedMotion, motion.shortDurationMillis)
motion.delay(reducedMotion, motion.revealStepMillis)
```

## Type Roles (`RunicTypeRoles.current`)

| Role | Base Style | Use |
|------|-----------|-----|
| `runicHero` | `displayMedium` (Bold) | Hero card runic text |
| `runicCard` | `headlineSmall` (SemiBold) | Card runic text |
| `runicCollection` | `titleLarge` (SemiBold) | Collection/grid runic text |
| `latinQuote` | `bodyLarge` (letterSpacing 0.45sp) | Latin quote body text |
| `quoteMeta` | `labelLarge` (letterSpacing 0.2sp) | Author name, metadata |

Type roles adjust per theme pack (stone/parchment/night_ink).

## Runic Script Font Sizing

| Script | Base Size | Letter Spacing | Line Height |
|--------|-----------|---------------|-------------|
| `ELDER_FUTHARK` | 33.sp | 0.35.sp | 44.sp |
| `YOUNGER_FUTHARK` | 31.sp | 0.15.sp | 42.sp |
| `CIRTH` | 35.sp | 0.25.sp | 46.sp |

All values scaled by `LocalRunicFontScale.current`.

## CompositionLocals

| Local | Type | Default | Source |
|-------|------|---------|--------|
| `LocalReduceMotion` | `Boolean` | `false` | `Theme.kt:249` |
| `LocalRunicFontScale` | `Float` | `1.0f` | `Theme.kt:248` |
| `LocalRunicExpressiveType` | `RunicExpressiveTypography` | stone pack | `Type.kt:254` |
| `LocalRunicShapeTokens` | `RunicShapeTokens` | defaults | `ExpressiveTokens.kt:97` |
| `LocalRunicElevationTokens` | `RunicElevationTokens` | defaults | `ExpressiveTokens.kt:98` |
| `LocalRunicMotionTokens` | `RunicMotionTokens` | defaults | `ExpressiveTokens.kt:99` |
