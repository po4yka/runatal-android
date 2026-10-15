package com.po4yka.runatal.ui

import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick

internal fun dismissOnboardingIfNeeded(composeRule: AndroidComposeTestRule<*, *>) {
    composeRule.waitUntil(10_000) {
        composeRule.onAllNodesWithTag("onboarding_screen").fetchSemanticsNodes().isNotEmpty() ||
            composeRule.onAllNodesWithTag("tab_today").fetchSemanticsNodes().isNotEmpty()
    }

    // Welcome and script selection each advance through the same next-button tag.
    repeat(2) {
        if (composeRule.onAllNodesWithTag("onboarding_next_button").fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithTag("onboarding_next_button").performClick()
            composeRule.waitForIdle()
        }
    }

    composeRule.waitUntil(10_000) {
        composeRule.onAllNodesWithTag("onboarding_finish_button").fetchSemanticsNodes().isNotEmpty() ||
            composeRule.onAllNodesWithTag("tab_today").fetchSemanticsNodes().isNotEmpty()
    }
    if (composeRule.onAllNodesWithTag("onboarding_finish_button").fetchSemanticsNodes().isNotEmpty()) {
        composeRule.onNodeWithTag("onboarding_finish_button").performClick()
    }

    composeRule.waitUntil(10_000) {
        composeRule.onAllNodesWithTag("tab_today").fetchSemanticsNodes().isNotEmpty()
    }
}
