package com.po4yka.runatal.ui.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback

/**
 * Action callback for refreshing the widget when user taps the refresh button.
 * Clears the cache and triggers a fresh widget update.
 */
class RefreshQuoteAction() : ActionCallback {

    private var refreshRunner: WidgetRefreshRunner = DefaultWidgetRefreshRunner

    internal constructor(refreshRunner: WidgetRefreshRunner) : this() {
        this.refreshRunner = refreshRunner
    }

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val widgetKey = glanceId.toString()
        // Clear caches to force fresh data
        WidgetStateCache.clear(widgetKey)
        PersistentWidgetStateCache.clear(context, widgetKey)
        WidgetInteractionState.requestRandomQuote(widgetKey)

        // Update the specific widget that was clicked
        refreshRunner.refresh(context, glanceId)
    }
}
