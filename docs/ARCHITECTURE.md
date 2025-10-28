# Architecture Documentation

## Table of Contents
- [Overview](#overview)
- [Architecture Principles](#architecture-principles)
- [Layer Breakdown](#layer-breakdown)
- [Data Flow](#data-flow)
- [Key Components](#key-components)
- [Design Patterns](#design-patterns)
- [Dependency Graph](#dependency-graph)

## Overview

Runic Quotes follows **Clean Architecture** principles combined with **MVVM** (Model-View-ViewModel) pattern. The app is structured in distinct layers with clear separation of concerns, making it maintainable, testable, and scalable.

### Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                     Presentation Layer                       │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐            │
│  │  Compose   │  │ ViewModel  │  │   Widget   │            │
│  │   UI       │◄─┤  (State)   │  │  (Glance)  │            │
│  └────────────┘  └────────────┘  └────────────┘            │
│         ▲              ▲                ▲                    │
└─────────┼──────────────┼────────────────┼────────────────────┘
          │              │                │
┌─────────┼──────────────┼────────────────┼────────────────────┐
│         │        Domain Layer           │                    │
│  ┌──────┴──────┐  ┌──────────┐  ┌──────┴──────┐            │
│  │   Models    │  │Use Cases │  │Transliterator│            │
│  │(Quote, etc) │  │(Optional)│  │  (Business)  │            │
│  └─────────────┘  └──────────┘  └──────────────┘            │
│         ▲                              ▲                     │
└─────────┼──────────────────────────────┼─────────────────────┘
          │                              │
┌─────────┼──────────────────────────────┼─────────────────────┐
│         │          Data Layer          │                     │
│  ┌──────┴──────┐  ┌──────────┐  ┌─────┴──────┐             │
│  │ Repository  │  │   Room    │  │ DataStore  │             │
│  │    (Impl)   │◄─┤  Database │  │(Preferences)│             │
│  └─────────────┘  └──────────┘  └────────────┘             │
└─────────────────────────────────────────────────────────────┘
          ▲
┌─────────┼───────────────────────────────────────────────────┐
│         │     Dependency Injection (Hilt)                   │
│  ┌──────┴──────┐  ┌────────────┐  ┌──────────────┐         │
│  │   Module    │  │   Module   │  │    Module    │         │
│  │ (Database)  │  │  (Data)    │  │  (Network)   │         │
│  └─────────────┘  └────────────┘  └──────────────┘         │
└─────────────────────────────────────────────────────────────┘
```

## Architecture Principles

### 1. Separation of Concerns
Each layer has a single, well-defined responsibility:
- **UI Layer**: Displays data and handles user interaction
- **Domain Layer**: Contains business logic and rules
- **Data Layer**: Manages data sources and persistence

### 2. Dependency Rule
Dependencies point inward - outer layers depend on inner layers, never the reverse:
```
UI → Domain ← Data
```

### 3. Unidirectional Data Flow (UDF)
```
User Action → ViewModel → Repository → Database
                 ↓
              UI State
                 ↓
              Compose
```

### 4. Single Source of Truth
- **Database (Room)** is the single source of truth
- All data flows from the database through the repository
- UI never directly accesses data sources

## Layer Breakdown

### Presentation Layer (`ui/`)

**Responsibility**: Display data and handle user interactions

#### Components:
```kotlin
ui/
├── screens/           # Screen-level composables and ViewModels
│   ├── quote/
│   │   ├── QuoteScreen.kt
│   │   ├── QuoteViewModel.kt
│   │   └── QuoteUiState.kt
│   ├── settings/
│   ├── addeditquote/
│   └── quotelist/
├── components/        # Reusable UI components
│   ├── RunicText.kt
│   └── ...
├── theme/             # Material 3 theming
│   ├── Color.kt
│   ├── Theme.kt
│   └── Type.kt
├── navigation/        # Navigation setup
│   └── NavGraph.kt
└── widget/            # Home screen widget
    ├── RunicQuoteWidget.kt
    └── RunicQuoteWidgetReceiver.kt
```

#### ViewModel Pattern:
```kotlin
@HiltViewModel
class QuoteViewModel @Inject constructor(
    private val repository: QuoteRepository,
    private val preferences: UserPreferencesManager
) : ViewModel() {

    // UI state as StateFlow
    private val _uiState = MutableStateFlow<QuoteUiState>(QuoteUiState.Loading)
    val uiState: StateFlow<QuoteUiState> = _uiState.asStateFlow()

    // Business logic delegation
    init {
        viewModelScope.launch {
            loadQuoteOfTheDay()
        }
    }

    private suspend fun loadQuoteOfTheDay() {
        val quote = repository.quoteOfTheDay(selectedScript)
        _uiState.value = QuoteUiState.Success(quote)
    }
}
```

#### Compose Screen:
```kotlin
@Composable
fun QuoteScreen(
    viewModel: QuoteViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    when (val state = uiState) {
        is QuoteUiState.Loading -> LoadingIndicator()
        is QuoteUiState.Success -> QuoteContent(state.quote)
        is QuoteUiState.Error -> ErrorMessage(state.message)
    }
}
```

### Domain Layer (`domain/`)

**Responsibility**: Business logic and domain models

```kotlin
domain/
├── model/             # Domain models
│   ├── Quote.kt
│   ├── RunicScript.kt
│   └── UserPreferences.kt
└── transliterator/    # Business logic
    ├── ElderFutharkTransliterator.kt
    ├── YoungerFutharkTransliterator.kt
    └── CirthTransliterator.kt
```

#### Domain Models:
```kotlin
data class Quote(
    val id: Long,
    val textLatin: String,
    val author: String,
    val runicElder: String?,
    val runicYounger: String?,
    val runicCirth: String?,
    val isUserCreated: Boolean,
    val isFavorite: Boolean,
    val createdAt: Long
)
```

#### Business Logic:
```kotlin
class ElderFutharkTransliterator @Inject constructor() {
    fun transliterate(text: String): String {
        // Complex transliteration logic
        return text.map { char -> mapToRune(char) }.joinToString("")
    }

    private fun mapToRune(char: Char): String {
        // Mapping logic
    }
}
```

### Data Layer (`data/`)

**Responsibility**: Data management and persistence

```kotlin
data/
├── local/             # Local data sources
│   ├── RunicQuotesDatabase.kt
│   ├── dao/
│   │   └── QuoteDao.kt
│   └── entity/
│       └── QuoteEntity.kt
├── preferences/       # DataStore preferences
│   └── UserPreferencesManager.kt
└── repository/        # Repository implementations
    ├── QuoteRepository.kt (interface)
    └── QuoteRepositoryImpl.kt
```

#### Repository Pattern:
```kotlin
interface QuoteRepository {
    suspend fun quoteOfTheDay(script: RunicScript): Quote?
    fun getAllQuotesFlow(): Flow<List<Quote>>
    suspend fun saveUserQuote(quote: Quote): Long
}

@Singleton
class QuoteRepositoryImpl @Inject constructor(
    private val quoteDao: QuoteDao
) : QuoteRepository {

    override suspend fun quoteOfTheDay(script: RunicScript): Quote? {
        val dayOfYear = LocalDate.now().dayOfYear
        val allQuotes = quoteDao.getAll()
        val index = dayOfYear % allQuotes.size
        return allQuotes[index].toDomain()
    }

    // Maps entity to domain model
    private fun QuoteEntity.toDomain() = Quote(...)
}
```

#### Room Database:
```kotlin
@Database(
    entities = [QuoteEntity::class],
    version = 2,
    exportSchema = true
)
abstract class RunicQuotesDatabase : RoomDatabase() {
    abstract fun quoteDao(): QuoteDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Migration logic
            }
        }
    }
}
```

## Data Flow

### Reading Data
```
User Opens App
     ↓
ViewModel.init()
     ↓
repository.quoteOfTheDay()
     ↓
quoteDao.getAll()
     ↓
Room Database
     ↓
QuoteEntity → Quote (mapping)
     ↓
StateFlow<QuoteUiState>
     ↓
Compose observes StateFlow
     ↓
UI Updates
```

### Writing Data
```
User Adds Quote
     ↓
ViewModel.saveQuote()
     ↓
repository.saveUserQuote(quote)
     ↓
Quote → QuoteEntity (mapping)
     ↓
quoteDao.insert(entity)
     ↓
Room Database
     ↓
Flow emits update
     ↓
Observers react
     ↓
UI Updates
```

## Key Components

### 1. Dependency Injection (Hilt)

```kotlin
// Database Module
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): RunicQuotesDatabase {
        return Room.databaseBuilder(context, RunicQuotesDatabase::class.java, "runic_quotes.db")
            .addMigrations(RunicQuotesDatabase.MIGRATION_1_2)
            .build()
    }

    @Provides
    fun provideQuoteDao(db: RunicQuotesDatabase): QuoteDao = db.quoteDao()
}

// Repository Module
@Module
@InstallIn(SingletonComponent::class)
interface DataModule {

    @Binds
    fun bindQuoteRepository(impl: QuoteRepositoryImpl): QuoteRepository
}
```

### 2. State Management

**StateFlow** for reactive state:
```kotlin
// ViewModel
private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
val uiState: StateFlow<UiState> = _uiState.asStateFlow()

// Compose
val state by viewModel.uiState.collectAsState()
```

**DataStore** for preferences:
```kotlin
@Singleton
class UserPreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    val userPreferencesFlow: Flow<UserPreferences> = dataStore.data.map { prefs ->
        UserPreferences(
            selectedScript = RunicScript.valueOf(
                prefs[SELECTED_SCRIPT] ?: RunicScript.ELDER_FUTHARK.name
            )
        )
    }
}
```

### 3. Background Work

**WorkManager** for periodic updates:
```kotlin
@HiltWorker
class QuoteUpdateWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: QuoteRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // Update widget with new quote
        return Result.success()
    }
}
```

## Design Patterns

### 1. Repository Pattern
- Abstracts data sources
- Provides clean API for data access
- Handles data mapping (Entity ↔ Domain)

### 2. Observer Pattern
- StateFlow for reactive state
- Flow for data streams
- Compose recomposition on state changes

### 3. Dependency Injection
- Hilt for all DI
- Constructor injection preferred
- Scoped lifecycles (@Singleton, @ViewModelScoped)

### 4. Factory Pattern
- Used for ViewModel creation via Hilt
- Transliterator instantiation

### 5. Strategy Pattern
- Different transliterators for different scripts
- Swappable algorithms

## Dependency Graph

```
┌─────────────────┐
│  Application    │
└────────┬────────┘
         │ @HiltAndroidApp
┌────────▼────────────────────────────┐
│         Hilt Component              │
│  ┌──────────────────────────────┐  │
│  │    SingletonComponent        │  │
│  │  - Database                  │  │
│  │  - Repository                │  │
│  │  - Preferences               │  │
│  │  - Transliterators           │  │
│  └──────────────┬───────────────┘  │
│                 │                   │
│  ┌──────────────▼───────────────┐  │
│  │  ViewModelComponent          │  │
│  │  - QuoteViewModel            │  │
│  │  - SettingsViewModel         │  │
│  │  - AddEditQuoteViewModel     │  │
│  └──────────────┬───────────────┘  │
│                 │                   │
│  ┌──────────────▼───────────────┐  │
│  │  ActivityComponent           │  │
│  │  - MainActivity              │  │
│  └──────────────────────────────┘  │
└─────────────────────────────────────┘
```

## Testing Strategy

### Unit Tests
- **Transliterators**: Pure business logic, easy to test
- **Repository**: Mocked DAO with MockK
- **ViewModels**: Turbine for Flow testing

### Integration Tests
- **Database**: Room in-memory database
- **End-to-end flows**: Full data pipeline

### UI Tests
- **Compose tests**: Semantic tree assertions
- **Screenshot tests**: Visual regression testing

## Performance Considerations

### 1. Database Optimization
- Indexed queries for fast lookups
- Flow-based queries for reactivity
- Proper migration strategy

### 2. Compose Performance
- Stable/Immutable data classes
- State hoisting
- LazyColumn for large lists
- Key stability for recomposition

### 3. Memory Management
- ViewModel scoping
- Proper coroutine cancellation
- Cache invalidation

## Future Enhancements

### Planned Improvements
- [ ] Multi-module architecture (app, domain, data modules)
- [ ] Use Cases layer for complex business logic
- [ ] Offline-first architecture with sync
- [ ] State machine for complex UI states
- [ ] Result wrapper for error handling

### Scalability
The current architecture supports:
- Adding new runic scripts easily
- Extending quote sources (network, cloud)
- New features without major refactoring
- Multiple entry points (widget, shortcuts, etc.)

---

**Document Version**: 1.0
**Last Updated**: Phase 5 Completion
**Author**: Runic Quotes Team
