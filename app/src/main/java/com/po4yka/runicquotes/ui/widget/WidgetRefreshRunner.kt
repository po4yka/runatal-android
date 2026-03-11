package com.po4yka.runicquotes.ui.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.updateAll

/**
 * Internal abstraction over widget refresh operations for worker and scheduler tests.
 */
internal interface WidgetRefreshRunner {
    suspend fun refreshAll(context: Context)

    suspend fun refresh(context: Context, glanceId: GlanceId)
}

internal object DefaultWidgetRefreshRunner : WidgetRefreshRunner {

    override suspend fun refreshAll(context: Context) {
        RunicQuoteWidget().updateAll(context.applicationContext)
    }

    override suspend fun refresh(context: Context, glanceId: GlanceId) {
        RunicQuoteWidget().update(context.applicationContext, glanceId)
    }
}
