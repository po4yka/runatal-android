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
