package com.po4yka.runicquotes.ui.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.po4yka.runicquotes.worker.WidgetUpdateWorker
import dagger.hilt.android.EntryPointAccessors
import java.time.Duration
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Coordinates widget refreshes and WorkManager scheduling.
 */
object WidgetSyncManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun refreshAllAsync(context: Context) {
        scope.launch {
            RunicQuoteWidget().updateAll(context.applicationContext)
        }
    }

    fun rescheduleAsync(context: Context) {
        scope.launch {
            reschedule(context.applicationContext)
        }
    }

    fun refreshAndRescheduleAsync(context: Context) {
        refreshAllAsync(context)
        rescheduleAsync(context)
    }

    fun cancelSchedule(context: Context) {
        WorkManager.getInstance(context.applicationContext)
            .cancelUniqueWork(WidgetUpdateWorker.WORK_NAME)
    }

    private suspend fun reschedule(context: Context) {
        val appContext = context.applicationContext
        val workManager = WorkManager.getInstance(appContext)
        val updateMode = readUpdateMode(appContext)

        if (updateMode == WidgetUpdateMode.MANUAL) {
            cancelSchedule(appContext)
            return
        }

        val intervalHours = updateMode.intervalHours ?: return
        val initialDelay = computeInitialDelay(updateMode, ZonedDateTime.now())
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(
            repeatInterval = intervalHours.toLong(),
            repeatIntervalTimeUnit = TimeUnit.HOURS
        )
            .setInitialDelay(initialDelay.toMillis(), TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            WidgetUpdateWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    private suspend fun readUpdateMode(context: Context): WidgetUpdateMode {
        val entryPoint = EntryPointAccessors.fromApplication(
            context,
            WidgetEntryPoint::class.java
        )
        val preferences = entryPoint.userPreferencesManager().userPreferencesFlow.first()
        return WidgetUpdateMode.fromPersistedValue(preferences.widgetUpdateMode)
    }

    private fun computeInitialDelay(
        updateMode: WidgetUpdateMode,
        now: ZonedDateTime
    ): Duration {
        val next = when (updateMode) {
            WidgetUpdateMode.DAILY -> {
                now.toLocalDate()
                    .plusDays(1)
                    .atStartOfDay(now.zone)
            }

            WidgetUpdateMode.EVERY_6_HOURS,
            WidgetUpdateMode.EVERY_12_HOURS -> {
                val intervalHours = updateMode.intervalHours ?: 24
                var candidate = now.truncatedTo(ChronoUnit.HOURS).plusHours(1)
                while (candidate.hour % intervalHours != 0) {
                    candidate = candidate.plusHours(1)
                }
                candidate
            }

            WidgetUpdateMode.MANUAL -> now
        }

        val delay = Duration.between(now, next)
        return if (delay.isNegative || delay.isZero) Duration.ofMinutes(1) else delay
    }
}
