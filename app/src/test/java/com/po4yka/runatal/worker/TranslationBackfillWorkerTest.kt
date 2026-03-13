package com.po4yka.runatal.worker

import android.content.Context
import androidx.work.ListenableWorker.Result
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Test

class TranslationBackfillWorkerTest {

    private val context = mockk<Context>(relaxed = true)

    @Test
    fun `doWork returns success when backfill completes`() = runTest {
        val backfillRunner = mockk<TranslationBackfillRunner>()
        coEvery { backfillRunner.backfillAllQuotes() } returns Unit

        val worker = worker(runAttemptCount = 0, backfillRunner = backfillRunner)

        assertThat(worker.doWork()).isEqualTo(Result.success())
    }

    @Test
    fun `doWork retries on io exception before max attempts`() = runTest {
        val backfillRunner = mockk<TranslationBackfillRunner>()
        coEvery { backfillRunner.backfillAllQuotes() } throws IOException("disk")

        val worker = worker(runAttemptCount = 1, backfillRunner = backfillRunner)

        assertThat(worker.doWork()).isEqualTo(Result.retry())
    }

    @Test
    fun `doWork fails on io exception after max attempts`() = runTest {
        val backfillRunner = mockk<TranslationBackfillRunner>()
        coEvery { backfillRunner.backfillAllQuotes() } throws IOException("disk")

        val worker = worker(runAttemptCount = 3, backfillRunner = backfillRunner)

        assertThat(worker.doWork()).isEqualTo(Result.failure())
    }

    @Test
    fun `doWork fails on invalid state exceptions`() = runTest {
        val backfillRunner = mockk<TranslationBackfillRunner>()
        coEvery { backfillRunner.backfillAllQuotes() } throws IllegalStateException("bad")

        val worker = worker(runAttemptCount = 0, backfillRunner = backfillRunner)

        assertThat(worker.doWork()).isEqualTo(Result.failure())
    }

    private fun worker(
        runAttemptCount: Int,
        backfillRunner: TranslationBackfillRunner
    ): TranslationBackfillWorker {
        return TranslationBackfillWorker(
            appContext = context,
            workerParams = workerParams(runAttemptCount = runAttemptCount),
            backfillRunner = backfillRunner
        )
    }

    private fun workerParams(runAttemptCount: Int): androidx.work.WorkerParameters {
        return mockk<androidx.work.WorkerParameters>().also {
            every { it.runAttemptCount } returns runAttemptCount
        }
    }
}
