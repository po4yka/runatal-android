package com.po4yka.runicquotes.ui.widget

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.po4yka.runicquotes.worker.WidgetUpdateWorker
import dagger.hilt.android.EntryPointAccessors
import java.time.Clock
import java.time.Duration
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Coordinates widget refreshes and WorkManager scheduling.
 * Requires an externally-managed CoroutineScope tied to the application lifecycle.
 */
class WidgetSyncManager internal constructor(
    private val scope: CoroutineScope,
    private val refreshRunner: WidgetRefreshRunner = DefaultWidgetRefreshRunner,
    private val workScheduler: WidgetWorkScheduler = WorkManagerWidgetWorkScheduler,
    private val updateModeReader: WidgetUpdateModeReader = HiltWidgetUpdateModeReader,
    private val clock: Clock = Clock.systemDefaultZone()
) {

    /** Triggers an asynchronous refresh of all widget instances. */
    fun refreshAllAsync(context: Context) {
        scope.launch {
            refreshRunner.refreshAll(context.applicationContext)
        }
    }

    /** Asynchronously reschedules the periodic widget update work. */
    fun rescheduleAsync(context: Context) {
        scope.launch {
            reschedule(context.applicationContext)
        }
    }

    /** Refreshes all widgets and reschedules the periodic update work. */
    fun refreshAndRescheduleAsync(context: Context) {
        refreshAllAsync(context)
        rescheduleAsync(context)
    }

    /** Cancels the scheduled periodic widget update work. */
    fun cancelSchedule(context: Context) {
        workScheduler.cancel(context.applicationContext)
    }

    private suspend fun reschedule(context: Context) {
        val appContext = context.applicationContext
        val updateMode = updateModeReader.read(appContext)

        if (updateMode == WidgetUpdateMode.MANUAL) {
            workScheduler.cancel(appContext)
            return
        }

        val intervalHours = updateMode.intervalHours ?: return
        val initialDelay = computeInitialDelay(updateMode, ZonedDateTime.now(clock))
        workScheduler.enqueuePeriodicRefresh(
            context = appContext,
            intervalHours = intervalHours.toLong(),
            initialDelay = initialDelay
        )
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

internal interface WidgetWorkScheduler {
    fun enqueuePeriodicRefresh(context: Context, intervalHours: Long, initialDelay: Duration)

    fun cancel(context: Context)
}

internal object WorkManagerWidgetWorkScheduler : WidgetWorkScheduler {
    override fun enqueuePeriodicRefresh(context: Context, intervalHours: Long, initialDelay: Duration) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(
            repeatInterval = intervalHours,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        )
            .setInitialDelay(initialDelay.toMillis(), TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
            WidgetUpdateWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    override fun cancel(context: Context) {
        WorkManager.getInstance(context.applicationContext)
            .cancelUniqueWork(WidgetUpdateWorker.WORK_NAME)
    }
}

internal interface WidgetUpdateModeReader {
    suspend fun read(context: Context): WidgetUpdateMode
}

internal object HiltWidgetUpdateModeReader : WidgetUpdateModeReader {
    override suspend fun read(context: Context): WidgetUpdateMode {
        val entryPoint = EntryPointAccessors.fromApplication(
            context,
            WidgetEntryPoint::class.java
        )
        val preferences = entryPoint.userPreferencesManager().userPreferencesFlow.first()
        return WidgetUpdateMode.fromPersistedValue(preferences.widgetUpdateMode)
    }
}
