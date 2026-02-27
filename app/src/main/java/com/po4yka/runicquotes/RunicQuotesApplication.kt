package com.po4yka.runicquotes

import android.app.Application
import android.content.Context
import com.po4yka.runicquotes.data.repository.QuoteRepository
import com.po4yka.runicquotes.ui.widget.WidgetSyncManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Application class for Runic Quotes.
 * Annotated with @HiltAndroidApp to enable Hilt dependency injection.
 * Handles app-level initialization such as database seeding.
 */
@HiltAndroidApp
class RunicQuotesApplication : Application() {

    @Inject
    lateinit var quoteRepository: QuoteRepository

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var widgetSyncManager: WidgetSyncManager
        private set

    override fun onCreate() {
        super.onCreate()
        widgetSyncManager = WidgetSyncManager(applicationScope)

        // Seed database on app startup (infrastructure concern, not ViewModel concern)
        applicationScope.launch {
            quoteRepository.seedIfNeeded()
        }
    }

    companion object {
        /**
         * Provides access to WidgetSyncManager from components that cannot use DI
         * (e.g., BroadcastReceivers, Glance widgets).
         */
        fun widgetSyncManager(context: Context): WidgetSyncManager {
            return (context.applicationContext as RunicQuotesApplication).widgetSyncManager
        }
    }
}
