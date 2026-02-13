package com.po4yka.runicquotes.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.navigation3.scene.Scene
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.po4yka.runicquotes.domain.model.RunicScript
import com.po4yka.runicquotes.ui.screens.addeditquote.AddEditQuoteScreen
import com.po4yka.runicquotes.ui.screens.onboarding.OnboardingScreen
import com.po4yka.runicquotes.ui.screens.quote.QuoteScreen
import com.po4yka.runicquotes.ui.screens.quotelist.QuoteListScreen
import com.po4yka.runicquotes.ui.screens.settings.SettingsScreen
import com.po4yka.runicquotes.ui.theme.LocalReduceMotion
import com.po4yka.runicquotes.ui.theme.RunicExpressiveTheme
import androidx.navigationevent.NavigationEvent

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
    val reducedMotion = LocalReduceMotion.current
    val motion = RunicExpressiveTheme.motion

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
            AnimatedVisibility(
                visible = showBottomBar,
                enter = if (reducedMotion) {
                    EnterTransition.None
                } else {
                    fadeIn(
                        animationSpec = tween(
                            durationMillis = motion.duration(
                                reducedMotion = reducedMotion,
                                base = motion.shortDurationMillis
                            )
                        )
                    ) + slideInVertically(
                        animationSpec = tween(
                            durationMillis = motion.duration(
                                reducedMotion = reducedMotion,
                                base = motion.shortDurationMillis
                            )
                        ),
                        initialOffsetY = { it / 2 }
                    )
                },
                exit = if (reducedMotion) {
                    ExitTransition.None
                } else {
                    fadeOut(
                        animationSpec = tween(
                            durationMillis = motion.duration(
                                reducedMotion = reducedMotion,
                                base = motion.shortDurationMillis
                            )
                        )
                    ) + slideOutVertically(
                        animationSpec = tween(
                            durationMillis = motion.duration(
                                reducedMotion = reducedMotion,
                                base = motion.shortDurationMillis
                            )
                        ),
                        targetOffsetY = { it / 2 }
                    )
                }
            ) {
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
            transitionSpec = {
                val forward = routeRank(targetState) >= routeRank(initialState)
                val direction = if (forward) {
                    slideIntoContainerLeft()
                } else {
                    slideIntoContainerRight()
                }
                val duration = motion.duration(
                    reducedMotion = reducedMotion,
                    base = motion.mediumDurationMillis
                )
                if (duration == 0) {
                    EnterTransition.None togetherWith ExitTransition.None
                } else {
                    direction.first(
                        duration,
                        motion.standardEasing
                    ) togetherWith direction.second(
                        duration,
                        motion.standardEasing
                    )
                }
            },
            popTransitionSpec = {
                val forward = routeRank(targetState) >= routeRank(initialState)
                val direction = if (forward) {
                    slideIntoContainerLeft()
                } else {
                    slideIntoContainerRight()
                }
                val duration = motion.duration(
                    reducedMotion = reducedMotion,
                    base = motion.mediumDurationMillis
                )
                if (duration == 0) {
                    EnterTransition.None togetherWith ExitTransition.None
                } else {
                    direction.first(
                        duration,
                        motion.standardEasing
                    ) togetherWith direction.second(
                        duration,
                        motion.standardEasing
                    )
                }
            },
            predictivePopTransitionSpec = { swipeEdge ->
                val duration = motion.duration(
                    reducedMotion = reducedMotion,
                    base = motion.shortDurationMillis
                )
                if (duration == 0) {
                    EnterTransition.None togetherWith ExitTransition.None
                } else {
                    val fromRight = swipeEdge == NavigationEvent.EDGE_RIGHT
                    val direction = if (fromRight) {
                        slideIntoContainerRight()
                    } else {
                        slideIntoContainerLeft()
                    }
                    direction.first(duration, motion.standardEasing) togetherWith
                        direction.second(duration, motion.standardEasing)
                }
            },
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
                    QuoteScreen(
                        onBrowseLibrary = {
                            switchTopLevelRoute(backStack, QuoteListRoute)
                        }
                    )
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

private fun routeRank(scene: Scene<Any>): Int {
    val key = scene.key.toString()
    return when {
        key.contains("OnboardingRoute") -> 0
        key.contains("QuoteRoute") -> 1
        key.contains("QuoteListRoute") -> 2
        key.contains("AddEditQuoteRoute") -> 3
        key.contains("SettingsRoute") -> 4
        else -> 2
    }
}

private fun androidx.compose.animation.AnimatedContentTransitionScope<Scene<Any>>.slideIntoContainerLeft():
    Pair<(Int, androidx.compose.animation.core.Easing) -> androidx.compose.animation.EnterTransition, (Int, androidx.compose.animation.core.Easing) -> androidx.compose.animation.ExitTransition> {
    return Pair(
        { duration, easing ->
            slideIntoContainer(
                towards = androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(durationMillis = duration, easing = easing)
            ) + fadeIn(animationSpec = tween(durationMillis = duration))
        },
        { duration, easing ->
            slideOutOfContainer(
                towards = androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(durationMillis = duration, easing = easing)
            ) + fadeOut(animationSpec = tween(durationMillis = duration))
        }
    )
}

private fun androidx.compose.animation.AnimatedContentTransitionScope<Scene<Any>>.slideIntoContainerRight():
    Pair<(Int, androidx.compose.animation.core.Easing) -> androidx.compose.animation.EnterTransition, (Int, androidx.compose.animation.core.Easing) -> androidx.compose.animation.ExitTransition> {
    return Pair(
        { duration, easing ->
            slideIntoContainer(
                towards = androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(durationMillis = duration, easing = easing)
            ) + fadeIn(animationSpec = tween(durationMillis = duration))
        },
        { duration, easing ->
            slideOutOfContainer(
                towards = androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(durationMillis = duration, easing = easing)
            ) + fadeOut(animationSpec = tween(durationMillis = duration))
        }
    )
}
