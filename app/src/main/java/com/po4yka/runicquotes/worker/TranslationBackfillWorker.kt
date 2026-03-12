package com.po4yka.runicquotes.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.android.EntryPointAccessors
import java.io.IOException

/**
 * One-time worker that fills the structured historical translation cache.
 */
class TranslationBackfillWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                TranslationWorkerEntryPoint::class.java
            )
            entryPoint.translationRepository().backfillAllQuotes()
            Result.success()
        } catch (exception: IOException) {
            Log.e(TAG, "IO error backfilling translations, attempt $runAttemptCount", exception)
            if (runAttemptCount < MAX_RETRY_ATTEMPTS) {
                Result.retry()
            } else {
                Result.failure()
            }
        } catch (exception: IllegalStateException) {
            Log.e(TAG, "Invalid state backfilling translations", exception)
            Result.failure()
        }
    }

    /** Worker constants shared with scheduling code. */
    companion object {
        private const val TAG = "TranslationBackfill"
        /** Unique work name used for one-time translation backfill scheduling. */
        const val WORK_NAME = "translation_backfill_worker"
        private const val MAX_RETRY_ATTEMPTS = 3
    }
}
