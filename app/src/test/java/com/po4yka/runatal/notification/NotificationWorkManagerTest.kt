package com.po4yka.runatal.notification

import android.Manifest
import android.app.Application
import android.content.Context
import android.app.NotificationManager
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import com.google.common.truth.Truth.assertThat
import com.po4yka.runatal.data.preferences.UserPreferencesManager
import com.po4yka.runatal.domain.model.Quote
import com.po4yka.runatal.domain.model.ReadingStats
import com.po4yka.runatal.domain.repository.QuoteRepository
import com.po4yka.runatal.domain.repository.ReadingHistoryRepository
import com.po4yka.runatal.util.TimeProvider
import com.po4yka.runatal.worker.DailyQuoteNotificationWorker
import com.po4yka.runatal.worker.PackUpdateNotificationWorker
import com.po4yka.runatal.worker.StreakNotificationWorker
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.io.File
import java.io.IOException
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.delay
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.SQLiteMode

/** Real WorkManager test driver executes the concrete worker and Android notification API. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class NotificationWorkManagerTest {
    private lateinit var context: Context
    private lateinit var preferences: UserPreferencesManager
    private lateinit var publisher: NotificationPublisher
    private lateinit var delivery: NotificationDelivery
    private lateinit var scheduler: NotificationScheduler
    private lateinit var factory: WorkerFactory
    private lateinit var quotes: QuoteRepository
    private lateinit var directory: File
    private lateinit var scope: CoroutineScope

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        shadowOf(context as Application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        directory = kotlin.io.path.createTempDirectory("notification-work-test").toFile()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        preferences = UserPreferencesManager(PreferenceDataStoreFactory.create(scope = scope) {
            File(directory, "notifications.preferences_pb")
        })
        publisher = NotificationPublisher(context).apply { createChannels() }
        quotes = mockk()
        coEvery { quotes.quoteOfTheDay() } returns Quote(1L, "A daily library quote", "Author", null, null, null)
        coEvery { quotes.getQuoteById(1L) } returns Quote(1L, "A daily library quote", "Author", null, null, null)
        val readings = mockk<ReadingHistoryRepository>()
        every { readings.stats() } returns flowOf(ReadingStats(0, 0, null))
        val time = mockk<TimeProvider>()
        every { time.getCurrentDate() } returns LocalDate.of(2026, 10, 9)
        val catalogue = BundledPackCatalogue()
        delivery = NotificationDelivery(preferences, publisher, quotes, readings, catalogue, time)
        factory = object : WorkerFactory() {
            override fun createWorker(appContext: Context, workerClassName: String,
                workerParameters: WorkerParameters): ListenableWorker? = when (workerClassName) {
                DailyQuoteNotificationWorker::class.java.name ->
                    DailyQuoteNotificationWorker(appContext, workerParameters, delivery)
                StreakNotificationWorker::class.java.name ->
                    StreakNotificationWorker(appContext, workerParameters, delivery)
                PackUpdateNotificationWorker::class.java.name ->
                    PackUpdateNotificationWorker(appContext, workerParameters, delivery)
                else -> null
            }
        }
        WorkManagerTestInitHelper.initializeTestWorkManager(context, Configuration.Builder()
            .setExecutor(SynchronousExecutor()).setWorkerFactory(factory).build())
        scheduler = NotificationScheduler(context, preferences, publisher, delivery, catalogue)
    }

    @After
    fun tearDown() {
        WorkManagerTestInitHelper.closeWorkDatabase()
        scope.cancel()
        directory.deleteRecursively()
    }

    @Test
    fun `scheduled daily work executes a real notification and disabling cancels work and posted output`() = runTest {
        scheduler.synchronize(preferences.notificationPreferencesFlow.first(), publisher.snapshot())
        val manager = WorkManager.getInstance(context)
        val daily = manager.getWorkInfosForUniqueWork(NotificationScheduler.DAILY_WORK).get().single()
        checkNotNull(WorkManagerTestInitHelper.getTestDriver(context)).setInitialDelayMet(daily.id)
        val notifications = context.getSystemService(NotificationManager::class.java)
        withContext(Dispatchers.IO) {
            withTimeout(5_000L) {
                while (notifications.activeNotifications.isEmpty()) delay(10L)
            }
        }
        assertThat(notifications.activeNotifications).hasLength(1)
        preferences.updateDailyQuoteNotifications(false)
        scheduler.synchronize(preferences.notificationPreferencesFlow.first(), publisher.snapshot())
        assertThat(manager.getWorkInfosForUniqueWork(NotificationScheduler.DAILY_WORK).get().single().state.isFinished)
            .isTrue()
        assertThat(notifications.activeNotifications).isEmpty()
    }

    @Test
    fun `worker retries preparation IO failure without posting or consuming its date`() = runTest {
        coEvery { quotes.quoteOfTheDay() } throws IOException("storage unavailable")
        val worker = TestListenableWorkerBuilder<DailyQuoteNotificationWorker>(context)
            .setWorkerFactory(factory).build()
        assertThat(worker.doWork()).isInstanceOf(ListenableWorker.Result.Retry::class.java)
        assertThat(context.getSystemService(NotificationManager::class.java).activeNotifications).isEmpty()
        assertThat(preferences.lastNotificationDay("DAILY")).isNull()
    }

    @Test
    fun `worker cancellation propagates and cannot post a notification`() = runTest {
        coEvery { quotes.quoteOfTheDay() } throws CancellationException("worker stopped")
        val worker = TestListenableWorkerBuilder<DailyQuoteNotificationWorker>(context)
            .setWorkerFactory(factory).build()
        try {
            worker.doWork()
            error("Expected cancellation")
        } catch (expected: CancellationException) {
            assertThat(expected.message).isEqualTo("worker stopped")
        }
        assertThat(context.getSystemService(NotificationManager::class.java).activeNotifications).isEmpty()
        assertThat(preferences.lastNotificationDay("DAILY")).isNull()
    }
}
