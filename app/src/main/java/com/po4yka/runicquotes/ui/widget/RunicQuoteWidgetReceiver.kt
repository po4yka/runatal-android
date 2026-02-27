package com.po4yka.runicquotes.ui.widget

import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.po4yka.runicquotes.RunicQuotesApplication

/**
 * Receiver for the Runic Quote widget.
 * Handles widget lifecycle events and schedules daily updates.
 */
class RunicQuoteWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = RunicQuoteWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        RunicQuotesApplication.widgetSyncManager(context).refreshAndRescheduleAsync(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        RunicQuotesApplication.widgetSyncManager(context).cancelSchedule(context)
        WidgetStateCache.clear()
        WidgetInteractionState.clear()
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_DATE_CHANGED -> {
                WidgetStateCache.clear()
                RunicQuotesApplication.widgetSyncManager(context).refreshAndRescheduleAsync(context)
            }
        }
    }
}
