# Runatal

[![Android CI](https://github.com/po4yka/runatal-android/workflows/Android%20CI/badge.svg)](https://github.com/po4yka/runatal-android/actions)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.21-blue.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-1.7-green.svg)](https://developer.android.com/jetpack/compose)

A beautiful Android app that displays inspiring quotes transliterated into ancient runic scripts. Built with Jetpack Compose and Material 3 Design, featuring Elder Futhark, Younger Futhark, and Tolkien's Cirth (Angerthas) scripts.

## ✨ Features

### Core Functionality
- **Daily Runatal**: Get a new inspirational quote every day, transliterated into runes
- **Multiple Runic Scripts**: Support for three ancient writing systems:
  - **Elder Futhark**: The oldest runic alphabet (2nd-8th centuries)
  - **Younger Futhark**: Simplified Viking Age runes (9th-11th centuries)
  - **Cirth (Angerthas)**: Tolkien's elvish runes from Middle-earth
- **Live Transliteration**: Automatic conversion from Latin alphabet to runic scripts
- **Home Screen Widget**: Display quote of the day directly on your home screen

### Advanced Features
- **User-Created Quotes**: Add your own quotes with live runic preview
- **Favorites System**: Mark and filter your favorite quotes
- **Quote Management**: Browse, edit, and delete your custom quotes
- **Quote Sharing**: Export quotes as beautiful images or share as text
- **Expressive Animations**: Sequential character-by-character rune reveal effect
- **Material 3 Design**: Modern UI with dynamic theming support

### Technical Highlights
- **Offline-First**: All quotes stored locally with Room database
- **Clean Architecture**: MVVM pattern with proper separation of concerns
- **Reactive UI**: StateFlow-based reactive updates throughout the app
- **Type Safety**: 100% Kotlin with comprehensive unit tests
- **CI/CD Ready**: Automated testing and builds with GitHub Actions

## 📱 Screenshots

> Screenshots coming soon! The app features:
> - Main quote screen with animated runic text
> - Settings screen for script and font selection
> - Quote list browser with filtering
> - Add/Edit quote screen with live preview
> - Home screen widget

## 🏗️ Tech Stack

### Core Technologies
- **Language**: Kotlin 2.2.21
- **UI Framework**: Jetpack Compose 1.7
- **Design System**: Material 3 Expressive
- **Architecture**: MVVM + Clean Architecture
- **Dependency Injection**: Hilt
- **Build System**: Gradle 8.1.3 with Kotlin DSL

### Jetpack Components
- **Room**: Local database with migrations
- **DataStore**: Preferences management
- **WorkManager**: Background quote updates
- **Glance**: Home screen widget framework
- **Navigation**: Type-safe navigation

### Testing & Quality
- **Unit Testing**: JUnit 4, MockK, Turbine
- **Coroutines Testing**: kotlinx-coroutines-test
- **Static Analysis**: Detekt with custom rules
- **CI/CD**: GitHub Actions

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17 or higher
- Android SDK with API level 26+ (Android 8.0 Oreo)
- Git

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/po4yka/runatal-android.git
   cd runatal-android
   ```

2. **Open in Android Studio**
   - Launch Android Studio
   - Select "Open an Existing Project"
   - Navigate to the cloned repository
   - Wait for Gradle sync to complete

3. **Run the app**
   - Connect an Android device or start an emulator
   - Click the "Run" button (▶️) or press Shift+F10
   - Select your target device

### Building from Command Line

```bash
# Debug build
./gradlew assembleDebug

# Release build (requires signing configuration)
./gradlew assembleRelease

# Run unit tests
./gradlew testDebugUnitTest

# Run static analysis
./gradlew detekt

# Run all checks
./gradlew check
```

The APK will be generated at:
- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release.apk`

## 🧪 Testing

The project maintains high test coverage with comprehensive test suites:

```bash
# Run all unit tests
./gradlew test

# Run project coverage report
./gradlew testDebugUnitTest jacocoProjectCoverageReport

# Enforce the transliteration coverage gate
./gradlew jacocoTransliterationCoverageVerification

# Run specific test class
./gradlew test --tests "ElderFutharkTransliteratorTest"

# Run instrumented tests and merge their coverage (requires emulator/device)
./gradlew testDebugUnitTest connectedDebugAndroidTest jacocoProjectCoverageReport
```

**Test Coverage:**
- Transliterators: 260+ tests covering all three runic scripts
- Repository Layer: 30+ tests with MockK
- ViewModels: 65+ tests using Turbine for Flow testing
- Transliteration domain: 90%+ line coverage enforced in CI
- Project-wide report: merges unit and Android test coverage when both exist

## 📚 Architecture

The app follows Clean Architecture principles with clear separation of concerns:

```
app/
├── data/              # Data layer
│   ├── local/         # Room database, DAOs, entities
│   ├── preferences/   # DataStore preferences
│   └── repository/    # Repository implementations
├── domain/            # Domain layer
│   ├── model/         # Domain models (Quote, RunicScript)
│   └── transliterator/# Business logic for rune conversion
├── di/                # Dependency injection modules
└── ui/                # Presentation layer
    ├── components/    # Reusable UI components
    ├── screens/       # Screen composables & ViewModels
    ├── theme/         # Material 3 theming
    └── widget/        # Home screen widget
```

**Key Design Patterns:**
- **MVVM**: ViewModels manage UI state, Views observe StateFlows
- **Repository Pattern**: Abstracts data sources from business logic
- **Dependency Injection**: Hilt provides dependencies across layers
- **Single Source of Truth**: Room database as the primary data source

For detailed architecture documentation, see [ARCHITECTURE.md](docs/ARCHITECTURE.md).

## 🤝 Contributing

We welcome contributions! Please see our [Contributing Guide](CONTRIBUTING.md) for details on:
- Code of Conduct
- Development workflow
- Coding standards
- Pull request process
- Issue reporting

Quick start for contributors:
```bash
# Fork the repo and clone your fork
git clone https://github.com/YOUR_USERNAME/runatal-android.git

# Create a feature branch
git checkout -b feature/amazing-feature

# Make your changes and commit
git commit -m "Add amazing feature"

# Push and create a PR
git push origin feature/amazing-feature
```

## 📋 Roadmap

### ✅ Completed (v1.0)
- [x] Project setup with modern Android stack
- [x] Three runic script transliterators
- [x] Room database with 100+ quotes
- [x] Jetpack Compose UI with Material 3
- [x] Home screen widget with Glance
- [x] User-created quotes and favorites
- [x] Quote sharing as images
- [x] Comprehensive test suite (260+ tests)
- [x] CI/CD with GitHub Actions

### 🚧 In Progress
- [ ] Comprehensive documentation
- [ ] Play Store assets and submission
- [ ] Performance optimization

### 🔮 Future Enhancements (v1.1+)
- [ ] Additional runic scripts (Anglo-Saxon Futhorc)
- [ ] Theme variants (stone, wood, parchment)
- [ ] Advanced widget configurations
- [ ] Cloud backup and sync
- [ ] Community quote sharing
- [ ] Multi-language support

See the full roadmap in [runic_quotes_android_readme.md](runic_quotes_android_readme.md).

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

### Font Licenses
- **Noto Sans**: Apache License 2.0
- **BabelStone Runic Fonts**: Free for personal and commercial use
- Cirth (Angerthas) fonts: See individual font licenses in `fonts_sources/`

## 🙏 Acknowledgments

- **J.R.R. Tolkien** for creating the Cirth script
- **Norse mythology** for inspiring the Elder and Younger Futhark
- **Open source community** for the amazing tools and libraries
- **Quote contributors** for the inspirational content

## 📞 Contact & Support

- **Issues**: [GitHub Issues](https://github.com/po4yka/runatal-android/issues)
- **Discussions**: [GitHub Discussions](https://github.com/po4yka/runatal-android/discussions)
- **Author**: [@po4yka](https://github.com/po4yka)

---

**Built with ❤️ and ᚱᚢᚾᛖᛋ**

*"Not all those who wander are lost."* — J.R.R. Tolkien
