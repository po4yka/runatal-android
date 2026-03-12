package com.po4yka.runicquotes.worker

import android.content.Context
import androidx.work.ListenableWorker.Result
import androidx.work.testing.TestListenableWorkerBuilder
import com.google.common.truth.Truth.assertThat
import com.po4yka.runicquotes.domain.repository.TranslationRepository
import dagger.hilt.android.EntryPointAccessors
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class TranslationBackfillWorkerTest {

    private lateinit var context: Context
    private lateinit var repository: TranslationRepository
    private lateinit var entryPoint: TranslationWorkerEntryPoint

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        repository = mockk()
        entryPoint = mockk()

        every { entryPoint.translationRepository() } returns repository
        mockkStatic(EntryPointAccessors::class)
        every {
            EntryPointAccessors.fromApplication(
                context,
                TranslationWorkerEntryPoint::class.java
            )
        } returns entryPoint
    }

    @After
    fun tearDown() {
        unmockkStatic(EntryPointAccessors::class)
    }

    @Test
    fun `doWork returns success when repository backfill completes`() = runTest {
        coEvery { repository.backfillAllQuotes() } returns Unit

        val worker = worker(runAttemptCount = 0)

        assertThat(worker.doWork()).isEqualTo(Result.success())
        coVerify(exactly = 1) { repository.backfillAllQuotes() }
    }

    @Test
    fun `doWork retries on io exception before max attempts`() = runTest {
        coEvery { repository.backfillAllQuotes() } throws IOException("disk")

        val worker = worker(runAttemptCount = 1)

        assertThat(worker.doWork()).isEqualTo(Result.retry())
    }

    @Test
    fun `doWork fails on io exception after max attempts`() = runTest {
        coEvery { repository.backfillAllQuotes() } throws IOException("disk")

        val worker = worker(runAttemptCount = 3)

        assertThat(worker.doWork()).isEqualTo(Result.failure())
    }

    @Test
    fun `doWork fails on invalid state exceptions`() = runTest {
        coEvery { repository.backfillAllQuotes() } throws IllegalStateException("bad")

        val worker = worker(runAttemptCount = 0)

        assertThat(worker.doWork()).isEqualTo(Result.failure())
    }

    private fun worker(runAttemptCount: Int): TranslationBackfillWorker {
        return TestListenableWorkerBuilder<TranslationBackfillWorker>(context)
            .setRunAttemptCount(runAttemptCount)
            .build()
    }
}
