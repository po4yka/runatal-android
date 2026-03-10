package com.po4yka.runicquotes.di

import com.po4yka.runicquotes.data.repository.ArchiveRepository
import com.po4yka.runicquotes.data.repository.ArchiveRepositoryImpl
import com.po4yka.runicquotes.data.repository.QuotePackRepository
import com.po4yka.runicquotes.data.repository.QuotePackRepositoryImpl
import com.po4yka.runicquotes.data.repository.QuoteRepository
import com.po4yka.runicquotes.data.repository.QuoteRepositoryImpl
import com.po4yka.runicquotes.data.repository.RuneReferenceRepository
import com.po4yka.runicquotes.data.repository.RuneReferenceRepositoryImpl
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

    /** Binds [QuoteRepositoryImpl] as the [QuoteRepository] implementation. */
    @Binds
    @Singleton
    abstract fun bindQuoteRepository(
        impl: QuoteRepositoryImpl
    ): QuoteRepository

    /** Binds [QuotePackRepositoryImpl] as the [QuotePackRepository] implementation. */
    @Binds
    @Singleton
    abstract fun bindQuotePackRepository(
        impl: QuotePackRepositoryImpl
    ): QuotePackRepository

    /** Binds [ArchiveRepositoryImpl] as the [ArchiveRepository] implementation. */
    @Binds
    @Singleton
    abstract fun bindArchiveRepository(
        impl: ArchiveRepositoryImpl
    ): ArchiveRepository

    /** Binds [RuneReferenceRepositoryImpl] as the [RuneReferenceRepository] implementation. */
    @Binds
    @Singleton
    abstract fun bindRuneReferenceRepository(
        impl: RuneReferenceRepositoryImpl
    ): RuneReferenceRepository
}
