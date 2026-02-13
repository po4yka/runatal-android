package com.po4yka.runicquotes.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.po4yka.runicquotes.domain.model.RunicScript
import com.po4yka.runicquotes.ui.screens.addeditquote.AddEditQuoteScreen
import com.po4yka.runicquotes.ui.screens.onboarding.OnboardingScreen
import com.po4yka.runicquotes.ui.screens.quote.QuoteScreen
import com.po4yka.runicquotes.ui.screens.quotelist.QuoteListScreen
import com.po4yka.runicquotes.ui.screens.settings.SettingsScreen

/**
 * Main navigation graph using Navigation 3.
 * Uses NavDisplay with entryProvider DSL pattern for type-safe navigation.
 */
@Composable
fun NavGraph(
    backStack: SnapshotStateList<Any>,
    hasCompletedOnboarding: Boolean,
    selectedScript: RunicScript,
    selectedThemePack: String,
    onSelectOnboardingStyle: (RunicScript, String) -> Unit,
    onCompleteOnboarding: () -> Unit
) {
    val currentRoute = backStack.lastOrNull() ?: QuoteRoute
    val showBottomBar =
        currentRoute is QuoteRoute || currentRoute is QuoteListRoute || currentRoute is SettingsRoute

    LaunchedEffect(hasCompletedOnboarding, currentRoute) {
        if (!hasCompletedOnboarding && currentRoute !is OnboardingRoute) {
            backStack.clear()
            backStack.add(OnboardingRoute)
        } else if (hasCompletedOnboarding && currentRoute is OnboardingRoute) {
            backStack.clear()
            backStack.add(QuoteRoute)
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                TopLevelBottomBar(currentRoute = currentRoute, backStack = backStack)
            }
        }
    ) { paddingValues ->
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            modifier = Modifier
                .padding(paddingValues)
                .testTag("nav_content"),
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator()
            ),
            entryProvider = entryProvider {
                entry<OnboardingRoute> {
                    OnboardingScreen(
                        selectedScript = selectedScript,
                        selectedThemePack = selectedThemePack,
                        onChooseStyle = onSelectOnboardingStyle,
                        onComplete = {
                            onCompleteOnboarding()
                            switchTopLevelRoute(backStack, QuoteRoute)
                        }
                    )
                }
                entry<QuoteRoute> {
                    QuoteScreen()
                }
                entry<SettingsRoute> {
                    SettingsScreen()
                }
                entry<QuoteListRoute> {
                    QuoteListScreen(
                        onNavigateToAddQuote = {
                            backStack.add(AddEditQuoteRoute())
                        },
                        onNavigateToEditQuote = { quoteId ->
                            backStack.add(AddEditQuoteRoute(quoteId = quoteId))
                        }
                    )
                }
                entry<AddEditQuoteRoute> { route ->
                    AddEditQuoteScreen(
                        onNavigateBack = {
                            backStack.removeLastOrNull()
                        },
                        quoteId = route.quoteId
                    )
                }
            }
        )
    }
}

@Composable
private fun TopLevelBottomBar(
    currentRoute: Any,
    backStack: SnapshotStateList<Any>
) {
    NavigationBar {
        NavigationBarItem(
            selected = currentRoute is QuoteRoute,
            onClick = { switchTopLevelRoute(backStack, QuoteRoute) },
            icon = { Text("ᚱ") },
            label = { Text("Daily") },
            modifier = Modifier.testTag("tab_daily")
        )
        NavigationBarItem(
            selected = currentRoute is QuoteListRoute,
            onClick = { switchTopLevelRoute(backStack, QuoteListRoute) },
            icon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.List,
                    contentDescription = null
                )
            },
            label = { Text("Library") },
            modifier = Modifier.testTag("tab_library")
        )
        NavigationBarItem(
            selected = currentRoute is SettingsRoute,
            onClick = { switchTopLevelRoute(backStack, SettingsRoute) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null
                )
            },
            label = { Text("Settings") },
            modifier = Modifier.testTag("tab_settings")
        )
    }
}

private fun switchTopLevelRoute(backStack: SnapshotStateList<Any>, route: Any) {
    if (backStack.lastOrNull() == route) {
        return
    }
    backStack.clear()
    backStack.add(route)
}
