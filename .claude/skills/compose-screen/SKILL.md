---
name: compose-screen
description: "Scaffold a new Compose screen with ViewModel, UI state, Navigation 3 route, and Hilt wiring in the Runic Quotes app. Use when: (1) adding a new screen, (2) creating a new ViewModel, (3) adding navigation routes, (4) wiring up a new feature screen. Triggers on: new screen, add screen, create screen, new feature, add page, navigation route."
---

# Compose Screen Scaffold

## Steps to Add a New Screen

1. Define route in `ui/navigation/Routes.kt`
2. Create screen package in `ui/screens/<name>/`
3. Create UI state sealed class/data class
4. Create ViewModel with `@HiltViewModel`
5. Create Screen composable
6. Register in `NavGraph.kt` entry provider
7. Add to `routeRankMap` in NavGraph.kt
8. Write ViewModel unit test

## File Locations

| What | Path |
|------|------|
| Routes | `ui/navigation/Routes.kt` |
| NavGraph | `ui/navigation/NavGraph.kt` |
| Screens | `ui/screens/<name>/` |
| Tests | `test/.../ui/screens/<name>/` |

All source paths relative to `app/src/main/java/com/po4yka/runicquotes/`.

## 1. Route (`Routes.kt`)

```kotlin
/** Brief description of the screen. */
@Serializable
data object FeatureRoute          // No params

@Serializable
data class FeatureRoute(val id: Long)  // With params
```

## 2. UI State

```kotlin
data class FeatureUiState(
    val items: List<Item> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
```

Use `sealed interface` only when states are mutually exclusive (Loading/Success/Error).
Use `data class` when states can overlap (isLoading + partial data).

## 3. ViewModel

```kotlin
@HiltViewModel
class FeatureViewModel @Inject constructor(
    private val repository: SomeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeatureUiState())
    val uiState: StateFlow<FeatureUiState> = _uiState.asStateFlow()

    init { loadData() }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.getDataFlow()
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
                }
                .collect { data ->
                    _uiState.update { it.copy(items = data, isLoading = false) }
                }
        }
    }
}
```

Rules:
- Use `viewModelScope.launch`, never `GlobalScope`
- Use `_uiState.update { }` for thread-safe mutations
- Catch `IOException` and `IllegalStateException` explicitly
- Add `companion object { private const val TAG = "FeatureViewModel" }` for logging

## 4. Screen Composable

```kotlin
@Composable
fun FeatureScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: FeatureViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // Compose UI here
}
```

Rules:
- State hoisting: ViewModel owns state, Screen observes
- Use `collectAsStateWithLifecycle()` for lifecycle-aware collection
- Navigation callbacks as lambda parameters
- No `remember { mutableStateOf() }` for business logic

## 5. NavGraph Registration

In `NavGraph.kt`, add entry to the appropriate section:

```kotlin
// Top-level (shown in bottom bar):
entry<FeatureRoute> {
    FeatureScreen(
        onNavigateToDetail = { id -> backStack.add(DetailRoute(id)) }
    )
}

// Detail (navigated to from another screen):
entry<FeatureRoute> { route ->
    FeatureScreen(
        onNavigateBack = { backStack.removeLastOrNull() },
        featureId = route.id
    )
}
```

Add to `routeRankMap` with next available rank number.
If top-level: add to `showBottomBar` condition and `TopLevelBottomBar`.

## 6. ViewModel Test

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class FeatureViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: SomeRepository
    private lateinit var viewModel: FeatureViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `initial state is loading then success`() = runTest {
        viewModel = FeatureViewModel(repository)
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.isLoading).isFalse()
        }
    }
}
```

Test tools: JUnit 4 + MockK + Turbine + Truth + `runTest`.
