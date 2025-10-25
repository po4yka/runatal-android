package com.po4yka.runicquotes.ui.widget

import android.graphics.Bitmap

/**
 * Data class representing the state of the widget.
 */
data class WidgetState(
    val runicText: String = "",
    val runicBitmap: Bitmap? = null,
    val latinText: String = "",
    val author: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)
