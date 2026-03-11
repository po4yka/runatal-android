# [runatal-android] AI Agent Guidelines

Cross-tool agent configuration for the Runic Quotes Android project. This file follows the [AGENTS.md](https://agents.md/) open standard and complements the Claude Code-specific `CLAUDE.md`.

## Project Summary

Single-module Android app (Kotlin + Jetpack Compose) that transliterates quotes into runic scripts. Clean Architecture + MVVM. Namespace: `com.po4yka.runicquotes`.

## Build and Test Commands

```bash
# Build
./gradlew assembleDebug

# Test
./gradlew testDebugUnitTest
./gradlew test --tests "com.po4yka.runicquotes.domain.transliteration.ElderFutharkTransliteratorTest"

# Static analysis (zero tolerance -- must report 0 issues)
./gradlew detekt

# Lint
./gradlew lintDebug

# Coverage
./gradlew testDebugUnitTest jacocoProjectCoverageReport
# Report: app/build/reports/jacoco/projectCoverage/html/index.html

# Coverage gate
./gradlew jacocoTransliterationCoverageVerification

# Full check pipeline (mirrors CI)
./gradlew check
```

## Code Style Guidelines

### Language and Formatting
- **Kotlin** exclusively (no Java source files)
- 4-space indentation, 120-character line limit
- Detekt with `maxIssues = 0` enforces all style rules -- run `./gradlew detekt` before committing
- Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)

### Naming
| Element | Convention | Example |
|---------|-----------|---------|
| Classes | PascalCase | `QuoteViewModel` |
| Functions | camelCase | `loadQuoteOfTheDay()` |
| Composables | PascalCase | `QuoteScreen()` |
| Constants | UPPER_SNAKE_CASE | `MAX_QUOTE_LENGTH` |
| Test functions | Backtick descriptive | `` `transliterate f to FEHU`() `` |

### Architecture Constraints
- **Clean Architecture layers**: `ui/` (presentation) -> `domain/` (business logic) <- `data/` (persistence)
- Domain layer must not depend on Android framework classes
- All data access through Repository interfaces (defined in `data/repository/`)
- UI state represented as `sealed class`/`sealed interface` with exhaustive `when` branches
- ViewModels expose `StateFlow<UiState>`; Compose screens collect via `collectAsStateWithLifecycle()`

### Dependency Injection
- Hilt for all DI; constructor injection only (no field injection)
- Existing modules: `DatabaseModule`, `RepositoryModule`, `DataStoreModule`, `WorkManagerModule`, `UtilModule`
- New repositories: add binding to `RepositoryModule`; new DAOs: add provider to `DatabaseModule`

## Testing Instructions

### Frameworks
- **JUnit 4** test runner
- **MockK** for mocking (Kotlin-native, use `mockk<T>()`, `every { }`, `coEvery { }`)
- **Turbine** for Flow testing (`flow.test { awaitItem(); cancelAndIgnoreRemainingEvents() }`)
- **Truth** for assertions (`assertThat(actual).isEqualTo(expected)`)
- **Robolectric** for tests requiring Android context

### Test Organization
```
app/src/test/          # Unit tests (run on JVM)
app/src/androidTest/   # Instrumented tests (run on device/emulator)
```

### Coverage Requirements
- Project-wide coverage is reported via `jacocoProjectCoverageReport` and merges Android test coverage when it exists
- 90% line coverage is enforced for `domain/transliteration` via `jacocoTransliterationCoverageVerification`
- Repository and ViewModel coverage targets remain 90% and 80% respectively, tracked through the project report
- Excludes: generated code (Hilt, Room), test files, BuildConfig

### Writing Tests
- One test class per production class, same package structure
- Use `@Before` for common setup; prefer focused test methods
- For coroutine tests: use `runTest` from `kotlinx-coroutines-test`
- For ViewModel tests: set `Dispatchers.setMain(UnconfinedTestDispatcher())` in `@Before`

## Security Considerations

- No API keys or secrets in source code; sensitive values go in `local.properties` (git-ignored)
- ProGuard/R8 enabled for release builds with minification and resource shrinking
- Room database is local-only; no network data transmission
- No WebView usage; no dynamic code loading

## Commit and PR Guidelines

### Commits
- [Conventional Commits](https://www.conventionalcommits.org/): `type(scope): description`
- Types: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`, `perf`, `ci`
- Subject line under 72 characters, imperative mood
- Example: `feat(quotes): add archive functionality for user quotes`

### Branches
- `feature/description`, `fix/description`, `refactor/description`, `docs/description`
- Branch from `main`, PR back to `main`

### Pull Requests
- Must pass CI: dependency check, detekt, lint, unit tests, build
- Coverage must not decrease on changed files (70% overall, 80% changed files)
- Fill out PR template; link related issues

## CI/CD Pipeline

GitHub Actions (`.github/workflows/ci.yml`) runs on push to main/develop and PRs:
1. **Dependency Check** -- security review of dependencies
2. **Lint** -- detekt + Android lint (reports uploaded as artifacts)
3. **Unit Tests** -- JUnit tests + JaCoCo coverage + Codecov upload
4. **Build** -- assembleDebug (gated on lint + test passing)
5. **Instrumented Tests** -- Espresso on emulator (PR-only, macOS runner)

## File Structure Reference

```
app/src/main/java/com/po4yka/runicquotes/
  data/
    local/          # Room: RunicQuotesDatabase, DAOs, entities
    preferences/    # DataStore: UserPreferencesManager
    repository/     # Repository interfaces and implementations
    seed/           # JSON seed data for initial DB population
  domain/
    model/          # Quote, RunicScript, QuotePack, RuneReference
    transliteration/  # ElderFuthark/YoungerFuthark/Cirth transliterators + factory
  di/               # Hilt modules
  ui/
    screens/        # Screen composables + ViewModels (quote/, settings/, etc.)
    components/     # Reusable composables (RunicText, SegmentedControl, etc.)
    navigation/     # Navigation 3 type-safe route definitions + NavGraph
    theme/          # Material 3 Color, Theme, Typography
    widget/         # Glance home screen widget
  worker/           # WorkManager periodic tasks
  util/             # ShareManager, extensions
```

## Common Pitfalls

- Room schema changes require a migration in `RunicQuotesDatabase`; export schema is enabled
- Glance widgets use their own Compose runtime (`androidx.glance.`) -- do not mix with regular Compose imports
- Navigation 3 uses route objects, not string routes; check `ui/navigation/` for patterns
- Detekt `LongParameterList` threshold is 8 for functions (ignores `@Composable` and default params)
- DataStore must be singleton-scoped; multiple instances cause `CorruptionException`
