package com.po4yka.runicquotes.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.scene.Scene
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.po4yka.runicquotes.domain.model.RunicScript
import com.po4yka.runicquotes.ui.screens.about.AboutScreen
import com.po4yka.runicquotes.ui.screens.addeditquote.AddEditQuoteScreen
import com.po4yka.runicquotes.ui.screens.archive.ArchiveScreen
import com.po4yka.runicquotes.ui.screens.create.CreateScreen
import com.po4yka.runicquotes.ui.screens.notificationsettings.NotificationSettingsScreen
import com.po4yka.runicquotes.ui.screens.onboarding.OnboardingScreen
import com.po4yka.runicquotes.ui.screens.packs.PackDetailScreen
import com.po4yka.runicquotes.ui.screens.packs.PacksScreen
import com.po4yka.runicquotes.ui.screens.profile.ProfileScreen
import com.po4yka.runicquotes.ui.screens.quote.QuoteScreen
import com.po4yka.runicquotes.ui.screens.quotelist.QuoteListScreen
import com.po4yka.runicquotes.ui.screens.references.ReferencesScreen
import com.po4yka.runicquotes.ui.screens.references.RuneDetailScreen
import com.po4yka.runicquotes.ui.screens.settings.SettingsScreen
import com.po4yka.runicquotes.ui.screens.share.ShareScreen
import com.po4yka.runicquotes.ui.screens.translation.TranslationScreen
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
    onSelectOnboardingStyle: (RunicScript, String) -> Unit,
    onCompleteOnboarding: () -> Unit
) {
    val currentRoute = backStack.lastOrNull() ?: QuoteRoute
    val showBottomBar = currentRoute is QuoteRoute || currentRoute is QuoteListRoute ||
        currentRoute is CreateRoute || currentRoute is PacksRoute || currentRoute is SettingsRoute
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
                val duration = motion.duration(
                    reducedMotion = reducedMotion,
                    base = motion.shortDurationMillis
                )
                if (duration == 0) {
                    EnterTransition.None togetherWith ExitTransition.None
                } else {
                    resolveSceneTransition(
                        initialState = initialState,
                        targetState = targetState,
                        duration = duration,
                        easing = motion.standardEasing
                    )
                }
            },
            popTransitionSpec = {
                val duration = motion.duration(
                    reducedMotion = reducedMotion,
                    base = motion.shortDurationMillis
                )
                if (duration == 0) {
                    EnterTransition.None togetherWith ExitTransition.None
                } else {
                    resolveSceneTransition(
                        initialState = initialState,
                        targetState = targetState,
                        duration = duration,
                        easing = motion.standardEasing
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
                        onChooseStyle = onSelectOnboardingStyle,
                        onComplete = {
                            onCompleteOnboarding()
                            switchTopLevelRoute(backStack, QuoteRoute)
                        }
                    )
                }
                entry<QuoteRoute> {
                    QuoteScreen(
                        onNavigateToEditQuote = { quoteId ->
                            backStack.add(AddEditQuoteRoute(quoteId = quoteId))
                        },
                        onNavigateToNotifications = {
                            backStack.add(NotificationSettingsRoute)
                        },
                        onBrowseLibrary = {
                            switchTopLevelRoute(backStack, QuoteListRoute)
                        }
                    )
                }
                entry<SettingsRoute> {
                    SettingsScreen(
                        onNavigateToNotifications = {
                            backStack.add(NotificationSettingsRoute)
                        },
                        onNavigateToAbout = { backStack.add(AboutRoute) },
                        onNavigateToProfile = { backStack.add(ProfileRoute) }
                    )
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
                entry<CreateRoute> {
                    CreateScreen()
                }
                entry<PacksRoute> {
                    PacksScreen(
                        onNavigateToPackDetail = { packId ->
                            backStack.add(PackDetailRoute(packId))
                        }
                    )
                }
                entry<PackDetailRoute> {
                    PackDetailScreen(
                        onNavigateBack = { backStack.removeLastOrNull() }
                    )
                }
                entry<ArchiveRoute> {
                    ArchiveScreen()
                }
                entry<ReferencesRoute> {
                    ReferencesScreen(
                        onNavigateToRuneDetail = { runeId ->
                            backStack.add(RuneDetailRoute(runeId))
                        }
                    )
                }
                entry<RuneDetailRoute> {
                    RuneDetailScreen(
                        onNavigateBack = { backStack.removeLastOrNull() }
                    )
                }
                entry<ShareRoute> {
                    ShareScreen(
                        onNavigateBack = { backStack.removeLastOrNull() }
                    )
                }
                entry<TranslationRoute> {
                    TranslationScreen()
                }
                entry<ProfileRoute> {
                    ProfileScreen()
                }
                entry<NotificationSettingsRoute> {
                    NotificationSettingsScreen(
                        onNavigateBack = { backStack.removeLastOrNull() }
                    )
                }
                entry<AboutRoute> {
                    AboutScreen(
                        onNavigateBack = { backStack.removeLastOrNull() }
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
            icon = {
                Box(modifier = Modifier.semantics { contentDescription = "Today" }) {
                    Text("ᚱ")
                }
            },
            label = { Text("Today") },
            modifier = Modifier.testTag("tab_today")
        )
        NavigationBarItem(
            selected = currentRoute is QuoteListRoute,
            onClick = { switchTopLevelRoute(backStack, QuoteListRoute) },
            icon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.List,
                    contentDescription = "Library"
                )
            },
            label = { Text("Library") },
            modifier = Modifier.testTag("tab_library")
        )
        NavigationBarItem(
            selected = currentRoute is CreateRoute,
            onClick = { switchTopLevelRoute(backStack, CreateRoute) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create"
                )
            },
            label = { Text("Create") },
            modifier = Modifier.testTag("tab_create")
        )
        NavigationBarItem(
            selected = currentRoute is PacksRoute,
            onClick = { switchTopLevelRoute(backStack, PacksRoute) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Packs"
                )
            },
            label = { Text("Packs") },
            modifier = Modifier.testTag("tab_packs")
        )
        NavigationBarItem(
            selected = currentRoute is SettingsRoute,
            onClick = { switchTopLevelRoute(backStack, SettingsRoute) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings"
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

// Navigation 3 Scene keys contain the route class name.
// String matching is the idiomatic approach since Scene<Any> doesn't expose
// the original typed route object in transition specs.
private fun routeRank(scene: Scene<Any>): Int {
    val key = scene.key.toString()
    return when {
        key.contains("OnboardingRoute") -> 0
        key.contains("QuoteRoute") -> 1
        key.contains("QuoteListRoute") -> 2
        key.contains("CreateRoute") -> 3
        key.contains("PacksRoute") -> 4
        key.contains("SettingsRoute") -> 5
        key.contains("AddEditQuoteRoute") -> 6
        key.contains("PackDetailRoute") -> 7
        key.contains("ArchiveRoute") -> 8
        key.contains("ReferencesRoute") -> 9
        key.contains("RuneDetailRoute") -> 10
        key.contains("ShareRoute") -> 11
        key.contains("TranslationRoute") -> 12
        key.contains("ProfileRoute") -> 13
        key.contains("NotificationSettingsRoute") -> 14
        key.contains("AboutRoute") -> 15
        else -> 3
    }
}

private fun androidx.compose.animation.AnimatedContentTransitionScope<Scene<Any>>.resolveSceneTransition(
    initialState: Scene<Any>,
    targetState: Scene<Any>,
    duration: Int,
    easing: androidx.compose.animation.core.Easing
): androidx.compose.animation.ContentTransform {
    val initialKind = routeKind(initialState)
    val targetKind = routeKind(targetState)
    val shouldUseFade = (initialKind == RouteKind.TOP_LEVEL && targetKind == RouteKind.TOP_LEVEL) ||
        initialKind == RouteKind.ONBOARDING || targetKind == RouteKind.ONBOARDING

    if (shouldUseFade) {
        return fadeIn(animationSpec = tween(durationMillis = duration, easing = easing)) togetherWith
            fadeOut(animationSpec = tween(durationMillis = duration, easing = easing))
    }

    val forward = routeRank(targetState) >= routeRank(initialState)
    val direction = if (forward) {
        slideIntoContainerLeft()
    } else {
        slideIntoContainerRight()
    }
    return direction.first(duration, easing) togetherWith direction.second(duration, easing)
}

private enum class RouteKind {
    ONBOARDING,
    TOP_LEVEL,
    DETAIL
}

private fun routeKind(scene: Scene<Any>): RouteKind {
    val key = scene.key.toString()
    return when {
        key.contains("OnboardingRoute") -> RouteKind.ONBOARDING
        key.contains("QuoteRoute") || key.contains("QuoteListRoute") ||
            key.contains("CreateRoute") || key.contains("PacksRoute") ||
            key.contains("SettingsRoute") -> RouteKind.TOP_LEVEL
        else -> RouteKind.DETAIL
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
