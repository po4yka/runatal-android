package com.po4yka.runatal.notification

import android.content.Context
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.po4yka.runatal.data.preferences.NotificationPreferencesSnapshot
import com.po4yka.runatal.data.preferences.UserPreferencesManager
import com.po4yka.runatal.worker.DailyQuoteNotificationWorker
import com.po4yka.runatal.worker.PackUpdateNotificationWorker
import com.po4yka.runatal.worker.StreakNotificationWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/** Application-scope scheduling follows requested settings and current system access. */
@Singleton
internal class NotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: UserPreferencesManager,
    private val publisher: NotificationPublisher,
    private val delivery: NotificationDelivery,
    private val catalogue: BundledPackCatalogue
) {
    // Resolved after Application member injection, when HiltWorkerFactory is available.
    private val workManager: WorkManager get() = WorkManager.getInstance(context)

    fun start(scope: CoroutineScope) {
        publisher.createChannels()
        scope.launch {
            try {
                if (preferences.notificationPackCatalogue() == null) {
                    preferences.markNotificationPackCatalogue(catalogue.fingerprints())
                }
                combine(preferences.notificationPreferencesFlow, publisher.access) { prefs, access -> prefs to access }
                    .distinctUntilChanged().collect { (prefs, access) -> synchronize(prefs, access) }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: IOException) {
                cancelAll()
                Log.e(TAG, "Notification preferences unavailable; delivery stopped", exception)
            } catch (exception: IllegalStateException) {
                cancelAll()
                Log.e(TAG, "Notification scheduling unavailable", exception)
            }
        }
    }

    suspend fun synchronize(prefs: NotificationPreferencesSnapshot, access: NotificationAccess) {
        scheduleDaily(prefs.dailyQuote && access.allows(NotificationKind.DAILY))
        scheduleStreak(prefs.streak && access.allows(NotificationKind.STREAK))
        if (prefs.packUpdates && access.allows(NotificationKind.PACKS)) {
            workManager.enqueueUniqueWork(PACK_WORK, ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<PackUpdateNotificationWorker>().build())
        } else {
            cancel(NotificationKind.PACKS)
            if (!prefs.packUpdates) preferences.markNotificationPackCatalogue(catalogue.fingerprints())
        }
    }

    private suspend fun scheduleDaily(enabled: Boolean) {
        if (enabled) {
            workManager.enqueueUniquePeriodicWork(DAILY_WORK, ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<DailyQuoteNotificationWorker>(1, TimeUnit.DAYS)
                    .setInitialDelay(delayUntilHour(MORNING_HOUR).toMillis(), TimeUnit.MILLISECONDS).build())
        } else cancel(NotificationKind.DAILY)
    }

    private suspend fun scheduleStreak(enabled: Boolean) {
        if (enabled) {
            workManager.enqueueUniquePeriodicWork(STREAK_WORK, ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<StreakNotificationWorker>(1, TimeUnit.DAYS)
                    .setInitialDelay(delayUntilHour(EVENING_HOUR).toMillis(), TimeUnit.MILLISECONDS).build())
        } else cancel(NotificationKind.STREAK)
    }

    private suspend fun cancel(kind: NotificationKind) {
        delivery.cancel(kind)
        try {
            workManager.cancelUniqueWork(workName(kind))
        } catch (exception: IllegalStateException) {
            Log.e(TAG, "WorkManager unavailable while cancelling ${kind.name}", exception)
        }
    }

    private suspend fun cancelAll() { NotificationKind.entries.forEach { cancel(it) } }

    companion object {
        const val DAILY_WORK = "daily_quote_notification"
        const val STREAK_WORK = "reading_streak_notification"
        const val PACK_WORK = "bundled_pack_update_notification"
        private const val MORNING_HOUR = 9
        private const val EVENING_HOUR = 20
        private const val TAG = "NotificationScheduler"

        fun workName(kind: NotificationKind): String = when (kind) {
            NotificationKind.DAILY -> DAILY_WORK
            NotificationKind.STREAK -> STREAK_WORK
            NotificationKind.PACKS -> PACK_WORK
        }

        fun delayUntilHour(hour: Int, now: ZonedDateTime = ZonedDateTime.now()): Duration {
            var next = now.withHour(hour).withMinute(0).withSecond(0).withNano(0)
            if (!next.isAfter(now)) next = next.plusDays(1)
            return Duration.between(now, next)
        }
    }
}
