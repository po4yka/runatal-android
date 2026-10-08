package com.po4yka.runatal.ui.navigation

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.po4yka.runatal.MainActivity
import com.po4yka.runatal.ui.dismissOnboardingIfNeeded
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CoreFlowsUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun todayScreen_canOpenShareAndCopyQuote() {
        dismissOnboardingIfNeeded(composeRule)

        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithContentDescription("Share quote").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithContentDescription("Share quote").performScrollTo().performClick()
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithText("Verse").fetchSemanticsNodes().isNotEmpty()
        }
        // The Card template offers export actions; Verse also exposes the copy action.
        composeRule.onNodeWithText("Verse").performScrollTo().performClick()
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithContentDescription("Copy quote").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithContentDescription("Copy quote").performScrollTo().performClick()
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithText("Quote copied").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun settingsScreen_canOpenNotificationSchedule_andReturn() {
        dismissOnboardingIfNeeded(composeRule)

        composeRule.onNodeWithTag("tab_settings").performClick()
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithText("Settings").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText("Notification schedule").performScrollTo().performClick()
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithText("Notifications").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText("Daily Quote Alert").performClick()
        composeRule.onNodeWithText("Community Picks").performClick()
        composeRule.onNodeWithText("Back to settings").performScrollTo().performClick()

        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithText("Settings").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun createTab_canCreateQuote_andOpenItFromLibrary() {
        val quoteText = "Create flow quote ${System.currentTimeMillis()}"
        val quoteAuthor = "Create Flow"

        dismissOnboardingIfNeeded(composeRule)

        composeRule.onNodeWithTag("tab_create").performClick()
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithText("Create").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText("New custom quote").performClick()
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithText("Create Quote").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("add_edit_quote_text").performTextInput(quoteText)
        composeRule.onNodeWithTag("add_edit_author_text").performTextInput(quoteAuthor)
        composeRule.onNodeWithTag("add_edit_save_button").performClick()

        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithText("View in Library").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("View in Library").performClick()

        composeRule.waitForIdle()
        if (composeRule.onAllNodesWithText("Your collection").fetchSemanticsNodes().isEmpty() &&
            composeRule.onAllNodesWithTag("tab_library").fetchSemanticsNodes().isNotEmpty()
        ) {
            composeRule.onNodeWithTag("tab_library").performClick()
        }

        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithText("Your collection").fetchSemanticsNodes().isNotEmpty()
        }

        val customFilter = hasText("Custom ", substring = true) and
            SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)
        composeRule.onNode(customFilter).performClick()
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodes(customFilter and isSelected()).fetchSemanticsNodes().size == 1
        }
        composeRule.onNode(customFilter).assertIsSelected()

        val quoteLabel = "\"$quoteText\""
        composeRule.onNodeWithTag("quote_list_lazy")
            .performScrollToNode(hasText(quoteLabel, substring = false))
        composeRule.onNodeWithText(quoteLabel)
            .onParent().onChildren()
            .filterToOne(hasContentDescription("More actions"))
            .performClick()

        composeRule.onNodeWithText("Edit Quote").performClick()
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodes(hasTestTag("add_edit_quote_text") and hasText(quoteText))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("add_edit_quote_text").assertTextContains(quoteText)
        composeRule.onNodeWithTag("add_edit_author_text").assertTextContains(quoteAuthor)
    }
}
