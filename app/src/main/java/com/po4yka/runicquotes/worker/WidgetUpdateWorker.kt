package com.po4yka.runicquotes.worker

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.po4yka.runicquotes.ui.widget.RunicQuoteWidget
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * WorkManager worker that updates the widget daily.
 * Uses Hilt for dependency injection.
 */
@HiltWorker
class WidgetUpdateWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // Trigger widget update
            RunicQuoteWidget().updateAll(applicationContext)
            Result.success()
        } catch (e: Exception) {
            // Retry on failure
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        const val WORK_NAME = "widget_update_worker"
    }
}
