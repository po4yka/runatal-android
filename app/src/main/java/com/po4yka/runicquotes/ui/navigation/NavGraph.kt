@file:Suppress("TooManyFunctions")

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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
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
import com.po4yka.runicquotes.ui.screens.translation.TranslationAccuracyScreen
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
        currentRoute is CreateRoute || currentRoute is SettingsRoute
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
                .consumeWindowInsets(paddingValues)
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
    val destinations = listOf(
        TopLevelDestination("Today", Icons.Default.AutoAwesome, QuoteRoute, "tab_today"),
        TopLevelDestination("Library", Icons.AutoMirrored.Filled.MenuBook, QuoteListRoute, "tab_library"),
        TopLevelDestination("Create", Icons.Default.EditNote, CreateRoute, "tab_create"),
        TopLevelDestination("Settings", Icons.Default.Settings, SettingsRoute, "tab_settings")
    )
    val navigationPadding = WindowInsets.navigationBars.asPaddingValues()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 10.dp,
                end = 10.dp,
                top = 4.dp,
                bottom = navigationPadding.calculateBottomPadding().coerceAtLeast(10.dp)
            )
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 0.dp,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                destinations.forEach { destination ->
                    TopLevelBottomBarItem(
                        label = destination.label,
                        icon = destination.icon,
                        selected = currentRoute::class == destination.route::class,
                        modifier = Modifier
                            .weight(1f)
                            .testTag(destination.testTag),
                        onClick = { switchTopLevelRoute(backStack, destination.route) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TopLevelBottomBarItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp)
            .semantics { contentDescription = label },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(width = 56.dp, height = 32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (selected) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (selected) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

private data class TopLevelDestination(
    val label: String,
    val icon: ImageVector,
    val route: Any,
    val testTag: String
)

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
            onNavigateToShare = { quoteId ->
                backStack.add(ShareRoute(quoteId = quoteId))
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
            onNavigateToProfile = { backStack.add(ProfileRoute) },
            onNavigateToReferences = { backStack.add(ReferencesRoute) },
            onNavigateToTranslation = { backStack.add(TranslationRoute) },
            onNavigateToPacks = { backStack.add(PacksRoute) }
        )
    }
    entry<QuoteListRoute> {
        QuoteListScreen(
            onNavigateToAddQuote = {
                backStack.add(AddEditQuoteRoute())
            },
            onNavigateToEditQuote = { quoteId ->
                backStack.add(AddEditQuoteRoute(quoteId = quoteId))
            },
            onNavigateToShare = { quoteId ->
                backStack.add(ShareRoute(quoteId = quoteId))
            },
            onNavigateToArchive = {
                backStack.add(ArchiveRoute)
            },
            onNavigateToPacks = {
                backStack.add(PacksRoute)
            }
        )
    }
    entry<CreateRoute> {
        CreateScreen(
            onCreateQuote = { backStack.add(AddEditQuoteRoute()) },
            onBrowsePacks = { backStack.add(PacksRoute) },
            onOpenTranslation = { backStack.add(TranslationRoute) }
        )
    }
    entry<PacksRoute> {
        PacksScreen(
            onNavigateBack = { backStack.removeLastOrNull() },
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
    entry<PackDetailRoute> { route ->
        PackDetailScreen(
            onNavigateBack = { backStack.removeLastOrNull() },
            onViewLibrary = {
                backStack.clear()
                backStack.add(QuoteListRoute)
            },
            packId = route.packId
        )
    }
    entry<ArchiveRoute> {
        ArchiveScreen()
    }
    entry<ReferencesRoute> {
        ReferencesScreen(
            onNavigateBack = { backStack.removeLastOrNull() },
            onNavigateToRuneDetail = { runeId ->
                backStack.add(RuneDetailRoute(runeId))
            }
        )
    }
    entry<RuneDetailRoute> { route ->
        RuneDetailScreen(
            onNavigateBack = { backStack.removeLastOrNull() },
            runeId = route.runeId
        )
    }
    entry<ShareRoute> { route ->
        ShareScreen(
            onNavigateBack = { backStack.removeLastOrNull() },
            quoteId = route.quoteId
        )
    }
    entry<TranslationRoute> {
        TranslationScreen(
            onNavigateBack = { backStack.removeLastOrNull() },
            onNavigateToAccuracyContext = { backStack.add(TranslationAccuracyRoute) }
        )
    }
    entry<TranslationAccuracyRoute> {
        TranslationAccuracyScreen(
            onNavigateBack = { backStack.removeLastOrNull() },
            onNavigateToReferences = { backStack.add(ReferencesRoute) }
        )
    }
    entry<ProfileRoute> {
        ProfileScreen(
            onNavigateBack = { backStack.removeLastOrNull() }
        )
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
    "SettingsRoute" to 4,
    "PacksRoute" to 5,
    "AddEditQuoteRoute" to 6,
    "PackDetailRoute" to 7,
    "ArchiveRoute" to 8,
    "ReferencesRoute" to 9,
    "RuneDetailRoute" to 10,
    "ShareRoute" to 11,
    "TranslationRoute" to 12,
    "TranslationAccuracyRoute" to 13,
    "ProfileRoute" to 14,
    "NotificationSettingsRoute" to 15,
    "AboutRoute" to 16
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
            key.contains("CreateRoute") || key.contains("SettingsRoute") -> RouteKind.TOP_LEVEL
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
