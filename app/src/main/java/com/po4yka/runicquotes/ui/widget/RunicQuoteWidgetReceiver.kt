package com.po4yka.runicquotes.ui.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.updateAll
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.po4yka.runicquotes.worker.WidgetUpdateWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Receiver for the Runic Quote widget.
 * Handles widget lifecycle events and schedules daily updates.
 */
class RunicQuoteWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = RunicQuoteWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)

        // Trigger immediate update when widget is first added
        // (Fixes: Poor first impression - no 1 hour wait)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            glanceAppWidget.updateAll(context)
        }

        // Schedule daily widget updates when the first widget is added
        scheduleWidgetUpdates(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        // Cancel scheduled updates when all widgets are removed
        cancelWidgetUpdates(context)
    }

    /**
     * Schedules daily widget updates using WorkManager.
     */
    private fun scheduleWidgetUpdates(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(
            repeatInterval = 24,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        )
            .setConstraints(constraints)
            // No initial delay - immediate update already triggered in onEnabled()
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WidgetUpdateWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE, // Changed from KEEP to UPDATE
            workRequest
        )
    }

    /**
     * Cancels scheduled widget updates.
     */
    private fun cancelWidgetUpdates(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WidgetUpdateWorker.WORK_NAME)
    }
}
