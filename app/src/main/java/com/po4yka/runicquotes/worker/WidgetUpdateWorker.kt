package com.po4yka.runicquotes.worker

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.po4yka.runicquotes.ui.widget.RunicQuoteWidget
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.IOException

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
        } catch (e: IOException) {
            Log.e(TAG, "IO error updating widget, attempt $runAttemptCount", e)
            // Retry on failure
            if (runAttemptCount < MAX_RETRY_ATTEMPTS) {
                Result.retry()
            } else {
                Log.e(TAG, "Max retry attempts reached, failing widget update", e)
                Result.failure()
            }
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Invalid state while updating widget", e)
            Result.failure()
        }
    }

    companion object {
        private const val TAG = "WidgetUpdateWorker"
        const val WORK_NAME = "widget_update_worker"
        private const val MAX_RETRY_ATTEMPTS = 3
    }
}
