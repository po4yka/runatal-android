package com.po4yka.runicquotes.ui.translation

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.po4yka.runicquotes.MainActivity
import com.po4yka.runicquotes.ui.dismissOnboardingIfNeeded
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertTrue
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TranslationExperienceUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun translationScreen_youngerStrictGoldExampleShowsDerivationAndProvenance() {
        openTranslationScreen()

        composeRule.onNodeWithTag("translation_mode_translate").performClick()
        composeRule.onNodeWithTag("translation_script_younger_futhark").performClick()
        composeRule.onNodeWithTag("translation_input_text").performTextInput("The wolf hunts at night")

        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithTag("translation_meta_section").fetchSemanticsNodes().isNotEmpty()
        }

        assertTrue(composeRule.onAllNodesWithText("Gold example").fetchSemanticsNodes().isNotEmpty())
        assertTrue(composeRule.onAllNodesWithTag("translation_provenance_section").fetchSemanticsNodes().isNotEmpty())
        assertTrue(composeRule.onAllNodesWithText("Runor-aligned Younger exemplar").fetchSemanticsNodes().isNotEmpty())
    }

    @Test
    fun translationScreen_elderStrictUnavailableShowsExplanation() {
        openTranslationScreen()

        composeRule.onNodeWithTag("translation_mode_translate").performClick()
        composeRule.onNodeWithTag("translation_script_elder_futhark").performClick()
        composeRule.onNodeWithTag("translation_input_text").performTextInput("signal")

        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithText("Elder Futhark unavailable").fetchSemanticsNodes().isNotEmpty()
        }

        assertTrue(
            composeRule.onAllNodesWithText("Missing attested or reconstructed Elder Futhark pattern.")
                .fetchSemanticsNodes().isNotEmpty()
        )
        assertTrue(composeRule.onAllNodesWithText("Suggested fallback").fetchSemanticsNodes().isNotEmpty())
    }

    @Test
    fun translationScreen_ereborSequenceTranscriptionShowsTrackAndSources() {
        openTranslationScreen()

        composeRule.onNodeWithTag("translation_mode_translate").performClick()
        composeRule.onNodeWithTag("translation_script_cirth").performClick()
        composeRule.onNodeWithTag("translation_input_text").performTextInput("night")

        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithText("Sequence transcription").fetchSemanticsNodes().isNotEmpty()
        }

        assertTrue(composeRule.onAllNodesWithText("Erebor transcription").fetchSemanticsNodes().isNotEmpty())
        assertTrue(composeRule.onAllNodesWithText("Sequence transcription").fetchSemanticsNodes().isNotEmpty())
        assertTrue(composeRule.onAllNodesWithText("Appendix E").fetchSemanticsNodes().isNotEmpty())
    }

    @Test
    fun translationAccuracyScreen_rendersSectionsAndBackNavigation() {
        openTranslationScreen()

        composeRule.onNodeWithTag("translation_accuracy_link").performScrollTo().performClick()

        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithText("Known limitations").fetchSemanticsNodes().isNotEmpty()
        }

        assertTrue(composeRule.onAllNodesWithText("Historical context").fetchSemanticsNodes().isNotEmpty())
        assertTrue(composeRule.onAllNodesWithText("Rune reference").fetchSemanticsNodes().isNotEmpty())
        composeRule.onNodeWithContentDescription("Back").performClick()

        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithTag("translation_input_text").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun openTranslationScreen() {
        dismissOnboardingIfNeeded(composeRule)

        composeRule.onNodeWithTag("tab_settings").performClick()
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithText("Settings").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText("Translation").performScrollTo().performClick()
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithTag("translation_input_text").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("translation_input_text").performTextClearance()
    }
}
