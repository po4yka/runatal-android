package com.po4yka.runatal.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.po4yka.runatal.domain.repository.NoOpTranslationRepository
import com.po4yka.runatal.domain.repository.TranslationRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.IOException

/**
 * One-time worker that fills the structured historical translation cache.
 */
@HiltWorker
internal class TranslationBackfillWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val translationRepository: TranslationRepository
) : CoroutineWorker(appContext, workerParams) {

    private var backfillRunner: TranslationBackfillRunner = RepositoryTranslationBackfillRunner(translationRepository)

    internal constructor(
        appContext: Context,
        workerParams: WorkerParameters,
        backfillRunner: TranslationBackfillRunner
    ) : this(
        appContext = appContext,
        workerParams = workerParams,
        translationRepository = NoOpTranslationRepository
    ) {
        this.backfillRunner = backfillRunner
    }

    override suspend fun doWork(): Result {
        return try {
            backfillRunner.backfillAllQuotes()
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

internal interface TranslationBackfillRunner {
    suspend fun backfillAllQuotes()
}

private class RepositoryTranslationBackfillRunner(
    private val translationRepository: TranslationRepository
) : TranslationBackfillRunner {
    override suspend fun backfillAllQuotes() {
        translationRepository.backfillAllQuotes()
    }
}
