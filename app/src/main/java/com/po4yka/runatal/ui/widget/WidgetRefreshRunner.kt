package com.po4yka.runatal.ui.widget

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
        RunatalWidget().updateAll(context.applicationContext)
    }

    override suspend fun refresh(context: Context, glanceId: GlanceId) {
        RunatalWidget().update(context.applicationContext, glanceId)
    }
}
