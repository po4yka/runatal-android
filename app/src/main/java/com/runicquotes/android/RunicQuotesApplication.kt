package com.runicquotes.android

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for Runic Quotes.
 * Annotated with @HiltAndroidApp to enable Hilt dependency injection.
 */
@HiltAndroidApp
class RunicQuotesApplication : Application()
