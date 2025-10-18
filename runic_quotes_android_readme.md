# Runic Quotes (Android) – README / TODO / ROADMAP

## 1. Overview

**Runic Quotes** is an Android application built with a modern Kotlin/Compose architecture that displays inspirational and literary quotes rendered in ancient runic scripts:

- **Elder Futhark**
- **Younger Futhark**
- **Tolkien’s Cirth (Angerthas, PUA-mapped)**

The app integrates custom runic fonts (Noto Sans Runic, BabelStone Runic, and a Cirth font) and provides a material, reactive UI powered by Jetpack Compose, with a home-screen widget built using Jetpack Glance.

The project emphasizes:
- Clean architecture with DI (Hilt)
- Reactive data flows (coroutines + Flow)
- Modern UI (Compose + Material3)
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
  - Material 3 components
  - Compose BOM
  - UI, animation, tooling
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

## 3. Runic Fonts & Rendering

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

### 6.1 Detekt
- Configured in `detekt.yml`
- Integrated into CI

### 6.2 GitHub Actions
Common CI pipeline:
- Run Detekt
- Run unit tests
- Run Robolectric tests
- Assemble debug build

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

## 8. TODO List

### Phase 1 – Infrastructure
- [ ] Create project structure, modules, DI graph
- [ ] Configure AGP, Kotlin, Compose BOM
- [ ] Add Room, DataStore, Hilt, Navigation dependencies
- [ ] Implement QuoteEntity, QuoteDao, Database
- [ ] Implement transliteration utilities
- [ ] Integrate runic fonts into `res/font`
- [ ] Implement QuoteRepository

### Phase 2 – UI & ViewModels
- [ ] QuoteViewModel with state flows
- [ ] QuoteScreen UI
- [ ] SettingsScreen
- [ ] Script + Font selector components

### Phase 3 – Widget
- [ ] Implement Glance widget
- [ ] Hilt entry point for repository
- [ ] Widget UI rendering with custom fonts
- [ ] WorkManager updates

### Phase 4 – Testing & QA
- [ ] Transliteration tests
- [ ] Repository tests
- [ ] Compose UI tests
- [ ] Hilt instrumented tests
- [ ] Robolectric tests
- [ ] CI pipeline setup

### Phase 5 – Polish & Advanced
- [ ] Quote sharing (image export)
- [ ] User-added quotes feature
- [ ] Themes (stone/wood/parchment)
- [ ] Animated rune transitions
- [ ] Optional: downloadable fonts integration

---

## 9. Roadmap

### v1.0 – MVP
- Working Compose app
- Runic fonts integrated
- Quote list preloaded
- Script switching
- Basic widget
- Daily update logic

### v1.1 – Customization
- Font selector
- Widget options
- Styling polish
- Sharing a quote image

### v1.2 – User Content & Themes
- User quotes CRUD
- Color themes
- Advanced widget layouts

### v2.0 – Advanced Features
- Animated runic transitions
- Additional scripts (Anglo-Saxon Futhorc)
- Cloud backup / sync
- Optional runic keyboard module

---

This document defines the technical blueprint, architecture, TODO list, and roadmap for the **Runic Quotes (Android)** application.

