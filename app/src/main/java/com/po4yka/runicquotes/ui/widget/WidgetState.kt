package com.po4yka.runicquotes.ui.widget

import android.graphics.Bitmap
import android.graphics.Color

/**
 * Data class representing the state of the widget.
 */
enum class WidgetSizeClass {
    COMPACT,
    MEDIUM,
    EXPANDED
}

data class WidgetState(
    val runicText: String = "",
    val runicBitmap: Bitmap? = null,
    val latinText: String = "",
    val author: String = "",
    val scriptLabel: String = "",
    val modeLabel: String = "",
    val updateModeLabel: String = "",
    val palette: WidgetPalette = WidgetPalette.default(),
    val sizeClass: WidgetSizeClass = WidgetSizeClass.MEDIUM,
    val displayMode: WidgetDisplayMode = WidgetDisplayMode.RUNE_LATIN,
    val isLoading: Boolean = false,
    val error: String? = null
)

data class WidgetPalette(
    val background: Int,
    val surface: Int,
    val onBackground: Int,
    val onSurface: Int,
    val primary: Int,
    val primaryContainer: Int,
    val onPrimaryContainer: Int,
    val error: Int,
    val runicText: Int
) {
    companion object {
        fun default() = WidgetPalette(
            background = Color.parseColor("#101419"),
            surface = Color.parseColor("#171C21"),
            onBackground = Color.parseColor("#E3E8EE"),
            onSurface = Color.parseColor("#E3E8EE"),
            primary = Color.parseColor("#B8C8D9"),
            primaryContainer = Color.parseColor("#364757"),
            onPrimaryContainer = Color.parseColor("#D6E4F2"),
            error = Color.parseColor("#FFB4AB"),
            runicText = Color.parseColor("#F5F7FA")
        )
    }
}
