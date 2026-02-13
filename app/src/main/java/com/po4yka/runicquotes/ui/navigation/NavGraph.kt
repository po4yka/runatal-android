package com.po4yka.runicquotes.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.po4yka.runicquotes.ui.screens.addeditquote.AddEditQuoteScreen
import com.po4yka.runicquotes.ui.screens.quote.QuoteScreen
import com.po4yka.runicquotes.ui.screens.quotelist.QuoteListScreen
import com.po4yka.runicquotes.ui.screens.settings.SettingsScreen

/**
 * Main navigation graph using Navigation 3.
 * Uses NavDisplay with entryProvider DSL pattern for type-safe navigation.
 */
@Composable
fun NavGraph(
    backStack: SnapshotStateList<Any>
) {
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        entryProvider = entryProvider {
            // Quote Screen
            entry<QuoteRoute> {
                QuoteScreen(
                    onNavigateToSettings = {
                        backStack.add(SettingsRoute)
                    },
                    onNavigateToQuoteList = {
                        backStack.add(QuoteListRoute)
                    }
                )
            }

            // Settings Screen
            entry<SettingsRoute> {
                SettingsScreen(
                    onNavigateBack = {
                        backStack.removeLastOrNull()
                    }
                )
            }

            // Quote List Screen
            entry<QuoteListRoute> {
                QuoteListScreen(
                    onNavigateBack = {
                        backStack.removeLastOrNull()
                    },
                    onNavigateToAddQuote = {
                        backStack.add(AddEditQuoteRoute())
                    },
                    onNavigateToEditQuote = { quoteId ->
                        backStack.add(AddEditQuoteRoute(quoteId = quoteId))
                    }
                )
            }

            // Add/Edit Quote Screen
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
