package com.po4yka.runicquotes.ui.navigation

/**
 * Sealed class representing navigation destinations in the app.
 */
sealed class Screen(val route: String) {
    /**
     * Quote screen - displays the daily quote in runic script.
     */
    data object Quote : Screen("quote")

    /**
     * Settings screen - allows users to configure preferences.
     */
    data object Settings : Screen("settings")
}
