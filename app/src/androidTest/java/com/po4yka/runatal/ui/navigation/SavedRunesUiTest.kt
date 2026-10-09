package com.po4yka.runatal.ui.navigation

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.po4yka.runatal.MainActivity
import com.po4yka.runatal.ui.dismissOnboardingIfNeeded
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Exercises reference bookmarking and the actual Profile destination. */
@RunWith(AndroidJUnit4::class)
class SavedRunesUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun bookmarkedReferenceIsReachableFromProfileAndCanBeRemoved() {
        dismissOnboardingIfNeeded(composeRule)
        composeRule.onNodeWithTag("tab_settings").performClick()
        composeRule.onNodeWithText("Rune References").performScrollTo().performClick()
        waitFor("Fehu")
        composeRule.onNodeWithText("Fehu").performClick()
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithText("Save rune").fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodesWithText("Remove saved rune").fetchSemanticsNodes().isNotEmpty()
        }
        if (composeRule.onAllNodesWithText("Remove saved rune").fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithText("Remove saved rune").performClick()
            waitFor("Save rune")
        }
        composeRule.onNodeWithText("Save rune").performClick()
        waitFor("Remove saved rune")
        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.onNodeWithText("Profile").performScrollTo().performClick()
        composeRule.onNodeWithText("Saved Runes").performScrollTo().performClick()
        waitFor("Fehu")
        composeRule.onNodeWithText("Fehu").performClick()
        waitFor("Remove saved rune")
        composeRule.onNodeWithText("Remove saved rune").performClick()
        waitFor("Save rune")
        composeRule.onNodeWithContentDescription("Back").performClick()
        waitFor("Saved runes")
        composeRule.waitUntil(10_000) { composeRule.onAllNodesWithText("Fehu").fetchSemanticsNodes().isEmpty() }
    }

    private fun waitFor(text: String) {
        composeRule.waitUntil(10_000) { composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    }
}
