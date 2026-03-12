package com.po4yka.runicquotes.worker

import com.po4yka.runicquotes.data.repository.TranslationRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Entry point for workers that cannot use constructor injection.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface TranslationWorkerEntryPoint {
    /** Provides access to structured historical translations inside workers. */
    fun translationRepository(): TranslationRepository
}
