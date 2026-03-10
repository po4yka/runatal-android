package com.po4yka.runicquotes.ui.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes for Navigation 3.
 * Using @Serializable data objects for compile-time route safety.
 */

/**
 * Quote screen - displays the daily quote in runic script.
 */
@Serializable
data object QuoteRoute

/**
 * Onboarding screen - introduces scripts and lets users pick a default style.
 */
@Serializable
data object OnboardingRoute

/**
 * Settings screen - allows users to configure preferences.
 */
@Serializable
data object SettingsRoute

/**
 * Quote list screen - allows browsing, filtering and managing quotes.
 */
@Serializable
data object QuoteListRoute

/**
 * Add/Edit quote screen.
 *
 * @property quoteId Quote ID to edit. Use 0 for creating a new quote.
 */
@Serializable
data class AddEditQuoteRoute(val quoteId: Long = 0L)

/**
 * Packs screen - browse curated quote packs.
 */
@Serializable
data object PacksRoute

/**
 * Pack detail screen - view quotes within a specific pack.
 *
 * @property packId Pack ID to display.
 */
@Serializable
data class PackDetailRoute(val packId: Long)

/**
 * Create screen - create new custom quotes.
 */
@Serializable
data object CreateRoute

/**
 * Archive screen - view archived and deleted quotes.
 */
@Serializable
data object ArchiveRoute

/**
 * References screen - browse rune reference grid grouped by script.
 */
@Serializable
data object ReferencesRoute

/**
 * Rune detail screen - detailed view of a single rune.
 *
 * @property runeId Rune reference ID to display.
 */
@Serializable
data class RuneDetailRoute(val runeId: Long)

/**
 * Share screen - share a quote with configurable templates.
 *
 * @property quoteId Quote ID to share.
 */
@Serializable
data class ShareRoute(val quoteId: Long)

/**
 * Translation screen - transliterate text using rune keyboards.
 */
@Serializable
data object TranslationRoute

/**
 * Accuracy and context screen for translation caveats and history.
 */
@Serializable
data object TranslationAccuracyRoute

/**
 * Profile screen - user stats and data overview.
 */
@Serializable
data object ProfileRoute

/**
 * Notification settings screen - configure notification preferences.
 */
@Serializable
data object NotificationSettingsRoute

/**
 * About screen - app version, source code, and acknowledgments.
 */
@Serializable
data object AboutRoute
