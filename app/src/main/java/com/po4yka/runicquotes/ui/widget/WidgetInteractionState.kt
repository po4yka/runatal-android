package com.po4yka.runicquotes.ui.widget

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.ConcurrentHashMap

/**
 * Ephemeral widget interaction state used by widget callbacks.
 */
object WidgetInteractionState {
    private val randomRequested = ConcurrentHashMap<String, AtomicBoolean>()

    fun requestRandomQuote(widgetKey: String) {
        randomRequested.getOrPut(widgetKey) { AtomicBoolean(false) }.set(true)
    }

    fun consumeRandomQuoteRequest(widgetKey: String): Boolean {
        return randomRequested[widgetKey]?.getAndSet(false) ?: false
    }

    fun clear(widgetKey: String) {
        randomRequested.remove(widgetKey)
    }

    fun clear() {
        randomRequested.clear()
    }
}
