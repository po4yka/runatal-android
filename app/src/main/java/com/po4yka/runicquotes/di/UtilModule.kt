package com.po4yka.runicquotes.di

import com.po4yka.runicquotes.util.SystemTimeProvider
import com.po4yka.runicquotes.util.TimeProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing utility implementations.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class UtilModule {

    @Binds
    @Singleton
    /** Binds [SystemTimeProvider] as the [TimeProvider] implementation. */
    abstract fun bindTimeProvider(
        impl: SystemTimeProvider
    ): TimeProvider
}
