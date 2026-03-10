package com.po4yka.runicquotes.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Easing
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
import androidx.navigation3.runtime.EntryProviderScope
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

    val bottomBarDuration = motion.duration(
        reducedMotion = reducedMotion,
        base = motion.shortDurationMillis
    )

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = bottomBarEnterTransition(reducedMotion, bottomBarDuration),
                exit = bottomBarExitTransition(reducedMotion, bottomBarDuration)
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
                sceneTransitionOrNone(bottomBarDuration, motion.standardEasing)
            },
            popTransitionSpec = {
                sceneTransitionOrNone(bottomBarDuration, motion.standardEasing)
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
                        slideTransitionPair(AnimatedContentTransitionScope.SlideDirection.Right)
                    } else {
                        slideTransitionPair(AnimatedContentTransitionScope.SlideDirection.Left)
                    }
                    direction.first(duration, motion.standardEasing) togetherWith
                        direction.second(duration, motion.standardEasing)
                }
            },
            entryProvider = navEntryProvider(
                backStack = backStack,
                selectedScript = selectedScript,
                onSelectOnboardingStyle = onSelectOnboardingStyle,
                onCompleteOnboarding = onCompleteOnboarding
            )
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

@Composable
private fun navEntryProvider(
    backStack: SnapshotStateList<Any>,
    selectedScript: RunicScript,
    onSelectOnboardingStyle: (RunicScript, String) -> Unit,
    onCompleteOnboarding: () -> Unit
) = entryProvider {
    onboardingEntries(backStack, selectedScript, onSelectOnboardingStyle, onCompleteOnboarding)
    topLevelEntries(backStack)
    detailEntries(backStack)
}

@Composable
private fun EntryProviderScope<Any>.onboardingEntries(
    backStack: SnapshotStateList<Any>,
    selectedScript: RunicScript,
    onSelectOnboardingStyle: (RunicScript, String) -> Unit,
    onCompleteOnboarding: () -> Unit
) {
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
}

@Composable
private fun EntryProviderScope<Any>.topLevelEntries(
    backStack: SnapshotStateList<Any>
) {
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
}

@Composable
private fun EntryProviderScope<Any>.detailEntries(
    backStack: SnapshotStateList<Any>
) {
    entry<AddEditQuoteRoute> { route ->
        AddEditQuoteScreen(
            onNavigateBack = { backStack.removeLastOrNull() },
            quoteId = route.quoteId
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

private fun bottomBarEnterTransition(
    reducedMotion: Boolean,
    duration: Int
): EnterTransition = if (reducedMotion) {
    EnterTransition.None
} else {
    fadeIn(animationSpec = tween(durationMillis = duration))
}

private fun bottomBarExitTransition(
    reducedMotion: Boolean,
    duration: Int
): ExitTransition = if (reducedMotion) {
    ExitTransition.None
} else {
    fadeOut(animationSpec = tween(durationMillis = duration))
}

private typealias SlideTransitionPair = Pair<
    (Int, Easing) -> EnterTransition,
    (Int, Easing) -> ExitTransition
>

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
private val routeRankMap = mapOf(
    "OnboardingRoute" to 0,
    "QuoteRoute" to 1,
    "QuoteListRoute" to 2,
    "CreateRoute" to 3,
    "PacksRoute" to 4,
    "SettingsRoute" to 5,
    "AddEditQuoteRoute" to 6,
    "PackDetailRoute" to 7,
    "ArchiveRoute" to 8,
    "ReferencesRoute" to 9,
    "RuneDetailRoute" to 10,
    "ShareRoute" to 11,
    "TranslationRoute" to 12,
    "ProfileRoute" to 13,
    "NotificationSettingsRoute" to 14,
    "AboutRoute" to 15
)

private fun routeRank(scene: Scene<Any>): Int {
    val key = scene.key.toString()
    return routeRankMap.entries.firstOrNull { key.contains(it.key) }?.value ?: 3
}

private fun AnimatedContentTransitionScope<Scene<Any>>.sceneTransitionOrNone(
    duration: Int,
    easing: Easing
): ContentTransform = if (duration == 0) {
    EnterTransition.None togetherWith ExitTransition.None
} else {
    resolveSceneTransition(
        initialState = initialState,
        targetState = targetState,
        duration = duration,
        easing = easing
    )
}

private fun AnimatedContentTransitionScope<Scene<Any>>.resolveSceneTransition(
    initialState: Scene<Any>,
    targetState: Scene<Any>,
    duration: Int,
    easing: Easing
): ContentTransform {
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
        slideTransitionPair(AnimatedContentTransitionScope.SlideDirection.Left)
    } else {
        slideTransitionPair(AnimatedContentTransitionScope.SlideDirection.Right)
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

private fun AnimatedContentTransitionScope<Scene<Any>>.slideTransitionPair(
    direction: AnimatedContentTransitionScope.SlideDirection
): SlideTransitionPair = Pair(
    { duration, easing ->
        slideIntoContainer(
            towards = direction,
            animationSpec = tween(durationMillis = duration, easing = easing)
        ) + fadeIn(animationSpec = tween(durationMillis = duration))
    },
    { duration, easing ->
        slideOutOfContainer(
            towards = direction,
            animationSpec = tween(durationMillis = duration, easing = easing)
        ) + fadeOut(animationSpec = tween(durationMillis = duration))
    }
)
