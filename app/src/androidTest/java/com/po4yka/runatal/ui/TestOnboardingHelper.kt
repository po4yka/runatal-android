package com.po4yka.runatal.ui

import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick

internal fun dismissOnboardingIfNeeded(composeRule: AndroidComposeTestRule<*, *>) {
    composeRule.waitForIdle()
    if (composeRule.onAllNodesWithTag("onboarding_finish_button").fetchSemanticsNodes().isNotEmpty()) {
        composeRule.onNodeWithTag("onboarding_finish_button").performClick()
    }
}
