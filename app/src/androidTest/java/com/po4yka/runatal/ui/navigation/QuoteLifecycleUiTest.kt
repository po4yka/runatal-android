package com.po4yka.runatal.ui.navigation

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.po4yka.runatal.MainActivity
import com.po4yka.runatal.ui.dismissOnboardingIfNeeded
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Real application journeys verify that retained states have reachable positive restore actions. */
@RunWith(AndroidJUnit4::class)
class QuoteLifecycleUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun library_hideQuoteAppearsInHiddenAndRestoresToLibrary() {
        val text = "Lifecycle hidden ${System.nanoTime()}"
        createQuote(text)
        openActions(text)
        composeRule.onNodeWithText("Hide Quote").performScrollTo().performClick()
        waitUntilAbsent("\"$text\"")
        restoreFromArchive(text, "Hidden")
    }

    @Test
    fun library_archiveQuoteAppearsInArchiveAndRestoresToLibrary() {
        val text = "Lifecycle archived ${System.nanoTime()}"
        createQuote(text)
        openActions(text)
        composeRule.onNodeWithText("Archive Quote").performScrollTo().performClick()
        waitUntilAbsent("\"$text\"")
        restoreFromArchive(text, "Archived")
    }

    @Test
    fun library_deleteMovesToTrashAndRestoreRemainsAvailableBeyondSnackbar() {
        val text = "Lifecycle trash ${System.nanoTime()}"
        createQuote(text)
        openActions(text)
        composeRule.onNodeWithText("Delete Quote").performScrollTo().performClick()
        composeRule.onNodeWithText("Delete").performClick()
        waitUntilAbsent("\"$text\"")
        restoreFromArchive(text, "Deleted")
    }

    private fun createQuote(text: String) {
        dismissOnboardingIfNeeded(composeRule)
        composeRule.onNodeWithTag("tab_create").performClick()
        composeRule.onNodeWithText("New custom quote").performClick()
        composeRule.onNodeWithTag("add_edit_quote_text").performTextInput(text)
        composeRule.onNodeWithTag("add_edit_author_text").performTextInput("Lifecycle UI")
        composeRule.onNodeWithTag("add_edit_save_button").performClick()
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithText("View in Library").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("View in Library").performClick()
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithText("Your collection").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNode(hasSetTextAction()).performTextClearance()
        val custom = hasText("Custom ", substring = true) and
            SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)
        composeRule.onNode(custom).performClick()
        composeRule.onNodeWithTag("quote_list_lazy").performScrollToNode(hasText("\"$text\""))
        composeRule.onNodeWithText("\"$text\"").assertIsDisplayed()
    }

    private fun openActions(text: String) {
        composeRule.onNodeWithText("\"$text\"").onParent().onChildren()
            .filterToOne(hasContentDescription("More actions")).performClick()
    }

    private fun restoreFromArchive(text: String, tab: String) {
        composeRule.onNodeWithContentDescription("Open archive").performClick()
        composeRule.onNodeWithText(tab).performClick()
        val label = "\u201C$text\u201D"
        composeRule.onNodeWithTag("archive_list_lazy").performScrollToNode(hasText(label))
        composeRule.onNodeWithText(label).assertIsDisplayed()
        composeRule.onNodeWithText(label).onParent().onChildren()
            .filterToOne(hasContentDescription("Restore quote")).performClick()
        waitUntilAbsent(label)
        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.onNodeWithTag("quote_list_lazy").performScrollToNode(hasText("\"$text\""))
        composeRule.onNodeWithText("\"$text\"").assertIsDisplayed()
    }

    private fun waitUntilAbsent(text: String) {
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isEmpty()
        }
    }
}
