package com.po4yka.runicquotes.ui.widget

import com.po4yka.runicquotes.data.preferences.UserPreferencesManager
import com.po4yka.runicquotes.data.repository.QuoteRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt EntryPoint for accessing dependencies in the widget.
 * Widgets cannot use @Inject directly, so we use EntryPoints.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun quoteRepository(): QuoteRepository
    fun userPreferencesManager(): UserPreferencesManager
}
