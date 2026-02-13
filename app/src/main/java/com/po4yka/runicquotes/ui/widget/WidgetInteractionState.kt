package com.po4yka.runicquotes.ui.widget

import java.util.concurrent.atomic.AtomicBoolean

/**
 * Ephemeral widget interaction state used by widget callbacks.
 */
object WidgetInteractionState {
    private val randomRequested = AtomicBoolean(false)

    fun requestRandomQuote() {
        randomRequested.set(true)
    }

    fun consumeRandomQuoteRequest(): Boolean {
        return randomRequested.getAndSet(false)
    }
}
