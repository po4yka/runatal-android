package com.po4yka.runatal.worker

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.google.common.truth.Truth.assertThat
import com.po4yka.runatal.ui.widget.WidgetRefreshRunner
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.IOException

class WidgetUpdateWorkerTest {

    private val context = mockk<Context>(relaxed = true)

    @Test
    fun `doWork returns success when refresh succeeds`() = runTest {
        val worker = WidgetUpdateWorker(context, workerParams(runAttemptCount = 0), SuccessfulRefreshRunner)

        val result = worker.doWork()

        assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)
    }

    @Test
    fun `doWork retries io failures before max attempts`() = runTest {
        val worker = WidgetUpdateWorker(context, workerParams(runAttemptCount = 1), FailingRefreshRunner(IOException("disk")))

        val result = worker.doWork()

        assertThat(result).isInstanceOf(ListenableWorker.Result.Retry::class.java)
    }

    @Test
    fun `doWork fails io failures after max attempts`() = runTest {
        val worker = WidgetUpdateWorker(context, workerParams(runAttemptCount = 3), FailingRefreshRunner(IOException("disk")))

        val result = worker.doWork()

        assertThat(result).isInstanceOf(ListenableWorker.Result.Failure::class.java)
    }

    @Test
    fun `doWork fails illegal state immediately`() = runTest {
        val worker = WidgetUpdateWorker(
            context,
            workerParams(runAttemptCount = 0),
            FailingRefreshRunner(IllegalStateException("bad"))
        )

        val result = worker.doWork()

        assertThat(result).isInstanceOf(ListenableWorker.Result.Failure::class.java)
    }

    private fun workerParams(runAttemptCount: Int): WorkerParameters {
        return mockk<WorkerParameters>().also {
            every { it.runAttemptCount } returns runAttemptCount
        }
    }

    private object SuccessfulRefreshRunner : WidgetRefreshRunner {
        override suspend fun refreshAll(context: Context) = Unit

        override suspend fun refresh(context: Context, glanceId: androidx.glance.GlanceId) = Unit
    }

    private class FailingRefreshRunner(
        private val throwable: Throwable
    ) : WidgetRefreshRunner {
        override suspend fun refreshAll(context: Context) {
            throw throwable
        }

        override suspend fun refresh(context: Context, glanceId: androidx.glance.GlanceId) = Unit
    }
}
