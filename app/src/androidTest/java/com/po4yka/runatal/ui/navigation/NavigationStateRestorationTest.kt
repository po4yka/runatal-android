package com.po4yka.runatal.ui.navigation

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.po4yka.runatal.MainActivity
import com.po4yka.runatal.ui.dismissOnboardingIfNeeded
import com.po4yka.runatal.ui.screens.settings.SettingsViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationStateRestorationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val settingsHeading = hasText("Settings") and
        SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)

    @Test
    fun restoresSettingsAfterActivityRecreation() {
        openSettings()

        composeRule.activityRule.scenario.recreate()

        awaitRestoredSettings()
    }

    @Test
    fun restoresSettingsWhileFreshViewModelsLoadPreferences() {
        openSettings()

        composeRule.activityRule.scenario.onActivity { it.viewModelStore.clear() }
        composeRule.activityRule.scenario.recreate()

        awaitRestoredSettings()
    }

    private fun openSettings() {
        dismissOnboardingIfNeeded(composeRule)
        composeRule.onNodeWithTag("tab_settings").performClick()
        awaitRestoredSettings()
    }

    private fun awaitRestoredSettings() {
        composeRule.waitUntil(10_000) {
            var preferencesLoaded = false
            composeRule.activityRule.scenario.onActivity { activity ->
                preferencesLoaded = ViewModelProvider(activity)[SettingsViewModel::class.java]
                    .onboardingCompleted.value == true
            }
            preferencesLoaded && composeRule.onAllNodes(settingsHeading).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.waitForIdle()
        composeRule.onNode(settingsHeading).assertIsDisplayed()
    }
}
