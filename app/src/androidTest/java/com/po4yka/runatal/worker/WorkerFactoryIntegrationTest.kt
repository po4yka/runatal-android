package com.po4yka.runatal.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.po4yka.runatal.RunatalApplication
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkerFactoryIntegrationTest {

    @Test
    fun createsWidgetUpdateWorkerFromHiltBindings() {
        val worker = buildWithHilt<WidgetUpdateWorker>()

        assertEquals(WidgetUpdateWorker::class.java, worker.javaClass)
    }

    @Test
    fun createsTranslationBackfillWorkerFromHiltBindings() {
        val worker = buildWithHilt<TranslationBackfillWorker>()

        assertEquals(TranslationBackfillWorker::class.java, worker.javaClass)
    }

    @Test
    fun createsDailyNotificationWorkerFromHiltBindings() {
        assertEquals(DailyQuoteNotificationWorker::class.java, buildWithHilt<DailyQuoteNotificationWorker>().javaClass)
    }

    @Test
    fun createsStreakNotificationWorkerFromHiltBindings() {
        assertEquals(StreakNotificationWorker::class.java, buildWithHilt<StreakNotificationWorker>().javaClass)
    }

    @Test
    fun createsPackNotificationWorkerFromHiltBindings() {
        assertEquals(PackUpdateNotificationWorker::class.java, buildWithHilt<PackUpdateNotificationWorker>().javaClass)
    }

    private inline fun <reified T : ListenableWorker> buildWithHilt(): T {
        val application = ApplicationProvider.getApplicationContext<RunatalApplication>()
        val hiltFactory = application.workerFactory
        assertSame(hiltFactory, application.workManagerConfiguration.workerFactory)

        // Fail before WorkManager's reflection fallback can conceal a missing Hilt binding.
        val strictFactory = object : WorkerFactory() {
            override fun createWorker(
                appContext: Context,
                workerClassName: String,
                workerParameters: WorkerParameters
            ): ListenableWorker = checkNotNull(
                hiltFactory.createWorker(appContext, workerClassName, workerParameters)
            ) {
                "Hilt has no factory binding for $workerClassName"
            }
        }

        return TestListenableWorkerBuilder<T>(application)
            .setWorkerFactory(strictFactory)
            .build()
    }
}
