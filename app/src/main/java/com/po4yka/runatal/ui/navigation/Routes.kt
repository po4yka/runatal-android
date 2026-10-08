package com.po4yka.runatal.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes for Navigation 3.
 * Using @Serializable data objects for compile-time route safety.
 */

/**
 * Quote screen - displays the daily quote in runic script.
 */
@Serializable
data object QuoteRoute : NavKey

/**
 * Onboarding screen - introduces scripts and lets users pick a default style.
 */
@Serializable
data object OnboardingRoute : NavKey

/**
 * Settings screen - allows users to configure preferences.
 */
@Serializable
data object SettingsRoute : NavKey

/**
 * Quote list screen - allows browsing, filtering and managing quotes.
 */
@Serializable
data object QuoteListRoute : NavKey

/**
 * Add/Edit quote screen.
 *
 * @property quoteId Quote ID to edit. Use 0 for creating a new quote.
 */
@Serializable
data class AddEditQuoteRoute(val quoteId: Long = 0L) : NavKey

/**
 * Packs screen - browse curated quote packs.
 */
@Serializable
data object PacksRoute : NavKey

/**
 * Pack detail screen - view quotes within a specific pack.
 *
 * @property packId Pack ID to display.
 */
@Serializable
data class PackDetailRoute(val packId: Long) : NavKey

/**
 * Create screen - create new custom quotes.
 */
@Serializable
data object CreateRoute : NavKey

/**
 * Archive screen - view archived and deleted quotes.
 */
@Serializable
data object ArchiveRoute : NavKey

/**
 * References screen - browse rune reference grid grouped by script.
 */
@Serializable
data object ReferencesRoute : NavKey

/**
 * Rune detail screen - detailed view of a single rune.
 *
 * @property runeId Rune reference ID to display.
 */
@Serializable
data class RuneDetailRoute(val runeId: Long) : NavKey

/**
 * Share screen - share a quote with configurable templates.
 *
 * @property quoteId Quote ID to share.
 */
@Serializable
data class ShareRoute(val quoteId: Long) : NavKey

/**
 * Translation screen - transliterate text using rune keyboards.
 */
@Serializable
data object TranslationRoute : NavKey

/**
 * Accuracy and context screen for translation caveats and history.
 */
@Serializable
data object TranslationAccuracyRoute : NavKey

/**
 * Profile screen - user stats and data overview.
 */
@Serializable
data object ProfileRoute : NavKey

/**
 * Notification settings screen - configure notification preferences.
 */
@Serializable
data object NotificationSettingsRoute : NavKey

/**
 * About screen - app version, source code, and acknowledgments.
 */
@Serializable
data object AboutRoute : NavKey
