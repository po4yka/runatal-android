package com.po4yka.runicquotes

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Constraints
import androidx.work.Configuration
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.po4yka.runicquotes.data.translation.AssetTranslationDatasetProvider
import com.po4yka.runicquotes.di.DefaultDispatcher
import com.po4yka.runicquotes.domain.repository.QuoteRepository
import com.po4yka.runicquotes.ui.widget.WidgetSyncManager
import com.po4yka.runicquotes.worker.TranslationBackfillWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Application class for Runic Quotes.
 * Annotated with @HiltAndroidApp to enable Hilt dependency injection.
 * Handles app-level initialization such as database seeding.
 */
@HiltAndroidApp
class RunicQuotesApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var quoteRepository: QuoteRepository

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    @DefaultDispatcher
    lateinit var defaultDispatcher: CoroutineDispatcher

    @Inject
    internal lateinit var translationDatasetProvider: AssetTranslationDatasetProvider

    private lateinit var applicationScope: CoroutineScope

    lateinit var widgetSyncManager: WidgetSyncManager
        private set

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        applicationScope = CoroutineScope(SupervisorJob() + defaultDispatcher)
        widgetSyncManager = WidgetSyncManager(applicationScope)

        // Seed database on app startup (infrastructure concern, not ViewModel concern)
        applicationScope.launch {
            quoteRepository.seedIfNeeded()
            scheduleHistoricalTranslationBackfill()
        }
        applicationScope.launch {
            runCatching {
                translationDatasetProvider.warmUp()
            }.onFailure { exception ->
                Log.w(TAG, "Translation dataset prewarm failed", exception)
            }
        }
    }

    private fun scheduleHistoricalTranslationBackfill() {
        try {
            val request = OneTimeWorkRequestBuilder<TranslationBackfillWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .build()
                )
                .build()
            WorkManager.getInstance(applicationContext).enqueueUniqueWork(
                TranslationBackfillWorker.WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request
            )
        } catch (exception: IllegalStateException) {
            Log.w(TAG, "WorkManager unavailable; skipping translation backfill scheduling", exception)
        }
    }

    /** Static accessors for components needed outside of DI scope. */
    companion object {
        private const val TAG = "RunicQuotesApp"

        /**
         * Provides access to WidgetSyncManager from components that cannot use DI
         * (e.g., BroadcastReceivers, Glance widgets).
         */
        fun widgetSyncManager(context: Context): WidgetSyncManager {
            return (context.applicationContext as RunicQuotesApplication).widgetSyncManager
        }
    }
}
