package com.po4yka.runatal.ui.widget

import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.po4yka.runatal.RunatalApplication

/**
 * Receiver for the Runic Quote widget.
 * Handles widget lifecycle events and schedules daily updates.
 */
class RunatalWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = RunatalWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        RunatalApplication.widgetSyncManager(context).refreshAndRescheduleAsync(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        RunatalApplication.widgetSyncManager(context).cancelSchedule(context)
        WidgetStateCache.clear()
        PersistentWidgetStateCache.clear(context)
        WidgetInteractionState.clear()
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_DATE_CHANGED -> {
                WidgetStateCache.clear()
                PersistentWidgetStateCache.clear(context)
                RunatalApplication.widgetSyncManager(context).refreshAndRescheduleAsync(context)
            }
        }
    }
}
