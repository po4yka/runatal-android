package com.po4yka.runatal.ui.widget

import com.po4yka.runatal.data.preferences.UserPreferencesManager
import com.po4yka.runatal.di.IoDispatcher
import com.po4yka.runatal.domain.repository.QuoteRepository
import com.po4yka.runatal.domain.usecase.quote.ResolveQuoteRenderingUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher

/**
 * Hilt EntryPoint for accessing dependencies in the widget.
 * Widgets cannot use @Inject directly, so we use EntryPoints.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface WidgetEntryPoint {
    /** Provides the quote repository instance. */
    fun quoteRepository(): QuoteRepository

    /** Provides the user preferences manager instance. */
    fun userPreferencesManager(): UserPreferencesManager

    /** Provides the transliteration factory instance. */
    fun resolveQuoteRenderingUseCase(): ResolveQuoteRenderingUseCase

    /** Provides the IO dispatcher for widget loading work. */
    @IoDispatcher
    fun ioDispatcher(): CoroutineDispatcher
}
