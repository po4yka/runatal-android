# Runic Quotes - Claude Code Project Context

## Project Overview

Android app displaying inspirational quotes transliterated into ancient runic scripts (Elder Futhark, Younger Futhark, Cirth). Single-module Clean Architecture + MVVM, built with Jetpack Compose and Kotlin.

## Tech Stack

- **Kotlin** 2.3.10, **Compose BOM** 2026.02.01, **AGP** 9.1.0, **Gradle** 8.1.3
- **SDK**: minSdk 26, targetSdk/compileSdk 36, **JDK** 17
- **DI**: Hilt 2.59.2 | **DB**: Room 2.8.4 | **Prefs**: DataStore 1.2.0
- **Nav**: Navigation 3 (type-safe routes) | **Widget**: Glance 1.1.1
- **Background**: WorkManager 2.11.1
- **Testing**: JUnit 4 + MockK 1.14.9 + Turbine 1.2.1 + Truth 1.4.5 + Robolectric
- **Static Analysis**: Detekt 1.23.8 (strict: maxIssues = 0)
- **Versions**: Centralized in `gradle/libs.versions.toml`

## Build Commands

```bash
./gradlew assembleDebug                    # Build debug APK
./gradlew testDebugUnitTest                # Unit tests
./gradlew testDebugUnitTest jacocoProjectCoverageReport  # Project coverage report
./gradlew jacocoTransliterationCoverageVerification      # Coverage gate
./gradlew detekt                           # Static analysis (must pass with 0 issues)
./gradlew lintDebug                        # Android lint
./gradlew check                            # All checks
./gradlew test --tests "ClassName"         # Single test class
```

## Project Structure

```
app/src/main/java/com/po4yka/runicquotes/
  data/           # Room DB, DAOs, entities, DataStore, repository impls
  domain/         # Models (Quote, RunicScript, etc.) + transliteration logic
  di/             # Hilt modules (Database, Repository, DataStore, WorkManager, Util)
  ui/             # Compose screens, ViewModels, components, theme, navigation, widget
  worker/         # WorkManager background jobs
  util/           # Utilities (share manager)
```

## Architecture Rules

- **Layers**: UI -> Domain <- Data (dependency rule: outer depends on inner)
- **State**: `StateFlow` in ViewModels, `sealed class/interface` for UI state (Loading | Success | Error | Empty)
- **Data flow**: User Action -> ViewModel -> Repository -> Room DB -> Flow -> Compose
- **Room** is single source of truth; UI never accesses data sources directly
- **Repository**: Interface + Impl separation; all data mapped through domain models
- **DI**: Constructor injection via Hilt; `@HiltViewModel`, `@AndroidEntryPoint`, `@HiltWorker`
- **Compose**: State hoisting (ViewModel owns state), `LaunchedEffect` for side effects, no `remember { mutableStateOf() }` for business logic

## Code Conventions

- Detekt enforces most style rules; 120-char line limit, 4-space indent
- KDoc required for public classes/functions (except tests, UI screens -- detekt handles this)
- Test function naming: backtick descriptive format `` `transliterate f to FEHU`() ``
- Conventional Commits: `feat:`, `fix:`, `docs:`, `refactor:`, `chore:`, `test:`
- Branch naming: `feature/`, `fix/`, `refactor/`, `docs/`, `test/`, `chore/`

## Testing Patterns

- **Transliterators**: Pure unit tests, no mocking needed
- **Repositories**: MockK for DAO dependencies
- **ViewModels**: Turbine for Flow testing, `runTest` for coroutines
- **Assertions**: Use Google Truth (`assertThat(...).isEqualTo(...)`)
- **Coverage gates**: 80% minimum (JaCoCo), 90% target for business logic

## Key Domain Knowledge

- Three runic scripts: Elder Futhark (2nd-8th c.), Younger Futhark (9th-11th c.), Cirth/Angerthas (Tolkien)
- `TransliteratorFactory` selects the right transliterator based on `RunicScript` enum
- Seed data pre-populates the Room database with quotes on first launch
- Home screen widget uses Glance framework (not legacy RemoteViews)

## Do-Not Rules

- Do NOT add SharedPreferences; use DataStore exclusively
- Do NOT use `GlobalScope`; use `viewModelScope` or structured concurrency
- Do NOT bypass detekt (`@Suppress` only with justification comment)
- Do NOT add new Hilt modules without clear separation rationale
- Do NOT use string-based navigation; use Navigation 3 type-safe route objects
