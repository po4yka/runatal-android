package com.po4yka.runatal.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.po4yka.runatal.notification.NotificationDelivery
import com.po4yka.runatal.notification.NotificationKind
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.IOException
import kotlinx.coroutines.CancellationException

/** Runs the local daily quote event through the guarded delivery path. */
@HiltWorker
internal class DailyQuoteNotificationWorker @AssistedInject constructor(
    @Assisted context: Context, @Assisted parameters: WorkerParameters,
    private val delivery: NotificationDelivery
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = notificationWork { delivery.deliver(NotificationKind.DAILY) }
}

/** Reminds only an actual unfinished reading streak. */
@HiltWorker
internal class StreakNotificationWorker @AssistedInject constructor(
    @Assisted context: Context, @Assisted parameters: WorkerParameters,
    private val delivery: NotificationDelivery
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = notificationWork { delivery.deliver(NotificationKind.STREAK) }
}

/** Announces real changes to bundled pack content. */
@HiltWorker
internal class PackUpdateNotificationWorker @AssistedInject constructor(
    @Assisted context: Context, @Assisted parameters: WorkerParameters,
    private val delivery: NotificationDelivery
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = notificationWork { delivery.deliver(NotificationKind.PACKS) }
}

private suspend fun notificationWork(action: suspend () -> Unit): ListenableWorker.Result = try {
    action()
    ListenableWorker.Result.success()
} catch (exception: CancellationException) {
    throw exception
} catch (_: IOException) {
    ListenableWorker.Result.retry()
} catch (_: IllegalStateException) {
    ListenableWorker.Result.failure()
} catch (_: IllegalArgumentException) {
    ListenableWorker.Result.failure()
}
