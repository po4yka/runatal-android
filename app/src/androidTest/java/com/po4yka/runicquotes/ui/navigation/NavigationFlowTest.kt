package com.po4yka.runicquotes.ui.navigation

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.po4yka.runicquotes.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationFlowTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun userCanNavigateThroughQuoteManagementRoutes() {
        val quoteText = "UI route quote ${System.currentTimeMillis()}"
        val quoteAuthor = "Route Tester"

        composeRule.onNodeWithTag("tab_library")
            .performClick()
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithText("Browse Quotes").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("quote_list_add_button")
            .performClick()
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithText("Add Quote").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("add_edit_quote_text").performTextInput(quoteText)
        composeRule.onNodeWithTag("add_edit_author_text").performTextInput(quoteAuthor)
        composeRule.onNodeWithTag("add_edit_save_button").performClick()

        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithText("Browse Quotes").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText("My Quotes").performClick()
        composeRule.onNodeWithTag("quote_list_lazy")
            .performScrollToNode(hasText(quoteText, substring = false))

        composeRule.onNodeWithText(quoteText)
            .performClick()

        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithText("Edit Quote").fetchSemanticsNodes().isNotEmpty()
        }
    }
}
