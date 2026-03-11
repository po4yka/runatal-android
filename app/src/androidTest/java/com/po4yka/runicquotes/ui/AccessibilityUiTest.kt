package com.po4yka.runicquotes.ui

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.po4yka.runicquotes.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccessibilityUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun quoteScreen_exposesHeadingAndAccessibleActions() {
        dismissOnboardingIfNeeded(composeRule)

        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithContentDescription("Share quote").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText("Quote of the Day").assert(isHeading())
        composeRule.onNodeWithTag("quote_word_by_word_toggle").assert(hasDefinedStateDescription())

        if (composeRule.onAllNodesWithContentDescription("Save quote").fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithText("Saved").assert(hasContentDescriptionValue("Save quote"))
        } else {
            composeRule.onNodeWithText("Saved").assert(hasContentDescriptionValue("Remove quote from saved"))
        }
    }

    @Test
    fun settingsAndNotifications_exposeSwitchMetadata() {
        dismissOnboardingIfNeeded(composeRule)

        composeRule.onNodeWithTag("tab_settings").performClick()
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithText("Settings").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText("Settings").assert(isHeading())
        composeRule.onNodeWithText("Large Runes")
            .assert(hasRole(Role.Switch))
            .assert(hasDefinedStateDescription())

        composeRule.onNodeWithText("Notification schedule").performScrollTo().performClick()
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithText("Notifications").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText("Notifications").assert(isHeading())
        composeRule.onNodeWithText("Daily Quote Alert")
            .assert(hasRole(Role.Switch))
            .assert(hasDefinedStateDescription())
    }

    @Test
    fun translationScreen_exposesLabeledInputAndReadableOutput() {
        dismissOnboardingIfNeeded(composeRule)

        composeRule.onNodeWithTag("tab_settings").performClick()
        composeRule.onNodeWithText("Translation").performScrollTo().performClick()

        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithText("Translate").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText("Translate").assert(isHeading())
        composeRule.onNodeWithTag("translation_input_text")
            .assert(hasContentDescriptionValue("English text input"))
        composeRule.onNodeWithTag("translation_input_text").performTextInput("rune stone")

        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithContentDescription("rune stone").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("translation_word_by_word_toggle").assert(hasDefinedStateDescription())
    }

    @Test
    fun editorScreen_exposesLabeledFields() {
        dismissOnboardingIfNeeded(composeRule)

        composeRule.onNodeWithTag("tab_create").performClick()
        composeRule.onNodeWithText("New custom quote").performClick()

        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithText("Create Quote").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText("Create Quote").assert(isHeading())
        composeRule.onNodeWithTag("add_edit_quote_text").assert(hasContentDescriptionValue("Quote"))
        composeRule.onNodeWithTag("add_edit_author_text").assert(hasContentDescriptionValue("Author"))
    }

    private fun isHeading(): SemanticsMatcher = SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)

    private fun hasDefinedStateDescription(): SemanticsMatcher =
        SemanticsMatcher.keyIsDefined(SemanticsProperties.StateDescription)

    private fun hasRole(role: Role): SemanticsMatcher =
        SemanticsMatcher.expectValue(SemanticsProperties.Role, role)

    private fun hasContentDescriptionValue(value: String): SemanticsMatcher =
        SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf(value))
}
