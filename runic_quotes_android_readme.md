# Runic Quotes (Android) – README / TODO / ROADMAP

## 1. Overview

**Runic Quotes** is an Android application built with a modern Kotlin/Compose architecture that displays inspirational and literary quotes rendered in ancient runic scripts:

- **Elder Futhark**
- **Younger Futhark**
- **Tolkien’s Cirth (Angerthas, PUA-mapped)**

The app integrates custom runic fonts (Noto Sans Runic, BabelStone Runic, and a Cirth font) and provides a **minimalist, monochromatic** Material 3 Expressive UI powered by Jetpack Compose, with a home-screen widget built using Jetpack Glance.

**Design Philosophy:**
- **Strictly black & white color scheme** with transparency, tints, and gradients
- **Material 3 Expressive** design language with physics-based motion
- Timeless aesthetic echoing ancient stone carvings and manuscripts

The project emphasizes:
- Clean architecture with DI (Hilt)
- Reactive data flows (coroutines + Flow)
- Modern UI (Compose + Material 3 Expressive)
- Monochromatic design (black, white, grayscale)
- Structured persistence (Room + DataStore)
- Background tasks (WorkManager)
- Widget support (Glance)
- Full test suite and CI enforcement

---

## 2. Tech Stack

### Language & Build
- Kotlin **2.2.21**
- Android Gradle Plugin **8.1.3**
- Kotlin serialization plugin
- Kotlin coroutines

### UI & Navigation
- Jetpack Compose
  - **Material 3 Expressive** components
  - Compose BOM
  - UI, animation, tooling
  - Physics-based motion system
  - Variable typography (Roboto Flex)
- Navigation Compose
  - Integrated with **Nav-3** library

### Architecture & DI
- **Hilt** for DI
  - Android entry points
  - Navigation integration
  - WorkManager integration
- MVVM with:
  - ViewModels
  - Use cases (optional)
  - Repository pattern

### Persistence & Preferences
- Room Database
  - `QuoteEntity`, `QuoteDao`, migrations
- Jetpack DataStore
  - `UserPreferencesManager` for all persistent settings

### Background Work
- WorkManager
  - Daily quote refresh
  - Widget update worker
  - Hilt-injected workers with assisted dependencies

### Widget Support
- Jetpack Glance
  - Home-screen widget
  - Hilt entry points
  - Glance text rendering with runic fonts

### Testing
- JUnit4
- AndroidX Test (JUnit, Espresso)
- MockK
- Turbine
- Truth
- Robolectric
- Hilt testing utilities
- Coroutine test artifacts

### Tooling & Linting
- Detekt static analysis
- (Optional) KtLint for formatting

---

## 3. Material 3 Expressive Design Language

**Runic Quotes** adheres to **Material 3 Expressive**, Google's latest evolution of Material Design launched in May 2025. This design system emphasizes emotional engagement, fluid motion, and personality-driven interfaces while maintaining usability and accessibility.

### 3.1 What is Material 3 Expressive?

Material 3 Expressive (M3 Expressive) is the most researched update to Google's design system, built on insights from 46 separate studies with over 18,000 participants worldwide. It extends Material You personalization with:

- **Enhanced visual expression**: More vibrant colors, dynamic shapes, and expressive typography
- **Physics-based motion**: Natural, springy animations that feel alive and delightful
- **Variable typography**: Roboto Flex with adjustable weight, width, and slant axes
- **Shape morphing**: Fluid transitions between component states
- **Emotional resonance**: Interfaces that mirror specific emotions and feelings

Research shows M3 Expressive is preferred across all age groups, with Gen Z preferring expressive screens 87% of the time over traditional Material designs.

### 3.2 Core Principles for Runic Quotes

#### 3.2.1 Typography System

Runic Quotes uses the M3 Expressive typography scale with **Roboto Flex** as the primary typeface:

```kotlin
// ui/theme/Type.kt
import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontVariation

val RunicTypography = Typography(
    // Display styles - for large, impactful text
    displayLarge = TextStyle(
        fontFamily = RobotoFlex,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        fontWeight = FontWeight.Bold,
        fontVariationSettings = FontVariation.Settings(
            FontVariation.weight(700),
            FontVariation.width(100f)
        )
    ),

    // Headline styles - for section headers
    headlineLarge = TextStyle(
        fontFamily = RobotoFlex,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        fontWeight = FontWeight.SemiBold
    ),

    // Title styles - for card titles, dialog headers
    titleLarge = TextStyle(
        fontFamily = RobotoFlex,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Medium
    ),

    // Body styles - for main content (quote authors, descriptions)
    bodyLarge = TextStyle(
        fontFamily = RobotoFlex,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Normal
    ),

    // Label styles - for buttons, small UI elements
    labelLarge = TextStyle(
        fontFamily = RobotoFlex,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.1.sp
    )
)
```

**Key Typography Guidelines:**
- Use **displayLarge** for the main runic quote text
- Use **headlineMedium** for screen titles
- Use **titleMedium** for quote authors
- Use **bodyLarge** for settings descriptions
- Leverage variable font axes for dynamic weight/width animations

#### 3.2.2 Physics-Based Motion

M3 Expressive replaces duration-based animations with **spring physics** for more natural, fluid motion:

```kotlin
// Example: Animated quote transition with spring physics
@Composable
fun AnimatedRunicQuote(quote: QuoteEntity) {
    val scale by animateFloatAsState(
        targetValue = if (quote != null) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    val alpha by animateFloatAsState(
        targetValue = if (quote != null) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        )
    )

    RunicText(
        text = quote.runicText,
        modifier = Modifier
            .scale(scale)
            .alpha(alpha)
    )
}
```

**Motion Principles:**
- **Entrance animations**: Use `Spring.DampingRatioMediumBouncy` for delightful introductions
- **Exit animations**: Use `Spring.DampingRatioNoBouncy` for smooth departures
- **Morphing shapes**: Animate container shapes when switching between scripts
- **Fluid interactions**: All taps, swipes should feel responsive and alive

#### 3.2.3 Shape Morphing

M3 Expressive introduces adaptive shapes that can morph between states:

```kotlin
// ui/theme/Shapes.kt
val RunicShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

// Morphing between shapes
@Composable
fun MorphingQuoteCard(expanded: Boolean) {
    val cornerRadius by animateDpAsState(
        targetValue = if (expanded) 24.dp else 12.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    Card(
        shape = RoundedCornerShape(cornerRadius),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Quote content
    }
}
```

#### 3.2.4 Color & Theming

**Runic Quotes adheres to a monochromatic black and white color scheme** with strategic use of transparency, tints, and gradients to create depth and visual hierarchy. This minimalist approach emphasizes the ancient, timeless nature of runic scripts while maintaining M3 Expressive's emotional expressiveness.

**Color Philosophy:**
- **Base Colors**: Pure black (#000000) and pure white (#FFFFFF)
- **Tints & Shades**: Grayscale values from 0-255 for hierarchy
- **Transparency (Alpha)**: 0.0 to 1.0 for layering and depth
- **Gradients**: Linear and radial gradients for backgrounds and effects
- **No chromatic colors**: Strictly monochromatic palette

```kotlin
// ui/theme/Color.kt

// Black & White with grayscale tints
object RunicColors {
    // Pure values
    val Black = Color(0xFF000000)
    val White = Color(0xFFFFFFFF)

    // Grayscale tints (for light theme)
    val Gray50 = Color(0xFFF9F9F9)   // Lightest
    val Gray100 = Color(0xFFF5F5F5)
    val Gray200 = Color(0xFFEEEEEE)
    val Gray300 = Color(0xFFE0E0E0)
    val Gray400 = Color(0xFFBDBDBD)
    val Gray500 = Color(0xFF9E9E9E)  // Mid-gray
    val Gray600 = Color(0xFF757575)
    val Gray700 = Color(0xFF616161)
    val Gray800 = Color(0xFF424242)
    val Gray900 = Color(0xFF212121)  // Darkest

    // Alpha variants for transparency
    val BlackAlpha10 = Color(0x1A000000)  // 10% opacity
    val BlackAlpha20 = Color(0x33000000)  // 20% opacity
    val BlackAlpha30 = Color(0x4D000000)  // 30% opacity
    val BlackAlpha50 = Color(0x80000000)  // 50% opacity
    val BlackAlpha70 = Color(0xB3000000)  // 70% opacity
    val BlackAlpha90 = Color(0xE6000000)  // 90% opacity

    val WhiteAlpha10 = Color(0x1AFFFFFF)
    val WhiteAlpha20 = Color(0x33FFFFFF)
    val WhiteAlpha30 = Color(0x4DFFFFFF)
    val WhiteAlpha50 = Color(0x80FFFFFF)
    val WhiteAlpha70 = Color(0xB3FFFFFF)
    val WhiteAlpha90 = Color(0xE6FFFFFF)
}

// Light theme - White background with black accents
val RunicLightScheme = lightColorScheme(
    primary = RunicColors.Black,
    onPrimary = RunicColors.White,
    primaryContainer = RunicColors.Gray100,
    onPrimaryContainer = RunicColors.Black,

    secondary = RunicColors.Gray700,
    onSecondary = RunicColors.White,
    secondaryContainer = RunicColors.Gray200,
    onSecondaryContainer = RunicColors.Gray900,

    tertiary = RunicColors.Gray600,
    onTertiary = RunicColors.White,
    tertiaryContainer = RunicColors.Gray300,
    onTertiaryContainer = RunicColors.Gray900,

    surface = RunicColors.White,
    onSurface = RunicColors.Black,
    surfaceVariant = RunicColors.Gray100,
    onSurfaceVariant = RunicColors.Gray700,

    outline = RunicColors.Gray400,
    outlineVariant = RunicColors.Gray200,

    background = RunicColors.White,
    onBackground = RunicColors.Black,
)

// Dark theme - Black background with white accents
val RunicDarkScheme = darkColorScheme(
    primary = RunicColors.White,
    onPrimary = RunicColors.Black,
    primaryContainer = RunicColors.Gray900,
    onPrimaryContainer = RunicColors.White,

    secondary = RunicColors.Gray400,
    onSecondary = RunicColors.Black,
    secondaryContainer = RunicColors.Gray800,
    onSecondaryContainer = RunicColors.Gray100,

    tertiary = RunicColors.Gray500,
    onTertiary = RunicColors.Black,
    tertiaryContainer = RunicColors.Gray700,
    onTertiaryContainer = RunicColors.Gray100,

    surface = RunicColors.Black,
    onSurface = RunicColors.White,
    surfaceVariant = RunicColors.Gray900,
    onSurfaceVariant = RunicColors.Gray400,

    outline = RunicColors.Gray700,
    outlineVariant = RunicColors.Gray800,

    background = RunicColors.Black,
    onBackground = RunicColors.White,
)

// Gradient definitions for backgrounds and effects
object RunicGradients {
    // Light theme gradients
    val LightRadialBackground = Brush.radialGradient(
        colors = listOf(
            RunicColors.Gray50,
            RunicColors.White
        )
    )

    val LightCardGradient = Brush.verticalGradient(
        colors = listOf(
            RunicColors.White,
            RunicColors.Gray100
        )
    )

    // Dark theme gradients
    val DarkRadialBackground = Brush.radialGradient(
        colors = listOf(
            RunicColors.Gray900,
            RunicColors.Black
        )
    )

    val DarkCardGradient = Brush.verticalGradient(
        colors = listOf(
            RunicColors.Gray900,
            RunicColors.Black
        )
    )

    // Accent gradients for emphasis
    val RuneGlowLight = Brush.radialGradient(
        colors = listOf(
            RunicColors.BlackAlpha20,
            RunicColors.BlackAlpha10,
            Color.Transparent
        )
    )

    val RuneGlowDark = Brush.radialGradient(
        colors = listOf(
            RunicColors.WhiteAlpha20,
            RunicColors.WhiteAlpha10,
            Color.Transparent
        )
    )
}

// Monochromatic theme implementation (no dynamic color)
@Composable
fun RunicQuotesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Strictly black & white - no dynamic color support
    val colorScheme = if (darkTheme) RunicDarkScheme else RunicLightScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = RunicTypography,
        shapes = RunicShapes,
        content = content
    )
}

// Example: Using gradients for backgrounds
@Composable
fun RunicQuoteBackground(
    darkTheme: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier.background(
            brush = if (darkTheme) {
                RunicGradients.DarkRadialBackground
            } else {
                RunicGradients.LightRadialBackground
            }
        )
    ) {
        content()
    }
}
```

**Usage Examples:**

```kotlin
// Card with subtle gradient
Card(
    modifier = Modifier
        .fillMaxWidth()
        .background(brush = RunicGradients.LightCardGradient),
    colors = CardDefaults.cardColors(
        containerColor = Color.Transparent // Let gradient show through
    )
) {
    // Content
}

// Runic text with glow effect
Box {
    // Glow layer
    Box(
        modifier = Modifier
            .matchParentSize()
            .background(brush = RunicGradients.RuneGlowDark)
    )

    // Runic text
    RunicText(
        text = quote.runicElder,
        style = MaterialTheme.typography.displayLarge
    )
}

// Layered transparency for depth
Surface(
    color = RunicColors.WhiteAlpha90,
    modifier = Modifier.fillMaxWidth()
) {
    Surface(
        color = RunicColors.BlackAlpha10,
        modifier = Modifier.padding(16.dp)
    ) {
        // Nested content with subtle depth
    }
}
```

### 3.3 Expressive Components for Runic Quotes

#### 3.3.1 Hero Quote Display

```kotlin
@Composable
fun ExpressiveQuoteDisplay(
    quote: QuoteEntity,
    script: RunicScript,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Runic text with expressive entrance animation
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ) + scaleIn(
                initialScale = 0.8f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy
                )
            )
        ) {
            RunicText(
                text = quote.getRunicText(script),
                style = MaterialTheme.typography.displayLarge,
                modifier = Modifier.padding(vertical = 32.dp)
            )
        }

        // Author with subtle slide-in
        AnimatedVisibility(
            visible = true,
            enter = slideInVertically(
                initialOffsetY = { it / 2 },
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            ) + fadeIn()
        ) {
            Text(
                text = "— ${quote.author}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
```

#### 3.3.2 Morphing Script Selector

```kotlin
@Composable
fun ExpressiveScriptSelector(
    selectedScript: RunicScript,
    onScriptSelected: (RunicScript) -> Unit
) {
    val scripts = listOf(
        RunicScript.ELDER_FUTHARK to "Elder",
        RunicScript.YOUNGER_FUTHARK to "Younger",
        RunicScript.CIRTH to "Cirth"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        scripts.forEach { (script, label) ->
            val isSelected = script == selectedScript
            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1.05f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
            )

            FilterChip(
                selected = isSelected,
                onClick = { onScriptSelected(script) },
                label = { Text(label) },
                modifier = Modifier.scale(scale)
            )
        }
    }
}
```

### 3.4 M3 Expressive Resources

**Official Documentation:**
- [Material 3 Expressive Guidelines](https://m3.material.io/expressive)
- [Material 3 in Compose](https://developer.android.com/develop/ui/compose/designsystems/material3)
- [Material Theme Builder](https://material.io/material-theme-builder)
- [Expressive Motion Specs](https://m3.material.io/styles/motion/overview)

**Design Tools:**
- [Material Theme Builder](https://material.io/material-theme-builder) - Generate color schemes
- [Figma Material 3 Kit](https://www.figma.com/community/file/1035203688168086460)
- [Roboto Flex on Google Fonts](https://fonts.google.com/specimen/Roboto+Flex)

**Sample Apps:**
- [Reply Sample](https://github.com/android/compose-samples/tree/main/Reply) - Full M3 implementation
- [Now in Android](https://github.com/android/nowinandroid) - M3 Expressive patterns

### 3.5 Implementation Checklist

#### Typography & Fonts
- [ ] Use Roboto Flex for primary UI typography
- [ ] Configure variable font axes for dynamic typography
- [ ] Integrate runic fonts (Noto Sans Runic, BabelStone, Cirth)
- [ ] Test font rendering across Android versions

#### Motion & Animation
- [ ] Implement spring-based animations for all transitions
- [ ] Use `Spring.DampingRatioMediumBouncy` for entrances
- [ ] Use `Spring.DampingRatioNoBouncy` for exits
- [ ] Apply shape morphing to interactive components
- [ ] Implement fluid motion for quote transitions
- [ ] Profile animation performance (maintain 60fps minimum)
- [ ] Add haptic feedback to enhance expressiveness

#### Color & Theming
- [ ] **Implement strict black & white color scheme**
- [ ] Define complete grayscale tint palette (Gray50-Gray900)
- [ ] Create alpha/transparency variants (10%, 20%, 30%, 50%, 70%, 90%)
- [ ] Design light theme (white background, black accents)
- [ ] Design dark theme (black background, white accents)
- [ ] Implement radial and linear gradients for backgrounds
- [ ] Create gradient overlays for runic text effects
- [ ] **NO chromatic colors** - validate in code review
- [ ] Ensure WCAG AAA contrast ratios (7:1 minimum for text)
- [ ] Test monochromatic theme in light/dark modes

#### Components & UI
- [ ] Build expressive quote display with animations
- [ ] Create morphing script selector
- [ ] Design cards with gradient backgrounds
- [ ] Implement layered transparency for depth
- [ ] Add glow effects for rune emphasis
- [ ] Test all components in both themes

#### Accessibility
- [ ] Ensure proper contrast ratios with black/white scheme
- [ ] Test with TalkBack screen reader
- [ ] Verify touch target sizes (48dp minimum)
- [ ] Add content descriptions for all interactive elements
- [ ] Test with font scaling up to 200%

#### Performance
- [ ] Profile rendering performance with gradients
- [ ] Optimize transparency layering
- [ ] Test on low-end devices (API 26+)
- [ ] Ensure smooth 60fps animations

### 3.6 Why M3 Expressive + Monochrome for Runic Quotes?

Material 3 Expressive with a black & white color scheme aligns perfectly with the Runic Quotes app vision:

1. **Timeless Aesthetic**: A monochromatic palette echoes ancient stone carvings and historical runic inscriptions
2. **Emotional Connection**: Ancient runes carry historical weight and mystique—M3 Expressive's emphasis on emotion through motion (not color) enhances this connection
3. **Fluid Motion**: The physics-based animations mirror the organic, hand-carved nature of historical runes
4. **Minimalist Focus**: Black and white forces attention on the runic glyphs themselves, letting the ancient scripts be the visual stars
5. **Personality Through Motion**: Each runic script has distinct character—M3 Expressive's shape morphing and typography flexibility lets each script shine without chromatic distractions
6. **Modern + Ancient**: Bridges cutting-edge Android design with timeless monochromatic aesthetics reminiscent of stone tablets and ancient manuscripts
7. **Universal Appeal**: Monochrome design transcends cultural color associations, making the app universally accessible
8. **Expressiveness Without Color**: Demonstrates that M3 Expressive's emotional impact comes from typography, motion, and shape—not just vibrant colors

---

## 4. Runic Fonts & Rendering

### 3.1 Fonts Used
- **Noto Sans Runic** (Unicode Runic block)
- **BabelStone Runic** (complete coverage of Unicode Runic)
- **Cirth Angerthas** (PUA codepoints for Tolkien’s script)

### 3.2 Bundling Fonts
Place the `.ttf` files inside:
```
app/src/main/res/font/
```
Example:
```
noto_sans_runic.ttf
babelstone_runic.ttf
cirth_angerthas.ttf
```
Gradle generates `R.font.*` identifiers.

### 3.3 Compose Font Families
Use Compose’s `FontFamily`:
```kotlin
val NotoSansRunic = FontFamily(Font(R.font.noto_sans_runic))
val BabelStoneRunic = FontFamily(Font(R.font.babelstone_runic))
val CirthFontFamily = FontFamily(Font(R.font.cirth_angerthas))
```

### 3.4 Font Selection Logic
```kotlin
enum class RunicScript { ELDER, YOUNGER, CIRITH }
enum class RunicFont { NOTO, BABELSTONE, CIRITH }

fun RunicFont.asFontFamily(script: RunicScript): FontFamily = when (script) {
    RunicScript.CIRITH -> CirthFontFamily
    else -> when (this) {
        RunicFont.NOTO -> NotoSansRunic
        RunicFont.BABELSTONE -> BabelStoneRunic
        RunicFont.CIRITH -> CirthFontFamily
    }
}
```

### 3.5 Transliteration
Latin → Runic mapping for:
- Elder Futhark (U+16A0–U+16FF)
- Younger Futhark (subset)
- Cirth (PUA range U+E080+)

Example:
```kotlin
object RunicTransliterator {
    private val elderMap = mapOf(
        'f' to '\u16A0',
        'u' to '\u16A2',
        // ...
    )

    fun toElder(text: String) = text.lowercase().map { elderMap[it] ?: it }.joinToString("")
}
```

### 3.6 Performance Considerations
- Precompute runic versions during DB seeding.
- Cache FontFamilies using `remember` in Compose.
- Keep widget text short and simple.
- Use WorkManager for periodic updates.
- Avoid heavy mapping logic in Composables.

---

## 4. Architecture

### 4.1 Data Layer

**Room Entities**
```kotlin
@Entity
data class QuoteEntity(
    @PrimaryKey val id: Long,
    val textLatin: String,
    val author: String,
    val runicElder: String?,
    val runicYounger: String?,
    val runicCirth: String?
)
```

**DAO**
```kotlin
@Dao
interface QuoteDao {
    @Query("SELECT * FROM QuoteEntity ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandom(): QuoteEntity

    @Query("SELECT * FROM QuoteEntity")
    suspend fun getAll(): List<QuoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<QuoteEntity>)
}
```

**Repository**
```kotlin
interface QuoteRepository {
    suspend fun seedIfNeeded()
    suspend fun quoteOfTheDay(script: RunicScript): QuoteEntity
    suspend fun randomQuote(script: RunicScript): QuoteEntity
}
```

### 4.2 Preferences Layer
`UserPreferencesManager` with Jetpack DataStore:
- selectedScript
- selectedFont
- widgetMode
- lastQuoteDate
- lastDailyQuoteId

### 4.3 Domain Layer (Optional)
- `GetQuoteOfTheDayUseCase`
- `GetRandomQuoteUseCase`
- `SeedQuotesIfNeededUseCase`

### 4.4 Presentation Layer
`QuoteViewModel`:
- Exposes `StateFlow<QuoteUiState>`
- Handles quote fetching, script switching, font switching
- Combines Room + DataStore flows

`SettingsViewModel`:
- Exposes and updates user preferences

### 4.5 UI Layer
**Screens:**
- QuoteScreen
- SettingsScreen
- AboutScreen

**Components:**
- `RunicQuoteText(text, script, font)`
- `ScriptSelector`
- `FontSelector`

---

## 5. Widget (Glance)

### 5.1 Architecture
- `RunicQuoteWidget` (GlanceAppWidget)
- `RunicQuoteWidgetReceiver`
- `RunicQuoteWidgetEntry`
- `RunicQuoteWidgetContent`
- Hilt EntryPoint for repository access

### 5.2 Widget Data Flow
- Worker updates stored quote based on preferences
- Widget reads stored value & displays runic text

### 5.3 Text Rendering Inside Widget
Use custom FontFamily:
```kotlin
@Composable
fun RunicWidgetText(text: String, script: RunicScript, font: RunicFont) {
    Text(
        text,
        style = TextStyle(fontFamily = font.asFontFamily(script), fontSize = 18.sp)
    )
}
```

### 5.4 Update Cadence
- Daily update at configured time
- Manual update on widget refresh

---

## 6. Build & CI

### 6.1 Gradle Configuration

#### Version Catalog (`gradle/libs.versions.toml`)
```toml
[versions]
kotlin = "2.2.21"
agp = "8.1.3"
compose-bom = "2024.02.00"
compose-compiler = "1.5.8"
hilt = "2.48"
room = "2.6.1"
datastore = "1.0.0"
work = "2.9.0"
glance = "1.0.0"
navigation = "2.7.6"
lifecycle = "2.7.0"
coroutines = "1.7.3"
serialization = "1.6.2"

[libraries]
# Kotlin
kotlin-stdlib = { module = "org.jetbrains.kotlin:kotlin-stdlib", version.ref = "kotlin" }
kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "coroutines" }
kotlinx-coroutines-android = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version.ref = "coroutines" }
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "serialization" }

# Compose
compose-bom = { module = "androidx.compose:compose-bom", version.ref = "compose-bom" }
compose-ui = { module = "androidx.compose.ui:ui" }
compose-material3 = { module = "androidx.compose.material3:material3" }
compose-ui-tooling-preview = { module = "androidx.compose.ui:ui-tooling-preview" }
compose-ui-tooling = { module = "androidx.compose.ui:ui-tooling" }
compose-ui-test-junit4 = { module = "androidx.compose.ui:ui-test-junit4" }
compose-ui-test-manifest = { module = "androidx.compose.ui:ui-test-manifest" }

# Hilt
hilt-android = { module = "com.google.dagger:hilt-android", version.ref = "hilt" }
hilt-compiler = { module = "com.google.dagger:hilt-compiler", version.ref = "hilt" }
hilt-navigation-compose = { module = "androidx.hilt:hilt-navigation-compose", version = "1.1.0" }
hilt-work = { module = "androidx.hilt:hilt-work", version = "1.1.0" }

# Room
room-runtime = { module = "androidx.room:room-runtime", version.ref = "room" }
room-compiler = { module = "androidx.room:room-compiler", version.ref = "room" }
room-ktx = { module = "androidx.room:room-ktx", version.ref = "room" }

# DataStore
datastore-preferences = { module = "androidx.datastore:datastore-preferences", version.ref = "datastore" }

# WorkManager
work-runtime-ktx = { module = "androidx.work:work-runtime-ktx", version.ref = "work" }

# Glance
glance-appwidget = { module = "androidx.glance:glance-appwidget", version.ref = "glance" }
glance-material3 = { module = "androidx.glance:glance-material3", version.ref = "glance" }

# Navigation
navigation-compose = { module = "androidx.navigation:navigation-compose", version.ref = "navigation" }

# Testing
junit = { module = "junit:junit", version = "4.13.2" }
mockk = { module = "io.mockk:mockk", version = "1.13.8" }
turbine = { module = "app.cash.turbine:turbine", version = "1.0.0" }
truth = { module = "com.google.truth:truth", version = "1.1.5" }
coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "coroutines" }
robolectric = { module = "org.robolectric:robolectric", version = "4.11.1" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-kapt = { id = "org.jetbrains.kotlin.kapt", version.ref = "kotlin" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
detekt = { id = "io.gitlab.arturbosch.detekt", version = "1.23.4" }
```

#### App-level `build.gradle.kts`
```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.detekt)
}

android {
    namespace = "com.runicquotes.android"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.runicquotes.android"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api"
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Compose BOM
    implementation(platform(libs.compose.bom))

    // Core
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Compose
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    kapt(libs.room.compiler)

    // DataStore
    implementation(libs.datastore.preferences)

    // WorkManager
    implementation(libs.work.runtime.ktx)

    // Glance
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)

    // Navigation
    implementation(libs.navigation.compose)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.truth)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.robolectric)

    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(files("$rootDir/detekt.yml"))
    baseline = file("$rootDir/detekt-baseline.xml")
}
```

### 6.2 Detekt Configuration

#### `detekt.yml`
```yaml
build:
  maxIssues: 0
  weights:
    complexity: 2
    LongParameterList: 1
    style: 1
    comments: 1

config:
  validation: true
  warningsAsErrors: false

complexity:
  active: true
  ComplexCondition:
    active: true
    threshold: 4
  ComplexInterface:
    active: true
    threshold: 10
  LongMethod:
    active: true
    threshold: 60
  LongParameterList:
    active: true
    functionThreshold: 6
  NestedBlockDepth:
    active: true
    threshold: 4

naming:
  active: true
  FunctionNaming:
    active: true
  ClassNaming:
    active: true
  VariableNaming:
    active: true

style:
  active: true
  MagicNumber:
    active: true
    ignoreNumbers: [-1, 0, 1, 2, 100, 1000]
  MaxLineLength:
    active: true
    maxLineLength: 120
```

### 6.3 ProGuard Rules

#### `proguard-rules.pro`
```proguard
# Runic Quotes specific rules

# Keep custom fonts
-keep class **.R$font { *; }

# Keep Room entities
-keep class com.runicquotes.android.data.local.entity.** { *; }

# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Keep Glance widget classes
-keep class androidx.glance.** { *; }
-keep class com.runicquotes.android.widget.** { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# DataStore
-keep class androidx.datastore.*.** {*;}
```

### 6.4 GitHub Actions CI/CD

#### `.github/workflows/ci.yml`
```yaml
name: Android CI

on:
  push:
    branches: [ main, develop, claude/** ]
  pull_request:
    branches: [ main, develop ]

jobs:
  build:
    runs-on: ubuntu-latest
    timeout-minutes: 30

    steps:
    - uses: actions/checkout@v4

    - name: Set up JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'
        cache: gradle

    - name: Grant execute permission for gradlew
      run: chmod +x gradlew

    - name: Run Detekt
      run: ./gradlew detekt

    - name: Upload Detekt reports
      if: always()
      uses: actions/upload-artifact@v4
      with:
        name: detekt-reports
        path: build/reports/detekt/

    - name: Run unit tests
      run: ./gradlew testDebugUnitTest

    - name: Upload test reports
      if: always()
      uses: actions/upload-artifact@v4
      with:
        name: test-reports
        path: app/build/reports/tests/

    - name: Build debug APK
      run: ./gradlew assembleDebug

    - name: Upload APK
      uses: actions/upload-artifact@v4
      with:
        name: app-debug
        path: app/build/outputs/apk/debug/app-debug.apk

  instrumented-tests:
    runs-on: macos-latest
    timeout-minutes: 45

    steps:
    - uses: actions/checkout@v4

    - name: Set up JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'
        cache: gradle

    - name: Grant execute permission for gradlew
      run: chmod +x gradlew

    - name: Run instrumented tests
      uses: reactivecircus/android-emulator-runner@v2
      with:
        api-level: 30
        target: google_apis
        arch: x86_64
        script: ./gradlew connectedDebugAndroidTest

    - name: Upload instrumented test reports
      if: always()
      uses: actions/upload-artifact@v4
      with:
        name: instrumented-test-reports
        path: app/build/reports/androidTests/
```

#### `.github/workflows/release.yml`
```yaml
name: Release Build

on:
  push:
    tags:
      - 'v*'

jobs:
  release:
    runs-on: ubuntu-latest

    steps:
    - uses: actions/checkout@v4

    - name: Set up JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'
        cache: gradle

    - name: Grant execute permission for gradlew
      run: chmod +x gradlew

    - name: Build release APK
      run: ./gradlew assembleRelease
      env:
        KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
        KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
        KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}

    - name: Build release AAB
      run: ./gradlew bundleRelease

    - name: Create Release
      uses: softprops/action-gh-release@v1
      with:
        files: |
          app/build/outputs/apk/release/app-release.apk
          app/build/outputs/bundle/release/app-release.aab
      env:
        GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

### 6.5 Continuous Integration Best Practices

- **Branch Protection**: Require CI checks to pass before merging
- **Code Coverage**: Target minimum 80% coverage
- **Automated Testing**: Run full test suite on every PR
- **Dependency Management**: Use Dependabot for automated updates
- **Security Scanning**: Integrate SAST tools (e.g., Snyk, GitGuardian)
- **Build Caching**: Leverage Gradle build cache for faster builds
- **Parallel Execution**: Run independent test suites in parallel
- **Artifact Management**: Store APKs/AABs for each successful build

---

## 7. Usage & UX

### 7.1 Main Screen
- Centered runic quote with selectable font + script
- Author label
- "Next quote" button

### 7.2 Settings Screen
- Script selection
- Font selection
- Widget update style
- About section

### 7.3 Widget
- Displays daily/random runic quote
- Tapping widget opens app

---

## 8. Project Structure

```
runatal-android/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/runicquotes/android/
│   │   │   │   ├── RunicQuotesApp.kt                   # Application class
│   │   │   │   ├── MainActivity.kt                     # Main activity
│   │   │   │   │
│   │   │   │   ├── data/
│   │   │   │   │   ├── local/
│   │   │   │   │   │   ├── entity/
│   │   │   │   │   │   │   └── QuoteEntity.kt
│   │   │   │   │   │   ├── dao/
│   │   │   │   │   │   │   └── QuoteDao.kt
│   │   │   │   │   │   └── RunicQuotesDatabase.kt
│   │   │   │   │   │
│   │   │   │   │   ├── preferences/
│   │   │   │   │   │   ├── UserPreferences.kt
│   │   │   │   │   │   ├── UserPreferencesSerializer.kt
│   │   │   │   │   │   └── UserPreferencesManager.kt
│   │   │   │   │   │
│   │   │   │   │   └── repository/
│   │   │   │   │       ├── QuoteRepository.kt
│   │   │   │   │       ├── QuoteRepositoryImpl.kt
│   │   │   │   │       └── QuoteSeedData.kt
│   │   │   │   │
│   │   │   │   ├── di/
│   │   │   │   │   ├── DatabaseModule.kt
│   │   │   │   │   ├── DataStoreModule.kt
│   │   │   │   │   └── RepositoryModule.kt
│   │   │   │   │
│   │   │   │   ├── domain/
│   │   │   │   │   ├── model/
│   │   │   │   │   │   ├── RunicScript.kt
│   │   │   │   │   │   ├── RunicFont.kt
│   │   │   │   │   │   ├── ThemeMode.kt
│   │   │   │   │   │   └── WidgetUpdateMode.kt
│   │   │   │   │   │
│   │   │   │   │   ├── transliteration/
│   │   │   │   │   │   ├── RunicTransliterator.kt
│   │   │   │   │   │   ├── ElderFutharkTransliterator.kt
│   │   │   │   │   │   ├── YoungerFutharkTransliterator.kt
│   │   │   │   │   │   ├── CirthTransliterator.kt
│   │   │   │   │   │   └── TransliterationFactory.kt
│   │   │   │   │   │
│   │   │   │   │   └── usecase/                        # Optional
│   │   │   │   │       ├── GetQuoteOfTheDayUseCase.kt
│   │   │   │   │       ├── GetRandomQuoteUseCase.kt
│   │   │   │   │       └── SeedQuotesUseCase.kt
│   │   │   │   │
│   │   │   │   ├── ui/
│   │   │   │   │   ├── theme/
│   │   │   │   │   │   ├── Color.kt
│   │   │   │   │   │   ├── Type.kt
│   │   │   │   │   │   ├── Theme.kt
│   │   │   │   │   │   ├── Shapes.kt
│   │   │   │   │   │   └── RunicFonts.kt
│   │   │   │   │   │
│   │   │   │   │   ├── components/
│   │   │   │   │   │   ├── RunicText.kt
│   │   │   │   │   │   ├── ScriptSelector.kt
│   │   │   │   │   │   ├── FontSelector.kt
│   │   │   │   │   │   ├── QuoteCard.kt
│   │   │   │   │   │   ├── LoadingIndicator.kt
│   │   │   │   │   │   └── ErrorMessage.kt
│   │   │   │   │   │
│   │   │   │   │   ├── navigation/
│   │   │   │   │   │   ├── Screen.kt
│   │   │   │   │   │   └── RunicNavGraph.kt
│   │   │   │   │   │
│   │   │   │   │   ├── quote/
│   │   │   │   │   │   ├── QuoteUiState.kt
│   │   │   │   │   │   ├── QuoteViewModel.kt
│   │   │   │   │   │   └── QuoteScreen.kt
│   │   │   │   │   │
│   │   │   │   │   ├── settings/
│   │   │   │   │   │   ├── SettingsUiState.kt
│   │   │   │   │   │   ├── SettingsViewModel.kt
│   │   │   │   │   │   └── SettingsScreen.kt
│   │   │   │   │   │
│   │   │   │   │   ├── about/
│   │   │   │   │   │   └── AboutScreen.kt
│   │   │   │   │   │
│   │   │   │   │   ├── quotelist/                      # Optional
│   │   │   │   │   │   ├── QuoteListViewModel.kt
│   │   │   │   │   │   └── QuoteListScreen.kt
│   │   │   │   │   │
│   │   │   │   │   └── share/                          # Optional
│   │   │   │   │       └── ShareQuoteViewModel.kt
│   │   │   │   │
│   │   │   │   ├── widget/
│   │   │   │   │   ├── RunicQuoteWidget.kt
│   │   │   │   │   ├── RunicQuoteWidgetReceiver.kt
│   │   │   │   │   ├── RunicQuoteWidgetContent.kt
│   │   │   │   │   └── data/
│   │   │   │   │       └── WidgetQuoteRepository.kt
│   │   │   │   │
│   │   │   │   └── worker/
│   │   │   │       ├── QuoteUpdateWorker.kt
│   │   │   │       └── WorkerScheduler.kt
│   │   │   │
│   │   │   ├── res/
│   │   │   │   ├── font/
│   │   │   │   │   ├── noto_sans_runic.ttf
│   │   │   │   │   ├── babelstone_runic.ttf
│   │   │   │   │   └── cirth_angerthas.ttf
│   │   │   │   │
│   │   │   │   ├── xml/
│   │   │   │   │   └── runic_widget_info.xml
│   │   │   │   │
│   │   │   │   ├── values/
│   │   │   │   │   ├── strings.xml
│   │   │   │   │   ├── colors.xml
│   │   │   │   │   └── themes.xml
│   │   │   │   │
│   │   │   │   └── drawable/
│   │   │   │       ├── ic_launcher.xml
│   │   │   │       └── ic_launcher_foreground.xml
│   │   │   │
│   │   │   └── AndroidManifest.xml
│   │   │
│   │   ├── test/                                        # Unit tests
│   │   │   └── java/com/runicquotes/android/
│   │   │       ├── transliteration/
│   │   │       ├── repository/
│   │   │       └── viewmodel/
│   │   │
│   │   └── androidTest/                                 # Instrumented tests
│   │       └── java/com/runicquotes/android/
│   │           ├── database/
│   │           ├── ui/
│   │           └── preferences/
│   │
│   ├── build.gradle.kts
│   └── proguard-rules.pro
│
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
│
├── .github/
│   └── workflows/
│       ├── ci.yml
│       └── release.yml
│
├── detekt.yml
├── detekt-baseline.xml
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
├── .gitignore
├── LICENSE
└── README.md
```

### Key Directory Explanations

- **data/**: All data-related code (Room, DataStore, Repository)
- **di/**: Hilt dependency injection modules
- **domain/**: Business logic, models, transliteration engine
- **ui/**: All UI code (Compose screens, ViewModels, components, theme)
- **widget/**: Glance widget implementation
- **worker/**: WorkManager background tasks
- **res/font/**: Custom runic font files
- **test/**: Unit tests (JVM)
- **androidTest/**: Instrumented tests (Android device/emulator)

---

## 9. TODO List

### Phase 1 – Project Setup & Infrastructure (Est: 2-3 days)

#### 1.1 Initial Project Setup
- [ ] Create new Android Studio project with Empty Compose Activity template
  - Package: `com.runicquotes.android`
  - Min SDK: 26 (Android 8.0)
  - Target SDK: 34
  - Compile SDK: 34
- [ ] Configure `gradle/libs.versions.toml` with version catalog
  - Kotlin: 2.2.21
  - AGP: 8.1.3
  - Compose BOM: 2024.02.00
  - Hilt: 2.48
  - Room: 2.6.1
  - DataStore: 1.0.0
  - WorkManager: 2.9.0
  - Glance: 1.0.0
- [ ] Set up `.gitignore` for Android projects
- [ ] Configure Gradle build files
  - `settings.gradle.kts`
  - Root `build.gradle.kts`
  - App `build.gradle.kts` with all plugins

#### 1.2 Dependency Injection Setup
- [ ] Add Hilt dependencies to `build.gradle.kts`
- [ ] Create `@HiltAndroidApp` application class in `RunicQuotesApp.kt`
- [ ] Create `di/` package for modules
- [ ] Create `di/DatabaseModule.kt` for Room dependencies
- [ ] Create `di/DataStoreModule.kt` for preferences
- [ ] Create `di/RepositoryModule.kt` for repository bindings
- [ ] Configure `AndroidManifest.xml` with application name

#### 1.3 Database Layer
- [ ] Create `data/local/entity/QuoteEntity.kt`
  ```kotlin
  @Entity(tableName = "quotes")
  data class QuoteEntity(
      @PrimaryKey(autoGenerate = true) val id: Long = 0,
      @ColumnInfo(name = "text_latin") val textLatin: String,
      @ColumnInfo(name = "author") val author: String,
      @ColumnInfo(name = "source") val source: String? = null,
      @ColumnInfo(name = "runic_elder") val runicElder: String,
      @ColumnInfo(name = "runic_younger") val runicYounger: String,
      @ColumnInfo(name = "runic_cirth") val runicCirth: String,
      @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
  )
  ```
- [ ] Create `data/local/dao/QuoteDao.kt` with operations:
  - `getRandom(): QuoteEntity`
  - `getById(id: Long): QuoteEntity?`
  - `getAll(): Flow<List<QuoteEntity>>`
  - `getAllPaged(): PagingSource<Int, QuoteEntity>`
  - `insertAll(quotes: List<QuoteEntity>)`
  - `delete(quote: QuoteEntity)`
  - `getCount(): Int`
- [ ] Create `data/local/RunicQuotesDatabase.kt`
  - Version 1
  - Include QuoteEntity
  - Implement database callback for seeding
- [ ] Create database migration strategies file

#### 1.4 Runic Transliteration Engine
- [ ] Create `domain/model/RunicScript.kt` enum
  ```kotlin
  enum class RunicScript {
      ELDER_FUTHARK,
      YOUNGER_FUTHARK,
      CIRTH
  }
  ```
- [ ] Create `domain/model/RunicFont.kt` enum
- [ ] Create `domain/transliteration/RunicTransliterator.kt` interface
- [ ] Implement `domain/transliteration/ElderFutharkTransliterator.kt`
  - Complete Unicode mapping for Elder Futhark (U+16A0–U+16EA)
  - Handle special cases (th, ng, etc.)
- [ ] Implement `domain/transliteration/YoungerFutharkTransliterator.kt`
  - Long-branch and short-twig variants
- [ ] Implement `domain/transliteration/CirthTransliterator.kt`
  - PUA mapping (U+E080+)
  - Tengwar mode support
- [ ] Create `domain/transliteration/TransliterationFactory.kt`
- [ ] Add comprehensive unit tests for all transliterators

#### 1.5 Font Integration
- [ ] Download and verify font licenses:
  - Noto Sans Runic (OFL)
  - BabelStone Runic (free use)
  - Cirth Angerthas font (verify license)
- [ ] Create `app/src/main/res/font/` directory
- [ ] Add `noto_sans_runic.ttf`
- [ ] Add `babelstone_runic.ttf`
- [ ] Add `cirth_angerthas.ttf`
- [ ] Create `ui/theme/RunicFonts.kt` with FontFamily definitions
- [ ] Test font rendering in Compose preview

#### 1.6 Preferences Layer
- [ ] Create `data/preferences/UserPreferences.kt` data class
  ```kotlin
  data class UserPreferences(
      val selectedScript: RunicScript = RunicScript.ELDER_FUTHARK,
      val selectedFont: RunicFont = RunicFont.NOTO,
      val widgetUpdateMode: WidgetUpdateMode = WidgetUpdateMode.DAILY,
      val lastQuoteDate: String? = null,
      val lastDailyQuoteId: Long? = null,
      val themeMode: ThemeMode = ThemeMode.SYSTEM
  )
  ```
- [ ] Create `data/preferences/UserPreferencesSerializer.kt` (Proto or JSON)
- [ ] Create `data/preferences/UserPreferencesManager.kt`
  - Expose `Flow<UserPreferences>`
  - Suspend functions for updates
- [ ] Add Proto DataStore schema if using proto (optional)

#### 1.7 Repository Layer
- [ ] Create `data/repository/QuoteRepository.kt` interface
- [ ] Implement `data/repository/QuoteRepositoryImpl.kt`
  - `seedDatabaseIfNeeded()` - 100+ quotes
  - `getQuoteOfTheDay(script: RunicScript): Flow<QuoteEntity>`
  - `getRandomQuote(): QuoteEntity`
  - `getAllQuotes(): Flow<List<QuoteEntity>>`
  - `searchQuotes(query: String): Flow<List<QuoteEntity>>`
- [ ] Create quote seed data in `data/repository/QuoteSeedData.kt`
  - Minimum 100 quotes from various sources
  - Norse mythology, Tolkien, literature, philosophy
  - Pre-transliterate all to Elder, Younger, Cirth

### Phase 2 – UI & ViewModels (Est: 3-4 days)

#### 2.1 Material 3 Expressive Design System & Theme
- [ ] Create `ui/theme/Color.kt` with **monochromatic black & white scheme**
  - Pure black (#000000) and pure white (#FFFFFF)
  - Grayscale tints (Gray50-Gray900)
  - Alpha/transparency variants (10%, 20%, 30%, 50%, 70%, 90%)
  - Light theme: white background, black accents
  - Dark theme: black background, white accents
  - **NO chromatic colors allowed**
- [ ] Create `ui/theme/Gradients.kt` with gradient definitions
  - Radial gradients for backgrounds
  - Linear gradients for cards
  - Glow effects for runic text
- [ ] Create `ui/theme/Type.kt` with **Roboto Flex** M3 Expressive typography
  - Variable font configuration
  - Font variation axes (weight, width)
  - Complete 15-style type scale
- [ ] Create `ui/theme/Theme.kt` with Material3 Expressive theming
  - No dynamic color support (monochrome only)
  - Spring-based animation defaults
- [ ] Create `ui/theme/Shapes.kt` with M3 Expressive shapes
  - Rounded corners (4dp, 8dp, 12dp, 16dp, 24dp)
  - Shape morphing support
- [ ] Create `ui/theme/RunicFonts.kt` with runic FontFamily definitions
- [ ] Add M3 Expressive motion utilities
  - Spring animation specs
  - Damping ratio constants
- [ ] Add theme previews for light/dark modes
- [ ] Validate WCAG AAA contrast ratios (7:1 minimum)

#### 2.2 Core UI Components
- [ ] Create `ui/components/RunicText.kt`
  ```kotlin
  @Composable
  fun RunicText(
      text: String,
      script: RunicScript,
      font: RunicFont,
      modifier: Modifier = Modifier,
      fontSize: TextUnit = 24.sp,
      textAlign: TextAlign = TextAlign.Center
  )
  ```
- [ ] Create `ui/components/ScriptSelector.kt`
  - Segmented button group or tabs
  - Elder / Younger / Cirth options
- [ ] Create `ui/components/FontSelector.kt`
  - Dropdown or radio buttons
- [ ] Create `ui/components/QuoteCard.kt`
  - Display quote with author
  - Runic + Latin text toggle
- [ ] Create `ui/components/LoadingIndicator.kt`
- [ ] Create `ui/components/ErrorMessage.kt`

#### 2.3 Navigation Setup
- [ ] Create `ui/navigation/Screen.kt` sealed class
  ```kotlin
  sealed class Screen(val route: String) {
      object Quote : Screen("quote")
      object Settings : Screen("settings")
      object About : Screen("about")
      object QuoteList : Screen("quote_list")
  }
  ```
- [ ] Create `ui/navigation/RunicNavGraph.kt`
- [ ] Configure NavHost in `MainActivity.kt`

#### 2.4 Quote Screen
- [ ] Create `ui/quote/QuoteUiState.kt`
  ```kotlin
  data class QuoteUiState(
      val quote: QuoteEntity? = null,
      val script: RunicScript = RunicScript.ELDER_FUTHARK,
      val font: RunicFont = RunicFont.NOTO,
      val showLatin: Boolean = false,
      val isLoading: Boolean = false,
      val error: String? = null
  )
  ```
- [ ] Create `ui/quote/QuoteViewModel.kt`
  - Inject repository and preferences
  - `loadQuoteOfTheDay()`
  - `loadRandomQuote()`
  - `updateScript(RunicScript)`
  - `updateFont(RunicFont)`
  - `toggleLatinView()`
  - `shareQuote()`
- [ ] Create `ui/quote/QuoteScreen.kt`
  - App bar with menu
  - Centered runic quote display
  - Author attribution
  - Bottom controls: Next, Share, Settings
  - Script/Font selectors
  - Swipe to refresh

#### 2.5 Settings Screen
- [ ] Create `ui/settings/SettingsUiState.kt`
- [ ] Create `ui/settings/SettingsViewModel.kt`
  - All preference updates
  - Widget configuration
- [ ] Create `ui/settings/SettingsScreen.kt`
  - Preference categories
  - Script selection
  - Font selection
  - Widget settings
  - Theme selection
  - About button

#### 2.6 About Screen
- [ ] Create `ui/about/AboutScreen.kt`
  - App version
  - Credits
  - Font licenses
  - Open source licenses
  - Links to documentation

#### 2.7 Quote List Screen (Optional)
- [ ] Create `ui/quotelist/QuoteListViewModel.kt`
- [ ] Create `ui/quotelist/QuoteListScreen.kt`
  - LazyColumn with all quotes
  - Search functionality
  - Filter by favorite

### Phase 3 – Widget (Glance) (Est: 2-3 days)

#### 3.1 Widget Foundation
- [ ] Add Glance dependencies
- [ ] Create `widget/RunicQuoteWidget.kt` (GlanceAppWidget)
- [ ] Create `widget/RunicQuoteWidgetReceiver.kt` (GlanceAppWidgetReceiver)
- [ ] Create `widget/RunicQuoteWidgetContent.kt` (@Composable for Glance)
- [ ] Register widget in `AndroidManifest.xml`
- [ ] Create widget preview in `res/xml/runic_widget_info.xml`
- [ ] Create widget layout configuration

#### 3.2 Widget Data Management
- [ ] Create `widget/data/WidgetQuoteRepository.kt`
  - Interface for widget-specific data access
- [ ] Create Hilt entry point for widget
  ```kotlin
  @EntryPoint
  @InstallIn(SingletonComponent::class)
  interface WidgetEntryPoint {
      fun quoteRepository(): QuoteRepository
      fun preferencesManager(): UserPreferencesManager
  }
  ```
- [ ] Implement widget state management with StateFlow

#### 3.3 Widget UI
- [ ] Design widget layout (4x2 size)
  - Runic quote text
  - Author
  - Tap to open app
- [ ] Implement font rendering in Glance
- [ ] Add widget configuration activity (optional)
- [ ] Handle different widget sizes (small, medium, large)
- [ ] Add widget refresh button

#### 3.4 WorkManager Integration
- [ ] Create `worker/QuoteUpdateWorker.kt`
  ```kotlin
  @HiltWorker
  class QuoteUpdateWorker @AssistedInject constructor(
      @Assisted context: Context,
      @Assisted params: WorkerParameters,
      private val repository: QuoteRepository,
      private val prefsManager: UserPreferencesManager
  ) : CoroutineWorker(context, params)
  ```
- [ ] Implement daily quote update logic
- [ ] Create `worker/WorkerScheduler.kt` for periodic work
- [ ] Schedule work in Application onCreate
- [ ] Handle work constraints (network, battery)
- [ ] Add retry policy

### Phase 4 – Testing & QA (Est: 3-4 days)

#### 4.1 Unit Tests
- [ ] Create `test/transliteration/` package
  - `ElderFutharkTransliteratorTest.kt` - all runes
  - `YoungerFutharkTransliteratorTest.kt`
  - `CirthTransliteratorTest.kt`
  - Edge cases: numbers, punctuation, emojis
- [ ] Create `test/repository/QuoteRepositoryTest.kt`
  - Mock DAO with MockK
  - Test all repository methods
  - Test seeding logic
- [ ] Create `test/viewmodel/` tests
  - `QuoteViewModelTest.kt` with Turbine
  - `SettingsViewModelTest.kt`
  - Test state flows and updates

#### 4.2 Instrumented Tests
- [ ] Create `androidTest/database/` tests
  - `QuoteDaoTest.kt` with in-memory database
  - Test all queries
- [ ] Create `androidTest/ui/` tests
  - `QuoteScreenTest.kt` with Compose testing
  - `SettingsScreenTest.kt`
  - Test interactions and state changes
- [ ] Create `androidTest/preferences/` tests
  - `UserPreferencesManagerTest.kt`
  - Test DataStore operations

#### 4.3 Integration Tests
- [ ] Create end-to-end test suite
  - App launch → Quote display → Settings change → Quote update
- [ ] Test WorkManager integration
- [ ] Test widget update flow
- [ ] Test database migrations (when applicable)

#### 4.4 UI/Screenshot Tests
- [ ] Set up Paparazzi or Roborazzi for screenshot testing
- [ ] Create screenshot tests for:
  - Quote screen (all scripts)
  - Settings screen
  - Widget (all sizes)
  - Dark/light themes
- [ ] Add visual regression tests

#### 4.5 Performance & Quality
- [ ] Run Detekt and fix all issues
  - Configure `detekt.yml`
  - Set up custom rules
- [ ] Run Android Lint and fix warnings
- [ ] Profile app with Android Profiler
  - Memory leaks check
  - Rendering performance
- [ ] Test on multiple devices/emulators
  - Various API levels (26-34)
  - Different screen sizes
  - Tablets and foldables
- [ ] Accessibility audit
  - TalkBack testing
  - Content descriptions
  - Touch target sizes

### Phase 5 – CI/CD & Deployment (Est: 1-2 days)

#### 5.1 GitHub Actions Setup
- [ ] Create `.github/workflows/ci.yml`
  ```yaml
  name: CI
  on: [push, pull_request]
  jobs:
    build:
      - Detekt
      - Unit tests
      - Instrumented tests (with emulator)
      - Build debug APK
      - Upload artifacts
  ```
- [ ] Create `.github/workflows/release.yml`
  - Build release APK/AAB
  - Sign with keystore
  - Upload to GitHub releases
- [ ] Set up Dependabot for dependency updates
- [ ] Add status badges to README

#### 5.2 Code Quality Gates
- [ ] Enforce Detekt in CI (no failures allowed)
- [ ] Require 80%+ test coverage
- [ ] Add PR template
- [ ] Add issue templates
- [ ] Configure branch protection rules

#### 5.3 Documentation
- [ ] Create proper `README.md` with:
  - Screenshots
  - Features list
  - Installation instructions
  - Contributing guide
- [ ] Add KDoc comments to public APIs
- [ ] Generate documentation with Dokka
- [ ] Create architecture diagrams

### Phase 6 – Polish & Advanced Features (Est: 3-5 days)

#### 6.1 Quote Sharing
- [ ] Create `ui/share/ShareQuoteViewModel.kt`
- [ ] Implement quote-to-image export
  - Canvas drawing with runic text
  - Custom background (stone texture)
  - Watermark
- [ ] Add share intent
- [ ] Support sharing to social media

#### 6.2 User-Added Quotes
- [ ] Add `isUserCreated` field to `QuoteEntity`
- [ ] Create Add/Edit quote screen
  - Input fields for quote and author
  - Preview runic transliteration
- [ ] Implement quote management
  - Delete user quotes
  - Edit user quotes
- [ ] Add favorites system
  - Boolean flag in entity
  - Favorites filter

#### 6.3 Themes & Visual Polish
- [ ] Create theme variants:
  - Stone background
  - Wood texture
  - Parchment style
- [ ] Implement dynamic theming
- [ ] Add animated transitions:
  - Rune fade-in animation
  - Page transitions
  - Particle effects (optional)
- [ ] Add haptic feedback

#### 6.4 Advanced Widget Features
- [ ] Multiple widget sizes/layouts
- [ ] Widget configuration screen
- [ ] Interactive widget buttons
  - Next quote
  - Favorite
- [ ] Widget themes matching app

#### 6.5 Additional Scripts
- [ ] Add Anglo-Saxon Futhorc
  - New transliterator
  - Font support
- [ ] Add Medieval Runes (if fonts available)
- [ ] Add transliteration accuracy mode
  - Strict vs. phonetic

#### 6.6 Cloud Features (Optional)
- [ ] Firebase integration
- [ ] Quote backup/restore
- [ ] Cross-device sync
- [ ] Community quotes (moderated)

### Phase 7 – Release Preparation (Est: 2-3 days)

#### 7.1 Play Store Assets
- [ ] Create app icon (adaptive icon)
- [ ] Create feature graphic (1024x500)
- [ ] Take screenshots (phone + tablet)
  - All major features
  - Multiple languages (if applicable)
- [ ] Create promotional video (optional)
- [ ] Write store description
- [ ] Translate store listing (optional)

#### 7.2 Legal & Compliance
- [ ] Privacy policy (if collecting data)
- [ ] Terms of service
- [ ] Open source licenses file
- [ ] Font license compliance
- [ ] GDPR compliance (if applicable)

#### 7.3 Final Testing
- [ ] Internal testing track
- [ ] Closed beta testing
- [ ] Gather feedback and iterate
- [ ] Performance optimization
- [ ] Battery usage optimization
- [ ] APK size optimization
  - ProGuard/R8 configuration
  - Resource shrinking

#### 7.4 Release
- [ ] Version 1.0.0 release
- [ ] Submit to Google Play
- [ ] Create GitHub release with changelog
- [ ] Announcement on social media
- [ ] Monitor crash reports
- [ ] Plan for updates

---

## 10. Roadmap

### v1.0.0 – MVP (Minimum Viable Product)
**Target: 3-4 weeks** | **Priority: Critical**

**Core Features:**
- ✅ Complete Android Compose app architecture
- ✅ Runic fonts fully integrated (Elder, Younger, Cirth)
- ✅ Database preloaded with 100+ quotes
- ✅ Script switching (Elder/Younger/Cirth)
- ✅ Basic home screen widget
- ✅ Daily quote update logic via WorkManager
- ✅ Material 3 Design implementation
- ✅ Room database persistence
- ✅ DataStore preferences
- ✅ Hilt dependency injection
- ✅ Basic navigation (Quote/Settings/About)

**Testing:**
- Unit tests for transliterators (>90% coverage)
- Repository tests
- ViewModel tests
- Basic UI tests

**Deliverables:**
- Fully functional APK
- CI/CD pipeline active
- README with screenshots
- Open source on GitHub

---

### v1.1.0 – Enhanced Customization
**Target: 2-3 weeks after v1.0** | **Priority: High**

**Features:**
- Font selector UI (Noto/BabelStone for runic scripts)
- Widget configuration options
  - Update frequency (daily/manual)
  - Widget theme selection
- Quote sharing to social media
  - Export quote as image with runic text
  - Custom backgrounds
- Improved styling and animations
  - Smooth transitions between quotes
  - Better loading states
- Settings screen enhancements
  - Theme mode (Light/Dark/Auto)
  - Accessibility options

**Improvements:**
- Performance optimization
- Better error handling
- Enhanced widget layouts
- Tablet UI optimization

**Deliverables:**
- Play Store beta release
- User documentation
- Video demo

---

### v1.2.0 – User Content & Advanced Theming
**Target: 3-4 weeks after v1.1** | **Priority: Medium**

**Features:**
- **User-created quotes:**
  - Add custom quotes with auto-transliteration
  - Edit/delete user quotes
  - Separate tab for user quotes
- **Favorites system:**
  - Mark quotes as favorites
  - Filter by favorites
  - Widget can show favorite quotes
- **Theme system:**
  - Stone background theme
  - Wood texture theme
  - Parchment style theme
  - Custom color schemes
- **Advanced widget:**
  - Multiple widget sizes (2x2, 4x2, 4x4)
  - Interactive buttons (next quote, favorite)
  - Widget-specific themes

**Data Management:**
- Local backup/restore
- Export quotes to JSON
- Import quotes from file

**Deliverables:**
- Public Play Store release
- Marketing materials
- User tutorial

---

### v1.3.0 – Polish & Performance
**Target: 2-3 weeks after v1.2** | **Priority: Medium**

**Features:**
- Animated runic transitions
  - Fade-in animations for runes
  - Particle effects (optional)
- Quote search functionality
- Quote categories/tags
- Statistics (quotes viewed, favorites count)
- App shortcuts for quick actions
- Improved accessibility
  - TalkBack support
  - Content descriptions for all runes
  - Adjustable text sizes

**Performance:**
- Database query optimization
- Widget rendering optimization
- Reduced APK size
- Battery usage optimization
- Memory leak fixes

**Deliverables:**
- Performance audit report
- Accessibility compliance report

---

### v2.0.0 – Extended Scripts & Cloud Features
**Target: 4-6 weeks after v1.3** | **Priority: Low-Medium**

**New Scripts:**
- Anglo-Saxon Futhorc
  - Complete transliterator
  - Font support
- Medieval Runes (if fonts available)
- Optional: Gothic script
- Transliteration modes:
  - Strict mode (historical accuracy)
  - Phonetic mode (modern pronunciation)

**Cloud Features (Optional):**
- Firebase integration
  - User authentication (optional)
  - Cloud backup/restore
  - Cross-device sync
- Community quotes (moderated):
  - Submit quotes for review
  - Vote on community quotes
  - Admin moderation panel

**Advanced Features:**
- Quote collections/playlists
- Scheduled quotes (not just daily)
- Quote notifications
- Lock screen widget (if Android supports)

**Deliverables:**
- Major version release
- Press release
- App Store feature consideration

---

### v2.1.0+ – Future Enhancements (Backlog)
**Target: TBD** | **Priority: Nice-to-have**

**Ideas for Consideration:**
- Runic keyboard module (IME)
- Learn runic alphabet mode
  - Interactive tutorials
  - Practice writing runes
- Audio pronunciation guide
- Historical context for quotes
- Runic calendar integration
- Watch OS companion app
- AR mode (view runes in space)
- Multiple languages support
  - UI translations
  - Quote translations
- Premium features (optional):
  - Advanced themes
  - Ad-free experience
  - Exclusive quote packs
- Integration with reading apps
- Export to PDF/epub format

---

### Roadmap Summary Table

| Version | Features | Timeline | Status |
|---------|----------|----------|--------|
| v1.0.0 | MVP - Core app, fonts, widget | 3-4 weeks | Planned |
| v1.1.0 | Customization, sharing | +2-3 weeks | Planned |
| v1.2.0 | User quotes, themes, favorites | +3-4 weeks | Planned |
| v1.3.0 | Polish, animations, performance | +2-3 weeks | Planned |
| v2.0.0 | Additional scripts, cloud | +4-6 weeks | Planned |
| v2.1.0+ | Future enhancements | TBD | Backlog |

**Total Estimated Timeline to v2.0:** ~14-20 weeks (3.5-5 months)

---

## 11. Troubleshooting & Known Issues

### Common Build Issues

#### Issue: Kotlin version mismatch
**Error:** `The Kotlin Gradle plugin was loaded multiple times`
**Solution:**
```gradle
// Ensure consistent Kotlin version in gradle/libs.versions.toml
kotlin = "2.2.21"
// Clean and rebuild
./gradlew clean build
```

#### Issue: Compose compiler version incompatibility
**Error:** `androidx.compose.compiler:compiler version mismatch`
**Solution:**
- Ensure `composeOptions.kotlinCompilerExtensionVersion` matches Compose BOM
- Check Kotlin-Compose compatibility matrix: https://developer.android.com/jetpack/androidx/releases/compose-kotlin

#### Issue: Hilt kapt errors
**Error:** `Hilt processor cannot find @HiltAndroidApp`
**Solution:**
```kotlin
// Ensure kapt is applied before hilt plugin in build.gradle.kts
plugins {
    kotlin("kapt")
    id("com.google.dagger.hilt.android")
}
```

#### Issue: Room schema export location
**Error:** `Schema export directory is not provided`
**Solution:**
```kotlin
android {
    defaultConfig {
        javaCompileOptions {
            annotationProcessorOptions {
                arguments["room.schemaLocation"] = "$projectDir/schemas"
            }
        }
    }
}
```

### Runtime Issues

#### Issue: Font not rendering in widget
**Symptom:** Widget shows blank squares instead of runes
**Solution:**
- Verify font files are in `res/font/`
- Check font file names match FontFamily definitions
- Ensure Glance supports custom fonts (limitation in early Glance versions)
- Fallback: Use Unicode runes without custom font

#### Issue: Widget not updating
**Symptom:** Widget shows old quote after daily update
**Solution:**
- Check WorkManager constraints
- Verify GlanceAppWidgetReceiver is registered in manifest
- Force widget update:
```kotlin
context.sendBroadcast(Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE))
```

#### Issue: Database seeding fails
**Symptom:** Empty quote list or crash on first launch
**Solution:**
- Check QuoteSeedData.kt for syntax errors
- Ensure database version is correct
- Clear app data and reinstall
- Add logging to seedDatabaseIfNeeded()

#### Issue: DataStore corruption
**Symptom:** App crashes on preference read
**Solution:**
```kotlin
// Add error handling
dataStore.data
    .catch { exception ->
        if (exception is IOException) {
            emit(emptyPreferences())
        } else {
            throw exception
        }
    }
```

### Testing Issues

#### Issue: Robolectric tests fail with "SDK not found"
**Solution:**
```gradle
testOptions {
    unitTests {
        isIncludeAndroidResources = true
        all {
            it.systemProperty("robolectric.enabledSdks", "26,30,33")
        }
    }
}
```

#### Issue: Compose UI tests timeout
**Solution:**
- Use `composeTestRule.waitForIdle()`
- Increase timeout in test configuration
- Check for infinite recomposition loops

### Performance Issues

#### Issue: Slow quote loading
**Solution:**
- Index database columns used in queries
- Cache transliterated quotes
- Use Flow instead of suspend functions for real-time updates

#### Issue: Widget rendering lag
**Solution:**
- Reduce widget text length
- Simplify widget layout
- Avoid complex calculations in widget composables
- Precompute widget data in Worker

### Known Limitations

1. **Cirth font availability:** Limited free fonts for Tolkien's Cirth
2. **Glance limitations:** Not all Compose features work in Glance
3. **Font rendering:** Some Android versions may have runic font rendering issues
4. **Widget size:** Minimum widget size constraints vary by launcher
5. **API 26 support:** Some Material3 features require newer APIs

---

## 12. Resources & References

### Official Documentation

**Android Development:**
- [Android Developers](https://developer.android.com/)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Material Design 3](https://m3.material.io/)
- [**Material 3 Expressive**](https://m3.material.io/expressive) - Latest evolution with physics-based motion
- [Material 3 in Compose](https://developer.android.com/develop/ui/compose/designsystems/material3)
- [Material Theme Builder](https://material.io/material-theme-builder) - Generate color schemes
- [Roboto Flex Variable Font](https://fonts.google.com/specimen/Roboto+Flex) - M3 Expressive typography
- [Jetpack Glance](https://developer.android.com/jetpack/androidx/releases/glance)

**Kotlin:**
- [Kotlin Documentation](https://kotlinlang.org/docs/home.html)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
- [Kotlin Flow](https://kotlinlang.org/docs/flow.html)

**Libraries:**
- [Hilt Documentation](https://dagger.dev/hilt/)
- [Room Database](https://developer.android.com/training/data-storage/room)
- [DataStore](https://developer.android.com/topic/libraries/architecture/datastore)
- [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager)

### Runic Scripts & Fonts

**Unicode Standards:**
- [Unicode Runic Block (U+16A0–U+16FF)](https://unicode.org/charts/PDF/U16A0.pdf)
- [Unicode Character Table - Runic](https://symbl.cc/en/unicode/blocks/runic/)

**Fonts:**
- [Noto Sans Runic](https://fonts.google.com/noto/specimen/Noto+Sans+Runic) - Google Fonts (OFL)
- [BabelStone Fonts](https://www.babelstone.co.uk/Fonts/) - Free runic fonts
- [Omniglot: Runic Alphabets](https://omniglot.com/writing/runic.htm)
- [Cirth (Angerthas)](https://www.evertype.com/standards/csur/cirth.html) - PUA allocation

**Learning Resources:**
- [Elder Futhark Guide](https://en.wikipedia.org/wiki/Elder_Futhark)
- [Younger Futhark](https://en.wikipedia.org/wiki/Younger_Futhark)
- [Tolkien's Cirth](https://tolkiengateway.net/wiki/Cirth)
- [Rune Converter Tools](https://www.runesecrets.com/rune-converter)

### Testing Resources

**Testing Libraries:**
- [JUnit 4](https://junit.org/junit4/)
- [MockK](https://mockk.io/)
- [Turbine](https://github.com/cashapp/turbine) - Flow testing
- [Truth](https://truth.dev/) - Assertions library
- [Robolectric](http://robolectric.org/)
- [Compose Testing](https://developer.android.com/jetpack/compose/testing)

**Best Practices:**
- [Android Testing Codelab](https://developer.android.com/codelabs/advanced-android-kotlin-training-testing-basics)
- [Testing Compose Layouts](https://developer.android.com/jetpack/compose/testing)
- [Testing ViewModels](https://developer.android.com/codelabs/android-testing)

### Code Quality & CI/CD

- [Detekt](https://detekt.dev/) - Kotlin static analysis
- [KtLint](https://github.com/pinterest/ktlint) - Kotlin linter
- [GitHub Actions for Android](https://github.com/marketplace?type=actions&query=android)
- [Dependabot](https://github.com/dependabot)

### Design Inspiration

- [Dribbble: Quote Apps](https://dribbble.com/search/quote-app)
- [Material Design Case Studies](https://material.io/design/material-studies)
- [Viking/Norse Design Patterns](https://www.pinterest.com/search/pins/?q=viking%20app%20design)

### Community & Support

- [r/androiddev](https://reddit.com/r/androiddev) - Android development community
- [Kotlin Slack](https://kotlinlang.slack.com/)
- [Android Development Discord](https://discord.gg/android-dev)
- [Stack Overflow - Android](https://stackoverflow.com/questions/tagged/android)
- [Stack Overflow - Jetpack Compose](https://stackoverflow.com/questions/tagged/android-jetpack-compose)

### Related Projects

- [Norse Mythology Apps](https://play.google.com/store/search?q=norse%20runes&c=apps)
- [Rune Translation Apps](https://play.google.com/store/search?q=runic%20translator&c=apps)
- [Daily Quote Apps](https://play.google.com/store/search?q=daily%20quotes&c=apps)

### Books & Publications

- "Android UI Development with Jetpack Compose" by Thomas Künneth
- "Kotlin Coroutines: Deep Dive" by Marcin Moskała
- "The Complete Runelore" by Nigel Pennick
- "Runes: A Handbook" by Michael P. Barnes

---

## 13. Contributing & License

### Contributing

Contributions are welcome! Please:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Follow the coding standards (run Detekt)
4. Write tests for new features
5. Commit your changes with clear messages
6. Push to your branch
7. Open a Pull Request

**Coding Standards:**
- Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Pass Detekt checks (no failures)
- Maintain 80%+ test coverage
- Use meaningful variable/function names
- Add KDoc for public APIs

### License

This project is licensed under the **MIT License** - see LICENSE file for details.

**Font Licenses:**
- Noto Sans Runic: SIL Open Font License (OFL)
- BabelStone Runic: Free for personal and commercial use
- Cirth Angerthas: Verify specific font license

**Third-Party Libraries:**
All dependencies are used under their respective licenses (Apache 2.0, MIT, etc.)

---

## 14. Acknowledgments

- **Google** - For Android, Jetpack Compose, and Material Design
- **JetBrains** - For Kotlin programming language
- **Font Creators** - Noto, BabelStone, and Cirth font developers
- **Unicode Consortium** - For standardizing runic characters
- **J.R.R. Tolkien** - For creating the Cirth (Angerthas) script
- **Open Source Community** - For libraries and tools

---

This comprehensive document defines the complete technical blueprint, architecture, detailed implementation TODO list, and strategic roadmap for the **Runic Quotes (Android)** application.

**Document Version:** 1.0
**Last Updated:** 2025-11-15
**Status:** Ready for Implementation

