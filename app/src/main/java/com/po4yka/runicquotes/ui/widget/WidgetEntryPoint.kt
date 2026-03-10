package com.po4yka.runicquotes.ui.widget

import com.po4yka.runicquotes.data.preferences.UserPreferencesManager
import com.po4yka.runicquotes.data.repository.QuoteRepository
import com.po4yka.runicquotes.domain.transliteration.TransliterationFactory
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
    /** Provides the quote repository instance. */
    fun quoteRepository(): QuoteRepository

    /** Provides the user preferences manager instance. */
    fun userPreferencesManager(): UserPreferencesManager

    /** Provides the transliteration factory instance. */
    fun transliterationFactory(): TransliterationFactory
}
