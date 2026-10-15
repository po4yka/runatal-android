package com.po4yka.runatal.ui.translation

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.po4yka.runatal.MainActivity
import com.po4yka.runatal.ui.dismissOnboardingIfNeeded
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WordByWordTransliterationUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun quoteScreen_wordToggleShowsBreakdown() {
        dismissOnboardingIfNeeded(composeRule)

        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithTag("quote_word_by_word_toggle").fetchSemanticsNodes().isNotEmpty()
        }

        if (composeRule.onAllNodesWithContentDescription("Show transliteration").fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithContentDescription("Show transliteration").performClick()
        }

        val wordToggle = hasTestTag("quote_word_by_word_toggle")
        val wordBreakdown = composeRule.onAllNodesWithTag("quote_word_breakdown", useUnmergedTree = true)
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodes(wordToggle and isEnabled()).fetchSemanticsNodes().isNotEmpty()
        }
        // Other journeys persist this setting; start from the full-transliteration state.
        if (composeRule.onAllNodes(wordToggle and isSelected()).fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithTag("quote_word_by_word_toggle").performClick()
        }
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodes(wordToggle and isSelected()).fetchSemanticsNodes().isEmpty() &&
                wordBreakdown.fetchSemanticsNodes().isEmpty()
        }
        assertTrue(wordBreakdown.fetchSemanticsNodes().isEmpty())

        composeRule.onNodeWithTag("quote_word_by_word_toggle").performClick()

        composeRule.waitUntil(10_000) {
            wordBreakdown.fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("quote_word_by_word_toggle").assertIsSelected()
    }

    @Test
    fun translationScreen_wordToggleShowsBreakdownSection() {
        dismissOnboardingIfNeeded(composeRule)

        composeRule.onNodeWithTag("tab_settings").performClick()
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithText("Settings").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText("Translation").performScrollTo().performClick()
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithText("Translate").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("translation_input_text").performTextInput("rune stone")
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithTag("translation_word_by_word_toggle").fetchSemanticsNodes().isNotEmpty()
        }

        assertTrue(composeRule.onAllNodesWithTag("translation_word_breakdown").fetchSemanticsNodes().isEmpty())

        composeRule.onNodeWithTag("translation_word_by_word_toggle").performClick()

        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithTag("translation_word_breakdown").fetchSemanticsNodes().isNotEmpty()
        }
    }
}
