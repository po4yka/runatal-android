package com.po4yka.runicquotes.ui.widget

/**
 * Data class representing the state of the widget.
 */
data class WidgetState(
    val runicText: String = "",
    val latinText: String = "",
    val author: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)
