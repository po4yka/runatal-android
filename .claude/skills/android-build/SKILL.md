---
name: android-build
description: "Project-specific build, test, lint, and CI commands for the Runic Quotes Android app. Use when: (1) building the app, (2) running tests (unit or instrumented), (3) running static analysis (Detekt/Lint), (4) checking coverage, (5) debugging CI failures, or (6) any Gradle task. Triggers on: build, compile, test, lint, detekt, coverage, APK, CI, Gradle."
---

# Android Build

## Quick Reference

```bash
# Build
./gradlew assembleDebug              # Debug APK
./gradlew assembleRelease             # Release APK (needs signing)
./gradlew bundleRelease               # AAB for Play Store

# Test
./gradlew testDebugUnitTest           # All unit tests
./gradlew test --tests "ClassName"    # Single test class
./gradlew connectedDebugAndroidTest   # Instrumented tests (emulator required)

# Coverage
./gradlew testDebugUnitTest jacocoTestReport   # Tests + HTML/XML report
./gradlew jacocoTestCoverageVerification       # Enforce 80% minimum

# Static Analysis
./gradlew detekt                      # Detekt (maxIssues=0, must be clean)
./gradlew lintDebug                   # Android Lint

# Combined
./gradlew check                       # All checks (test + lint + detekt)
```

## Key Build Facts

- **Single module**: `:app` only
- **SDK**: minSdk 26, targetSdk 36, compileSdk 36
- **JDK**: 17 (set in `gradle-daemon-jvm.properties`)
- **Gradle**: 9.3.1 with configuration cache enabled
- **Kotlin**: 2.3.10, Compose BOM 2026.02.01
- **Version catalog**: `gradle/libs.versions.toml`

## Detekt Rules

Detekt runs with `maxIssues = 0` -- any issue fails the build. Key thresholds:
- MaxLineLength: 120 (excludes imports/package)
- LongMethod: 100 lines (excludes `@Composable`)
- LongParameterList: 8 params (ignores defaults and Composables)
- TooManyFunctions: 15 (excludes DAOs/repositories)
- KDoc required for public classes/functions (excludes tests, UI screens)

Use `@Suppress("RuleName")` only with a justification comment.

## Coverage

JaCoCo targets:
- 80% minimum (enforced by `jacocoTestCoverageVerification`)
- 90% target for business logic (transliterators, repositories)
- Reports: `app/build/reports/jacoco/jacocoTestReport/html/index.html`

Included: transliterators, repositories, ViewModels.
Excluded: generated code, Hilt classes, UI screens, Compose.

## CI Pipeline (`.github/workflows/ci.yml`)

5 jobs in order:
1. **dependency-check** - Gradle dependency review
2. **static-analysis** - Detekt + Android Lint
3. **unit-tests** - Tests + coverage upload to Codecov
4. **build-apk** - `assembleDebug` (depends on 1-3)
5. **instrumented-tests** - Connected tests on emulator (PR-only, macOS runner)

## Troubleshooting

- **KSP errors**: Run `./gradlew clean` then rebuild. Check `libs.versions.toml` KSP version matches Kotlin.
- **Detekt failures**: Run `./gradlew detekt` locally, fix all issues. Check `detekt.yml` for rule config.
- **Room schema mismatch**: Export schema with `./gradlew kspDebugKotlin`, check `app/schemas/` for JSON diffs.
- **Config cache issues**: `./gradlew --no-configuration-cache assembleDebug` to bypass.
