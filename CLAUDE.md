# Runatal - Claude Code Project Context

## Project Overview

Android app displaying inspirational quotes transliterated into ancient runic scripts (Elder Futhark, Younger Futhark, Cirth). Single-module Clean Architecture + MVVM, built with Jetpack Compose and Kotlin.

## Tech Stack

- **Kotlin + Compose**: Jetpack Compose with Material 3 Expressive; built-in Kotlin support in AGP
- **SDK**: minSdk 26, targetSdk/compileSdk 37; **Gradle runtime**: JDK 21; **bytecode target**: Java 17
- **DI**: Hilt with `androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel`
- **DB / preferences / background**: Room 3, DataStore, WorkManager
- **Nav / widget**: Navigation 3 type-safe routes, Glance
- **Testing**: JUnit 4, MockK, Turbine, Truth, Robolectric, Konsist; JaCoCo coverage
- **Static analysis**: Detekt with `maxIssues = 0`, Android lint
- **Version source of truth**: `gradle/libs.versions.toml`; Gradle distribution: `gradle/wrapper/gradle-wrapper.properties`

## Build Commands

```bash
./gradlew assembleDebug                    # Build debug APK
./gradlew testDebugUnitTest                # Unit tests
./gradlew testDebugUnitTest jacocoProjectCoverageReport  # Project coverage report
./gradlew jacocoTransliterationCoverageVerification      # Transliteration coverage gate
./gradlew jacocoTranslationCoverageVerification          # Translation coverage gate
./gradlew detekt                           # Static analysis (must pass with 0 issues)
./gradlew lintDebug                        # Android lint
./gradlew check                            # All checks
./gradlew test --tests "ClassName"         # Single test class
```

On this Mac, wrap heavy local commands with `build-gate --` and keep Gradle workers at four or fewer.

## Project Structure

```
app/src/main/java/com/po4yka/runatal/
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
- **Coverage gates**: `jacocoTransliterationCoverageVerification` and `jacocoTranslationCoverageVerification`; thresholds are defined in `app/build.gradle.kts`
- **CI device**: Android 17 (API 37) emulator for PRs, main pushes, and manual runs

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
