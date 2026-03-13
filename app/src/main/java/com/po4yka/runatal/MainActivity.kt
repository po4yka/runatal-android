package com.po4yka.runatal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.po4yka.runatal.ui.navigation.NavGraph
import com.po4yka.runatal.ui.navigation.QuoteRoute
import com.po4yka.runatal.ui.screens.settings.SettingsViewModel
import com.po4yka.runatal.ui.theme.RunatalTheme
import com.po4yka.runatal.util.AppIconManager
import com.po4yka.runatal.util.AppIconVariant
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
            RunatalApp()
        }
    }
}

/**
 * Root composable for the Runic Quotes app.
 * Handles theme preferences and navigation.
 */
@Composable
fun RunatalApp() {
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val preferences by settingsViewModel.userPreferences.collectAsStateWithLifecycle()
    val systemInDarkTheme = isSystemInDarkTheme()
    val context = LocalContext.current

    // Determine dark theme based on preferences
    val darkTheme = when (preferences.themeMode) {
        "light" -> false
        "dark" -> true
        else -> systemInDarkTheme // "system" or default
    }

    LaunchedEffect(preferences.appIconVariant) {
        AppIconManager.apply(
            context = context,
            variant = AppIconVariant.fromPersistedValue(preferences.appIconVariant)
        )
    }

    RunatalTheme(
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
