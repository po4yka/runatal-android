package com.po4yka.runicquotes.ui.widget

import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test

class WidgetInteractionStateTest {

    @After
    fun tearDown() {
        WidgetInteractionState.clear()
    }

    @Test
    fun `requestRandomQuote is consumed only once per widget`() {
        WidgetInteractionState.requestRandomQuote("widget-1")

        assertThat(WidgetInteractionState.consumeRandomQuoteRequest("widget-1")).isTrue()
        assertThat(WidgetInteractionState.consumeRandomQuoteRequest("widget-1")).isFalse()
    }

    @Test
    fun `clear removes a single widget state`() {
        WidgetInteractionState.requestRandomQuote("widget-1")
        WidgetInteractionState.requestRandomQuote("widget-2")

        WidgetInteractionState.clear("widget-1")

        assertThat(WidgetInteractionState.consumeRandomQuoteRequest("widget-1")).isFalse()
        assertThat(WidgetInteractionState.consumeRandomQuoteRequest("widget-2")).isTrue()
    }
}
