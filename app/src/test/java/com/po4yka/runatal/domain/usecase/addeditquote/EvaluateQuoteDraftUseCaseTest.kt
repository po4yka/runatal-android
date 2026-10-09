package com.po4yka.runatal.domain.usecase.addeditquote

import com.google.common.truth.Truth.assertThat
import com.po4yka.runatal.domain.model.QuoteInputPolicy
import org.junit.Test

class EvaluateQuoteDraftUseCaseTest {
    private val evaluate = EvaluateQuoteDraftUseCase()

    @Test
    fun `shared nonblank minimum accepts short text for creation and author only edits`() {
        listOf("I", "Go").forEach { source ->
            assertThat(evaluate(source, "User", false, "", "", true).canSave).isTrue()
            val edited = evaluate(source, "Changed author", true, source, "User", true)
            assertThat(edited.quoteTextError).isNull()
            assertThat(edited.canSave).isTrue()
            assertThat(edited.quoteCharCount).isEqualTo(source.length)
        }
    }

    @Test
    fun `blank source is invalid and existing quote and author budgets remain unchanged`() {
        listOf("", " ", "\t\n").forEach { blank ->
            val draft = evaluate(blank, "User", false, "", "", true)
            assertThat(draft.canSave).isFalse()
            assertThat(draft.quoteTextError).isEqualTo("Quote is required")
        }
        assertThat(evaluate("a".repeat(280), "a".repeat(60), false, "", "", true).canSave).isTrue()
        assertThat(evaluate("a".repeat(281), "User", false, "", "", true).quoteTextError)
            .isEqualTo("Keep quote under 280 characters")
        assertThat(evaluate("I", "a".repeat(61), false, "", "", true).authorError)
            .isEqualTo("Keep author under 60 characters")
        assertThat(QuoteInputPolicy.MIN_QUOTE_LENGTH).isEqualTo(1)
    }
}
