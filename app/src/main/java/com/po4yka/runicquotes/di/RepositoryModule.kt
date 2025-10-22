package com.po4yka.runicquotes.di

import com.po4yka.runicquotes.data.repository.QuoteRepository
import com.po4yka.runicquotes.data.repository.QuoteRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing repository implementations.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindQuoteRepository(
        impl: QuoteRepositoryImpl
    ): QuoteRepository
}
