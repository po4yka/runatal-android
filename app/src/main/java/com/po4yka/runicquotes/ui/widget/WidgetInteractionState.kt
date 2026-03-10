package com.po4yka.runicquotes.ui.widget

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.ConcurrentHashMap

/**
 * Ephemeral widget interaction state used by widget callbacks.
 */
object WidgetInteractionState {
    private val randomRequested = ConcurrentHashMap<String, AtomicBoolean>()

    /** Flags a random-quote request for the given widget. */
    fun requestRandomQuote(widgetKey: String) {
        randomRequested.getOrPut(widgetKey) { AtomicBoolean(false) }.set(true)
    }

    /** Atomically consumes and returns the pending random-quote request flag. */
    fun consumeRandomQuoteRequest(widgetKey: String): Boolean {
        return randomRequested[widgetKey]?.getAndSet(false) ?: false
    }

    /** Clears the interaction state for a single widget. */
    fun clear(widgetKey: String) {
        randomRequested.remove(widgetKey)
    }

    /** Clears the interaction state for all widgets. */
    fun clear() {
        randomRequested.clear()
    }
}
