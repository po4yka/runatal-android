package com.po4yka.runicquotes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.po4yka.runicquotes.ui.navigation.NavGraph
import com.po4yka.runicquotes.ui.navigation.QuoteRoute
import com.po4yka.runicquotes.ui.screens.settings.SettingsViewModel
import com.po4yka.runicquotes.ui.theme.RunicQuotesTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main Activity for Runic Quotes.
 * Uses Jetpack Compose for the UI with Navigation.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RunicQuotesApp()
        }
    }
}

/**
 * Root composable for the Runic Quotes app.
 * Handles theme preferences and navigation.
 */
@Composable
fun RunicQuotesApp() {
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val preferences by settingsViewModel.userPreferences.collectAsState()
    val systemInDarkTheme = isSystemInDarkTheme()

    // Determine dark theme based on preferences
    val darkTheme = when (preferences.themeMode) {
        "light" -> false
        "dark" -> true
        else -> systemInDarkTheme // "system" or default
    }

    RunicQuotesTheme(
        darkTheme = darkTheme,
        dynamicColorEnabled = preferences.dynamicColorEnabled,
        themePack = preferences.themePack,
        runicFontScale = if (preferences.largeRunesEnabled) 1.25f else 1.0f,
        highContrast = preferences.highContrastEnabled,
        reducedMotion = preferences.reducedMotionEnabled
    ) {
        val backStack = rememberSaveable {
            mutableStateListOf<Any>(QuoteRoute)
        }
        NavGraph(
            backStack = backStack,
            hasCompletedOnboarding = preferences.hasCompletedOnboarding,
            selectedScript = preferences.selectedScript,
            selectedThemePack = preferences.themePack,
            onSelectOnboardingStyle = { script, themePack ->
                settingsViewModel.updateSelectedScript(script)
                settingsViewModel.updateThemePack(themePack)
            },
            onCompleteOnboarding = {
                settingsViewModel.completeOnboarding()
            }
        )
    }
}
