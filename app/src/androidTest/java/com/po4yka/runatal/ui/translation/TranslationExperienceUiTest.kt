package com.po4yka.runatal.ui.translation

import android.content.ClipboardManager
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
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
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TranslationExperienceUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun translationScreen_attestedOutputShowsHeuristicScoreWithEvidenceAndContext() {
        openTranslationScreen()

        composeRule.onNodeWithTag("translation_mode_translate").performClick()
        composeRule.onNodeWithTag("translation_script_elder_futhark").performClick()
        composeRule.onNodeWithTag("translation_fidelity_strict").performClick()
        composeRule.onNodeWithTag("translation_input_text").performTextInput("Hlewagastiz")
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithText("Attested").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("translation_heuristic_score").performScrollTo()
            .assertIsDisplayed().assertTextEquals("0.98 / 1")
        composeRule.onNodeWithTag("translation_score_context").performScrollTo().assertIsDisplayed()
            .assertTextEquals(
                "This heuristic summarizes local evidence and rule coverage. " +
                    "It is not a measured probability of correctness."
            )
        assertTrue(composeRule.onAllNodesWithText("hlewagastiR").fetchSemanticsNodes().isNotEmpty())
        assertTrue(composeRule.onAllNodes(
            hasText("Gallehus short golden horn inscription", substring = true) and
                hasAnyAncestor(hasTestTag("translation_provenance_section"))
        ).fetchSemanticsNodes().isNotEmpty())

        // The score accompanies the real attested output, not a synthetic metadata-only preview.
        composeRule.onNodeWithText("Copy").performScrollTo().performClick()
        composeRule.runOnIdle {
            val clipboard = composeRule.activity.getSystemService(ClipboardManager::class.java)
            assertEquals("ᚻᛚᛖᚹᚨᚷᚨᛊᛏᛁᛉ", clipboard.primaryClip?.getItemAt(0)?.text?.toString())
        }
    }

    @Test
    fun translationScreen_youngerStrictGrammarShowsDerivationAndProvenance() {
        openTranslationScreen()

        composeRule.onNodeWithTag("translation_mode_translate").performClick()
        composeRule.onNodeWithTag("translation_script_younger_futhark").performClick()
        composeRule.onNodeWithTag("translation_input_text").performTextInput("The wolf hunts at night")

        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithTag("translation_meta_section").fetchSemanticsNodes().isNotEmpty()
        }

        assertTrue(composeRule.onAllNodesWithText("Token composed").fetchSemanticsNodes().isNotEmpty())
        assertTrue(composeRule.onAllNodesWithText("úlfrinn veiðir um nótt").fetchSemanticsNodes().isNotEmpty())
        assertTrue(composeRule.onAllNodesWithTag("translation_provenance_section").fetchSemanticsNodes().isNotEmpty())
        assertTrue(
            composeRule.onAllNodes(
                hasText("Michael Barnes: A New Introduction to Old Norse I", substring = true) and
                    hasAnyAncestor(hasTestTag("translation_provenance_section"))
            ).fetchSemanticsNodes().isNotEmpty()
        )
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
    fun translationScreen_publishedCirthProfileShowsReconstructionGlyphsAndSources() {
        openTranslationScreen()

        composeRule.onNodeWithTag("translation_mode_translate").performClick()
        composeRule.onNodeWithTag("translation_script_cirth").performClick()
        composeRule.onNodeWithTag("translation_fidelity_strict").performClick()
        composeRule.onNodeWithTag("translation_input_text").performTextClearance()
        composeRule.onNodeWithTag("translation_input_text")
            .performTextInput("The Lord of the Rings translated from the Red Book")
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithText("Phrase template").fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue(composeRule.onAllNodesWithText("Erebor transcription").fetchSemanticsNodes().isNotEmpty())
        assertTrue(composeRule.onAllNodesWithText("Reconstructed").fetchSemanticsNodes().isNotEmpty())
        assertTrue(composeRule.onAllNodesWithText("the lord of the rings translated from the red book")
            .fetchSemanticsNodes().isNotEmpty())
        assertTrue(composeRule.onAllNodes(
            hasText("Published title-page English profile", substring = true) and
                hasAnyAncestor(hasTestTag("translation_provenance_section"))
        ).fetchSemanticsNodes().isNotEmpty())

        // Copy observes the actual output despite RunicText's source-based accessibility description.
        composeRule.onNodeWithText("Copy").performScrollTo().performClick()
        val expected = "\uE0E9\uE08A\uE0BA\uE0E7\uE09E\uE0B3\uE08B\uE088\uE0E7" +
            "\uE0B3\uE083\uE0E7\uE08A\uE0BA\uE0E7\uE08B\uE0A7\uE0A3\uE0A2\uE0E7 " +
            "\uE087\uE08B\uE0B1\uE095\uE0A2\uE09E\uE0B1\uE087\uE0BB\uE088\uE0E7" +
            "\uE082\uE08B\uE0B3\uE085\uE0E7\uE08A\uE0BA\uE0E7\uE08B\uE0AF\uE088\uE0E7" +
            "\uE081\uE0B4\uE091\uE0EA"
        composeRule.runOnIdle {
            val clipboard = composeRule.activity.getSystemService(ClipboardManager::class.java)
            assertEquals(expected, clipboard.primaryClip?.getItemAt(0)?.text?.toString())
        }
    }

    @Test
    fun translationAccuracyScreen_rendersSectionsAndBackNavigation() {
        openTranslationScreen()
        composeRule.onNodeWithTag("translation_mode_transliterate").performClick()
        composeRule.onNodeWithTag("translation_script_elder_futhark").performClick()
        composeRule.onNodeWithTag("translation_input_text").performTextInput("CK Q QU X V W")
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithText("Copy").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Copy").performScrollTo().performClick()
        composeRule.runOnIdle {
            val clipboard = composeRule.activity.getSystemService(ClipboardManager::class.java)
            assertEquals("ᚲᚲ ᚲᚹ ᚲᚹ ᚲᛊ ᚢ ᚹ", clipboard.primaryClip?.getItemAt(0)?.text?.toString())
        }

        composeRule.onNodeWithTag("translation_accuracy_link").performScrollTo().performClick()

        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithText("Known limitations").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Modern Elder mappings"))
        composeRule.onNodeWithText("Modern Elder mappings").assertIsDisplayed()
        assertTrue(composeRule.onAllNodes(
            hasText("Q and QU to KW (ᚲᚹ); X to KS (ᚲᛊ)", substring = true)
        ).fetchSemanticsNodes().isNotEmpty())
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Scores are heuristic"))
        composeRule.onNodeWithText("Scores are heuristic").assertIsDisplayed()
        assertTrue(composeRule.onAllNodes(
            hasText("not calibrated against measured accuracy", substring = true)
        ).fetchSemanticsNodes().isNotEmpty())

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Historical context"))
        assertTrue(composeRule.onAllNodesWithText("Historical context").fetchSemanticsNodes().isNotEmpty())
        composeRule.onNodeWithText("Historical context").assertIsDisplayed()
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Rune reference"))
        assertTrue(composeRule.onAllNodesWithText("Rune reference").fetchSemanticsNodes().isNotEmpty())
        composeRule.onNodeWithText("Rune reference").assertIsDisplayed()
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
