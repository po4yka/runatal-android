# Contributing to Runic Quotes

Thank you for your interest in contributing to Runic Quotes! This document provides guidelines and instructions for contributing to the project.

## Table of Contents
- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Workflow](#development-workflow)
- [Coding Standards](#coding-standards)
- [Testing Guidelines](#testing-guidelines)
- [Commit Message Guidelines](#commit-message-guidelines)
- [Pull Request Process](#pull-request-process)
- [Issue Reporting](#issue-reporting)

## Code of Conduct

### Our Pledge
We pledge to make participation in our project a harassment-free experience for everyone, regardless of age, body size, disability, ethnicity, gender identity and expression, level of experience, nationality, personal appearance, race, religion, or sexual identity and orientation.

### Our Standards
- Be respectful and inclusive
- Welcome newcomers and help them learn
- Accept constructive criticism gracefully
- Focus on what is best for the community
- Show empathy towards other community members

### Unacceptable Behavior
- Harassment, trolling, or inflammatory comments
- Publishing others' private information
- Other conduct which could reasonably be considered inappropriate

## Getting Started

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17 or higher
- Git
- Basic knowledge of Kotlin and Jetpack Compose

### Setting Up Development Environment

1. **Fork and Clone**
   ```bash
   # Fork the repository on GitHub
   # Then clone your fork
   git clone https://github.com/YOUR_USERNAME/runatal-android.git
   cd runatal-android
   ```

2. **Add Upstream Remote**
   ```bash
   git remote add upstream https://github.com/po4yka/runatal-android.git
   git fetch upstream
   ```

3. **Open in Android Studio**
   - Launch Android Studio
   - Select "Open an Existing Project"
   - Navigate to the cloned directory
   - Wait for Gradle sync to complete

4. **Verify Setup**
   ```bash
   # Run tests
   ./gradlew test

   # Run Detekt
   ./gradlew detekt

   # Build the app
   ./gradlew assembleDebug
   ```

## Development Workflow

### Creating a Feature Branch

```bash
# Update your main branch
git checkout main
git pull upstream main

# Create a feature branch
git checkout -b feature/your-feature-name
```

### Branch Naming Convention
- `feature/` - New features (e.g., `feature/add-futhorc-script`)
- `fix/` - Bug fixes (e.g., `fix/widget-crash`)
- `refactor/` - Code refactoring (e.g., `refactor/repository-layer`)
- `docs/` - Documentation (e.g., `docs/update-readme`)
- `test/` - Test additions (e.g., `test/add-viewmodel-tests`)
- `chore/` - Maintenance tasks (e.g., `chore/update-dependencies`)

### Making Changes

1. **Write Code**
   - Follow the coding standards (see below)
   - Add/update tests
   - Update documentation

2. **Test Locally**
   ```bash
   # Run all tests
   ./gradlew test

   # Run Detekt static analysis
   ./gradlew detekt

   # Build the app
   ./gradlew assembleDebug
   ```

3. **Commit Changes**
   ```bash
   git add .
   git commit -m "feat: add your feature description"
   ```

4. **Push to Your Fork**
   ```bash
   git push origin feature/your-feature-name
   ```

5. **Create Pull Request**
   - Go to GitHub and create a PR from your fork
   - Fill out the PR template completely
   - Link any related issues

## Coding Standards

### Kotlin Style Guide
We follow the [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html) with some project-specific additions:

#### Naming
- **Classes**: PascalCase (e.g., `QuoteViewModel`)
- **Functions**: camelCase (e.g., `getQuoteOfTheDay()`)
- **Constants**: UPPER_SNAKE_CASE (e.g., `MAX_QUOTE_LENGTH`)
- **Composables**: PascalCase (e.g., `QuoteScreen()`)

#### File Organization
```kotlin
// 1. Package declaration
package com.po4yka.runicquotes.ui.screens.quote

// 2. Imports (organized and grouped)
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import javax.inject.Inject

// 3. KDoc comment
/**
 * ViewModel for the Quote screen.
 * Manages UI state and coordinates between repository and preferences.
 */
// 4. Class declaration
@HiltViewModel
class QuoteViewModel @Inject constructor(
    private val repository: QuoteRepository
) : ViewModel() {
    // Implementation
}
```

#### Code Formatting
- **Indentation**: 4 spaces (no tabs)
- **Line length**: 120 characters max
- **No trailing whitespace**
- **Empty line at end of file**

Run `./gradlew detekt` to check for style violations.

### Architecture Guidelines

#### Clean Architecture Layers
```
ui/         → Presentation (Composables, ViewModels)
domain/     → Business Logic (Models, Use Cases)
data/       → Data Sources (Repository, Database, Network)
di/         → Dependency Injection
```

#### MVVM Pattern
- **View** (Composable): Observes ViewModel state, handles user input
- **ViewModel**: Manages UI state, business logic delegation
- **Model** (Repository): Data operations, single source of truth

#### Dependency Injection
- Use Hilt for all dependency injection
- Constructor injection preferred over field injection
- Keep modules focused and cohesive

### Compose Best Practices

1. **State Hoisting**
   ```kotlin
   // ✅ Good - State hoisted
   @Composable
   fun QuoteScreen(viewModel: QuoteViewModel = hiltViewModel()) {
       val state by viewModel.state.collectAsState()
       QuoteContent(state = state, onAction = viewModel::handleAction)
   }

   // ❌ Bad - State managed in composable
   @Composable
   fun QuoteScreen() {
       var quote by remember { mutableStateOf("") }
       // Don't do this
   }
   ```

2. **Stable Parameters**
   - Pass stable types to composables
   - Use `@Stable` or `@Immutable` when appropriate

3. **Avoid Side Effects in Composition**
   - Use `LaunchedEffect` for coroutines
   - Use `DisposableEffect` for cleanup

## Testing Guidelines

### Test Coverage Requirements
- **Business Logic**: 90%+ coverage (transliterators, repositories)
- **ViewModels**: 80%+ coverage
- **UI**: Basic smoke tests

### Writing Tests

#### Unit Tests
```kotlin
class ElderFutharkTransliteratorTest {
    private lateinit var transliterator: ElderFutharkTransliterator

    @Before
    fun setup() {
        transliterator = ElderFutharkTransliterator()
    }

    @Test
    fun `transliterate f to FEHU`() {
        val result = transliterator.transliterate("f")
        assertEquals("\u16A0", result)
    }
}
```

#### ViewModel Tests with Turbine
```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class QuoteViewModelTest {
    @Test
    fun `loadQuote updates state`() = runTest {
        viewModel.uiState.test {
            val loading = awaitItem()
            assertTrue(loading is QuoteUiState.Loading)

            val success = awaitItem()
            assertTrue(success is QuoteUiState.Success)
        }
    }
}
```

### Running Tests
```bash
# All tests
./gradlew test

# Specific test class
./gradlew test --tests "ElderFutharkTransliteratorTest"

# With coverage
./gradlew testDebugUnitTest jacocoTestReport
```

## Commit Message Guidelines

We follow the [Conventional Commits](https://www.conventionalcommits.org/) specification:

### Format
```
<type>(<scope>): <subject>

<body>

<footer>
```

### Types
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation only changes
- `style`: Code style changes (formatting, missing semicolons, etc.)
- `refactor`: Code refactoring
- `test`: Adding or updating tests
- `chore`: Maintenance tasks (dependencies, build configuration)
- `perf`: Performance improvements
- `ci`: CI/CD changes

### Examples
```bash
# Feature
git commit -m "feat(quotes): add user-created quotes functionality"

# Bug fix
git commit -m "fix(widget): resolve crash on Android 13"

# Documentation
git commit -m "docs(readme): update installation instructions"

# With body
git commit -m "refactor(repository): migrate to Flow-based API

Changed getAllQuotes() to return Flow<List<Quote>> instead of
suspend function for better reactive updates.

Closes #123"
```

## Pull Request Process

### Before Submitting
1. ✅ All tests pass (`./gradlew test`)
2. ✅ No Detekt violations (`./gradlew detekt`)
3. ✅ Code is properly formatted
4. ✅ Documentation is updated
5. ✅ Commits follow conventional commits format
6. ✅ Branch is up to date with `main`

### PR Checklist
- [ ] Fill out PR template completely
- [ ] Link related issues
- [ ] Add screenshots for UI changes
- [ ] Update CHANGELOG.md (if applicable)
- [ ] Request review from maintainers
- [ ] Respond to review comments
- [ ] Ensure CI checks pass

### Review Process
1. **Automated Checks**: CI runs tests, Detekt, and builds
2. **Code Review**: Maintainer reviews code quality, architecture, tests
3. **Feedback**: Address review comments and update PR
4. **Approval**: Maintainer approves PR
5. **Merge**: Maintainer merges PR to main branch

### After Merge
- Delete your feature branch
- Update your local main branch
- Celebrate! 🎉

## Issue Reporting

### Before Creating an Issue
1. Search existing issues to avoid duplicates
2. Check if the issue exists in the latest version
3. Gather relevant information (logs, screenshots, device info)

### Creating an Issue
Use the appropriate issue template:
- **Bug Report**: For bugs and unexpected behavior
- **Feature Request**: For new features or enhancements

Provide as much detail as possible to help us address the issue quickly.

## Questions?

- **Discussions**: Use [GitHub Discussions](https://github.com/po4yka/runatal-android/discussions) for questions
- **Issues**: Only use issues for bugs and feature requests
- **Email**: Contact maintainers for private concerns

## Recognition

Contributors will be recognized in:
- CONTRIBUTORS.md file
- Release notes
- Project documentation

Thank you for contributing to Runic Quotes! 🙏

---

**Happy Coding! ᚱᚢᚾᛁᚲ ᚴᚢᛟᛏᛖᛋ**
