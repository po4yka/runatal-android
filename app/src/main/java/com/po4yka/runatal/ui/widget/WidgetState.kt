package com.po4yka.runatal.ui.widget

import android.graphics.Bitmap
import com.po4yka.runatal.data.preferences.WidgetDisplayMode
import com.po4yka.runatal.ui.theme.foundationRunicColorScheme

/**
 * Data class representing the state of the widget.
 */
enum class WidgetSizeClass {
    COMPACT,
    MEDIUM,
    EXPANDED
}

/** Snapshot of all data needed to render the widget. */
data class WidgetState(
    val quoteId: Long = 0,
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

/** Color palette applied to the widget surface. */
data class WidgetPalette(
    val background: Int,
    val surface: Int,
    val surfaceMuted: Int,
    val onBackground: Int,
    val onSurface: Int,
    val onSurfaceVariant: Int,
    val outline: Int,
    val primary: Int,
    val primaryContainer: Int,
    val onPrimaryContainer: Int,
    val error: Int,
    val runicText: Int
) {
    /** Factory methods for [WidgetPalette]. */
    companion object {
        /** Returns the default dark-theme palette. */
        fun default() = widgetPaletteFromColorScheme(foundationRunicColorScheme(darkTheme = true))
    }
}

/** Exact quote content required before accepting a previously rendered widget. */
data class WidgetQuoteContent(val quoteId: Long, val latinText: String, val author: String, val runicText: String) {
    /** Rejects edited, deleted, or differently rendered quote snapshots. */
    fun matches(state: WidgetState): Boolean = quoteId == state.quoteId && latinText == state.latinText &&
        author == state.author && runicText == state.runicText
}
